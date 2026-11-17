package app.reelvocab.core.data.dictionary

import android.database.sqlite.SQLiteDatabase
import app.reelvocab.core.domain.model.DictionaryEntry
import app.reelvocab.core.domain.model.DictionarySense
import app.reelvocab.core.domain.model.Thesaurus
import app.reelvocab.core.domain.model.WordClass

private const val MAX_RELATIONS = 8

internal fun SQLiteDatabase.entryFor(lemma: String): DictionaryEntry? {
    val row = rawQuery(
        """
        SELECT id, lemma, pos, ipa, freq_rank, difficulty
        FROM words
        WHERE lemma = ?
        ORDER BY (freq_rank IS NULL), freq_rank
        LIMIT 1
        """.trimIndent(),
        arrayOf(lemma),
    ).use { cursor ->
        if (!cursor.moveToFirst()) {
            null
        } else {
            WordRow(
                id = cursor.getLong(0),
                lemma = cursor.getString(1),
                pos = cursor.getString(2),
                ipa = cursor.getString(3),
                frequencyRank = if (cursor.isNull(4)) null else cursor.getInt(4),
                difficulty = if (cursor.isNull(5)) null else cursor.getInt(5),
            )
        }
    } ?: return null

    return DictionaryEntry(
        lemma = row.lemma,
        wordClass = WordClass.fromStorage(row.pos),
        ipa = row.ipa?.let { "/$it/" },
        frequencyRank = row.frequencyRank,
        difficulty = row.difficulty,
        senses = sensesFor(row.id),
        thesaurus = thesaurusForWordId(row.id),
    )
}

private fun SQLiteDatabase.sensesFor(wordId: Long): List<DictionarySense> {
    val senses = mutableListOf<DictionarySense>()
    rawQuery(
        "SELECT definition, example FROM senses WHERE word_id = ? ORDER BY sense_order, id",
        arrayOf(wordId.toString()),
    ).use { cursor ->
        while (cursor.moveToNext()) {
            senses += DictionarySense(
                definition = cursor.getString(0),
                example = if (cursor.isNull(1)) null else cursor.getString(1),
            )
        }
    }
    return senses
}

private fun SQLiteDatabase.thesaurusForWordId(wordId: Long): Thesaurus {
    val synonyms = sortedSetOf<String>()
    val antonyms = sortedSetOf<String>()
    rawQuery(
        "SELECT type, target FROM relations WHERE word_id = ? ORDER BY target",
        arrayOf(wordId.toString()),
    ).use { cursor ->
        while (cursor.moveToNext()) {
            val target = cursor.getString(1)
            when (cursor.getString(0)) {
                "syn" -> synonyms += target
                "ant" -> antonyms += target
            }
        }
    }
    return Thesaurus(
        synonyms = synonyms.take(MAX_RELATIONS),
        antonyms = antonyms.take(MAX_RELATIONS),
    )
}

internal fun SQLiteDatabase.thesaurusFor(lemma: String): Thesaurus {
    val wordIds = mutableListOf<Long>()
    rawQuery("SELECT id FROM words WHERE lemma = ?", arrayOf(lemma)).use { cursor ->
        while (cursor.moveToNext()) {
            wordIds += cursor.getLong(0)
        }
    }

    val synonyms = sortedSetOf<String>()
    val antonyms = sortedSetOf<String>()
    wordIds.forEach { id ->
        val entry = thesaurusForWordId(id)
        synonyms += entry.synonyms
        antonyms += entry.antonyms
    }

    rawQuery(
        """
        SELECT DISTINCT w.lemma
        FROM relations r
        JOIN words w ON w.id = r.word_id
        WHERE r.target = ? AND r.type = 'ant'
        ORDER BY w.lemma
        """.trimIndent(),
        arrayOf(lemma),
    ).use { cursor ->
        while (cursor.moveToNext()) {
            antonyms += cursor.getString(0)
        }
    }

    synonyms -= lemma
    antonyms -= lemma
    return Thesaurus(
        synonyms = synonyms.take(MAX_RELATIONS),
        antonyms = antonyms.take(MAX_RELATIONS),
    )
}

private data class WordRow(
    val id: Long,
    val lemma: String,
    val pos: String,
    val ipa: String?,
    val frequencyRank: Int?,
    val difficulty: Int?,
)
