package app.reelvocab.feature.dictionary

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.reelvocab.core.designsystem.component.DictionaryDetailLabels
import app.reelvocab.core.designsystem.component.DictionaryDetailSheet
import app.reelvocab.core.designsystem.component.LoadingState
import app.reelvocab.core.designsystem.component.ReelRule
import app.reelvocab.core.designsystem.component.ReelScreenHeader
import app.reelvocab.core.designsystem.component.ReelSearchField
import app.reelvocab.core.designsystem.theme.ReelTheme
import app.reelvocab.core.domain.model.DictionaryEntry
import kotlinx.serialization.Serializable

@Serializable
data object DictionaryRoute

@Composable
fun DictionaryScreen(
    modifier: Modifier = Modifier,
    viewModel: DictionaryViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val colors = ReelTheme.colors

    LaunchedEffect(Unit) { viewModel.loadWordOfTheDay() }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = ReelTheme.spacing.screenH,
            end = ReelTheme.spacing.screenH,
            top = ReelTheme.spacing.xl,
            bottom = ReelTheme.spacing.screenBottom,
        ),
        verticalArrangement = Arrangement.spacedBy(ReelTheme.spacing.m),
    ) {
        item(key = "dictionary-header") {
            ReelScreenHeader(
                title = stringResource(R.string.dictionary_title),
                subtitle = stringResource(R.string.dictionary_subtitle),
            )
        }

        item(key = "dictionary-controls") {
            Column(verticalArrangement = Arrangement.spacedBy(ReelTheme.spacing.m)) {
                val wordOfTheDay = state.wordOfTheDay
                if (state.query.isBlank() && wordOfTheDay != null) {
                    WordOfTheDayCard(
                        entry = wordOfTheDay,
                        onClick = { viewModel.openDetail(wordOfTheDay) },
                    )
                }
                ReelSearchField(
                    value = state.query,
                    onValueChange = viewModel::onQueryChange,
                    placeholder = stringResource(R.string.dictionary_search_hint),
                    clearDescription = stringResource(R.string.dictionary_search_clear),
                )
            }
        }

        when {
            state.searching -> item(key = "searching") {
                LoadingState(message = stringResource(R.string.dictionary_searching))
            }

            state.aiError -> item(key = "ai-error") {
                Text(
                    text = stringResource(R.string.dictionary_ai_error),
                    style = ReelTheme.type.bodyM,
                    color = colors.error,
                )
            }

            state.error -> item(key = "error") {
                Text(
                    text = stringResource(R.string.dictionary_error),
                    style = ReelTheme.type.bodyM,
                    color = colors.error,
                )
            }

            state.query.trim().length < 2 -> item(key = "prompt") {
                PromptCard()
            }

            state.searched && state.results.isEmpty() -> item(key = "empty") {
                Text(
                    text = stringResource(R.string.dictionary_empty),
                    style = ReelTheme.type.bodyM,
                    color = colors.textSecondary,
                )
            }

            else -> items(state.results, key = { it.lemma }) { entry ->
                DictionaryResult(entry = entry, onClick = { viewModel.openDetail(entry) })
            }
        }
    }

    state.detail?.let { detail ->
        DictionaryDetailSheet(
            model = detail,
            loading = state.detailLoading,
            labels = DictionaryDetailLabels(
                title = stringResource(R.string.dictionary_detail_title),
                definitions = stringResource(R.string.dictionary_definitions),
                synonyms = stringResource(R.string.dictionary_synonyms),
                antonyms = stringResource(R.string.dictionary_antonyms),
                notFound = stringResource(R.string.dictionary_not_found),
                loading = stringResource(R.string.dictionary_lookup),
                close = stringResource(R.string.dictionary_close),
                save = stringResource(R.string.dictionary_save),
                saved = stringResource(R.string.dictionary_saved),
            ),
            onDismiss = viewModel::closeDetail,
            onWordClick = viewModel::openWord,
            onSave = viewModel::saveDetail,
            saving = state.saving,
            saved = state.saved,
        )
    }
}

