package app.reelvocab.feature.dictionary

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.reelvocab.core.designsystem.component.DictionaryDetailModel
import app.reelvocab.core.designsystem.component.DictionarySenseModel
import app.reelvocab.core.domain.model.DictionaryEntry
import app.reelvocab.core.domain.model.DictionaryRepository
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
)

@HiltViewModel
class DictionaryViewModel @Inject constructor(
    private val dictionary: DictionaryRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(DictionaryUiState())
    val state: StateFlow<DictionaryUiState> = _state.asStateFlow()

    private var searchJob: Job? = null
    private var detailJob: Job? = null

    fun onQueryChange(value: String) {
        searchJob?.cancel()
        detailJob?.cancel()
        _state.update {
            it.copy(
                query = value,
                searching = false,
                searched = false,
                error = false,
                detail = null,
                detailLoading = false,
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
        _state.update {
            it.copy(
                detail = entry.toDetailModel(),
                detailLoading = false,
            )
        }
    }

    fun closeDetail() {
        detailJob?.cancel()
        _state.update { it.copy(detail = null, detailLoading = false) }
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
