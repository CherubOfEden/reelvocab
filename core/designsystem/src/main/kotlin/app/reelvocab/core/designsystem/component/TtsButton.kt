package app.reelvocab.core.designsystem.component

import app.reelvocab.core.designsystem.icon.ReelIcons

import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import app.reelvocab.core.designsystem.theme.ReelTheme

@Composable
fun TtsButton(
    onClick: () -> Unit,
    speaking: Boolean,
    contentDescription: String,
    modifier: Modifier = Modifier,
) {
    val colors = ReelTheme.colors
    IconButton(onClick = onClick, modifier = modifier) {
        Icon(
            imageVector = ReelIcons.Volume2,
            contentDescription = contentDescription,
            tint = if (speaking) colors.secondary else colors.accent,
        )
    }
}
