package app.reelvocab.feature.capture

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.reelvocab.core.domain.model.CardRepository
import app.reelvocab.core.domain.model.Enrichment
import app.reelvocab.core.domain.model.EnrichmentService
import app.reelvocab.core.domain.model.NewCard
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
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
    val preview: Enrichment? = null,
    val previewLoading: Boolean = false,
    val saving: Boolean = false,
    val saved: Boolean = false,
    val saveError: Boolean = false,
)

@HiltViewModel
class CaptureViewModel @Inject constructor(
    private val ocr: OcrEngine,
    private val enrichment: EnrichmentService,
    private val cards: CardRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(CaptureUiState())
    val state: StateFlow<CaptureUiState> = _state.asStateFlow()

    private var previewJob: Job? = null

    fun recognize(bitmap: Bitmap) {
        previewJob?.cancel()
        viewModelScope.launch {
            _state.update {
                it.copy(
                    loading = true,
                    error = false,
                    lines = emptyList(),
                    selected = null,
                    preview = null,
                    previewLoading = false,
                    saveError = false,
                )
            }
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
        previewJob?.cancel()
        _state.update {
            it.copy(
                selected = word,
                preview = null,
                previewLoading = true,
                saveError = false,
            )
        }
        previewJob = viewModelScope.launch {
            val preview = try {
                enrichment.enrich(word.text, word.line)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                null
            }
            _state.update { current ->
                if (current.selected == word) {
                    current.copy(preview = preview, previewLoading = false)
                } else {
                    current
                }
            }
        }
    }

    fun dismissSelection() {
        previewJob?.cancel()
        _state.update { it.copy(selected = null, preview = null, previewLoading = false) }
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
            _state.update { it.copy(saving = true, saveError = false) }
            val headword = editedText.trim().ifEmpty { word.text }
            val cached = _state.value.preview
            val enriched = if (cached != null && cached.headword.equals(headword, ignoreCase = true)) {
                cached
            } else {
                try {
                    enrichment.enrich(headword, word.line)
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (error: Exception) {
                    null
                }
            }
            val meaning = enriched
                ?.definitions
                ?.firstOrNull()
                ?.definition
                ?.takeIf { it.isNotBlank() }
                ?: word.line
            try {
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
                previewJob?.cancel()
                _state.update {
                    it.copy(
                        saving = false,
                        selected = null,
                        preview = null,
                        previewLoading = false,
                        saved = true,
                    )
                }
                onSaved()
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                _state.update { it.copy(saving = false, saveError = true) }
            }
        }
    }
}
