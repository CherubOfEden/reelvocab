package app.reelvocab.core.domain.model

import kotlinx.coroutines.flow.Flow

enum class ReviewGrade { Again, Hard, Good, Easy }

data class ReviewState(
    val cardId: Long,
    val repetitions: Int,
    val easeFactor: Double,
    val intervalDays: Int,
    val dueAtEpochMs: Long,
    val lastReviewedAtEpochMs: Long?,
    val lapses: Int,
)

data class ReviewLog(
    val id: Long,
    val cardId: Long,
    val grade: ReviewGrade,
    val reviewedAtEpochMs: Long,
    val repetitions: Int,
    val easeFactor: Double,
    val intervalDays: Int,
)

data class ReviewCard(
    val card: Card,
    val state: ReviewState,
)

interface ReviewRepository {
    fun observeDueCount(nowEpochMs: Long): Flow<Int>
    fun observeDueCardIds(nowEpochMs: Long): Flow<Set<Long>>
    suspend fun dueCards(nowEpochMs: Long, limit: Int = 50): List<ReviewCard>
    suspend fun grade(cardId: Long, grade: ReviewGrade, nowEpochMs: Long): ReviewState?
    suspend fun ensureScheduled(cardId: Long, nowEpochMs: Long): ReviewState
    suspend fun history(cardId: Long, limit: Int = 20): List<ReviewLog>
}
