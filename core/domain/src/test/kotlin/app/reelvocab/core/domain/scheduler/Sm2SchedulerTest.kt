package app.reelvocab.core.domain.scheduler

import app.reelvocab.core.domain.model.ReviewGrade
import java.time.Instant
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class Sm2SchedulerTest {

    private val zone = ZoneId.of("UTC")
    private val now = Instant.parse("2026-11-18T12:00:00Z").toEpochMilli()

    @Test
    fun `new card is due at 8am the next morning`() {
        val state = Sm2Scheduler.initial(cardId = 7, nowEpochMs = now)

        assertEquals(7, state.cardId)
        assertEquals(Sm2Scheduler.DEFAULT_EASE, state.easeFactor, 0.0001)
        assertEquals(0, state.repetitions)
        assertEquals(
            Instant.parse("2026-11-19T08:00:00Z").toEpochMilli(),
            state.dueAtEpochMs,
        )
    }

    @Test
    fun `good grades walk the 1 6 15 day ladder`() {
        val initial = Sm2Scheduler.initial(1, now)
        val first = Sm2Scheduler.next(initial, ReviewGrade.Good, now)
        val second = Sm2Scheduler.next(first, ReviewGrade.Good, now)
        val third = Sm2Scheduler.next(second, ReviewGrade.Good, now)

        assertEquals(1, first.intervalDays)
        assertEquals(6, second.intervalDays)
        assertEquals(15, third.intervalDays)
        assertEquals(1, first.repetitions)
        assertEquals(3, third.repetitions)
        assertEquals(Sm2Scheduler.DEFAULT_EASE, third.easeFactor, 0.0001)
    }

    @Test
    fun `again resets the ladder and schedules a ten minute relearn`() {
        val started = Sm2Scheduler.next(
            Sm2Scheduler.initial(1, now),
            ReviewGrade.Good,
            now,
        )
        val failed = Sm2Scheduler.next(started, ReviewGrade.Again, now)

        assertEquals(0, failed.repetitions)
        assertEquals(0, failed.intervalDays)
        assertEquals(1, failed.lapses)
        assertEquals(now + 10 * 60 * 1000L, failed.dueAtEpochMs)
        assertTrue(failed.easeFactor < started.easeFactor)
        assertEquals(2.18, failed.easeFactor, 0.0001)
    }

    @Test
    fun `hard lowers ease and easy raises it`() {
        val started = Sm2Scheduler.initial(1, now)
        val hard = Sm2Scheduler.next(started, ReviewGrade.Hard, now)
        val easy = Sm2Scheduler.next(started, ReviewGrade.Easy, now)

        assertEquals(2.36, hard.easeFactor, 0.0001)
        assertEquals(2.6, easy.easeFactor, 0.0001)
        assertEquals(4, easy.intervalDays)
    }

    @Test
    fun `ease never drops below the floor`() {
        var state = Sm2Scheduler.initial(1, now)
        repeat(20) {
            state = Sm2Scheduler.next(state, ReviewGrade.Again, now)
        }
        assertEquals(Sm2Scheduler.MIN_EASE, state.easeFactor, 0.0001)
    }

    @Test
    fun `next morning respects the zone`() {
        val tokyo = ZoneId.of("Asia/Tokyo")
        val due = Sm2Scheduler.nextMorning(now, tokyo)
        assertEquals(
            Instant.parse("2026-11-19T08:00:00+09:00").toEpochMilli(),
            due,
        )
    }
}
