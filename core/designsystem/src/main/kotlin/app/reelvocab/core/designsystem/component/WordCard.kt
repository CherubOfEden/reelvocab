package app.reelvocab.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocalMovies
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Style
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.reelvocab.core.designsystem.theme.ReelTheme
import java.util.Locale

@Immutable
data class WordCardLabels(
    val ai: String,
    val playPronunciation: String,
    val tag: String,
    val more: String,
    val synonyms: String = "",
    val antonyms: String = "",
)

@Immutable
data class WordCardModel(
    val headword: String,
    val ipa: String?,
    val partOfSpeech: String?,
    val meaning: String,
    val sentence: String?,
    val target: String?,
    val sourceTitle: String?,
    val sourceTimestampMs: Long?,
    val sourceTint: Color,
    val isAiContext: Boolean,
    val hasAudio: Boolean,
    val difficulty: Int? = null,
    val synonyms: List<String> = emptyList(),
    val antonyms: List<String> = emptyList(),
    val tags: List<String> = emptyList(),
)

@Composable
fun WordCard(
    model: WordCardModel,
    labels: WordCardLabels,
    onClick: () -> Unit,
    onTts: () -> Unit,
    onTag: () -> Unit,
    onMore: () -> Unit,
    modifier: Modifier = Modifier,
    compact: Boolean = false,
) {
    val colors = ReelTheme.colors
    val type = ReelTheme.type

    CardSurface(modifier = modifier, onClick = onClick) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = model.headword,
                        style = if (compact) {
                            type.displayM.copy(fontSize = 24.sp, lineHeight = 28.sp)
                        } else {
                            type.displayM
                        },
                        color = colors.textPrimary,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (model.isAiContext) {
                        Spacer(Modifier.width(ReelTheme.spacing.s))
                        AIBadge(label = labels.ai)
                    }
                }
                if (model.ipa != null) {
                    Text(
                        text = model.ipa,
                        style = type.ipa,
                        color = colors.textSecondary,
                        modifier = Modifier.padding(top = ReelTheme.spacing.xxs),
                    )
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(top = ReelTheme.spacing.xs),
                ) {
                    if (model.partOfSpeech != null) {
                        Text(
                            text = model.partOfSpeech,
                            style = type.label,
                            color = colors.teal,
                        )
                    }
                    if (model.partOfSpeech != null && model.difficulty != null) {
                        Spacer(Modifier.width(ReelTheme.spacing.s))
                    }
                    if (model.difficulty != null) {
                        DifficultyDots(level = model.difficulty)
                    }
                }
            }
            if (model.hasAudio) {
                TtsButton(
                    onClick = onTts,
                    speaking = false,
                    contentDescription = labels.playPronunciation,
                )
            }
        }

        Spacer(Modifier.height(ReelTheme.spacing.m))
        Text(
            text = model.meaning,
            style = type.bodyM,
            color = colors.textSecondary,
            maxLines = if (compact) 2 else 4,
            overflow = TextOverflow.Ellipsis,
        )

        if (model.tags.isNotEmpty()) {
            Spacer(Modifier.height(ReelTheme.spacing.m))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(ReelTheme.spacing.xs),
            ) {
                model.tags.forEach { tag ->
                    TagChip(text = tag, onClick = onTag)
                }
            }
        }

        if (model.synonyms.isNotEmpty() || model.antonyms.isNotEmpty()) {
            Spacer(Modifier.height(ReelTheme.spacing.m))
            Column(verticalArrangement = Arrangement.spacedBy(ReelTheme.spacing.xs)) {
                if (model.synonyms.isNotEmpty()) {
                    ThesaurusRow(
                        label = labels.synonyms,
                        words = model.synonyms,
                        tint = colors.teal,
                    )
                }
                if (model.antonyms.isNotEmpty()) {
                    ThesaurusRow(
                        label = labels.antonyms,
                        words = model.antonyms,
                        tint = colors.rose,
                    )
                }
            }
        }

        val sentence = model.sentence
        val target = model.target
        if (sentence != null && target != null) {
            Spacer(Modifier.height(ReelTheme.spacing.m))
            ContextSentence(
                sentence = sentence,
                target = target,
                onTapTarget = onClick,
            )
        }

        val sourceTitle = model.sourceTitle
        Spacer(Modifier.height(ReelTheme.spacing.m))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (sourceTitle != null) {
                Box(modifier = Modifier.weight(1f)) {
                    SourceChip(
                        title = sourceTitle,
                        timestampMs = model.sourceTimestampMs,
                        tint = model.sourceTint,
                    )
                }
            } else {
                Spacer(Modifier.weight(1f))
            }
            IconButton(onClick = onTag) {
                Icon(
                    imageVector = Icons.Filled.Style,
                    contentDescription = labels.tag,
                    tint = colors.textTertiary,
                )
            }
            IconButton(onClick = onMore) {
                Icon(
                    imageVector = Icons.Filled.MoreHoriz,
                    contentDescription = labels.more,
                    tint = colors.textTertiary,
                )
            }
        }
    }
}

@Composable
fun SourceChip(
    title: String,
    timestampMs: Long?,
    tint: Color,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
) {
    val colors = ReelTheme.colors
    val shape = RoundedCornerShape(ReelTheme.radius.chip)
    Row(
        modifier = modifier
            .clip(shape)
            .background(tint.copy(alpha = 0.16f))
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = ReelTheme.spacing.s, vertical = ReelTheme.spacing.xs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Filled.LocalMovies,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(14.dp),
        )
        Spacer(Modifier.width(ReelTheme.spacing.xs))
        Text(
            text = title,
            style = ReelTheme.type.label,
            color = colors.textSecondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f, fill = false),
        )
        if (timestampMs != null) {
            Spacer(Modifier.width(ReelTheme.spacing.xs))
            Text(
                text = formatFilmTime(timestampMs),
                style = ReelTheme.type.mono,
                color = colors.textTertiary,
            )
        }
    }
}

internal fun formatFilmTime(ms: Long): String {
    val totalSeconds = (ms / 1000L).coerceAtLeast(0L)
    val hours = totalSeconds / 3600L
    val minutes = (totalSeconds % 3600L) / 60L
    val seconds = totalSeconds % 60L
    return if (hours > 0L) {
        String.format(Locale.US, "%d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format(Locale.US, "%d:%02d", minutes, seconds)
    }
}
