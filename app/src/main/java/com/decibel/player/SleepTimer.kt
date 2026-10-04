package com.decibel.player

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

enum class SleepTimerOption(val label: String, val durationMs: Long?) {
    Off("Off", null),
    Min15("15 min", 15 * 60_000L),
    Min30("30 min", 30 * 60_000L),
    Min45("45 min", 45 * 60_000L),
    Min60("60 min", 60 * 60_000L),
    Min90("90 min", 90 * 60_000L),
}

data class SleepTimerState(
    val option: SleepTimerOption = SleepTimerOption.Off,
    /** Milliseconds left while a timer is running; 0 when off. */
    val remainingMs: Long = 0L,
    /**
     * Set when the timer fires so the UI can drop keep-screen-on and let
     * the device sleep until the user plays again or starts a new timer.
     */
    val allowDeviceSleep: Boolean = false,
) {
    val isActive: Boolean
        get() = option != SleepTimerOption.Off && remainingMs > 0L
}

/**
 * Counts down and invokes [onFire] (pause playback) when time is up.
 */
class SleepTimer(
    private val scope: CoroutineScope,
    private val onFire: () -> Unit,
) {
    private val _state = MutableStateFlow(SleepTimerState())
    val state: StateFlow<SleepTimerState> = _state.asStateFlow()

    private var job: Job? = null

    fun setOption(option: SleepTimerOption) {
        job?.cancel()
        job = null
        val duration = option.durationMs
        if (option == SleepTimerOption.Off || duration == null) {
            _state.value = SleepTimerState()
            return
        }
        _state.value = SleepTimerState(
            option = option,
            remainingMs = duration,
            allowDeviceSleep = false,
        )
        job = scope.launch {
            var left = duration
            while (isActive && left > 0L) {
                val step = minOf(1_000L, left)
                delay(step)
                left -= step
                _state.value = _state.value.copy(remainingMs = left.coerceAtLeast(0L))
            }
            if (isActive) {
                _state.value = SleepTimerState(
                    option = SleepTimerOption.Off,
                    remainingMs = 0L,
                    allowDeviceSleep = true,
                )
                onFire()
            }
        }
    }

    /** Clear the “allow sleep” latch after the user resumes interaction. */
    fun clearAllowDeviceSleep() {
        if (_state.value.allowDeviceSleep) {
            _state.value = _state.value.copy(allowDeviceSleep = false)
        }
    }

    fun cancel() = setOption(SleepTimerOption.Off)
}

fun formatSleepRemaining(ms: Long): String {
    val totalSec = ((ms + 999) / 1000).toInt().coerceAtLeast(0)
    val m = totalSec / 60
    val s = totalSec % 60
    return "%d:%02d".format(m, s)
}
