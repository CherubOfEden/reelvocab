package app.reelvocab.feature.capture

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.reelvocab.core.domain.model.CardRepository
import app.reelvocab.core.domain.model.EnrichmentService
import app.reelvocab.core.domain.model.NewCard
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class CaptureUiState(
    val loading: Boolean = false,
    val error: Boolean = false,
    val lines: List<CaptureLine> = emptyList(),
    val selected: CaptureWord? = null,
    val saving: Boolean = false,
    val saved: Boolean = false,
)

@HiltViewModel
class CaptureViewModel @Inject constructor(
    private val ocr: OcrEngine,
    private val enrichment: EnrichmentService,
    private val cards: CardRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(CaptureUiState())
    val state: StateFlow<CaptureUiState> = _state.asStateFlow()

    fun recognize(bitmap: Bitmap) {
        viewModelScope.launch {
            _state.update { it.copy(loading = true, error = false, lines = emptyList()) }
            try {
                val result = ocr.recognize(bitmap)
                _state.update { it.copy(loading = false, lines = result.lines) }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                _state.update { it.copy(loading = false, error = true) }
            }
        }
    }

    fun select(word: CaptureWord) {
        _state.update { it.copy(selected = word) }
    }

    fun dismissSelection() {
        _state.update { it.copy(selected = null) }
    }

    fun save(
        word: CaptureWord,
        editedText: String,
        sourceTitle: String,
        timestampMs: Long,
        onSaved: () -> Unit,
    ) {
        if (_state.value.saving) return
        viewModelScope.launch {
            _state.update { it.copy(saving = true) }
            val headword = editedText.trim().ifEmpty { word.text }
            val enriched = runCatching { enrichment.enrich(headword, word.line) }.getOrNull()
            val meaning = enriched
                ?.definitions
                ?.firstOrNull()
                ?.definition
                ?.takeIf { it.isNotBlank() }
                ?: word.line
            cards.addCard(
                NewCard(
                    headword = headword,
                    meaning = meaning,
                    ipa = enriched?.ipa,
                    wordClass = enriched?.wordClass,
                    contextLine = word.line,
                    sourceTitle = sourceTitle,
                    sourceTimestampMs = timestampMs,
                ),
            )
            _state.update { it.copy(saving = false, selected = null, saved = true) }
            onSaved()
        }
    }
}
