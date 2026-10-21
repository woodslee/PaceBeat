package com.wl.pacebeat.service

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.PowerManager
import android.os.SystemClock
import android.speech.tts.TextToSpeech
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import com.wl.pacebeat.MainActivity
import com.wl.pacebeat.R
import com.wl.pacebeat.audio.MetronomeEngine
import com.wl.pacebeat.data.PlanCodec
import com.wl.pacebeat.data.PlanType
import com.wl.pacebeat.data.PlayerSettings
import com.wl.pacebeat.data.Segment
import com.wl.pacebeat.data.SoundProfile
import com.wl.pacebeat.data.WorkoutPlan
import com.wl.pacebeat.data.formatRemaining
import java.util.Locale
import kotlin.math.ceil

class MetronomeService : Service() {

    private val handler = Handler(Looper.getMainLooper())
    private var engine: MetronomeEngine? = null
    private var wakeLock: PowerManager.WakeLock? = null

    private var plan: WorkoutPlan = WorkoutPlan("", PlanType.BLANK, emptyList())
    private var settings = PlayerSettings()
    private var index = 0
    private var startedAt = 0L
    private var beatBaseAt = 0L
    private var paused = false
    private var completed = false
    private var active = false
    private var pauseRemainingMs = 0L
    private var sessionStartedAt = 0L
    private var pauseStartedAt = 0L
    private var pausedTotalMs = 0L
    private var lastCountdownSecond = -1
    private var lastNotificationAt = 0L
    private var lastNotifiedSecond = -1
    private var foregroundStarted = false

    private var tts: TextToSpeech? = null
    private var ttsReady = false

