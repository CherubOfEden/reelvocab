package app.reelvocab.core.designsystem.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.unit.dp
import app.reelvocab.core.designsystem.theme.ReelTheme

@Composable
fun WaveformMini(
    amplitudes: List<Float>,
    active: Boolean,
    modifier: Modifier = Modifier,
) {
    val colors = ReelTheme.colors
    val barColor = if (active) colors.accent else colors.textTertiary
    Canvas(modifier = modifier.size(width = 72.dp, height = 24.dp)) {
        if (amplitudes.isEmpty()) return@Canvas
        val slot = size.width / amplitudes.size
        val barWidth = slot * 0.5f
        val radius = CornerRadius(barWidth / 2f, barWidth / 2f)
        amplitudes.forEachIndexed { index, raw ->
            val height = raw.coerceIn(0.06f, 1f) * size.height
            drawRoundRect(
                color = barColor,
                topLeft = Offset(index * slot, (size.height - height) / 2f),
                size = Size(barWidth, height),
                cornerRadius = radius,
            )
        }
    }
}
