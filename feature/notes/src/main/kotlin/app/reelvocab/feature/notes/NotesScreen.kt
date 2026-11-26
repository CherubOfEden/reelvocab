package app.reelvocab.feature.notes

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import app.reelvocab.core.designsystem.icon.ReelIcon
import app.reelvocab.core.designsystem.icon.ReelIcons
import app.reelvocab.core.designsystem.theme.ReelTheme
import kotlinx.serialization.Serializable

@Serializable
data object NotesRoute

@Composable
fun NotesScreen(modifier: Modifier = Modifier) {
    val colors = ReelTheme.colors
    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = ReelTheme.spacing.screenH)
            .padding(bottom = ReelTheme.spacing.screenBottom),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(ReelTheme.spacing.m),
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(colors.accent.copy(alpha = 0.10f)),
                contentAlignment = Alignment.Center,
            ) {
                ReelIcon(
                    imageVector = ReelIcons.NotebookPen,
                    contentDescription = null,
                    tint = colors.accent,
                    modifier = Modifier.size(24.dp),
                )
            }
            Spacer(Modifier.height(ReelTheme.spacing.xs))
            Text(
                text = stringResource(R.string.notes_title),
                style = ReelTheme.type.titleL,
                color = colors.textPrimary,
            )
            Text(
                text = stringResource(R.string.notes_placeholder),
                style = ReelTheme.type.bodyM,
                color = colors.textSecondary,
                textAlign = TextAlign.Center,
            )
        }
    }
}
