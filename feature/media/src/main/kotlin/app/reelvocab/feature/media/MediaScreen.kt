package app.reelvocab.feature.media

import app.reelvocab.core.designsystem.icon.ReelIcons

import android.app.Activity
import android.app.RecoverableSecurityException
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Size
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
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
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.reelvocab.core.designsystem.component.CardSurface
import app.reelvocab.core.designsystem.component.EmptyState
import app.reelvocab.core.designsystem.component.ReelButton
import app.reelvocab.core.designsystem.component.ReelButtonStyle
import app.reelvocab.core.designsystem.component.ReelChip
import app.reelvocab.core.designsystem.component.ReelRule
import app.reelvocab.core.designsystem.component.ReelScreenHeader
import app.reelvocab.core.designsystem.theme.ReelTheme
import app.reelvocab.core.domain.model.MediaItem
import app.reelvocab.core.domain.model.MediaKind
import java.text.DateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
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
    onEdit: (uri: String, title: String, kind: String) -> Unit,
    modifier: Modifier = Modifier,
    embedded: Boolean = false,
    viewModel: MediaViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val state by viewModel.state.collectAsStateWithLifecycle()
    val colors = ReelTheme.colors

    val required = remember { requiredMediaPermissions() }
    val requiredAudio = remember { requiredAudioPermissions() }
    var access by remember { mutableStateOf(currentMediaAccess(context)) }
    var autoRequested by rememberSaveable { mutableStateOf(false) }
    var detailsFor by remember { mutableStateOf<MediaItem?>(null) }
    var deleteFor by remember { mutableStateOf<MediaItem?>(null) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) {
        access = currentMediaAccess(context)
    }
    val audioPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) {
        access = currentMediaAccess(context)
    }
    val deleteLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult(),
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            viewModel.scan(access, force = true)
        }
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

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                access = currentMediaAccess(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(Unit) {
        if (!access.hasAny && !autoRequested) {
            autoRequested = true
            permissionLauncher.launch(required)
        }
    }
    LaunchedEffect(access) {
        if (access.hasAny) viewModel.scan(access)
    }

    fun requestDelete(item: MediaItem) {
        val uri = Uri.parse(item.uri)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            runCatching {
                MediaStore.createDeleteRequest(context.contentResolver, listOf(uri))
            }.onSuccess { request ->
                deleteLauncher.launch(
                    IntentSenderRequest.Builder(request.intentSender).build(),
                )
            }.onFailure {
                Toast.makeText(context, R.string.media_delete_failed, Toast.LENGTH_SHORT).show()
            }
            return
        }

        scope.launch {
            val removed = withContext(Dispatchers.IO) {
                runCatching { context.contentResolver.delete(uri, null, null) }
            }
            removed.onSuccess { count ->
                if (count > 0) {
                    viewModel.scan(access, force = true)
                } else {
                    Toast.makeText(
                        context,
                        R.string.media_delete_failed,
                        Toast.LENGTH_SHORT,
                    ).show()
                }
            }.onFailure { error ->
                val sender = (error as? RecoverableSecurityException)
                    ?.userAction
                    ?.actionIntent
                    ?.intentSender
                if (sender != null) {
                    deleteLauncher.launch(IntentSenderRequest.Builder(sender).build())
                } else {
                    Toast.makeText(
                        context,
                        R.string.media_delete_failed,
                        Toast.LENGTH_SHORT,
                    ).show()
                }
            }
        }
    }

    fun share(item: MediaItem) {
        val uri = Uri.parse(item.uri)
        val mimeType = item.mimeType
            ?: context.contentResolver.getType(uri)
            ?: "*/*"
        val send = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        runCatching {
            context.startActivity(
                Intent.createChooser(send, context.getString(R.string.media_share)),
            )
        }.onFailure {
            Toast.makeText(context, R.string.media_share_failed, Toast.LENGTH_SHORT).show()
        }
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
                ReelScreenHeader(
                    title = stringResource(R.string.media_title),
                    subtitle = stringResource(R.string.media_subtitle),
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

        if (access.visualPartial) {
            item {
                Text(
                    text = stringResource(R.string.media_partial_access),
                    style = ReelTheme.type.caption,
                    color = colors.textTertiary,
                )
            }
        }

        when {
            !access.hasAny -> item {
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
                        color = colors.error,
                    )
                    ReelButton(
                        text = stringResource(R.string.media_retry),
                        onClick = { viewModel.scan(access, force = true) },
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
                    onDetails = { detailsFor = item },
                    onEdit = {
                        onEdit(item.uri, item.title, item.kind.toPlayerKind())
                    },
                    onShare = { share(item) },
                    onDelete = { deleteFor = item },
                )
            }
        }

        if (access.hasAny && !access.hasVisual) {
            item {
                ReelButton(
                    text = stringResource(R.string.media_permission_visual_cta),
                    onClick = { permissionLauncher.launch(required) },
                    style = ReelButtonStyle.Ghost,
                )
            }
        }

        if (access.hasVisual && !access.hasAudio) {
            item {
                ReelButton(
                    text = stringResource(R.string.media_permission_audio_cta),
                    onClick = { audioPermissionLauncher.launch(requiredAudio) },
                    style = ReelButtonStyle.Ghost,
                )
            }
        }

        if (access.hasAny) {
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

    detailsFor?.let { item ->
        MediaDetailsDialog(item = item, onDismiss = { detailsFor = null })
    }

    deleteFor?.let { item ->
        AlertDialog(
            onDismissRequest = { deleteFor = null },
            title = {
                Text(
                    text = stringResource(R.string.media_delete_title),
                    style = ReelTheme.type.titleM,
                    color = colors.textPrimary,
                )
            },
            text = {
                Text(
                    text = stringResource(R.string.media_delete_body, item.title),
                    style = ReelTheme.type.bodyM,
                    color = colors.textSecondary,
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        deleteFor = null
                        requestDelete(item)
                    },
                ) {
                    Text(
                        text = stringResource(R.string.media_delete_confirm),
                        color = colors.error,
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { deleteFor = null }) {
                    Text(
                        text = stringResource(R.string.media_skip_cancel),
                        color = colors.textSecondary,
                    )
                }
            },
            containerColor = colors.elevated,
        )
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
    onDetails: () -> Unit,
    onEdit: () -> Unit,
    onShare: () -> Unit,
    onDelete: () -> Unit,
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
    Column(
        modifier = Modifier.clickable {
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
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = ReelTheme.spacing.m),
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
                    style = ReelTheme.type.caption,
                    color = colors.textTertiary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Box {
                var menuOpen by remember { mutableStateOf(false) }
                IconButton(onClick = { menuOpen = true }) {
                    Icon(
                        imageVector = ReelIcons.EllipsisVertical,
                        contentDescription = stringResource(R.string.media_more),
                        tint = colors.textSecondary,
                    )
                }
                DropdownMenu(
                    expanded = menuOpen,
                    onDismissRequest = { menuOpen = false },
                ) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.media_menu_details)) },
                        leadingIcon = { Icon(ReelIcons.Info, contentDescription = null) },
                        onClick = {
                            menuOpen = false
                            onDetails()
                        },
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.media_menu_edit)) },
                        leadingIcon = { Icon(ReelIcons.Pencil, contentDescription = null) },
                        onClick = {
                            menuOpen = false
                            onEdit()
                        },
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.media_share)) },
                        leadingIcon = { Icon(ReelIcons.Share2, contentDescription = null) },
                        onClick = {
                            menuOpen = false
                            onShare()
                        },
                    )
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = stringResource(R.string.media_menu_delete),
                                color = colors.error,
                            )
                        },
                        leadingIcon = {
                            Icon(
                                ReelIcons.Trash2,
                                contentDescription = null,
                                tint = colors.error,
                            )
                        },
                        onClick = {
                            menuOpen = false
                            onDelete()
                        },
                    )
                }
            }
        }
        ReelRule()
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
    MediaKind.Video -> ReelIcons.Clapperboard
    MediaKind.Image -> ReelIcons.Image
    MediaKind.Audio -> ReelIcons.Music
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

