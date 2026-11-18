package app.reelvocab.core.data.dictionary

import app.reelvocab.core.domain.model.DictionaryEntry
import app.reelvocab.core.domain.model.DictionarySense
import app.reelvocab.core.domain.model.Thesaurus
import app.reelvocab.core.domain.model.WordClass
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

@Singleton
class FreeDictionaryClient @Inject constructor() {

    suspend fun lookup(rawWord: String): DictionaryEntry? = withContext(Dispatchers.IO) {
        val word = rawWord.trim()
        if (word.isEmpty()) return@withContext null
        runCatching {
            val encoded = URLEncoder.encode(word, "UTF-8").replace("+", "%20")
            val connection = (URL("$BASE_URL/$encoded").openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = CONNECT_TIMEOUT_MS
                readTimeout = READ_TIMEOUT_MS
                setRequestProperty("Accept", "application/json")
                setRequestProperty("User-Agent", USER_AGENT)
            }
            try {
                if (connection.responseCode !in 200..299) return@runCatching null
                val body = connection.inputStream
                    .bufferedReader()
                    .use { reader -> reader.readText() }
                parse(word, body)
            } finally {
                connection.disconnect()
            }
        }.getOrNull()
    }

    private fun parse(fallbackWord: String, body: String): DictionaryEntry? {
        val root = JSONArray(body)
        if (root.length() == 0) return null
        val first = root.optJSONObject(0) ?: return null

        val senses = mutableListOf<DictionarySense>()
        val synonyms = linkedSetOf<String>()
        val antonyms = linkedSetOf<String>()
        var wordClass: WordClass? = null

        val meanings = first.optJSONArray("meanings") ?: return null
        for (meaningIndex in 0 until meanings.length()) {
            val meaning = meanings.optJSONObject(meaningIndex) ?: continue
            if (wordClass == null) {
                wordClass = WordClass.fromStorage(meaning.optString("partOfSpeech"))
            }
            addRelations(meaning.optJSONArray("synonyms"), synonyms)
            addRelations(meaning.optJSONArray("antonyms"), antonyms)

            val definitions = meaning.optJSONArray("definitions") ?: continue
            for (definitionIndex in 0 until definitions.length()) {
                if (senses.size >= MAX_SENSES) break
                val definition = definitions.optJSONObject(definitionIndex) ?: continue
                val text = definition.optString("definition").trim()
                if (text.isEmpty()) continue
                senses += DictionarySense(
                    definition = text,
                    example = definition.optString("example").trim().takeIf { it.isNotEmpty() },
                )
                addRelations(definition.optJSONArray("synonyms"), synonyms)
                addRelations(definition.optJSONArray("antonyms"), antonyms)
            }
        }
        if (senses.isEmpty()) return null

        val lemma = first.optString("word").trim()
            .takeIf { it.isNotEmpty() }
            ?: fallbackWord

        return DictionaryEntry(
            lemma = lemma.lowercase(),
            wordClass = wordClass,
            ipa = phonetic(first),
            frequencyRank = null,
            difficulty = null,
            senses = senses,
            thesaurus = Thesaurus(
                synonyms = synonyms.filterNot { it.equals(lemma, ignoreCase = true) }
                    .take(MAX_RELATIONS),
                antonyms = antonyms.filterNot { it.equals(lemma, ignoreCase = true) }
                    .take(MAX_RELATIONS),
            ),
        )
    }

    private fun phonetic(root: JSONObject): String? {
        val direct = root.optString("phonetic").trim().takeIf { it.isNotEmpty() }
        val value = direct ?: run {
            val phonetics = root.optJSONArray("phonetics") ?: return null
            var found: String? = null
            for (index in 0 until phonetics.length()) {
                val text = phonetics.optJSONObject(index)
                    ?.optString("text")
                    ?.trim()
                    ?.takeIf { it.isNotEmpty() }
                if (text != null) {
                    found = text
                    break
                }
            }
            found
        } ?: return null
        return if (value.startsWith("/")) value else "/$value/"
    }

    private fun addRelations(values: org.json.JSONArray?, target: MutableSet<String>) {
        if (values == null) return
        for (index in 0 until values.length()) {
            val value = values.optString(index).trim()
            if (value.isNotEmpty()) target += value
        }
    }

    private companion object {
        const val BASE_URL = "https://api.dictionaryapi.dev/api/v2/entries/en"
        const val CONNECT_TIMEOUT_MS = 2_500
        const val READ_TIMEOUT_MS = 3_000
        const val USER_AGENT = "ReelVocab/0.1 (offline-first vocabulary app)"
        const val MAX_SENSES = 8
        const val MAX_RELATIONS = 8
    }
}
