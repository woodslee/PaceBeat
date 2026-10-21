package com.wl.pacebeat.service

import com.wl.pacebeat.data.PlayerSettings
import com.wl.pacebeat.data.Segment
import com.wl.pacebeat.data.WorkoutPlan
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class PlaybackState(
    val active: Boolean = false,
    val paused: Boolean = false,
    val completed: Boolean = false,
    val planName: String = "",
    val segments: List<Segment> = emptyList(),
    val index: Int = 0,
    val remainingMs: Long = 0L,
    val bpm: Int = 160,
    val beatIndex: Int = 0,
    val elapsedMs: Long = 0L,
    val actualElapsedMs: Long = 0L,
    val soundId: String = "cc0_mpc",
    val volume: Float = 0.78f,
    val multiplier: Int = 1,
    val voice: Boolean = true,
    val beatsPerBar: Int = 2
) {
    val current: Segment? get() = segments.getOrNull(index)
    val next: Segment? get() = segments.getOrNull(index + 1)
    val totalSeconds: Int get() = segments.sumOf { it.durationSec }
    val progress: Float
        get() {
            if (segments.isEmpty()) return 0f
            val elapsedInSegment = (current?.durationMs ?: 0L) - remainingMs
            val totalElapsed = segments.take(index).sumOf { it.durationMs } + elapsedInSegment
            val total = totalSeconds * 1000L
            return (totalElapsed.toFloat() / total.coerceAtLeast(1L)).coerceIn(0f, 1f)
        }
}

/**
 * Single-process source of truth between the playback service and Compose.
 */
object PlaybackBus {
    private val _state = MutableStateFlow(PlaybackState())
    val state: StateFlow<PlaybackState> = _state.asStateFlow()

    fun prepare(plan: WorkoutPlan, settings: PlayerSettings) {
        val first = plan.segments.firstOrNull()
        _state.value = PlaybackState(
            active = plan.segments.isNotEmpty(),
            paused = false,
            completed = false,
            planName = plan.name,
            segments = plan.segments,
            index = 0,
            remainingMs = first?.durationMs ?: 0L,
            bpm = first?.spm ?: 160,
            beatIndex = 0,
            elapsedMs = 0L,
            actualElapsedMs = 0L,
            soundId = settings.soundId,
            volume = settings.volume,
            multiplier = settings.multiplier,
            voice = settings.voice,
            beatsPerBar = settings.beatsPerBar
        )
    }

    fun update(block: (PlaybackState) -> PlaybackState) {
        _state.value = block(_state.value)
    }

    fun set(state: PlaybackState) {
        _state.value = state
    }

    fun clear() {
        _state.value = PlaybackState()
    }
}
