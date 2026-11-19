package app.reelvocab.feature.deck

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.reelvocab.core.designsystem.component.ConfirmDestructive
import app.reelvocab.core.designsystem.component.DictionaryDetailLabels
import app.reelvocab.core.designsystem.component.DictionaryDetailSheet
import app.reelvocab.core.designsystem.component.EmptyState
import app.reelvocab.core.designsystem.component.LoadingState
import app.reelvocab.core.designsystem.component.ReelChip
import app.reelvocab.core.designsystem.component.ReelSearchField
import app.reelvocab.core.designsystem.component.TagEditorLabels
import app.reelvocab.core.designsystem.component.TagEditorSheet
import app.reelvocab.core.designsystem.component.WordCard
import app.reelvocab.core.designsystem.component.WordCardLabels
import app.reelvocab.core.designsystem.component.WordCardModel
import app.reelvocab.core.designsystem.illustration.ReelIllustration
import app.reelvocab.core.designsystem.theme.ReelColors
import app.reelvocab.core.designsystem.theme.ReelTheme
import app.reelvocab.core.domain.model.Card
import kotlinx.serialization.Serializable

@Serializable
enum class DeckFilter { All, Favorites, Due, Words, Sentences }

@Serializable
data class DeckRoute(val filter: DeckFilter = DeckFilter.All)

@Composable
fun DeckScreen(
    onOpenLibrary: () -> Unit,
    modifier: Modifier = Modifier,
    initialFilter: DeckFilter = DeckFilter.All,
    viewModel: DeckViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val colors = ReelTheme.colors
    var pendingDelete by remember { mutableStateOf<Card?>(null) }

    LaunchedEffect(initialFilter) {
        viewModel.applyInitialFilter(initialFilter)
        viewModel.refreshDue()
    }

    val visibleCards = remember(state.cards, state.filter, state.dueCardIds, state.query) {
        filterDeckCards(
            cards = state.cards,
            filter = state.filter,
            dueCardIds = state.dueCardIds,
            query = state.query,
        )
    }
    val cardLabels = WordCardLabels(
        ai = stringResource(R.string.deck_ai_badge),
        playPronunciation = stringResource(R.string.deck_play),
        tag = stringResource(R.string.deck_edit_tags),
        more = stringResource(R.string.deck_more),
        synonyms = stringResource(R.string.deck_synonyms),
        antonyms = stringResource(R.string.deck_antonyms),
        delete = stringResource(R.string.deck_delete),
        favorite = stringResource(R.string.deck_favorite),
        unfavorite = stringResource(R.string.deck_unfavorite),
    )

    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
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
                if (!state.loading && state.cards.isNotEmpty()) {
                    Spacer(Modifier.height(ReelTheme.spacing.xxs))
                    Text(
                        text = pluralStringResource(
                            R.plurals.deck_count,
                            state.cards.size,
                            state.cards.size,
                        ),
                        style = ReelTheme.type.label,
                        color = colors.textTertiary,
                    )
                }
            }

            item {
                ReelSearchField(
                    value = state.query,
                    onValueChange = viewModel::onQueryChange,
                    placeholder = stringResource(R.string.deck_search_hint),
                    clearDescription = stringResource(R.string.deck_search_clear),
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
                            onClick = { viewModel.setFilter(filter) },
                        )
                    }
                }
            }

            when {
                state.loading -> item(key = "loading") {
                    LoadingState()
                }

                state.cards.isEmpty() -> item(key = "empty") {
                    Column {
                        EmptyState(
                            title = stringResource(R.string.deck_empty_title),
                            body = stringResource(R.string.deck_empty_body),
                            ctaLabel = stringResource(R.string.deck_cta_library),
                            onCta = onOpenLibrary,
                            illustration = ReelIllustration.EmptyDeck,
                        )
                    }
                }

                visibleCards.isEmpty() -> item(key = "no-results") {
                    Column {
                        EmptyState(
                            title = stringResource(R.string.deck_no_results_title),
                            body = stringResource(R.string.deck_no_results_body),
                            ctaLabel = stringResource(R.string.deck_clear_search),
                            onCta = {
                                viewModel.onQueryChange("")
                                viewModel.setFilter(DeckFilter.All)
                            },
                            illustration = ReelIllustration.Ticket,
                        )
                    }
                }

                else -> items(visibleCards, key = { it.id }) { card ->
                    WordCard(
                        model = card.toWordCardModel(colors),
                        labels = cardLabels,
                        onClick = { viewModel.openDetail(card) },
                        onTts = {},
                        onTag = { viewModel.openTagEditor(card) },
                        onMore = { viewModel.openDetail(card) },
                        onDelete = { pendingDelete = card },
                        onFavorite = { viewModel.toggleFavorite(card) },
                    )
                }
            }
        }

        state.detail?.let { detail ->
            DictionaryDetailSheet(
                model = detail,
                loading = state.detailLoading,
                labels = DictionaryDetailLabels(
                    title = stringResource(R.string.deck_dictionary_title),
                    definitions = stringResource(R.string.deck_definitions),
                    synonyms = stringResource(R.string.deck_synonyms),
                    antonyms = stringResource(R.string.deck_antonyms),
                    notFound = stringResource(R.string.deck_not_found),
                    loading = stringResource(R.string.deck_lookup),
                    close = stringResource(R.string.deck_close),
                ),
                onDismiss = viewModel::closeDetail,
            )
        }

        state.tagEditor?.let { editor ->
            TagEditorSheet(
                model = editor,
                labels = TagEditorLabels(
                    title = stringResource(R.string.deck_tags_title),
                    hint = stringResource(R.string.deck_tag_hint),
                    add = stringResource(R.string.deck_tag_add),
                    save = stringResource(R.string.deck_tag_save),
                    empty = stringResource(R.string.deck_tag_empty),
                    removeDescription = stringResource(R.string.deck_tag_remove),
                ),
                onDraftChange = viewModel::onTagDraftChange,
                onAdd = viewModel::addTag,
                onRemove = viewModel::removeTag,
                onSave = viewModel::saveTags,
                onDismiss = viewModel::closeTagEditor,
            )
        }

        pendingDelete?.let { card ->
            ConfirmDestructive(
                title = stringResource(R.string.deck_delete_title),
                body = stringResource(R.string.deck_delete_body, card.headword),
                confirmLabel = stringResource(R.string.deck_delete_confirm),
                dismissLabel = stringResource(R.string.deck_delete_cancel),
                onConfirm = {
                    viewModel.deleteCard(card.id)
                    pendingDelete = null
                },
                onDismiss = { pendingDelete = null },
            )
        }
    }
}

private fun Card.toWordCardModel(colors: ReelColors): WordCardModel = WordCardModel(
    headword = headword,
    ipa = ipa,
    partOfSpeech = wordClass?.storageValue,
    meaning = meaning,
    example = example,
    sentence = contextLine,
    target = headword,
    sourceTitle = sourceTitle,
    sourceTimestampMs = sourceTimestampMs,
    sourceTint = colors.teal,
    isAiContext = false,
    hasAudio = hasAudio,
    difficulty = null,
    tags = tags,
    isFavorite = isFavorite,
)

private fun DeckFilter.labelRes(): Int = when (this) {
    DeckFilter.All -> R.string.deck_filter_all
    DeckFilter.Favorites -> R.string.deck_filter_favorites
    DeckFilter.Due -> R.string.deck_filter_due
    DeckFilter.Words -> R.string.deck_filter_words
    DeckFilter.Sentences -> R.string.deck_filter_sentences
}
