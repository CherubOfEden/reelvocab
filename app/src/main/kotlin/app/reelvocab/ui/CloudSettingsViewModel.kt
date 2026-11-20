package app.reelvocab.ui

import androidx.lifecycle.ViewModel
import app.reelvocab.core.data.enrichment.CloudConfigStore
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class CloudSettingsState(
    val enabled: Boolean,
    val serverUrl: String,
)

@HiltViewModel
class CloudSettingsViewModel @Inject constructor(
    private val store: CloudConfigStore,
) : ViewModel() {

    private val _state = MutableStateFlow(
        CloudSettingsState(
            enabled = store.enabled,
            serverUrl = store.serverUrl,
        ),
    )
    val state: StateFlow<CloudSettingsState> = _state.asStateFlow()

    fun save(state: CloudSettingsState) {
        store.enabled = state.enabled
        store.serverUrl = state.serverUrl
        _state.value = state
    }
}
