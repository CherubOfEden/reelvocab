package app.reelvocab.core.designsystem.component

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import app.reelvocab.core.designsystem.theme.ReelTheme

@Composable
fun StatTile(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    delta: String? = null,
    accent: Color? = null,
) {
    val colors = ReelTheme.colors
    CardSurface(modifier = modifier) {
        Text(
            text = label,
            style = ReelTheme.type.label,
            color = colors.textTertiary,
        )
        Spacer(Modifier.height(ReelTheme.spacing.s))
        Text(
            text = value,
            style = ReelTheme.type.displayM,
            color = accent ?: colors.textPrimary,
        )
        if (delta != null) {
            Spacer(Modifier.height(ReelTheme.spacing.xs))
            Text(
                text = delta,
                style = ReelTheme.type.label,
                color = colors.green,
            )
        }
    }
}