internal fun formatMediaDuration(durationMs: Long): String {
    val totalSeconds = (durationMs / 1000).coerceAtLeast(0)
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return if (hours > 0) {
        String.format(Locale.US, "%d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format(Locale.US, "%d:%02d", minutes, seconds)
    }
}

private data class MediaDetails(
    val displayName: String?,
    val sizeBytes: Long?,
    val modifiedAtMs: Long?,
    val mimeType: String?,
)

@Composable
private fun MediaDetailsDialog(
    item: MediaItem,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val colors = ReelTheme.colors
    var details by remember(item.uri) { mutableStateOf<MediaDetails?>(null) }
    LaunchedEffect(item.uri) {
        details = withContext(Dispatchers.IO) { loadMediaDetails(context, item) }
    }
    val loaded = details
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(R.string.media_details_title),
                style = ReelTheme.type.titleM,
                color = colors.textPrimary,
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(ReelTheme.spacing.s)) {
                DetailLine(stringResource(R.string.media_detail_title), item.title)
                DetailLine(
                    stringResource(R.string.media_detail_kind),
                    stringResource(
                        when (item.kind) {
                            MediaKind.Video -> R.string.media_kind_video
                            MediaKind.Image -> R.string.media_kind_image
                            MediaKind.Audio -> R.string.media_kind_audio
                        },
                    ),
                )
                if (item.durationMs > 0L) {
                    DetailLine(
                        stringResource(R.string.media_detail_duration),
                        formatMediaDuration(item.durationMs),
                    )
                }
                val mime = loaded?.mimeType ?: item.mimeType
                if (!mime.isNullOrBlank()) {
                    DetailLine(stringResource(R.string.media_detail_mime), mime)
                }
                val name = loaded?.displayName
                if (!name.isNullOrBlank()) {
                    DetailLine(stringResource(R.string.media_detail_name), name)
                }
                val size = loaded?.sizeBytes
                if (size != null) {
                    DetailLine(stringResource(R.string.media_detail_size), formatFileSize(size))
                }
                val modified = loaded?.modifiedAtMs
                if (modified != null) {
                    DetailLine(
                        stringResource(R.string.media_detail_modified),
                        DateFormat.getDateTimeInstance().format(Date(modified)),
                    )
                }
                DetailLine(stringResource(R.string.media_detail_path), item.uri)
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(R.string.media_close), color = colors.accent)
            }
        },
        containerColor = colors.elevated,
    )
}

