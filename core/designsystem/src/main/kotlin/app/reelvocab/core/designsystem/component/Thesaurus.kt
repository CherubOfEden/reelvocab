package app.reelvocab.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.reelvocab.core.designsystem.theme.ReelTheme

@Composable
fun ThesaurusRow(
    label: String,
    words: List<String>,
    tint: Color,
    modifier: Modifier = Modifier,
) {
    val colors = ReelTheme.colors
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = ReelTheme.type.label,
            color = colors.textTertiary,
            modifier = Modifier.width(64.dp),
        )
        Row(
            modifier = Modifier
                .weight(1f)
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(ReelTheme.spacing.xs),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            words.forEach { word ->
                ThesaurusTag(text = word, tint = tint)
            }
        }
    }
}

@Composable
private fun ThesaurusTag(
    text: String,
    tint: Color,
) {
    Text(
        text = text,
        style = ReelTheme.type.label,
        color = tint,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier
            .clip(RoundedCornerShape(ReelTheme.radius.chip))
            .background(tint.copy(alpha = 0.12f))
            .padding(
                horizontal = ReelTheme.spacing.s,
                vertical = ReelTheme.spacing.xxs,
            ),
    )
}
