package app.reelvocab.core.data.dictionary

import app.reelvocab.core.common.Headwords
import app.reelvocab.core.domain.model.DictionaryEntry
import app.reelvocab.core.domain.model.DictionaryRepository
import app.reelvocab.core.domain.model.Thesaurus
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DictionaryRepositoryImpl @Inject constructor(
    private val asset: DictionaryAsset,
) : DictionaryRepository {

    override suspend fun lookup(rawLemma: String): DictionaryEntry? {
        val lemma = Headwords.normalize(rawLemma)
        if (lemma.isEmpty()) return null
        return asset.read { db -> db.entryFor(lemma) }
    }

    override suspend fun search(prefix: String, limit: Int): List<DictionaryEntry> {
        val term = Headwords.normalize(prefix).filter { it.isLetter() }
        if (term.length < 2) return emptyList()
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

    override suspend fun isReady(): Boolean = runCatching {
        asset.read { db ->
            db.rawQuery("SELECT 1 FROM words LIMIT 1", null).use { it.moveToFirst() }
        }
    }.getOrDefault(false)
}
