package app.reelvocab.core.designsystem.component

import app.reelvocab.core.designsystem.icon.ReelIcons

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
            .background(colors.secondary.copy(alpha = 0.14f))
            .padding(horizontal = 6.dp, vertical = 2.dp),
    ) {
        Text(text = label, style = ReelTheme.type.label, color = colors.secondary)
    }
}

@Composable
fun TagChip(
    text: String,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
) {
    val colors = ReelTheme.colors
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(ReelTheme.radius.chip))
            .background(colors.accent.copy(alpha = 0.12f))
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = ReelTheme.spacing.s, vertical = ReelTheme.spacing.xxs),
    ) {
        Text(
            text = text,
            style = ReelTheme.type.label,
            color = colors.accent,
            maxLines = 1,
        )
    }
}

@Composable
fun EditableTagChip(
    text: String,
    removeDescription: String,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = ReelTheme.colors
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(ReelTheme.radius.chip))
            .background(colors.accent.copy(alpha = 0.12f))
            .padding(start = ReelTheme.spacing.s),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = text,
            style = ReelTheme.type.label,
            color = colors.accent,
        )
        Spacer(Modifier.width(ReelTheme.spacing.xxs))
        IconButton(
            onClick = onRemove,
            modifier = Modifier.size(28.dp),
        ) {
            Icon(
                imageVector = ReelIcons.X,
                contentDescription = removeDescription,
                tint = colors.accent,
                modifier = Modifier.size(14.dp),
            )
        }
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
                    .background(if (index < level) colors.accent else colors.strokeSubtle),
            )
        }
    }
}
