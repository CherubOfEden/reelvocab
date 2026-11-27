package app.reelvocab.core.data.enrichment

import app.reelvocab.core.common.DEFAULT_SERVER_URL
import app.reelvocab.core.domain.model.TranslationResult
import app.reelvocab.core.domain.model.TranslationService
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CloudTranslationService @Inject constructor(
    private val cloud: CloudEnrichmentClient,
) : TranslationService {

    override suspend fun translate(text: String): TranslationResult? {
        val term = text.trim()
        if (term.isEmpty()) return null
        val result = cloud.translatePhrase(term, DEFAULT_SERVER_URL) ?: return null
        return TranslationResult(
            translation = result.translation,
            sourceLanguage = result.sourceLanguage,
        )
    }
}
