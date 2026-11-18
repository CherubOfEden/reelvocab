package app.reelvocab.feature.today

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.reelvocab.core.domain.model.CardRepository
import app.reelvocab.core.domain.model.ReviewRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

@HiltViewModel
class TodayViewModel @Inject constructor(
    private val cards: CardRepository,
    private val reviews: ReviewRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(TodayUiState())
    val state: StateFlow<TodayUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            try {
                cards.observeCount().collect { count ->
                    _state.update { it.copy(cardCount = count, loading = false) }
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

    fun refresh() {
        viewModelScope.launch { refreshDueCount() }
    }

    private suspend fun refreshDueCount() {
        try {
            val due = reviews.observeDueCount(System.currentTimeMillis()).first()
            _state.update { it.copy(dueCount = due) }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Exception) {
            _state.update { it.copy(dueCount = 0) }
        }
    }

    private companion object {
        const val DUE_REFRESH_MS = 30_000L
    }
}
