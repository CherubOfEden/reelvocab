package app.reelvocab.core.designsystem.component

import app.reelvocab.core.designsystem.icon.ReelIcons

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.reelvocab.core.designsystem.theme.ReelTheme

@Immutable
data class AudioBarState(
    val available: Boolean,
    val playing: Boolean,
    val progress: Float,
    val label: String,
    val kept: Boolean,
)

@Composable
fun AudioPlayerBar(
    state: AudioBarState,
    playLabel: String,
    pauseLabel: String,
    keepLabel: String,
    keptLabel: String,
    unavailableLabel: String,
    onPlayPause: () -> Unit,
    onKeep: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    val colors = ReelTheme.colors

    if (!state.available) {
        Row(
            modifier = modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = ReelIcons.VolumeX,
                contentDescription = null,
                tint = colors.textTertiary,
                modifier = Modifier.size(18.dp),
            )
            Spacer(Modifier.width(ReelTheme.spacing.s))
            Text(
                text = unavailableLabel,
                style = ReelTheme.type.bodyM,
                color = colors.textTertiary,
            )
        }
        return
    }

    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(ReelTheme.spacing.s),
    ) {
        IconButton(onClick = onPlayPause) {
            Icon(
                imageVector = if (state.playing) ReelIcons.Pause else ReelIcons.Play,
                contentDescription = if (state.playing) pauseLabel else playLabel,
                tint = colors.accent,
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            LinearProgressIndicator(
                progress = { state.progress.coerceIn(0f, 1f) },
                modifier = Modifier.fillMaxWidth(),
                color = colors.secondary,
                trackColor = colors.strokeSubtle,
            )
            Text(
                text = state.label,
                style = ReelTheme.type.mono,
                color = colors.textTertiary,
                modifier = Modifier.padding(top = ReelTheme.spacing.xxs),
            )
        }
        if (state.kept) {
            Text(
                text = keptLabel,
                style = ReelTheme.type.label,
                color = colors.textTertiary,
                modifier = Modifier.padding(horizontal = ReelTheme.spacing.s),
            )
        } else if (onKeep != null) {
            TextButton(onClick = onKeep) {
                Text(
                    text = keepLabel,
                    style = ReelTheme.type.label,
                    color = colors.accent,
                )
            }
        }
    }
}
