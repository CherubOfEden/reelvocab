package app.reelvocab.feature.media

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.reelvocab.core.domain.model.MediaItem
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class MediaCategory(val labelRes: Int) {
    All(R.string.media_section_all),
    Videos(R.string.media_section_videos),
    Images(R.string.media_section_images),
    Audio(R.string.media_section_audio),
}

data class MediaUiState(
    val loading: Boolean = false,
    val error: Boolean = false,
    val items: List<MediaItem> = emptyList(),
    val scanned: Boolean = false,
    val category: MediaCategory = MediaCategory.All,
)

@HiltViewModel
class MediaViewModel @Inject constructor(
    private val scanner: MediaScanner,
) : ViewModel() {

    private val _state = MutableStateFlow(MediaUiState())
    val state: StateFlow<MediaUiState> = _state.asStateFlow()

    private var lastAccess: MediaAccess? = null
    private var scanJob: Job? = null

    fun setCategory(category: MediaCategory) {
        _state.update { it.copy(category = category) }
    }

    fun scan(access: MediaAccess, force: Boolean = false) {
        if (!access.hasAny) {
            scanJob?.cancel()
            scanJob = null
            lastAccess = access
            _state.update {
                it.copy(loading = false, error = false, items = emptyList(), scanned = false)
            }
            return
        }
        val current = _state.value
        if (current.loading && lastAccess == access) return
        if (!force && current.scanned && lastAccess == access) return
        lastAccess = access
        scanJob?.cancel()
        scanJob = viewModelScope.launch {
            _state.update { it.copy(loading = true, error = false) }
            try {
                val result = scanner.scan(access)
                _state.update {
                    it.copy(
                        loading = false,
                        error = result.items.isEmpty() && result.failedKinds.isNotEmpty(),
                        items = result.items,
                        scanned = true,
                    )
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                _state.update { it.copy(loading = false, error = true, scanned = true) }
            }
        }
    }
}
