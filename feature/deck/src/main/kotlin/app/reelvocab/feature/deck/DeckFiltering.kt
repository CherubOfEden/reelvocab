package app.reelvocab.feature.deck

import app.reelvocab.core.domain.model.Card

internal fun filterDeckCards(
    cards: List<Card>,
    filter: DeckFilter,
    dueCardIds: Set<Long>,
    query: String,
): List<Card> {
    val needle = query.trim()
    return cards.asSequence()
        .filter { card ->
            when (filter) {
                DeckFilter.All -> true
                DeckFilter.Favorites -> card.isFavorite
                DeckFilter.Due -> card.id in dueCardIds
                DeckFilter.Words -> !card.headword.contains(' ')
                DeckFilter.Sentences -> card.headword.contains(' ')
            }
        }
        .filter { card -> needle.isEmpty() || card.matches(needle) }
        .toList()
}

private fun Card.matches(query: String): Boolean =
    headword.contains(query, ignoreCase = true) ||
        sourceTitle?.contains(query, ignoreCase = true) == true ||
        meaning.contains(query, ignoreCase = true) ||
        tags.any { it.contains(query, ignoreCase = true) }
