package app.reelvocab.feature.today

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.reelvocab.core.designsystem.component.EmptyState
import app.reelvocab.core.designsystem.component.GradeBar
import app.reelvocab.core.designsystem.component.GradeLabels
import app.reelvocab.core.designsystem.component.GradeTone
import app.reelvocab.core.designsystem.component.ReelButton
import app.reelvocab.core.designsystem.component.ReelButtonStyle
import app.reelvocab.core.designsystem.component.WordCard
import app.reelvocab.core.designsystem.component.WordCardLabels
import app.reelvocab.core.designsystem.component.WordCardModel
import app.reelvocab.core.designsystem.theme.ReelTheme
import app.reelvocab.core.domain.model.ReviewCard
import app.reelvocab.core.domain.model.ReviewGrade
import kotlinx.serialization.Serializable

@Serializable
data object ReviewRoute

@Composable
fun ReviewScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ReviewViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val colors = ReelTheme.colors

    LaunchedEffect(Unit) {
        viewModel.start()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = ReelTheme.spacing.screenH),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = ReelTheme.spacing.m),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.review_back),
                    tint = colors.textPrimary,
                )
            }
            Text(
                text = stringResource(R.string.review_title),
                style = ReelTheme.type.titleL,
                color = colors.textPrimary,
            )
        }

        when {
            state.loading -> Text(
                text = stringResource(R.string.review_loading),
                style = ReelTheme.type.bodyM,
                color = colors.textSecondary,
                modifier = Modifier.padding(top = ReelTheme.spacing.l),
            )

            state.empty -> Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Center,
            ) {
                EmptyState(
                    title = stringResource(R.string.review_empty_title),
                    body = stringResource(R.string.review_empty_body),
                    ctaLabel = stringResource(R.string.review_empty_cta),
                    onCta = onBack,
                )
            }

            state.finished -> ReviewDone(onBack = onBack)

            else -> {
                val current = state.current
                if (current != null) {
                    ReviewBody(
                        card = current,
                        position = state.position + 1,
                        total = state.queue.size,
                        onGrade = viewModel::grade,
                    )
                }
            }
        }
    }
}

@Composable
private fun ReviewBody(
    card: ReviewCard,
    position: Int,
    total: Int,
    onGrade: (ReviewGrade) -> Unit,
) {
    val colors = ReelTheme.colors
    val labels = GradeLabels(
        again = stringResource(R.string.review_grade_again),
        hard = stringResource(R.string.review_grade_hard),
        good = stringResource(R.string.review_grade_good),
        easy = stringResource(R.string.review_grade_easy),
    )
    val cardLabels = WordCardLabels(
        ai = stringResource(R.string.review_ai_badge),
        playPronunciation = stringResource(R.string.review_play),
        tag = stringResource(R.string.review_tag),
        more = stringResource(R.string.review_more),
        synonyms = stringResource(R.string.review_synonyms),
        antonyms = stringResource(R.string.review_antonyms),
    )
    val model = WordCardModel(
        headword = card.card.headword,
        ipa = card.card.ipa,
        partOfSpeech = card.card.wordClass?.storageValue,
        meaning = card.card.meaning,
        sentence = card.card.contextLine,
        target = card.card.headword,
        sourceTitle = card.card.sourceTitle,
        sourceTimestampMs = card.card.sourceTimestampMs,
        sourceTint = colors.teal,
        isAiContext = false,
        hasAudio = card.card.hasAudio,
        difficulty = null,
    )

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState()),
        ) {
            Text(
                text = stringResource(R.string.review_progress, position, total),
                style = ReelTheme.type.label,
                color = colors.textTertiary,
                modifier = Modifier.padding(top = ReelTheme.spacing.s),
            )
            Spacer(Modifier.height(ReelTheme.spacing.s))
            WordCard(
                model = model,
                labels = cardLabels,
                onClick = {},
                onTts = {},
                onTag = {},
                onMore = {},
            )
        }
        GradeBar(
            labels = labels,
            onGrade = { tone ->
                onGrade(
                    when (tone) {
                        GradeTone.Again -> ReviewGrade.Again
                        GradeTone.Hard -> ReviewGrade.Hard
                        GradeTone.Good -> ReviewGrade.Good
                        GradeTone.Easy -> ReviewGrade.Easy
                    },
                )
            },
            modifier = Modifier.padding(
                top = ReelTheme.spacing.m,
                bottom = ReelTheme.spacing.xxl,
            ),
        )
    }
}

@Composable
private fun ReviewDone(onBack: () -> Unit) {
    val colors = ReelTheme.colors
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResource(R.string.review_done_title),
            style = ReelTheme.type.titleL,
            color = colors.textPrimary,
        )
        Spacer(Modifier.height(ReelTheme.spacing.s))
        Text(
            text = stringResource(R.string.review_done_body),
            style = ReelTheme.type.bodyM,
            color = colors.textSecondary,
        )
        Spacer(Modifier.height(ReelTheme.spacing.xl))
        ReelButton(
            text = stringResource(R.string.review_done_cta),
            onClick = onBack,
            style = ReelButtonStyle.Secondary,
        )
    }
}
