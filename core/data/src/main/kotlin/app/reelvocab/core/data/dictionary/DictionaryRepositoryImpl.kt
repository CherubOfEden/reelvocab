package app.reelvocab.core.data.dictionary

import app.reelvocab.core.common.DEFAULT_SERVER_URL
import app.reelvocab.core.common.Headwords
import app.reelvocab.core.data.enrichment.CloudEnrichmentClient
import app.reelvocab.core.domain.model.DictionaryAiException
import app.reelvocab.core.domain.model.DictionaryEntry
import app.reelvocab.core.domain.model.DictionaryRepository
import app.reelvocab.core.domain.model.DictionarySense
import app.reelvocab.core.domain.model.Thesaurus
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DictionaryRepositoryImpl @Inject constructor(
    private val asset: DictionaryAsset,
    private val online: FreeDictionaryClient,
    private val cloud: CloudEnrichmentClient,
) : DictionaryRepository {

    override suspend fun lookup(rawLemma: String): DictionaryEntry? {
        val lemma = Headwords.normalize(rawLemma)
        if (lemma.isEmpty()) return null
        if (lemma.contains(' ')) return lookupWithAi(lemma)

        val local = asset.read { db -> db.entryFor(lemma) }
        if (local != null) return local

        val onlineEntry = online.lookup(lemma)
        if (onlineEntry != null) return onlineEntry

        return lookupWithAi(lemma)
    }

    private suspend fun lookupWithAi(lemma: String): DictionaryEntry? = try {
        val ai = cloud.definePhrase(lemma, DEFAULT_SERVER_URL) ?: return null
        DictionaryEntry(
            lemma = lemma,
            wordClass = null,
            ipa = null,
            frequencyRank = null,
            difficulty = null,
            senses = listOf(
                DictionarySense(
                    definition = ai.meaning,
                    example = ai.example,
                    examples = ai.examples,
                ),
            ),
            thesaurus = Thesaurus(
                synonyms = ai.synonyms,
                antonyms = ai.antonyms,
            ),
        )
    } catch (error: DictionaryAiException) {
        localFallback(lemma) ?: throw error
    }

    private suspend fun localFallback(lemma: String): DictionaryEntry? {
        val hints = lemma.split(' ')
            .filter { it.isNotBlank() }
            .distinct()
            .mapNotNull { word ->
                val entry = asset.read { db -> db.entryFor(word) } ?: return@mapNotNull null
                val definition = entry.senses.firstOrNull()?.definition?.trim()
                    ?.takeIf { it.isNotEmpty() }
                    ?: return@mapNotNull null
                "$word: $definition"
            }
        if (hints.isEmpty()) return null
        return DictionaryEntry(
            lemma = lemma,
            wordClass = null,
            ipa = null,
            frequencyRank = null,
            difficulty = null,
            senses = listOf(DictionarySense(definition = hints.joinToString("\n"))),
            thesaurus = Thesaurus(),
            fallbackWarning = FALLBACK_WARNING,
        )
    }

    override suspend fun search(prefix: String, limit: Int): List<DictionaryEntry> {
        val term = Headwords.normalize(prefix)
        if (term.length < 2) return emptyList()
        if (term.contains(' ')) {
            return listOfNotNull(lookup(term))
        }
        val safeLimit = limit.coerceIn(1, 50)
        return asset.read { db ->
            val lemmas = mutableListOf<String>()
            db.rawQuery(
                """
                SELECT DISTINCT lemma
                FROM word_fts
                WHERE lemma MATCH ?
                ORDER BY lemma
                LIMIT ?
                """.trimIndent(),
                arrayOf("$term*", safeLimit.toString()),
            ).use { cursor ->
                while (cursor.moveToNext()) {
                    lemmas += cursor.getString(0)
                }
            }
            lemmas.mapNotNull { db.entryFor(it) }
        }
    }

    override suspend fun thesaurus(rawLemma: String): Thesaurus {
        val lemma = Headwords.normalize(rawLemma)
        if (lemma.isEmpty()) return Thesaurus()
        return asset.read { db -> db.thesaurusFor(lemma) }
    }

    override suspend fun wordOfTheDay(): DictionaryEntry? {
        val lemma = asset.read { db ->
            db.rawQuery(
                "SELECT lemma FROM words ORDER BY RANDOM() LIMIT 1",
                null,
            ).use { cursor ->
                if (cursor.moveToFirst()) cursor.getString(0) else null
            }
        } ?: return null
        return asset.read { db -> db.entryFor(lemma) }
    }

    override suspend fun isReady(): Boolean = runCatching {
        asset.read { db ->
            db.rawQuery("SELECT 1 FROM words LIMIT 1", null).use { it.moveToFirst() }
        }
    }.getOrDefault(false)

    private companion object {
        const val FALLBACK_WARNING = "AI unavailable. Showing basic word hints."
    }
}
