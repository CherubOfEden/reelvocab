package app.reelvocab.feature.capture

import android.graphics.Bitmap
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.reelvocab.core.designsystem.component.ReelButton
import app.reelvocab.core.designsystem.component.ReelButtonStyle
import app.reelvocab.core.designsystem.component.ReelSheet
import app.reelvocab.core.designsystem.theme.ReelTheme
import app.reelvocab.core.domain.model.Enrichment
import java.util.Locale

@Composable
fun InAppCapture(
    bitmap: Bitmap,
    sourceTitle: String,
    timestampMs: Long,
    onDismiss: () -> Unit,
    onSaved: () -> Unit,
    viewModel: CaptureViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val colors = ReelTheme.colors
    var phraseRange by remember(state.lines) { mutableStateOf<PhraseRange?>(null) }

    LaunchedEffect(bitmap) {
        viewModel.recognize(bitmap)
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false,
        ),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(colors.scrim.copy(alpha = 0.18f)),
        ) {
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .heightIn(max = 520.dp)
                    .clip(
                        RoundedCornerShape(
                            topStart = ReelTheme.radius.sheetTop,
                            topEnd = ReelTheme.radius.sheetTop,
                        ),
                    )
                    .background(colors.surface)
                    .navigationBarsPadding()
                    .padding(horizontal = ReelTheme.spacing.screenH)
                    .padding(top = ReelTheme.spacing.m, bottom = ReelTheme.spacing.l),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = ReelTheme.spacing.l),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = stringResource(R.string.capture_pick_title),
                        style = ReelTheme.type.titleL,
                        color = colors.textPrimary,
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = stringResource(R.string.capture_close),
                            tint = colors.textSecondary,
                        )
                    }
                }

                Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 140.dp)
                        .clip(RoundedCornerShape(ReelTheme.radius.card)),
                    contentScale = ContentScale.Fit,
                )

                Spacer(Modifier.height(ReelTheme.spacing.m))

                when {
                    state.loading -> Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(ReelTheme.spacing.s),
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                            color = colors.gold,
                        )
                        Text(
                            text = stringResource(R.string.capture_scanning),
                            style = ReelTheme.type.bodyM,
                            color = colors.textSecondary,
                        )
                    }

                    state.error -> Column {
                        Text(
                            text = stringResource(R.string.capture_error),
                            style = ReelTheme.type.bodyM,
                            color = colors.rose,
                        )
                        Spacer(Modifier.height(ReelTheme.spacing.s))
                        ReelButton(
                            text = stringResource(R.string.capture_retry),
                            onClick = { viewModel.recognize(bitmap) },
                            style = ReelButtonStyle.Secondary,
                        )
                    }

                    state.lines.isEmpty() -> Text(
                        text = stringResource(R.string.capture_empty),
                        style = ReelTheme.type.bodyM,
                        color = colors.textSecondary,
                    )

                    else -> LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 300.dp),
                        contentPadding = PaddingValues(bottom = ReelTheme.spacing.s),
                        verticalArrangement = Arrangement.spacedBy(ReelTheme.spacing.m),
                    ) {
                        if (phraseRange != null) {
                            item(key = "phrase-hint") {
                                Text(
                                    text = stringResource(R.string.capture_phrase_hint),
                                    style = ReelTheme.type.label,
                                    color = colors.gold,
                                )
                            }
                        }
                        itemsIndexed(state.lines) { lineIndex, line ->
                            CaptureLineRow(
                                line = line,
                                selectedRange = phraseRange?.takeIf { it.lineIndex == lineIndex },
                                onWordTap = { wordIndex, word ->
                                    val active = phraseRange
                                    if (active != null && active.lineIndex == lineIndex) {
                                        val start = minOf(active.start, wordIndex)
                                        val end = maxOf(active.end, wordIndex)
                                        val phrase = line.words
                                            .subList(start, end + 1)
                                            .joinToString(" ") { it.text }
                                        viewModel.select(CaptureWord(text = phrase, line = line.text))
                                        phraseRange = null
                                    } else {
                                        phraseRange = null
                                        viewModel.select(word)
                                    }
                                },
                                onWordLongPress = { wordIndex ->
                                    phraseRange = PhraseRange(
                                        lineIndex = lineIndex,
                                        start = wordIndex,
                                        end = wordIndex,
                                    )
                                },
                            )
                        }
                    }
                }
            }
        }
    }

    val selected = state.selected
    if (selected != null) {
        SaveWordSheet(
            word = selected,
            sourceTitle = sourceTitle,
            timestampMs = timestampMs,
            preview = state.preview,
            previewLoading = state.previewLoading,
            saving = state.saving,
            saveError = state.saveError,
            onDismiss = viewModel::dismissSelection,
            onSave = { edited -> viewModel.save(selected, edited, sourceTitle, timestampMs, onSaved) },
        )
    }
}

