package app.reelvocab.core.designsystem.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.reelvocab.core.designsystem.icon.ReelIcon
import app.reelvocab.core.designsystem.icon.ReelIcons
import app.reelvocab.core.designsystem.theme.ReelTheme
import java.util.Locale

@Composable
fun ReelScreenHeader(
    title: String,
    modifier: Modifier = Modifier,
    eyebrow: String? = null,
    subtitle: String? = null,
) {
    val colors = ReelTheme.colors
    Column(modifier = modifier.fillMaxWidth()) {
        if (eyebrow != null) {
            Text(
                text = eyebrow.uppercase(Locale.getDefault()),
                style = ReelTheme.type.sectionLabel,
                color = colors.accent,
            )
            Spacer(Modifier.height(ReelTheme.spacing.xs))
        }
        Text(
            text = title,
            style = ReelTheme.type.titleL,
            color = colors.textPrimary,
        )
        if (subtitle != null) {
            Spacer(Modifier.height(ReelTheme.spacing.s))
            Text(
                text = subtitle,
                style = ReelTheme.type.bodyM,
                color = colors.textSecondary,
            )
        }
    }
}

@Composable
fun ReelSectionLabel(
    text: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text = text.uppercase(Locale.getDefault()),
        style = ReelTheme.type.sectionLabel,
        color = ReelTheme.colors.textTertiary,
        modifier = modifier,
    )
}

@Composable
fun ReelRule(
    modifier: Modifier = Modifier,
) {
    HorizontalDivider(
        modifier = modifier,
        thickness = ReelTheme.spacing.hairline,
        color = ReelTheme.colors.divider,
    )
}

@Composable
fun ReelListRow(
    label: String,
    value: String? = null,
    modifier: Modifier = Modifier,
    leadingIcon: ImageVector? = null,
    showChevron: Boolean = false,
    onClick: (() -> Unit)? = null,
) {
    val colors = ReelTheme.colors
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
                .padding(vertical = ReelTheme.spacing.l),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(ReelTheme.spacing.m),
        ) {
            if (leadingIcon != null) {
                ReelIcon(
                    imageVector = leadingIcon,
                    contentDescription = null,
                    tint = colors.textSecondary,
                    modifier = Modifier.size(20.dp),
                )
            }
            Text(
                text = label,
                style = ReelTheme.type.bodyL,
                color = colors.textPrimary,
                modifier = Modifier.weight(1f),
            )
            if (value != null) {
                Text(
                    text = value,
                    style = ReelTheme.type.bodyM,
                    color = colors.textSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            if (showChevron) {
                ReelIcon(
                    imageVector = ReelIcons.ChevronRight,
                    contentDescription = null,
                    tint = colors.textTertiary,
                    modifier = Modifier.size(18.dp),
                )
            }
        }
        ReelRule()
    }
}
