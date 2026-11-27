package app.reelvocab.core.domain.model

data class TranslationResult(
    val translation: String,
    val sourceLanguage: String? = null,
)

interface TranslationService {
    suspend fun translate(text: String): TranslationResult?
}
