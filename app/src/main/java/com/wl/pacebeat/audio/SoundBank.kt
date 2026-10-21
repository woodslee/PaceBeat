package com.wl.pacebeat.audio

import android.content.Context
import android.util.Log
import com.wl.pacebeat.data.SoundProfile
import java.nio.ByteBuffer
import java.nio.ByteOrder

/** A pre-rendered accent/normal click pair, stored as 44.1 kHz mono 16-bit PCM. */
data class ClickPair(
    val accent: ShortArray,
    val normal: ShortArray
)

/**
 * Loads the two short PCM one-shots for each sound profile from assets.
 *
 * Click profiles come from CC0 Freesound previews. Percussion profiles are
 * extracted from the user-provided 1/2-time, 8-second loops (beat 1 = accent,
 * beat 2 = normal). No network access or full SoundFont is required at runtime.
 */
object SoundBank {
    private const val TAG = "PaceBeatSoundBank"
    private val cache = HashMap<String, ClickPair?>()

    fun load(context: Context, profile: SoundProfile): ClickPair? {
        cache[profile.id]?.let { return it }
        if (cache.containsKey(profile.id)) return null
        val loaded = runCatching {
            val accent = readPcm(context, "sounds/${profile.id}_accent.pcm")
            val normal = readPcm(context, "sounds/${profile.id}_normal.pcm")
            if (accent == null || normal == null) null else ClickPair(accent, normal)
        }.onFailure {
            Log.w(TAG, "Unable to load samples for ${profile.id}", it)
        }.getOrNull()
        cache[profile.id] = loaded
        return loaded
    }

    private fun readPcm(context: Context, path: String): ShortArray? {
        val bytes = context.assets.open(path).use { it.readBytes() }
        if (bytes.isEmpty() || bytes.size % 2 != 0) return null
        val shorts = ShortArray(bytes.size / 2)
        ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().get(shorts)
        return shorts
    }
}
