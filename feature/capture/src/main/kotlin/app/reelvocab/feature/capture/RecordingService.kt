package app.reelvocab.feature.capture

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.provider.Settings
import android.view.WindowManager
import android.widget.Toast
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import app.reelvocab.core.domain.model.MediaVaultRepository
import app.reelvocab.core.domain.model.VaultItem
import app.reelvocab.core.domain.model.VaultKind
import dagger.hilt.android.AndroidEntryPoint
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

@AndroidEntryPoint
class RecordingService : Service() {

    @Inject lateinit var vault: MediaVaultRepository
    @Inject lateinit var autoCapture: AutoCaptureManager

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val extractionScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val transcribeMutex = Mutex()

    private lateinit var windowManager: WindowManager
    private val handler = Handler(Looper.getMainLooper())

    private var recordingEngine: MediaRecordingEngine? = null
    private var recordingFile: File? = null
    private var recordingProjection: MediaProjection? = null
    private var recordingKind: RecordingKind? = null
    private var recordingTitle: String = ""
    private var recordingStartedAtEpochMs = 0L
    private var recordingPanel: RecordingPanel? = null
    private var recordingBubble: RecordingBubble? = null
    private var transcriber: OnDeviceTranscriber? = null
    private var launchSource = CaptureLaunchSource.App
    private var stoppingRecording = false
    private var recordingActive = false
    private var countdownRemaining = 0
    private var countdownEngine: MediaRecordingEngine? = null

    private val timerRunnable = object : Runnable {
        override fun run() {
            val elapsed = if (recordingActive) recordingEngine?.elapsedMs() ?: 0L else 0L
            recordingPanel?.setElapsed(elapsed)
            recordingBubble?.setElapsed(elapsed)
            handler.postDelayed(this, TIMER_INTERVAL_MS)
        }
    }

    private val countdownRunnable = object : Runnable {
        override fun run() {
            val engine = countdownEngine ?: return
            if (stoppingRecording) return
            if (countdownRemaining <= 1) {
                countdownRemaining = 0
                countdownEngine = null
                startEngine(engine)
                return
            }
            countdownRemaining -= 1
            recordingPanel?.setCountdown(countdownRemaining)
            handler.postDelayed(this, COUNTDOWN_INTERVAL_MS)
        }
    }

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(WindowManager::class.java)
        createChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START_RECORDING -> {
                val data = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    intent.getParcelableExtra(EXTRA_DATA, Intent::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    intent.getParcelableExtra<Intent>(EXTRA_DATA)
                }
                val mode = intent.getStringExtra(EXTRA_RECORD_MODE) ?: CaptureActivity.MODE_AUDIO
                val source = AudioSourceMode.fromStorage(intent.getStringExtra(EXTRA_AUDIO_SOURCE))
                val sourceLaunch = CaptureLaunchSource.fromStorage(
                    intent.getStringExtra(EXTRA_LAUNCH_SOURCE),
                )
                val needsProjection =
                    mode == CaptureActivity.MODE_VIDEO || source != AudioSourceMode.Microphone
                if (needsProjection && data == null) {
                    requestProjectionConsent(mode, source, sourceLaunch)
                    finishService()
                    return START_NOT_STICKY
                }
                startRecording(
                    resultCode = intent.getIntExtra(EXTRA_RESULT_CODE, 0),
                    data = data,
                    mode = mode,
                    requestedSource = source,
                    startFrom = sourceLaunch,
                )
            }

