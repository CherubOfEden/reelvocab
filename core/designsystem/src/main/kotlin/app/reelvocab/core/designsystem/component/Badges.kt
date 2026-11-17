package app.reelvocab.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import app.reelvocab.core.designsystem.theme.ReelTheme

@Composable
fun AIBadge(
    label: String,
    modifier: Modifier = Modifier,
) {
    val colors = ReelTheme.colors
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(ReelTheme.radius.chip))
            .background(colors.teal.copy(alpha = 0.14f))
            .padding(horizontal = 6.dp, vertical = 2.dp),
    ) {
        Text(text = label, style = ReelTheme.type.label, color = colors.teal)
    }
}

@Composable
fun DifficultyDots(
    level: Int,
    modifier: Modifier = Modifier,
    max: Int = 5,
) {
    val colors = ReelTheme.colors
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        repeat(max) { index ->
            Box(
                modifier = Modifier
                    .size(5.dp)
                    .clip(CircleShape)
                    .background(if (index < level) colors.gold else colors.strokeSubtle),
            )
        }
    }
}
