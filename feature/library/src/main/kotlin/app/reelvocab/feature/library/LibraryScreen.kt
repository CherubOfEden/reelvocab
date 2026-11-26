package app.reelvocab.feature.library

import app.reelvocab.core.designsystem.icon.ReelIcons

import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.reelvocab.core.designsystem.component.EmptyState
import app.reelvocab.core.designsystem.component.ReelChip
import app.reelvocab.core.designsystem.component.ReelScreenHeader
import app.reelvocab.core.designsystem.component.ReelRule
import app.reelvocab.core.designsystem.illustration.ReelIllustration
import app.reelvocab.core.designsystem.theme.ReelTheme
import app.reelvocab.core.domain.model.VaultItem
import app.reelvocab.core.domain.model.VaultKind
import app.reelvocab.feature.media.MediaScreen
import app.reelvocab.feature.media.PlayerRoute
import coil.ImageLoader
import coil.compose.SubcomposeAsyncImage
import coil.decode.VideoFrameDecoder
import coil.request.ImageRequest
import coil.request.videoFrameMillis
import java.io.File
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable

@Serializable
data object LibraryRoute

private enum class LibrarySection(val labelRes: Int) {
    InternalMedia(R.string.library_segment_internal_media),
    Vault(R.string.library_segment_vault),
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun LibraryScreen(
    onOpenDeck: () -> Unit,
    onPlay: (uri: String, title: String, kind: String) -> Unit,
    onEdit: (uri: String, title: String, kind: String) -> Unit,
    onPlayMedia: (PlayerRoute) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: LibraryViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val colors = ReelTheme.colors
    var section by rememberSaveable { mutableStateOf(LibrarySection.Vault) }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var detailsFor by remember { mutableStateOf<VaultItem?>(null) }
    var deleteFor by remember { mutableStateOf<VaultItem?>(null) }
    var exportFor by remember { mutableStateOf<VaultItem?>(null) }
    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("*/*"),
    ) { target ->
        val item = exportFor
        exportFor = null
        if (target != null && item != null) {
            scope.launch {
                val exported = withContext(Dispatchers.IO) {
                    exportVaultItem(context, item, target)
                }
                Toast.makeText(
                    context,
                    if (exported) R.string.library_export_done else R.string.library_export_failed,
                    Toast.LENGTH_SHORT,
                ).show()
            }
        }
    }
    val imageLoader = remember(context) {
        ImageLoader.Builder(context)
            .components { add(VideoFrameDecoder.Factory()) }
            .crossfade(true)
            .build()
    }
    DisposableEffect(imageLoader) {
        onDispose { imageLoader.shutdown() }
    }
    val visible = if (state.category == null) {
        state.items
    } else {
        state.items.filter { it.kind == state.category }
    }

