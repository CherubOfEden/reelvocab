package app.reelvocab.core.designsystem.theme

import android.os.Build
import android.view.HapticFeedbackConstants
import android.view.View
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Immutable

@Immutable
class ReelMotion(
    val standardMs: Int,
    val sheetStiffness: Float,
    val sheetDampingRatio: Float,
    val captureScaleFrom: Float,
    val pulseMs: Int,
    val standardEasing: Easing,
) {
    val standardSpec: FiniteAnimationSpec<Float>
        get() = tween(durationMillis = standardMs, easing = standardEasing)

    val sheetSpec: FiniteAnimationSpec<Float>
        get() = spring(
            dampingRatio = sheetDampingRatio,
            stiffness = sheetStiffness,
        )

    companion object {
        val Standard = ReelMotion(
            standardMs = 220,
            sheetStiffness = 380f,
            sheetDampingRatio = 0.85f,
            captureScaleFrom = 0.96f,
            pulseMs = 200,
            standardEasing = CubicBezierEasing(0.2f, 0.8f, 0.2f, 1f),
        )

        val Reduced = ReelMotion(
            standardMs = 0,
            sheetStiffness = 380f,
            sheetDampingRatio = 1f,
            captureScaleFrom = 1f,
            pulseMs = 0,
            standardEasing = LinearEasing,
        )
    }
}

class ReelHaptics internal constructor(private val view: View?) {
    fun tick() {
        view?.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
    }

    fun save() {
        view?.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
    }

    fun success() {
        view?.performHapticFeedback(confirmOrLongPress())
    }

    fun error() {
        view?.performHapticFeedback(rejectOrLongPress())
    }

    private fun confirmOrLongPress(): Int =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            HapticFeedbackConstants.CONFIRM
        } else {
            HapticFeedbackConstants.LONG_PRESS
        }

    private fun rejectOrLongPress(): Int =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            HapticFeedbackConstants.REJECT
        } else {
            HapticFeedbackConstants.LONG_PRESS
        }
}
