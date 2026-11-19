package app.reelvocab.core.data.db

import app.reelvocab.core.common.Tags
import app.reelvocab.core.domain.model.Card
import app.reelvocab.core.domain.model.ReviewState
import app.reelvocab.core.domain.model.WordClass

internal fun CardEntity.toDomain(): Card = Card(
    id = id,
    headword = headword,
    ipa = ipa,
    meaning = meaning,
    wordClass = WordClass.fromStorage(wordClass),
    contextLine = contextLine,
    sourceTitle = sourceTitle,
    sourceTimestampMs = sourceTimestampMs,
    hasAudio = hasAudio,
    createdAtEpochMs = createdAtEpochMs,
    tags = Tags.decode(tags),
    isFavorite = isFavorite,
)

internal fun ReviewEntity.toDomain(): ReviewState = ReviewState(
    cardId = cardId,
    repetitions = repetitions,
    easeFactor = easeFactor,
    intervalDays = intervalDays,
    dueAtEpochMs = dueAtEpochMs,
    lastReviewedAtEpochMs = lastReviewedAtEpochMs,
    lapses = lapses,
)

internal fun ReviewState.toEntity(): ReviewEntity = ReviewEntity(
    cardId = cardId,
    repetitions = repetitions,
    easeFactor = easeFactor,
    intervalDays = intervalDays,
    dueAtEpochMs = dueAtEpochMs,
    lastReviewedAtEpochMs = lastReviewedAtEpochMs,
    lapses = lapses,
)
