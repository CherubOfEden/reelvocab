package app.reelvocab.feature.media

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.util.Size
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Photo
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.reelvocab.core.designsystem.component.CardSurface
import app.reelvocab.core.designsystem.component.EmptyState
import app.reelvocab.core.designsystem.component.ReelButton
import app.reelvocab.core.designsystem.component.ReelButtonStyle
import app.reelvocab.core.designsystem.component.ReelChip
import app.reelvocab.core.designsystem.theme.ReelTheme
import app.reelvocab.core.domain.model.MediaItem
import app.reelvocab.core.domain.model.MediaKind
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable

@Serializable
data object MediaRoute

@Serializable
data class PlayerTrack(
    val uri: String,
    val title: String,
    val kind: String,
    val mimeType: String? = null,
)

@Serializable
data class PlayerRoute(
    val uri: String,
    val title: String,
    val kind: String = "video",
    val mimeType: String? = null,
    val queue: List<PlayerTrack> = emptyList(),
    val index: Int = 0,
)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MediaScreen(
    onPlay: (PlayerRoute) -> Unit,
    modifier: Modifier = Modifier,
    embedded: Boolean = false,
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
            val mimeType = context.contentResolver.getType(uri)
            onPlay(
                PlayerRoute(
                    uri = uri.toString(),
                    title = uri.lastPathSegment?.substringAfterLast('/') ?: "Media",
                    kind = kindForMime(mimeType),
                    mimeType = mimeType,
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

    val visibleItems = remember(state.items, state.category) {
        when (state.category) {
            MediaCategory.All -> state.items
            MediaCategory.Videos -> state.items.filter { it.kind == MediaKind.Video }
            MediaCategory.Images -> state.items.filter { it.kind == MediaKind.Image }
            MediaCategory.Audio -> state.items.filter { it.kind == MediaKind.Audio }
        }
    }
    val queue = remember(visibleItems) { visibleItems.map(MediaItem::toTrack) }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = ReelTheme.spacing.screenH,
            end = ReelTheme.spacing.screenH,
            top = if (embedded) ReelTheme.spacing.s else ReelTheme.spacing.xl,
            bottom = ReelTheme.spacing.screenBottom,
        ),
        verticalArrangement = Arrangement.spacedBy(ReelTheme.spacing.s),
    ) {
        if (!embedded) {
            item {
                Text(
                    text = stringResource(R.string.media_title),
                    style = ReelTheme.type.titleL,
                    color = colors.textPrimary,
                )
                Spacer(Modifier.height(ReelTheme.spacing.xs))
                Text(
                    text = stringResource(R.string.media_subtitle),
                    style = ReelTheme.type.bodyM,
                    color = colors.textSecondary,
                )
            }
        }

        stickyHeader(key = "media-categories") {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(colors.base)
                    .padding(vertical = ReelTheme.spacing.xs)
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(ReelTheme.spacing.s),
            ) {
                MediaCategory.entries.forEach { category ->
                    ReelChip(
                        text = stringResource(category.labelRes),
                        selected = state.category == category,
                        onClick = { viewModel.setCategory(category) },
                    )
                }
            }
        }

        when {
            !granted -> item {
                MediaPermissionCard(
                    onAllow = { permissionLauncher.launch(required) },
                    onOpenFile = { launchOpenDocument(openDocument) },
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

            visibleItems.isEmpty() && state.category == MediaCategory.All -> item {
                EmptyState(
                    title = stringResource(R.string.media_empty_title),
                    body = stringResource(R.string.media_empty_body),
                    ctaLabel = stringResource(R.string.media_open_file),
                    onCta = { launchOpenDocument(openDocument) },
                )
            }

            visibleItems.isEmpty() -> item {
                Text(
                    text = stringResource(R.string.media_empty_category),
                    style = ReelTheme.type.bodyM,
                    color = colors.textSecondary,
                )
            }

            else -> items(visibleItems, key = { "${it.kind}-${it.id}" }) { item ->
                MediaRow(
                    item = item,
                    queue = queue,
                    onPlay = onPlay,
                )
            }
        }

        if (granted) {
            item {
                Spacer(Modifier.height(ReelTheme.spacing.s))
                ReelButton(
                    text = stringResource(R.string.media_open_file),
                    onClick = { launchOpenDocument(openDocument) },
                    style = ReelButtonStyle.Ghost,
                )
            }
        }
    }
}

private fun launchOpenDocument(
    launcher: androidx.activity.result.ActivityResultLauncher<Array<String>>,
) {
    launcher.launch(
        arrayOf(
            "video/*",
            "audio/*",
            "image/*",
        ),
    )
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
private fun MediaRow(
    item: MediaItem,
    queue: List<PlayerTrack>,
    onPlay: (PlayerRoute) -> Unit,
) {
    val colors = ReelTheme.colors
    val mimeType = item.mimeType
    val playableKind = item.kind.toPlayerKind()
    val itemQueue = queue.filter { it.kind == playableKind }
    val itemIndex = itemQueue.indexOfFirst { it.uri == item.uri }.coerceAtLeast(0)
    val windowStart = (itemIndex - QUEUE_WINDOW_BEFORE).coerceAtLeast(0)
    val windowEnd = (itemIndex + QUEUE_WINDOW_AFTER + 1).coerceAtMost(itemQueue.size)
    val itemWindow = itemQueue.subList(windowStart, windowEnd)
    val windowIndex = (itemIndex - windowStart).coerceAtLeast(0)
    CardSurface(
        onClick = {
            onPlay(
                PlayerRoute(
                    uri = item.uri,
                    title = item.title,
                    kind = playableKind,
                    mimeType = mimeType,
                    queue = itemWindow,
                    index = windowIndex,
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
            MediaThumbnail(item = item)
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.title,
                    style = ReelTheme.type.bodyL,
                    color = colors.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(ReelTheme.spacing.xxs))
                Text(
                    text = mediaSubtitle(item),
                    style = ReelTheme.type.label,
                    color = colors.textTertiary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Icon(
                imageVector = item.kind.icon(),
                contentDescription = null,
                tint = colors.gold,
            )
        }
    }
}

@Composable
private fun MediaThumbnail(item: MediaItem) {
    val context = LocalContext.current
    val colors = ReelTheme.colors
    var bitmap by remember(item.uri, item.kind) { mutableStateOf<Bitmap?>(null) }
    LaunchedEffect(item.uri, item.kind) {
        bitmap = withContext(Dispatchers.IO) { loadThumbnail(context, item) }
    }
    Box(
        modifier = Modifier
            .size(width = 96.dp, height = 64.dp)
            .clip(RoundedCornerShape(ReelTheme.radius.input))
            .background(colors.elevated),
        contentAlignment = Alignment.Center,
    ) {
        val image = bitmap
        if (image != null) {
            Image(
                bitmap = image.asImageBitmap(),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(ReelTheme.radius.input)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = item.kind.icon(),
                    contentDescription = null,
                    tint = colors.textTertiary,
                )
            }
        }
    }
}

private fun loadThumbnail(context: Context, item: MediaItem): Bitmap? = runCatching {
    when (item.kind) {
        MediaKind.Video, MediaKind.Image -> context.contentResolver.loadThumbnail(
            Uri.parse(item.uri),
            Size(THUMBNAIL_WIDTH, THUMBNAIL_HEIGHT),
            null,
        )
        MediaKind.Audio -> loadAlbumArt(context, item.uri)
    }
}.getOrNull()

private fun loadAlbumArt(context: Context, uri: String): Bitmap? {
    val retriever = MediaMetadataRetriever()
    return try {
        retriever.setDataSource(context, Uri.parse(uri))
        retriever.embeddedPicture?.let { bytes ->
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        }
    } catch (error: Exception) {
        null
    } finally {
        runCatching { retriever.release() }
    }
}

private fun MediaItem.toTrack(): PlayerTrack = PlayerTrack(
    uri = uri,
    title = title,
    kind = kind.toPlayerKind(),
    mimeType = mimeType,
)

private fun MediaKind.toPlayerKind(): String = when (this) {
    MediaKind.Video -> "video"
    MediaKind.Image -> "image"
    MediaKind.Audio -> "audio"
}

private fun kindForMime(mimeType: String?): String = when {
    mimeType == null -> "image"
    mimeType.startsWith("video/") -> "video"
    mimeType.startsWith("image/") -> "image"
    mimeType.startsWith("audio/") -> "audio"
    else -> "image"
}

private fun MediaKind.icon() = when (this) {
    MediaKind.Video -> Icons.Filled.Movie
    MediaKind.Image -> Icons.Filled.Photo
    MediaKind.Audio -> Icons.Filled.MusicNote
}

@Composable
private fun mediaSubtitle(item: MediaItem): String {
    val kind = stringResource(
        when (item.kind) {
            MediaKind.Video -> R.string.media_kind_video
            MediaKind.Image -> R.string.media_kind_image
            MediaKind.Audio -> R.string.media_kind_audio
        },
    )
    return if (item.kind == MediaKind.Video || item.kind == MediaKind.Audio) {
        "$kind · ${formatMediaDuration(item.durationMs)}"
    } else {
        kind
    }
}

private fun requiredMediaPermissions(): Array<String> =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        arrayOf(
            Manifest.permission.READ_MEDIA_VIDEO,
            Manifest.permission.READ_MEDIA_IMAGES,
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

private const val QUEUE_WINDOW_BEFORE = 20
private const val QUEUE_WINDOW_AFTER = 30
private const val THUMBNAIL_WIDTH = 320
private const val THUMBNAIL_HEIGHT = 200
