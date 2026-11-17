package app.reelvocab.core.designsystem.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.reelvocab.core.designsystem.theme.ReelTheme

enum class ReelButtonStyle { Primary, Secondary, Ghost, Destructive }

@Composable
fun ReelButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    style: ReelButtonStyle = ReelButtonStyle.Primary,
    enabled: Boolean = true,
    leadingIcon: ImageVector? = null,
    loading: Boolean = false,
) {
    val colors = ReelTheme.colors
    val shape = RoundedCornerShape(ReelTheme.radius.input)
    val size = modifier
        .height(ReelTheme.spacing.controlHeight)
        .fillMaxWidth()
    val active = enabled && !loading
    val contentPadding = PaddingValues(horizontal = ReelTheme.spacing.xl)
    val flatElevation = ButtonDefaults.buttonElevation(
        defaultElevation = 0.dp,
        pressedElevation = 0.dp,
        focusedElevation = 0.dp,
        hoveredElevation = 0.dp,
        disabledElevation = 0.dp,
    )

    val content: @Composable RowScope.() -> Unit = {
        if (loading) {
            CircularProgressIndicator(
                modifier = Modifier.size(18.dp),
                strokeWidth = 2.dp,
                color = LocalContentColor.current,
            )
        } else {
            if (leadingIcon != null) {
                Icon(leadingIcon, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(ReelTheme.spacing.s))
            }
            Text(
                text = text,
                style = ReelTheme.type.bodyL.copy(fontWeight = FontWeight.SemiBold),
            )
        }
    }

    when (style) {
        ReelButtonStyle.Primary -> Button(
            onClick = onClick,
            modifier = size,
            enabled = active,
            shape = shape,
            colors = ButtonDefaults.buttonColors(
                containerColor = colors.gold,
                contentColor = colors.onAccent,
                disabledContainerColor = colors.goldDim,
                disabledContentColor = colors.onAccent.copy(alpha = 0.5f),
            ),
            elevation = flatElevation,
            contentPadding = contentPadding,
            content = content,
        )

        ReelButtonStyle.Secondary -> OutlinedButton(
            onClick = onClick,
            modifier = size,
            enabled = active,
            shape = shape,
            border = BorderStroke(ReelTheme.spacing.hairline, colors.strokeSubtle),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = colors.textPrimary,
                disabledContentColor = colors.textTertiary,
            ),
            contentPadding = contentPadding,
            content = content,
        )

        ReelButtonStyle.Ghost -> TextButton(
            onClick = onClick,
            modifier = size,
            enabled = active,
            shape = shape,
            colors = ButtonDefaults.textButtonColors(
                contentColor = colors.teal,
                disabledContentColor = colors.textTertiary,
            ),
            contentPadding = contentPadding,
            content = content,
        )

        ReelButtonStyle.Destructive -> Button(
            onClick = onClick,
            modifier = size,
            enabled = active,
            shape = shape,
            colors = ButtonDefaults.buttonColors(
                containerColor = colors.rose,
                contentColor = colors.onAccent,
                disabledContainerColor = colors.rose.copy(alpha = 0.4f),
                disabledContentColor = colors.onAccent.copy(alpha = 0.5f),
            ),
            elevation = flatElevation,
            contentPadding = contentPadding,
            content = content,
        )
    }
}

@Composable
fun ReelChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    leadingIcon: ImageVector? = null,
) {
    val colors = ReelTheme.colors
    FilterChip(
        selected = selected,
        onClick = onClick,
        modifier = modifier,
        label = { Text(text = text, style = ReelTheme.type.bodyM) },
        shape = RoundedCornerShape(ReelTheme.radius.chip),
        leadingIcon = leadingIcon?.let { icon ->
            {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                )
            }
        },
        colors = FilterChipDefaults.filterChipColors(
            containerColor = colors.elevated,
            labelColor = colors.textSecondary,
            iconColor = colors.textSecondary,
            selectedContainerColor = colors.gold.copy(alpha = 0.16f),
            selectedLabelColor = colors.gold,
            selectedLeadingIconColor = colors.gold,
        ),
        border = FilterChipDefaults.filterChipBorder(
            enabled = true,
            selected = selected,
            borderColor = colors.strokeSubtle,
            selectedBorderColor = colors.gold.copy(alpha = 0.5f),
            disabledBorderColor = colors.strokeSubtle,
            disabledSelectedBorderColor = colors.goldDim,
            borderWidth = ReelTheme.spacing.hairline,
            selectedBorderWidth = ReelTheme.spacing.hairline,
        ),
    )
}
