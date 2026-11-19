package app.reelvocab.core.designsystem.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import app.reelvocab.core.designsystem.theme.ReelTheme

@Immutable
data class DictionarySenseModel(
    val definition: String,
    val example: String? = null,
)

@Immutable
data class DictionaryDetailModel(
    val headword: String,
    val ipa: String? = null,
    val partOfSpeech: String? = null,
    val difficulty: Int? = null,
    val senses: List<DictionarySenseModel> = emptyList(),
    val synonyms: List<String> = emptyList(),
    val antonyms: List<String> = emptyList(),
    val fallbackMeaning: String? = null,
    val isKnown: Boolean = true,
)

@Immutable
data class DictionaryDetailLabels(
    val title: String,
    val definitions: String,
    val synonyms: String,
    val antonyms: String,
    val notFound: String,
    val loading: String,
    val close: String,
    val save: String = "",
    val saved: String = "",
)

@Composable
fun DictionaryDetailSheet(
    model: DictionaryDetailModel?,
    loading: Boolean,
    labels: DictionaryDetailLabels,
    onDismiss: () -> Unit,
    onWordClick: ((String) -> Unit)? = null,
    onSave: (() -> Unit)? = null,
    saving: Boolean = false,
    saved: Boolean = false,
) {
    val colors = ReelTheme.colors
    ReelSheet(onDismiss = onDismiss, title = labels.title) {
        when {
            loading -> LoadingState(message = labels.loading)

            model == null -> Text(
                text = labels.notFound,
                style = ReelTheme.type.bodyM,
                color = colors.textSecondary,
            )

            else -> {
                val entry = model
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                ) {
                Text(
                    text = entry.headword,
                    style = if (entry.headword.contains(' ')) {
                        ReelTheme.type.titleL
                    } else {
                        ReelTheme.type.displayM
                    },
                    color = colors.textPrimary,
                    modifier = Modifier.fillMaxWidth(),
                )
                if (entry.ipa != null) {
                    Text(
                        text = entry.ipa,
                        style = ReelTheme.type.ipa,
                        color = colors.textSecondary,
                    )
                }
                Row(
                    modifier = Modifier.padding(top = ReelTheme.spacing.xs),
                    horizontalArrangement = Arrangement.spacedBy(ReelTheme.spacing.s),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (entry.partOfSpeech != null) {
                        Text(
                            text = entry.partOfSpeech,
                            style = ReelTheme.type.label,
                            color = colors.teal,
                        )
                    }
                    if (entry.difficulty != null) {
                        DifficultyDots(level = entry.difficulty)
                    }
                }

                val senses = entry.senses.ifEmpty {
                    entry.fallbackMeaning
                        ?.takeIf { it.isNotBlank() }
                        ?.let { listOf(DictionarySenseModel(it)) }
                        .orEmpty()
                }
                if (senses.isNotEmpty()) {
                    Spacer(Modifier.height(ReelTheme.spacing.xl))
                    Text(
                        text = labels.definitions.uppercase(),
                        style = ReelTheme.type.label.copy(fontWeight = FontWeight.SemiBold),
                        color = colors.textTertiary,
                    )
                    senses.forEachIndexed { index, sense ->
                        Column(
                            modifier = Modifier.padding(top = ReelTheme.spacing.s),
                            verticalArrangement = Arrangement.spacedBy(ReelTheme.spacing.xxs),
                        ) {
                            Text(
                                text = "${index + 1}. ${sense.definition}",
                                style = ReelTheme.type.bodyL,
                                color = colors.textPrimary,
                            )
                            if (sense.example != null) {
                                Text(
                                    text = "\u201C${sense.example}\u201D",
                                    style = ReelTheme.type.bodyM,
                                    color = colors.textSecondary,
                                )
                            }
                        }
                    }
                } else if (!entry.isKnown) {
                    Spacer(Modifier.height(ReelTheme.spacing.l))
                    Text(
                        text = labels.notFound,
                        style = ReelTheme.type.bodyM,
                        color = colors.textSecondary,
                    )
                }

                if (entry.synonyms.isNotEmpty() || entry.antonyms.isNotEmpty()) {
                    Spacer(Modifier.height(ReelTheme.spacing.xl))
                    Column(verticalArrangement = Arrangement.spacedBy(ReelTheme.spacing.s)) {
                        if (entry.synonyms.isNotEmpty()) {
                            ThesaurusRow(
                                label = labels.synonyms,
                                words = entry.synonyms,
                                tint = colors.teal,
                                onWordClick = onWordClick,
                            )
                        }
                        if (entry.antonyms.isNotEmpty()) {
                            ThesaurusRow(
                                label = labels.antonyms,
                                words = entry.antonyms,
                                tint = colors.rose,
                                onWordClick = onWordClick,
                            )
                        }
                    }
                }

                if (onSave != null && labels.save.isNotEmpty()) {
                    Spacer(Modifier.height(ReelTheme.spacing.xxl))
                    ReelButton(
                        text = if (saved) labels.saved else labels.save,
                        onClick = onSave,
                        loading = saving,
                        enabled = !saved,
                    )
                    Spacer(Modifier.height(ReelTheme.spacing.s))
                    ReelButton(
                        text = labels.close,
                        onClick = onDismiss,
                        style = ReelButtonStyle.Ghost,
                    )
                } else {
                Spacer(Modifier.height(ReelTheme.spacing.xxl))
                ReelButton(
                    text = labels.close,
                    onClick = onDismiss,
                    style = ReelButtonStyle.Secondary,
                )
                }
                }
            }
        }
    }
}
