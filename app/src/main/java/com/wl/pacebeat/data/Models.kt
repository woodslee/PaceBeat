package com.wl.pacebeat.data

import java.util.UUID
import kotlin.math.ceil

enum class PlanType(
    val label: String,
    val subtitle: String,
    val color: Long
) {
    BLANK("空白计划", "自由添加分段", 0xFF8E8E93),
    EASY("轻松跑", "每日恢复", 0xFF35C77B),
    AEROBIC("有氧跑", "基础耐力", 0xFF3D9DFF),
    PROGRESSIVE("渐进跑", "阶梯提速", 0xFFA36BFF),
    TEMPO("节奏跑", "阈值配速", 0xFFFF5D73),
    INTERVALS("间歇跑", "突破极限", 0xFFFF9B0A),
    PYRAMID("金字塔", "升降节奏", 0xFF6C63FF),
    FARTLEK("法特莱克", "随机变速", 0xFF1FC7D4);

    companion object {
        fun fromName(name: String?): PlanType = entries.firstOrNull { it.name == name } ?: AEROBIC
    }
}

data class Segment(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val durationSec: Int,
    val spm: Int
) {
    val durationMs: Long get() = durationSec * 1000L
    val durationLabel: String get() = formatClock(durationSec)
}

data class WorkoutPlan(
    val name: String,
    val type: PlanType,
    val segments: List<Segment>
) {
    val totalSeconds: Int get() = segments.sumOf { it.durationSec }
    val totalLabel: String get() = formatTotal(totalSeconds)
    val hasContent: Boolean get() = segments.isNotEmpty()
}

data class PlayerSettings(
    val soundId: String = SoundProfile.CC0_MPC.id,
    val volume: Float = 0.78f,
    val multiplier: Int = 1,
    val voice: Boolean = true,
    val beatsPerBar: Int = 2
) {
    val sound: SoundProfile get() = SoundProfile.fromId(soundId)
}

/** Parameters shown in the preset configuration sheet. */
data class PlanConfig(
    val mainMinutes: Int = 25,
    val spm: Int = 170,
    val peakSpm: Int = 185,
    val warmup: Boolean = true,
    val cooldown: Boolean = true,
    val repeats: Int = 4,
    val fastSeconds: Int = 60,
    val recoverySeconds: Int = 120
)

fun defaultConfig(type: PlanType): PlanConfig = when (type) {
    PlanType.BLANK -> PlanConfig(mainMinutes = 20, spm = 165, peakSpm = 180, warmup = false, cooldown = false)
    PlanType.EASY -> PlanConfig(mainMinutes = 20, spm = 160, peakSpm = 170, warmup = true, cooldown = true)
    PlanType.AEROBIC -> PlanConfig(mainMinutes = 25, spm = 170, peakSpm = 180, warmup = true, cooldown = true)
    PlanType.PROGRESSIVE -> PlanConfig(mainMinutes = 24, spm = 160, peakSpm = 180, warmup = true, cooldown = true)
    PlanType.TEMPO -> PlanConfig(mainMinutes = 20, spm = 175, peakSpm = 180, warmup = true, cooldown = true)
    PlanType.INTERVALS -> PlanConfig(mainMinutes = 20, spm = 150, peakSpm = 185, warmup = true, cooldown = true, repeats = 4, fastSeconds = 60, recoverySeconds = 120)
    PlanType.PYRAMID -> PlanConfig(mainMinutes = 20, spm = 165, peakSpm = 185, warmup = true, cooldown = true)
    PlanType.FARTLEK -> PlanConfig(mainMinutes = 25, spm = 165, peakSpm = 185, warmup = true, cooldown = true)
}

fun formatClock(totalSeconds: Int): String {
    val safe = totalSeconds.coerceAtLeast(0)
    val hours = safe / 3600
    val minutes = (safe % 3600) / 60
    val seconds = safe % 60
    return if (hours > 0) {
        "%d:%02d:%02d".format(hours, minutes, seconds)
    } else {
        "%02d:%02d".format(minutes, seconds)
    }
}

fun formatTotal(totalSeconds: Int): String {
    val safe = totalSeconds.coerceAtLeast(0)
    val hours = safe / 3600
    val minutes = (safe % 3600) / 60
    val seconds = safe % 60
    return if (hours > 0) {
        "%d:%02d:%02d".format(hours, minutes, seconds)
    } else {
        "%02d:%02d".format(minutes, seconds)
    }
}

fun formatRemaining(remainingMs: Long): String {
    val totalSeconds = ceil(remainingMs.coerceAtLeast(0L) / 1000.0).toInt()
    return formatClock(totalSeconds)
}

fun clampSpm(value: Int): Int = value.coerceIn(100, 240)
