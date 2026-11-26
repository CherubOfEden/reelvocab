package app.reelvocab.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import app.reelvocab.core.designsystem.theme.ReelTheme

enum class GradeTone { Again, Hard, Good, Easy }

@Immutable
data class GradeLabels(
    val again: String,
    val hard: String,
    val good: String,
    val easy: String,
)

@Composable
fun GradeBar(
    labels: GradeLabels,
    onGrade: (GradeTone) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val colors = ReelTheme.colors
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(ReelTheme.spacing.xs),
    ) {
        GradeSegment(
            label = labels.again,
            color = colors.error,
            tone = GradeTone.Again,
            enabled = enabled,
            onGrade = onGrade,
            modifier = Modifier.weight(1f),
        )
        GradeSegment(
            label = labels.hard,
            color = colors.accentMuted,
            tone = GradeTone.Hard,
            enabled = enabled,
            onGrade = onGrade,
            modifier = Modifier.weight(1f),
        )
        GradeSegment(
            label = labels.good,
            color = colors.accent,
            tone = GradeTone.Good,
            enabled = enabled,
            onGrade = onGrade,
            modifier = Modifier.weight(1f),
        )
        GradeSegment(
            label = labels.easy,
            color = colors.success,
            tone = GradeTone.Easy,
            enabled = enabled,
            onGrade = onGrade,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun GradeSegment(
    label: String,
    color: Color,
    tone: GradeTone,
    enabled: Boolean,
    onGrade: (GradeTone) -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .height(ReelTheme.spacing.controlHeight)
            .clip(RoundedCornerShape(ReelTheme.radius.input))
            .background(if (enabled) color else color.copy(alpha = 0.4f))
            .clickable(enabled = enabled) { onGrade(tone) },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            style = ReelTheme.type.label.copy(fontWeight = FontWeight.SemiBold),
            color = ReelTheme.colors.onAccent,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
