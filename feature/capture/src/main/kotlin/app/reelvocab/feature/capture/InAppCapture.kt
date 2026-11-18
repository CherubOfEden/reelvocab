package app.reelvocab.feature.capture

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
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
import app.reelvocab.core.designsystem.component.ReelChip
import app.reelvocab.core.designsystem.component.ReelSheet
import app.reelvocab.core.designsystem.theme.ReelTheme
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
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = colors.base,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = ReelTheme.spacing.screenH),
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
                        .heightIn(max = 220.dp)
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
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = ReelTheme.spacing.screenBottom),
                        verticalArrangement = Arrangement.spacedBy(ReelTheme.spacing.m),
                    ) {
                        items(state.lines) { line ->
                            CaptureLineRow(
                                line = line,
                                onSelect = viewModel::select,
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
            saving = state.saving,
            onDismiss = viewModel::dismissSelection,
            onSave = { edited -> viewModel.save(selected, edited, sourceTitle, timestampMs, onSaved) },
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CaptureLineRow(
    line: CaptureLine,
    onSelect: (CaptureWord) -> Unit,
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
            line.words.forEach { word ->
                ReelChip(
                    text = word.text,
                    selected = false,
                    onClick = { onSelect(word) },
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
    saving: Boolean,
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

internal fun formatTimestamp(timestampMs: Long): String {
    val totalSeconds = (timestampMs / 1000).coerceAtLeast(0)
    return String.format(
        Locale.US,
        "%d:%02d",
        totalSeconds / 60,
        totalSeconds % 60,
    )
}
