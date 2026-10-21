package com.wl.pacebeat.data

import org.json.JSONArray
import org.json.JSONObject

object PlanCodec {
    fun encode(plan: WorkoutPlan): String = JSONObject().apply {
        put("name", plan.name)
        put("type", plan.type.name)
        put("segments", JSONArray().apply {
            plan.segments.forEach { segment ->
                put(JSONObject().apply {
                    put("id", segment.id)
                    put("name", segment.name)
                    put("duration", segment.durationSec)
                    put("spm", segment.spm)
                })
            }
        })
    }.toString()

    fun decode(raw: String?): WorkoutPlan? {
        if (raw.isNullOrBlank()) return null
        return runCatching {
            val json = JSONObject(raw)
            val array = json.optJSONArray("segments") ?: JSONArray()
            val segments = buildList {
                for (i in 0 until array.length()) {
                    val item = array.optJSONObject(i) ?: continue
                    val name = item.optString("name", "分段").ifBlank { "分段" }
                    val duration = item.optInt("duration", 60).coerceIn(5, 3600)
                    val spm = item.optInt("spm", 160).coerceIn(100, 240)
                    val id = item.optString("id", "").ifBlank { java.util.UUID.randomUUID().toString() }
                    add(Segment(id = id, name = name, durationSec = duration, spm = spm))
                }
            }
            WorkoutPlan(
                name = json.optString("name", "我的计划").ifBlank { "我的计划" },
                type = PlanType.fromName(json.optString("type", PlanType.EASY.name)),
                segments = segments
            )
        }.getOrNull()
    }
}
