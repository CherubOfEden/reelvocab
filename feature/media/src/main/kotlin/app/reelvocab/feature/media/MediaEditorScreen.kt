package app.reelvocab.feature.media

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import app.reelvocab.core.designsystem.component.ReelButton
import app.reelvocab.core.designsystem.component.ReelChip
import app.reelvocab.core.designsystem.theme.ReelTheme
import app.reelvocab.core.domain.model.VaultKind
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable

@Serializable
data class EditorRoute(
    val uri: String,
    val title: String,
    val kind: String,
)

@androidx.annotation.OptIn(UnstableApi::class)
@Composable
fun MediaEditorScreen(
    route: EditorRoute,
    onBack: () -> Unit,
    onSaved: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: MediaEditorViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    BackHandler(onBack = onBack)
    LaunchedEffect(route.uri) { viewModel.load(route) }
    LaunchedEffect(state.savedUri) {
        if (state.savedUri != null) {
            Toast.makeText(context, R.string.media_editor_saved, Toast.LENGTH_SHORT).show()
            onSaved()
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(ReelTheme.colors.base)
            .statusBarsPadding()
            .navigationBarsPadding(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = ReelTheme.spacing.xs, vertical = ReelTheme.spacing.xs),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.media_back),
                    tint = ReelTheme.colors.textPrimary,
                )
            }
            Text(
                text = stringResource(R.string.media_editor_title),
                style = ReelTheme.type.titleM,
                color = ReelTheme.colors.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
        }

        when {
            state.loading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = stringResource(R.string.media_editor_loading),
                        style = ReelTheme.type.bodyM,
                        color = ReelTheme.colors.textSecondary,
                    )
                }
            }

            state.loadError -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = stringResource(R.string.media_editor_failed),
                        style = ReelTheme.type.bodyM,
                        color = ReelTheme.colors.textSecondary,
                    )
                }
            }

            else -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = ReelTheme.spacing.screenH),
                    verticalArrangement = Arrangement.spacedBy(ReelTheme.spacing.m),
                ) {
                    EditorPreview(
                        uri = route.uri,
                        title = state.title.ifBlank { route.title },
                        kind = state.kind,
                        crop = state.crop,
                        sourceWidth = state.sourceWidth,
                        sourceHeight = state.sourceHeight,
                    )

                    if (state.kind != VaultKind.Image) {
                        TrimControls(
                            state = state,
                            onStartChange = viewModel::setStart,
                            onEndChange = viewModel::setEnd,
                        )
                    }

                    if (state.kind != VaultKind.Audio) {
                        Text(
                            text = stringResource(R.string.media_editor_crop),
                            style = ReelTheme.type.label,
                            color = ReelTheme.colors.textSecondary,
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(ReelTheme.spacing.s),
                        ) {
                            EditorCrop.entries.forEach { crop ->
                                ReelChip(
                                    text = stringResource(crop.labelRes()),
                                    selected = state.crop == crop,
                                    onClick = { viewModel.setCrop(crop) },
                                )
                            }
                        }
                    }

                    if (state.exporting) {
                        Text(
                            text = stringResource(R.string.media_editor_exporting, state.progress),
                            style = ReelTheme.type.bodyM,
                            color = ReelTheme.colors.textSecondary,
                        )
                        LinearProgressIndicator(
                            progress = { state.progress / 100f },
                            modifier = Modifier.fillMaxWidth(),
                            color = ReelTheme.colors.gold,
                            trackColor = ReelTheme.colors.strokeSubtle,
                        )
                    }

                    if (state.error) {
                        Text(
                            text = stringResource(R.string.media_editor_failed),
                            style = ReelTheme.type.bodyM,
                            color = ReelTheme.colors.rose,
                        )
                    }

                    Text(
                        text = stringResource(R.string.media_editor_keep_original),
                        style = ReelTheme.type.label,
                        color = ReelTheme.colors.textTertiary,
                    )
                    ReelButton(
                        text = stringResource(R.string.media_editor_save),
                        onClick = viewModel::export,
                        loading = state.exporting,
                        enabled = !state.exporting,
                    )
                    Spacer(Modifier.height(ReelTheme.spacing.xl))
                }
            }
        }
    }
}

