package app.reelvocab.core.data.enrichment

import app.reelvocab.core.domain.model.DictionarySense
import app.reelvocab.core.domain.model.DictionaryRepository
import app.reelvocab.core.domain.model.Enrichment
import app.reelvocab.core.domain.model.EnrichmentService
import app.reelvocab.core.domain.model.EnrichmentSource
import app.reelvocab.core.domain.model.Thesaurus
import javax.inject.Inject
import javax.inject.Singleton

/**
 * M2 stand-in for the on-device enrichment pipeline. Dictionary entries win;
 * unknown words come back flagged as [EnrichmentSource.AiContext] so the UI can
 * offer a manual definition once the editor lands in M5.
 */
@Singleton
class FakeEnrichmentService @Inject constructor(
    private val dictionary: DictionaryRepository,
) : EnrichmentService {

    override suspend fun enrich(headword: String, contextLine: String?): Enrichment {
        val entry = dictionary.lookup(headword)
        if (entry != null) {
            return Enrichment(
                headword = entry.lemma,
                ipa = entry.ipa,
                wordClass = entry.wordClass,
                difficulty = entry.difficulty,
                definitions = entry.senses,
                thesaurus = entry.thesaurus,
                source = EnrichmentSource.Dictionary,
                isKnown = true,
            )
        }

        val trimmed = headword.trim()
        val line = contextLine?.trim()?.takeIf { it.isNotEmpty() }
        return Enrichment(
            headword = trimmed,
            ipa = null,
            wordClass = null,
            difficulty = null,
            definitions = listOf(
                DictionarySense(
                    definition = "Not in the offline dictionary yet.",
                    example = line,
                ),
            ),
            thesaurus = Thesaurus(),
            source = EnrichmentSource.AiContext,
            isKnown = false,
        )
    }
}
