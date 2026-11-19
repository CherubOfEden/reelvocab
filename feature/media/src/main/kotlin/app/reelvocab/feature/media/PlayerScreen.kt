package app.reelvocab.feature.media

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.ActivityInfo
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.widget.Toast
import android.view.WindowManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.BrightnessHigh
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import app.reelvocab.core.designsystem.component.ReelButton
import app.reelvocab.core.designsystem.theme.ReelTheme
import app.reelvocab.feature.capture.InAppCapture
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@androidx.annotation.OptIn(UnstableApi::class)
@Composable
fun PlayerScreen(
    route: PlayerRoute,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val activity = remember(context) { context.findActivity() }
    val scope = rememberCoroutineScope()

    val kind = remember(route.kind) {
        when (route.kind.lowercase()) {
            "video" -> PlayerKind.Video
            "audio" -> PlayerKind.Audio
            "image" -> PlayerKind.Image
            else -> PlayerKind.Image
        }
    }
    val playable = kind == PlayerKind.Video || kind == PlayerKind.Audio
    val isVideo = kind == PlayerKind.Video

    val tracks = remember(route.uri, route.queue) {
        if (route.queue.isNotEmpty()) {
            route.queue
        } else {
            listOf(
                PlayerTrack(
                    uri = route.uri,
                    title = route.title,
                    kind = route.kind,
                    mimeType = route.mimeType,
                ),
            )
        }
    }
    val startIndex = remember(route.index, tracks) {
        route.index.coerceIn(0, tracks.lastIndex.coerceAtLeast(0))
    }

    val player = remember(route.uri) {
        if (playable) {
            ExoPlayer.Builder(context).build()
        } else {
            null
        }
    }
    DisposableEffect(player) {
        onDispose { player?.release() }
    }
    DisposableEffect(activity) {
        val window = activity?.window
        window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        onDispose {
            window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        }
    }

    var subtitleUri by remember { mutableStateOf<Uri?>(null) }
    var selectedSubtitleMime by remember { mutableStateOf<String?>(null) }
    val subtitlePicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri != null) {
            runCatching {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION,
                )
            }
            selectedSubtitleMime = subtitleMimeType(context, uri)
            subtitleUri = uri
        }
    }

    LaunchedEffect(player, subtitleUri) {
        val exo = player ?: return@LaunchedEffect
        val hasItems = exo.mediaItemCount > 0
        val index = if (hasItems) {
            exo.currentMediaItemIndex.coerceIn(0, tracks.lastIndex.coerceAtLeast(0))
        } else {
            startIndex
        }
        val position = if (hasItems) exo.currentPosition.coerceAtLeast(0L) else 0L
        val resume = if (hasItems) exo.playWhenReady else true
        val items = tracks.mapIndexed { trackIndex, track ->
            val builder = MediaItem.Builder().setUri(track.uri)
            val selectedSubtitle = subtitleUri
            if (trackIndex == index && selectedSubtitle != null) {
                builder.setSubtitleConfigurations(
                    listOf(
                        MediaItem.SubtitleConfiguration.Builder(selectedSubtitle)
                            .setMimeType(selectedSubtitleMime ?: MimeTypes.APPLICATION_SUBRIP)
                            .setLanguage("en")
                            .build(),
                    ),
                )
            }
            builder.build()
        }
        exo.setMediaItems(items, index, position)
        exo.prepare()
        exo.playWhenReady = resume
    }

    var currentTitle by remember { mutableStateOf(tracks.getOrNull(startIndex)?.title ?: route.title) }
    var currentUri by remember { mutableStateOf(tracks.getOrNull(startIndex)?.uri ?: route.uri) }
    DisposableEffect(player, tracks) {
        val exo = player
        val listener = object : Player.Listener {
            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                val index = exo?.currentMediaItemIndex ?: return
                currentTitle = tracks.getOrNull(index)?.title ?: route.title
                currentUri = mediaItem?.localConfiguration?.uri?.toString() ?: route.uri
            }
        }
        exo?.addListener(listener)
        onDispose { exo?.removeListener(listener) }
    }

    var frame by remember { mutableStateOf<Bitmap?>(null) }
    var frameAtMs by remember { mutableLongStateOf(0L) }
    var frameTitle by remember { mutableStateOf(route.title) }
    var capturing by remember { mutableStateOf(false) }
    var menuOpen by remember { mutableStateOf(false) }
    var gestureFeedback by remember { mutableStateOf<GestureFeedback?>(null) }

    LaunchedEffect(gestureFeedback) {
        if (gestureFeedback != null) {
            delay(850)
            gestureFeedback = null
        }
    }

    Box(modifier = modifier.fillMaxSize().background(Color.Black)) {
        when (kind) {
            PlayerKind.Video, PlayerKind.Audio -> {
                val exo = player
                if (exo != null) {
                    AndroidView(
                        factory = { viewContext ->
                            PlayerView(viewContext).apply {
                                this.player = exo
                                useController = true
                                controllerShowTimeoutMs = 3_500
                            }
                        },
                        update = { view -> view.player = exo },
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }

            PlayerKind.Image -> {
                ImagePlayer(uri = route.uri)
            }
        }

        if (playable) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .fillMaxHeight(0.72f)
                    .fillMaxWidth(0.30f)
                    .pointerInput(player, activity) {
                        var totalX = 0f
                        var totalY = 0f
                        var horizontal: Boolean? = null
                        detectDragGestures(
                            onDragStart = {
                                totalX = 0f
                                totalY = 0f
                                horizontal = null
                            },
                            onDrag = { change, dragAmount ->
                                change.consume()
                                totalX += dragAmount.x
                                totalY += dragAmount.y
                                if (horizontal == null && (abs(totalX) > 12f || abs(totalY) > 12f)) {
                                    horizontal = abs(totalX) > abs(totalY)
                                }
                                val delta = when (horizontal) {
                                    true -> dragAmount.x / 600f
                                    false -> -dragAmount.y / 600f
                                    null -> 0f
                                }
                                if (delta != 0f) {
                                    val next = applyBrightness(activity, delta)
                                    gestureFeedback = GestureFeedback(GestureKind.Brightness, next)
                                }
                            },
                        )
                    },
            )
            Box(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .fillMaxHeight(0.72f)
                    .fillMaxWidth(0.30f)
                    .pointerInput(player) {
                        val exo = player ?: return@pointerInput
                        var totalX = 0f
                        var totalY = 0f
                        var horizontal: Boolean? = null
                        detectDragGestures(
                            onDragStart = {
                                totalX = 0f
                                totalY = 0f
                                horizontal = null
                            },
                            onDrag = { change, dragAmount ->
                                change.consume()
                                totalX += dragAmount.x
                                totalY += dragAmount.y
                                if (horizontal == null && (abs(totalX) > 12f || abs(totalY) > 12f)) {
                                    horizontal = abs(totalX) > abs(totalY)
                                }
                                val delta = when (horizontal) {
                                    true -> dragAmount.x / 600f
                                    false -> -dragAmount.y / 600f
                                    null -> 0f
                                }
                                if (delta != 0f) {
                                    val next = (exo.volume + delta).coerceIn(0f, 1f)
                                    exo.volume = next
                                    gestureFeedback = GestureFeedback(GestureKind.Volume, next)
                                }
                            },
                        )
                    },
            )
        }

        if (playable) {
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .fillMaxWidth(0.40f)
                    .fillMaxHeight(0.50f)
                    .pointerInput(player) {
                        detectTapGestures(
                            onDoubleTap = { offset ->
                                val exo = player ?: return@detectTapGestures
                                if (offset.x > size.width / 2f) {
                                    val end = exo.duration.takeIf { it > 0L } ?: Long.MAX_VALUE
                                    exo.seekTo((exo.currentPosition + 15_000L).coerceAtMost(end))
                                    Toast.makeText(
                                        context,
                                        R.string.media_seek_forward,
                                        Toast.LENGTH_SHORT,
                                    ).show()
                                } else {
                                    exo.seekTo((exo.currentPosition - 15_000L).coerceAtLeast(0L))
                                    Toast.makeText(
                                        context,
                                        R.string.media_seek_back,
                                        Toast.LENGTH_SHORT,
                                    ).show()
                                }
                            },
                        )
                    },
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.Black.copy(alpha = 0.48f))
                .statusBarsPadding()
                .padding(horizontal = ReelTheme.spacing.xs, vertical = ReelTheme.spacing.xs),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.media_back),
                    tint = Color.White,
                )
            }
            Text(
                text = currentTitle,
                style = ReelTheme.type.bodyL,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            if (isVideo) {
                IconButton(
                    onClick = {
                        if (capturing) return@IconButton
                        val exo = player ?: return@IconButton
                        capturing = true
                        exo.pause()
                        val atMs = exo.currentPosition.coerceAtLeast(0L)
                        val captureUri = currentUri
                        val captureTitle = currentTitle
                        scope.launch {
                            val bitmap = withContext(Dispatchers.IO) {
                                extractFrame(context, captureUri, atMs)
                            }
                            if (bitmap != null) {
                                frameAtMs = atMs
                                frameTitle = captureTitle
                                frame = bitmap
                            } else {
                                Toast.makeText(
                                    context,
                                    R.string.media_capture_failed,
                                    Toast.LENGTH_SHORT,
                                ).show()
                            }
                            capturing = false
                        }
                    },
                ) {
                    Icon(
                        imageVector = Icons.Filled.CameraAlt,
                        contentDescription = stringResource(R.string.media_capture),
                        tint = Color.White,
                    )
                }
            }
            if (playable) {
                Box {
                    IconButton(onClick = { menuOpen = true }) {
                        Icon(
                            imageVector = Icons.Filled.MoreVert,
                            contentDescription = stringResource(R.string.media_menu_more),
                            tint = Color.White,
                        )
                    }
                    PlayerMenu(
                        expanded = menuOpen,
                        player = player,
                        canGoNext = tracks.size > 1,
                        canShowSubtitles = isVideo,
                        onDismiss = { menuOpen = false },
                        onPrevious = {
                            player?.seekToPreviousMediaItem()
                            menuOpen = false
                        },
                        onNext = {
                            player?.seekToNextMediaItem()
                            menuOpen = false
                        },
                        onOrientation = { orientation ->
                            activity?.requestedOrientation = orientation
                            menuOpen = false
                        },
                        onSubtitles = {
                            menuOpen = false
                            subtitlePicker.launch(
                                arrayOf(
                                    "application/x-subrip",
                                    "text/vtt",
                                    "text/plain",
                                    "application/ttml+xml",
                                ),
                            )
                        },
                    )
                }
            }
        }

        val feedback = gestureFeedback
        if (feedback != null) {
            Row(
                modifier = Modifier
                    .align(Alignment.Center)
                    .clip(RoundedCornerShape(ReelTheme.radius.input))
                    .background(ReelTheme.colors.scrim)
                    .padding(horizontal = ReelTheme.spacing.l, vertical = ReelTheme.spacing.s),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(ReelTheme.spacing.s),
            ) {
                Icon(
                    imageVector = feedback.kind.icon,
                    contentDescription = null,
                    tint = Color.White,
                )
                Text(
                    text = stringResource(
                        feedback.kind.labelRes,
                        (feedback.value * 100).roundToInt(),
                    ),
                    style = ReelTheme.type.bodyM,
                    color = Color.White,
                )
            }
        }
    }

    val captured = frame
    if (captured != null) {
        InAppCapture(
            bitmap = captured,
            sourceTitle = frameTitle,
            timestampMs = frameAtMs,
            onDismiss = { frame = null },
            onSaved = { },
        )
    }
}

