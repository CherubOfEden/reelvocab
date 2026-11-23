package app.reelvocab.feature.media

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.effect.Crop
import androidx.media3.transformer.Composition
import androidx.media3.transformer.EditedMediaItem
import androidx.media3.transformer.Effects
import androidx.media3.transformer.ExportException
import androidx.media3.transformer.ExportResult
import androidx.media3.transformer.ProgressHolder
import androidx.media3.transformer.Transformer
import app.reelvocab.core.domain.model.MediaVaultRepository
import app.reelvocab.core.domain.model.VaultItem
import app.reelvocab.core.domain.model.VaultKind
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class EditorCrop(val aspect: Float?) {
    Original(null),
    Landscape(16f / 9f),
    Square(1f),
    Portrait(9f / 16f),
}

data class MediaEditorUiState(
    val loading: Boolean = true,
    val kind: VaultKind = VaultKind.Video,
    val title: String = "",
    val uri: String = "",
    val durationMs: Long = 0L,
    val startMs: Long = 0L,
    val endMs: Long = 0L,
    val sourceWidth: Int = 0,
    val sourceHeight: Int = 0,
    val crop: EditorCrop = EditorCrop.Original,
    val exporting: Boolean = false,
    val progress: Int = 0,
    val savedUri: String? = null,
    val error: Boolean = false,
    val loadError: Boolean = false,
)

