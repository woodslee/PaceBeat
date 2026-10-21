package com.wl.pacebeat.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.util.Log
import com.wl.pacebeat.data.SoundProfile

/** Plays the sample-accurate click bar through an AudioTrack static buffer. */
class MetronomeEngine(private val context: Context) {
    private var track: AudioTrack? = null
    private var playing = false

    private var bpm: Int = 160
    private var profile: SoundProfile = SoundProfile.CC0_MPC
    private var boost: Int = 1
    private var volume: Float = 0.78f
    private var beatsPerBar: Int = 2
    private var active = false

    fun start(
        bpm: Int,
        profile: SoundProfile,
        boost: Int,
        volume: Float,
        beatsPerBar: Int = 2
    ) {
        this.bpm = bpm
        this.profile = profile
        this.boost = boost
        this.volume = volume
        this.beatsPerBar = beatsPerBar.coerceIn(2, 4)
        this.active = true
        rebuild(playImmediately = true)
    }

    fun setBpm(value: Int) {
        val safe = value.coerceIn(40, 300)
        if (safe == bpm) return
        bpm = safe
        if (active) rebuild(playImmediately = playing)
    }

    fun setProfile(value: SoundProfile) {
        if (value == profile) return
        profile = value
        if (active) rebuild(playImmediately = playing)
    }

    fun setBoost(value: Int) {
        val safe = value.coerceIn(1, 4)
        if (safe == boost) return
        boost = safe
        if (active) rebuild(playImmediately = playing)
    }

    fun setBeatsPerBar(value: Int) {
        val safe = value.coerceIn(2, 4)
        if (safe == beatsPerBar) return
        beatsPerBar = safe
        if (active) rebuild(playImmediately = playing)
    }

    fun setVolume(value: Float) {
        volume = value.coerceIn(0f, 1f)
        track?.setVolume(volume)
    }

    fun pause() {
        if (!playing) return
        playing = false
        runCatching { track?.pause() }
    }

    fun resume() {
        if (!active) return
        playing = true
        runCatching {
            track?.play()
        }.onFailure {
            rebuild(playImmediately = true)
        }
    }

    fun stop() {
        active = false
        playing = false
        val old = track
        track = null
        runCatching { old?.pause() }
        runCatching { old?.flush() }
        runCatching { old?.release() }
    }

    private fun rebuild(playImmediately: Boolean) {
        val old = track
        track = null
        playing = false
        runCatching { old?.pause() }
        runCatching { old?.flush() }
        runCatching { old?.release() }

        val pair = SoundBank.load(context, profile)
        val pcm = if (pair != null) {
            ClickSynth.buildBar(bpm, pair, boost, beatsPerBar)
        } else {
            Log.w(TAG, "Using synthesized fallback for ${profile.id}")
            ClickSynth.buildBar(bpm, profile, boost, beatsPerBar)
        }
        val minBuffer = AudioTrack.getMinBufferSize(
            ClickSynth.SAMPLE_RATE,
            AudioFormat.CHANNEL_OUT_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        )
        val bytes = pcm.size * 2
        val bufferSize = maxOf(bytes, if (minBuffer > 0) minBuffer else bytes)

        try {
            val created = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(ClickSynth.SAMPLE_RATE)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build()
                )
                .setBufferSizeInBytes(bufferSize)
                .setTransferMode(AudioTrack.MODE_STATIC)
                .build()

            val written = created.write(pcm, 0, pcm.size, AudioTrack.WRITE_BLOCKING)
            if (written <= 0) {
                created.release()
                Log.e(TAG, "AudioTrack write failed: $written")
                return
            }
            created.setLoopPoints(0, pcm.size, -1)
            created.setVolume(volume)
            track = created
            if (playImmediately) {
                created.play()
                playing = true
            }
        } catch (error: Throwable) {
            Log.e(TAG, "Unable to create metronome AudioTrack", error)
            track = null
            playing = false
        }
    }

    private companion object {
        const val TAG = "PaceBeatEngine"
    }
}
