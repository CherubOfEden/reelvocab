package app.reelvocab.core.data.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "words",
    indices = [Index(value = ["normalized", "part_of_speech"], unique = true)],
)
data class WordEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val headword: String,
    val normalized: String,
    val ipa: String?,
    @ColumnInfo(name = "part_of_speech") val partOfSpeech: String?,
    val difficulty: Int?,
    @ColumnInfo(name = "created_at") val createdAtEpochMs: Long,
)

@Entity(
    tableName = "word_senses",
    foreignKeys = [
        ForeignKey(
            entity = WordEntity::class,
            parentColumns = ["id"],
            childColumns = ["word_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("word_id")],
)
data class WordSenseEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "word_id") val wordId: Long,
    val definition: String,
    val example: String?,
    @ColumnInfo(name = "is_ai_context") val isAiContext: Boolean = false,
)

@Entity(
    tableName = "films",
    indices = [Index(value = ["title"], unique = true)],
)
data class FilmEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val year: Int?,
)

@Entity(
    tableName = "cards",
    foreignKeys = [
        ForeignKey(
            entity = WordEntity::class,
            parentColumns = ["id"],
            childColumns = ["word_id"],
            onDelete = ForeignKey.SET_NULL,
        ),
        ForeignKey(
            entity = FilmEntity::class,
            parentColumns = ["id"],
            childColumns = ["film_id"],
            onDelete = ForeignKey.SET_NULL,
        ),
    ],
    indices = [Index("word_id"), Index("film_id"), Index("created_at")],
)
data class CardEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "word_id") val wordId: Long?,
    val headword: String,
    val ipa: String?,
    val meaning: String,
    @ColumnInfo(name = "word_class") val wordClass: String?,
    @ColumnInfo(name = "context_line") val contextLine: String?,
    @ColumnInfo(name = "film_id") val filmId: Long?,
    @ColumnInfo(name = "source_title") val sourceTitle: String?,
    @ColumnInfo(name = "source_timestamp_ms") val sourceTimestampMs: Long?,
    @ColumnInfo(name = "has_audio") val hasAudio: Boolean,
    @ColumnInfo(name = "created_at") val createdAtEpochMs: Long,
)
