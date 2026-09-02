package com.warptr.CaptureCapMP3

import android.Manifest
import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.SharedPreferences
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioPlaybackCaptureConfiguration
import android.media.AudioRecord
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.SystemClock
import android.provider.Settings
import android.view.Gravity
import android.view.WindowManager
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.app.NotificationCompat
import java.io.OutputStream
import java.util.concurrent.atomic.AtomicBoolean

class InternalAudioRecordingService : Service() {
    private val mainHandler = Handler(Looper.getMainLooper())
    private val running = AtomicBoolean(false)
    private val stopping = AtomicBoolean(false)
    private lateinit var catalog: RecordingCatalog
    private lateinit var preferences: SharedPreferences
    private var projection: MediaProjection? = null
    private var audioRecord: AudioRecord? = null
    private var encoder: Mp3Encoder? = null
    private var output: OutputStream? = null
    private var target: RecordingTarget? = null
    private var worker: Thread? = null
    private var startedAtElapsed = 0L
    private var startedAtWallClock = 0L
    private var overlayView: LinearLayout? = null
    private var overlayTime: TextView? = null
    private var windowManager: WindowManager? = null

    private val tick = object : Runnable {
        override fun run() {
            if (!running.get()) return
            overlayTime?.text = "正在录音 ${formatDuration(SystemClock.elapsedRealtime() - startedAtElapsed)}"
            mainHandler.postDelayed(this, 1_000)
        }
    }

