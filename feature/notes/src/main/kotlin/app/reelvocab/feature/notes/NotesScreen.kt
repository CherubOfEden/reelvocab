package app.reelvocab.feature.notes

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
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
            verticalArrangement = Arrangement.spacedBy(ReelTheme.spacing.s),
        ) {
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
