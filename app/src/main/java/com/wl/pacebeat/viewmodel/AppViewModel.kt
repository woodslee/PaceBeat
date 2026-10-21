package com.wl.pacebeat.viewmodel

import android.app.Application
import android.content.Context
import android.content.Intent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import com.wl.pacebeat.data.AppPrefs
import com.wl.pacebeat.data.PlanCodec
import com.wl.pacebeat.data.PlanConfig
import com.wl.pacebeat.data.PlanPresets
import com.wl.pacebeat.data.PlanType
import com.wl.pacebeat.data.PlayerSettings
import com.wl.pacebeat.data.Segment
import com.wl.pacebeat.data.SoundProfile
import com.wl.pacebeat.data.WorkoutPlan
import com.wl.pacebeat.service.MetronomeService
import com.wl.pacebeat.service.PlaybackBus

enum class AppScreen {
    HOME,
    RUN
}

class AppViewModel(application: Application) : AndroidViewModel(application) {
    private val prefs = AppPrefs(application)

    var plan by mutableStateOf(prefs.loadPlan())
        private set
    var settings by mutableStateOf(prefs.loadSettings())
        private set
    var screen by mutableStateOf(AppScreen.HOME)
        private set

    fun startPlayback(context: Context) {
        if (!plan.hasContent) return
        PlaybackBus.prepare(plan, settings)
        val intent = Intent(context, MetronomeService::class.java).apply {
            action = MetronomeService.ACTION_START
            putExtra(MetronomeService.EXTRA_PLAN, PlanCodec.encode(plan))
            putExtra(MetronomeService.EXTRA_SOUND, settings.soundId)
            putExtra(MetronomeService.EXTRA_VOLUME, settings.volume)
            putExtra(MetronomeService.EXTRA_BOOST, settings.multiplier)
            putExtra(MetronomeService.EXTRA_VOICE, settings.voice)
            putExtra(MetronomeService.EXTRA_BEATS, settings.beatsPerBar)
        }
        ContextCompat.startForegroundService(context, intent)
        screen = AppScreen.RUN
    }

    fun pause(context: Context) = send(context, MetronomeService.ACTION_PAUSE)
    fun resume(context: Context) = send(context, MetronomeService.ACTION_RESUME)
    fun next(context: Context) = send(context, MetronomeService.ACTION_NEXT)
    fun reset(context: Context) = send(context, MetronomeService.ACTION_RESET)

    fun stop(context: Context) {
        send(context, MetronomeService.ACTION_STOP)
        PlaybackBus.clear()
        screen = AppScreen.HOME
    }

    fun dismissCompleted() {
        PlaybackBus.clear()
        screen = AppScreen.HOME
    }

    fun openRun() {
        screen = AppScreen.RUN
    }

    fun backToHome() {
        screen = AppScreen.HOME
    }

    fun applyPlan(newPlan: WorkoutPlan) {
        plan = newPlan
        prefs.savePlan(newPlan)
    }

    fun applyPreset(type: PlanType, config: PlanConfig, name: String? = null) {
        applyPlan(PlanPresets.build(type, name, config))
    }

    fun addSegment(segment: Segment) {
        applyPlan(plan.copy(segments = plan.segments + segment))
    }

    fun updateSegment(segment: Segment) {
        applyPlan(plan.copy(segments = plan.segments.map { if (it.id == segment.id) segment else it }))
    }

    fun deleteSegment(segment: Segment) {
        applyPlan(plan.copy(segments = plan.segments.filterNot { it.id == segment.id }))
    }

    fun moveSegment(from: Int, to: Int) {
        val segments = plan.segments
        if (from == to || from !in segments.indices || to !in segments.indices) return
        val reordered = segments.toMutableList()
        val moved = reordered.removeAt(from)
        reordered.add(to, moved)
        applyPlan(plan.copy(segments = reordered))
    }

    fun setVolume(context: Context, value: Float) {
        updateSettings(settings.copy(volume = value.coerceIn(0f, 1f)))
        send(context, MetronomeService.ACTION_SET_VOLUME) {
            putExtra(MetronomeService.EXTRA_VOLUME, value.coerceIn(0f, 1f))
        }
    }

    fun setSound(context: Context, sound: SoundProfile) {
        updateSettings(settings.copy(soundId = sound.id))
        send(context, MetronomeService.ACTION_SET_SOUND) {
            putExtra(MetronomeService.EXTRA_SOUND, sound.id)
        }
    }

    fun setMultiplier(context: Context, multiplier: Int) {
        val safe = multiplier.coerceIn(1, 4)
        updateSettings(settings.copy(multiplier = safe))
        send(context, MetronomeService.ACTION_SET_BOOST) {
            putExtra(MetronomeService.EXTRA_BOOST, safe)
        }
    }

    fun setVoice(context: Context, enabled: Boolean) {
        updateSettings(settings.copy(voice = enabled))
        send(context, MetronomeService.ACTION_SET_VOICE) {
            putExtra(MetronomeService.EXTRA_VOICE, enabled)
        }
    }

    fun setBeatsPerBar(context: Context, beats: Int) {
        val safe = beats.coerceIn(2, 4)
        updateSettings(settings.copy(beatsPerBar = safe))
        send(context, MetronomeService.ACTION_SET_BEATS) {
            putExtra(MetronomeService.EXTRA_BEATS, safe)
        }
    }

    fun setBpm(context: Context, bpm: Int) {
        val safe = bpm.coerceIn(100, 240)
        val current = PlaybackBus.state.value
        val segment = current.current ?: return
        val updatedSegment = segment.copy(spm = safe)
        plan = plan.copy(
            segments = plan.segments.map { if (it.id == segment.id) updatedSegment else it }
        )
        prefs.savePlan(plan)
        send(context, MetronomeService.ACTION_SET_BPM) {
            putExtra(MetronomeService.EXTRA_BPM, safe)
        }
    }

    private fun updateSettings(newSettings: PlayerSettings) {
        settings = newSettings
        prefs.saveSettings(newSettings)
    }

    private fun send(context: Context, action: String, extras: Intent.() -> Unit = {}) {
        val state = PlaybackBus.state.value
        if (!state.active || state.completed) return
        val intent = Intent(context, MetronomeService::class.java).apply {
            this.action = action
            extras()
        }
        runCatching { context.startService(intent) }
    }
}