    override fun onCreate() {
        super.onCreate()
        catalog = RecordingCatalog(this)
        preferences = getSharedPreferences(RecordingCatalog.PREFERENCES, Context.MODE_PRIVATE)
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> stopRecording(publish = true)
            ACTION_START -> if (!running.get()) startRecording(intent)
        }
        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    @Suppress("DEPRECATION")
    private fun startRecording(intent: Intent) {
        val resultCode = intent.getIntExtra(EXTRA_RESULT_CODE, Int.MIN_VALUE)
        val projectionData = intent.getParcelableExtra<Intent>(EXTRA_PROJECTION_DATA)
        if (resultCode == Int.MIN_VALUE || projectionData == null) {
            finishWithError("未获得内部音频授权")
            return
        }
        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            finishWithError("未授予录音权限")
            return
        }
        startedAtElapsed = SystemClock.elapsedRealtime()
        startedAtWallClock = System.currentTimeMillis()
        startForeground(NOTIFICATION_ID, buildNotification())
        try {
            val manager = getSystemService(MediaProjectionManager::class.java)
            val mediaProjection = manager.getMediaProjection(resultCode, projectionData)
                ?: error("无法创建内部音频捕获会话")
            projection = mediaProjection
            mediaProjection.registerCallback(object : MediaProjection.Callback() {
                override fun onStop() {
                    stopRecording(publish = true)
                }
            }, mainHandler)
            target = catalog.createTarget()
            output = contentResolver.openOutputStream(target!!.uri, "w")
                ?: error("无法打开录音文件")
            encoder = Mp3Encoder(bitRateKbps = catalog.getBitRate())
            audioRecord = createPlaybackRecord(projection!!).also { it.startRecording() }
            running.set(true)
            stopping.set(false)
            preferences.edit()
                .putBoolean(RecordingCatalog.KEY_ACTIVE, true)
                .putLong(RecordingCatalog.KEY_STARTED_AT, startedAtWallClock)
                .apply()
            maybeShowOverlay()
            mainHandler.post(tick)
            broadcastState(active = true)
            worker = Thread(::encodeLoop, "柿柿录音编码").apply { start() }
        } catch (error: Exception) {
            finishWithError(error.message ?: "无法开始内部录音")
        }
    }

    @SuppressLint("MissingPermission")
    private fun createPlaybackRecord(mediaProjection: MediaProjection): AudioRecord {
        val format = AudioFormat.Builder()
            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
            .setSampleRate(Mp3Encoder.SAMPLE_RATE)
            .setChannelMask(AudioFormat.CHANNEL_IN_STEREO)
            .build()
        val minimum = AudioRecord.getMinBufferSize(
            Mp3Encoder.SAMPLE_RATE,
            AudioFormat.CHANNEL_IN_STEREO,
            AudioFormat.ENCODING_PCM_16BIT,
        )
        check(minimum > 0) { "设备不支持 48 kHz 立体声内部音频捕获" }
        val config = AudioPlaybackCaptureConfiguration.Builder(mediaProjection)
            .addMatchingUsage(AudioAttributes.USAGE_MEDIA)
            .addMatchingUsage(AudioAttributes.USAGE_GAME)
            .addMatchingUsage(AudioAttributes.USAGE_UNKNOWN)
            .build()
        return AudioRecord.Builder()
            .setAudioFormat(format)
            .setBufferSizeInBytes(maxOf(minimum * 2, 16 * 1024))
            .setAudioPlaybackCaptureConfig(config)
            .build()
            .also { check(it.state == AudioRecord.STATE_INITIALIZED) { "内部音频捕获初始化失败" } }
    }

    private fun encodeLoop() {
        val pcm = ShortArray(8_192)
        var failure: Exception? = null
        try {
            while (running.get()) {
                val read = audioRecord?.read(pcm, 0, pcm.size, AudioRecord.READ_BLOCKING) ?: break
                if (read > 0) {
                    val usable = read - (read % Mp3Encoder.CHANNELS)
                    if (usable > 0) {
                        encoder?.encode(pcm, usable)?.takeIf { it.isNotEmpty() }?.let(output!!::write)
                    }
                } else if (read != AudioRecord.ERROR_DEAD_OBJECT && read != AudioRecord.ERROR_INVALID_OPERATION) {
                    throw IllegalStateException("内部音频读取失败：$read")
                }
            }
        } catch (error: Exception) {
            failure = error
        } finally {
            if (failure != null) {
                mainHandler.post { finishWithError(failure!!.message ?: "MP3 编码失败") }
            } else if (!stopping.get()) {
                mainHandler.post { stopRecording(publish = true) }
            }
        }
    }

    private fun stopRecording(publish: Boolean) {
        if (!stopping.compareAndSet(false, true)) return
        running.set(false)
        mainHandler.removeCallbacks(tick)
        runCatching { audioRecord?.stop() }
        worker?.takeIf { it !== Thread.currentThread() }?.join(2_000)
        runCatching { encoder?.flush()?.takeIf { it.isNotEmpty() }?.let(output!!::write) }
        runCatching { output?.flush() }
        runCatching { output?.close() }
        if (publish) {
            runCatching { catalog.publish(target!!, SystemClock.elapsedRealtime() - startedAtElapsed) }
                .onFailure { catalog.discard(target) }
        } else {
            catalog.discard(target)
        }
        releaseCapture()
        preferences.edit().putBoolean(RecordingCatalog.KEY_ACTIVE, false).remove(RecordingCatalog.KEY_STARTED_AT).apply()
        hideOverlay()
        broadcastState(active = false)
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun finishWithError(message: String) {
        if (stopping.compareAndSet(false, true)) {
            running.set(false)
            mainHandler.removeCallbacks(tick)
            runCatching { audioRecord?.stop() }
            runCatching { output?.close() }
            catalog.discard(target)
            releaseCapture()
            preferences.edit().putBoolean(RecordingCatalog.KEY_ACTIVE, false).remove(RecordingCatalog.KEY_STARTED_AT).apply()
            hideOverlay()
            broadcastState(active = false, error = message)
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
        }
    }

    private fun releaseCapture() {
        runCatching { audioRecord?.release() }
        audioRecord = null
        runCatching { encoder?.close() }
        encoder = null
        output = null
        target = null
        runCatching { projection?.stop() }
        projection = null
    }

    private fun buildNotification(): Notification {
        val stopIntent = PendingIntent.getService(
            this,
            0,
            Intent(this, InternalAudioRecordingService::class.java).setAction(ACTION_STOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_recording)
            .setContentTitle("柿柿录音正在录制内部音频")
            .setContentText("MP3 · ${catalog.getBitRate()} kbps")
            .setOngoing(true)
            .setUsesChronometer(true)
            .setWhen(startedAtWallClock)
            .addAction(0, "停止", stopIntent)
            .build()
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(CHANNEL_ID, "内部录音", NotificationManager.IMPORTANCE_LOW).apply {
            description = "内部音频录制进行中"
        }
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    private fun maybeShowOverlay() {
        if (!catalog.isOverlayEnabled() || !Settings.canDrawOverlays(this)) return
        windowManager = getSystemService(WindowManager::class.java)
        overlayView = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(dp(16), dp(10), dp(16), dp(10))
            background = GradientDrawable().apply {
                setColor(Color.argb(232, 34, 34, 34))
                cornerRadius = dp(16).toFloat()
            }
            overlayTime = TextView(context).apply {
                setTextColor(Color.WHITE)
                textSize = 14f
                text = "正在录音 00:00"
            }
            addView(overlayTime)
            addView(Button(context).apply {
                text = "停止"
                setOnClickListener { stopRecording(publish = true) }
            })
        }
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            android.graphics.PixelFormat.TRANSLUCENT,
        ).apply {
            gravity = Gravity.TOP or Gravity.END
            x = dp(12)
            y = dp(88)
        }
        runCatching { windowManager?.addView(overlayView, params) }
    }

    private fun hideOverlay() {
        overlayView?.let { view -> runCatching { windowManager?.removeView(view) } }
        overlayView = null
        overlayTime = null
        windowManager = null
    }

    private fun broadcastState(active: Boolean, error: String? = null) {
        sendBroadcast(Intent(ACTION_STATE_CHANGED).setPackage(packageName).apply {
            putExtra(EXTRA_ACTIVE, active)
            if (error != null) putExtra(EXTRA_ERROR, error)
        })
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    override fun onDestroy() {
        if (running.get() && !stopping.get()) stopRecording(publish = true)
        super.onDestroy()
    }

    companion object {
        const val ACTION_START = "com.warptr.CaptureCapMP3.START"
        const val ACTION_STOP = "com.warptr.CaptureCapMP3.STOP"
        const val ACTION_STATE_CHANGED = "com.warptr.CaptureCapMP3.STATE_CHANGED"
        const val EXTRA_RESULT_CODE = "result_code"
        const val EXTRA_PROJECTION_DATA = "projection_data"
        const val EXTRA_ACTIVE = "active"
        const val EXTRA_ERROR = "error"
        private const val CHANNEL_ID = "internal_recording"
        private const val NOTIFICATION_ID = 1001

        fun formatDuration(durationMs: Long): String {
            val totalSeconds = durationMs / 1_000
            return "%02d:%02d".format(totalSeconds / 60, totalSeconds % 60)
        }
    }
}
