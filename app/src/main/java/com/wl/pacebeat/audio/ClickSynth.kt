package com.wl.pacebeat.audio

import com.wl.pacebeat.data.SoundProfile
import com.wl.pacebeat.data.Timbre
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.random.Random

/**
 * Generates a complete four-beat bar as 16-bit mono PCM.
 *
 * The generated bar is looped by an [android.media.AudioTrack] in static mode,
 * so beat spacing is sample-accurate and does not depend on coroutine timers.
 */
object ClickSynth {
    const val SAMPLE_RATE = 44_100
    const val BEATS_PER_BAR = 4

    fun buildBar(
        bpm: Int,
        profile: SoundProfile,
        boost: Int,
        beatsPerBar: Int = BEATS_PER_BAR
    ): ShortArray {
        val gain = (0.28 * boost.coerceIn(1, 4)).coerceAtMost(0.96)
        val accent = renderClick(profile, accent = true, gain = gain)
        val normal = renderClick(profile, accent = false, gain = gain)
        return mixBar(bpm, accent, normal, gain = 1.0, beatsPerBar = beatsPerBar)
    }

    /** Builds a bar from one-shot SF2 samples, with headroom for the 1x–4x boost. */
    fun buildBar(
        bpm: Int,
        pair: ClickPair,
        boost: Int,
        beatsPerBar: Int = BEATS_PER_BAR
    ): ShortArray {
        val gain = (0.35 * boost.coerceIn(1, 4)).coerceAtMost(0.98)
        return mixBar(bpm, pair.accent, pair.normal, gain, beatsPerBar)
    }

    private fun mixBar(
        bpm: Int,
        accent: ShortArray,
        normal: ShortArray,
        gain: Double,
        beatsPerBar: Int
    ): ShortArray {
        val safeBpm = bpm.coerceIn(40, 300)
        val beats = beatsPerBar.coerceIn(1, 8)
        val framesPerBeat = (SAMPLE_RATE * 60.0 / safeBpm).roundToInt().coerceAtLeast(512)
        val totalFrames = framesPerBeat * beats
        val out = ShortArray(totalFrames)
        for (beat in 0 until beats) {
            val source = if (beat == 0) accent else normal
            val start = beat * framesPerBeat
            val end = (start + source.size).coerceAtMost(totalFrames)
            for (i in start until end) {
                val mixed = out[i].toInt() + (source[i - start] * gain).roundToInt()
                out[i] = mixed.coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
            }
        }
        // The SF2 release tail can extend past the last beat. Fade the very end
        // of the loop so the static AudioTrack wrap is click-free.
        val fadeFrames = (SAMPLE_RATE * 0.012).roundToInt().coerceAtMost(totalFrames / 4)
        for (i in 0 until fadeFrames) {
            val index = totalFrames - 1 - i
            out[index] = (out[index] * (i.toDouble() / fadeFrames)).roundToInt().toShort()
        }
        return out
    }

    fun renderClick(profile: SoundProfile, accent: Boolean, gain: Double): ShortArray {
        val sampleRate = SAMPLE_RATE
        val duration = when (profile.timbre) {
            Timbre.WOOD -> 0.085
            else -> 0.125
        }
        val frames = (sampleRate * duration).roundToInt()
        val out = ShortArray(frames)
        val frequency = if (accent) profile.accentHz else profile.normalHz
        val level = if (accent) 1.0 else 0.62
        val noise = Random(if (accent) 17 else 41)

        var phase = 0.0
        for (i in 0 until frames) {
            val t = i.toDouble() / sampleRate
            val attack = (t / 0.0025).coerceAtMost(1.0)
            val envelope = attack * exp(-t * if (profile.timbre == Timbre.WOOD) 54.0 else 27.0)
            val value = when (profile.timbre) {
                Timbre.ELECTRONIC -> {
                    phase += 2.0 * PI * frequency / sampleRate
                    val fundamental = sin(phase)
                    val second = sin(2.0 * phase) * 0.24
                    val third = sin(3.0 * phase) * 0.09
                    val transient = if (t < 0.012) noise.nextDouble(-0.12, 0.12) else 0.0
                    (fundamental + second + third) * envelope + transient
                }

                Timbre.SINE -> {
                    phase += 2.0 * PI * frequency / sampleRate
                    sin(phase) * envelope
                }

                Timbre.WOOD -> {
                    val sweep = frequency * (1.0 + 0.55 * exp(-t * 75.0))
                    phase += 2.0 * PI * sweep / sampleRate
                    val body = sin(phase) * envelope
                    val knock = if (t < 0.018) noise.nextDouble(-0.55, 0.55) * exp(-t * 120.0) else 0.0
                    body * 0.75 + knock
                }
            }
            val scaled = value * gain * level
            out[i] = (scaled.coerceIn(-1.0, 1.0) * Short.MAX_VALUE).toInt().toShort()
        }
        return out
    }
}
