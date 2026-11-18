package app.reelvocab.feature.media

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.reelvocab.core.domain.model.MediaItem
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class MediaUiState(
    val loading: Boolean = false,
    val error: Boolean = false,
    val items: List<MediaItem> = emptyList(),
    val scanned: Boolean = false,
)

@HiltViewModel
class MediaViewModel @Inject constructor(
    private val scanner: MediaScanner,
) : ViewModel() {

    private val _state = MutableStateFlow(MediaUiState())
    val state: StateFlow<MediaUiState> = _state.asStateFlow()

    fun scan(force: Boolean = false) {
        val current = _state.value
        if (current.loading || (current.scanned && !force)) return
        viewModelScope.launch {
            _state.update { it.copy(loading = true, error = false) }
            try {
                val items = scanner.scan()
                _state.update {
                    it.copy(loading = false, error = false, items = items, scanned = true)
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                _state.update { it.copy(loading = false, error = true, scanned = true) }
            }
        }
    }
}
