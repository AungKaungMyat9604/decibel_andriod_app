package com.decibel.player

import androidx.media3.common.C
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.audio.TeeAudioProcessor
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference
import kotlin.math.cos
import kotlin.math.ln
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Analyzes PCM from ExoPlayer’s audio pipeline (no microphone / RECORD_AUDIO).
 *
 * TeeAudioProcessor sees PCM *before* it enters AudioTrack. ExoPlayer’s default
 * sink buffers ~250 ms of PCM, so UI snapshots are delayed by that amount
 * (sample-counted, not wall-clock) so waves land with the speaker.
 *
 * Band levels use peak magnitude + spectral flux so kicks/onsets read clearly.
 */
@OptIn(UnstableApi::class)
class PlaybackWaveCapture {
    private val active = AtomicBoolean(false)

    private val previousBands = FloatArray(BAND_COUNT)
    private val previousRaw = FloatArray(BAND_COUNT)
    private val bandsRef = AtomicReference(FloatArray(BAND_COUNT))
    private val energyRef = AtomicReference(0f)

    /**
     * Sample-accurate delay line of analyzed frames.
     * Write happens on the audio thread; UI reads a slot [delayFrames] behind.
     */
    private val delayBands = Array(DELAY_CAPACITY) { FloatArray(BAND_COUNT) }
    private val delayEnergy = FloatArray(DELAY_CAPACITY)
    private var delayWrite = 0
    private var delayCount = 0
    private val delayLock = Any()

    @Volatile private var sampleRateHz = 44_100
    @Volatile private var channelCount = 2
    @Volatile private var encoding = C.ENCODING_PCM_16BIT
    /** Total Tee→speaker delay (wired base + optional Bluetooth extra). */
    @Volatile private var pipelineLatencyMs = BASE_LATENCY_MS
    @Volatile private var delayFrames = delayFramesFor(44_100, BASE_LATENCY_MS)
    @Volatile private var bluetoothOutput = false

    private val ring = FloatArray(RING_SIZE)
    private var ringWrite = 0
    private var samplesSinceHop = 0
    /** 1 = full rate; 2 = eco (skip every other analysis hop). */
    @Volatile private var analyzeEveryNHops = 1
    private var hopsUntilAnalyze = 0

    private val re = FloatArray(FFT_SIZE)
    private val im = FloatArray(FFT_SIZE)
    private val mag = FloatArray(FFT_SIZE / 2)
    private val bandBinStart = IntArray(BAND_COUNT)
    private val bandBinEnd = IntArray(BAND_COUNT)
    private var binsConfiguredForRate = -1

    /** Slow peak tracker for gentle AGC (must not crush kick dynamics). */
    private var agcPeak = 0.05f

    val audioProcessor: AudioProcessor = TeeAudioProcessor(
        object : TeeAudioProcessor.AudioBufferSink {
            override fun flush(sampleRateHz: Int, channelCount: Int, encoding: Int) {
                this@PlaybackWaveCapture.sampleRateHz = sampleRateHz.coerceAtLeast(8_000)
                this@PlaybackWaveCapture.channelCount = channelCount.coerceAtLeast(1)
                this@PlaybackWaveCapture.encoding = encoding
                ringWrite = 0
                samplesSinceHop = 0
                binsConfiguredForRate = -1
                recalculateDelayFrames()
                synchronized(delayLock) {
                    delayWrite = 0
                    delayCount = 0
                }
            }

            override fun handleBuffer(buffer: ByteBuffer) {
                if (!active.get()) return
                when (encoding) {
                    C.ENCODING_PCM_FLOAT -> ingestPcmFloat(buffer)
                    C.ENCODING_PCM_16BIT -> ingestPcm16(buffer)
                    C.ENCODING_PCM_24BIT -> ingestPcm24(buffer)
                    C.ENCODING_PCM_32BIT -> ingestPcm32(buffer)
                    else -> ingestPcm16(buffer)
                }
            }
        },
    )

    fun setActive(enabled: Boolean) {
        active.set(enabled)
        if (!enabled) {
            previousBands.fill(0f)
            previousRaw.fill(0f)
            bandsRef.set(FloatArray(BAND_COUNT))
            energyRef.set(0f)
            ringWrite = 0
            samplesSinceHop = 0
            hopsUntilAnalyze = 0
            agcPeak = 0.05f
            synchronized(delayLock) {
                delayWrite = 0
                delayCount = 0
            }
        }
    }

