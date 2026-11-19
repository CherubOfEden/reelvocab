package app.reelvocab.core.domain.model

import kotlinx.coroutines.flow.Flow

data class Enrichment(
    val headword: String,
    val ipa: String?,
    val wordClass: WordClass?,
    val difficulty: Int?,
    val definitions: List<DictionarySense>,
    val thesaurus: Thesaurus,
    val source: EnrichmentSource,
    val isKnown: Boolean,
)

enum class EnrichmentSource { Dictionary, AiContext }

data class TranscriptionRequest(
    val audioPath: String,
    val filmTitle: String?,
)

data class Transcription(
    val text: String,
    val language: String = "en",
)

interface EnrichmentService {
    suspend fun enrich(headword: String, contextLine: String? = null): Enrichment
}

interface TranscriptionService {
    suspend fun transcribe(request: TranscriptionRequest): Transcription
}

interface CardRepository {
    fun observeCards(): Flow<List<Card>>
    fun observeCount(): Flow<Int>
    suspend fun addCard(card: NewCard): Long
    suspend fun deleteCard(id: Long)
    suspend fun updateTags(cardId: Long, tags: List<String>)
    suspend fun setFavorite(cardId: Long, favorite: Boolean)
}
