package com.decibel.player

import android.content.Context
import android.media.audiofx.Equalizer
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.roundToInt

data class EqBand(
    val index: Int,
    val centerHz: Int,
    val levelMb: Int,
    val minLevelMb: Int,
    val maxLevelMb: Int,
)

data class EqUiState(
    val available: Boolean = false,
    val enabled: Boolean = false,
    val presetId: String = EqPrefs.PRESET_FLAT,
    val bands: List<EqBand> = emptyList(),
)

class EqController(context: Context) {
    private val prefs = EqPrefs(context.applicationContext)
    private var equalizer: Equalizer? = null
    private var attachedSessionId: Int = -1

    private val _state = MutableStateFlow(
        EqUiState(
            enabled = prefs.enabled,
            presetId = prefs.presetId,
        ),
    )
    val state: StateFlow<EqUiState> = _state.asStateFlow()

    /**
     * Attach (or re-attach) to an ExoPlayer audio session.
     * [force] recreates the platform Equalizer even if the session id is unchanged —
     * needed after notification / Dolby focus glitches where the effect goes stale.
     */
    fun attach(audioSessionId: Int, force: Boolean = false) {
        if (audioSessionId < 0) return
        if (!force && audioSessionId == attachedSessionId && equalizer != null) {
            applyCurrentSettings()
            return
        }
        bindSession(audioSessionId)
    }

    /** Re-create Equalizer on the current session after focus / OEM DSP disruption. */
    fun rebind() {
        val session = attachedSessionId
        if (session < 0) return
        bindSession(session)
    }

    fun setEnabled(enabled: Boolean) {
        prefs.enabled = enabled
        if (equalizer == null) {
            _state.value = _state.value.copy(enabled = enabled, available = false)
            return
        }
        // Full rebind on enable so bands commit after OEM/Dolby disruption.
        if (enabled) {
            rebind()
        } else {
            runCatching { equalizer?.enabled = false }
            _state.value = _state.value.copy(enabled = false, available = true)
        }
    }

    fun applyPreset(presetId: String) {
        val eq = equalizer ?: run {
            prefs.presetId = presetId
            _state.value = _state.value.copy(presetId = presetId)
            return
        }
        val levels = if (presetId == EqPrefs.PRESET_CUSTOM) {
            remapBands(prefs.bandLevelsMb, eq.numberOfBands.toInt())
        } else {
            curveFor(presetId, eq.numberOfBands.toInt(), eq.bandLevelRange)
        }
        prefs.presetId = presetId
        prefs.bandLevelsMb = levels
        if (prefs.enabled) {
            // Recreate then write — more reliable than setBandLevel on a stale effect.
            rebind()
        } else {
            applyLevels(eq, levels)
            publish(eq, levels)
        }
    }

    fun setBandLevel(index: Int, levelMb: Int) {
        val eq = equalizer ?: return
        val bandCount = eq.numberOfBands.toInt()
        if (index !in 0 until bandCount) return
        val range = eq.bandLevelRange
        val clamped = levelMb.coerceIn(range[0].toInt(), range[1].toInt())
        if (prefs.enabled) {
            runCatching { eq.enabled = true }
        }
        val ok = runCatching { eq.setBandLevel(index.toShort(), clamped.toShort()) }.isSuccess
        if (!ok && prefs.enabled) {
            prefs.presetId = EqPrefs.PRESET_CUSTOM
            val levels = IntArray(bandCount) { i ->
                if (i == index) clamped else prefs.bandLevelsMb.getOrElse(i) { 0 }
            }
            prefs.bandLevelsMb = levels
            rebind()
            return
        }
        val levels = IntArray(bandCount) { i ->
            if (i == index) clamped else eq.getBandLevel(i.toShort()).toInt()
        }
        prefs.presetId = EqPrefs.PRESET_CUSTOM
        prefs.bandLevelsMb = levels
        publish(eq, levels)
    }

    fun release() {
        releaseEqualizer()
        attachedSessionId = -1
    }

    private fun bindSession(audioSessionId: Int) {
        releaseEqualizer()
        attachedSessionId = audioSessionId
        val eq = runCatching {
            Equalizer(/* priority */ 0, audioSessionId)
        }.onFailure {
            Log.w(TAG, "Equalizer unavailable for session $audioSessionId", it)
        }.getOrNull()
        if (eq == null) {
            _state.value = EqUiState(available = false, enabled = prefs.enabled, presetId = prefs.presetId)
            return
        }
        equalizer = eq
        runCatching { eq.enabled = prefs.enabled }
        val levels = resolveLevels(eq)
        applyLevels(eq, levels)
        // Some OEMs need a second enable pass after bands are written.
        if (prefs.enabled) {
            runCatching {
                eq.enabled = false
                eq.enabled = true
                applyLevels(eq, levels)
            }
        }
        publish(eq, levels)
    }

    private fun applyCurrentSettings() {
        val eq = equalizer ?: return
        runCatching { eq.enabled = prefs.enabled }
        val levels = resolveLevels(eq)
        applyLevels(eq, levels)
        publish(eq, levels)
    }

    private fun resolveLevels(eq: Equalizer): IntArray {
        val bandCount = eq.numberOfBands.toInt()
        return when (prefs.presetId) {
            EqPrefs.PRESET_CUSTOM -> remapBands(prefs.bandLevelsMb, bandCount)
            else -> curveFor(prefs.presetId, bandCount, eq.bandLevelRange).also {
                prefs.bandLevelsMb = it
            }
        }
    }

    private fun applyLevels(eq: Equalizer, levels: IntArray) {
        val range = eq.bandLevelRange
        levels.forEachIndexed { index, level ->
            val clamped = level.coerceIn(range[0].toInt(), range[1].toInt())
            runCatching { eq.setBandLevel(index.toShort(), clamped.toShort()) }
        }
    }

