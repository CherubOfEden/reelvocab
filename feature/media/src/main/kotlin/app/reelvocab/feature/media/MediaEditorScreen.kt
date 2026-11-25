package app.reelvocab.feature.media

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.view.LayoutInflater
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
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
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
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
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntSize
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
import kotlinx.coroutines.delay
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
                        cropRect = state.cropRect,
                        cropEnabled = state.crop != EditorCrop.Original,
                        sourceWidth = state.sourceWidth,
                        sourceHeight = state.sourceHeight,
                        onCropRectChange = viewModel::setCropRect,
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
    cropRect: CropRect,
    cropEnabled: Boolean,
    sourceWidth: Int,
    sourceHeight: Int,
    onCropRectChange: (CropRect) -> Unit,
) {
    val colors = ReelTheme.colors
    val shape = RoundedCornerShape(ReelTheme.radius.card)
    val aspect = previewAspect(sourceWidth, sourceHeight)
    when (kind) {
        VaultKind.Video -> VideoPreview(
            uri = uri,
            aspect = aspect,
            shape = shape,
            cropRect = cropRect,
            cropEnabled = cropEnabled,
            onCropRectChange = onCropRectChange,
        )

        VaultKind.Audio -> AudioPreview(uri = uri, title = title, shape = shape)

        VaultKind.Image -> {
            val context = LocalContext.current
            var bitmap by remember(uri) { mutableStateOf<Bitmap?>(null) }
            LaunchedEffect(uri) { bitmap = decodePreview(context, uri) }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(aspect)
                    .clip(shape)
                    .background(colors.elevated),
                contentAlignment = Alignment.Center,
            ) {
                bitmap?.let { image ->
                    Image(
                        bitmap = image.asImageBitmap(),
                        contentDescription = null,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.fillMaxSize(),
                    )
                } ?: Icon(
                    imageVector = Icons.Filled.Movie,
                    contentDescription = null,
                    tint = colors.textTertiary,
                    modifier = Modifier.size(40.dp),
                )
                if (cropEnabled) {
                    CropOverlay(
                        rect = cropRect,
                        onRectChange = onCropRectChange,
                    )
                }
            }
        }
    }
}

@androidx.annotation.OptIn(UnstableApi::class)
@Composable
private fun VideoPreview(
    uri: String,
    aspect: Float,
    shape: RoundedCornerShape,
    cropRect: CropRect,
    onCropRectChange: (CropRect) -> Unit,
) {
    val colors = ReelTheme.colors
    val context = LocalContext.current
    val player = remember(uri) { ExoPlayer.Builder(context).build() }
    var positionMs by remember(player) { mutableStateOf(0L) }
    var durationMs by remember(player) { mutableStateOf(0L) }
    var playing by remember(player) { mutableStateOf(false) }

    DisposableEffect(player) {
        player.setMediaItem(MediaItem.fromUri(uri))
        player.prepare()
        player.playWhenReady = false
        player.repeatMode = Player.REPEAT_MODE_OFF
        onDispose { player.release() }
    }

    LaunchedEffect(player) {
        while (true) {
            positionMs = player.currentPosition.coerceAtLeast(0L)
            durationMs = player.duration.coerceAtLeast(0L)
            playing = player.isPlaying
            delay(250L)
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(ReelTheme.spacing.xs)) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(aspect)
                .clip(shape)
                .background(colors.elevated),
        ) {
            AndroidView(
                factory = { viewContext ->
                    val view = LayoutInflater.from(viewContext)
                        .inflate(R.layout.media_player_view, null) as PlayerView
                    view.apply {
                        this.player = player
                        keepScreenOn = true
                    }
                },
                update = { view -> view.player = player },
                modifier = Modifier.fillMaxSize(),
            )
            if (cropEnabled) {
                CropOverlay(
                    rect = cropRect,
                    onRectChange = onCropRectChange,
                )
            }
        }
        PlaybackControls(
            positionMs = positionMs,
            durationMs = durationMs,
            playing = playing,
            onPlayPause = {
                if (player.isPlaying) {
                    player.pause()
                } else {
                    if (durationMs > 0L && player.currentPosition >= durationMs - 250L) {
                        player.seekTo(0L)
                    }
                    player.play()
                }
            },
            onSeek = { player.seekTo(it) },
        )
    }
}

