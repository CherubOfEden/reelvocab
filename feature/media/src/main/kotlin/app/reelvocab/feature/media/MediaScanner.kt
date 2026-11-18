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

@Singleton
class MediaScanner @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    suspend fun scan(): List<MediaItem> = withContext(Dispatchers.IO) {
        val videos = query(
            collection = MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
            kind = MediaKind.Video,
        )
        val audio = query(
            collection = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
            kind = MediaKind.Audio,
        )
        (videos + audio).sortedByDescending { it.id }
    }

    private fun query(collection: Uri, kind: MediaKind): List<MediaItem> {
        val projection = arrayOf(
            MediaStore.MediaColumns._ID,
            MediaStore.MediaColumns.TITLE,
            MediaStore.MediaColumns.DISPLAY_NAME,
            MediaStore.MediaColumns.DURATION,
        )
        return context.contentResolver.query(
            collection,
            projection,
            "${MediaStore.MediaColumns.DURATION} > 0",
            null,
            "${MediaStore.MediaColumns.DATE_ADDED} DESC",
        )?.use { cursor ->
            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns._ID)
            val titleColumn = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.TITLE)
            val nameColumn = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DISPLAY_NAME)
            val durationColumn = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DURATION)
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
                            durationMs = cursor.getLong(durationColumn),
                            kind = kind,
                        ),
                    )
                }
            }
        }.orEmpty()
    }
}