    private fun publish(eq: Equalizer, levels: IntArray) {
        val range = eq.bandLevelRange
        val min = range[0].toInt()
        val max = range[1].toInt()
        val bands = List(eq.numberOfBands.toInt()) { index ->
            EqBand(
                index = index,
                centerHz = eq.getCenterFreq(index.toShort()) / 1000,
                levelMb = levels.getOrElse(index) { eq.getBandLevel(index.toShort()).toInt() },
                minLevelMb = min,
                maxLevelMb = max,
            )
        }
        _state.value = EqUiState(
            available = true,
            enabled = prefs.enabled,
            presetId = prefs.presetId,
            bands = bands,
        )
    }

    private fun releaseEqualizer() {
        runCatching { equalizer?.enabled = false }
        runCatching { equalizer?.release() }
        equalizer = null
        // Keep attachedSessionId so [rebind] can recreate on the same session.
    }

    companion object {
        private const val TAG = "EqController"

        /** Keep boosts moderate — full-range + Dolby/Atmos often clips or glitches. */
        private const val MaxBoostFraction = 0.55f

        fun curveFor(presetId: String, bandCount: Int, range: ShortArray): IntArray {
            if (bandCount <= 0) return intArrayOf()
            val maxBoost = (range[1].toInt().coerceAtLeast(0) * MaxBoostFraction).roundToInt()
            val maxCut = (kotlin.math.abs(range[0].toInt()).coerceAtLeast(0) * MaxBoostFraction * 0.45f).roundToInt()
            fun t(i: Int) = i.toFloat() / (bandCount - 1).coerceAtLeast(1)
            fun midPeak(i: Int, width: Float = 1f): Float {
                val mid = (bandCount - 1) / 2f
                val dist = kotlin.math.abs(i - mid) / (mid.coerceAtLeast(1f) * width)
                return (1f - dist).coerceIn(0f, 1f)
            }
            val shape = when (presetId) {
                EqPrefs.PRESET_BASS -> FloatArray(bandCount) { i -> 1f - t(i) }
                EqPrefs.PRESET_TREBLE -> FloatArray(bandCount) { i -> t(i) }
                EqPrefs.PRESET_VOCAL -> FloatArray(bandCount) { i -> midPeak(i) * 0.9f }
                EqPrefs.PRESET_ROCK -> FloatArray(bandCount) { i ->
                    // Smile: bass + treble up, slight mid dip
                    val smile = kotlin.math.abs(t(i) - 0.5f) * 2f
                    smile * 0.95f - midPeak(i, 0.7f) * 0.15f
                }
                EqPrefs.PRESET_POP -> FloatArray(bandCount) { i ->
                    // Mild smile + soft mid presence
                    val smile = kotlin.math.abs(t(i) - 0.5f) * 2f
                    smile * 0.55f + midPeak(i) * 0.35f
                }
                EqPrefs.PRESET_ELECTRONIC -> FloatArray(bandCount) { i ->
                    // Heavy low, light high, scooped mids
                    val bass = (1f - t(i)).coerceIn(0f, 1f).let { it * it }
                    val treble = t(i).let { it * it } * 0.55f
                    bass + treble - midPeak(i, 0.85f) * 0.2f
                }
                EqPrefs.PRESET_HIP_HOP -> FloatArray(bandCount) { i ->
                    // Strong sub/bass, restrained highs
                    val bass = (1f - t(i)).coerceIn(0f, 1f)
                    bass * bass * 1.05f + t(i) * 0.2f - midPeak(i, 0.6f) * 0.12f
                }
                EqPrefs.PRESET_JAZZ -> FloatArray(bandCount) { i ->
                    // Warm mids, gentle bass, soft air
                    midPeak(i, 1.15f) * 0.75f + (1f - t(i)) * 0.25f + t(i) * 0.2f
                }
                EqPrefs.PRESET_CLASSICAL -> FloatArray(bandCount) { i ->
                    // Smooth lift toward highs, light bass
                    t(i) * 0.7f + midPeak(i) * 0.35f + (1f - t(i)) * 0.15f
                }
                EqPrefs.PRESET_ACOUSTIC -> FloatArray(bandCount) { i ->
                    // Natural mids, mild extremes
                    midPeak(i, 1.1f) * 0.85f + kotlin.math.abs(t(i) - 0.5f) * 0.25f
                }
                EqPrefs.PRESET_LOUDNESS -> FloatArray(bandCount) { i ->
                    // Fletcher-Munson-ish smile at low volume
                    val smile = kotlin.math.abs(t(i) - 0.5f) * 2f
                    smile * smile * 0.95f + midPeak(i) * 0.1f
                }
                else -> FloatArray(bandCount) { 0f }
            }
            return IntArray(bandCount) { i ->
                val v = shape[i]
                when {
                    v >= 0f -> (v * maxBoost).roundToInt()
                    else -> (-v * maxCut).roundToInt().unaryMinus()
                }
            }
        }

        fun remapBands(saved: IntArray, bandCount: Int): IntArray {
            if (bandCount <= 0) return intArrayOf()
            if (saved.isEmpty()) return IntArray(bandCount) { 0 }
            if (saved.size == bandCount) return saved.copyOf()
            return IntArray(bandCount) { i ->
                val src = i.toFloat() * (saved.lastIndex) / (bandCount - 1).coerceAtLeast(1)
                val lo = src.toInt().coerceIn(0, saved.lastIndex)
                val hi = (lo + 1).coerceAtMost(saved.lastIndex)
                val frac = src - lo
                (saved[lo] * (1 - frac) + saved[hi] * frac).roundToInt()
            }
        }
    }
}
