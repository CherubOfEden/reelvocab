package app.reelvocab.feature.library

import android.net.Uri
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Photo
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
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
import app.reelvocab.core.designsystem.component.CardSurface
import app.reelvocab.core.designsystem.component.EmptyState
import app.reelvocab.core.designsystem.component.ReelChip
import app.reelvocab.core.designsystem.illustration.ReelIllustration
import app.reelvocab.core.designsystem.theme.ReelTheme
import app.reelvocab.core.domain.model.VaultItem
import app.reelvocab.core.domain.model.VaultKind
import coil.ImageLoader
import coil.compose.SubcomposeAsyncImage
import coil.decode.VideoFrameDecoder
import coil.request.ImageRequest
import coil.request.videoFrameMillis
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale
import kotlinx.serialization.Serializable

@Serializable
data object LibraryRoute

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun LibraryScreen(
    onOpenDeck: () -> Unit,
    onPlay: (uri: String, title: String, kind: String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: LibraryViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val colors = ReelTheme.colors
    val context = LocalContext.current
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

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = ReelTheme.spacing.screenH,
            end = ReelTheme.spacing.screenH,
            top = ReelTheme.spacing.xl,
            bottom = ReelTheme.spacing.screenBottom,
        ),
        verticalArrangement = Arrangement.spacedBy(ReelTheme.spacing.m),
    ) {
        item {
            Text(
                text = stringResource(R.string.library_title),
                style = ReelTheme.type.titleL,
                color = colors.textPrimary,
            )
            Spacer(Modifier.height(ReelTheme.spacing.xs))
            Text(
                text = stringResource(R.string.library_subtitle),
                style = ReelTheme.type.bodyM,
                color = colors.textSecondary,
            )
        }

        stickyHeader(key = "library-categories") {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(colors.background)
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
                    onDelete = { viewModel.delete(item) },
                )
            }
        }
    }
}

@Composable
private fun VaultRow(
    item: VaultItem,
    imageLoader: ImageLoader,
    onPlay: () -> Unit,
    onDelete: () -> Unit,
) {
    val colors = ReelTheme.colors
    CardSurface(onClick = onPlay) {
        Row(
            modifier = Modifier.fillMaxWidth(),
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
            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Filled.DeleteOutline,
                    contentDescription = stringResource(R.string.library_delete),
                    tint = colors.rose,
                )
            }
        }
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
                imageVector = Icons.Filled.GraphicEq,
                contentDescription = null,
                tint = colors.gold,
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
    VaultKind.Image -> Icons.Filled.Photo
    VaultKind.Video -> Icons.Filled.Movie
    VaultKind.Audio -> Icons.Filled.GraphicEq
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