@Composable
private fun PromptCard() {
    val colors = ReelTheme.colors
    Column {
        ReelRule()
        Spacer(Modifier.height(ReelTheme.spacing.xl))
        Text(
            text = stringResource(R.string.dictionary_prompt_title),
            style = ReelTheme.type.titleM,
            color = colors.textPrimary,
        )
        Spacer(Modifier.height(ReelTheme.spacing.xs))
        Text(
            text = stringResource(R.string.dictionary_prompt_body),
            style = ReelTheme.type.bodyM,
            color = colors.textSecondary,
        )
    }
}

@Composable
private fun DictionaryResult(
    entry: DictionaryEntry,
    onClick: () -> Unit,
) {
    val colors = ReelTheme.colors
    Column(modifier = Modifier.clickable(onClick = onClick)) {
        Text(
            text = entry.lemma,
            style = ReelTheme.type.wordmark,
            color = colors.textPrimary,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis,
        )
        val ipa = entry.ipa
        if (ipa != null) {
            Text(
                text = ipa,
                style = ReelTheme.type.ipa,
                color = colors.textSecondary,
            )
        }
        val partOfSpeech = entry.wordClass?.storageValue
        if (partOfSpeech != null) {
            Spacer(Modifier.height(ReelTheme.spacing.xxs))
            Text(
                text = partOfSpeech,
                style = ReelTheme.type.label,
                color = colors.secondary,
            )
        }
        val definition = entry.senses.firstOrNull()?.definition
        if (definition != null) {
            Spacer(Modifier.height(ReelTheme.spacing.s))
            Text(
                text = definition,
                style = ReelTheme.type.bodyM,
                color = colors.textSecondary,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
            )
        }
        val example = entry.senses.firstNotNullOfOrNull { sense ->
            sense.allExamples.firstOrNull()
        }
        if (example != null) {
            Spacer(Modifier.height(ReelTheme.spacing.xs))
            Text(
                text = "\u201C$example\u201D",
                style = ReelTheme.type.bodyM,
                color = colors.textTertiary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Spacer(Modifier.height(ReelTheme.spacing.l))
        ReelRule()
    }
}

@Composable
private fun WordOfTheDayCard(
    entry: DictionaryEntry,
    onClick: () -> Unit,
) {
    val colors = ReelTheme.colors
    Column(modifier = Modifier.clickable(onClick = onClick)) {
        ReelRule()
        Spacer(Modifier.height(ReelTheme.spacing.xl))
        Text(
            text = stringResource(R.string.dictionary_word_of_the_day).uppercase(),
            style = ReelTheme.type.sectionLabel,
            color = colors.accent,
        )
        Spacer(Modifier.height(ReelTheme.spacing.xs))
        Text(
            text = entry.lemma,
            style = ReelTheme.type.displayL,
            color = colors.textPrimary,
        )
        val partOfSpeech = entry.wordClass?.storageValue
        if (partOfSpeech != null) {
            Text(
                text = partOfSpeech,
                style = ReelTheme.type.label,
                color = colors.secondary,
            )
        }
        val definition = entry.senses.firstOrNull()?.definition
        if (definition != null) {
            Spacer(Modifier.height(ReelTheme.spacing.s))
            Text(
                text = definition,
                style = ReelTheme.type.bodyM,
                color = colors.textSecondary,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
            )
        }
        val example = entry.senses.firstNotNullOfOrNull { sense ->
            sense.allExamples.firstOrNull()
        }
        if (example != null) {
            Spacer(Modifier.height(ReelTheme.spacing.xs))
            Text(
                text = "\u201C$example\u201D",
                style = ReelTheme.type.bodyM,
                color = colors.textTertiary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Spacer(Modifier.height(ReelTheme.spacing.m))
        Text(
            text = stringResource(R.string.dictionary_word_of_the_day_cta),
            style = ReelTheme.type.label,
            color = colors.accent,
        )
        Spacer(Modifier.height(ReelTheme.spacing.xl))
    }
}
