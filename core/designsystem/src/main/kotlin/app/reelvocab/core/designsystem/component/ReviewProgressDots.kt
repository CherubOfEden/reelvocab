package app.reelvocab.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import app.reelvocab.core.designsystem.theme.ReelTheme

@Composable
fun ReviewProgressDots(
    total: Int,
    current: Int,
    modifier: Modifier = Modifier,
) {
    val colors = ReelTheme.colors
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(ReelTheme.spacing.xs),
    ) {
        repeat(total.coerceAtLeast(0)) { index ->
            val color = when {
                index < current -> colors.teal
                index == current -> colors.gold
                else -> colors.strokeSubtle
            }
            Box(
                modifier = Modifier
                    .size(if (index == current) 8.dp else 6.dp)
                    .clip(CircleShape)
                    .background(color),
            )
        }
    }
}