    /**
     * Standby eco: analyze half as often (~86 Hz → ~43 Hz at 44.1 kHz) to cut FFT cost.
     * Visuals stay smooth enough at 30 UI fps.
     */
    fun setEcoMode(enabled: Boolean) {
        analyzeEveryNHops = if (enabled) 2 else 1
        hopsUntilAnalyze = 0
    }

    fun setBluetoothOutput(enabled: Boolean) {
        if (bluetoothOutput == enabled) return
        bluetoothOutput = enabled
        pipelineLatencyMs = BASE_LATENCY_MS + if (enabled) BLUETOOTH_EXTRA_LATENCY_MS else 0
        recalculateDelayFrames()
        // Drop history so we don't show frames timed for the old delay.
        synchronized(delayLock) {
            delayWrite = 0
            delayCount = 0
        }
    }

    fun isBluetoothOutput(): Boolean = bluetoothOutput

    private fun recalculateDelayFrames() {
        delayFrames = delayFramesFor(sampleRateHz, pipelineLatencyMs)
    }

    fun snapshotBands(): FloatArray = delayedBands().copyOf()

    fun snapshotEnergy(): Float = delayedEnergy()

    /** Copy delayed bands into [dst] without allocating (Standby UI path). */
    fun copyBandsInto(dst: FloatArray) {
        val src = delayedBands()
        val n = min(dst.size, BAND_COUNT)
        System.arraycopy(src, 0, dst, 0, n)
    }

    private fun delayedBands(): FloatArray {
        synchronized(delayLock) {
            if (delayCount <= 0) return bandsRef.get()
            // Until the delay line is primed, hold at silence rather than showing early.
            if (delayCount < delayFrames) return ZERO_BANDS
            val read = (delayWrite - delayFrames + DELAY_CAPACITY) % DELAY_CAPACITY
            return delayBands[read]
        }
    }

    private fun delayedEnergy(): Float {
        synchronized(delayLock) {
            if (delayCount <= 0) return energyRef.get()
            if (delayCount < delayFrames) return 0f
            val read = (delayWrite - delayFrames + DELAY_CAPACITY) % DELAY_CAPACITY
            return delayEnergy[read]
        }
    }

    private fun publish(bands: FloatArray, energy: Float) {
        bandsRef.set(bands)
        energyRef.set(energy)
        synchronized(delayLock) {
            val dst = delayBands[delayWrite]
            System.arraycopy(bands, 0, dst, 0, BAND_COUNT)
            delayEnergy[delayWrite] = energy
            delayWrite = (delayWrite + 1) % DELAY_CAPACITY
            if (delayCount < DELAY_CAPACITY) delayCount++
        }
    }

    private fun ingestPcm16(buffer: ByteBuffer) {
        val channels = channelCount
        val dup = buffer.asReadOnlyBuffer().order(ByteOrder.LITTLE_ENDIAN)
        val frameBytes = 2 * channels
        while (dup.remaining() >= frameBytes) {
            var mono = 0
            for (c in 0 until channels) mono += dup.short.toInt()
            pushSample((mono / channels) / 32768f)
        }
    }

    private fun ingestPcmFloat(buffer: ByteBuffer) {
        val channels = channelCount
        val dup = buffer.asReadOnlyBuffer().order(ByteOrder.LITTLE_ENDIAN)
        val frameBytes = 4 * channels
        while (dup.remaining() >= frameBytes) {
            var mono = 0f
            for (c in 0 until channels) mono += dup.float
            pushSample(mono / channels)
        }
    }

    private fun ingestPcm24(buffer: ByteBuffer) {
        val channels = channelCount
        val dup = buffer.asReadOnlyBuffer().order(ByteOrder.LITTLE_ENDIAN)
        val frameBytes = 3 * channels
        while (dup.remaining() >= frameBytes) {
            var mono = 0
            for (c in 0 until channels) {
                val b0 = dup.get().toInt() and 0xFF
                val b1 = dup.get().toInt() and 0xFF
                val b2 = dup.get().toInt()
                var sample = (b2 shl 16) or (b1 shl 8) or b0
                if (sample and 0x800000 != 0) sample = sample or -0x1000000
                mono += sample
            }
            pushSample((mono / channels) / 8388608f)
        }
    }

