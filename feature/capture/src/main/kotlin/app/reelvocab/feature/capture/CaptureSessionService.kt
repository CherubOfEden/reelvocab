package app.reelvocab.feature.capture

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
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
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import androidx.compose.ui.graphics.toArgb
import app.reelvocab.core.designsystem.theme.ReelDarkColors
import dagger.hilt.android.AndroidEntryPoint
import java.io.File
import java.io.FileOutputStream
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

@AndroidEntryPoint
class CaptureSessionService : Service() {

    private lateinit var windowManager: WindowManager
    private var pill: View? = null
    private var projection: MediaProjection? = null
    private var virtualDisplay: VirtualDisplay? = null
    private var imageReader: ImageReader? = null
    private var capturing = false

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
        removePill()
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
        if (projection == null) {
            launchConsent()
        } else {
            captureFrame()
        }
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

            val file = File(cacheDir, "capture_${System.currentTimeMillis()}.png")
            FileOutputStream(file).use { output ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, output)
            }
            bitmap.recycle()

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
        removePill()
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
