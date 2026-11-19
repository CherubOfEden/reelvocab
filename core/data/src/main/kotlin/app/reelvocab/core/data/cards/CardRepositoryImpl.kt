package app.reelvocab.core.data.cards

import app.reelvocab.core.common.Tags
import app.reelvocab.core.data.db.CardDao
import app.reelvocab.core.data.db.CardEntity
import app.reelvocab.core.data.db.ReviewDao
import app.reelvocab.core.data.db.toDomain
import app.reelvocab.core.data.db.toEntity
import app.reelvocab.core.domain.model.Card
import app.reelvocab.core.domain.model.CardRepository
import app.reelvocab.core.domain.model.NewCard
import app.reelvocab.core.domain.scheduler.Sm2Scheduler
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

@Singleton
class CardRepositoryImpl @Inject constructor(
    private val cardDao: CardDao,
    private val reviewDao: ReviewDao,
) : CardRepository {

    override fun observeCards(): Flow<List<Card>> =
        cardDao.observeAll().map { rows -> rows.map(CardEntity::toDomain) }

    override fun observeCount(): Flow<Int> = cardDao.observeCount()

    override suspend fun addCard(card: NewCard): Long {
        val now = System.currentTimeMillis()
        val id = cardDao.insert(
            CardEntity(
                wordId = null,
                headword = card.headword,
                ipa = card.ipa,
                meaning = card.meaning,
                example = card.example,
                wordClass = card.wordClass?.storageValue,
                contextLine = card.contextLine,
                filmId = null,
                sourceTitle = card.sourceTitle,
                sourceTimestampMs = card.sourceTimestampMs,
                hasAudio = card.hasAudio,
                createdAtEpochMs = now,
                tags = Tags.encode(card.tags),
            ),
        )
        reviewDao.upsert(Sm2Scheduler.initial(id, now).toEntity())
        return id
    }

    override suspend fun deleteCard(id: Long) = cardDao.delete(id)

    override suspend fun updateTags(cardId: Long, tags: List<String>) {
        cardDao.updateTags(cardId, Tags.encode(tags))
    }

    override suspend fun setFavorite(cardId: Long, favorite: Boolean) {
        cardDao.updateFavorite(cardId, favorite)
    }
}
