package app.reelvocab.feature.media

import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import app.reelvocab.core.designsystem.component.ReelButton
import app.reelvocab.core.designsystem.theme.ReelTheme
import app.reelvocab.feature.capture.InAppCapture
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun PlayerScreen(
    route: PlayerRoute,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val isVideo = route.kind != "audio"

    val player = remember(route.uri) {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(route.uri))
            prepare()
            playWhenReady = true
        }
    }
    DisposableEffect(player) {
        onDispose { player.release() }
    }

    var frame by remember { mutableStateOf<Bitmap?>(null) }
    var frameAtMs by remember { mutableLongStateOf(0L) }
    var capturing by remember { mutableStateOf(false) }

    Box(modifier = modifier.fillMaxSize().background(Color.Black)) {
        AndroidView(
            factory = { viewContext ->
                PlayerView(viewContext).apply {
                    this.player = player
                    useController = true
                }
            },
            modifier = Modifier.fillMaxSize(),
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.Black.copy(alpha = 0.45f))
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
                text = route.title,
                style = ReelTheme.type.bodyL,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
        }

        if (isVideo) {
            ReelButton(
                text = stringResource(R.string.media_capture),
                onClick = {
                    if (capturing) return@ReelButton
                    capturing = true
                    player.pause()
                    val atMs = player.currentPosition.coerceAtLeast(0)
                    scope.launch {
                        val bitmap = withContext(Dispatchers.IO) {
                            extractFrame(context, route.uri, atMs)
                        }
                        if (bitmap != null) {
                            frameAtMs = atMs
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
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(
                        start = ReelTheme.spacing.screenH,
                        end = ReelTheme.spacing.screenH,
                        bottom = ReelTheme.spacing.xxl,
                    ),
                loading = capturing,
            )
        }
    }

    val captured = frame
    if (captured != null) {
        InAppCapture(
            bitmap = captured,
            sourceTitle = route.title,
            timestampMs = frameAtMs,
            onDismiss = { frame = null },
            onSaved = {
                frame = null
                Toast.makeText(context, R.string.media_saved, Toast.LENGTH_SHORT).show()
            },
        )
    }
}

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
