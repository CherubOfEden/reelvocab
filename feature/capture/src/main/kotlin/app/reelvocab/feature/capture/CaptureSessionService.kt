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
import android.media.MediaRecorder
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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

@AndroidEntryPoint
class CaptureSessionService : Service() {

    @Inject lateinit var vault: MediaVaultRepository

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private lateinit var windowManager: WindowManager
    private var pill: View? = null
    private var projection: MediaProjection? = null
    private var virtualDisplay: VirtualDisplay? = null
    private var imageReader: ImageReader? = null
    private var capturing = false
    private var recorder: MediaRecorder? = null
    private var recordingFile: File? = null
    private var recordingDisplay: VirtualDisplay? = null
    private var recordingProjection: MediaProjection? = null
    private var recordingMode: String? = null
    private var modeMenu: View? = null

    private val handler = Handler(Looper.getMainLooper())

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
                ensurePill()
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
                    ensurePill()
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
        scope.cancel()
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

    private fun ensurePill() {
        if (pill != null) return
        if (!Settings.canDrawOverlays(this)) {
            stopEverything()
            return
        }

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
        windowManager.addView(frame, params)
        pill = frame
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
                    stopEverything()
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
        if (recorder != null) return
        recordingMode = mode
        startRecordingForeground(mode)
        val metrics = resources.displayMetrics
        val startedAt = System.currentTimeMillis()
        val vaultDir = File(filesDir, "vault").apply { mkdirs() }
        val extension = if (mode == CaptureActivity.MODE_VIDEO) "mp4" else "m4a"
        val file = File(vaultDir, "recording_$startedAt.$extension")
        recordingFile = file
        val mediaRecorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            MediaRecorder(this)
        } else {
            @Suppress("DEPRECATION")
            MediaRecorder()
        }
        recorder = mediaRecorder
        try {
            val wantsAudio = ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.RECORD_AUDIO,
            ) == PackageManager.PERMISSION_GRANTED
            if (mode == CaptureActivity.MODE_VIDEO) {
                if (wantsAudio) {
                    mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC)
                }
                mediaRecorder.setVideoSource(MediaRecorder.VideoSource.SURFACE)
                mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                mediaRecorder.setOutputFile(file.absolutePath)
                mediaRecorder.setVideoSize(
                    metrics.widthPixels.coerceAtLeast(2),
                    metrics.heightPixels.coerceAtLeast(2),
                )
                mediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264)
                mediaRecorder.setVideoFrameRate(30)
                mediaRecorder.setVideoEncodingBitRate(6_000_000)
                if (wantsAudio) {
                    mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                    mediaRecorder.setAudioEncodingBitRate(128_000)
                    mediaRecorder.setAudioSamplingRate(44_100)
                }
                mediaRecorder.prepare()
                val projectionData = data ?: error("Screen permission missing")
                val manager = getSystemService(MediaProjectionManager::class.java)
                val created = manager.getMediaProjection(resultCode, projectionData)
                    ?: error("Screen recording could not start")
                created.registerCallback(
                    object : MediaProjection.Callback() {
                        override fun onStop() {
                            stopRecording()
                        }
                    },
                    handler,
                )
                recordingProjection = created
                recordingDisplay = created.createVirtualDisplay(
                    "reelvocab-recording",
                    metrics.widthPixels,
                    metrics.heightPixels,
                    metrics.densityDpi,
                    DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                    mediaRecorder.surface,
                    null,
                    null,
                )
                mediaRecorder.start()
            } else {
                if (!wantsAudio) error("Microphone permission missing")
                mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC)
                mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                mediaRecorder.setOutputFile(file.absolutePath)
                mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                mediaRecorder.setAudioEncodingBitRate(128_000)
                mediaRecorder.setAudioSamplingRate(44_100)
                mediaRecorder.setAudioChannels(1)
                mediaRecorder.prepare()
                mediaRecorder.start()
            }
        } catch (error: Exception) {
            releaseRecording()
            runCatching { file.delete() }
            stopEverything()
        }
    }

    private fun stopRecording() {
        val mediaRecorder = recorder ?: return
        recorder = null
        val file = recordingFile
        val startedAt = System.currentTimeMillis()
        val mode = recordingMode
        recordingMode = null
        var valid = true
        runCatching { mediaRecorder.stop() }.onFailure {
            valid = false
            file?.delete()
        }
        runCatching { mediaRecorder.reset() }
        runCatching { mediaRecorder.release() }
        releaseRecording()
        if (valid && file != null && file.length() > 0L) {
            scope.launch {
                vault.add(
                    VaultItem(
                        id = "recording_$startedAt",
                        uri = "file://${file.absolutePath}",
                        title = "Recording " + SimpleDateFormat(
                            "MMM d, HH:mm",
                            Locale.getDefault(),
                        ).format(Date(startedAt)),
                        kind = if (mode == CaptureActivity.MODE_VIDEO) {
                            VaultKind.Video
                        } else {
                            VaultKind.Audio
                        },
                        createdAtEpochMs = startedAt,
                    ),
                )
            }
        }
        stopEverything()
    }

    private fun releaseRecording() {
        runCatching { recorder?.reset() }
        runCatching { recorder?.release() }
        recorder = null
        recordingDisplay?.release()
        recordingDisplay = null
        runCatching { recordingProjection?.stop() }
        recordingProjection = null
        recordingFile = null
    }

    private fun startRecordingForeground(mode: String) {
        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            if (mode == CaptureActivity.MODE_VIDEO) {
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION
            } else {
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
            }
        } else {
            0
        }
        ServiceCompat.startForeground(
            this,
            NOTIFICATION_ID,
            buildRecordingNotification(),
            type,
        )
    }

    private fun buildRecordingNotification(): Notification {
        val stopIntent = PendingIntent.getService(
            this,
            2,
            Intent(this, CaptureSessionService::class.java).setAction(ACTION_STOP_RECORD),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.capture_recording))
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
