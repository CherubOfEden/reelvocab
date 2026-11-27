package app.reelvocab.feature.today

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.reelvocab.core.common.Tags
import app.reelvocab.core.designsystem.component.DictionaryDetailModel
import app.reelvocab.core.designsystem.component.DictionarySenseModel
import app.reelvocab.core.designsystem.component.TagEditorModel
import app.reelvocab.core.domain.model.CardRepository
import app.reelvocab.core.domain.model.DictionaryEntry
import app.reelvocab.core.domain.model.DictionaryRepository
import app.reelvocab.core.domain.model.ReviewCard
import app.reelvocab.core.domain.model.ReviewGrade
import app.reelvocab.core.domain.model.ReviewRepository
import app.reelvocab.core.domain.model.TranslationService
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
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
    val detail: DictionaryDetailModel? = null,
    val detailLoading: Boolean = false,
    val tagEditor: TagEditorModel? = null,
    val tagEditorCardId: Long? = null,
) {
    val current: ReviewCard? get() = queue.getOrNull(position)
    val total: Int get() = queue.size
    val finished: Boolean get() = !loading && position >= queue.size
    val empty: Boolean get() = !loading && queue.isEmpty()
}

@HiltViewModel
class ReviewViewModel @Inject constructor(
    private val reviews: ReviewRepository,
    private val cards: CardRepository,
    private val dictionary: DictionaryRepository,
    private val translation: TranslationService,
) : ViewModel() {

    private val _state = MutableStateFlow(ReviewUiState())
    val state: StateFlow<ReviewUiState> = _state.asStateFlow()

    private var started = false
    private var grading = false
    private var detailJob: Job? = null
    private var translationJob: Job? = null

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
                        detail = null,
                        detailLoading = false,
                        tagEditor = null,
                        tagEditorCardId = null,
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

    fun openDetail() {
        val card = _state.value.current?.card ?: return
        detailJob?.cancel()
        translationJob?.cancel()
        _state.update {
            it.copy(
                detail = DictionaryDetailModel(
                    headword = card.headword,
                    fallbackMeaning = card.meaning,
                    isKnown = false,
                ),
                detailLoading = true,
            )
        }
        detailJob = viewModelScope.launch {
            val entry = try {
                dictionary.lookup(card.headword)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                null
            }
            _state.update { current ->
                if (current.detail?.headword != card.headword) {
                    current
                } else {
                    current.copy(
                        detail = entry.toDictionaryDetail(card.headword, card.meaning),
                        detailLoading = false,
                    )
                }
            }
        }
    }

    fun closeDetail() {
        detailJob?.cancel()
        translationJob?.cancel()
        _state.update { it.copy(detail = null, detailLoading = false) }
    }

    fun translateDetail() {
        val detail = _state.value.detail ?: return
        val headword = detail.headword.trim()
        if (headword.isEmpty() || detail.translationLoading) return
        translationJob?.cancel()
        _state.update {
            it.copy(
                detail = it.detail?.copy(
                    translation = null,
                    translationLoading = true,
                    translationError = false,
                ),
            )
        }
        translationJob = viewModelScope.launch {
            val result = try {
                translation.translate(headword)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                null
            }
            _state.update { current ->
                val active = current.detail
                if (active == null || active.headword != detail.headword) {
                    current
                } else {
                    current.copy(
                        detail = active.copy(
                            translation = result?.translation,
                            translationLoading = false,
                            translationError = result == null,
                        ),
                    )
                }
            }
        }
    }

    fun openTagEditor() {
        val card = _state.value.current?.card ?: return
        _state.update {
            it.copy(
                tagEditor = TagEditorModel(headword = card.headword, tags = card.tags),
                tagEditorCardId = card.id,
            )
        }
    }

    fun closeTagEditor() {
        _state.update { it.copy(tagEditor = null, tagEditorCardId = null) }
    }

    fun onTagDraftChange(draft: String) {
        _state.update { it.copy(tagEditor = it.tagEditor?.copy(draft = draft)) }
    }

    fun addTag() {
        _state.update { state ->
            val editor = state.tagEditor ?: return@update state
            state.copy(
                tagEditor = editor.copy(
                    tags = Tags.withAdded(editor.tags, editor.draft),
                    draft = "",
                ),
            )
        }
    }

    fun removeTag(tag: String) {
        _state.update { state ->
            val editor = state.tagEditor ?: return@update state
            state.copy(tagEditor = editor.copy(tags = Tags.without(editor.tags, tag)))
        }
    }

    fun saveTags() {
        val cardId = _state.value.tagEditorCardId ?: return
        val editor = _state.value.tagEditor ?: return
        viewModelScope.launch {
            try {
                cards.updateTags(cardId, editor.tags)
                _state.update { state ->
                    state.copy(
                        queue = state.queue.map { reviewCard ->
                            if (reviewCard.card.id == cardId) {
                                reviewCard.copy(card = reviewCard.card.copy(tags = editor.tags))
                            } else {
                                reviewCard
                            }
                        },
                        tagEditor = null,
                        tagEditorCardId = null,
                    )
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                // Keep the sheet open so the edit is not silently lost.
            }
        }
    }
}

internal fun DictionaryEntry?.toDictionaryDetail(
    headword: String,
    fallbackMeaning: String?,
): DictionaryDetailModel {
    val entry = this
    if (entry == null) {
        return DictionaryDetailModel(
            headword = headword,
            fallbackMeaning = fallbackMeaning,
            isKnown = false,
        )
    }
    return DictionaryDetailModel(
        headword = entry.lemma,
        ipa = entry.ipa,
        partOfSpeech = entry.wordClass?.storageValue,
        difficulty = entry.difficulty,
        senses = entry.senses.map { sense ->
            DictionarySenseModel(definition = sense.definition, example = sense.example)
        },
        synonyms = entry.thesaurus.synonyms,
        antonyms = entry.thesaurus.antonyms,
        fallbackMeaning = fallbackMeaning,
        fallbackWarning = entry.fallbackWarning,
        isKnown = true,
    )
}
