package app.reelvocab.core.domain.scheduler

import app.reelvocab.core.domain.model.ReviewGrade
import app.reelvocab.core.domain.model.ReviewState
import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import kotlin.math.roundToInt

/**
 * SM-2 with the four grades the deck UI already uses.
 *
 * Algorithms follow the 1987 SuperMemo formulation; the first review of a new
 * card is scheduled for 08:00 the next morning instead of "now" so cards land
 * in the morning review.
 */
object Sm2Scheduler {

    const val DEFAULT_EASE: Double = 2.5
    const val MIN_EASE: Double = 1.3

    private const val DAY_MS = 24 * 60 * 60 * 1000L
    private const val RELEARN_DELAY_MS = 10 * 60 * 1000L
    private const val MORNING_HOUR = 8

    fun initial(cardId: Long, nowEpochMs: Long): ReviewState = ReviewState(
        cardId = cardId,
        repetitions = 0,
        easeFactor = DEFAULT_EASE,
        intervalDays = 0,
        dueAtEpochMs = nextMorning(nowEpochMs),
        lastReviewedAtEpochMs = null,
        lapses = 0,
    )

    fun next(state: ReviewState, grade: ReviewGrade, nowEpochMs: Long): ReviewState {
        val quality = when (grade) {
            ReviewGrade.Again -> 2
            ReviewGrade.Hard -> 3
            ReviewGrade.Good -> 4
            ReviewGrade.Easy -> 5
        }
        val failed = quality < 3
        val repetitions = if (failed) 0 else state.repetitions + 1
        val intervalDays = when {
            failed -> 0
            repetitions == 1 -> if (grade == ReviewGrade.Easy) 4 else 1
            repetitions == 2 -> 6
            else -> (state.intervalDays.coerceAtLeast(1) * state.easeFactor)
                .roundToInt()
                .coerceAtLeast(1)
        }
        val easeFactor = (
            state.easeFactor +
                (0.1 - (5 - quality) * (0.08 + (5 - quality) * 0.02))
            ).coerceAtLeast(MIN_EASE)
        val dueAt = if (failed) {
            nowEpochMs + RELEARN_DELAY_MS
        } else {
            nowEpochMs + intervalDays * DAY_MS
        }

        return ReviewState(
            cardId = state.cardId,
            repetitions = repetitions,
            easeFactor = easeFactor,
            intervalDays = intervalDays,
            dueAtEpochMs = dueAt,
            lastReviewedAtEpochMs = nowEpochMs,
            lapses = if (failed) state.lapses + 1 else state.lapses,
        )
    }

    fun nextMorning(
        nowEpochMs: Long,
        zone: ZoneId = ZoneId.systemDefault(),
    ): Long {
        val now = ZonedDateTime.ofInstant(Instant.ofEpochMilli(nowEpochMs), zone)
        val next = now.toLocalDate()
            .plusDays(1)
            .atTime(LocalTime.of(MORNING_HOUR, 0))
            .atZone(zone)
        return next.toInstant().toEpochMilli()
    }
}