@Composable
private fun PlayerMenu(
    expanded: Boolean,
    player: ExoPlayer?,
    canGoNext: Boolean,
    canShowSubtitles: Boolean,
    onDismiss: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onOrientation: (Int) -> Unit,
    onSubtitles: () -> Unit,
) {
    val colors = ReelTheme.colors
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismiss,
    ) {
        if (player != null) {
            val playing = player.isPlaying
            DropdownMenuItem(
                text = {
                    Text(
                        text = stringResource(
                            if (playing) R.string.media_menu_pause else R.string.media_menu_play,
                        ),
                        style = ReelTheme.type.bodyM,
                        color = colors.textPrimary,
                    )
                },
                onClick = {
                    if (playing) player.pause() else player.play()
                    onDismiss()
                },
            )
            RepeatOption(
                label = stringResource(R.string.media_menu_repeat_off),
                selected = player.repeatMode == Player.REPEAT_MODE_OFF,
                onClick = {
                    player.repeatMode = Player.REPEAT_MODE_OFF
                    onDismiss()
                },
            )
            RepeatOption(
                label = stringResource(R.string.media_menu_repeat_one),
                selected = player.repeatMode == Player.REPEAT_MODE_ONE,
                onClick = {
                    player.repeatMode = Player.REPEAT_MODE_ONE
                    onDismiss()
                },
            )
            RepeatOption(
                label = stringResource(R.string.media_menu_repeat_all),
                selected = player.repeatMode == Player.REPEAT_MODE_ALL,
                onClick = {
                    player.repeatMode = Player.REPEAT_MODE_ALL
                    onDismiss()
                },
            )
        }
        if (player != null && canGoNext) {
            DropdownMenuItem(
                text = {
                    Text(
                        text = stringResource(R.string.media_menu_previous),
                        style = ReelTheme.type.bodyM,
                        color = colors.textPrimary,
                    )
                },
                onClick = onPrevious,
            )
            DropdownMenuItem(
                text = {
                    Text(
                        text = stringResource(R.string.media_menu_next),
                        style = ReelTheme.type.bodyM,
                        color = colors.textPrimary,
                    )
                },
                onClick = onNext,
            )
        }
        if (player != null) {
            DropdownMenuItem(
                text = {
                    Text(
                        text = stringResource(R.string.media_menu_portrait),
                        style = ReelTheme.type.bodyM,
                        color = colors.textPrimary,
                    )
                },
                onClick = {
                    onOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT)
                },
            )
            DropdownMenuItem(
                text = {
                    Text(
                        text = stringResource(R.string.media_menu_landscape),
                        style = ReelTheme.type.bodyM,
                        color = colors.textPrimary,
                    )
                },
                onClick = {
                    onOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE)
                },
            )
            DropdownMenuItem(
                text = {
                    Text(
                        text = stringResource(R.string.media_menu_auto_rotate),
                        style = ReelTheme.type.bodyM,
                        color = colors.textPrimary,
                    )
                },
                onClick = {
                    onOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED)
                },
            )
        }
        if (canShowSubtitles) {
            DropdownMenuItem(
                text = {
                    Text(
                        text = stringResource(R.string.media_menu_subtitles),
                        style = ReelTheme.type.bodyM,
                        color = colors.textPrimary,
                    )
                },
                onClick = onSubtitles,
            )
        }
    }
}

