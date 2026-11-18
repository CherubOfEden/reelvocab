package app.reelvocab.core.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface CardDao {
    @Query("SELECT * FROM cards ORDER BY created_at DESC")
    fun observeAll(): Flow<List<CardEntity>>

    @Query("SELECT COUNT(*) FROM cards")
    fun observeCount(): Flow<Int>

    @Query("SELECT * FROM cards WHERE id = :id")
    suspend fun byId(id: Long): CardEntity?

    @Insert
    suspend fun insert(card: CardEntity): Long

    @Query("UPDATE cards SET tags = :tags WHERE id = :id")
    suspend fun updateTags(id: Long, tags: String)

    @Query("DELETE FROM cards WHERE id = :id")
    suspend fun delete(id: Long)
}

@Dao
interface WordDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(word: WordEntity): Long

    @Query("SELECT * FROM words WHERE normalized = :normalized LIMIT 1")
    suspend fun findByNormalized(normalized: String): WordEntity?
}

@Dao
interface FilmDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(film: FilmEntity): Long

    @Query("SELECT * FROM films ORDER BY title")
    fun observeAll(): Flow<List<FilmEntity>>
}

@Dao
interface ReviewDao {
    @Query("SELECT * FROM reviews WHERE due_at <= :now ORDER BY due_at ASC LIMIT :limit")
    suspend fun due(now: Long, limit: Int): List<ReviewEntity>

    @Query("SELECT COUNT(*) FROM reviews WHERE due_at <= :now")
    fun observeDueCount(now: Long): Flow<Int>

    @Query("SELECT card_id FROM reviews WHERE due_at <= :now")
    fun observeDueCardIds(now: Long): Flow<List<Long>>

    @Query("SELECT * FROM reviews WHERE card_id = :cardId")
    suspend fun byCard(cardId: Long): ReviewEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(review: ReviewEntity)

    @Insert
    suspend fun insertLog(log: ReviewLogEntity)

    @Query("SELECT * FROM review_logs WHERE card_id = :cardId ORDER BY reviewed_at DESC LIMIT :limit")
    suspend fun history(cardId: Long, limit: Int): List<ReviewLogEntity>
}
