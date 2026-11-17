package app.reelvocab.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.reelvocab.core.domain.model.DictionaryEntry
import app.reelvocab.core.domain.model.DictionaryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class DictionaryProbeUiState(
    val query: String = "grit",
    val loading: Boolean = false,
    val error: Boolean = false,
    val searched: Boolean = false,
    val exact: DictionaryEntry? = null,
    val results: List<DictionaryEntry> = emptyList(),
)

@HiltViewModel
class DictionaryProbeViewModel @Inject constructor(
    private val dictionary: DictionaryRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(DictionaryProbeUiState())
    val state: StateFlow<DictionaryProbeUiState> = _state.asStateFlow()

    private var job: Job? = null

    fun onQueryChange(query: String) {
        _state.update { it.copy(query = query) }
    }

    fun search() {
        val query = _state.value.query.trim()
        if (query.isEmpty()) return
        job?.cancel()
        job = viewModelScope.launch {
            _state.update { it.copy(loading = true, error = false, searched = true) }
            try {
                val exact = dictionary.lookup(query)
                val results = dictionary.search(query, limit = 12)
                _state.update {
                    it.copy(
                        loading = false,
                        exact = exact,
                        results = results.filterNot { entry ->
                            entry.lemma == exact?.lemma && entry.wordClass == exact.wordClass
                        },
                    )
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                _state.update { it.copy(loading = false, error = true) }
            }
        }
    }
}