@Composable
private fun RepeatOption(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val colors = ReelTheme.colors
    DropdownMenuItem(
        text = {
            Text(
                text = if (selected) "✓ $label" else label,
                style = ReelTheme.type.bodyM,
                color = if (selected) colors.gold else colors.textPrimary,
            )
        },
        onClick = onClick,
    )
}

@Composable
private fun ImagePlayer(uri: String) {
    val context = LocalContext.current
    val colors = ReelTheme.colors
    var image by remember(uri) { mutableStateOf<Bitmap?>(null) }
    LaunchedEffect(uri) {
        image = withContext(Dispatchers.IO) { loadImage(context, uri) }
    }
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        val bitmap = image
        if (bitmap != null) {
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Fit,
            )
        } else {
            Text(
                text = stringResource(R.string.media_image_loading),
                style = ReelTheme.type.bodyM,
                color = colors.textSecondary,
            )
        }
    }
}


private const val MAX_IMAGE_DIMENSION = 2_048

private enum class PlayerKind { Video, Image, Audio }

private enum class GestureKind(
    val icon: ImageVector,
    val labelRes: Int,
) {
    Brightness(Icons.Filled.BrightnessHigh, R.string.media_brightness),
    Volume(Icons.Filled.VolumeUp, R.string.media_volume),
}

