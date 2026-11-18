package app.reelvocab.core.data.reviews

import app.reelvocab.core.data.db.CardDao
import app.reelvocab.core.data.db.ReviewDao
import app.reelvocab.core.data.db.ReviewLogEntity
import app.reelvocab.core.data.db.toDomain
import app.reelvocab.core.data.db.toEntity
import app.reelvocab.core.domain.model.ReviewCard
import app.reelvocab.core.domain.model.ReviewGrade
import app.reelvocab.core.domain.model.ReviewLog
import app.reelvocab.core.domain.model.ReviewRepository
import app.reelvocab.core.domain.model.ReviewState
import app.reelvocab.core.domain.scheduler.Sm2Scheduler
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

@Singleton
class ReviewRepositoryImpl @Inject constructor(
    private val reviewDao: ReviewDao,
    private val cardDao: CardDao,
) : ReviewRepository {

    override fun observeDueCount(nowEpochMs: Long): Flow<Int> =
        reviewDao.observeDueCount(nowEpochMs)

    override fun observeDueCardIds(nowEpochMs: Long): Flow<Set<Long>> =
        reviewDao.observeDueCardIds(nowEpochMs).map { it.toSet() }

    override suspend fun dueCards(nowEpochMs: Long, limit: Int): List<ReviewCard> =
        reviewDao.due(nowEpochMs, limit).mapNotNull { review ->
            val card = cardDao.byId(review.cardId) ?: return@mapNotNull null
            ReviewCard(card = card.toDomain(), state = review.toDomain())
        }

    override suspend fun grade(
        cardId: Long,
        grade: ReviewGrade,
        nowEpochMs: Long,
    ): ReviewState? {
        val current = reviewDao.byCard(cardId) ?: return null
        val next = Sm2Scheduler.next(current.toDomain(), grade, nowEpochMs)
        reviewDao.upsert(next.toEntity())
        reviewDao.insertLog(
            ReviewLogEntity(
                cardId = cardId,
                grade = grade.name,
                reviewedAtEpochMs = nowEpochMs,
                repetitions = next.repetitions,
                easeFactor = next.easeFactor,
                intervalDays = next.intervalDays,
            ),
        )
        return next
    }

    override suspend fun ensureScheduled(cardId: Long, nowEpochMs: Long): ReviewState {
        val existing = reviewDao.byCard(cardId)
        if (existing != null) return existing.toDomain()
        val state = Sm2Scheduler.initial(cardId, nowEpochMs)
        reviewDao.upsert(state.toEntity())
        return state
    }

    override suspend fun history(cardId: Long, limit: Int): List<ReviewLog> =
        reviewDao.history(cardId, limit).map { row ->
            ReviewLog(
                id = row.id,
                cardId = row.cardId,
                grade = ReviewGrade.entries.firstOrNull { it.name == row.grade }
                    ?: ReviewGrade.Again,
                reviewedAtEpochMs = row.reviewedAtEpochMs,
                repetitions = row.repetitions,
                easeFactor = row.easeFactor,
                intervalDays = row.intervalDays,
            )
        }
}
