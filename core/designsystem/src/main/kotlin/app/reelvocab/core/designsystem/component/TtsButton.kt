package app.reelvocab.core.designsystem.component

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.VolumeUp
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
            imageVector = Icons.Filled.VolumeUp,
            contentDescription = contentDescription,
            tint = if (speaking) colors.teal else colors.gold,
        )
    }
}