private data class GestureFeedback(
    val kind: GestureKind,
    val value: Float,
)

private fun applyBrightness(activity: Activity?, delta: Float): Float {
    val window = activity?.window ?: return 0.5f
    val attributes = window.attributes
    val current = if (attributes.screenBrightness < 0f) {
        0.5f
    } else {
        attributes.screenBrightness
    }
    val next = (current + delta).coerceIn(0.05f, 1f)
    attributes.screenBrightness = next
    window.attributes = attributes
    return next
}

private fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

private fun subtitleMimeType(context: Context, uri: Uri): String {
    val resolved = context.contentResolver.getType(uri)
    val name = uri.lastPathSegment.orEmpty().lowercase()
    return when {
        resolved == MimeTypes.TEXT_VTT || name.endsWith(".vtt") -> MimeTypes.TEXT_VTT
        resolved == MimeTypes.APPLICATION_TTML || name.endsWith(".ttml") -> MimeTypes.APPLICATION_TTML
        else -> MimeTypes.APPLICATION_SUBRIP
    }
}

private fun loadImage(context: Context, uri: String): Bitmap? = runCatching {
    val parsed = Uri.parse(uri)
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    context.contentResolver.openInputStream(parsed)?.use { stream ->
        BitmapFactory.decodeStream(stream, null, bounds)
    }
    if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return@runCatching null
    var sampleSize = 1
    while (
        bounds.outWidth / sampleSize > MAX_IMAGE_DIMENSION ||
        bounds.outHeight / sampleSize > MAX_IMAGE_DIMENSION
    ) {
        sampleSize *= 2
    }
    val options = BitmapFactory.Options().apply { inSampleSize = sampleSize }
    context.contentResolver.openInputStream(parsed)?.use { stream ->
        BitmapFactory.decodeStream(stream, null, options)
    }
}.getOrNull()

private fun extractFrame(context: Context, uri: String, atMs: Long): Bitmap? = runCatching {
    val retriever = MediaMetadataRetriever()
    try {
        retriever.setDataSource(context, Uri.parse(uri))
        val frameTimeUs = atMs * 1000
        retriever.getFrameAtTime(frameTimeUs, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
            ?: retriever.getFrameAtTime(frameTimeUs, MediaMetadataRetriever.OPTION_CLOSEST)
    } finally {
        retriever.release()
    }
}.getOrNull()
