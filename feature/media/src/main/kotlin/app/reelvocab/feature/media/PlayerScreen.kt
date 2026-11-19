package app.reelvocab.feature.media

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.ActivityInfo
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import app.reelvocab.core.designsystem.theme.ReelTheme
import app.reelvocab.feature.capture.InAppCapture
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
        if (playable) ExoPlayer.Builder(context).build() else null
    }
    DisposableEffect(player) {
        onDispose { player?.release() }
    }
    DisposableEffect(activity) {
        val window = activity?.window
        window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        onDispose { window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON) }
    }

    var orientation by rememberSaveable {
        mutableStateOf(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED)
    }
    LaunchedEffect(activity, orientation) {
        activity?.requestedOrientation = orientation
    }
    val resetAndLeave: () -> Unit = {
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        onBack()
    }
    BackHandler(onBack = resetAndLeave)

    LaunchedEffect(player, tracks, startIndex) {
        val exo = player ?: return@LaunchedEffect
        exo.setMediaItems(
            tracks.map { MediaItem.Builder().setUri(it.uri).build() },
            startIndex,
            0L,
        )
        exo.prepare()
        exo.playWhenReady = true
    }

    var currentTitle by remember { mutableStateOf(tracks.getOrNull(startIndex)?.title ?: route.title) }
    var currentUri by remember { mutableStateOf(tracks.getOrNull(startIndex)?.uri ?: route.uri) }
    var isPlaying by remember { mutableStateOf(false) }
    var hasPrevious by remember { mutableStateOf(false) }
    var hasNext by remember { mutableStateOf(false) }
    var controlsVisible by remember { mutableStateOf(true) }
    var interactionTick by remember { mutableIntStateOf(0) }

    fun refreshPlayerState() {
        val exo = player ?: return
        isPlaying = exo.isPlaying
        hasPrevious = exo.hasPreviousMediaItem()
        hasNext = exo.hasNextMediaItem()
    }

    fun togglePlayback() {
        val exo = player ?: return
        if (exo.playbackState == Player.STATE_ENDED) {
            exo.seekTo(0L)
            exo.play()
        } else if (exo.isPlaying) {
            exo.pause()
        } else {
            exo.play()
        }
        refreshPlayerState()
    }

    fun seekRelative(deltaMs: Long) {
        val exo = player ?: return
        val duration = exo.duration.takeIf { it > 0L }
        val target = (exo.currentPosition + deltaMs).coerceAtLeast(0L)
        exo.seekTo(if (duration != null) target.coerceAtMost(duration) else target)
    }

    DisposableEffect(player, tracks) {
        val exo = player
        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(playing: Boolean) {
                isPlaying = playing
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                refreshPlayerState()
            }

            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                val index = exo?.currentMediaItemIndex ?: return
                currentTitle = tracks.getOrNull(index)?.title ?: route.title
                currentUri = tracks.getOrNull(index)?.uri ?: route.uri
                refreshPlayerState()
            }
        }
        exo?.addListener(listener)
        refreshPlayerState()
        onDispose { exo?.removeListener(listener) }
    }

    LaunchedEffect(controlsVisible, isPlaying, interactionTick) {
        if (controlsVisible && isPlaying) {
            delay(3_500)
            controlsVisible = false
        }
    }

    var frame by remember { mutableStateOf<Bitmap?>(null) }
    var frameAtMs by remember { mutableLongStateOf(0L) }
    var frameTitle by remember { mutableStateOf(route.title) }
    var capturing by remember { mutableStateOf(false) }
    var menuOpen by remember { mutableStateOf(false) }

    Box(modifier = modifier.fillMaxSize().background(Color.Black)) {
        when (kind) {
            PlayerKind.Video, PlayerKind.Audio -> {
                val exo = player
                if (exo != null) {
                    AndroidView(
                        factory = { viewContext ->
                            PlayerView(viewContext).apply {
                                this.player = exo
                                useController = false
                                keepScreenOn = true
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
                    .fillMaxSize()
                    .pointerInput(player) {
                        detectTapGestures(
                            onTap = {
                                controlsVisible = !controlsVisible
                                interactionTick++
                            },
                            onDoubleTap = { offset ->
                                val exo = player ?: return@detectTapGestures
                                if (offset.x >= size.width / 2f) {
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
                                controlsVisible = true
                                interactionTick++
                            },
                        )
                    },
            )
        }

        val showChrome = !playable || controlsVisible
        if (showChrome) {
            Row(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .background(Color.Black.copy(alpha = 0.55f))
                    .statusBarsPadding()
                    .padding(horizontal = ReelTheme.spacing.xs, vertical = ReelTheme.spacing.xs),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = resetAndLeave) {
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
                            refreshPlayerState()
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
                    val exo = player
                    if (exo != null) {
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
                                player = exo,
                                orientation = orientation,
                                onDismiss = { menuOpen = false },
                                onOrientation = { requested ->
                                    orientation = requested
                                    activity?.requestedOrientation = requested
                                    menuOpen = false
                                },
                            )
                        }
                    }
                }
            }

            if (playable) {
                PlayerControls(
                    isPlaying = isPlaying,
                    canGoPrevious = hasPrevious,
                    canGoNext = hasNext,
                    onPrevious = {
                        player?.seekToPreviousMediaItem()
                        interactionTick++
                    },
                    onRewind = {
                        seekRelative(-15_000L)
                        interactionTick++
                    },
                    onTogglePlay = {
                        togglePlayback()
                        interactionTick++
                    },
                    onForward = {
                        seekRelative(15_000L)
                        interactionTick++
                    },
                    onNext = {
                        player?.seekToNextMediaItem()
                        interactionTick++
                    },
                    modifier = Modifier.align(Alignment.BottomCenter),
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
private fun PlayerControls(
    isPlaying: Boolean,
    canGoPrevious: Boolean,
    canGoNext: Boolean,
    onPrevious: () -> Unit,
    onRewind: () -> Unit,
    onTogglePlay: () -> Unit,
    onForward: () -> Unit,
    onNext: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(Color.Black.copy(alpha = 0.55f))
            .navigationBarsPadding()
            .padding(horizontal = ReelTheme.spacing.s, vertical = ReelTheme.spacing.s),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceEvenly,
    ) {
        ControlButton(
            icon = Icons.Filled.SkipPrevious,
            description = stringResource(R.string.media_controls_previous),
            enabled = canGoPrevious,
            onClick = onPrevious,
        )
        ControlButton(
            icon = Icons.Filled.FastRewind,
            description = stringResource(R.string.media_controls_rewind),
            enabled = true,
            onClick = onRewind,
        )
        IconButton(
            onClick = onTogglePlay,
            modifier = Modifier.size(56.dp),
        ) {
            Icon(
                imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                contentDescription = stringResource(
                    if (isPlaying) R.string.media_controls_pause else R.string.media_controls_play,
                ),
                tint = Color.White,
                modifier = Modifier.size(36.dp),
            )
        }
        ControlButton(
            icon = Icons.Filled.FastForward,
            description = stringResource(R.string.media_controls_forward),
            enabled = true,
            onClick = onForward,
        )
        ControlButton(
            icon = Icons.Filled.SkipNext,
            description = stringResource(R.string.media_controls_next),
            enabled = canGoNext,
            onClick = onNext,
        )
    }
}

@Composable
private fun ControlButton(
    icon: ImageVector,
    description: String,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    IconButton(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.size(48.dp),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = description,
            tint = if (enabled) Color.White else Color.White.copy(alpha = 0.35f),
            modifier = Modifier.size(28.dp),
        )
    }
}

@Composable
private fun PlayerMenu(
    expanded: Boolean,
    player: ExoPlayer,
    orientation: Int,
    onDismiss: () -> Unit,
    onOrientation: (Int) -> Unit,
) {
    val colors = ReelTheme.colors
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismiss,
    ) {
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
        RotationOption(
            label = stringResource(R.string.media_menu_portrait),
            selected = orientation == ActivityInfo.SCREEN_ORIENTATION_PORTRAIT,
            onClick = { onOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT) },
        )
        RotationOption(
            label = stringResource(R.string.media_menu_landscape),
            selected = orientation == ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE,
            onClick = { onOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE) },
        )
        RotationOption(
            label = stringResource(R.string.media_menu_auto_rotate),
            selected = orientation == ActivityInfo.SCREEN_ORIENTATION_FULL_SENSOR,
            onClick = { onOrientation(ActivityInfo.SCREEN_ORIENTATION_FULL_SENSOR) },
        )
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
private fun RotationOption(
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

private fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
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