    Column(modifier = modifier.fillMaxSize()) {
        LibrarySectionToggle(
            selected = section,
            onSelect = { section = it },
            modifier = Modifier
                .fillMaxWidth()
                .background(colors.base)
                .padding(horizontal = ReelTheme.spacing.screenH)
                .padding(top = ReelTheme.spacing.m, bottom = ReelTheme.spacing.s),
        )

        if (section == LibrarySection.InternalMedia) {
            MediaScreen(
                onPlay = onPlayMedia,
                modifier = Modifier.weight(1f),
                embedded = true,
            )
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(
                    start = ReelTheme.spacing.screenH,
                    end = ReelTheme.spacing.screenH,
                    top = ReelTheme.spacing.xl,
                    bottom = ReelTheme.spacing.screenBottom,
                ),
                verticalArrangement = Arrangement.spacedBy(ReelTheme.spacing.m),
            ) {
                item {
                    ReelScreenHeader(
                        title = stringResource(R.string.library_title),
                        subtitle = stringResource(R.string.library_subtitle),
                    )
                }

                stickyHeader(key = "library-categories") {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(colors.base)
                            .padding(vertical = ReelTheme.spacing.xs)
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(ReelTheme.spacing.s),
                    ) {
                        ReelChip(
                            text = stringResource(R.string.library_all),
                            selected = state.category == null,
                            onClick = { viewModel.setCategory(null) },
                        )
                        ReelChip(
                            text = stringResource(R.string.library_images),
                            selected = state.category == VaultKind.Image,
                            onClick = { viewModel.setCategory(VaultKind.Image) },
                        )
                        ReelChip(
                            text = stringResource(R.string.library_videos),
                            selected = state.category == VaultKind.Video,
                            onClick = { viewModel.setCategory(VaultKind.Video) },
                        )
                        ReelChip(
                            text = stringResource(R.string.library_audio),
                            selected = state.category == VaultKind.Audio,
                            onClick = { viewModel.setCategory(VaultKind.Audio) },
                        )
                    }
                }

                if (state.loading) {
                    item {
                        Text(
                            text = stringResource(R.string.library_loading),
                            style = ReelTheme.type.bodyM,
                            color = colors.textSecondary,
                        )
                    }
                } else if (visible.isEmpty()) {
                    item {
                        EmptyState(
                            title = stringResource(R.string.library_empty_title),
                            body = stringResource(R.string.library_empty_body),
                            ctaLabel = stringResource(R.string.library_cta_deck),
                            onCta = onOpenDeck,
                            illustration = ReelIllustration.Ticket,
                        )
                    }
                } else {
                    items(visible, key = { it.id }) { item ->
                        VaultRow(
                            item = item,
                            imageLoader = imageLoader,
                            onPlay = {
                                onPlay(item.uri, item.title, item.kind.playerKind())
                            },
                            onDetails = { detailsFor = item },
                            onEdit = {
                                onEdit(item.uri, item.title, item.kind.playerKind())
                            },
                            onExport = {
                                exportFor = item
                                exportLauncher.launch(exportFileName(item))
                            },
                            onDelete = { deleteFor = item },
                        )
                    }
                }
            }

        }
    }

    detailsFor?.let { item ->
        VaultDetailsDialog(item = item, onDismiss = { detailsFor = null })
    }
    deleteFor?.let { item ->
        AlertDialog(
            onDismissRequest = { deleteFor = null },
            title = {
                Text(
                    text = stringResource(R.string.library_delete_title),
                    style = ReelTheme.type.titleM,
                    color = colors.textPrimary,
                )
            },
            text = {
                Text(
                    text = stringResource(R.string.library_delete_body, item.title),
                    style = ReelTheme.type.bodyM,
                    color = colors.textSecondary,
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.delete(item)
                        deleteFor = null
                    },
                ) {
                    Text(
                        text = stringResource(R.string.library_delete_confirm),
                        color = colors.error,
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { deleteFor = null }) {
                    Text(
                        text = stringResource(R.string.library_cancel),
                        color = colors.textSecondary,
                    )
                }
            },
            containerColor = colors.elevated,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LibrarySectionToggle(
    selected: LibrarySection,
    onSelect: (LibrarySection) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = ReelTheme.colors
    SingleChoiceSegmentedButtonRow(modifier = modifier) {
        LibrarySection.entries.forEachIndexed { index, section ->
            SegmentedButton(
                selected = selected == section,
                onClick = { onSelect(section) },
                shape = SegmentedButtonDefaults.itemShape(
                    index = index,
                    count = LibrarySection.entries.size,
                ),
                colors = SegmentedButtonDefaults.colors(
                    activeContainerColor = colors.accent.copy(alpha = 0.16f),
                    activeContentColor = colors.accent,
                    activeBorderColor = colors.accent.copy(alpha = 0.5f),
                    inactiveContainerColor = colors.elevated,
                    inactiveContentColor = colors.textSecondary,
                    inactiveBorderColor = colors.strokeSubtle,
                ),
                icon = {},
                label = {
                    Text(
                        text = stringResource(section.labelRes),
                        style = ReelTheme.type.bodyM,
                    )
                },
            )
        }
    }
}

@Composable
private fun VaultRow(
    item: VaultItem,
    imageLoader: ImageLoader,
    onPlay: () -> Unit,
    onDetails: () -> Unit,
    onEdit: () -> Unit,
    onExport: () -> Unit,
    onDelete: () -> Unit,
) {
    val colors = ReelTheme.colors
    Column(modifier = Modifier.clickable(onClick = onPlay)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = ReelTheme.spacing.m),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(ReelTheme.spacing.m),
        ) {
            VaultThumbnail(
                item = item,
                imageLoader = imageLoader,
                modifier = Modifier.size(72.dp),
            )
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
                    text = "${item.kind.label()} · ${item.createdLabel()}",
                    style = ReelTheme.type.label,
                    color = colors.textTertiary,
                )
            }
            Box {
                var menuOpen by remember { mutableStateOf(false) }
                IconButton(onClick = { menuOpen = true }) {
                    Icon(
                        imageVector = ReelIcons.EllipsisVertical,
                        contentDescription = stringResource(R.string.library_more),
                        tint = colors.textSecondary,
                    )
                }
                DropdownMenu(
                    expanded = menuOpen,
                    onDismissRequest = { menuOpen = false },
                ) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.library_details)) },
                        leadingIcon = { Icon(ReelIcons.Info, contentDescription = null) },
                        onClick = {
                            menuOpen = false
                            onDetails()
                        },
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.library_edit)) },
                        leadingIcon = { Icon(ReelIcons.Pencil, contentDescription = null) },
                        onClick = {
                            menuOpen = false
                            onEdit()
                        },
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.library_export)) },
                        leadingIcon = { Icon(ReelIcons.Share2, contentDescription = null) },
                        onClick = {
                            menuOpen = false
                            onExport()
                        },
                    )
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = stringResource(R.string.library_delete),
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
private fun VaultThumbnail(
    item: VaultItem,
    imageLoader: ImageLoader,
    modifier: Modifier = Modifier,
) {
    val colors = ReelTheme.colors
    val context = LocalContext.current
    val shape = RoundedCornerShape(ReelTheme.radius.input)
    if (item.kind == VaultKind.Audio) {
        Box(
            modifier = modifier
                .clip(shape)
                .background(colors.elevated),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = ReelIcons.AudioLines,
                contentDescription = null,
                tint = colors.accent,
                modifier = Modifier.size(32.dp),
            )
        }
        return
    }
    val request = remember(item.uri, item.kind, context) {
        ImageRequest.Builder(context)
            .data(Uri.parse(item.uri))
            .apply {
                if (item.kind == VaultKind.Video) {
                    videoFrameMillis(1_000L)
                }
            }
            .size(512, 512)
            .crossfade(true)
            .build()
    }
    SubcomposeAsyncImage(
        model = request,
        imageLoader = imageLoader,
        contentDescription = item.title,
        contentScale = ContentScale.Crop,
        modifier = modifier
            .clip(shape)
            .background(colors.elevated),
        loading = { VaultThumbnailPlaceholder(kind = item.kind) },
        error = { VaultThumbnailPlaceholder(kind = item.kind) },
    )
}