@androidx.annotation.OptIn(UnstableApi::class)
@HiltViewModel
class MediaEditorViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val vault: MediaVaultRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(MediaEditorUiState())
    val state: StateFlow<MediaEditorUiState> = _state.asStateFlow()

    private var transformer: Transformer? = null
    private var loadedUri: String? = null

    fun load(route: EditorRoute) {
        if (loadedUri == route.uri) return
        loadedUri = route.uri
        val kind = when (route.kind) {
            "audio" -> VaultKind.Audio
            "image" -> VaultKind.Image
            else -> VaultKind.Video
        }
        _state.value = MediaEditorUiState(kind = kind, title = route.title, uri = route.uri)
        viewModelScope.launch {
            val probe = withContext(Dispatchers.IO) {
                runCatching { probeMedia(route.uri, kind) }.getOrNull()
            }
            _state.update {
                if (probe == null) {
                    it.copy(loading = false, loadError = true)
                } else {
                    it.copy(
                        loading = false,
                        durationMs = probe.durationMs,
                        endMs = probe.durationMs,
                        sourceWidth = probe.width,
                        sourceHeight = probe.height,
                    )
                }
            }
        }
    }

    fun setStart(value: Long) {
        _state.update { current ->
            val end = current.endMs.coerceAtLeast(current.startMs + MIN_CLIP_MS)
            current.copy(startMs = value.coerceIn(0L, (end - MIN_CLIP_MS).coerceAtLeast(0L)))
        }
    }

    fun setEnd(value: Long) {
        _state.update { current ->
            val start = current.startMs.coerceAtMost(
                (current.durationMs - MIN_CLIP_MS).coerceAtLeast(0L),
            )
            current.copy(endMs = value.coerceIn(start + MIN_CLIP_MS, current.durationMs))
        }
    }

    fun setCrop(crop: EditorCrop) {
        _state.update { it.copy(crop = crop) }
    }

    fun export() {
        val current = _state.value
        if (current.exporting || current.loading) return
        _state.update { it.copy(exporting = true, progress = 0, error = false, savedUri = null) }
        when (current.kind) {
            VaultKind.Image -> exportImage(current)
            VaultKind.Video, VaultKind.Audio -> startTransformer(current)
        }
    }

    private fun startTransformer(current: MediaEditorUiState) {
        val vaultDir = File(context.filesDir, "vault").apply { mkdirs() }
        val extension = "mp4"
        val startedAt = System.currentTimeMillis()
        val output = File(vaultDir, "edit_$startedAt.$extension")
        val end = current.endMs.coerceAtLeast(current.startMs + MIN_CLIP_MS)
        val mediaItem = MediaItem.Builder()
            .setUri(current.uri)
            .setClippingConfiguration(
                MediaItem.ClippingConfiguration.Builder()
                    .setStartPositionMs(current.startMs)
                    .setEndPositionMs(end)
                    .build(),
            )
            .build()
        val edited = EditedMediaItem.Builder(mediaItem).apply {
            cropEffect(current)?.let { effect ->
                setEffects(Effects(listOf(), listOf(effect)))
            }
        }.build()

        val listener = object : Transformer.Listener {
            override fun onCompleted(composition: Composition, exportResult: ExportResult) {
                viewModelScope.launch {
                    val item = VaultItem(
                        id = "edit_$startedAt",
                        uri = "file://${output.absolutePath}",
                        title = "${current.title} (edited)",
                        kind = current.kind,
                        createdAtEpochMs = startedAt,
                    )
                    runCatching { vault.add(item) }
                    transformer = null
                    _state.update {
                        it.copy(
                            exporting = false,
                            progress = 100,
                            savedUri = item.uri,
                        )
                    }
                }
            }

            override fun onError(
                composition: Composition,
                exportResult: ExportResult,
                exportException: ExportException,
            ) {
                runCatching { output.delete() }
                transformer = null
                _state.update {
                    it.copy(exporting = false, progress = 0, error = true)
                }
            }
        }

        val instance = Transformer.Builder(context)
            .addListener(listener)
            .build()
        transformer = instance
        runCatching {
            instance.start(edited, output.absolutePath)
            monitorProgress(instance)
        }.onFailure {
            runCatching { output.delete() }
            transformer = null
            _state.update { it.copy(exporting = false, progress = 0, error = true) }
        }
    }

    private fun monitorProgress(instance: Transformer) {
        viewModelScope.launch {
            val holder = ProgressHolder()
            while (_state.value.exporting && transformer === instance) {
                val state = instance.getProgress(holder)
                if (state == Transformer.PROGRESS_STATE_AVAILABLE) {
                    _state.update { it.copy(progress = holder.progress.coerceIn(0, 100)) }
                }
                delay(220L)
            }
        }
    }

    private fun exportImage(current: MediaEditorUiState) {
        viewModelScope.launch {
            val saved = withContext(Dispatchers.IO) {
                runCatching {
                    val source = decodeImage(current.uri)
                        ?: return@runCatching null
                    val cropped = cropBitmap(source, current.crop.aspect)
                    val vaultDir = File(context.filesDir, "vault").apply { mkdirs() }
                    val startedAt = System.currentTimeMillis()
                    val output = File(vaultDir, "edit_$startedAt.jpg")
                    FileOutputStream(output).use { stream ->
                        cropped.compress(Bitmap.CompressFormat.JPEG, 94, stream)
                    }
                    if (cropped !== source) source.recycle()
                    cropped.recycle()
                    VaultItem(
                        id = "edit_$startedAt",
                        uri = "file://${output.absolutePath}",
                        title = "${current.title} (edited)",
                        kind = VaultKind.Image,
                        createdAtEpochMs = startedAt,
                    )
                }.getOrNull()
            }
            if (saved != null) {
                runCatching { vault.add(saved) }
                _state.update {
                    it.copy(
                        exporting = false,
                        progress = 100,
                        savedUri = saved.uri,
                    )
                }
            } else {
                _state.update { it.copy(exporting = false, progress = 0, error = true) }
            }
        }
    }

    private fun cropEffect(current: MediaEditorUiState): Crop? {
        if (current.kind != VaultKind.Video) return null
        val aspect = current.crop.aspect ?: return null
        val width = current.sourceWidth
        val height = current.sourceHeight
        if (width <= 0 || height <= 0) return null
        val sourceAspect = width.toFloat() / height.toFloat()
        val keepWidth: Float
        val keepHeight: Float
        if (sourceAspect > aspect) {
            keepWidth = aspect / sourceAspect
            keepHeight = 1f
        } else {
            keepWidth = 1f
            keepHeight = sourceAspect / aspect
        }
        return Crop(-keepWidth, keepWidth, -keepHeight, keepHeight)
    }

    private fun probeMedia(uri: String, kind: VaultKind): MediaProbe {
        if (kind == VaultKind.Image) {
            val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            context.contentResolver.openInputStream(Uri.parse(uri))?.use { stream ->
                BitmapFactory.decodeStream(stream, null, options)
            }
            return MediaProbe(
                durationMs = IMAGE_DURATION_MS,
                width = options.outWidth.coerceAtLeast(0),
                height = options.outHeight.coerceAtLeast(0),
            )
        }
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(context, Uri.parse(uri))
            var width = retriever.extractMetadata(
                MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH,
            )?.toIntOrNull() ?: 0
            var height = retriever.extractMetadata(
                MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT,
            )?.toIntOrNull() ?: 0
            val rotation = retriever.extractMetadata(
                MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION,
            )?.toIntOrNull() ?: 0
            if (rotation == 90 || rotation == 270) {
                val swap = width
                width = height
                height = swap
            }
            MediaProbe(
                durationMs = retriever.extractMetadata(
                    MediaMetadataRetriever.METADATA_KEY_DURATION,
                )?.toLongOrNull()?.coerceAtLeast(MIN_CLIP_MS) ?: MIN_CLIP_MS,
                width = width,
                height = height,
            )
        } finally {
            runCatching { retriever.release() }
        }
    }

    private fun decodeImage(uri: String): Bitmap? {
        val parsed = Uri.parse(uri)
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(parsed)?.use { stream ->
            BitmapFactory.decodeStream(stream, null, bounds)
        }
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
        var sampleSize = 1
        while (
            bounds.outWidth / sampleSize > MAX_IMAGE_DIMENSION ||
            bounds.outHeight / sampleSize > MAX_IMAGE_DIMENSION
        ) {
            sampleSize *= 2
        }
        val options = BitmapFactory.Options().apply { inSampleSize = sampleSize }
        return context.contentResolver.openInputStream(parsed)?.use { stream ->
            BitmapFactory.decodeStream(stream, null, options)
        }
    }

    private fun cropBitmap(source: Bitmap, aspect: Float?): Bitmap {
        if (aspect == null) return source
        val width = source.width
        val height = source.height
        if (width <= 0 || height <= 0) return source
        val sourceAspect = width.toFloat() / height.toFloat()
        return if (sourceAspect > aspect) {
            val newWidth = (height * aspect).toInt().coerceAtLeast(1)
            val left = (width - newWidth) / 2
            Bitmap.createBitmap(source, left, 0, newWidth, height)
        } else {
            val newHeight = (width / aspect).toInt().coerceAtLeast(1)
            val top = (height - newHeight) / 2
            Bitmap.createBitmap(source, 0, top, width, newHeight)
        }
    }

    override fun onCleared() {
        transformer?.let { runCatching { it.cancel() } }
        transformer = null
        super.onCleared()
    }

    private data class MediaProbe(
        val durationMs: Long,
        val width: Int,
        val height: Int,
    )

    private companion object {
        const val MIN_CLIP_MS = 500L
        const val IMAGE_DURATION_MS = 1L
        const val MAX_IMAGE_DIMENSION = 4_096
    }
}
