package app.reelvocab.core.designsystem.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.reelvocab.core.designsystem.theme.ReelTheme

@Composable
fun StreakRing(
    current: Int,
    label: String,
    modifier: Modifier = Modifier,
    diameter: Dp = 84.dp,
    onClick: (() -> Unit)? = null,
) {
    val colors = ReelTheme.colors
    val progress = when {
        current <= 0 -> 0f
        current % 7 == 0 -> 1f
        else -> (current % 7) / 7f
    }
    Column(
        modifier = modifier.then(
            if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier,
        ),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier.size(diameter),
            contentAlignment = Alignment.Center,
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val stroke = 4.dp.toPx()
                val inset = stroke / 2f
                val arcSize = Size(size.width - stroke, size.height - stroke)
                drawArc(
                    color = colors.strokeSubtle,
                    startAngle = -90f,
                    sweepAngle = 360f,
                    useCenter = false,
                    topLeft = Offset(inset, inset),
                    size = arcSize,
                    style = Stroke(width = stroke),
                )
                if (progress > 0f) {
                    drawArc(
                        color = colors.accent.copy(alpha = 0.26f),
                        startAngle = -90f,
                        sweepAngle = progress * 360f,
                        useCenter = false,
                        topLeft = Offset(inset, inset),
                        size = arcSize,
                        style = Stroke(width = stroke * 2.4f, cap = StrokeCap.Round),
                    )
                    drawArc(
                        color = colors.accent,
                        startAngle = -90f,
                        sweepAngle = progress * 360f,
                        useCenter = false,
                        topLeft = Offset(inset, inset),
                        size = arcSize,
                        style = Stroke(width = stroke, cap = StrokeCap.Round),
                    )
                }
            }
            Text(
                text = current.toString(),
                style = ReelTheme.type.displayM.copy(fontSize = 28.sp, lineHeight = 30.sp),
                color = colors.textPrimary,
            )
        }
        Spacer(Modifier.height(ReelTheme.spacing.xs))
        Text(
            text = label,
            style = ReelTheme.type.label,
            color = colors.textSecondary,
        )
    }
}
