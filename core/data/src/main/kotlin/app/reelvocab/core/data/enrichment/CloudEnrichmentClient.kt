package app.reelvocab.core.data.enrichment

import app.reelvocab.core.common.AppDispatchers
import app.reelvocab.core.domain.model.DictionaryAiException
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.withContext
import org.json.JSONObject

data class AiDictionaryDefinition(
    val meaning: String,
    val synonyms: List<String> = emptyList(),
    val antonyms: List<String> = emptyList(),
    val examples: List<String> = emptyList(),
) {
    val example: String? get() = examples.firstOrNull()
}

@Singleton
class CloudEnrichmentClient @Inject constructor(
    private val config: CloudConfigStore,
    private val dispatchers: AppDispatchers,
) {
    suspend fun definePhrase(
        phrase: String,
        serverUrl: String,
    ): AiDictionaryDefinition? = withContext(dispatchers.io) {
        val term = phrase.trim()
        if (term.isEmpty() || !config.enabled) return@withContext null
        try {
            requestDefinition(term, serverUrl)
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Exception) {
            throw DictionaryAiException(cause = error)
        }
    }

    private fun requestDefinition(
        query: String,
        serverUrl: String,
    ): AiDictionaryDefinition {
        if (serverUrl.isBlank()) throw IOException("Missing AI server URL")
        val payload = JSONObject()
            .put("query", query)
            .put("type", TYPE_DEFINITION)
        val connection = (URL(serverUrl).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = 8_000
            readTimeout = 20_000
            doOutput = true
            setRequestProperty("Content-Type", "application/json; charset=utf-8")
            setRequestProperty("Accept", "application/json")
        }
        return try {
            connection.outputStream.use { output ->
                output.write(payload.toString().toByteArray(Charsets.UTF_8))
            }
            val code = connection.responseCode
            val body = (if (code in 200..299) connection.inputStream else connection.errorStream)
                ?.bufferedReader()
                ?.use { it.readText() }
            if (code !in 200..299) {
                throw IOException("AI server returned HTTP $code")
            }
            if (body.isNullOrBlank()) {
                throw IOException("AI server returned an empty response")
            }
            parseDefinition(body)
                ?: throw IOException("AI server response did not contain a definition")
        } finally {
            connection.disconnect()
        }
    }

    private fun parseDefinition(body: String): AiDictionaryDefinition? {
        val root = runCatching { JSONObject(body) }.getOrNull() ?: return null
        val meaning = firstNonBlank(
            root.optString("definition"),
            root.optString("meaning"),
        ) ?: return null
        val examples = linkedSetOf<String>()
        root.optJSONArray("examples")?.let { array ->
            for (index in 0 until array.length()) {
                bareString(array.optString(index))?.let(examples::add)
            }
        }
        firstNonBlank(root.optString("example"))?.let(examples::add)
        return AiDictionaryDefinition(
            meaning = meaning,
            synonyms = readRelations(root, "synonyms"),
            antonyms = readRelations(root, "antonyms"),
            examples = examples.toList(),
        )
    }

    private fun readRelations(root: JSONObject, key: String): List<String> {
        val values = linkedSetOf<String>()
        val array = root.optJSONArray(key)
        if (array != null) {
            for (index in 0 until array.length()) {
                bareString(array.optString(index))?.let(values::add)
            }
        } else {
            root.optString(key)
                .split(',')
                .mapNotNull { value -> bareString(value) }
                .forEach(values::add)
        }
        return values.take(8)
    }

    private fun firstNonBlank(vararg values: String?): String? =
        values.firstOrNull {
            !it.isNullOrBlank() && !it.equals("null", ignoreCase = true)
        }?.trim()

    private fun bareString(value: String): String? =
        value.trim().trim('"', '\'').takeIf {
            it.isNotEmpty() && !it.equals("null", ignoreCase = true)
        }

    private companion object {
        const val TYPE_DEFINITION = "definition"
    }
}