    private fun ingestPcm32(buffer: ByteBuffer) {
        val channels = channelCount
        val dup = buffer.asReadOnlyBuffer().order(ByteOrder.LITTLE_ENDIAN)
        val frameBytes = 4 * channels
        while (dup.remaining() >= frameBytes) {
            var mono = 0
            for (c in 0 until channels) mono += dup.int
            pushSample((mono / channels) / 2147483648f)
        }
    }

    private fun pushSample(sample: Float) {
        ring[ringWrite] = sample
        ringWrite = (ringWrite + 1) and RING_MASK
        samplesSinceHop++
        if (samplesSinceHop >= HOP_SIZE) {
            samplesSinceHop = 0
            hopsUntilAnalyze++
            if (hopsUntilAnalyze >= analyzeEveryNHops) {
                hopsUntilAnalyze = 0
                analyzeWindow()
            }
        }
    }

    private fun ensureBandBins() {
        val rate = sampleRateHz
        if (binsConfiguredForRate == rate) return
        binsConfiguredForRate = rate
        val nyquist = rate / 2.0
        val usableBins = FFT_SIZE / 2
        // Start lower so kick fundamentals (~50–80 Hz) land in early bands.
        val minHz = 30.0
        val maxHz = min(14_000.0, nyquist * 0.9)
        for (b in 0 until BAND_COUNT) {
            val t0 = b.toDouble() / BAND_COUNT
            val t1 = (b + 1).toDouble() / BAND_COUNT
            val f0 = minHz * (maxHz / minHz).pow(t0)
            val f1 = minHz * (maxHz / minHz).pow(t1)
            val start = ((f0 / nyquist) * usableBins).toInt().coerceIn(1, usableBins - 1)
            val end = ((f1 / nyquist) * usableBins).toInt().coerceIn(start + 1, usableBins)
            bandBinStart[b] = start
            bandBinEnd[b] = end
        }
    }

    private fun analyzeWindow() {
        ensureBandBins()

        var idx = (ringWrite - FFT_SIZE) and RING_MASK
        var rms = 0f
        for (i in 0 until FFT_SIZE) {
            val s = ring[idx]
            re[i] = s * HANN[i]
            im[i] = 0f
            rms += s * s
            idx = (idx + 1) and RING_MASK
        }
        rms = sqrt(rms / FFT_SIZE)

        fftInPlace(re, im)

        val half = FFT_SIZE / 2
        for (k in 0 until half) {
            mag[k] = sqrt(re[k] * re[k] + im[k] * im[k]) / (FFT_SIZE / 2f)
        }

        // Peak per band (not average) — preserves kick/snare spikes.
        val raw = FloatArray(BAND_COUNT)
        var rawPeak = 0f
        var flux = 0f
        for (b in 0 until BAND_COUNT) {
            var peak = 0f
            for (k in bandBinStart[b] until bandBinEnd[b]) {
                val m = mag[k]
                if (m > peak) peak = m
            }
            // Mild high-shelf so hats still move; bass stays dominant for beats.
            val tilt = 0.85f + 0.35f * (b.toFloat() / (BAND_COUNT - 1))
            val v = peak * tilt
            raw[b] = v
            if (v > rawPeak) rawPeak = v
            val delta = v - previousRaw[b]
            if (delta > 0f) flux += delta
            previousRaw[b] = v
        }

        // Onset boost: spectral flux rides on top of steady energy.
        val onset = (flux * 2.4f).coerceIn(0f, 1.2f)

        val signalPeak = max(rawPeak, rms * 2.2f)
        agcPeak = if (signalPeak > agcPeak) {
            agcPeak * 0.82f + signalPeak * 0.18f
        } else {
            agcPeak * 0.998f + signalPeak * 0.002f
        }
        agcPeak = agcPeak.coerceIn(0.02f, 1f)
        val gain = 0.85f / agcPeak

        val out = FloatArray(BAND_COUNT)
        var bassEnergy = 0f
        var energySum = 0f
        for (b in 0 until BAND_COUNT) {
            var scaled = compress(raw[b] * gain)
            // Kick/snare bands get onset punch.
            if (b < BASS_BANDS) {
                scaled = (scaled + onset * 0.45f).coerceIn(0f, 1f)
            } else if (b < BASS_BANDS + 4) {
                scaled = (scaled + onset * 0.2f).coerceIn(0f, 1f)
            }

            // Fast attack / medium release — bass snaps with the beat.
            val attack = if (b < BASS_BANDS) 0.92f else 0.78f
            val release = if (b < BASS_BANDS) 0.45f else 0.55f
            val smoothed = if (scaled >= previousBands[b]) {
                previousBands[b] * (1f - attack) + scaled * attack
            } else {
                previousBands[b] * release + scaled * (1f - release)
            }
            previousBands[b] = smoothed
            out[b] = smoothed
            energySum += smoothed
            if (b < BASS_BANDS) bassEnergy += smoothed
        }

        // Energy leans on bass so pulse/rings hit with the kick.
        val energy = (
            energySum / BAND_COUNT * 0.45f +
                bassEnergy / BASS_BANDS * 0.55f +
                onset * 0.15f
            ).coerceIn(0f, 1f)

        publish(out, energy)
    }

