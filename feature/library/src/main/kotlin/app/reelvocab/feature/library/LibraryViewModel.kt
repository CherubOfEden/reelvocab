package app.reelvocab.feature.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.reelvocab.core.domain.model.MediaVaultRepository
import app.reelvocab.core.domain.model.VaultItem
import app.reelvocab.core.domain.model.VaultKind
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class LibraryUiState(
    val items: List<VaultItem> = emptyList(),
    val loading: Boolean = true,
    val category: VaultKind? = null,
)

@HiltViewModel
class LibraryViewModel @Inject constructor(
    private val vault: MediaVaultRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(LibraryUiState())
    val state: StateFlow<LibraryUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            try {
                vault.observeAll().collect { items ->
                    _state.update { it.copy(items = items, loading = false) }
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                _state.update { it.copy(loading = false) }
            }
        }
    }

    fun setCategory(kind: VaultKind?) {
        _state.update { it.copy(category = kind) }
    }

    fun delete(item: VaultItem) {
        viewModelScope.launch {
            try {
                vault.delete(item.id)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                // Keep the row visible if deletion fails.
            }
        }
    }
}
