package app.reelvocab.feature.capture

import android.graphics.Bitmap
import android.widget.Toast
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
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.platform.LocalContext
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
    val context = LocalContext.current
    var phraseRange by remember(state.lines) { mutableStateOf<PhraseRange?>(null) }
    var multiSelect by remember(state.lines) { mutableStateOf(false) }
    var selectedWords by remember(state.lines) { mutableStateOf<Set<CaptureWord>>(emptySet()) }
    var selectedLines by remember(state.lines) { mutableStateOf<Set<Int>>(emptySet()) }

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
                    .heightIn(max = 680.dp)
                    .clip(
                        RoundedCornerShape(
                            topStart = ReelTheme.radius.sheetTop,
                            topEnd = ReelTheme.radius.sheetTop,
                        ),
                    )
                    .background(colors.surface)
                    .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom))
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
                        text = stringResource(
                            if (multiSelect) {
                                R.string.capture_selection_title
                            } else {
                                R.string.capture_pick_title
                            },
                        ),
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
                        .heightIn(max = 130.dp)
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
                            .heightIn(max = 430.dp),
                        contentPadding = PaddingValues(bottom = ReelTheme.spacing.s),
                        verticalArrangement = Arrangement.spacedBy(ReelTheme.spacing.m),
                    ) {
                        if (multiSelect) {
                            item(key = "multi-hint") {
                                Text(
                                    text = stringResource(R.string.capture_select_hint),
                                    style = ReelTheme.type.label,
                                    color = colors.green,
                                )
                            }
                        } else if (phraseRange != null) {
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
                                lineSelected = lineIndex in selectedLines,
                                onLineSelectedChange = { checked ->
                                    val updated = if (checked) {
                                        selectedLines + lineIndex
                                    } else {
                                        selectedLines - lineIndex
                                    }
                                    selectedLines = updated
                                    if (checked) {
                                        multiSelect = true
                                    } else if (updated.isEmpty() && selectedWords.isEmpty()) {
                                        multiSelect = false
                                    }
                                },
                                selectedRange = phraseRange?.takeIf { it.lineIndex == lineIndex },
                                selectedWords = selectedWords,
                                onWordTap = { wordIndex, word ->
                                    if (multiSelect) {
                                        val updated = if (word in selectedWords) {
                                            selectedWords - word
                                        } else {
                                            selectedWords + word
                                        }
                                        selectedWords = updated
                                        if (selectedLines.isEmpty() && updated.isEmpty()) {
                                            multiSelect = false
                                        }
                                    } else {
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
                                    }
                                },
                                onWordLongPress = { wordIndex ->
                                    val word = line.words[wordIndex]
                                    phraseRange = null
                                    multiSelect = true
                                    selectedWords = selectedWords + word
                                },
                            )
                        }
                    }
                }

                if (multiSelect) {
                    Spacer(Modifier.height(ReelTheme.spacing.s))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(ReelTheme.spacing.s),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        if (state.multiLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp,
                                color = colors.green,
                            )
                            Text(
                                text = stringResource(R.string.capture_preview_loading),
                                style = ReelTheme.type.label,
                                color = colors.textSecondary,
                                modifier = Modifier.weight(1f),
                            )
                        } else {
                            TextButton(
                                onClick = {
                                    multiSelect = false
                                    selectedWords = emptySet()
                                    selectedLines = emptySet()
                                },
                            ) {
                                Text(
                                    text = stringResource(R.string.capture_cancel),
                                    style = ReelTheme.type.label,
                                    color = colors.textSecondary,
                                )
                            }
                            Spacer(Modifier.weight(1f))
                            Box(modifier = Modifier.weight(0.45f)) {
                                ReelButton(
                                    text = stringResource(R.string.capture_select_ok),
                                    onClick = {
                                        val fullLines = state.lines
                                            .filterIndexed { index, _ -> index in selectedLines }
                                            .map { CaptureWord(text = it.text, line = it.text) }
                                        val selectedLineTexts = fullLines
                                            .map { it.line.trim().lowercase() }
                                            .toSet()
                                        val individualWords = state.lines
                                            .flatMap { it.words }
                                            .filter { it in selectedWords }
                                            .filter {
                                                it.line.trim().lowercase() !in selectedLineTexts
                                            }
                                        viewModel.prepareSelection(
                                            (fullLines + individualWords)
                                                .distinctBy { it.text.trim().lowercase() },
                                        )
                                    },
                                    enabled = selectedWords.isNotEmpty() || selectedLines.isNotEmpty(),
                                )
                            }
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
            onTextChange = { viewModel.previewEdited(it, selected.line) },
            onSave = { edited, definition ->
                viewModel.save(selected, edited, definition, sourceTitle, timestampMs) {
                    Toast.makeText(context, R.string.capture_saved, Toast.LENGTH_SHORT).show()
                }
            },
        )
    }

    val multiSelection = state.multiSelection
    if (multiSelection != null) {
        SaveSelectionSheet(
            selection = multiSelection,
            saving = state.multiSaving,
            saveError = state.multiError,
            onDismiss = {
                viewModel.dismissMultiSelection()
                multiSelect = false
                selectedWords = emptySet()
                selectedLines = emptySet()
            },
            onSave = {
                viewModel.saveSelection(multiSelection, sourceTitle, timestampMs) {
                    Toast.makeText(context, R.string.capture_saved, Toast.LENGTH_SHORT).show()
                    multiSelect = false
                    selectedWords = emptySet()
                    selectedLines = emptySet()
                    onSaved()
                }
            },
        )
    }
}

