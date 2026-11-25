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
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class MultiSelectionWord(
    val word: CaptureWord,
    val enrichment: Enrichment?,
)

data class MultiSelection(
    val words: List<MultiSelectionWord>,
    val phrase: String,
    val phraseEnrichment: Enrichment?,
    val line: String,
)

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
    val multiSelection: MultiSelection? = null,
    val multiLoading: Boolean = false,
    val multiSaving: Boolean = false,
    val multiError: Boolean = false,
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
    private var selectionJob: Job? = null
    private var previewToken = 0

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
                    multiSelection = null,
                    multiLoading = false,
                    multiSaving = false,
                    multiError = false,
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
        previewToken += 1
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
        previewToken += 1
        _state.update { it.copy(selected = null, preview = null, previewLoading = false) }
    }

    fun clear() {
        previewJob?.cancel()
        selectionJob?.cancel()
        previewJob = null
        selectionJob = null
        previewToken += 1
        _state.value = CaptureUiState()
    }

    fun previewEdited(text: String, line: String) {
        previewJob?.cancel()
        val token = ++previewToken
        val query = text.trim()
        if (query.isEmpty()) {
            _state.update { it.copy(preview = null, previewLoading = false) }
            return
        }
        _state.update { it.copy(preview = null, previewLoading = true, saveError = false) }
        previewJob = viewModelScope.launch {
            delay(PREVIEW_DEBOUNCE_MS)
            if (token != previewToken) return@launch
            val preview = try {
                enrichment.enrich(query, line)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                null
            }
            if (token != previewToken) return@launch
            _state.update { current ->
                if (current.selected != null) {
                    current.copy(preview = preview, previewLoading = false)
                } else {
                    current
                }
            }
        }
    }

    fun prepareSelection(words: List<CaptureWord>) {
        if (words.isEmpty()) return
        selectionJob?.cancel()
        _state.update {
            it.copy(
                multiSelection = null,
                multiLoading = true,
                multiError = false,
                selected = null,
                preview = null,
            )
        }
        selectionJob = viewModelScope.launch {
            try {
                val line = words.first().line
                val enriched = words.map { word ->
                    MultiSelectionWord(
                        word = word,
                        enrichment = enrichSafely(word.text, line),
                    )
                }
                val hasFullLine = words.any {
                    it.text.trim().equals(it.line.trim(), ignoreCase = true)
                }
                val phrase = if (!hasFullLine && words.size > 1) {
                    words.joinToString(" ") { it.text }
                } else {
                    ""
                }
                val phraseEnrichment = if (phrase.isNotEmpty()) {
                    enrichSafely(phrase, line)
                } else {
                    null
                }
                _state.update {
                    it.copy(
                        multiSelection = MultiSelection(
                            words = enriched,
                            phrase = phrase,
                            phraseEnrichment = phraseEnrichment,
                            line = line,
                        ),
                        multiLoading = false,
                    )
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                _state.update { it.copy(multiLoading = false, multiError = true) }
            }
        }
    }

    fun dismissMultiSelection() {
        selectionJob?.cancel()
        _state.update {
            it.copy(multiSelection = null, multiLoading = false, multiError = false)
        }
    }

    fun saveSelection(
        selection: MultiSelection,
        sourceTitle: String,
        timestampMs: Long,
        editedDefinitions: List<String?>,
        onSaved: () -> Unit,
    ) {
        if (_state.value.multiSaving) return
        viewModelScope.launch {
            _state.update { it.copy(multiSaving = true, multiError = false) }
            try {
                selection.words.forEachIndexed { index, selected ->
                    val word = selected.word
                    val meaning = editedDefinitions
                        .getOrNull(index)
                        ?.trim()
                        ?.takeIf { it.isNotEmpty() }
                        ?: selected.enrichment
                            ?.definitions
                            ?.firstOrNull()
                            ?.definition
                            ?.takeIf { it.isNotBlank() }
                        ?: word.line
                    cards.addCard(
                        NewCard(
                            headword = word.text,
                            meaning = meaning,
                            example = selected.enrichment?.definitions
                                ?.firstNotNullOfOrNull { sense ->
                                    sense.example?.trim()?.takeIf { value ->
                                        value.isNotEmpty() && !value.equals(word.line, ignoreCase = true)
                                    }
                                },
                            ipa = selected.enrichment?.ipa,
                            wordClass = selected.enrichment?.wordClass,
                            contextLine = word.line,
                            sourceTitle = sourceTitle,
                            sourceTimestampMs = timestampMs,
                        ),
                    )
                }
                if (selection.phrase.isNotBlank()) {
                    val phraseMeaning = editedDefinitions
                        .getOrNull(selection.words.size)
                        ?.trim()
                        ?.takeIf { it.isNotEmpty() }
                        ?: selection.phraseEnrichment
                            ?.definitions
                            ?.firstOrNull()
                            ?.definition
                            ?.takeIf { it.isNotBlank() }
                        ?: selection.line
                    cards.addCard(
                        NewCard(
                            headword = selection.phrase,
                            meaning = phraseMeaning,
                            example = selection.phraseEnrichment?.definitions
                                ?.firstNotNullOfOrNull { sense ->
                                    sense.example?.trim()?.takeIf { value ->
                                        value.isNotEmpty() &&
                                            !value.equals(selection.line, ignoreCase = true)
                                    }
                                },
                            ipa = selection.phraseEnrichment?.ipa,
                            wordClass = selection.phraseEnrichment?.wordClass,
                            contextLine = selection.line,
                            sourceTitle = sourceTitle,
                            sourceTimestampMs = timestampMs,
                        ),
                    )
                }
                _state.update {
                    it.copy(
                        multiSaving = false,
                        multiSelection = null,
                        selected = null,
                        preview = null,
                        saved = true,
                    )
                }
                onSaved()
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                _state.update { it.copy(multiSaving = false, multiError = true) }
            }
        }
    }

    private companion object {
        const val PREVIEW_DEBOUNCE_MS = 500L
    }

    private suspend fun enrichSafely(text: String, line: String): Enrichment? = try {
        enrichment.enrich(text, line)
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (error: Exception) {
        null
    }

    fun save(
        word: CaptureWord,
        editedText: String,
        editedDefinition: String?,
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
            val meaning = editedDefinition
                ?.trim()
                ?.takeIf { it.isNotEmpty() }
                ?: enriched
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
                        example = enriched?.definitions
                            ?.firstNotNullOfOrNull { sense ->
                                sense.example?.trim()?.takeIf { value ->
                                    value.isNotEmpty() && !value.equals(word.line, ignoreCase = true)
                                }
                            },
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
