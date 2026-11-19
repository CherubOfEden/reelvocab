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
    val example: String?,
    @ColumnInfo(name = "word_class") val wordClass: String?,
    @ColumnInfo(name = "context_line") val contextLine: String?,
    @ColumnInfo(name = "film_id") val filmId: Long?,
    @ColumnInfo(name = "source_title") val sourceTitle: String?,
    @ColumnInfo(name = "source_timestamp_ms") val sourceTimestampMs: Long?,
    @ColumnInfo(name = "has_audio") val hasAudio: Boolean,
    @ColumnInfo(name = "created_at") val createdAtEpochMs: Long,
    @ColumnInfo(defaultValue = "''") val tags: String = "",
    @ColumnInfo(name = "is_favorite", defaultValue = "0") val isFavorite: Boolean = false,
)

@Entity(
    tableName = "reviews",
    foreignKeys = [
        ForeignKey(
            entity = CardEntity::class,
            parentColumns = ["id"],
            childColumns = ["card_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("due_at")],
)
data class ReviewEntity(
    @PrimaryKey
    @ColumnInfo(name = "card_id") val cardId: Long,
    val repetitions: Int,
    @ColumnInfo(name = "ease_factor") val easeFactor: Double,
    @ColumnInfo(name = "interval_days") val intervalDays: Int,
    @ColumnInfo(name = "due_at") val dueAtEpochMs: Long,
    @ColumnInfo(name = "last_reviewed_at") val lastReviewedAtEpochMs: Long?,
    val lapses: Int,
)

@Entity(
    tableName = "review_logs",
    foreignKeys = [
        ForeignKey(
            entity = CardEntity::class,
            parentColumns = ["id"],
            childColumns = ["card_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("card_id"), Index("reviewed_at")],
)
data class ReviewLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "card_id") val cardId: Long,
    val grade: String,
    @ColumnInfo(name = "reviewed_at") val reviewedAtEpochMs: Long,
    val repetitions: Int,
    @ColumnInfo(name = "ease_factor") val easeFactor: Double,
    @ColumnInfo(name = "interval_days") val intervalDays: Int,
)
