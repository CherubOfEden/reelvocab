package app.reelvocab.feature.deck

import app.reelvocab.core.domain.model.Card
import app.reelvocab.core.domain.model.WordClass
import java.time.Instant
import java.time.ZoneOffset
import org.junit.Assert.assertEquals
import org.junit.Test

class DeckFilteringTest {

    private val grit = card(
        id = 1,
        headword = "grit",
        source = "True Grit",
        tags = listOf("courage"),
        line = "You gotta have grit.",
    )
    private val ledger = card(
        id = 2,
        headword = "ledger",
        source = "The Dark Knight",
        tags = listOf("finance", "idiom"),
        line = null,
    )

    private val cards = listOf(grit, ledger)

    @Test
    fun `matches headword tags and film title`() {
        assertEquals(listOf(grit), filterDeckCards(cards, DeckFilter.All, emptySet(), "gri"))
        assertEquals(listOf(ledger), filterDeckCards(cards, DeckFilter.All, emptySet(), "finance"))
        assertEquals(listOf(ledger), filterDeckCards(cards, DeckFilter.All, emptySet(), "knight"))
    }

    @Test
    fun `due filter uses scheduler ids`() {
        assertEquals(
            listOf(ledger),
            filterDeckCards(cards, DeckFilter.Due, setOf(2L), ""),
        )
    }

    @Test
    fun `word and sentence filters split on headword length`() {
        val phrase = card(
            id = 3,
            headword = "break a leg",
            source = "Theatre",
            tags = emptyList(),
            line = null,
        )
        val sample = listOf(grit, ledger, phrase)
        assertEquals(
            listOf(grit, ledger),
            filterDeckCards(sample, DeckFilter.Words, emptySet(), ""),
        )
        assertEquals(
            listOf(phrase),
            filterDeckCards(sample, DeckFilter.Sentences, emptySet(), ""),
        )
    }

    @Test
    fun `due window includes everything scheduled through the end of today`() {
        val now = Instant.parse("2026-11-19T10:15:00Z").toEpochMilli()
        val expected = Instant.parse("2026-11-20T00:00:00Z").toEpochMilli() - 1
        assertEquals(expected, dueThroughToday(now, ZoneOffset.UTC))
    }

    @Test
    fun `favorites filter keeps only starred cards`() {
        assertEquals(emptyList<Card>(), filterDeckCards(cards, DeckFilter.Favorites, emptySet(), ""))
        val starred = listOf(grit.copy(isFavorite = true), ledger)
        assertEquals(
            listOf(starred.first()),
            filterDeckCards(starred, DeckFilter.Favorites, emptySet(), ""),
        )
    }

    @Test
    fun `search combines with the active filter`() {
        assertEquals(
            emptyList<Card>(),
            filterDeckCards(cards, DeckFilter.Due, setOf(1L), "finance"),
        )
    }

    private fun card(
        id: Long,
        headword: String,
        source: String,
        tags: List<String>,
        line: String?,
    ) = Card(
        id = id,
        headword = headword,
        ipa = null,
        meaning = "Meaning of $headword",
        wordClass = WordClass.Noun,
        contextLine = line,
        sourceTitle = source,
        sourceTimestampMs = null,
        hasAudio = false,
        createdAtEpochMs = 0,
        tags = tags,
    )
}
