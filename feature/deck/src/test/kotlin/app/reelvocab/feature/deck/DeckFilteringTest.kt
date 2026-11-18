package app.reelvocab.feature.deck

import app.reelvocab.core.domain.model.Card
import app.reelvocab.core.domain.model.WordClass
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
    fun `sentence and word filters split on context`() {
        assertEquals(listOf(grit), filterDeckCards(cards, DeckFilter.Sentences, emptySet(), ""))
        assertEquals(listOf(ledger), filterDeckCards(cards, DeckFilter.Words, emptySet(), ""))
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
