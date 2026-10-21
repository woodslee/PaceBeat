package com.wl.pacebeat.data

import kotlin.math.max
import kotlin.math.roundToInt

object PlanPresets {

    fun build(type: PlanType, name: String? = null, cfg: PlanConfig = defaultConfig(type)): WorkoutPlan {
        if (type == PlanType.BLANK) {
            return WorkoutPlan(name = name?.ifBlank { "新的计划" } ?: "新的计划", type = type, segments = emptyList())
        }

        val segments = mutableListOf<Segment>()
        val base = clampSpm(cfg.spm)
        val peak = clampSpm(max(cfg.peakSpm, base))
        val warm = clampSpm(base - 10)
        if (cfg.warmup) {
            segments += Segment(name = "热身", durationSec = 5 * 60, spm = warm)
        }

        when (type) {
            PlanType.EASY -> {
                segments += Segment(name = "轻松跑", durationSec = max(5, cfg.mainMinutes) * 60, spm = base)
            }

            PlanType.AEROBIC -> {
                segments += Segment(name = "有氧巡航", durationSec = max(5, cfg.mainMinutes) * 60, spm = base)
            }

            PlanType.PROGRESSIVE -> {
                val total = max(9, cfg.mainMinutes) * 60
                val part = max(60, total / 3)
                val middle = clampSpm((base + peak) / 2)
                segments += Segment(name = "渐进 · 第一段", durationSec = part, spm = base)
                segments += Segment(name = "渐进 · 第二段", durationSec = part, spm = middle)
                segments += Segment(name = "渐进 · 第三段", durationSec = max(60, total - part * 2), spm = peak)
            }

            PlanType.TEMPO -> {
                val total = max(8, cfg.mainMinutes) * 60
                val first = max(180, (total * 0.7).roundToInt())
                val second = max(120, total - first)
                segments += Segment(name = "节奏巡航", durationSec = first, spm = base)
                segments += Segment(name = "恢复慢跑", durationSec = 3 * 60, spm = clampSpm(base - 20))
                segments += Segment(name = "节奏冲刺", durationSec = second, spm = clampSpm(base + 5))
            }

            PlanType.INTERVALS -> {
                val repeats = cfg.repeats.coerceIn(2, 10)
                repeat(repeats) { index ->
                    segments += Segment(
                        name = "间歇冲刺 ${index + 1}/$repeats",
                        durationSec = cfg.fastSeconds.coerceIn(20, 600),
                        spm = peak
                    )
                    segments += Segment(
                        name = "恢复慢跑 ${index + 1}/$repeats",
                        durationSec = cfg.recoverySeconds.coerceIn(20, 600),
                        spm = base
                    )
                }
            }

            PlanType.PYRAMID -> {
                val unit = max(30, (max(5, cfg.mainMinutes) * 60) / 9)
                val steps = listOf(
                    Triple("金字塔 · 起步", unit, base),
                    Triple("金字塔 · 加速", unit * 2, clampSpm(base + 8)),
                    Triple("金字塔 · 顶点", unit * 3, peak),
                    Triple("金字塔 · 回落", unit * 2, clampSpm(base + 8)),
                    Triple("金字塔 · 收尾", unit, base)
                )
                steps.forEach { (n, d, s) -> segments += Segment(name = n, durationSec = d, spm = s) }
            }

            PlanType.FARTLEK -> {
                val total = max(10, cfg.mainMinutes) * 60
                val weights = listOf(3, 2, 3, 2, 3)
                val unit = max(30, total / weights.sum())
                val names = listOf("自由加速", "轻松巡航", "快速摆动", "稳定呼吸", "最后加速")
                val speeds = listOf(base, clampSpm(base - 8), peak, base, clampSpm(peak - 5))
                names.forEachIndexed { i, n ->
                    segments += Segment(name = n, durationSec = unit * weights[i], spm = speeds[i])
                }
            }

            PlanType.BLANK -> Unit
        }

        if (cfg.cooldown) {
            segments += Segment(name = "冷身", durationSec = 5 * 60, spm = warm)
        }

        val planName = name?.ifBlank { null } ?: when (type) {
            PlanType.EASY -> "今日轻松跑"
            PlanType.AEROBIC -> "今日有氧跑"
            PlanType.PROGRESSIVE -> "渐进提速"
            PlanType.TEMPO -> "节奏巡航"
            PlanType.INTERVALS -> "间歇挑战"
            PlanType.PYRAMID -> "金字塔训练"
            PlanType.FARTLEK -> "法特莱克"
            PlanType.BLANK -> "新的计划"
        }
        return WorkoutPlan(name = planName, type = type, segments = segments)
    }

    /** A compact preview used in the plan configuration sheet. */
    fun summary(type: PlanType, cfg: PlanConfig): String {
        val plan = build(type, cfg = cfg)
        if (plan.segments.isEmpty()) return "空白计划"
        if (plan.segments.size <= 4) {
            return plan.segments.joinToString("  +  ") { "${it.name} ${formatClock(it.durationSec)}" }
        }
        val head = plan.segments.take(3).joinToString("  +  ") { "${it.name} ${formatClock(it.durationSec)}" }
        return "$head  …  共 ${plan.segments.size} 段，合计 ${plan.totalLabel}"
    }

    fun totalSeconds(type: PlanType, cfg: PlanConfig): Int = build(type, cfg = cfg).totalSeconds
}
