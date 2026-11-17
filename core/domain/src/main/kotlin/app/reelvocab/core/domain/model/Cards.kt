package app.reelvocab.core.domain.model

data class Card(
    val id: Long,
    val headword: String,
    val ipa: String?,
    val meaning: String,
    val wordClass: WordClass?,
    val contextLine: String?,
    val sourceTitle: String?,
    val sourceTimestampMs: Long?,
    val hasAudio: Boolean,
    val createdAtEpochMs: Long,
)

data class NewCard(
    val headword: String,
    val meaning: String,
    val ipa: String? = null,
    val wordClass: WordClass? = null,
    val contextLine: String? = null,
    val sourceTitle: String? = null,
    val sourceTimestampMs: Long? = null,
    val hasAudio: Boolean = false,
)

data class Film(
    val id: Long,
    val title: String,
    val year: Int?,
)

enum class MediaKind { Video, Audio }

data class MediaItem(
    val id: Long,
    val uri: String,
    val title: String,
    val durationMs: Long,
    val kind: MediaKind,
)