private data class PhraseRange(
    val lineIndex: Int,
    val start: Int,
    val end: Int,
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CaptureLineRow(
    line: CaptureLine,
    selectedRange: PhraseRange?,
    onWordTap: (Int, CaptureWord) -> Unit,
    onWordLongPress: (Int) -> Unit,
) {
    val colors = ReelTheme.colors
    Column(verticalArrangement = Arrangement.spacedBy(ReelTheme.spacing.xs)) {
        Text(
            text = line.text,
            style = ReelTheme.type.bodyM,
            color = colors.textSecondary,
        )
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(ReelTheme.spacing.xs),
            verticalArrangement = Arrangement.spacedBy(ReelTheme.spacing.xs),
        ) {
            line.words.forEachIndexed { index, word ->
                WordChip(
                    text = word.text,
                    selected = selectedRange != null &&
                        index >= selectedRange.start &&
                        index <= selectedRange.end,
                    onClick = { onWordTap(index, word) },
                    onLongClick = { onWordLongPress(index) },
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun WordChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
) {
    val colors = ReelTheme.colors
    val shape = RoundedCornerShape(ReelTheme.radius.chip)
    val container = if (selected) colors.gold.copy(alpha = 0.16f) else colors.elevated
    val border = if (selected) colors.gold.copy(alpha = 0.55f) else colors.strokeSubtle
    Box(
        modifier = Modifier
            .clip(shape)
            .background(container)
            .border(ReelTheme.spacing.hairline, border, shape)
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick,
            )
            .padding(horizontal = ReelTheme.spacing.s, vertical = ReelTheme.spacing.xs),
    ) {
        Text(
            text = text,
            style = ReelTheme.type.bodyM,
            color = if (selected) colors.gold else colors.textSecondary,
        )
    }
}

@Composable
private fun SaveWordSheet(
    word: CaptureWord,
    sourceTitle: String,
    timestampMs: Long,
    preview: Enrichment?,
    previewLoading: Boolean,
    saving: Boolean,
    saveError: Boolean,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit,
) {
    val colors = ReelTheme.colors
    var edited by remember(word) { mutableStateOf(word.text) }

    ReelSheet(onDismiss = onDismiss, title = stringResource(R.string.capture_save_title)) {
        OutlinedTextField(
            value = edited,
            onValueChange = { edited = it },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            label = { Text(stringResource(R.string.capture_word_label)) },
        )
        Spacer(Modifier.height(ReelTheme.spacing.m))
        Text(
            text = word.line,
            style = ReelTheme.type.bodyM,
            color = colors.textSecondary,
        )
        Spacer(Modifier.height(ReelTheme.spacing.xs))
        Text(
            text = stringResource(
                R.string.capture_source_format,
                sourceTitle,
                formatTimestamp(timestampMs),
            ),
            style = ReelTheme.type.label,
            color = colors.textTertiary,
        )

        Spacer(Modifier.height(ReelTheme.spacing.l))
        when {
            edited.trim() != word.text -> Text(
                text = stringResource(R.string.capture_preview_edited),
                style = ReelTheme.type.label,
                color = colors.textTertiary,
            )

            previewLoading -> Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(ReelTheme.spacing.s),
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    strokeWidth = 2.dp,
                    color = colors.gold,
                )
                Text(
                    text = stringResource(R.string.capture_preview_loading),
                    style = ReelTheme.type.bodyM,
                    color = colors.textSecondary,
                )
            }

            preview != null -> DictionaryPreview(preview = preview)

            else -> Text(
                text = stringResource(R.string.capture_preview_unavailable),
                style = ReelTheme.type.label,
                color = colors.textTertiary,
            )
        }

        if (saveError) {
            Spacer(Modifier.height(ReelTheme.spacing.s))
            Text(
                text = stringResource(R.string.capture_save_error),
                style = ReelTheme.type.bodyM,
                color = colors.rose,
            )
        }

        Spacer(Modifier.height(ReelTheme.spacing.xl))
        ReelButton(
            text = stringResource(R.string.capture_save),
            onClick = { onSave(edited) },
            loading = saving,
            enabled = edited.isNotBlank(),
        )
        Spacer(Modifier.height(ReelTheme.spacing.s))
        ReelButton(
            text = stringResource(R.string.capture_cancel),
            onClick = onDismiss,
            style = ReelButtonStyle.Ghost,
            enabled = !saving,
        )
    }
}

@Composable
private fun DictionaryPreview(preview: Enrichment) {
    val colors = ReelTheme.colors
    val meaning = preview.definitions.firstOrNull()?.definition
        ?: stringResource(R.string.capture_preview_unavailable)
    Text(
        text = stringResource(R.string.capture_preview_label),
        style = ReelTheme.type.label,
        color = colors.textTertiary,
    )
    Spacer(Modifier.height(ReelTheme.spacing.xs))
    if (preview.ipa != null) {
        Text(
            text = preview.ipa,
            style = ReelTheme.type.ipa,
            color = colors.textSecondary,
        )
        Spacer(Modifier.height(ReelTheme.spacing.xxs))
    }
    Text(
        text = meaning,
        style = ReelTheme.type.bodyM,
        color = colors.textPrimary,
    )
    if (preview.thesaurus.synonyms.isNotEmpty()) {
        Spacer(Modifier.height(ReelTheme.spacing.xs))
        Text(
            text = stringResource(
                R.string.capture_preview_synonyms,
                preview.thesaurus.synonyms.take(4).joinToString(", "),
            ),
            style = ReelTheme.type.label,
            color = colors.teal,
        )
    }
}

internal fun formatTimestamp(timestampMs: Long): String {
    val totalSeconds = (timestampMs / 1000).coerceAtLeast(0)
    return String.format(
        Locale.US,
        "%d:%02d",
        totalSeconds / 60,
        totalSeconds % 60,
    )
}
