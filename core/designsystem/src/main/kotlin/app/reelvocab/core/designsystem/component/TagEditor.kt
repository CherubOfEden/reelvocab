package app.reelvocab.core.designsystem.component

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import app.reelvocab.core.designsystem.theme.ReelTheme

@Immutable
data class TagEditorModel(
    val headword: String,
    val tags: List<String> = emptyList(),
    val draft: String = "",
)

@Immutable
data class TagEditorLabels(
    val title: String,
    val hint: String,
    val add: String,
    val save: String,
    val empty: String,
    val removeDescription: String,
)

@Composable
fun TagEditorSheet(
    model: TagEditorModel,
    labels: TagEditorLabels,
    onDraftChange: (String) -> Unit,
    onAdd: () -> Unit,
    onRemove: (String) -> Unit,
    onSave: () -> Unit,
    onDismiss: () -> Unit,
) {
    val colors = ReelTheme.colors
    val keyboard = LocalSoftwareKeyboardController.current

    ReelSheet(onDismiss = onDismiss, title = labels.title) {
        Text(
            text = model.headword,
            style = ReelTheme.type.titleM,
            color = colors.textPrimary,
        )
        Spacer(Modifier.height(ReelTheme.spacing.m))

        if (model.tags.isEmpty()) {
            Text(
                text = labels.empty,
                style = ReelTheme.type.bodyM,
                color = colors.textSecondary,
            )
        } else {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(ReelTheme.spacing.xs),
            ) {
                model.tags.forEach { tag ->
                    EditableTagChip(
                        text = tag,
                        removeDescription = labels.removeDescription,
                        onRemove = { onRemove(tag) },
                    )
                }
            }
        }

        Spacer(Modifier.height(ReelTheme.spacing.l))
        OutlinedTextField(
            value = model.draft,
            onValueChange = onDraftChange,
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            placeholder = {
                Text(
                    text = labels.hint,
                    style = ReelTheme.type.bodyM,
                    color = colors.textTertiary,
                )
            },
            textStyle = ReelTheme.type.bodyL,
            shape = RoundedCornerShape(ReelTheme.radius.input),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(
                onDone = {
                    keyboard?.hide()
                    onAdd()
                },
            ),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = colors.textPrimary,
                unfocusedTextColor = colors.textPrimary,
                focusedContainerColor = colors.surface,
                unfocusedContainerColor = colors.surface,
                focusedBorderColor = colors.gold,
                unfocusedBorderColor = colors.strokeSubtle,
                cursorColor = colors.gold,
            ),
        )
        Spacer(Modifier.height(ReelTheme.spacing.m))
        ReelButton(
            text = labels.add,
            onClick = onAdd,
            style = ReelButtonStyle.Secondary,
            enabled = model.draft.isNotBlank(),
        )
        Spacer(Modifier.height(ReelTheme.spacing.s))
        ReelButton(text = labels.save, onClick = onSave)
    }
}