            ACTION_STOP -> stopRecording()
        }
        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        cancelCountdown()
        handler.removeCallbacks(timerRunnable)
        removeRecordingPanel()
        removeRecordingBubble()
        val engine = recordingEngine
        recordingEngine = null
        if (engine != null) {
            Thread({ runCatching { engine.stop() } }, "rv-recording-release").start()
        }
        runCatching { recordingProjection?.stop() }
        recordingProjection = null
        clearRecordingState()
        resetBubble()
        scope.cancel()
        extractionScope.cancel()
        super.onDestroy()
    }

    private fun requestProjectionConsent(
        mode: String,
        source: AudioSourceMode,
        launchSource: CaptureLaunchSource,
    ) {
        val intent = CaptureActivity.recordIntent(
            context = this,
            mode = mode,
            launchSource = launchSource,
            audioSource = source,
            skipSourceDialog = true,
        ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        runCatching { startActivity(intent) }.onFailure {
            showToast(R.string.recording_screen_permission_required)
        }
    }

    private fun startRecording(
        resultCode: Int,
        data: Intent?,
        mode: String,
        requestedSource: AudioSourceMode,
        startFrom: CaptureLaunchSource,
    ) {
        if (recordingEngine != null || stoppingRecording) return
        val kind = if (mode == CaptureActivity.MODE_VIDEO) {
            RecordingKind.Video
        } else {
            RecordingKind.Audio
        }
        val hasMicPermission = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.RECORD_AUDIO,
        ) == PackageManager.PERMISSION_GRANTED

        if (kind == RecordingKind.Audio && !hasMicPermission) {
            showToast(R.string.recording_permission_required)
            finishService()
            return
        }

        val usesProjection = data != null
        if (kind == RecordingKind.Video && !usesProjection) {
            showToast(R.string.recording_screen_permission_required)
            finishService()
            return
        }

        if (CaptureSessionService.isRunning) {
            CaptureSessionService.recordingStarted(this)
        }

        launchSource = startFrom
        try {
            startRecordingForeground(kind, usesProjection, hasMicPermission)
        } catch (error: Throwable) {
            showToast(R.string.recording_start_failed)
            finishService()
            return
        }

        var projection: MediaProjection? = null
        if (data != null) {
            val manager = getSystemService(MediaProjectionManager::class.java)
            projection = runCatching { manager.getMediaProjection(resultCode, data) }.getOrNull()
            if (kind == RecordingKind.Video && projection == null) {
                showToast(R.string.recording_screen_permission_required)
                finishService()
                return
            }
            projection?.registerCallback(
                object : MediaProjection.Callback() {
                    override fun onStop() {
                        handler.post { stopRecording() }
                    }
                },
                handler,
            )
        }

        var source = requestedSource
        if (kind == RecordingKind.Audio && source != AudioSourceMode.Microphone && projection == null) {
            source = AudioSourceMode.Microphone
            showToast(R.string.recording_internal_fallback)
        }

        val startedAt = System.currentTimeMillis()
        val vaultDir = File(filesDir, "vault").apply { mkdirs() }
        val extension = if (kind == RecordingKind.Video) "mp4" else "m4a"
        val file = File(vaultDir, "recording_$startedAt.$extension")

        recordingKind = kind
        recordingFile = file
        recordingProjection = projection
        recordingStartedAtEpochMs = startedAt
        recordingTitle = getString(
            if (kind == RecordingKind.Video) {
                R.string.recording_title_video
            } else {
                R.string.recording_title_audio
            },
        )
        stoppingRecording = false
        recordingActive = false
        autoCapture.reset()
        transcriber = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            OnDeviceTranscriber(this)
        } else {
            null
        }

        val engine = try {
            MediaRecordingEngine(
                context = this,
                kind = kind,
                projection = projection,
                requestedAudioSource = source,
                outputFile = file,
                listener = object : RecordingListener {
                    override fun onLevel(level: Float) {
                        handler.post {
                            if (recordingActive) recordingPanel?.pushLevel(level)
                        }
                    }

                    override fun onSpeechSegment(segment: SpeechSegment) {
                        if (!recordingActive || recordingEngine == null) return
                        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
                        val recognizer = transcriber ?: return
                        val sessionStartedAt = recordingStartedAtEpochMs
                        val title = recordingTitle
                        runCatching {
                            extractionScope.launch {
                                try {
                                    val transcript = transcribeMutex.withLock {
                                        runCatching { recognizer.transcribe(segment) }.getOrNull()
                                    } ?: return@launch
                                    if (
                                        recordingEngine == null ||
                                        recordingStartedAtEpochMs != sessionStartedAt
                                    ) {
                                        return@launch
                                    }
                                    autoCapture.capture(
                                        transcript = transcript,
                                        segmentStartMs = segment.startMs,
                                        sourceTitle = title,
                                        recordingStartEpochMs = sessionStartedAt,
                                    )
                                } catch (cancelled: CancellationException) {
                                    throw cancelled
                                } catch (error: Throwable) {
                                    // Auto-capture is best effort and must never stop recording.
                                }
                            }
                        }
                    }

                    override fun onError(error: Throwable) {
                        handler.post { if (!stoppingRecording) stopRecording() }
                    }

                    override fun onAudioSourceChanged(
                        requested: AudioSourceMode,
                        active: AudioSourceMode,
                    ) {
                        handler.post { showAudioSourceToast(requested, active) }
                    }
                },
            )
        } catch (error: Throwable) {
            runCatching { projection?.stop() }
            runCatching { file.delete() }
            clearRecordingState()
            showToast(R.string.recording_start_failed)
            finishService()
            return
        }

        recordingEngine = engine
        if (showRecordingPanel(kind)) {
            beginCountdown(engine)
        } else {
            startEngine(engine)
        }
    }

    private fun beginCountdown(engine: MediaRecordingEngine) {
        countdownEngine = engine
        countdownRemaining = COUNTDOWN_SECONDS
        recordingPanel?.setCountdown(countdownRemaining)
        handler.postDelayed(countdownRunnable, COUNTDOWN_INTERVAL_MS)
    }

    private fun cancelCountdown() {
        handler.removeCallbacks(countdownRunnable)
        countdownRemaining = 0
        countdownEngine = null
    }

    private fun startEngine(engine: MediaRecordingEngine) {
        if (stoppingRecording) return
        recordingStartedAtEpochMs = System.currentTimeMillis()
        try {
            engine.start()
        } catch (error: Throwable) {
            cancelCountdown()
            removeRecordingPanel()
            removeRecordingBubble()
            runCatching { recordingProjection?.stop() }
            runCatching { recordingFile?.delete() }
            clearRecordingState()
            showToast(R.string.recording_start_failed)
            finishService()
            return
        }
        recordingActive = true
        recordingPanel?.setCountdownComplete()
        recordingPanel?.setPaused(false)
        startTimer()
        collapseToBubble()
    }

    private fun stopRecording() {
        if (stoppingRecording) return
        val engine = recordingEngine
        if (engine == null) {
            finishService()
            return
        }
        stoppingRecording = true
        cancelCountdown()
        handler.removeCallbacks(timerRunnable)
        removeRecordingPanel()
        removeRecordingBubble()

        val file = recordingFile
        val startedAt = recordingStartedAtEpochMs
        val kind = recordingKind
        val projection = recordingProjection
        val title = recordingTitle
        val wasActive = recordingActive

        recordingEngine = null
        recordingFile = null
        recordingProjection = null
        recordingKind = null
        transcriber = null
        recordingActive = false

        scope.launch {
            val valid = if (wasActive) {
                runCatching { engine.stop() }.getOrDefault(false)
            } else {
                runCatching { engine.stop() }
                false
            }
            runCatching { projection?.stop() }
            if (valid && file != null && file.length() > 0L && kind != null) {
                runCatching {
                    vault.add(
                        VaultItem(
                            id = "recording_$startedAt",
                            uri = "file://${file.absolutePath}",
                            title = "$title · " + SimpleDateFormat(
                                "MMM d, HH:mm",
                                Locale.getDefault(),
                            ).format(Date(startedAt)),
                            kind = if (kind == RecordingKind.Video) {
                                VaultKind.Video
                            } else {
                                VaultKind.Audio
                            },
                            createdAtEpochMs = startedAt,
                        ),
                    )
                }
            } else {
                runCatching { file?.delete() }
            }
            withContext(Dispatchers.Main) {
                stoppingRecording = false
                finishService()
            }
        }
    }

    private fun resetBubble() {
        if (CaptureSessionService.isRunning) {
            CaptureSessionService.recordingStopped(this)
        }
    }

    private fun finishService() {
        resetBubble()
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun showRecordingPanel(kind: RecordingKind): Boolean {
        if (recordingPanel != null) return true
        if (!Settings.canDrawOverlays(this)) {
            showToast(R.string.recording_overlay_required)
            return false
        }
        val panel = RecordingPanel(
            context = this,
            title = getString(
                if (kind == RecordingKind.Video) {
                    R.string.recording_title_video
                } else {
                    R.string.recording_title_audio
                },
            ),
            pauseLabel = getString(R.string.recording_pause),
            resumeLabel = getString(R.string.recording_resume),
            stopLabel = getString(R.string.recording_stop),
            hideLabel = getString(R.string.recording_hide),
            onPause = { recordingEngine?.pause() },
            onResume = {
                recordingEngine?.resume()
                collapseToBubble()
            },
            onStop = { stopRecording() },
            onHide = { collapseToBubble() },
        )
        var added = false
        runCatching {
            windowManager.addView(panel.view, panel.windowParams())
        }.onSuccess {
            recordingPanel = panel
            added = true
            panel.setElapsed(recordingEngine?.elapsedMs() ?: 0L)
        }
        return added
    }

    private fun removeRecordingPanel() {
        val panel = recordingPanel ?: return
        recordingPanel = null
        runCatching { windowManager.removeView(panel.view) }
    }

    private fun collapseToBubble() {
        removeRecordingPanel()
        if (!recordingActive || !Settings.canDrawOverlays(this)) return
        if (recordingBubble != null) return
        val bubble = RecordingBubble(this) { expandControls() }
        runCatching {
            windowManager.addView(bubble.view, bubble.windowParams())
        }.onSuccess {
            recordingBubble = bubble
            bubble.setElapsed(recordingEngine?.elapsedMs() ?: 0L)
        }
    }

    private fun expandControls() {
        if (!recordingActive) return
        removeRecordingBubble()
        val kind = recordingKind ?: return
        if (!showRecordingPanel(kind)) return
        recordingPanel?.setPaused(recordingEngine?.isPaused() == true)
        recordingPanel?.setElapsed(recordingEngine?.elapsedMs() ?: 0L)
    }

    private fun removeRecordingBubble() {
        val bubble = recordingBubble ?: return
        recordingBubble = null
        runCatching { windowManager.removeView(bubble.view) }
    }

    private fun startTimer() {
        handler.removeCallbacks(timerRunnable)
        handler.post(timerRunnable)
    }

    private fun createChannel() {
        val manager = getSystemService(NotificationManager::class.java)
        val channel = NotificationChannel(
            CHANNEL_ID,
            getString(R.string.recording_channel_name),
            NotificationManager.IMPORTANCE_LOW,
        )
        channel.description = getString(R.string.recording_channel_description)
        manager.createNotificationChannel(channel)
    }

    private fun startRecordingForeground(
        kind: RecordingKind,
        usesProjection: Boolean,
        hasMic: Boolean,
    ) {
        var type = 0
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            if (usesProjection) type = type or ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION
            if (hasMic) type = type or ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
        }
        ServiceCompat.startForeground(
            this,
            NOTIFICATION_ID,
            buildRecordingNotification(kind),
            type,
        )
    }

    private fun buildRecordingNotification(kind: RecordingKind): Notification {
        val stopIntent = PendingIntent.getService(
            this,
            2,
            Intent(this, RecordingService::class.java).setAction(ACTION_STOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(
                getString(
                    if (kind == RecordingKind.Video) {
                        R.string.recording_notification_video
                    } else {
                        R.string.recording_notification_audio
                    },
                ),
            )
            .setContentText(getString(R.string.recording_notification_text))
            .setSmallIcon(R.drawable.ic_capture_pill)
            .setOngoing(true)
            .setSilent(true)
            .addAction(0, getString(R.string.capture_stop), stopIntent)
            .build()
    }

    private fun showAudioSourceToast(requested: AudioSourceMode, active: AudioSourceMode) {
        val message = when {
            requested == AudioSourceMode.Both && active == AudioSourceMode.Both -> return
            requested == AudioSourceMode.Both && active == AudioSourceMode.Microphone ->
                R.string.recording_both_mic_only

            requested == AudioSourceMode.Both && active == AudioSourceMode.Internal ->
                R.string.recording_both_internal_only

            requested == AudioSourceMode.Internal && active == AudioSourceMode.Microphone ->
                R.string.recording_internal_fallback

            else -> return
        }
        showToast(message)
    }

    private fun showToast(message: Int) {
        Toast.makeText(this, getString(message), Toast.LENGTH_LONG).show()
    }

    private fun clearRecordingState() {
        recordingFile = null
        recordingProjection = null
        recordingKind = null
        recordingStartedAtEpochMs = 0L
        recordingTitle = ""
        recordingActive = false
        stoppingRecording = false
    }

    companion object {
        private const val CHANNEL_ID = "recording_session"
        private const val NOTIFICATION_ID = 4203
        private const val ACTION_START_RECORDING = "app.reelvocab.recording.START"
        private const val ACTION_STOP = "app.reelvocab.recording.STOP"
        private const val EXTRA_RECORD_MODE = "record_mode"
        private const val EXTRA_AUDIO_SOURCE = "audio_source"
        private const val EXTRA_LAUNCH_SOURCE = "launch_source"
        private const val EXTRA_RESULT_CODE = "result_code"
        private const val EXTRA_DATA = "projection_data"
        private const val TIMER_INTERVAL_MS = 250L
        private const val COUNTDOWN_SECONDS = 5
        private const val COUNTDOWN_INTERVAL_MS = 1_000L

        fun start(
            context: Context,
            resultCode: Int,
            data: Intent?,
            mode: String,
            audioSource: AudioSourceMode,
            launchSource: CaptureLaunchSource,
        ) {
            val intent = Intent(context, RecordingService::class.java)
                .setAction(ACTION_START_RECORDING)
                .putExtra(EXTRA_RESULT_CODE, resultCode)
                .putExtra(EXTRA_RECORD_MODE, mode)
                .putExtra(EXTRA_AUDIO_SOURCE, audioSource.storage)
                .putExtra(EXTRA_LAUNCH_SOURCE, launchSource.storage)
            if (data != null) intent.putExtra(EXTRA_DATA, data)
            ContextCompat.startForegroundService(context, intent)
        }
    }
}