    private fun compress(x: Float): Float {
        val v = x.coerceAtLeast(0f)
        // Lighter curve than before — keeps kick dynamics.
        return (ln(1.0 + v * 10.0) / ln(11.0)).toFloat().coerceIn(0f, 1f)
    }

    private fun fftInPlace(re: FloatArray, im: FloatArray) {
        val n = re.size
        var j = 0
        for (i in 1 until n) {
            var bit = n shr 1
            while (j and bit != 0) {
                j = j xor bit
                bit = bit shr 1
            }
            j = j xor bit
            if (i < j) {
                val tr = re[i]; re[i] = re[j]; re[j] = tr
                val ti = im[i]; im[i] = im[j]; im[j] = ti
            }
        }
        var len = 2
        while (len <= n) {
            val ang = -2.0 * Math.PI / len
            val wlenRe = cos(ang).toFloat()
            val wlenIm = sin(ang).toFloat()
            var i = 0
            while (i < n) {
                var wRe = 1f
                var wIm = 0f
                for (k in 0 until len / 2) {
                    val uRe = re[i + k]
                    val uIm = im[i + k]
                    val vRe = re[i + k + len / 2] * wRe - im[i + k + len / 2] * wIm
                    val vIm = re[i + k + len / 2] * wIm + im[i + k + len / 2] * wRe
                    re[i + k] = uRe + vRe
                    im[i + k] = uIm + vIm
                    re[i + k + len / 2] = uRe - vRe
                    im[i + k + len / 2] = uIm - vIm
                    val nextWRe = wRe * wlenRe - wIm * wlenIm
                    wIm = wRe * wlenIm + wIm * wlenRe
                    wRe = nextWRe
                }
                i += len
            }
            len = len shl 1
        }
    }

    companion object {
        const val BAND_COUNT = 32
        private const val BASS_BANDS = 6
        private const val FFT_SIZE = 512
        /** ~75% overlap → finer onset timing (~172 Hz at 44.1 kHz). */
        private const val HOP_SIZE = 128
        private const val RING_SIZE = 2048
        private const val RING_MASK = RING_SIZE - 1

        /**
         * ExoPlayer DefaultAudioSink holds PCM before the speaker; 350 ms covers
         * typical sink buffering on phone speakers. Bluetooth A2DP/LE codecs add
         * another ~280 ms of transport latency on top.
         */
        private const val BASE_LATENCY_MS = 350
        private const val BLUETOOTH_EXTRA_LATENCY_MS = 280
        private const val DELAY_CAPACITY = 256
        private val ZERO_BANDS = FloatArray(BAND_COUNT)

        private fun delayFramesFor(sampleRateHz: Int, latencyMs: Int): Int {
            val samples = sampleRateHz.toLong() * latencyMs / 1000L
            return (samples / HOP_SIZE).toInt().coerceIn(8, DELAY_CAPACITY - 2)
        }

        private val HANN: FloatArray = FloatArray(FFT_SIZE) { i ->
            (0.5f * (1.0 - cos(2.0 * Math.PI * i / (FFT_SIZE - 1)))).toFloat()
        }
    }
}
