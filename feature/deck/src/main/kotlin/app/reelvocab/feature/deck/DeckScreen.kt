package app.reelvocab.feature.deck

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import app.reelvocab.core.designsystem.component.EmptyState
import app.reelvocab.core.designsystem.component.ReelChip
import app.reelvocab.core.designsystem.illustration.ReelIllustration
import app.reelvocab.core.designsystem.theme.ReelTheme
import kotlinx.serialization.Serializable

@Serializable
enum class DeckFilter { All, Due, Words, Sentences }

@Serializable
data class DeckRoute(val filter: DeckFilter = DeckFilter.All)

@Immutable
data class DeckUiState(
    val filter: DeckFilter = DeckFilter.All,
    val cardCount: Int = 0,
)

@Composable
fun DeckScreen(
    state: DeckUiState,
    onFilterChange: (DeckFilter) -> Unit,
    onOpenLibrary: () -> Unit,
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
                text = stringResource(R.string.deck_title),
                style = ReelTheme.type.titleL,
                color = colors.textPrimary,
            )
        }

        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(ReelTheme.spacing.s),
            ) {
                DeckFilter.entries.forEach { filter ->
                    ReelChip(
                        text = stringResource(filter.labelRes()),
                        selected = state.filter == filter,
                        onClick = { onFilterChange(filter) },
                    )
                }
            }
        }

        if (state.cardCount == 0) {
            item {
                Column(modifier = Modifier.padding(top = ReelTheme.spacing.xxxl)) {
                    EmptyState(
                        title = stringResource(R.string.deck_empty_title),
                        body = stringResource(R.string.deck_empty_body),
                        ctaLabel = stringResource(R.string.deck_cta_library),
                        onCta = onOpenLibrary,
                        illustration = ReelIllustration.EmptyDeck,
                    )
                }
            }
        }
    }
}

private fun DeckFilter.labelRes(): Int = when (this) {
    DeckFilter.All -> R.string.deck_filter_all
    DeckFilter.Due -> R.string.deck_filter_due
    DeckFilter.Words -> R.string.deck_filter_words
    DeckFilter.Sentences -> R.string.deck_filter_sentences
}