    private val ticker = object : Runnable {
        override fun run() {
            if (!active || completed) return
            if (paused) return
            val segment = plan.segments.getOrNull(index) ?: return
            val now = SystemClock.elapsedRealtime()
            val elapsed = (now - startedAt).coerceAtLeast(0L)
            val remaining = segment.durationMs - elapsed
            if (remaining <= 0L) {
                advance(auto = true)
                return
            }
            val beatDuration = 60_000.0 / segment.spm.coerceAtLeast(1)
            val beatIndex = ((now - beatBaseAt) / beatDuration).toInt()
                .coerceIn(0, Int.MAX_VALUE) % settings.beatsPerBar.coerceIn(2, 4)
            val totalElapsed = plan.segments.take(index).sumOf { it.durationMs } + elapsed
            PlaybackBus.update {
                it.copy(
                    active = true,
                    paused = false,
                    completed = false,
                    planName = plan.name,
                    segments = plan.segments,
                    index = index,
                    remainingMs = remaining,
                    bpm = segment.spm,
                    beatIndex = beatIndex,
                    elapsedMs = totalElapsed,
                    actualElapsedMs = activeElapsedMs(now),
                    soundId = settings.soundId,
                    volume = settings.volume,
                    multiplier = settings.multiplier,
                    voice = settings.voice,
                    beatsPerBar = settings.beatsPerBar
                )
            }
            handleCountdown(remaining)
            updateNotification(force = false)
            handler.postDelayed(this, 60L)
        }
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        initTts()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> startSession(intent)
            ACTION_PAUSE -> pauseSession()
            ACTION_RESUME -> resumeSession()
            ACTION_NEXT -> advance(auto = false)
            ACTION_RESET -> resetSession()
            ACTION_STOP -> stopSession()
            ACTION_CLEAR -> {
                PlaybackBus.clear()
                stopSession()
            }

            ACTION_SET_VOLUME -> setVolume(intent.getFloatExtra(EXTRA_VOLUME, settings.volume))
            ACTION_SET_SOUND -> setSound(intent.getStringExtra(EXTRA_SOUND) ?: settings.soundId)
            ACTION_SET_BOOST -> setBoost(intent.getIntExtra(EXTRA_BOOST, settings.multiplier))
            ACTION_SET_VOICE -> setVoice(intent.getBooleanExtra(EXTRA_VOICE, settings.voice))
            ACTION_SET_BPM -> setBpm(intent.getIntExtra(EXTRA_BPM, 160))
            ACTION_SET_BEATS -> setBeatsPerBar(intent.getIntExtra(EXTRA_BEATS, settings.beatsPerBar))
            else -> Unit
        }
        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        engine?.stop()
        engine = null
        runCatching { tts?.stop() }
        runCatching { tts?.shutdown() }
        tts = null
        releaseWakeLock()
        if (!completed) {
            active = false
            PlaybackBus.clear()
        }
        super.onDestroy()
    }

    private fun startSession(intent: Intent) {
        val decoded = PlanCodec.decode(intent.getStringExtra(EXTRA_PLAN))
        if (decoded == null || decoded.segments.isEmpty()) {
            PlaybackBus.clear()
            stopSelf()
            return
        }
        settings = PlayerSettings(
            soundId = SoundProfile.fromId(intent.getStringExtra(EXTRA_SOUND)).id,
            volume = intent.getFloatExtra(EXTRA_VOLUME, settings.volume).coerceIn(0f, 1f),
            multiplier = intent.getIntExtra(EXTRA_BOOST, settings.multiplier).coerceIn(1, 4),
            voice = intent.getBooleanExtra(EXTRA_VOICE, settings.voice),
            beatsPerBar = intent.getIntExtra(EXTRA_BEATS, settings.beatsPerBar).coerceIn(2, 4)
        )
        plan = decoded
        index = 0
        paused = false
        completed = false
        active = true
        pauseRemainingMs = decoded.segments.first().durationMs
        sessionStartedAt = SystemClock.elapsedRealtime()
        pauseStartedAt = 0L
        pausedTotalMs = 0L
        PlaybackBus.prepare(decoded, settings)
        startForegroundCompat(buildNotification())
        acquireWakeLock()
        startSegment(0, announce = true)
    }

    private fun startSegment(newIndex: Int, announce: Boolean) {
        if (newIndex !in plan.segments.indices) {
            completeSession()
            return
        }
        index = newIndex
        paused = false
        completed = false
        lastCountdownSecond = -1
        lastNotifiedSecond = -1
        val now = SystemClock.elapsedRealtime()
        if (pauseStartedAt > 0L) {
            pausedTotalMs += (now - pauseStartedAt).coerceAtLeast(0L)
            pauseStartedAt = 0L
        }
        startedAt = now
        beatBaseAt = now
        val segment = plan.segments[index]
        val currentEngine = engine ?: MetronomeEngine(applicationContext).also { engine = it }
        currentEngine.start(
            bpm = segment.spm,
            profile = settings.sound,
            boost = settings.multiplier,
            volume = settings.volume,
            beatsPerBar = settings.beatsPerBar
        )
        PlaybackBus.update {
            it.copy(
                active = true,
                paused = false,
                completed = false,
                planName = plan.name,
                segments = plan.segments,
                index = index,
                remainingMs = segment.durationMs,
                bpm = segment.spm,
                beatIndex = 0,
                elapsedMs = plan.segments.take(index).sumOf { it.durationMs },
                soundId = settings.soundId,
                volume = settings.volume,
                multiplier = settings.multiplier,
                voice = settings.voice,
                beatsPerBar = settings.beatsPerBar
            )
        }
        updateNotification(force = true)
        if (announce) {
            val prefix = if (index == 0) "开始训练。" else "下一段。"
            speak("$prefix${segment.name}，${segment.spm} 步频")
        }
        handler.removeCallbacks(ticker)
        handler.post(ticker)
    }

    private fun pauseSession() {
        if (!active || paused || completed) return
        val segment = plan.segments.getOrNull(index) ?: return
        val elapsed = SystemClock.elapsedRealtime() - startedAt
        pauseRemainingMs = (segment.durationMs - elapsed).coerceAtLeast(0L)
        paused = true
        pauseStartedAt = SystemClock.elapsedRealtime()
        handler.removeCallbacks(ticker)
        engine?.pause()
        PlaybackBus.update {
            it.copy(paused = true, active = true, remainingMs = pauseRemainingMs)
        }
        updateNotification(force = true)
    }

    private fun resumeSession() {
        if (!active || !paused || completed) return
        val segment = plan.segments.getOrNull(index) ?: return
        paused = false
        val now = SystemClock.elapsedRealtime()
        if (pauseStartedAt > 0L) {
            pausedTotalMs += (now - pauseStartedAt).coerceAtLeast(0L)
            pauseStartedAt = 0L
        }
        startedAt = now - (segment.durationMs - pauseRemainingMs).coerceAtLeast(0L)
        beatBaseAt = now
        lastCountdownSecond = -1
        engine?.resume()
        PlaybackBus.update { it.copy(paused = false, active = true) }
        updateNotification(force = true)
        handler.removeCallbacks(ticker)
        handler.post(ticker)
    }

    private fun resetSession() {
        if (!active || completed) return
        val segment = plan.segments.getOrNull(index) ?: return
        speak("重新开始。${segment.name}")
        startSegment(index, announce = false)
    }

    private fun advance(auto: Boolean) {
        if (!active || completed) return
        if (index + 1 < plan.segments.size) {
            startSegment(index + 1, announce = true)
        } else {
            completeSession()
        }
    }

    private fun completeSession() {
        if (completed) return
        completed = true
        paused = false
        val actualElapsed = activeElapsedMs(SystemClock.elapsedRealtime())
        handler.removeCallbacks(ticker)
        engine?.stop()
        PlaybackBus.update {
            it.copy(
                active = true,
                paused = false,
                completed = true,
                remainingMs = 0L,
                beatIndex = 0,
                elapsedMs = plan.segments.sumOf { segment -> segment.durationMs },
                actualElapsedMs = actualElapsed,
                soundId = settings.soundId,
                volume = settings.volume,
                multiplier = settings.multiplier,
                voice = settings.voice
            )
        }
        speak("训练完成。做得很好。")
        updateNotification(force = true)
        releaseWakeLock()
        handler.postDelayed({
            stopForeground(STOP_FOREGROUND_REMOVE)
            foregroundStarted = false
            stopSelf()
        }, 2200L)
    }

    private fun stopSession() {
        active = false
        paused = false
        completed = false
        handler.removeCallbacksAndMessages(null)
        engine?.stop()
        engine = null
        runCatching { tts?.stop() }
        releaseWakeLock()
        PlaybackBus.clear()
        stopForeground(STOP_FOREGROUND_REMOVE)
        foregroundStarted = false
        stopSelf()
    }

    private fun setBeatsPerBar(value: Int) {
        val safe = value.coerceIn(2, 4)
        if (settings.beatsPerBar == safe) return
        settings = settings.copy(beatsPerBar = safe)
        engine?.setBeatsPerBar(safe)
        beatBaseAt = SystemClock.elapsedRealtime()
        PlaybackBus.update { it.copy(beatsPerBar = safe) }
        updateNotification(force = true)
    }

    private fun activeElapsedMs(now: Long): Long {
        if (sessionStartedAt <= 0L) return 0L
        val pausedNow = if (pauseStartedAt > 0L) (now - pauseStartedAt).coerceAtLeast(0L) else 0L
        return (now - sessionStartedAt - pausedTotalMs - pausedNow).coerceAtLeast(0L)
    }

    private fun setVolume(value: Float) {
        val safe = value.coerceIn(0f, 1f)
        settings = settings.copy(volume = safe)
        engine?.setVolume(safe)
        PlaybackBus.update { it.copy(volume = safe) }
    }

    private fun setSound(soundId: String) {
        settings = settings.copy(soundId = SoundProfile.fromId(soundId).id)
        engine?.setProfile(settings.sound)
        beatBaseAt = SystemClock.elapsedRealtime()
        PlaybackBus.update { it.copy(soundId = settings.soundId) }
        updateNotification(force = true)
    }

    private fun setBoost(value: Int) {
        settings = settings.copy(multiplier = value.coerceIn(1, 4))
        engine?.setBoost(settings.multiplier)
        beatBaseAt = SystemClock.elapsedRealtime()
        PlaybackBus.update { it.copy(multiplier = settings.multiplier) }
    }

    private fun setVoice(value: Boolean) {
        settings = settings.copy(voice = value)
        if (!value) runCatching { tts?.stop() }
        PlaybackBus.update { it.copy(voice = value) }
    }

    private fun setBpm(value: Int) {
        if (!active || completed) return
        val segment = plan.segments.getOrNull(index) ?: return
        val safe = value.coerceIn(100, 240)
        if (segment.spm == safe) return
        val updated = segment.copy(spm = safe)
        plan = plan.copy(segments = plan.segments.toMutableList().also { it[index] = updated })
        engine?.setBpm(safe)
        beatBaseAt = SystemClock.elapsedRealtime()
        PlaybackBus.update {
            it.copy(
                segments = plan.segments,
                bpm = safe,
                beatIndex = 0
            )
        }
        updateNotification(force = true)
    }

    private fun handleCountdown(remainingMs: Long) {
        if (!settings.voice) return
        val seconds = ceil(remainingMs / 1000.0).toInt()
        if (seconds in 1..3 && seconds != lastCountdownSecond) {
            lastCountdownSecond = seconds
            speak(
                when (seconds) {
                    3 -> "三"
                    2 -> "二"
                    else -> "一"
                }
            )
        }
    }

    private fun acquireWakeLock() {
        if (wakeLock != null) return
        val manager = getSystemService(Context.POWER_SERVICE) as? PowerManager ?: return
        wakeLock = manager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "PaceBeat::Metronome").apply {
            setReferenceCounted(false)
            acquire(6 * 60 * 60 * 1000L)
        }
    }

    private fun releaseWakeLock() {
        runCatching {
            wakeLock?.takeIf { it.isHeld }?.release()
        }
        wakeLock = null
    }

    private fun initTts() {
        tts = TextToSpeech(applicationContext) { status ->
            if (status == TextToSpeech.SUCCESS) {
                val result = tts?.setLanguage(Locale.SIMPLIFIED_CHINESE)
                ttsReady = result != TextToSpeech.LANG_MISSING_DATA &&
                    result != TextToSpeech.LANG_NOT_SUPPORTED
                if (ttsReady) {
                    tts?.setSpeechRate(1.08f)
                }
            }
        }
    }

    private fun speak(text: String) {
        if (!settings.voice || !ttsReady) return
        runCatching {
            tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "pacebeat_${System.nanoTime()}")
        }
    }

    private fun startForegroundCompat(notification: Notification) {
        ServiceCompat.startForeground(
            this,
            NOTIFICATION_ID,
            notification,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
            } else {
                0
            }
        )
        foregroundStarted = true
    }

    private fun updateNotification(force: Boolean) {
        if (!foregroundStarted) return
        val state = PlaybackBus.state.value
        val now = SystemClock.elapsedRealtime()
        val wholeSecond = (state.remainingMs / 1000L).toInt()
        if (!force && now - lastNotificationAt < 850L && wholeSecond == lastNotifiedSecond) {
            return
        }
        lastNotificationAt = now
        lastNotifiedSecond = wholeSecond
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        try {
            manager.notify(NOTIFICATION_ID, buildNotification())
        } catch (error: SecurityException) {
            Log.w(TAG, "Notification permission missing", error)
        }
    }

    private fun buildNotification(): Notification {
        val state = PlaybackBus.state.value
        val current = state.current
        val pausedLabel = if (state.paused) "已暂停" else "正在播放"
        val title = when {
            state.completed -> "PaceBeat · 训练完成"
            current != null -> "PaceBeat · ${state.bpm} SPM · $pausedLabel"
            else -> "PaceBeat"
        }
        val text = when {
            state.completed -> "本次训练已完成，做得好！"
            current != null -> {
                val next = state.next?.let { " · 下一段 ${it.name}" } ?: " · 最后一段"
                "${current.name} · 剩余 ${formatRemaining(state.remainingMs)}$next"
            }

            else -> "准备开始"
        }
        val pauseAction = if (state.paused) ACTION_RESUME else ACTION_PAUSE
        val pauseLabel = if (state.paused) "继续" else "暂停"
        val contentIntent = PendingIntent.getActivity(
            this,
            1,
            Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_beat)
            .setContentTitle(title)
            .setContentText(text)
            .setContentIntent(contentIntent)
            .setOnlyAlertOnce(true)
            .setOngoing(!state.completed)
            .setSilent(true)
            .setShowWhen(false)
            .setCategory(NotificationCompat.CATEGORY_TRANSPORT)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setProgress(100, (state.progress * 100).toInt().coerceIn(0, 100), false)
            .addAction(0, pauseLabel, servicePendingIntent(pauseAction, 11))
            .addAction(0, "下一段", servicePendingIntent(ACTION_NEXT, 12))
            .addAction(0, "停止", servicePendingIntent(ACTION_STOP, 13))
            .build()
    }

    private fun servicePendingIntent(action: String, requestCode: Int): PendingIntent {
        return PendingIntent.getService(
            this,
            requestCode,
            Intent(this, MetronomeService::class.java).setAction(action),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channel = NotificationChannel(
            CHANNEL_ID,
            getString(R.string.notification_channel_name),
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = getString(R.string.notification_channel_desc)
            setSound(null, null)
            enableVibration(false)
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
        }
        manager.createNotificationChannel(channel)
    }

    companion object {
        const val ACTION_START = "com.wl.pacebeat.action.START"
        const val ACTION_PAUSE = "com.wl.pacebeat.action.PAUSE"
        const val ACTION_RESUME = "com.wl.pacebeat.action.RESUME"
        const val ACTION_NEXT = "com.wl.pacebeat.action.NEXT"
        const val ACTION_RESET = "com.wl.pacebeat.action.RESET"
        const val ACTION_STOP = "com.wl.pacebeat.action.STOP"
        const val ACTION_CLEAR = "com.wl.pacebeat.action.CLEAR"
        const val ACTION_SET_VOLUME = "com.wl.pacebeat.action.SET_VOLUME"
        const val ACTION_SET_SOUND = "com.wl.pacebeat.action.SET_SOUND"
        const val ACTION_SET_BOOST = "com.wl.pacebeat.action.SET_BOOST"
        const val ACTION_SET_VOICE = "com.wl.pacebeat.action.SET_VOICE"
        const val ACTION_SET_BPM = "com.wl.pacebeat.action.SET_BPM"
        const val ACTION_SET_BEATS = "com.wl.pacebeat.action.SET_BEATS"

        const val EXTRA_PLAN = "plan"
        const val EXTRA_SOUND = "sound"
        const val EXTRA_VOLUME = "volume"
        const val EXTRA_BOOST = "boost"
        const val EXTRA_VOICE = "voice"
        const val EXTRA_BPM = "bpm"
        const val EXTRA_BEATS = "beats"

        private const val CHANNEL_ID = "pacebeat_playback"
        private const val NOTIFICATION_ID = 1001
        private const val TAG = "PaceBeatService"
    }
}
