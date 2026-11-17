package app.reelvocab.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.reelvocab.R
import app.reelvocab.core.designsystem.component.ReelButton
import app.reelvocab.core.designsystem.component.ReelButtonStyle
import app.reelvocab.core.designsystem.component.ThesaurusRow
import app.reelvocab.core.designsystem.theme.ReelTheme
import app.reelvocab.core.domain.model.DictionaryEntry
import kotlinx.serialization.Serializable

@Serializable
data object DictionaryProbeRoute

@Composable
fun DictionaryProbeScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: DictionaryProbeViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val colors = ReelTheme.colors
    val keyboard = LocalSoftwareKeyboardController.current

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = ReelTheme.spacing.screenH),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = ReelTheme.spacing.m),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = null,
                    tint = colors.textPrimary,
                )
            }
            Text(
                text = stringResource(R.string.probe_title),
                style = ReelTheme.type.titleL,
                color = colors.textPrimary,
            )
        }

        Spacer(Modifier.height(ReelTheme.spacing.s))

        OutlinedTextField(
            value = state.query,
            onValueChange = viewModel::onQueryChange,
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            label = { Text(stringResource(R.string.probe_hint)) },
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(
                onSearch = {
                    keyboard?.hide()
                    viewModel.search()
                },
            ),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = colors.textPrimary,
                unfocusedTextColor = colors.textPrimary,
                focusedContainerColor = colors.surface,
                unfocusedContainerColor = colors.surface,
                focusedBorderColor = colors.gold,
                unfocusedBorderColor = colors.strokeSubtle,
                focusedLabelColor = colors.gold,
                unfocusedLabelColor = colors.textTertiary,
                cursorColor = colors.gold,
            ),
        )

        Spacer(Modifier.height(ReelTheme.spacing.m))

        ReelButton(
            text = stringResource(R.string.probe_action),
            onClick = { viewModel.search() },
            style = ReelButtonStyle.Secondary,
            enabled = state.query.isNotBlank(),
            loading = state.loading,
        )

        Spacer(Modifier.height(ReelTheme.spacing.l))

        when {
            state.error -> Text(
                text = stringResource(R.string.probe_error),
                style = ReelTheme.type.bodyM,
                color = colors.rose,
            )

            state.loading -> Text(
                text = stringResource(R.string.probe_loading),
                style = ReelTheme.type.bodyM,
                color = colors.textSecondary,
            )

            state.searched && state.exact == null && state.results.isEmpty() -> Text(
                text = stringResource(R.string.probe_empty),
                style = ReelTheme.type.bodyM,
                color = colors.textSecondary,
            )

            else -> LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = ReelTheme.spacing.screenBottom),
                verticalArrangement = Arrangement.spacedBy(ReelTheme.spacing.l),
            ) {
                val exact = state.exact
                if (exact != null) {
                    item(key = "exact") {
                        Column(verticalArrangement = Arrangement.spacedBy(ReelTheme.spacing.s)) {
                            ProbeSectionLabel(text = stringResource(R.string.probe_exact))
                            EntryBlock(entry = exact)
                        }
                    }
                }
                if (state.results.isNotEmpty()) {
                    item(key = "matches-label") {
                        ProbeSectionLabel(text = stringResource(R.string.probe_matches))
                    }
                    items(state.results, key = { "${it.lemma}:${it.wordClass}" }) { entry ->
                        CompactEntry(entry = entry)
                    }
                }
            }
        }
    }
}

@Composable
private fun ProbeSectionLabel(text: String) {
    Text(
        text = text.uppercase(),
        style = ReelTheme.type.label.copy(fontWeight = FontWeight.SemiBold),
        color = ReelTheme.colors.textTertiary,
    )
}

@Composable
private fun EntryBlock(entry: DictionaryEntry) {
    val colors = ReelTheme.colors
    Column(verticalArrangement = Arrangement.spacedBy(ReelTheme.spacing.s)) {
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                text = entry.lemma,
                style = ReelTheme.type.titleL,
                color = colors.textPrimary,
            )
            if (entry.ipa != null) {
                Spacer(Modifier.height(ReelTheme.spacing.xxs))
                Text(
                    text = "  ${entry.ipa}",
                    style = ReelTheme.type.ipa,
                    color = colors.textSecondary,
                )
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(ReelTheme.spacing.s)) {
            entry.wordClass?.let { wordClass ->
                Text(
                    text = wordClass.storageValue,
                    style = ReelTheme.type.label,
                    color = colors.teal,
                )
            }
            entry.difficulty?.let { difficulty ->
                Text(
                    text = "level $difficulty",
                    style = ReelTheme.type.label,
                    color = colors.textTertiary,
                )
            }
            entry.frequencyRank?.let { rank ->
                Text(
                    text = "rank $rank",
                    style = ReelTheme.type.label,
                    color = colors.textTertiary,
                )
            }
        }
        entry.senses.forEachIndexed { index, sense ->
            Column(verticalArrangement = Arrangement.spacedBy(ReelTheme.spacing.xxs)) {
                Text(
                    text = "${index + 1}. ${sense.definition}",
                    style = ReelTheme.type.bodyM,
                    color = colors.textPrimary,
                )
                sense.example?.let { example ->
                    Text(
                        text = "\u201C$example\u201D",
                        style = ReelTheme.type.bodyM,
                        color = colors.textSecondary,
                    )
                }
            }
        }
        if (entry.thesaurus.synonyms.isNotEmpty()) {
            ThesaurusRow(
                label = stringResource(R.string.card_synonyms),
                words = entry.thesaurus.synonyms,
                tint = colors.teal,
            )
        }
        if (entry.thesaurus.antonyms.isNotEmpty()) {
            ThesaurusRow(
                label = stringResource(R.string.card_antonyms),
                words = entry.thesaurus.antonyms,
                tint = colors.rose,
            )
        }
    }
}

@Composable
private fun CompactEntry(entry: DictionaryEntry) {
    val colors = ReelTheme.colors
    Column(verticalArrangement = Arrangement.spacedBy(ReelTheme.spacing.xxs)) {
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                text = entry.lemma,
                style = ReelTheme.type.bodyL.copy(fontWeight = FontWeight.Medium),
                color = colors.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            entry.ipa?.let { ipa ->
                Text(
                    text = "  $ipa",
                    style = ReelTheme.type.ipa,
                    color = colors.textSecondary,
                )
            }
        }
        entry.senses.firstOrNull()?.let { sense ->
            Text(
                text = sense.definition,
                style = ReelTheme.type.bodyM,
                color = colors.textSecondary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
        HorizontalDivider(color = colors.strokeSubtle)
    }
}