private data class PhraseRange(
    val lineIndex: Int,
    val start: Int,
    val end: Int,
)

@OptIn(ExperimentalLayoutApi::class, ExperimentalFoundationApi::class)
@Composable
private fun CaptureLineRow(
    line: CaptureLine,
    lineSelected: Boolean,
    onLineSelectedChange: (Boolean) -> Unit,
    selectedRange: PhraseRange?,
    selectedWords: Set<CaptureWord>,
    onWordTap: (Int, CaptureWord) -> Unit,
    onWordLongPress: (Int) -> Unit,
) {
    val colors = ReelTheme.colors
    Column(verticalArrangement = Arrangement.spacedBy(ReelTheme.spacing.xs)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(
                checked = lineSelected,
                onCheckedChange = onLineSelectedChange,
            )
            Text(
                text = line.text,
                style = ReelTheme.type.bodyM,
                color = if (lineSelected) colors.textPrimary else colors.textSecondary,
                modifier = Modifier.weight(1f),
            )
        }
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
                    checked = word in selectedWords,
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
    checked: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
) {
    val colors = ReelTheme.colors
    val shape = RoundedCornerShape(ReelTheme.radius.chip)
    val active = selected || checked
    val accent = if (checked) colors.green else colors.gold
    val container = if (active) accent.copy(alpha = 0.16f) else colors.elevated
    val border = if (active) accent.copy(alpha = 0.55f) else colors.strokeSubtle
    Box(
        modifier = Modifier
            .clip(shape)
            .background(container)
            .border(ReelTheme.spacing.hairline, border, shape)
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick,
            )
            .padding(horizontal = ReelTheme.spacing.m, vertical = ReelTheme.spacing.s),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(ReelTheme.spacing.xs),
        ) {
            Text(
                text = text,
                style = ReelTheme.type.bodyL,
                color = if (active) accent else colors.textSecondary,
            )
            if (checked) {
                Icon(
                    imageVector = Icons.Filled.Check,
                    contentDescription = null,
                    tint = colors.green,
                    modifier = Modifier.size(16.dp),
                )
            }
        }
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
    onTextChange: (String) -> Unit,
    onSave: (String, String?) -> Unit,
) {
    val colors = ReelTheme.colors
    var edited by remember(word) { mutableStateOf(word.text) }
    var editedDefinition by remember(word) { mutableStateOf("") }
    var definitionDirty by remember(word) { mutableStateOf(false) }

    LaunchedEffect(preview) {
        if (!definitionDirty) {
            editedDefinition = preview
                ?.definitions
                ?.firstOrNull()
                ?.definition
                .orEmpty()
        }
    }

    ReelSheet(onDismiss = onDismiss, title = stringResource(R.string.capture_save_title)) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 520.dp)
                .verticalScroll(rememberScrollState()),
        ) {
            OutlinedTextField(
                value = edited,
                onValueChange = { value ->
                    edited = value
                    editedDefinition = ""
                    definitionDirty = false
                    onTextChange(value)
                },
                modifier = Modifier.fillMaxWidth(),
                minLines = 1,
                maxLines = 6,
                label = { Text(stringResource(R.string.capture_word_label)) },
            )
            Spacer(Modifier.height(ReelTheme.spacing.s))
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
            OutlinedTextField(
                value = editedDefinition,
                onValueChange = { value ->
                    definitionDirty = true
                    editedDefinition = value
                },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2,
                maxLines = 8,
                label = { Text(stringResource(R.string.capture_preview_label)) },
                supportingText = {
                    when {
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
                                style = ReelTheme.type.label,
                                color = colors.textSecondary,
                            )
                        }

                        preview == null && !definitionDirty && editedDefinition.isBlank() -> Text(
                            text = stringResource(R.string.capture_preview_unavailable),
                            style = ReelTheme.type.label,
                            color = colors.textTertiary,
                        )

                        else -> Unit
                    }
                },
            )

            preview?.ipa?.let { ipa ->
                Spacer(Modifier.height(ReelTheme.spacing.xs))
                Text(
                    text = ipa,
                    style = ReelTheme.type.ipa,
                    color = colors.textSecondary,
                )
            }
            val synonyms = preview?.thesaurus?.synonyms.orEmpty()
            if (synonyms.isNotEmpty()) {
                Spacer(Modifier.height(ReelTheme.spacing.xs))
                Text(
                    text = stringResource(
                        R.string.capture_preview_synonyms,
                        synonyms.take(4).joinToString(", "),
                    ),
                    style = ReelTheme.type.label,
                    color = colors.teal,
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
                onClick = {
                    onSave(
                        edited,
                        editedDefinition.trim().takeIf { it.isNotEmpty() },
                    )
                },
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
}

@Composable
private fun SaveSelectionSheet(
    selection: MultiSelection,
    saving: Boolean,
    saveError: Boolean,
    onDismiss: () -> Unit,
    onSave: () -> Unit,
) {
    val colors = ReelTheme.colors
    ReelSheet(onDismiss = onDismiss, title = stringResource(R.string.capture_selection_title)) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 480.dp)
                .verticalScroll(rememberScrollState()),
        ) {
            selection.words.forEach { selected ->
                Text(
                    text = selected.word.text,
                    style = ReelTheme.type.titleM,
                    color = colors.textPrimary,
                )
                val meaning = selected.enrichment?.definitions?.firstOrNull()?.definition
                Text(
                    text = meaning ?: stringResource(R.string.capture_preview_unavailable),
                    style = ReelTheme.type.bodyM,
                    color = colors.textSecondary,
                )
                Spacer(Modifier.height(ReelTheme.spacing.m))
            }
            if (selection.phrase.isNotBlank()) {
                Spacer(Modifier.height(ReelTheme.spacing.xs))
                Text(
                    text = stringResource(R.string.capture_phrase_title),
                    style = ReelTheme.type.label,
                    color = colors.textTertiary,
                )
                Spacer(Modifier.height(ReelTheme.spacing.xxs))
                Text(
                    text = selection.phrase,
                    style = ReelTheme.type.titleM,
                    color = colors.gold,
                )
                val phraseMeaning = selection.phraseEnrichment
                    ?.definitions
                    ?.firstOrNull()
                    ?.definition
                    ?: stringResource(R.string.capture_preview_unavailable)
                Text(
                    text = phraseMeaning,
                    style = ReelTheme.type.bodyM,
                    color = colors.textSecondary,
                )
            }
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
            text = stringResource(R.string.capture_save_selection),
            onClick = onSave,
            loading = saving,
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


internal fun formatTimestamp(timestampMs: Long): String {
    val totalSeconds = (timestampMs / 1000).coerceAtLeast(0)
    return String.format(
        Locale.US,
        "%d:%02d",
        totalSeconds / 60,
        totalSeconds % 60,
    )
}
