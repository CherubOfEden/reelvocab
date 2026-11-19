package app.reelvocab.core.domain.model

data class Card(
    val id: Long,
    val headword: String,
    val ipa: String?,
    val meaning: String,
    val example: String? = null,
    val wordClass: WordClass?,
    val contextLine: String?,
    val sourceTitle: String?,
    val sourceTimestampMs: Long?,
    val hasAudio: Boolean,
    val createdAtEpochMs: Long,
    val tags: List<String> = emptyList(),
    val isFavorite: Boolean = false,
)

data class NewCard(
    val headword: String,
    val meaning: String,
    val example: String? = null,
    val ipa: String? = null,
    val wordClass: WordClass? = null,
    val contextLine: String? = null,
    val sourceTitle: String? = null,
    val sourceTimestampMs: Long? = null,
    val hasAudio: Boolean = false,
    val tags: List<String> = emptyList(),
)

data class Film(
    val id: Long,
    val title: String,
    val year: Int?,
)

enum class MediaKind { Video, Image, Audio }

data class MediaItem(
    val id: Long,
    val uri: String,
    val title: String,
    val durationMs: Long,
    val kind: MediaKind,
    val mimeType: String? = null,
)
