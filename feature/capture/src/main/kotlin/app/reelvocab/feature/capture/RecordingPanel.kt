package app.reelvocab.feature.capture

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.LinearLayout
import android.widget.TextView
import androidx.compose.ui.graphics.toArgb
import app.reelvocab.core.designsystem.theme.ReelDarkColors
import java.util.Locale
import kotlin.math.roundToInt

@SuppressLint("ViewConstructor")
class RecordingPanel(
    context: Context,
    title: String,
    private val pauseLabel: String,
    private val resumeLabel: String,
    stopLabel: String,
    hideLabel: String,
    private val onPause: () -> Unit,
    private val onResume: () -> Unit,
    private val onStop: () -> Unit,
    private val onHide: () -> Unit,
) {
    private val density = context.resources.displayMetrics.density
    private val timer = TextView(context)
    private val pauseButton = actionButton(context, pauseLabel)
    private val hideButton = actionButton(context, hideLabel) { onHide() }
    private val waveform = WaveformView(context)
    private val countdown = TextView(context).apply {
        textSize = 38f
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        setTextColor(ReelDarkColors.accent.toArgb())
        gravity = Gravity.CENTER
        visibility = View.GONE
    }
    private var paused = false
    private var lastElapsed = -1L

    val view: LinearLayout = LinearLayout(context).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(dp(14), dp(10), dp(14), dp(10))
        background = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = dp(18).toFloat()
            setColor(ReelDarkColors.elevated.toArgb())
            setStroke(dp(1), ReelDarkColors.strokeSubtle.toArgb())
        }
        elevation = dp(12).toFloat()

        addView(
            LinearLayout(context).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                addView(
                    TextView(context).apply {
                        text = title.uppercase(Locale.getDefault())
                        textSize = 11f
                        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                        setTextColor(ReelDarkColors.accent.toArgb())
                        letterSpacing = 0.12f
                    },
                    LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f),
                )
                timer.apply {
                    text = "00:00"
                    textSize = 13f
                    typeface = Typeface.MONOSPACE
                    setTextColor(ReelDarkColors.textPrimary.toArgb())
                }.also { addView(it) }
            },
        )

        addView(
            countdown,
            LinearLayout.LayoutParams(dp(210), dp(44)).apply {
                topMargin = dp(4)
            },
        )

        addView(
            waveform,
            LinearLayout.LayoutParams(dp(210), dp(34)).apply {
                topMargin = dp(8)
            },
        )

        addView(
            LinearLayout(context).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                addView(pauseButton, buttonParams())
                addView(hideButton, buttonParams())
                addView(actionButton(context, stopLabel) { onStop() }, buttonParams())
            },
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT,
            ).apply {
                topMargin = dp(8)
                gravity = Gravity.END
            },
        )
    }

    init {
        pauseButton.setOnClickListener {
            paused = !paused
            pauseButton.text = if (paused) resumeLabel else pauseLabel
            if (paused) onPause() else onResume()
        }
    }

    fun setElapsed(elapsedMs: Long) {
        if (elapsedMs == lastElapsed) return
        lastElapsed = elapsedMs
        val totalSeconds = elapsedMs / 1000L
        val hours = totalSeconds / 3600L
        val minutes = (totalSeconds % 3600L) / 60L
        val seconds = totalSeconds % 60L
        timer.text = if (hours > 0L) {
            String.format(Locale.US, "%d:%02d:%02d", hours, minutes, seconds)
        } else {
            String.format(Locale.US, "%02d:%02d", minutes, seconds)
        }
    }

    fun setPaused(value: Boolean) {
        paused = value
        pauseButton.text = if (paused) resumeLabel else pauseLabel
    }

    fun setCountdown(remaining: Int) {
        countdown.text = remaining.toString()
        countdown.visibility = View.VISIBLE
        waveform.visibility = View.GONE
        pauseButton.isEnabled = false
        hideButton.isEnabled = false
    }

    fun setCountdownComplete() {
        countdown.visibility = View.GONE
        waveform.visibility = View.VISIBLE
        pauseButton.isEnabled = true
        hideButton.isEnabled = true
    }

    fun pushLevel(level: Float) {
        waveform.push(level)
    }

    private fun actionButton(
        context: Context,
        label: String,
        onClick: (() -> Unit)? = null,
    ): TextView = TextView(context).apply {
        text = label
        textSize = 12f
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        setTextColor(ReelDarkColors.textPrimary.toArgb())
        gravity = Gravity.CENTER
        setPadding(dp(14), dp(8), dp(14), dp(8))
        background = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = dp(12).toFloat()
            setColor(ReelDarkColors.surface.toArgb())
            setStroke(dp(1), ReelDarkColors.strokeSubtle.toArgb())
        }
        onClick?.let { click -> setOnClickListener { click() } }
    }

    private fun buttonParams(): LinearLayout.LayoutParams =
        LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT,
        ).apply { marginStart = dp(8) }

    private fun dp(value: Int): Int = (value * density).roundToInt()

    fun windowParams(): WindowManager.LayoutParams =
        WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT,
        ).apply {
            gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
            y = dp(52)
        }

    private class WaveformView(context: Context) : View(context) {
        private val levels = FloatArray(36)
        private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = ReelDarkColors.accent.toArgb()
        }
        private val idlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = ReelDarkColors.strokeSubtle.toArgb()
        }

        fun push(level: Float) {
            System.arraycopy(levels, 1, levels, 0, levels.size - 1)
            levels[levels.lastIndex] = level.coerceIn(0f, 1f)
            invalidate()
        }

        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)
            val center = height / 2f
            val gap = width.toFloat() / levels.size
            levels.forEachIndexed { index, level ->
                val barHeight = (height * (0.08f + level * 0.92f)).coerceAtLeast(height * 0.08f)
                val left = index * gap + gap * 0.24f
                val right = left + gap * 0.52f
                canvas.drawRoundRect(
                    left,
                    center - barHeight / 2f,
                    right,
                    center + barHeight / 2f,
                    gap * 0.25f,
                    gap * 0.25f,
                    if (level > 0.02f) paint else idlePaint,
                )
            }
        }
    }
}