@androidx.annotation.OptIn(UnstableApi::class)
@Composable
private fun AudioPreview(
    uri: String,
    title: String,
    shape: RoundedCornerShape,
) {
    val colors = ReelTheme.colors
    val context = LocalContext.current
    val player = remember(uri) { ExoPlayer.Builder(context).build() }
    var positionMs by remember(player) { mutableStateOf(0L) }
    var durationMs by remember(player) { mutableStateOf(0L) }
    var playing by remember(player) { mutableStateOf(false) }

    DisposableEffect(player) {
        player.setMediaItem(MediaItem.fromUri(uri))
        player.prepare()
        player.playWhenReady = false
        onDispose { player.release() }
    }

    LaunchedEffect(player) {
        while (true) {
            positionMs = player.currentPosition.coerceAtLeast(0L)
            durationMs = player.duration.coerceAtLeast(0L)
            playing = player.isPlaying
            delay(250L)
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(ReelTheme.spacing.xs)) {
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
        PlaybackControls(
            positionMs = positionMs,
            durationMs = durationMs,
            playing = playing,
            onPlayPause = {
                if (player.isPlaying) player.pause() else player.play()
            },
            onSeek = { player.seekTo(it) },
        )
    }
}

@Composable
private fun PlaybackControls(
    positionMs: Long,
    durationMs: Long,
    playing: Boolean,
    onPlayPause: () -> Unit,
    onSeek: (Long) -> Unit,
) {
    val colors = ReelTheme.colors
    val duration = durationMs.coerceAtLeast(1L)
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(ReelTheme.spacing.xs),
    ) {
        IconButton(onClick = onPlayPause) {
            Icon(
                imageVector = if (playing) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                contentDescription = stringResource(
                    if (playing) R.string.media_editor_pause else R.string.media_editor_play,
                ),
                tint = colors.gold,
            )
        }
        Slider(
            value = positionMs.coerceIn(0L, duration).toFloat(),
            onValueChange = { onSeek(it.toLong()) },
            valueRange = 0f..duration.toFloat(),
            modifier = Modifier.weight(1f),
        )
        Text(
            text = formatEditorTime(positionMs),
            style = ReelTheme.type.mono,
            color = colors.textSecondary,
        )
    }
}

@Composable
private fun CropOverlay(
    rect: CropRect,
    onRectChange: (CropRect) -> Unit,
) {
    val colors = ReelTheme.colors
    val density = LocalDensity.current
    val handleRadius = with(density) { 32.dp.toPx() }
    val handleVisualRadius = with(density) { 7.dp.toPx() }
    var viewSize by remember { mutableStateOf(IntSize.Zero) }
    val currentRect by rememberUpdatedState(rect)
    val currentOnRectChange by rememberUpdatedState(onRectChange)

    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .onSizeChanged { viewSize = it }
            .pointerInput(viewSize, handleRadius) {
                if (viewSize.width <= 0 || viewSize.height <= 0) return@pointerInput
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    var frame = currentRect
                    val corner = nearestCorner(
                        frame,
                        down.position,
                        viewSize,
                        handleRadius,
                    ) ?: return@awaitEachGesture
                    down.consume()
                    val pointerId = down.id
                    while (true) {
                        val change = awaitPointerEvent()
                            .changes
                            .firstOrNull { it.id == pointerId }
                            ?: break
                        if (!change.pressed) break
                        val delta = change.positionChange()
                        if (delta != Offset.Zero) {
                            frame = frame.dragCorner(
                                corner = corner,
                                dx = delta.x / viewSize.width,
                                dy = delta.y / viewSize.height,
                            )
                            currentOnRectChange(frame)
                            change.consume()
                        }
                    }
                }
            },
    ) {
        val left = rect.left * size.width
        val top = rect.top * size.height
        val right = rect.right * size.width
        val bottom = rect.bottom * size.height
        val shade = Color.Black.copy(alpha = 0.55f)
        drawRect(shade, topLeft = Offset.Zero, size = Size(size.width, top))
        drawRect(shade, topLeft = Offset(0f, bottom), size = Size(size.width, size.height - bottom))
        drawRect(shade, topLeft = Offset(0f, top), size = Size(left, bottom - top))
        drawRect(
            shade,
            topLeft = Offset(right, top),
            size = Size(size.width - right, bottom - top),
        )
        drawRect(
            color = Color.White,
            topLeft = Offset(left, top),
            size = Size((right - left).coerceAtLeast(0f), (bottom - top).coerceAtLeast(0f)),
            style = Stroke(width = 2.dp.toPx()),
        )
        for (step in 1..2) {
            val x = left + (right - left) * step / 3f
            val y = top + (bottom - top) * step / 3f
            drawLine(
                color = Color.White.copy(alpha = 0.45f),
                start = Offset(x, top),
                end = Offset(x, bottom),
                strokeWidth = 1.dp.toPx(),
            )
            drawLine(
                color = Color.White.copy(alpha = 0.45f),
                start = Offset(left, y),
                end = Offset(right, y),
                strokeWidth = 1.dp.toPx(),
            )
        }
        cropCorners(rect, size).forEach { point ->
            drawCircle(
                color = colors.gold,
                radius = handleVisualRadius,
                center = point,
            )
            drawCircle(
                color = Color.Black.copy(alpha = 0.4f),
                radius = handleVisualRadius,
                center = point,
                style = Stroke(width = 1.dp.toPx()),
            )
        }
    }
}

