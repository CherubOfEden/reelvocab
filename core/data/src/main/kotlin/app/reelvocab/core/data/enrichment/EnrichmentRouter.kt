package app.reelvocab.core.data.enrichment

import app.reelvocab.core.domain.model.DictionarySense
import app.reelvocab.core.domain.model.Enrichment
import app.reelvocab.core.domain.model.EnrichmentService
import app.reelvocab.core.domain.model.EnrichmentSource
import app.reelvocab.core.domain.model.Thesaurus
import app.reelvocab.core.domain.model.DictionaryRepository
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Dictionary first. When the learner opts into a cloud endpoint, unknown words
 * get a context definition from it; any failure silently falls back to the
 * offline path so capture never blocks on the network.
 */
@Singleton
class EnrichmentRouter @Inject constructor(
    private val dictionary: DictionaryRepository,
    private val fallback: FakeEnrichmentService,
    private val cloud: CloudEnrichmentClient,
) : EnrichmentService {

    override suspend fun enrich(headword: String, contextLine: String?): Enrichment {
        val offline = fallback.enrich(headword, contextLine)
        if (offline.isKnown) return offline

        val cloudText = cloud.define(offline.headword, contextLine) ?: return offline
        val entry = dictionary.lookup(offline.headword)
        return Enrichment(
            headword = entry?.lemma ?: offline.headword,
            ipa = entry?.ipa ?: offline.ipa,
            wordClass = entry?.wordClass,
            difficulty = entry?.difficulty,
            definitions = listOf(DictionarySense(definition = cloudText, example = contextLine)),
            thesaurus = entry?.thesaurus ?: Thesaurus(),
            source = EnrichmentSource.AiContext,
            isKnown = true,
        )
    }
}