@androidx.annotation.OptIn(UnstableApi::class)
@Composable
private fun EditorPreview(
    uri: String,
    title: String,
    kind: VaultKind,
    crop: EditorCrop,
    sourceWidth: Int,
    sourceHeight: Int,
) {
    val colors = ReelTheme.colors
    val shape = RoundedCornerShape(ReelTheme.radius.card)
    when (kind) {
        VaultKind.Video -> {
            val context = LocalContext.current
            val player = remember(uri) { ExoPlayer.Builder(context).build() }
            DisposableEffect(player) {
                player.setMediaItem(MediaItem.fromUri(uri))
                player.prepare()
                player.playWhenReady = false
                player.repeatMode = Player.REPEAT_MODE_OFF
                onDispose { player.release() }
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(previewAspect(crop, sourceWidth, sourceHeight))
                    .clip(shape)
                    .background(colors.elevated),
            ) {
                AndroidView(
                    factory = { viewContext ->
                        PlayerView(viewContext).apply {
                            this.player = player
                            useController = false
                            keepScreenOn = true
                        }
                    },
                    update = { view -> view.player = player },
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }

        VaultKind.Audio -> {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(shape)
                    .background(colors.elevated)
                    .padding(ReelTheme.spacing.m),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(ReelTheme.spacing.m),
            ) {
                Icon(
                    imageVector = Icons.Filled.GraphicEq,
                    contentDescription = null,
                    tint = colors.gold,
                    modifier = Modifier.size(40.dp),
                )
                Text(
                    text = title,
                    style = ReelTheme.type.bodyL,
                    color = colors.textPrimary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }

        VaultKind.Image -> {
            val context = LocalContext.current
            var bitmap by remember(uri) { mutableStateOf<Bitmap?>(null) }
            LaunchedEffect(uri) { bitmap = decodePreview(context, uri) }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(previewAspect(crop, sourceWidth, sourceHeight))
                    .clip(shape)
                    .background(colors.elevated),
                contentAlignment = Alignment.Center,
            ) {
                bitmap?.let { image ->
                    Image(
                        bitmap = image.asImageBitmap(),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                    )
                } ?: Icon(
                    imageVector = Icons.Filled.Movie,
                    contentDescription = null,
                    tint = colors.textTertiary,
                    modifier = Modifier.size(40.dp),
                )
            }
        }
    }
}

@Composable
private fun TrimControls(
    state: MediaEditorUiState,
    onStartChange: (Long) -> Unit,
    onEndChange: (Long) -> Unit,
) {
    val duration = state.durationMs.coerceAtLeast(1L)
    Column(verticalArrangement = Arrangement.spacedBy(ReelTheme.spacing.xs)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = stringResource(R.string.media_editor_trim),
                style = ReelTheme.type.label,
                color = ReelTheme.colors.textSecondary,
            )
            Text(
                text = "${formatEditorTime(state.startMs)} – ${formatEditorTime(state.endMs)}",
                style = ReelTheme.type.mono,
                color = ReelTheme.colors.gold,
            )
        }
        Text(
            text = stringResource(R.string.media_editor_start),
            style = ReelTheme.type.label,
            color = ReelTheme.colors.textTertiary,
        )
        Slider(
            value = state.startMs.toFloat().coerceIn(0f, duration.toFloat()),
            onValueChange = { onStartChange(it.toLong()) },
            valueRange = 0f..duration.toFloat(),
        )
        Text(
            text = stringResource(R.string.media_editor_end),
            style = ReelTheme.type.label,
            color = ReelTheme.colors.textTertiary,
        )
        Slider(
            value = state.endMs.toFloat().coerceIn(0f, duration.toFloat()),
            onValueChange = { onEndChange(it.toLong()) },
            valueRange = 0f..duration.toFloat(),
        )
    }
}

private suspend fun decodePreview(context: Context, uri: String): Bitmap? =
    withContext(Dispatchers.IO) {
        runCatching {
            val parsed = Uri.parse(uri)
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            context.contentResolver.openInputStream(parsed)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, bounds)
            }
            if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return@runCatching null
            var sample = 1
            while (
                bounds.outWidth / sample > PREVIEW_MAX_DIMENSION ||
                bounds.outHeight / sample > PREVIEW_MAX_DIMENSION
            ) {
                sample *= 2
            }
            val options = BitmapFactory.Options().apply { inSampleSize = sample }
            context.contentResolver.openInputStream(parsed)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, options)
            }
        }.getOrNull()
    }

private fun previewAspect(crop: EditorCrop, width: Int, height: Int): Float {
    crop.aspect?.let { return it }
    if (width > 0 && height > 0) return width.toFloat() / height.toFloat()
    return 16f / 9f
}

private fun EditorCrop.labelRes(): Int = when (this) {
    EditorCrop.Original -> R.string.media_editor_crop_original
    EditorCrop.Landscape -> R.string.media_editor_crop_landscape
    EditorCrop.Square -> R.string.media_editor_crop_square
    EditorCrop.Portrait -> R.string.media_editor_crop_portrait
}

private const val PREVIEW_MAX_DIMENSION = 2_048

private fun formatEditorTime(positionMs: Long): String {
    val totalSeconds = (positionMs.coerceAtLeast(0L) / 1_000L).toInt()
    val hours = totalSeconds / 3_600
    val minutes = (totalSeconds % 3_600) / 60
    val seconds = totalSeconds % 60
    val base = String.format(Locale.US, "%02d:%02d", minutes, seconds)
    return if (hours > 0) String.format(Locale.US, "%d:%s", hours, base) else base
}