private enum class CropCorner {
    TopLeft,
    TopRight,
    BottomLeft,
    BottomRight,
}

private fun cropCorners(rect: CropRect, size: Size): List<Offset> = listOf(
    Offset(rect.left * size.width, rect.top * size.height),
    Offset(rect.right * size.width, rect.top * size.height),
    Offset(rect.left * size.width, rect.bottom * size.height),
    Offset(rect.right * size.width, rect.bottom * size.height),
)

private fun nearestCorner(
    rect: CropRect,
    offset: Offset,
    size: IntSize,
    threshold: Float,
): CropCorner? {
    val corners = listOf(
        CropCorner.TopLeft to Offset(rect.left * size.width, rect.top * size.height),
        CropCorner.TopRight to Offset(rect.right * size.width, rect.top * size.height),
        CropCorner.BottomLeft to Offset(rect.left * size.width, rect.bottom * size.height),
        CropCorner.BottomRight to Offset(rect.right * size.width, rect.bottom * size.height),
    )
    return corners
        .minByOrNull { (_, point) -> (point - offset).getDistance() }
        ?.takeIf { (_, point) -> (point - offset).getDistance() <= threshold }
        ?.first
}

private fun CropRect.dragCorner(
    corner: CropCorner,
    dx: Float,
    dy: Float,
): CropRect {
    val minSize = MIN_CROP_SIZE
    val safeLeft = left.coerceIn(0f, 1f - minSize)
    val safeTop = top.coerceIn(0f, 1f - minSize)
    val safeRight = right.coerceIn(safeLeft + minSize, 1f)
    val safeBottom = bottom.coerceIn(safeTop + minSize, 1f)
    return when (corner) {
        CropCorner.TopLeft -> CropRect(
            left = (safeLeft + dx).coerceIn(0f, safeRight - minSize),
            top = (safeTop + dy).coerceIn(0f, safeBottom - minSize),
            right = safeRight,
            bottom = safeBottom,
        )

        CropCorner.TopRight -> CropRect(
            left = safeLeft,
            top = (safeTop + dy).coerceIn(0f, safeBottom - minSize),
            right = (safeRight + dx).coerceIn(safeLeft + minSize, 1f),
            bottom = safeBottom,
        )

        CropCorner.BottomLeft -> CropRect(
            left = (safeLeft + dx).coerceIn(0f, safeRight - minSize),
            top = safeTop,
            right = safeRight,
            bottom = (safeBottom + dy).coerceIn(safeTop + minSize, 1f),
        )

        CropCorner.BottomRight -> CropRect(
            left = safeLeft,
            top = safeTop,
            right = (safeRight + dx).coerceIn(safeLeft + minSize, 1f),
            bottom = (safeBottom + dy).coerceIn(safeTop + minSize, 1f),
        )
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

private fun previewAspect(width: Int, height: Int): Float {
    if (width > 0 && height > 0) return width.toFloat() / height.toFloat()
    return 16f / 9f
}

private fun EditorCrop.labelRes(): Int = when (this) {
    EditorCrop.Original -> R.string.media_editor_crop_original
    EditorCrop.Free -> R.string.media_editor_crop_free
    EditorCrop.Landscape -> R.string.media_editor_crop_landscape
    EditorCrop.Square -> R.string.media_editor_crop_square
    EditorCrop.Portrait -> R.string.media_editor_crop_portrait
}

private const val PREVIEW_MAX_DIMENSION = 2_048
private const val MIN_CROP_SIZE = 0.08f

private fun formatEditorTime(positionMs: Long): String {
    val totalSeconds = (positionMs.coerceAtLeast(0L) / 1_000L).toInt()
    val hours = totalSeconds / 3_600
    val minutes = (totalSeconds % 3_600) / 60
    val seconds = totalSeconds % 60
    val base = String.format(Locale.US, "%02d:%02d", minutes, seconds)
    return if (hours > 0) String.format(Locale.US, "%d:%s", hours, base) else base
}
