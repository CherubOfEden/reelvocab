package app.reelvocab.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Screenshot
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.reelvocab.R
import app.reelvocab.core.designsystem.component.AudioBarState
import app.reelvocab.core.designsystem.component.AudioPlayerBar
import app.reelvocab.core.designsystem.component.ConfirmDestructive
import app.reelvocab.core.designsystem.component.EmptyState
import app.reelvocab.core.designsystem.component.ErrorState
import app.reelvocab.core.designsystem.component.GradeBar
import app.reelvocab.core.designsystem.component.GradeLabels
import app.reelvocab.core.designsystem.component.GradeTone
import app.reelvocab.core.designsystem.component.InlineError
import app.reelvocab.core.designsystem.component.LoadingState
import app.reelvocab.core.designsystem.component.OfflineBanner
import app.reelvocab.core.designsystem.component.PermissionSheet
import app.reelvocab.core.designsystem.component.ReelButton
import app.reelvocab.core.designsystem.component.ReelButtonStyle
import app.reelvocab.core.designsystem.component.ReelChip
import app.reelvocab.core.designsystem.component.ReelSheet
import app.reelvocab.core.designsystem.component.ReviewProgressDots
import app.reelvocab.core.designsystem.component.StatTile
import app.reelvocab.core.designsystem.component.StreakRing
import app.reelvocab.core.designsystem.component.WaveformMini
import app.reelvocab.core.designsystem.component.WordCard
import app.reelvocab.core.designsystem.component.WordCardLabels
import app.reelvocab.core.designsystem.component.WordCardModel
import app.reelvocab.core.designsystem.illustration.ReelIllustration
import app.reelvocab.core.designsystem.illustration.ReelIllustrationArt
import app.reelvocab.core.designsystem.theme.ReelTheme
import kotlinx.serialization.Serializable

@Serializable
data object DesignSystemRoute

private enum class GallerySheet { None, Cards, Permission, Confirm }

