package app.reelvocab.feature.deck

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.reelvocab.core.common.Tags
import app.reelvocab.core.designsystem.component.DictionaryDetailModel
import app.reelvocab.core.designsystem.component.DictionarySenseModel
import app.reelvocab.core.designsystem.component.TagEditorModel
import app.reelvocab.core.domain.model.Card
import app.reelvocab.core.domain.model.CardRepository
import app.reelvocab.core.domain.model.DictionaryEntry
import app.reelvocab.core.domain.model.DictionaryRepository
import app.reelvocab.core.domain.model.ReviewRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

data class DeckUiState(
    val filter: DeckFilter = DeckFilter.All,
    val query: String = "",
    val cards: List<Card> = emptyList(),
    val dueCardIds: Set<Long> = emptySet(),
    val loading: Boolean = true,
    val detail: DictionaryDetailModel? = null,
    val detailLoading: Boolean = false,
    val tagEditor: TagEditorModel? = null,
    val tagEditorCardId: Long? = null,
)

@HiltViewModel
class DeckViewModel @Inject constructor(
    private val cards: CardRepository,
    private val reviews: ReviewRepository,
    private val dictionary: DictionaryRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(DeckUiState())
    val state: StateFlow<DeckUiState> = _state.asStateFlow()

    private var detailJob: Job? = null
    private var appliedInitialFilter: DeckFilter? = null

    init {
        viewModelScope.launch {
            try {
                cards.observeCards().collect { list ->
                    _state.update { it.copy(cards = list, loading = false) }
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                _state.update { it.copy(loading = false) }
            }
        }
        viewModelScope.launch {
            while (isActive) {
                refreshDueCount()
                delay(DUE_REFRESH_MS)
            }
        }
    }

    fun refreshDue() {
        viewModelScope.launch { refreshDueCount() }
    }

    private suspend fun refreshDueCount() {
        try {
            val due = reviews.observeDueCardIds(dueThroughToday(System.currentTimeMillis())).first()
            _state.update { it.copy(dueCardIds = due) }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Exception) {
            _state.update { it.copy(dueCardIds = emptySet()) }
        }
    }

    fun applyInitialFilter(filter: DeckFilter) {
        if (appliedInitialFilter != null) return
        appliedInitialFilter = filter
        setFilter(filter)
    }

    fun setFilter(filter: DeckFilter) {
        _state.update { it.copy(filter = filter) }
    }

    fun onQueryChange(query: String) {
        _state.update { it.copy(query = query) }
    }

    fun openDetail(card: Card) {
        detailJob?.cancel()
        _state.update {
            it.copy(
                detail = DictionaryDetailModel(
                    headword = card.headword,
                    fallbackMeaning = card.meaning,
                    isKnown = false,
                ),
                detailLoading = true,
            )
        }
        detailJob = viewModelScope.launch {
            val entry = try {
                dictionary.lookup(card.headword)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                null
            }
            _state.update { current ->
                if (current.detail?.headword != card.headword) {
                    current
                } else {
                    current.copy(
                        detail = entry.toDetailModel(card.headword, card.meaning),
                        detailLoading = false,
                    )
                }
            }
        }
    }

    fun closeDetail() {
        detailJob?.cancel()
        _state.update { it.copy(detail = null, detailLoading = false) }
    }

    fun openTagEditor(card: Card) {
        _state.update {
            it.copy(
                tagEditor = TagEditorModel(headword = card.headword, tags = card.tags),
                tagEditorCardId = card.id,
            )
        }
    }

    fun closeTagEditor() {
        _state.update { it.copy(tagEditor = null, tagEditorCardId = null) }
    }

    fun onTagDraftChange(draft: String) {
        _state.update { it.copy(tagEditor = it.tagEditor?.copy(draft = draft)) }
    }

    fun addTag() {
        _state.update { state ->
            val editor = state.tagEditor ?: return@update state
            state.copy(
                tagEditor = editor.copy(
                    tags = Tags.withAdded(editor.tags, editor.draft),
                    draft = "",
                ),
            )
        }
    }

    fun removeTag(tag: String) {
        _state.update { state ->
            val editor = state.tagEditor ?: return@update state
            state.copy(tagEditor = editor.copy(tags = Tags.without(editor.tags, tag)))
        }
    }

    fun toggleFavorite(card: Card) {
        val next = !card.isFavorite
        _state.update { state ->
            state.copy(
                cards = state.cards.map {
                    if (it.id == card.id) it.copy(isFavorite = next) else it
                },
            )
        }
        viewModelScope.launch {
            try {
                cards.setFavorite(card.id, next)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                _state.update { state ->
                    state.copy(
                        cards = state.cards.map {
                            if (it.id == card.id) it.copy(isFavorite = card.isFavorite) else it
                        },
                    )
                }
            }
        }
    }

    fun deleteCard(cardId: Long) {
        viewModelScope.launch {
            try {
                cards.deleteCard(cardId)
                _state.update { state ->
                    state.copy(
                        cards = state.cards.filterNot { it.id == cardId },
                        tagEditor = state.tagEditor.takeUnless { state.tagEditorCardId == cardId },
                        tagEditorCardId = state.tagEditorCardId.takeUnless { it == cardId },
                    )
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                // The card stays visible if the delete fails; the user can retry.
            }
        }
    }

    fun saveTags() {
        val cardId = _state.value.tagEditorCardId ?: return
        val editor = _state.value.tagEditor ?: return
        viewModelScope.launch {
            try {
                cards.updateTags(cardId, editor.tags)
                _state.update { state ->
                    state.copy(
                        cards = state.cards.map { card ->
                            if (card.id == cardId) card.copy(tags = editor.tags) else card
                        },
                        tagEditor = null,
                        tagEditorCardId = null,
                    )
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                // Keep the sheet open so the edit is not silently lost.
            }
        }
    }

    private companion object {
        const val DUE_REFRESH_MS = 60_000L
    }
}

internal fun DictionaryEntry?.toDetailModel(
    headword: String,
    fallbackMeaning: String?,
): DictionaryDetailModel {
    val entry = this
    if (entry == null) {
        return DictionaryDetailModel(
            headword = headword,
            fallbackMeaning = fallbackMeaning,
            isKnown = false,
        )
    }
    return DictionaryDetailModel(
        headword = entry.lemma,
        ipa = entry.ipa,
        partOfSpeech = entry.wordClass?.storageValue,
        difficulty = entry.difficulty,
        senses = entry.senses.map { sense ->
            DictionarySenseModel(definition = sense.definition, example = sense.example)
        },
        synonyms = entry.thesaurus.synonyms,
        antonyms = entry.thesaurus.antonyms,
        fallbackMeaning = fallbackMeaning,
        fallbackWarning = entry.fallbackWarning,
        isKnown = true,
    )
}
