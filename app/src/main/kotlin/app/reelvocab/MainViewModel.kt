package app.reelvocab

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.reelvocab.data.OnboardingStore
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface MainUiState {
    data object Loading : MainUiState
    data object Onboarding : MainUiState
    data object Main : MainUiState
}

@HiltViewModel
class MainViewModel @Inject constructor(
    private val onboardingStore: OnboardingStore,
) : ViewModel() {

    private val completedOverride = MutableStateFlow<Boolean?>(null)

    val state: StateFlow<MainUiState> = combine(
        onboardingStore.completed,
        completedOverride,
    ) { persisted, override ->
        if (override == true || persisted) MainUiState.Main else MainUiState.Onboarding
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = MainUiState.Loading,
    )

    fun completeOnboarding() {
        completedOverride.value = true
        viewModelScope.launch {
            onboardingStore.markCompleted()
        }
    }
}
