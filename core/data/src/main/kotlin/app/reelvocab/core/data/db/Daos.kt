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
