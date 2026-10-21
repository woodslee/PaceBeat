package com.wl.pacebeat.data

import android.content.Context

class AppPrefs(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences("pacebeat_prefs", Context.MODE_PRIVATE)

    fun loadPlan(): WorkoutPlan {
        val decoded = PlanCodec.decode(prefs.getString(KEY_PLAN, null))
        return decoded ?: PlanPresets.build(
            type = PlanType.AEROBIC,
            name = "今日有氧",
            cfg = defaultConfig(PlanType.AEROBIC)
        )
    }

    fun savePlan(plan: WorkoutPlan) {
        prefs.edit().putString(KEY_PLAN, PlanCodec.encode(plan)).apply()
    }

    fun loadSettings(): PlayerSettings {
        return PlayerSettings(
            soundId = SoundProfile.fromId(prefs.getString(KEY_SOUND, null)).id,
            volume = prefs.getFloat(KEY_VOLUME, 0.78f).coerceIn(0f, 1f),
            multiplier = prefs.getInt(KEY_MULTIPLIER, 1).coerceIn(1, 4),
            voice = prefs.getBoolean(KEY_VOICE, true),
            beatsPerBar = prefs.getInt(KEY_BEATS_PER_BAR, 2).coerceIn(2, 4)
        )
    }

    fun saveSettings(settings: PlayerSettings) {
        prefs.edit()
            .putString(KEY_SOUND, settings.soundId)
            .putFloat(KEY_VOLUME, settings.volume.coerceIn(0f, 1f))
            .putInt(KEY_MULTIPLIER, settings.multiplier.coerceIn(1, 4))
            .putBoolean(KEY_VOICE, settings.voice)
            .putInt(KEY_BEATS_PER_BAR, settings.beatsPerBar.coerceIn(2, 4))
            .apply()
    }

    fun isIntroSeen(): Boolean = prefs.getBoolean(KEY_INTRO, false)

    fun markIntroSeen() {
        prefs.edit().putBoolean(KEY_INTRO, true).apply()
    }

    private companion object {
        const val KEY_PLAN = "plan"
        const val KEY_SOUND = "sound"
        const val KEY_VOLUME = "volume"
        const val KEY_MULTIPLIER = "multiplier"
        const val KEY_VOICE = "voice"
        const val KEY_BEATS_PER_BAR = "beats_per_bar"
        const val KEY_INTRO = "intro"
    }
}
