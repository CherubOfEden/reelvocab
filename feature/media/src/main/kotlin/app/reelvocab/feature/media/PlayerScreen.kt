package app.reelvocab.feature.media

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.ActivityInfo
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.AudioManager
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.BrightnessHigh
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
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
        if (playable) ExoPlayer.Builder(context).build() else null
    }
    DisposableEffect(player) {
        player?.repeatMode = Player.REPEAT_MODE_OFF
        player?.pauseAtEndOfMediaItems = true
        onDispose { player?.release() }
    }
    val initialBrightness = remember(activity) {
        activity?.window?.attributes?.screenBrightness ?: -1f
    }
    DisposableEffect(activity) {
        val window = activity?.window
        window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        onDispose {
            window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            window?.let { current ->
                val attributes = current.attributes
                attributes.screenBrightness = initialBrightness
                current.attributes = attributes
            }
        }
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
    var positionMs by remember { mutableLongStateOf(0L) }
    var durationMs by remember { mutableLongStateOf(0L) }
    var scrubbing by remember { mutableStateOf(false) }
    var scrubPositionMs by remember { mutableLongStateOf(0L) }
    var skipSeconds by rememberSaveable { mutableIntStateOf(15) }
    var showSkipDialog by remember { mutableStateOf(false) }
    var gestureFeedback by remember { mutableStateOf<GestureFeedback?>(null) }

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

    fun seekRelative(deltaSeconds: Int) {
        val exo = player ?: return
        val duration = exo.duration.takeIf { it > 0L }
        val target = (exo.currentPosition + deltaSeconds * 1_000L).coerceAtLeast(0L)
        exo.seekTo(if (duration != null) target.coerceAtMost(duration) else target)
        positionMs = exo.currentPosition.coerceAtLeast(0L)
    }

    LaunchedEffect(player) {
        val exo = player ?: return@LaunchedEffect
        while (true) {
            durationMs = exo.duration.takeIf { it > 0L } ?: 0L
            if (!scrubbing) {
                positionMs = exo.currentPosition.coerceAtLeast(0L)
            }
            delay(500)
        }
    }

    DisposableEffect(player, tracks) {
        val exo = player
        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(playing: Boolean) {
                isPlaying = playing
                if (!playing) {
                    controlsVisible = true
                }
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_ENDED &&
                    exo?.repeatMode == Player.REPEAT_MODE_OFF
                ) {
                    exo?.playWhenReady = false
                    controlsVisible = true
                }
                refreshPlayerState()
            }

            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                val index = exo?.currentMediaItemIndex ?: return
                currentTitle = tracks.getOrNull(index)?.title ?: route.title
                currentUri = tracks.getOrNull(index)?.uri ?: route.uri
                positionMs = 0L
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
                    .pointerInput(player, skipSeconds) {
                        detectTapGestures(
                            onTap = {
                                controlsVisible = !controlsVisible
                                interactionTick++
                            },
                            onDoubleTap = { offset ->
                                val exo = player ?: return@detectTapGestures
                                val stepMs = skipSeconds * 1_000L
                                if (offset.x >= size.width / 2f) {
                                    val end = exo.duration.takeIf { it > 0L } ?: Long.MAX_VALUE
                                    exo.seekTo((exo.currentPosition + stepMs).coerceAtMost(end))
                                    positionMs = exo.currentPosition.coerceAtLeast(0L)
                                    Toast.makeText(
                                        context,
                                        context.getString(R.string.media_seek_forward, skipSeconds),
                                        Toast.LENGTH_SHORT,
                                    ).show()
                                } else {
                                    exo.seekTo((exo.currentPosition - stepMs).coerceAtLeast(0L))
                                    positionMs = exo.currentPosition.coerceAtLeast(0L)
                                    Toast.makeText(
                                        context,
                                        context.getString(R.string.media_seek_back, skipSeconds),
                                        Toast.LENGTH_SHORT,
                                    ).show()
                                }
                                controlsVisible = true
                                interactionTick++
                            },
                        )
                    }
                    .pointerInput(player) {
                        var side: DragSide? = null
                        var totalDragY = 0f
                        var startVolumeFraction = 0f
                        var startBrightness = 0.5f
                        val audioManager = context.getSystemService(AudioManager::class.java)
                        detectVerticalDragGestures(
                            onDragStart = { offset ->
                                side = if (offset.x < size.width / 2f) {
                                    DragSide.Brightness
                                } else {
                                    DragSide.Volume
                                }
                                totalDragY = 0f
                                if (side == DragSide.Volume) {
                                    val maxVolume = audioManager
                                        ?.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
                                        ?.coerceAtLeast(1) ?: 1
                                    val currentVolume = audioManager
                                        ?.getStreamVolume(AudioManager.STREAM_MUSIC) ?: 0
                                    startVolumeFraction = currentVolume.toFloat() / maxVolume
                                    gestureFeedback = GestureFeedback.Volume(
                                        (startVolumeFraction * 100f).roundToInt(),
                                    )
                                } else {
                                    val window = activity?.window
                                    startBrightness = window?.attributes?.screenBrightness
                                        ?.takeIf { it >= 0f } ?: 0.5f
                                    gestureFeedback = GestureFeedback.Brightness(
                                        (startBrightness * 100f).roundToInt(),
                                    )
                                }
                            },
                            onVerticalDrag = { change, dragAmount ->
                                change.consume()
                                val activeSide = side
                                if (activeSide != null) {
                                    totalDragY += dragAmount
                                    val fractionDelta = -totalDragY / (size.height * 0.6f)
                                    when (activeSide) {
                                        DragSide.Volume -> {
                                            val maxVolume = audioManager
                                                ?.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
                                                ?.coerceAtLeast(1) ?: 1
                                            val fraction = (startVolumeFraction + fractionDelta)
                                                .coerceIn(0f, 1f)
                                            val index = (fraction * maxVolume)
                                                .roundToInt()
                                                .coerceIn(0, maxVolume)
                                            runCatching {
                                                audioManager?.setStreamVolume(
                                                    AudioManager.STREAM_MUSIC,
                                                    index,
                                                    0,
                                                )
                                            }
                                            gestureFeedback = GestureFeedback.Volume(
                                                (index * 100f / maxVolume).roundToInt(),
                                            )
                                        }

                                        DragSide.Brightness -> {
                                            val fraction = (startBrightness + fractionDelta)
                                                .coerceIn(0.05f, 1f)
                                            activity?.window?.let { window ->
                                                val attributes = window.attributes
                                                attributes.screenBrightness = fraction
                                                window.attributes = attributes
                                            }
                                            gestureFeedback = GestureFeedback.Brightness(
                                                (fraction * 100f).roundToInt(),
                                            )
                                        }
                                    }
                                    interactionTick++
                                }
                            },
                            onDragEnd = {
                                side = null
                                gestureFeedback = null
                            },
                            onDragCancel = {
                                side = null
                                gestureFeedback = null
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
                                skipSeconds = skipSeconds,
                                onDismiss = { menuOpen = false },
                                onOrientation = { requested ->
                                    orientation = requested
                                    activity?.requestedOrientation = requested
                                    menuOpen = false
                                },
                                onSkipDuration = {
                                    menuOpen = false
                                    showSkipDialog = true
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
                    skipSeconds = skipSeconds,
                    onPrevious = {
                        player?.seekToPreviousMediaItem()
                        interactionTick++
                    },
                    onRewind = {
                        seekRelative(-skipSeconds)
                        interactionTick++
                    },
                    onTogglePlay = {
                        togglePlayback()
                        interactionTick++
                    },
                    onForward = {
                        seekRelative(skipSeconds)
                        interactionTick++
                    },
                    onNext = {
                        player?.seekToNextMediaItem()
                        interactionTick++
                    },
                    modifier = Modifier.align(Alignment.Center),
                )
                PlayerTimeline(
                    positionMs = if (scrubbing) scrubPositionMs else positionMs,
                    durationMs = durationMs,
                    onScrub = { target ->
                        scrubbing = true
                        scrubPositionMs = target
                        interactionTick++
                    },
                    onScrubFinished = {
                        player?.seekTo(scrubPositionMs)
                        positionMs = scrubPositionMs
                        scrubbing = false
                        interactionTick++
                    },
                    modifier = Modifier.align(Alignment.BottomCenter),
                )
            }
        }

        gestureFeedback?.let { feedback ->
            val isVolume = feedback is GestureFeedback.Volume
            Row(
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(bottom = 120.dp)
                    .background(Color.Black.copy(alpha = 0.45f), RoundedCornerShape(12.dp))
                    .padding(horizontal = ReelTheme.spacing.m, vertical = ReelTheme.spacing.s),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(ReelTheme.spacing.s),
            ) {
                Icon(
                    imageVector = if (isVolume) Icons.Filled.VolumeUp else Icons.Filled.BrightnessHigh,
                    contentDescription = null,
                    tint = Color.White,
                )
                Text(
                    text = when (feedback) {
                        is GestureFeedback.Volume ->
                            stringResource(R.string.media_volume, feedback.percent)
                        is GestureFeedback.Brightness ->
                            stringResource(R.string.media_brightness, feedback.percent)
                    },
                    style = ReelTheme.type.bodyM,
                    color = Color.White,
                )
            }
        }
    }

    if (showSkipDialog) {
        SkipDurationDialog(
            selectedSeconds = skipSeconds,
            onSelect = { seconds ->
                skipSeconds = seconds
                showSkipDialog = false
                interactionTick++
            },
            onDismiss = { showSkipDialog = false },
        )
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
    skipSeconds: Int,
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
            description = stringResource(R.string.media_controls_rewind, skipSeconds),
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
            description = stringResource(R.string.media_controls_forward, skipSeconds),
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
private fun PlayerTimeline(
    positionMs: Long,
    durationMs: Long,
    onScrub: (Long) -> Unit,
    onScrubFinished: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val duration = durationMs.coerceAtLeast(0L)
    val fraction = if (duration > 0L) {
        (positionMs.toFloat() / duration).coerceIn(0f, 1f)
    } else {
        0f
    }
    Column(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = ReelTheme.spacing.s, vertical = ReelTheme.spacing.xs),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(28.dp)
                .pointerInput(duration) {
                    if (duration <= 0L) return@pointerInput
                    detectHorizontalDragGestures(
                        onDragStart = { offset ->
                            val target = (offset.x / size.width.toFloat() * duration.toFloat()).toLong()
                            onScrub(target.coerceIn(0L, duration))
                        },
                        onHorizontalDrag = { change, _ ->
                            change.consume()
                            val target = (change.position.x / size.width.toFloat() * duration.toFloat()).toLong()
                            onScrub(target.coerceIn(0L, duration))
                        },
                        onDragEnd = onScrubFinished,
                        onDragCancel = onScrubFinished,
                    )
                }
                .pointerInput(duration) {
                    if (duration <= 0L) return@pointerInput
                    detectTapGestures { offset ->
                        val target = (offset.x / size.width.toFloat() * duration.toFloat()).toLong()
                        onScrub(target.coerceIn(0L, duration))
                        onScrubFinished()
                    }
                },
            contentAlignment = Alignment.Center,
        ) {
            Canvas(modifier = Modifier.fillMaxWidth().height(2.dp)) {
                val centerY = size.height / 2f
                val activeWidth = size.width * fraction
                drawLine(
                    color = Color.White.copy(alpha = 0.35f),
                    start = Offset(0f, centerY),
                    end = Offset(size.width, centerY),
                    strokeWidth = size.height,
                    cap = StrokeCap.Round,
                )
                drawLine(
                    color = Color.White,
                    start = Offset(0f, centerY),
                    end = Offset(activeWidth, centerY),
                    strokeWidth = size.height,
                    cap = StrokeCap.Round,
                )
                drawCircle(
                    color = Color.White,
                    radius = 4.dp.toPx(),
                    center = Offset(activeWidth.coerceIn(0f, size.width), centerY),
                )
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = formatTimestamp(positionMs),
                style = ReelTheme.type.mono,
                color = Color.White,
            )
            Text(
                text = formatTimestamp(duration),
                style = ReelTheme.type.mono,
                color = Color.White,
            )
        }
    }
}

