package app.reelvocab.feature.today

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
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
)

@Composable
fun TodayScreen(
    state: TodayUiState,
    onOpenSettings: () -> Unit,
    onOpenDeck: () -> Unit,
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
            bottom = ReelTheme.spacing.screenBottom,
        ),
        verticalArrangement = Arrangement.spacedBy(ReelTheme.spacing.l),
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = dateLabel,
                        style = ReelTheme.type.label,
                        color = colors.textTertiary,
                    )
                    Spacer(Modifier.height(ReelTheme.spacing.xs))
                    Text(
                        text = stringResource(R.string.today_title),
                        style = ReelTheme.type.titleL,
                        color = colors.textPrimary,
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    StreakRing(
                        current = state.streakDays,
                        label = streakLabel,
                    )
                    IconButton(onClick = onOpenSettings) {
                        Icon(
                            imageVector = Icons.Filled.Settings,
                            contentDescription = stringResource(R.string.today_settings),
                            tint = colors.textSecondary,
                        )
                    }
                }
            }
        }

        item {
            Spacer(Modifier.height(ReelTheme.spacing.xxxl))
        }

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
}
