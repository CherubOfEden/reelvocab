package app.reelvocab.feature.capture

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.PixelFormat
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
class RecordingBubble(
    context: Context,
    onTap: () -> Unit,
) {
    private val density = context.resources.displayMetrics.density
    private val timer = TextView(context).apply {
        text = "00:00"
        textSize = 13f
        typeface = android.graphics.Typeface.MONOSPACE
        setTextColor(ReelDarkColors.textPrimary.toArgb())
    }
    private var lastElapsed = -1L

    val view: LinearLayout = LinearLayout(context).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        setPadding(dp(12), dp(8), dp(14), dp(8))
        background = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = dp(22).toFloat()
            setColor(ReelDarkColors.elevated.toArgb())
            setStroke(dp(1), ReelDarkColors.gold.toArgb())
        }
        elevation = dp(10).toFloat()
        contentDescription = context.getString(R.string.recording_bubble_description)
        setOnClickListener { onTap() }

        addView(
            View(context).apply {
                background = GradientDrawable().apply {
                    shape = GradientDrawable.OVAL
                    setColor(ReelDarkColors.rose.toArgb())
                }
                layoutParams = LinearLayout.LayoutParams(dp(8), dp(8)).apply {
                    marginEnd = dp(8)
                }
            },
        )
        addView(timer)
    }

    fun setElapsed(elapsedMs: Long) {
        if (elapsedMs == lastElapsed) return
        lastElapsed = elapsedMs
        val totalSeconds = elapsedMs / 1_000L
        val hours = totalSeconds / 3_600L
        val minutes = (totalSeconds % 3_600L) / 60L
        val seconds = totalSeconds % 60L
        timer.text = if (hours > 0L) {
            String.format(Locale.US, "%d:%02d:%02d", hours, minutes, seconds)
        } else {
            String.format(Locale.US, "%02d:%02d", minutes, seconds)
        }
    }

    fun windowParams(): WindowManager.LayoutParams =
        WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                WindowManager.LayoutParams.FLAG_SECURE,
            PixelFormat.TRANSLUCENT,
        ).apply {
            gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
            y = dp(52)
        }

    private fun dp(value: Int): Int = (value * density).roundToInt()
}