@Composable
private fun SkipDurationDialog(
    selectedSeconds: Int,
    onSelect: (Int) -> Unit,
    onDismiss: () -> Unit,
) {
    val colors = ReelTheme.colors
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(R.string.media_skip_duration_title),
                style = ReelTheme.type.titleM,
                color = colors.textPrimary,
            )
        },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                SKIP_DURATION_OPTIONS.forEach { seconds ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelect(seconds) }
                            .padding(vertical = ReelTheme.spacing.s),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = stringResource(R.string.media_skip_seconds, seconds),
                            style = ReelTheme.type.bodyM,
                            color = if (seconds == selectedSeconds) colors.gold else colors.textPrimary,
                            modifier = Modifier.weight(1f),
                        )
                        if (seconds == selectedSeconds) {
                            Icon(
                                imageVector = Icons.Filled.Check,
                                contentDescription = null,
                                tint = colors.gold,
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    text = stringResource(R.string.media_skip_cancel),
                    color = colors.gold,
                )
            }
        },
        containerColor = colors.surface,
    )
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

@androidx.annotation.OptIn(UnstableApi::class)
@Composable
private fun PlayerMenu(
    expanded: Boolean,
    player: ExoPlayer,
    orientation: Int,
    skipSeconds: Int,
    onDismiss: () -> Unit,
    onOrientation: (Int) -> Unit,
    onSkipDuration: () -> Unit,
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
                player.pauseAtEndOfMediaItems = true
                onDismiss()
            },
        )
        RepeatOption(
            label = stringResource(R.string.media_menu_repeat_one),
            selected = player.repeatMode == Player.REPEAT_MODE_ONE,
            onClick = {
                player.repeatMode = Player.REPEAT_MODE_ONE
                player.pauseAtEndOfMediaItems = false
                onDismiss()
            },
        )
        RepeatOption(
            label = stringResource(R.string.media_menu_repeat_all),
            selected = player.repeatMode == Player.REPEAT_MODE_ALL,
            onClick = {
                player.repeatMode = Player.REPEAT_MODE_ALL
                player.pauseAtEndOfMediaItems = false
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
        DropdownMenuItem(
            text = {
                Text(
                    text = stringResource(
                        R.string.media_menu_skip_duration,
                        skipSeconds,
                    ),
                    style = ReelTheme.type.bodyM,
                    color = colors.textPrimary,
                )
            },
            onClick = onSkipDuration,
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

private enum class DragSide { Brightness, Volume }

private sealed interface GestureFeedback {
    data class Brightness(val percent: Int) : GestureFeedback
    data class Volume(val percent: Int) : GestureFeedback
}

private val SKIP_DURATION_OPTIONS = listOf(5, 10, 15, 20, 25, 30)

private fun formatTimestamp(positionMs: Long): String {
    val totalSeconds = (positionMs.coerceAtLeast(0L) / 1_000L).toInt()
    val hours = totalSeconds / 3_600
    val minutes = (totalSeconds % 3_600) / 60
    val seconds = totalSeconds % 60
    val paddedMinutes = minutes.toString().padStart(2, '0')
    val paddedSeconds = seconds.toString().padStart(2, '0')
    return if (hours > 0) {
        "$hours:$paddedMinutes:$paddedSeconds"
    } else {
        "$paddedMinutes:$paddedSeconds"
    }
}

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
