package app.reelvocab.core.designsystem.component

import androidx.compose.foundation.clickable
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import app.reelvocab.core.designsystem.theme.ReelTheme

@Composable
fun ContextSentence(
    sentence: String,
    target: String,
    onTapTarget: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = ReelTheme.colors
    val highlight = colors.gold
    val annotated = remember(sentence, target, highlight) {
        buildAnnotatedString {
            val index = sentence.indexOf(target, ignoreCase = true)
            if (index >= 0) {
                append(sentence.substring(0, index))
                withStyle(SpanStyle(color = highlight, fontWeight = FontWeight.SemiBold)) {
                    append(sentence.substring(index, index + target.length))
                }
                append(sentence.substring(index + target.length))
            } else {
                append(sentence)
            }
        }
    }
    Text(
        text = annotated,
        style = ReelTheme.type.bodyL,
        color = colors.textSecondary,
        modifier = modifier.clickable(onClick = onTapTarget),
    )
}
