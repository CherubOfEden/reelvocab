package app.reelvocab.feature.capture

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
import android.content.pm.ServiceInfo
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.provider.Settings
import android.view.GestureDetector
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import androidx.compose.ui.graphics.toArgb
import app.reelvocab.core.designsystem.theme.ReelDarkColors
import app.reelvocab.core.domain.model.MediaVaultRepository
import app.reelvocab.core.domain.model.VaultItem
import app.reelvocab.core.domain.model.VaultKind
import dagger.hilt.android.AndroidEntryPoint
import java.io.File
import java.io.FileOutputStream
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
import kotlinx.coroutines.withContext
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

@AndroidEntryPoint
class CaptureSessionService : Service() {

    @Inject lateinit var vault: MediaVaultRepository
    @Inject lateinit var autoCapture: AutoCaptureManager

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val extractionScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val transcribeMutex = Mutex()
    private lateinit var windowManager: WindowManager
    private var pill: View? = null
    private var projection: MediaProjection? = null
    private var virtualDisplay: VirtualDisplay? = null
    private var imageReader: ImageReader? = null
    private var capturing = false
    private var recordingEngine: MediaRecordingEngine? = null
    private var recordingFile: File? = null
    private var recordingProjection: MediaProjection? = null
    private var recordingKind: RecordingKind? = null
    private var recordingTitle: String = ""
    private var recordingStartedAtEpochMs = 0L
    private var recordingPanel: RecordingPanel? = null
    private var transcriber: OnDeviceTranscriber? = null
    private var stoppingRecording = false
    private var modeMenu: View? = null

    private val handler = Handler(Looper.getMainLooper())
    private val timerRunnable = object : Runnable {
        override fun run() {
            recordingPanel?.setElapsed(recordingEngine?.elapsedMs() ?: 0L)
            handler.postDelayed(this, TIMER_INTERVAL_MS)
        }
    }

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(WindowManager::class.java)
        setRunning(true)
        createChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START_PILL -> {
                startPillForeground()
                if (!ensurePill()) stopEverything()
            }

            ACTION_START_SESSION -> {
                val data = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    intent.getParcelableExtra(EXTRA_DATA, Intent::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    intent.getParcelableExtra<Intent>(EXTRA_DATA)
                }
                val resultCode = intent.getIntExtra(EXTRA_RESULT_CODE, 0)
                val captureNow = intent.getBooleanExtra(EXTRA_CAPTURE_NOW, false)
                if (data == null) {
                    stopEverything()
                } else {
                    startSession(resultCode, data)
                    if (!ensurePill()) {
                        stopEverything()
                        return START_NOT_STICKY
                    }
                    if (captureNow) handler.postDelayed({ captureFrame() }, CAPTURE_DELAY_MS)
                }
            }

            ACTION_START_RECORDING -> {
                val data = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    intent.getParcelableExtra(EXTRA_DATA, Intent::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    intent.getParcelableExtra<Intent>(EXTRA_DATA)
                }
                val mode = intent.getStringExtra(EXTRA_RECORD_MODE) ?: CaptureActivity.MODE_AUDIO
                startRecording(intent.getIntExtra(EXTRA_RESULT_CODE, 0), data, mode)
            }

            ACTION_STOP_RECORD -> stopRecording()

            ACTION_CAPTURE -> if (projection == null) {
                launchConsent()
            } else {
                captureFrame()
            }