@Composable
private fun DetailLine(label: String, value: String) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            text = label,
            style = ReelTheme.type.label,
            color = ReelTheme.colors.textTertiary,
        )
        Text(
            text = value,
            style = ReelTheme.type.bodyM,
            color = ReelTheme.colors.textPrimary,
        )
    }
}

private fun loadMediaDetails(context: Context, item: MediaItem): MediaDetails {
    val uri = Uri.parse(item.uri)
    return runCatching {
        val projection = arrayOf(
            MediaStore.MediaColumns.DISPLAY_NAME,
            MediaStore.MediaColumns.SIZE,
            MediaStore.MediaColumns.DATE_MODIFIED,
        )
        context.contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
            if (!cursor.moveToFirst()) return@use null
            val nameColumn = cursor.getColumnIndex(MediaStore.MediaColumns.DISPLAY_NAME)
            val sizeColumn = cursor.getColumnIndex(MediaStore.MediaColumns.SIZE)
            val modifiedColumn = cursor.getColumnIndex(MediaStore.MediaColumns.DATE_MODIFIED)
            MediaDetails(
                displayName = nameColumn.takeIf { it >= 0 }?.let(cursor::getString),
                sizeBytes = sizeColumn.takeIf { it >= 0 }?.let(cursor::getLong),
                modifiedAtMs = modifiedColumn.takeIf { it >= 0 }
                    ?.let(cursor::getLong)
                    ?.takeIf { it > 0L }
                    ?.times(1_000L),
                mimeType = item.mimeType ?: context.contentResolver.getType(uri),
            )
        }
    }.getOrNull() ?: MediaDetails(
        displayName = null,
        sizeBytes = null,
        modifiedAtMs = null,
        mimeType = item.mimeType,
    )
}

private fun formatFileSize(bytes: Long): String {
    if (bytes < 1_024L) return "$bytes B"
    val units = arrayOf("KB", "MB", "GB")
    var value = bytes.toDouble() / 1_024.0
    var unit = 0
    while (value >= 1_024.0 && unit < units.lastIndex) {
        value /= 1_024.0
        unit++
    }
    return String.format(Locale.getDefault(), "%.1f %s", value, units[unit])
}

private const val QUEUE_WINDOW_BEFORE = 20
private const val QUEUE_WINDOW_AFTER = 30
private const val THUMBNAIL_WIDTH = 320
private const val THUMBNAIL_HEIGHT = 200
