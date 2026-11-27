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
    val examples: List<String> = emptyList(),
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
    val fallbackWarning: String? = null,
    val isKnown: Boolean = true,
    val translation: String? = null,
    val translationLoading: Boolean = false,
    val translationError: Boolean = false,
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
    val playPronunciation: String = "",
    val translate: String = "",
    val translating: String = "",
    val translation: String = "",
    val translationError: String = "",
)

@Composable
fun DictionaryDetailSheet(
    model: DictionaryDetailModel?,
    loading: Boolean,
    labels: DictionaryDetailLabels,
    onDismiss: () -> Unit,
    onWordClick: ((String) -> Unit)? = null,
    onTranslate: (() -> Unit)? = null,
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
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = entry.headword,
                        style = if (entry.headword.contains(' ')) {
                            ReelTheme.type.titleL
                        } else {
                            ReelTheme.type.displayM
                        },
                        color = colors.textPrimary,
                        modifier = Modifier.weight(1f),
                    )
                    if (labels.playPronunciation.isNotEmpty()) {
                        ReelSpeakButton(
                            text = entry.headword,
                            contentDescription = labels.playPronunciation,
                        )
                    }
                }
                if (entry.ipa != null) {
                    Text(
                        text = entry.ipa,
                        style = ReelTheme.type.ipa,
                        color = colors.textSecondary,
                    )
                }
                val warning = entry.fallbackWarning?.takeIf { it.isNotBlank() }
                if (warning != null) {
                    Spacer(Modifier.height(ReelTheme.spacing.m))
                    Text(
                        text = warning,
                        style = ReelTheme.type.bodyM,
                        color = colors.accent,
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
                            color = colors.secondary,
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
                            val examples = sense.examples.ifEmpty {
                                listOfNotNull(sense.example)
                            }
                            examples.forEach { example ->
                                Text(
                                    text = "\u201C$example\u201D",
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

                val translateAction = onTranslate
                if (translateAction != null && labels.translate.isNotEmpty()) {
                    Spacer(Modifier.height(ReelTheme.spacing.l))
                    val translated = entry.translation?.trim()?.takeIf { it.isNotEmpty() }
                    when {
                        entry.translationLoading -> {
                            Text(
                                text = labels.translating,
                                style = ReelTheme.type.bodyM,
                                color = colors.textSecondary,
                            )
                        }

                        entry.translationError -> {
                            Text(
                                text = labels.translationError,
                                style = ReelTheme.type.bodyM,
                                color = colors.error,
                            )
                            Spacer(Modifier.height(ReelTheme.spacing.s))
                            ReelButton(
                                text = labels.translate,
                                onClick = translateAction,
                                style = ReelButtonStyle.Secondary,
                            )
                        }

                        translated != null -> {
                            Text(
                                text = labels.translation.uppercase(),
                                style = ReelTheme.type.label.copy(fontWeight = FontWeight.SemiBold),
                                color = colors.textTertiary,
                            )
                            Row(
                                modifier = Modifier.padding(top = ReelTheme.spacing.xs),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    text = translated,
                                    style = ReelTheme.type.bodyL,
                                    color = colors.textPrimary,
                                    modifier = Modifier.weight(1f),
                                )
                                ReelSpeakButton(
                                    text = translated,
                                    contentDescription = labels.playPronunciation,
                                )
                            }
                        }

                        else -> {
                            ReelButton(
                                text = labels.translate,
                                onClick = translateAction,
                                style = ReelButtonStyle.Secondary,
                            )
                        }
                    }
                }

                if (entry.synonyms.isNotEmpty() || entry.antonyms.isNotEmpty()) {
                    Spacer(Modifier.height(ReelTheme.spacing.xl))
                    Column(verticalArrangement = Arrangement.spacedBy(ReelTheme.spacing.s)) {
                        if (entry.synonyms.isNotEmpty()) {
                            ThesaurusRow(
                                label = labels.synonyms,
                                words = entry.synonyms,
                                tint = colors.secondary,
                                onWordClick = onWordClick,
                            )
                        }
                        if (entry.antonyms.isNotEmpty()) {
                            ThesaurusRow(
                                label = labels.antonyms,
                                words = entry.antonyms,
                                tint = colors.error,
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
