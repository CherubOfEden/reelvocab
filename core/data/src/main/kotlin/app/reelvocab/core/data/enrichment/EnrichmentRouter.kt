package app.reelvocab.core.data.enrichment

import app.reelvocab.core.domain.model.Enrichment
import app.reelvocab.core.domain.model.EnrichmentService
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class EnrichmentRouter @Inject constructor(
    private val fallback: FakeEnrichmentService,
) : EnrichmentService {

    override suspend fun enrich(headword: String, contextLine: String?): Enrichment =
        fallback.enrich(headword, contextLine)
}
