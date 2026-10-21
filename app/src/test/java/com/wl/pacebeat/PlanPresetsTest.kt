package com.wl.pacebeat

import com.wl.pacebeat.data.PlanPresets
import com.wl.pacebeat.data.PlanType
import com.wl.pacebeat.data.SoundProfile
import com.wl.pacebeat.data.defaultConfig
import com.wl.pacebeat.audio.ClickSynth
import com.wl.pacebeat.audio.ClickPair
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class PlanPresetsTest {
    @Test
    fun everyPresetHasValidSegments() {
        PlanType.entries.filter { it != PlanType.BLANK }.forEach { type ->
            val plan = PlanPresets.build(type, cfg = defaultConfig(type))
            assertTrue("$type should contain segments", plan.segments.isNotEmpty())
            plan.segments.forEach { segment ->
                assertTrue("duration must be positive", segment.durationSec > 0)
                assertTrue("SPM in range", segment.spm in 100..240)
                assertTrue("name must not be blank", segment.name.isNotBlank())
            }
            assertTrue("total duration", plan.totalSeconds > 0)
        }
    }

    @Test
    fun blankPlanIsEmpty() {
        val plan = PlanPresets.build(PlanType.BLANK, "周末 LSD")
        assertEquals("周末 LSD", plan.name)
        assertTrue(plan.segments.isEmpty())
    }

    @Test
    fun clickBarHasExactBeatLength() {
        val bar = ClickSynth.buildBar(bpm = 180, profile = SoundProfile.CC0_MPC, boost = 1)
        val expectedFramesPerBeat = (ClickSynth.SAMPLE_RATE * 60.0 / 180).toInt()
        assertEquals(expectedFramesPerBeat * 4, bar.size)
        assertTrue(bar.any { it.toInt() != 0 })
    }

    @Test
    fun allClickSamplesArePresent() {
        val directory = File("src/main/assets/sounds")
        assertTrue("sample directory must exist", directory.isDirectory)
        SoundProfile.entries.forEach { profile ->
            listOf("accent", "normal").forEach { suffix ->
                val file = File(directory, "${profile.id}_$suffix.pcm")
                assertTrue("missing $file", file.isFile)
                assertTrue("empty $file", file.length() > 200L)
                assertEquals("PCM must be 16-bit", 0L, file.length() % 2L)
            }
        }
    }

    @Test
    fun sampleBasedBarHasExactBeatLength() {
        val pair = ClickPair(
            accent = ShortArray(600) { 12_000 },
            normal = ShortArray(600) { 8_000 }
        )
        val bar = ClickSynth.buildBar(bpm = 180, pair = pair, boost = 1)
        assertEquals((ClickSynth.SAMPLE_RATE * 60.0 / 180).toInt() * 4, bar.size)
        assertTrue(bar.any { it.toInt() != 0 })
    }

    @Test
    fun barLengthFollowsBeatsPerBar() {
        val pair = ClickPair(
            accent = ShortArray(400) { 10_000 },
            normal = ShortArray(400) { 6_000 }
        )
        val framesPerBeat = (ClickSynth.SAMPLE_RATE * 60.0 / 180).toInt()
        for (beats in 2..4) {
            val bar = ClickSynth.buildBar(bpm = 180, pair = pair, boost = 1, beatsPerBar = beats)
            assertEquals(framesPerBeat * beats, bar.size)
        }
    }

    @Test
    fun barEndIsFadedForSeamlessLoop() {
        val pair = ClickPair(
            accent = ShortArray(8_000) { 20_000 },
            normal = ShortArray(8_000) { 14_000 }
        )
        val bar = ClickSynth.buildBar(bpm = 180, pair = pair, boost = 4, beatsPerBar = 3)
        assertTrue("loop boundary should be near silence", kotlin.math.abs(bar.last().toInt()) < 200)
    }

    @Test
    fun allSoundProfilesProduceAudio() {
        SoundProfile.entries.forEach { profile ->
            val click = ClickSynth.renderClick(profile, accent = true, gain = 0.3)
            assertTrue(click.isNotEmpty())
            assertTrue("$profile must produce sound", click.any { it.toInt() != 0 })
        }
    }
}
