package app.reelvocab.feature.today

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.reelvocab.core.domain.model.ReviewCard
import app.reelvocab.core.domain.model.ReviewGrade
import app.reelvocab.core.domain.model.ReviewRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ReviewUiState(
    val loading: Boolean = true,
    val queue: List<ReviewCard> = emptyList(),
    val position: Int = 0,
    val reviewed: Int = 0,
) {
    val current: ReviewCard? get() = queue.getOrNull(position)
    val total: Int get() = queue.size
    val finished: Boolean get() = !loading && position >= queue.size
    val empty: Boolean get() = !loading && queue.isEmpty()
}

@HiltViewModel
class ReviewViewModel @Inject constructor(
    private val reviews: ReviewRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(ReviewUiState())
    val state: StateFlow<ReviewUiState> = _state.asStateFlow()

    private var started = false
    private var grading = false

    fun start() {
        if (started) return
        started = true
        viewModelScope.launch {
            try {
                val today = reviews.dueCards(System.currentTimeMillis())
                _state.update {
                    it.copy(loading = false, queue = today, position = 0, reviewed = 0)
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                _state.update { it.copy(loading = false) }
            }
        }
    }

    fun grade(grade: ReviewGrade) {
        if (grading) return
        val current = _state.value.current ?: return
        grading = true
        viewModelScope.launch {
            try {
                val next = reviews.grade(current.card.id, grade, System.currentTimeMillis())
                _state.update { state ->
                    val queue = if (grade == ReviewGrade.Again && next != null) {
                        state.queue + current.copy(state = next)
                    } else {
                        state.queue
                    }
                    state.copy(
                        queue = queue,
                        position = state.position + 1,
                        reviewed = state.reviewed + 1,
                    )
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                _state.update { it.copy(position = it.position + 1) }
            } finally {
                grading = false
            }
        }
    }
}
