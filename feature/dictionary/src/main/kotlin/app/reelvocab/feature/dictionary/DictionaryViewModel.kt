package app.reelvocab.feature.dictionary

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.reelvocab.core.designsystem.component.DictionaryDetailModel
import app.reelvocab.core.designsystem.component.DictionarySenseModel
import app.reelvocab.core.domain.model.CardRepository
import app.reelvocab.core.domain.model.DictionaryEntry
import app.reelvocab.core.domain.model.DictionaryRepository
import app.reelvocab.core.domain.model.DictionarySense
import app.reelvocab.core.domain.model.Enrichment
import app.reelvocab.core.domain.model.EnrichmentService
import app.reelvocab.core.domain.model.NewCard
import app.reelvocab.core.domain.model.Thesaurus
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class DictionaryUiState(
    val query: String = "",
    val results: List<DictionaryEntry> = emptyList(),
    val searching: Boolean = false,
    val searched: Boolean = false,
    val error: Boolean = false,
    val detail: DictionaryDetailModel? = null,
    val detailLoading: Boolean = false,
    val wordOfTheDay: DictionaryEntry? = null,
    val wordOfTheDayLoading: Boolean = false,
    val saving: Boolean = false,
    val saved: Boolean = false,
)

@HiltViewModel
class DictionaryViewModel @Inject constructor(
    private val dictionary: DictionaryRepository,
    private val cards: CardRepository,
    private val enrichment: EnrichmentService,
) : ViewModel() {

    private val _state = MutableStateFlow(DictionaryUiState())
    val state: StateFlow<DictionaryUiState> = _state.asStateFlow()

    private var searchJob: Job? = null
    private var detailJob: Job? = null
    private var detailEntry: DictionaryEntry? = null

    fun loadWordOfTheDay() {
        if (_state.value.wordOfTheDayLoading) return
        viewModelScope.launch {
            _state.update { it.copy(wordOfTheDayLoading = true) }
            val entry = try {
                dictionary.wordOfTheDay()
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                null
            }
            _state.update { it.copy(wordOfTheDay = entry, wordOfTheDayLoading = false) }
        }
    }

    fun onQueryChange(value: String) {
        searchJob?.cancel()
        detailJob?.cancel()
        detailEntry = null
        _state.update {
            it.copy(
                query = value,
                searching = false,
                searched = false,
                error = false,
                detail = null,
                detailLoading = false,
                saving = false,
                saved = false,
            )
        }
        val term = value.trim()
        if (term.length < 2) {
            _state.update { it.copy(results = emptyList()) }
            return
        }
        searchJob = viewModelScope.launch {
            delay(SEARCH_DEBOUNCE_MS)
            _state.update { current ->
                if (current.query.trim() == term) current.copy(searching = true) else current
            }
            try {
                var matches = dictionary.search(term)
                if (matches.isEmpty()) {
                    dictionary.lookup(term)?.let { matches = listOf(it) }
                }
                if (matches.isEmpty() && term.contains(' ')) {
                    phraseFallback(term)?.let { matches = listOf(it) }
                }
                _state.update { current ->
                    if (current.query.trim() == term) {
                        current.copy(
                            results = matches,
                            searching = false,
                            searched = true,
                            error = false,
                        )
                    } else {
                        current
                    }
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                _state.update { current ->
                    if (current.query.trim() == term) {
                        current.copy(
                            results = emptyList(),
                            searching = false,
                            searched = true,
                            error = true,
                        )
                    } else {
                        current
                    }
                }
            }
        }
    }

    fun openDetail(entry: DictionaryEntry) {
        detailJob?.cancel()
        detailEntry = entry
        _state.update {
            it.copy(
                detail = entry.toDetailModel(),
                detailLoading = false,
                saving = false,
                saved = false,
            )
        }
    }

    fun openWord(word: String) {
        detailJob?.cancel()
        detailEntry = null
        _state.update {
            it.copy(
                detail = DictionaryDetailModel(headword = word, isKnown = false),
                detailLoading = true,
                saving = false,
                saved = false,
            )
        }
        detailJob = viewModelScope.launch {
            val entry = try {
                dictionary.lookup(word)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                null
            }
            if (entry != null) detailEntry = entry
            _state.update { current ->
                current.copy(
                    detail = entry?.toDetailModel()
                        ?: DictionaryDetailModel(headword = word, isKnown = false),
                    detailLoading = false,
                )
            }
        }
    }

    fun saveDetail() {
        val detail = _state.value.detail ?: return
        if (_state.value.saving || _state.value.saved) return
        viewModelScope.launch {
            _state.update { it.copy(saving = true) }
            val entry = detailEntry
            val fallback = detail.senses.firstOrNull()?.definition
                ?: detail.fallbackMeaning
                ?: detail.headword
            try {
                val enriched = if (entry != null) {
                    null
                } else {
                    try {
                        enrichment.enrich(detail.headword, null)
                    } catch (cancelled: CancellationException) {
                        throw cancelled
                    } catch (error: Exception) {
                        null
                    }
                }
                cards.addCard(
                    NewCard(
                        headword = entry?.lemma ?: detail.headword,
                        meaning = enriched?.definitions?.firstOrNull()?.definition
                            ?: fallback,
                        ipa = entry?.ipa ?: enriched?.ipa,
                        wordClass = entry?.wordClass ?: enriched?.wordClass,
                        tags = listOf("dictionary"),
                    ),
                )
                _state.update { it.copy(saving = false, saved = true) }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                _state.update { it.copy(saving = false) }
            }
        }
    }

    fun closeDetail() {
        detailJob?.cancel()
        detailEntry = null
        _state.update { it.copy(detail = null, detailLoading = false, saved = false) }
    }

    private suspend fun phraseFallback(phrase: String): DictionaryEntry? {
        val words = phrase.split(' ')
            .map { it.trim() }
            .filter { it.length >= 2 }
        if (words.isEmpty()) return null
        val parts = words.mapNotNull { word ->
            try {
                dictionary.lookup(word)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                null
            }
        }
        if (parts.isEmpty()) return null
        val senses = parts.mapIndexed { index, entry ->
            DictionarySense(
                definition = "${words[index]}: ${entry.senses.firstOrNull()?.definition.orEmpty()}",
            )
        }
        return DictionaryEntry(
            lemma = phrase,
            wordClass = null,
            ipa = null,
            frequencyRank = null,
            difficulty = null,
            senses = senses,
            thesaurus = Thesaurus(),
        )
    }

    private companion object {
        const val SEARCH_DEBOUNCE_MS = 220L
    }
}

private fun DictionaryEntry.toDetailModel(): DictionaryDetailModel = DictionaryDetailModel(
    headword = lemma,
    ipa = ipa,
    partOfSpeech = wordClass?.storageValue,
    difficulty = difficulty,
    senses = senses.map { sense ->
        DictionarySenseModel(definition = sense.definition, example = sense.example)
    },
    synonyms = thesaurus.synonyms,
    antonyms = thesaurus.antonyms,
    fallbackMeaning = null,
    isKnown = true,
)
