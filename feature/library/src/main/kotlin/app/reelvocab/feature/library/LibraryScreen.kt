package app.reelvocab.feature.library

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import app.reelvocab.core.designsystem.component.CardSurface
import app.reelvocab.core.designsystem.component.EmptyState
import app.reelvocab.core.designsystem.illustration.ReelIllustration
import app.reelvocab.core.designsystem.theme.ReelTheme
import kotlinx.serialization.Serializable

@Serializable
data object LibraryRoute

@Composable
fun LibraryScreen(
    onOpenDeck: () -> Unit,
    onOpenMedia: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = ReelTheme.colors

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = ReelTheme.spacing.screenH,
            end = ReelTheme.spacing.screenH,
            top = ReelTheme.spacing.xl,
            bottom = ReelTheme.spacing.screenBottom,
        ),
        verticalArrangement = Arrangement.spacedBy(ReelTheme.spacing.l),
    ) {
        item {
            Text(
                text = stringResource(R.string.library_title),
                style = ReelTheme.type.titleL,
                color = colors.textPrimary,
            )
        }

        item {
            CardSurface(onClick = onOpenMedia) {
                Text(
                    text = stringResource(R.string.library_media_title),
                    style = ReelTheme.type.titleM,
                    color = colors.textPrimary,
                )
                Spacer(Modifier.height(ReelTheme.spacing.xs))
                Text(
                    text = stringResource(R.string.library_media_body),
                    style = ReelTheme.type.bodyM,
                    color = colors.textSecondary,
                )
            }
        }

        item {
            EmptyState(
                title = stringResource(R.string.library_empty_title),
                body = stringResource(R.string.library_empty_body),
                ctaLabel = stringResource(R.string.library_cta_deck),
                onCta = onOpenDeck,
                illustration = ReelIllustration.Ticket,
            )
        }
    }
}
