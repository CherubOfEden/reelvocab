package app.reelvocab.core.designsystem.component

import app.reelvocab.core.designsystem.icon.ReelIcons

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.input.ImeAction
import app.reelvocab.core.designsystem.theme.ReelTheme

@Composable
fun ReelSearchField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    clearDescription: String,
    modifier: Modifier = Modifier,
    leadingIcon: ImageVector = ReelIcons.Search,
    onSearch: (() -> Unit)? = null,
) {
    val colors = ReelTheme.colors
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.fillMaxWidth(),
        singleLine = true,
        placeholder = {
            Text(
                text = placeholder,
                style = ReelTheme.type.bodyM,
                color = colors.textTertiary,
            )
        },
        leadingIcon = {
            Icon(
                imageVector = leadingIcon,
                contentDescription = null,
                tint = colors.textTertiary,
            )
        },
        trailingIcon = if (value.isNotEmpty()) {
            {
                IconButton(onClick = { onValueChange("") }) {
                    Icon(
                        imageVector = ReelIcons.X,
                        contentDescription = clearDescription,
                        tint = colors.textTertiary,
                    )
                }
            }
        } else {
            null
        },
        textStyle = ReelTheme.type.bodyL,
        shape = androidx.compose.foundation.shape.RoundedCornerShape(ReelTheme.radius.input),
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(onSearch = { onSearch?.invoke() }),
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = colors.textPrimary,
            unfocusedTextColor = colors.textPrimary,
            focusedContainerColor = colors.surface,
            unfocusedContainerColor = colors.surface,
            focusedBorderColor = colors.accent,
            unfocusedBorderColor = colors.strokeSubtle,
            cursorColor = colors.accent,
        ),
    )
}