            ACTION_STOP -> stopEverything()
        }
        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        releaseProjection()
        releaseRecording()
        removePill()
        removeModeMenu()
        removeRecordingPanel()
        scope.cancel()
        extractionScope.cancel()
        setRunning(false)
        super.onDestroy()
    }

    private fun startPillForeground() {
        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
        } else {
            0
        }
        ServiceCompat.startForeground(this, NOTIFICATION_ID, buildNotification(), type)
    }

    private fun startCaptureForeground() {
        ServiceCompat.startForeground(
            this,
            NOTIFICATION_ID,
            buildNotification(),
            ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION,
        )
    }

    private fun createChannel() {
        val manager = getSystemService(NotificationManager::class.java)
        val channel = NotificationChannel(
            CHANNEL_ID,
            getString(R.string.capture_channel_name),
            NotificationManager.IMPORTANCE_LOW,
        )
        channel.description = getString(R.string.capture_channel_description)
        manager.createNotificationChannel(channel)
    }

    private fun buildNotification(): Notification {
        val stopIntent = PendingIntent.getService(
            this,
            1,
            Intent(this, CaptureSessionService::class.java).setAction(ACTION_STOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.capture_notification_title))
            .setContentText(getString(R.string.capture_notification_text))
            .setSmallIcon(R.drawable.ic_capture_pill)
            .setOngoing(true)
            .setSilent(true)
            .addAction(0, getString(R.string.capture_stop), stopIntent)
            .build()
    }

    private fun ensurePill(): Boolean {
        if (pill != null) return true
        if (!Settings.canDrawOverlays(this)) return false

        val density = resources.displayMetrics.density
        val size = (PILL_SIZE_DP * density).roundToInt()
        val margin = (PILL_MARGIN_DP * density).roundToInt()

        val frame = FrameLayout(this).apply {
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                gradientType = GradientDrawable.LINEAR_GRADIENT
                colors = intArrayOf(ReelDarkColors.gold.toArgb(), ReelDarkColors.goldDim.toArgb())
                orientation = GradientDrawable.Orientation.TOP_BOTTOM
            }
            elevation = 8f * density
            contentDescription = getString(R.string.capture_pill_description)
        }
        val iconSize = (PILL_ICON_DP * density).roundToInt()
        frame.addView(
            ImageView(this).apply {
                setImageResource(R.drawable.ic_capture_pill)
                layoutParams = FrameLayout.LayoutParams(
                    iconSize,
                    iconSize,
                    Gravity.CENTER,
                )
            },
        )

        val params = WindowManager.LayoutParams(
            size,
            size,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT,
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = resources.displayMetrics.widthPixels - size - margin
            y = resources.displayMetrics.heightPixels / 3
        }

        attachGestures(frame, params, margin)
        runCatching { windowManager.addView(frame, params) }.onSuccess { pill = frame }
        return pill != null
    }

    private fun attachGestures(
        view: View,
        params: WindowManager.LayoutParams,
        margin: Int,
    ) {
        val gestures = GestureDetector(
            this,
            object : GestureDetector.SimpleOnGestureListener() {
                override fun onDown(e: MotionEvent): Boolean = true
                override fun onSingleTapUp(e: MotionEvent): Boolean {
                    onPillTapped()
                    return true
                }

                override fun onLongPress(e: MotionEvent) {
                    if (recordingEngine != null) stopRecording() else stopEverything()
                }
            },
        )
        val slop = android.view.ViewConfiguration.get(this).scaledTouchSlop
        var startX = 0
        var startY = 0
        var downX = 0f
        var downY = 0f
        var dragging = false

        view.setOnTouchListener { v, event ->
            gestures.onTouchEvent(event)
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    startX = params.x
                    startY = params.y
                    downX = event.rawX
                    downY = event.rawY
                    dragging = false
                    true
                }

                MotionEvent.ACTION_MOVE -> {
                    val dx = event.rawX - downX
                    val dy = event.rawY - downY
                    if (!dragging && (abs(dx) > slop || abs(dy) > slop)) dragging = true
                    if (dragging) {
                        params.x = (startX + dx).roundToInt()
                        params.y = (startY + dy).roundToInt()
                        windowManager.updateViewLayout(v, params)
                    }
                    true
                }

                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    if (dragging) {
                        val metrics = resources.displayMetrics
                        params.x = if (params.x + v.width / 2 < metrics.widthPixels / 2) {
                            margin
                        } else {
                            metrics.widthPixels - v.width - margin
                        }
                        params.y = params.y.coerceIn(0, metrics.heightPixels - v.height)
                        windowManager.updateViewLayout(v, params)
                    }
                    dragging
                }

                else -> false
            }
        }
    }

    private fun onPillTapped() {
        removeModeMenu()
        if (recordingEngine != null) {
            if (recordingPanel == null) {
                recordingKind?.let { showRecordingPanel(it) }
            }
            return
        }
        when (CapturePreferences.pipMode(this)) {
            PipCaptureMode.WordsOnly -> {
                if (projection == null) launchConsent() else captureFrame()
            }

            PipCaptureMode.Video -> openRecorder(CaptureActivity.MODE_VIDEO)
            PipCaptureMode.Audio -> openRecorder(CaptureActivity.MODE_AUDIO)
            PipCaptureMode.All -> showModeMenu()
        }
    }

    private fun openRecorder(mode: String) {
        removeModeMenu()
        startActivity(
            CaptureActivity.recordIntent(this, mode)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
        )
    }

    private fun showModeMenu() {
        removeModeMenu()
        if (!Settings.canDrawOverlays(this)) return
        val density = resources.displayMetrics.density
        val menu = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = 18f * density
                setColor(ReelDarkColors.elevated.toArgb())
            }
            setPadding(
                (8 * density).toInt(),
                (8 * density).toInt(),
                (8 * density).toInt(),
                (8 * density).toInt(),
            )
            elevation = 10f * density
        }
        fun item(label: String, action: () -> Unit) {
            menu.addView(
                TextView(this).apply {
                    text = label
                    textSize = 15f
                    setTextColor(ReelDarkColors.textPrimary.toArgb())
                    setPadding(
                        (18 * density).toInt(),
                        (14 * density).toInt(),
                        (18 * density).toInt(),
                        (14 * density).toInt(),
                    )
                    setOnClickListener {
                        removeModeMenu()
                        action()
                    }
                },
            )
        }
        item(getString(R.string.capture_mode_words)) {
            if (projection == null) launchConsent() else captureFrame()
        }
        item(getString(R.string.capture_mode_video)) { openRecorder(CaptureActivity.MODE_VIDEO) }
        item(getString(R.string.capture_mode_audio)) { openRecorder(CaptureActivity.MODE_AUDIO) }
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT,
        ).apply {
            gravity = Gravity.CENTER
        }
        runCatching { windowManager.addView(menu, params) }.onSuccess { modeMenu = menu }
    }

    private fun removeModeMenu() {
        val menu = modeMenu ?: return
        modeMenu = null
        runCatching { windowManager.removeView(menu) }
    }

    private fun launchConsent() {
        startActivity(
            CaptureActivity.consentIntent(this)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
        )
    }

    private fun startSession(resultCode: Int, data: Intent) {
        if (projection != null) return
        startCaptureForeground()
        val manager = getSystemService(MediaProjectionManager::class.java)
        val created = runCatching { manager.getMediaProjection(resultCode, data) }.getOrNull()
        if (created == null) {
            stopEverything()
            return
        }
        created.registerCallback(
            object : MediaProjection.Callback() {
                override fun onStop() {
                    releaseResources()
                }
            },
            handler,
        )
        val metrics = resources.displayMetrics
        val reader = ImageReader.newInstance(
            metrics.widthPixels,
            metrics.heightPixels,
            PixelFormat.RGBA_8888,
            2,
        )
        virtualDisplay = created.createVirtualDisplay(
            "reelvocab-capture",
            metrics.widthPixels,
            metrics.heightPixels,
            metrics.densityDpi,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
            reader.surface,
            null,
            null,
        )
        imageReader = reader
        projection = created
    }

    private fun captureFrame() {
        if (capturing) return
        val reader = imageReader
        if (reader == null) {
            launchConsent()
            return
        }
        capturing = true
        val image = reader.acquireLatestImage()
        if (image == null) {
            capturing = false
            handler.postDelayed({ captureFrame() }, RETRY_DELAY_MS)
            return
        }
        try {
            val plane = image.planes[0]
            val pixelStride = plane.pixelStride
            val rowStride = plane.rowStride
            val width = image.width
            val height = image.height
            val rowPadding = rowStride - pixelStride * width
            val padded = Bitmap.createBitmap(
                width + rowPadding / pixelStride,
                height,
                Bitmap.Config.ARGB_8888,
            )
            padded.copyPixelsFromBuffer(plane.buffer)
            val bitmap = Bitmap.createBitmap(padded, 0, 0, width, height)
            if (padded !== bitmap) padded.recycle()

            val capturedAt = System.currentTimeMillis()
            val file = File(cacheDir, "capture_$capturedAt.png")
            FileOutputStream(file).use { output ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, output)
            }
            bitmap.recycle()
            runCatching {
                val vaultDir = File(filesDir, "vault").apply { mkdirs() }
                val saved = File(vaultDir, "capture_$capturedAt.png")
                file.copyTo(saved, overwrite = true)
                scope.launch {
                    vault.add(
                        VaultItem(
                            id = "capture_$capturedAt",
                            uri = "file://${saved.absolutePath}",
                            title = "Capture " + SimpleDateFormat(
                                "MMM d, HH:mm",
                                Locale.getDefault(),
                            ).format(Date(capturedAt)),
                            kind = VaultKind.Image,
                            createdAtEpochMs = capturedAt,
                        ),
                    )
                }
            }

            startActivity(
                CaptureActivity.imageIntent(this, file.absolutePath)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            )
        } catch (error: Exception) {
            handler.postDelayed({ captureFrame() }, RETRY_DELAY_MS)
        } finally {
            image.close()
            capturing = false
        }
    }

    @SuppressLint("MissingPermission")
    private fun startRecording(resultCode: Int, data: Intent?, mode: String) {
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
        if (kind == RecordingKind.Audio && !hasMicPermission && data == null) {
            stopEverything()
            return
        }

        startRecordingForeground(kind, usesProjection = data != null, hasMic = hasMicPermission)

        val startedAt = System.currentTimeMillis()
        val vaultDir = File(filesDir, "vault").apply { mkdirs() }
        val extension = if (kind == RecordingKind.Video) "mp4" else "m4a"
        val file = File(vaultDir, "recording_$startedAt.$extension")

        var projection: MediaProjection? = null
        if (data != null) {
            val manager = getSystemService(MediaProjectionManager::class.java)
            projection = runCatching { manager.getMediaProjection(resultCode, data) }.getOrNull()
            if (kind == RecordingKind.Video && projection == null) {
                runCatching { file.delete() }
                stopEverything()
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
                outputFile = file,
                listener = object : RecordingListener {
                    override fun onLevel(level: Float) {
                        handler.post { recordingPanel?.pushLevel(level) }
                    }

                    override fun onSpeechSegment(segment: SpeechSegment) {
                        if (recordingEngine == null) return
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
                                        startMs = segment.startMs,
                                        recordingTitle = title,
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
                },
            )
        } catch (error: Throwable) {
            runCatching { projection?.stop() }
            runCatching { file.delete() }
            clearRecordingState()
            stopEverything()
            return
        }

        recordingEngine = engine
        try {
            engine.start()
        } catch (error: Throwable) {
            runCatching { projection?.stop() }
            runCatching { file.delete() }
            clearRecordingState()
            stopEverything()
            return
        }

        ensurePill()
        showRecordingPanel(kind)
        startTimer()
    }

    private fun stopRecording() {
        if (stoppingRecording) return
        val engine = recordingEngine
        if (engine == null) {
            stopEverything()
            return
        }
        stoppingRecording = true
        removeRecordingPanel()
        handler.removeCallbacks(timerRunnable)

        val file = recordingFile
        val startedAt = recordingStartedAtEpochMs
        val kind = recordingKind
        val projection = recordingProjection
        val title = recordingTitle

        recordingEngine = null
        recordingFile = null
        recordingProjection = null
        recordingKind = null
        transcriber = null

        scope.launch {
            val valid = runCatching { engine.stop() }.getOrDefault(false)
            runCatching { projection?.stop() }
            if (valid && file != null && file.length() > 0L && kind != null) {
                vault.add(
                    VaultItem(
                        id = "recording_$startedAt",
                        uri = "file://${file.absolutePath}",
                        title = "$title · " + SimpleDateFormat(
                            "MMM d, HH:mm",
                            Locale.getDefault(),
                        ).format(Date(startedAt)),
                        kind = if (kind == RecordingKind.Video) VaultKind.Video else VaultKind.Audio,
                        createdAtEpochMs = startedAt,
                    ),
                )
            } else {
                runCatching { file?.delete() }
            }
            withContext(Dispatchers.Main) {
                stoppingRecording = false
                stopEverything()
            }
        }
    }

    private fun releaseRecording() {
        handler.removeCallbacks(timerRunnable)
        removeRecordingPanel()
        val engine = recordingEngine
        recordingEngine = null
        if (engine != null) {
            val file = recordingFile
            val startedAt = recordingStartedAtEpochMs
            val kind = recordingKind
            val title = recordingTitle
            scope.launch {
                val valid = runCatching { engine.stop() }.getOrDefault(false)
                if (valid && file != null && file.length() > 0L && kind != null) {
                    vault.add(
                        VaultItem(
                            id = "recording_$startedAt",
                            uri = "file://${file.absolutePath}",
                            title = "$title · " + SimpleDateFormat(
                                "MMM d, HH:mm",
                                Locale.getDefault(),
                            ).format(Date(startedAt)),
                            kind = if (kind == RecordingKind.Video) VaultKind.Video else VaultKind.Audio,
                            createdAtEpochMs = startedAt,
                        ),
                    )
                }
            }
        }
        runCatching { recordingProjection?.stop() }
        clearRecordingState()
        transcriber = null
    }

    private fun clearRecordingState() {
        recordingFile = null
        recordingProjection = null
        recordingKind = null
        recordingStartedAtEpochMs = 0L
        recordingTitle = ""
        stoppingRecording = false
    }

    private fun showRecordingPanel(kind: RecordingKind) {
        if (recordingPanel != null) return
        if (!Settings.canDrawOverlays(this)) return
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
            onPause = { recordingEngine?.pause() },
            onResume = { recordingEngine?.resume() },
            onStop = { stopRecording() },
        )
        runCatching {
            windowManager.addView(panel.view, panel.windowParams())
        }.onSuccess {
            recordingPanel = panel
            panel.setElapsed(recordingEngine?.elapsedMs() ?: 0L)
        }
    }

    private fun removeRecordingPanel() {
        val panel = recordingPanel ?: return
        recordingPanel = null
        runCatching { windowManager.removeView(panel.view) }
    }

    private fun startTimer() {
        handler.removeCallbacks(timerRunnable)
        handler.post(timerRunnable)
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
            Intent(this, CaptureSessionService::class.java).setAction(ACTION_STOP_RECORD),
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

    private fun releaseProjection() {
        val current = projection
        projection = null
        if (current != null) runCatching { current.stop() }
        releaseResources()
    }

    private fun releaseResources() {
        virtualDisplay?.release()
        virtualDisplay = null
        imageReader?.close()
        imageReader = null
        projection = null
    }

    private fun removePill() {
        val current = pill ?: return
        pill = null
        runCatching { windowManager.removeView(current) }
    }

    private fun stopEverything() {
        releaseProjection()
        releaseRecording()
        removePill()
        removeModeMenu()
        setRunning(false)
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    companion object {
        private const val CHANNEL_ID = "capture_session"
        private const val NOTIFICATION_ID = 4102
        private const val ACTION_START_PILL = "app.reelvocab.capture.START_PILL"
        private const val ACTION_START_SESSION = "app.reelvocab.capture.START_SESSION"
        private const val ACTION_CAPTURE = "app.reelvocab.capture.CAPTURE"
        private const val ACTION_STOP = "app.reelvocab.capture.STOP"
        private const val ACTION_START_RECORDING = "app.reelvocab.capture.START_RECORDING"
        private const val ACTION_STOP_RECORD = "app.reelvocab.capture.STOP_RECORD"
        private const val EXTRA_RECORD_MODE = "record_mode"
        private const val EXTRA_RESULT_CODE = "result_code"
        private const val EXTRA_DATA = "projection_data"
        private const val EXTRA_CAPTURE_NOW = "capture_now"

        private const val PILL_SIZE_DP = 44
        private const val PILL_ICON_DP = 20
        private const val PILL_MARGIN_DP = 12
        private const val CAPTURE_DELAY_MS = 350L
        private const val RETRY_DELAY_MS = 150L
        private const val TIMER_INTERVAL_MS = 250L

        private val _running = MutableStateFlow(false)
        val runningFlow: StateFlow<Boolean> = _running.asStateFlow()
        val isRunning: Boolean get() = _running.value

        private fun setRunning(value: Boolean) {
            _running.value = value
        }

        fun startRecording(
            context: Context,
            resultCode: Int,
            data: Intent?,
            mode: String,
        ) {
            val intent = Intent(context, CaptureSessionService::class.java)
                .setAction(ACTION_START_RECORDING)
                .putExtra(EXTRA_RESULT_CODE, resultCode)
                .putExtra(EXTRA_RECORD_MODE, mode)
            if (data != null) intent.putExtra(EXTRA_DATA, data)
            ContextCompat.startForegroundService(context, intent)
        }

        fun startPill(context: Context) {
            ContextCompat.startForegroundService(
                context,
                Intent(context, CaptureSessionService::class.java)
                    .setAction(ACTION_START_PILL),
            )
        }

        fun startSession(
            context: Context,
            resultCode: Int,
            data: Intent,
            captureImmediately: Boolean,
        ) {
            ContextCompat.startForegroundService(
                context,
                Intent(context, CaptureSessionService::class.java)
                    .setAction(ACTION_START_SESSION)
                    .putExtra(EXTRA_RESULT_CODE, resultCode)
                    .putExtra(EXTRA_DATA, data)
                    .putExtra(EXTRA_CAPTURE_NOW, captureImmediately),
            )
        }

        fun requestCapture(context: Context) {
            context.startService(
                Intent(context, CaptureSessionService::class.java)
                    .setAction(ACTION_CAPTURE),
            )
        }

        fun stop(context: Context) {
            context.startService(
                Intent(context, CaptureSessionService::class.java)
                    .setAction(ACTION_STOP),
            )
        }
    }
}
