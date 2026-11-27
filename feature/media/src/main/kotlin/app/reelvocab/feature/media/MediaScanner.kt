package app.reelvocab.feature.media

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import app.reelvocab.core.domain.model.MediaItem
import app.reelvocab.core.domain.model.MediaKind
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class MediaScanResult(
    val items: List<MediaItem>,
    val failedKinds: Set<MediaKind>,
)

@Singleton
class MediaScanner @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    suspend fun scan(access: MediaAccess): MediaScanResult = withContext(Dispatchers.IO) {
        val items = mutableListOf<MediaItem>()
        val failed = mutableSetOf<MediaKind>()

        fun load(
            kind: MediaKind,
            allowed: Boolean,
            collection: Uri,
            hasDuration: Boolean,
            selection: String? = null,
        ) {
            if (!allowed) return
            runCatching {
                query(
                    collection = collection,
                    kind = kind,
                    hasDuration = hasDuration,
                    selection = selection,
                )
            }.onSuccess { loaded ->
                items += loaded
            }.onFailure {
                failed += kind
            }
        }

        load(
            kind = MediaKind.Video,
            allowed = access.video,
            collection = MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
            hasDuration = true,
            selection = "${MediaStore.MediaColumns.DURATION} > 0",
        )
        load(
            kind = MediaKind.Image,
            allowed = access.images,
            collection = MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            hasDuration = false,
        )
        load(
            kind = MediaKind.Audio,
            allowed = access.audio,
            collection = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
            hasDuration = true,
            selection = "${MediaStore.MediaColumns.DURATION} > 0",
        )

        MediaScanResult(
            items = items.distinctBy { it.uri },
            failedKinds = failed,
        )
    }

    private fun query(
        collection: Uri,
        kind: MediaKind,
        hasDuration: Boolean,
        selection: String? = null,
    ): List<MediaItem> {
        val projection = if (hasDuration) {
            arrayOf(
                MediaStore.MediaColumns._ID,
                MediaStore.MediaColumns.TITLE,
                MediaStore.MediaColumns.DISPLAY_NAME,
                MediaStore.MediaColumns.MIME_TYPE,
                MediaStore.MediaColumns.DURATION,
            )
        } else {
            arrayOf(
                MediaStore.MediaColumns._ID,
                MediaStore.MediaColumns.TITLE,
                MediaStore.MediaColumns.DISPLAY_NAME,
                MediaStore.MediaColumns.MIME_TYPE,
            )
        }
        return context.contentResolver.query(
            collection,
            projection,
            selection,
            null,
            "${MediaStore.MediaColumns.DATE_ADDED} DESC",
        )?.use { cursor ->
            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns._ID)
            val titleColumn = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.TITLE)
            val nameColumn = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DISPLAY_NAME)
            val mimeColumn = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.MIME_TYPE)
            val durationColumn = if (hasDuration) {
                cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DURATION)
            } else {
                -1
            }
            buildList {
                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idColumn)
                    val title = cursor.getString(titleColumn)
                        ?.takeIf { it.isNotBlank() }
                        ?: cursor.getString(nameColumn)?.substringBeforeLast('.')
                        ?: "Untitled"
                    add(
                        MediaItem(
                            id = id,
                            uri = ContentUris.withAppendedId(collection, id).toString(),
                            title = title,
                            durationMs = if (durationColumn >= 0) {
                                cursor.getLong(durationColumn)
                            } else {
                                0L
                            },
                            kind = kind,
                            mimeType = cursor.getString(mimeColumn),
                        ),
                    )
                }
            }
        }.orEmpty()
    }
}