@Composable
fun DesignSystemGallery(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = ReelTheme.colors
    val spacing = ReelTheme.spacing
    val haptics = ReelTheme.haptics
    var lastGrade by remember { mutableStateOf<GradeTone?>(null) }
    var playing by remember { mutableStateOf(false) }
    var sheet by remember { mutableStateOf(GallerySheet.None) }
    var selectedChip by remember { mutableStateOf(0) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.base)
            .systemBarsPadding(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = spacing.s, vertical = spacing.s),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.common_back),
                    tint = colors.textSecondary,
                )
            }
            Text(
                text = stringResource(R.string.gallery_title),
                style = ReelTheme.type.titleL,
                color = colors.textPrimary,
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = spacing.screenH)
                .padding(bottom = spacing.huge),
            verticalArrangement = Arrangement.spacedBy(spacing.xxl),
        ) {
            GallerySection(title = stringResource(R.string.gallery_section_color)) {
                val swatches = listOf(
                    stringResource(R.string.gallery_color_base) to colors.base,
                    stringResource(R.string.gallery_color_surface) to colors.surface,
                    stringResource(R.string.gallery_color_elevated) to colors.elevated,
                    stringResource(R.string.gallery_color_stroke) to colors.strokeSubtle,
                    stringResource(R.string.gallery_color_text_primary) to colors.textPrimary,
                    stringResource(R.string.gallery_color_text_secondary) to colors.textSecondary,
                    stringResource(R.string.gallery_color_text_tertiary) to colors.textTertiary,
                    stringResource(R.string.gallery_color_gold) to colors.gold,
                    stringResource(R.string.gallery_color_gold_dim) to colors.goldDim,
                    stringResource(R.string.gallery_color_teal) to colors.teal,
                    stringResource(R.string.gallery_color_rose) to colors.rose,
                    stringResource(R.string.gallery_color_green) to colors.green,
                )
                Column(verticalArrangement = Arrangement.spacedBy(spacing.s)) {
                    swatches.chunked(4).forEach { row ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(spacing.s),
                        ) {
                            row.forEach { (label, color) ->
                                Swatch(label = label, color = color, modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            }

            GallerySection(title = stringResource(R.string.gallery_section_type)) {
                TypeSamples()
            }

            GallerySection(title = stringResource(R.string.gallery_section_buttons)) {
                Column(verticalArrangement = Arrangement.spacedBy(spacing.s)) {
                    ReelButton(
                        text = stringResource(R.string.gallery_btn_primary),
                        onClick = {},
                    )
                    ReelButton(
                        text = stringResource(R.string.gallery_btn_secondary),
                        onClick = {},
                        style = ReelButtonStyle.Secondary,
                    )
                    ReelButton(
                        text = stringResource(R.string.gallery_btn_ghost),
                        onClick = {},
                        style = ReelButtonStyle.Ghost,
                    )
                    ReelButton(
                        text = stringResource(R.string.gallery_btn_destructive),
                        onClick = {},
                        style = ReelButtonStyle.Destructive,
                    )
                    ReelButton(
                        text = stringResource(R.string.gallery_btn_loading),
                        onClick = {},
                        loading = true,
                    )
                    ReelButton(
                        text = stringResource(R.string.gallery_btn_disabled),
                        onClick = {},
                        enabled = false,
                    )
                }
            }

            GallerySection(title = stringResource(R.string.gallery_section_chips)) {
                val chipLabels = listOf(
                    R.string.gallery_chip_all,
                    R.string.gallery_chip_due,
                    R.string.gallery_chip_words,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(spacing.s)) {
                    chipLabels.forEachIndexed { index, label ->
                        ReelChip(
                            text = stringResource(label),
                            selected = selectedChip == index,
                            onClick = { selectedChip = index },
                        )
                    }
                }
            }

            GallerySection(title = stringResource(R.string.gallery_section_cards)) {
                val model = WordCardModel(
                    headword = stringResource(R.string.gallery_word),
                    ipa = stringResource(R.string.gallery_ipa),
                    partOfSpeech = stringResource(R.string.gallery_pos),
                    meaning = stringResource(R.string.gallery_meaning),
                    sentence = stringResource(R.string.gallery_sentence),
                    target = stringResource(R.string.gallery_word),
                    sourceTitle = stringResource(R.string.gallery_film),
                    sourceTimestampMs = 5_046_000L,
                    sourceTint = colors.teal,
                    isAiContext = true,
                    hasAudio = true,
                    difficulty = 3,
                    synonyms = listOf(
                        stringResource(R.string.gallery_syn_1),
                        stringResource(R.string.gallery_syn_2),
                        stringResource(R.string.gallery_syn_3),
                    ),
                    antonyms = listOf(stringResource(R.string.gallery_ant_1)),
                )
                val labels = WordCardLabels(
                    ai = stringResource(R.string.gallery_ai),
                    playPronunciation = stringResource(R.string.gallery_play_pronunciation),
                    tag = stringResource(R.string.gallery_tag),
                    more = stringResource(R.string.gallery_more),
                    synonyms = stringResource(R.string.card_synonyms),
                    antonyms = stringResource(R.string.card_antonyms),
                )
                Column(verticalArrangement = Arrangement.spacedBy(spacing.m)) {
                    WordCard(
                        model = model,
                        labels = labels,
                        onClick = {},
                        onTts = {},
                        onTag = {},
                        onMore = {},
                    )
                    WordCard(
                        model = model,
                        labels = labels,
                        onClick = {},
                        onTts = {},
                        onTag = {},
                        onMore = {},
                        compact = true,
                    )
                }
            }

            GallerySection(title = stringResource(R.string.gallery_section_grading)) {
                val gradeLabels = GradeLabels(
                    again = stringResource(R.string.gallery_grade_again),
                    hard = stringResource(R.string.gallery_grade_hard),
                    good = stringResource(R.string.gallery_grade_good),
                    easy = stringResource(R.string.gallery_grade_easy),
                )
                Column(verticalArrangement = Arrangement.spacedBy(spacing.s)) {
                    GradeBar(
                        labels = gradeLabels,
                        onGrade = { tone ->
                            lastGrade = tone
                            haptics.save()
                        },
                    )
                    val grade = lastGrade
                    if (grade != null) {
                        Text(
                            text = stringResource(
                                R.string.gallery_last_grade,
                                gradeText(grade, gradeLabels),
                            ),
                            style = ReelTheme.type.label,
                            color = colors.textTertiary,
                        )
                    }
                }
            }

            GallerySection(title = stringResource(R.string.gallery_section_progress)) {
                Column(verticalArrangement = Arrangement.spacedBy(spacing.m)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(spacing.xl),
                    ) {
                        StreakRing(
                            current = 4,
                            label = stringResource(R.string.gallery_streak_label),
                        )
                        ReviewProgressDots(total = 5, current = 2)
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(spacing.s)) {
                        StatTile(
                            label = stringResource(R.string.gallery_stat_words),
                            value = stringResource(R.string.gallery_stat_words_value),
                            modifier = Modifier.weight(1f),
                        )
                        StatTile(
                            label = stringResource(R.string.gallery_stat_films),
                            value = stringResource(R.string.gallery_stat_films_value),
                            modifier = Modifier.weight(1f),
                        )
                    }
                    StatTile(
                        label = stringResource(R.string.gallery_stat_accuracy),
                        value = stringResource(R.string.gallery_stat_accuracy_value),
                        delta = stringResource(R.string.gallery_stat_delta),
                        accent = colors.teal,
                    )
                }
            }

            GallerySection(title = stringResource(R.string.gallery_section_audio)) {
                Column(verticalArrangement = Arrangement.spacedBy(spacing.m)) {
                    WaveformMini(
                        amplitudes = listOf(0.3f, 0.6f, 0.9f, 0.5f, 0.75f, 0.4f, 1f, 0.55f, 0.2f),
                        active = playing,
                    )
                    AudioPlayerBar(
                        state = AudioBarState(
                            available = true,
                            playing = playing,
                            progress = 0.35f,
                            label = stringResource(R.string.gallery_audio_label),
                            kept = false,
                        ),
                        playLabel = stringResource(R.string.gallery_audio_play),
                        pauseLabel = stringResource(R.string.gallery_audio_pause),
                        keepLabel = stringResource(R.string.gallery_audio_keep),
                        keptLabel = stringResource(R.string.gallery_audio_kept),
                        unavailableLabel = stringResource(R.string.gallery_audio_unavailable),
                        onPlayPause = { playing = !playing },
                        onKeep = {},
                    )
                    AudioPlayerBar(
                        state = AudioBarState(
                            available = false,
                            playing = false,
                            progress = 0f,
                            label = stringResource(R.string.gallery_audio_label),
                            kept = false,
                        ),
                        playLabel = stringResource(R.string.gallery_audio_play),
                        pauseLabel = stringResource(R.string.gallery_audio_pause),
                        keepLabel = stringResource(R.string.gallery_audio_keep),
                        keptLabel = stringResource(R.string.gallery_audio_kept),
                        unavailableLabel = stringResource(R.string.gallery_audio_unavailable),
                        onPlayPause = {},
                        onKeep = null,
                    )
                }
            }

            GallerySection(title = stringResource(R.string.gallery_section_states)) {
                Column(verticalArrangement = Arrangement.spacedBy(spacing.m)) {
                    LoadingState(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(colors.surface),
                        message = stringResource(R.string.gallery_loading_message),
                    )
                    OfflineBanner(
                        message = stringResource(R.string.gallery_offline),
                        retryLabel = stringResource(R.string.gallery_offline_retry),
                        onRetry = {},
                    )
                    InlineError(text = stringResource(R.string.gallery_inline_error))
                    EmptyState(
                        title = stringResource(R.string.gallery_empty_title),
                        body = stringResource(R.string.gallery_empty_body),
                        ctaLabel = stringResource(R.string.gallery_empty_cta),
                        onCta = {},
                        illustration = ReelIllustration.EmptyDeck,
                    )
                    ErrorState(
                        title = stringResource(R.string.gallery_error_title),
                        body = stringResource(R.string.gallery_error_body),
                        retryLabel = stringResource(R.string.gallery_error_retry),
                        onRetry = {},
                    )
                }
            }

            GallerySection(title = stringResource(R.string.gallery_section_sheets)) {
                Column(verticalArrangement = Arrangement.spacedBy(spacing.s)) {
                    ReelButton(
                        text = stringResource(R.string.gallery_open_sheet),
                        onClick = { sheet = GallerySheet.Cards },
                        style = ReelButtonStyle.Secondary,
                    )
                    ReelButton(
                        text = stringResource(R.string.gallery_open_permission),
                        onClick = { sheet = GallerySheet.Permission },
                        style = ReelButtonStyle.Secondary,
                    )
                    ReelButton(
                        text = stringResource(R.string.gallery_open_confirm),
                        onClick = { sheet = GallerySheet.Confirm },
                        style = ReelButtonStyle.Secondary,
                    )
                }
            }

            GallerySection(title = stringResource(R.string.gallery_section_illustrations)) {
                Column(verticalArrangement = Arrangement.spacedBy(spacing.m)) {
                    listOf(
                        ReelIllustration.Poster,
                        ReelIllustration.EmptyDeck,
                        ReelIllustration.Ticket,
                        ReelIllustration.Projector,
                    ).chunked(2).forEach { row ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(spacing.m),
                        ) {
                            row.forEach { illustration ->
                                Box(
                                    modifier = Modifier.weight(1f),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    ReelIllustrationArt(illustration = illustration)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    when (sheet) {
        GallerySheet.None -> Unit

        GallerySheet.Cards -> ReelSheet(
            onDismiss = { sheet = GallerySheet.None },
            title = stringResource(R.string.gallery_sheet_title),
        ) {
            Text(
                text = stringResource(R.string.gallery_sheet_body),
                style = ReelTheme.type.bodyM,
                color = colors.textSecondary,
            )
            Spacer(Modifier.height(ReelTheme.spacing.xxl))
            ReelButton(
                text = stringResource(R.string.common_close),
                onClick = { sheet = GallerySheet.None },
            )
        }

        GallerySheet.Permission -> PermissionSheet(
            icon = Icons.Filled.Screenshot,
            title = stringResource(R.string.gallery_permission_title),
            body = stringResource(R.string.gallery_permission_body),
            why = stringResource(R.string.gallery_permission_why),
            confirmLabel = stringResource(R.string.gallery_permission_confirm),
            onConfirm = { sheet = GallerySheet.None },
            onDismiss = { sheet = GallerySheet.None },
        )

        GallerySheet.Confirm -> ConfirmDestructive(
            title = stringResource(R.string.gallery_confirm_title),
            body = stringResource(R.string.gallery_confirm_body),
            confirmLabel = stringResource(R.string.gallery_confirm_yes),
            dismissLabel = stringResource(R.string.gallery_confirm_no),
            onConfirm = { sheet = GallerySheet.None },
            onDismiss = { sheet = GallerySheet.None },
        )
    }
}

@Composable
private fun GallerySection(
    title: String,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = title,
            style = ReelTheme.type.label,
            color = ReelTheme.colors.textTertiary,
            modifier = Modifier.padding(bottom = ReelTheme.spacing.m),
        )
        content()
    }
}

@Composable
private fun Swatch(
    label: String,
    color: Color,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(ReelTheme.radius.chip)
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(36.dp)
                .clip(shape)
                .background(color)
                .border(ReelTheme.spacing.hairline, ReelTheme.colors.strokeSubtle, shape),
        )
        Spacer(Modifier.height(ReelTheme.spacing.xxs))
        Text(
            text = label,
            style = ReelTheme.type.label,
            color = ReelTheme.colors.textTertiary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun TypeSamples() {
    val type = ReelTheme.type
    val colors = ReelTheme.colors
    val samples = listOf(
        stringResource(R.string.gallery_word) to R.string.gallery_type_hero,
        stringResource(R.string.gallery_film) to R.string.gallery_type_display_l,
        stringResource(R.string.gallery_word) to R.string.gallery_type_display_m,
        stringResource(R.string.gallery_film) to R.string.gallery_type_title_l,
        stringResource(R.string.gallery_sentence) to R.string.gallery_type_body_l,
        stringResource(R.string.gallery_meaning) to R.string.gallery_type_body_m,
        stringResource(R.string.gallery_pos) to R.string.gallery_type_label,
        stringResource(R.string.gallery_ipa) to R.string.gallery_type_ipa,
        stringResource(R.string.gallery_audio_label) to R.string.gallery_type_mono,
    )
    val styles = listOf(
        type.hero,
        type.displayL,
        type.displayM,
        type.titleL,
        type.bodyL,
        type.bodyM,
        type.label,
        type.ipa,
        type.mono,
    )
    Column(verticalArrangement = Arrangement.spacedBy(ReelTheme.spacing.m)) {
        samples.forEachIndexed { index, (sample, labelRes) ->
            Column {
                Text(
                    text = sample,
                    style = styles[index],
                    color = colors.textPrimary,
                )
                Text(
                    text = stringResource(labelRes),
                    style = type.label,
                    color = colors.textTertiary,
                )
            }
        }
    }
}

private fun gradeText(tone: GradeTone, labels: GradeLabels): String = when (tone) {
    GradeTone.Again -> labels.again
    GradeTone.Hard -> labels.hard
    GradeTone.Good -> labels.good
    GradeTone.Easy -> labels.easy
}