@Composable
private fun VaultThumbnailPlaceholder(kind: VaultKind) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ReelTheme.colors.elevated),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = kind.thumbnailIcon(),
            contentDescription = null,
            tint = ReelTheme.colors.textTertiary,
            modifier = Modifier.size(28.dp),
        )
    }
}

private fun VaultKind.thumbnailIcon(): ImageVector = when (this) {
    VaultKind.Image -> ReelIcons.Image
    VaultKind.Video -> ReelIcons.Clapperboard
    VaultKind.Audio -> ReelIcons.AudioLines
}

private fun VaultKind.playerKind(): String = when (this) {
    VaultKind.Image -> "image"
    VaultKind.Video -> "video"
    VaultKind.Audio -> "audio"
}

@Composable
private fun VaultKind.label(): String = stringResource(
    when (this) {
        VaultKind.Image -> R.string.library_images
        VaultKind.Video -> R.string.library_videos
        VaultKind.Audio -> R.string.library_audio
    },
)

private fun VaultItem.createdLabel(): String = runCatching {
    Instant.ofEpochMilli(createdAtEpochMs)
        .atZone(ZoneId.systemDefault())
        .format(
            DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM)
                .withLocale(Locale.getDefault()),
        )
}.getOrDefault("")

@Composable
private fun VaultDetailsDialog(
    item: VaultItem,
    onDismiss: () -> Unit,
) {
    val colors = ReelTheme.colors
    val file = remember(item.uri) { vaultFile(item) }
    val sizeLabel = remember(file) {
        file?.takeIf { it.exists() }?.length()?.let(::formatFileSize) ?: "—"
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(R.string.library_details_title),
                style = ReelTheme.type.titleM,
                color = colors.textPrimary,
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(ReelTheme.spacing.s),
            ) {
                DetailLine(stringResource(R.string.library_detail_title), item.title)
                DetailLine(stringResource(R.string.library_detail_kind), item.kind.label())
                DetailLine(stringResource(R.string.library_detail_size), sizeLabel)
                DetailLine(stringResource(R.string.library_detail_created), item.createdLabel())
                DetailLine(
                    stringResource(R.string.library_detail_path),
                    file?.absolutePath ?: item.uri,
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(R.string.library_close), color = colors.accent)
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

private fun vaultFile(item: VaultItem): File? = runCatching {
    val uri = Uri.parse(item.uri)
    if (uri.scheme != "file") return@runCatching null
    uri.path?.let(::File)
}.getOrNull()

private fun exportFileName(item: VaultItem): String =
    vaultFile(item)?.name ?: item.title

private fun exportVaultItem(context: Context, item: VaultItem, target: Uri): Boolean =
    runCatching {
        val source = vaultFile(item)?.takeIf { it.exists() } ?: return@runCatching false
        val output = context.contentResolver.openOutputStream(target)
            ?: return@runCatching false
        output.use { destination ->
            source.inputStream().use { input -> input.copyTo(destination) }
        }
        true
    }.getOrDefault(false)

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
