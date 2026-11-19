package app.reelvocab.feature.today

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.PictureInPictureAlt
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import app.reelvocab.core.designsystem.component.CardSurface
import app.reelvocab.core.designsystem.component.ReelButton
import app.reelvocab.core.designsystem.component.ReelButtonStyle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.reelvocab.core.designsystem.component.EmptyState
import app.reelvocab.core.designsystem.component.StreakRing
import app.reelvocab.core.designsystem.illustration.ReelIllustration
import app.reelvocab.core.designsystem.theme.ReelTheme
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale
import kotlinx.serialization.Serializable

@Serializable
data object TodayRoute

@Immutable
data class TodayUiState(
    val streakDays: Int = 0,
    val dueCount: Int = 0,
    val cardCount: Int = 0,
    val loading: Boolean = true,
)

@Composable
fun TodayScreen(
    state: TodayUiState,
    pipActive: Boolean,
    onTogglePip: () -> Unit,
    onOpenAlarm: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenDeck: () -> Unit,
    onStartReview: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = ReelTheme.colors
    val dateLabel = remember {
        LocalDate.now().format(
            DateTimeFormatter.ofLocalizedDate(FormatStyle.FULL).withLocale(Locale.getDefault()),
        )
    }
    val streakLabel = pluralStringResource(
        R.plurals.today_streak,
        state.streakDays,
        state.streakDays,
    )

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = ReelTheme.spacing.screenH,
            end = ReelTheme.spacing.screenH,
            top = ReelTheme.spacing.xl,
            bottom = ReelTheme.spacing.screenBottom + ReelTheme.spacing.colossal,
        ),
        verticalArrangement = Arrangement.spacedBy(ReelTheme.spacing.l),
    ) {
        item {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = dateLabel,
                        style = ReelTheme.type.label,
                        color = colors.textTertiary,
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(ReelTheme.spacing.xs)) {
                        TodayAction(
                            icon = Icons.Filled.PictureInPictureAlt,
                            label = stringResource(R.string.today_label_capture),
                            contentDescription = stringResource(
                                if (pipActive) {
                                    R.string.today_pip_on
                                } else {
                                    R.string.today_pip_off
                                },
                            ),
                            tint = if (pipActive) colors.gold else colors.textSecondary,
                            onClick = onTogglePip,
                        )
                        TodayAction(
                            icon = Icons.Filled.Alarm,
                            label = stringResource(R.string.today_label_alarm),
                            contentDescription = stringResource(R.string.today_reminder),
                            tint = colors.textSecondary,
                            onClick = onOpenAlarm,
                        )
                        TodayAction(
                            icon = Icons.Filled.Settings,
                            label = stringResource(R.string.today_label_settings),
                            contentDescription = stringResource(R.string.today_settings),
                            tint = colors.textSecondary,
                            onClick = onOpenSettings,
                        )
                    }
                }
                Spacer(Modifier.height(ReelTheme.spacing.xs))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = stringResource(R.string.today_title),
                        style = ReelTheme.type.titleL,
                        color = colors.textPrimary,
                    )
                    StreakRing(
                        current = state.streakDays,
                        label = streakLabel,
                    )
                }
            }
        }

        item {
            Spacer(Modifier.height(ReelTheme.spacing.xxxl))
        }

        item {
            CardSurface {
                Text(
                    text = stringResource(R.string.today_review_title),
                    style = ReelTheme.type.titleM,
                    color = colors.textPrimary,
                )
                Spacer(Modifier.height(ReelTheme.spacing.xs))
                Text(
                    text = pluralStringResource(
                        R.plurals.today_due_now,
                        state.dueCount,
                        state.dueCount,
                    ),
                    style = ReelTheme.type.titleM,
                    color = if (state.dueCount > 0) colors.gold else colors.textSecondary,
                )
                Spacer(Modifier.height(ReelTheme.spacing.xs))
                Text(
                    text = stringResource(R.string.today_review_body),
                    style = ReelTheme.type.bodyM,
                    color = colors.textSecondary,
                )
                Spacer(Modifier.height(ReelTheme.spacing.m))
                ReelButton(
                    text = stringResource(R.string.today_review_cta),
                    onClick = onStartReview,
                    style = ReelButtonStyle.Secondary,
                )
            }
        }

        if (!state.loading && state.cardCount == 0) {
            item {
                EmptyState(
                    title = stringResource(R.string.today_empty_title),
                    body = stringResource(R.string.today_empty_body),
                    ctaLabel = stringResource(R.string.today_cta_deck),
                    onCta = onOpenDeck,
                    illustration = ReelIllustration.Poster,
                )
            }
        }

        if (!state.loading && state.cardCount > 0) {
            item {
                CardSurface(onClick = onOpenDeck) {
                    Text(
                        text = stringResource(R.string.today_deck_title),
                        style = ReelTheme.type.titleM,
                        color = colors.textPrimary,
                    )
                    Spacer(Modifier.height(ReelTheme.spacing.xs))
                    Text(
                        text = pluralStringResource(
                            R.plurals.today_deck_count,
                            state.cardCount,
                            state.cardCount,
                        ),
                        style = ReelTheme.type.bodyM,
                        color = colors.textSecondary,
                    )
                }
            }
        }
    }
}

@Composable
private fun TodayAction(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    contentDescription: String,
    tint: androidx.compose.ui.graphics.Color,
    onClick: () -> Unit,
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        IconButton(onClick = onClick, modifier = Modifier.size(36.dp)) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = tint,
                modifier = Modifier.size(20.dp),
            )
        }
        Text(
            text = label,
            style = ReelTheme.type.label,
            color = ReelTheme.colors.textTertiary,
            textAlign = TextAlign.Center,
            maxLines = 1,
        )
    }
}
