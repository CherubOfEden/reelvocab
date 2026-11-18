package app.reelvocab.feature.media

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.reelvocab.core.designsystem.component.CardSurface
import app.reelvocab.core.designsystem.component.EmptyState
import app.reelvocab.core.designsystem.component.ReelButton
import app.reelvocab.core.designsystem.component.ReelButtonStyle
import app.reelvocab.core.designsystem.theme.ReelTheme
import app.reelvocab.core.domain.model.MediaItem
import app.reelvocab.core.domain.model.MediaKind
import kotlinx.serialization.Serializable

@Serializable
data object MediaRoute

@Serializable
data class PlayerRoute(
    val uri: String,
    val title: String,
    val kind: String = "video",
)

@Composable
fun MediaScreen(
    onBack: () -> Unit,
    onPlay: (PlayerRoute) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: MediaViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val state by viewModel.state.collectAsStateWithLifecycle()
    val colors = ReelTheme.colors

    val required = remember { requiredMediaPermissions() }
    var granted by remember { mutableStateOf(hasMediaPermissions(context, required)) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { result ->
        granted = result.values.all { it } || hasMediaPermissions(context, required)
    }

    val openDocument = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri != null) {
            runCatching {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION,
                )
            }
            onPlay(
                PlayerRoute(
                    uri = uri.toString(),
                    title = uri.lastPathSegment?.substringAfterLast('/') ?: "Media",
                    kind = "video",
                ),
            )
        }
    }

    LaunchedEffect(Unit) {
        if (!granted) permissionLauncher.launch(required)
    }
    LaunchedEffect(granted) {
        if (granted) viewModel.scan()
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = ReelTheme.spacing.screenH,
            end = ReelTheme.spacing.screenH,
            top = ReelTheme.spacing.m,
            bottom = ReelTheme.spacing.screenBottom,
        ),
        verticalArrangement = Arrangement.spacedBy(ReelTheme.spacing.s),
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(R.string.media_back),
                        tint = colors.textPrimary,
                    )
                }
                Text(
                    text = stringResource(R.string.media_title),
                    style = ReelTheme.type.titleL,
                    color = colors.textPrimary,
                )
            }
        }

        item {
            Text(
                text = stringResource(R.string.media_subtitle),
                style = ReelTheme.type.bodyM,
                color = colors.textSecondary,
                modifier = Modifier.padding(bottom = ReelTheme.spacing.s),
            )
        }

        when {
            !granted -> item {
                MediaPermissionCard(
                    onAllow = { permissionLauncher.launch(required) },
                    onOpenFile = { openDocument.launch(arrayOf("video/*", "audio/*")) },
                )
            }

            state.error -> item {
                Column(verticalArrangement = Arrangement.spacedBy(ReelTheme.spacing.s)) {
                    Text(
                        text = stringResource(R.string.media_error),
                        style = ReelTheme.type.bodyM,
                        color = colors.rose,
                    )
                    ReelButton(
                        text = stringResource(R.string.media_retry),
                        onClick = { viewModel.scan(force = true) },
                        style = ReelButtonStyle.Secondary,
                    )
                }
            }

            state.loading && state.items.isEmpty() -> item {
                Text(
                    text = stringResource(R.string.media_loading),
                    style = ReelTheme.type.bodyM,
                    color = colors.textSecondary,
                )
            }

            state.scanned && state.items.isEmpty() -> item {
                EmptyState(
                    title = stringResource(R.string.media_empty_title),
                    body = stringResource(R.string.media_empty_body),
                    ctaLabel = stringResource(R.string.media_open_file),
                    onCta = { openDocument.launch(arrayOf("video/*", "audio/*")) },
                )
            }

            else -> {
                val videos = state.items.filter { it.kind == MediaKind.Video }
                val audio = state.items.filter { it.kind == MediaKind.Audio }
                if (videos.isNotEmpty()) {
                    item { MediaSectionHeader(stringResource(R.string.media_section_videos)) }
                    items(videos, key = { "v${it.id}" }) { item ->
                        MediaRow(item = item, onPlay = onPlay)
                    }
                }
                if (audio.isNotEmpty()) {
                    item { MediaSectionHeader(stringResource(R.string.media_section_audio)) }
                    items(audio, key = { "a${it.id}" }) { item ->
                        MediaRow(item = item, onPlay = onPlay)
                    }
                }
                item {
                    Spacer(Modifier.height(ReelTheme.spacing.s))
                    ReelButton(
                        text = stringResource(R.string.media_open_file),
                        onClick = { openDocument.launch(arrayOf("video/*", "audio/*")) },
                        style = ReelButtonStyle.Ghost,
                    )
                }
            }
        }
    }
}

@Composable
private fun MediaPermissionCard(
    onAllow: () -> Unit,
    onOpenFile: () -> Unit,
) {
    val colors = ReelTheme.colors
    CardSurface {
        Text(
            text = stringResource(R.string.media_permission_title),
            style = ReelTheme.type.titleM,
            color = colors.textPrimary,
        )
        Spacer(Modifier.height(ReelTheme.spacing.xs))
        Text(
            text = stringResource(R.string.media_permission_body),
            style = ReelTheme.type.bodyM,
            color = colors.textSecondary,
        )
        Spacer(Modifier.height(ReelTheme.spacing.m))
        ReelButton(
            text = stringResource(R.string.media_permission_cta),
            onClick = onAllow,
        )
        Spacer(Modifier.height(ReelTheme.spacing.xs))
        ReelButton(
            text = stringResource(R.string.media_open_file),
            onClick = onOpenFile,
            style = ReelButtonStyle.Ghost,
        )
    }
}

@Composable
private fun MediaSectionHeader(text: String) {
    Text(
        text = text,
        style = ReelTheme.type.label,
        color = ReelTheme.colors.textTertiary,
        modifier = Modifier.padding(top = ReelTheme.spacing.s),
    )
}

@Composable
private fun MediaRow(
    item: MediaItem,
    onPlay: (PlayerRoute) -> Unit,
) {
    val colors = ReelTheme.colors
    CardSurface(
        onClick = {
            onPlay(
                PlayerRoute(
                    uri = item.uri,
                    title = item.title,
                    kind = if (item.kind == MediaKind.Video) "video" else "audio",
                ),
            )
        },
        contentPadding = PaddingValues(
            horizontal = ReelTheme.spacing.m,
            vertical = ReelTheme.spacing.m,
        ),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(ReelTheme.spacing.m),
        ) {
            Icon(
                imageVector = Icons.Filled.PlayArrow,
                contentDescription = null,
                tint = colors.gold,
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.title,
                    style = ReelTheme.type.bodyL,
                    color = colors.textPrimary,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                )
                Text(
                    text = formatMediaDuration(item.durationMs),
                    style = ReelTheme.type.label,
                    color = colors.textTertiary,
                )
            }
        }
    }
}

private fun requiredMediaPermissions(): Array<String> =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        arrayOf(
            Manifest.permission.READ_MEDIA_VIDEO,
            Manifest.permission.READ_MEDIA_AUDIO,
        )
    } else {
        arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
    }

private fun hasMediaPermissions(context: Context, permissions: Array<String>): Boolean =
    permissions.all { permission ->
        ContextCompat.checkSelfPermission(context, permission) ==
            PackageManager.PERMISSION_GRANTED
    }

internal fun formatMediaDuration(durationMs: Long): String {
    val totalSeconds = (durationMs / 1000).coerceAtLeast(0)
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return if (hours > 0) {
        String.format(java.util.Locale.US, "%d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format(java.util.Locale.US, "%d:%02d", minutes, seconds)
    }
}
