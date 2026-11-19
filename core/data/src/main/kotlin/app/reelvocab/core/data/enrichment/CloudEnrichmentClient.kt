package app.reelvocab.core.data.enrichment

import app.reelvocab.core.common.AppDispatchers
import java.net.HttpURLConnection
import java.net.URL
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import org.json.JSONObject

data class AiDictionaryDefinition(
    val meaning: String,
    val synonyms: List<String> = emptyList(),
    val example: String? = null,
)

@Singleton
class CloudEnrichmentClient @Inject constructor(
    private val config: CloudConfigStore,
    private val dispatchers: AppDispatchers,
) {
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun define(headword: String, contextLine: String?): String? =
        withContext(dispatchers.io) {
            if (!config.isConfigured) return@withContext null
            runCatching { request(headword, contextLine) }.getOrNull()
        }

    suspend fun definePhrase(phrase: String): AiDictionaryDefinition? =
        withContext(dispatchers.io) {
            val term = phrase.trim()
            if (term.isEmpty() || !config.isConfigured) return@withContext null
            runCatching { requestPhrase(term) }.getOrNull()
        }

    private fun request(headword: String, contextLine: String?): String? {
        val payload = buildJsonObject {
            put("model", config.model)
            put("temperature", 0.2)
            put(
                "messages",
                buildJsonArray {
                    add(
                        buildJsonObject {
                            put("role", "system")
                            put(
                                "content",
                                "You define words for a film-vocabulary learner. " +
                                    "Answer with one short definition (max two sentences) " +
                                    "for the headword as used in the line. No preamble.",
                            )
                        },
                    )
                    add(
                        buildJsonObject {
                            put("role", "user")
                            put(
                                "content",
                                buildString {
                                    append("Word: ").append(headword)
                                    if (!contextLine.isNullOrBlank()) {
                                        append("\nLine: ").append(contextLine)
                                    }
                                },
                            )
                        },
                    )
                },
            )
        }

        return post(payload)
    }

    private fun requestPhrase(phrase: String): AiDictionaryDefinition? {
        val payload = buildJsonObject {
            put("model", config.model)
            put("temperature", 0.2)
            put(
                "messages",
                buildJsonArray {
                    add(
                        buildJsonObject {
                            put("role", "system")
                            put(
                                "content",
                                "You define English words and multi-word phrases for a film-vocabulary " +
                                    "learner. Return strict JSON with keys meaning (string), synonyms " +
                                    "(array of strings), and example (string). Define the phrase in the " +
                                    "context of movies or general English. No markdown or commentary.",
                            )
                        },
                    )
                    add(
                        buildJsonObject {
                            put("role", "user")
                            put(
                                "content",
                                "Define the phrase '$phrase' in the context of movies or general " +
                                    "English. Provide the meaning, synonyms, and an example sentence.",
                            )
                        },
                    )
                },
            )
        }
        val content = post(payload) ?: return null
        return parseDefinition(content)
    }

    private fun post(payload: JsonObject): String? {
        val connection = (URL(config.endpoint).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = 8_000
            readTimeout = 15_000
            doOutput = true
            setRequestProperty("Content-Type", "application/json")
            if (config.apiKey.isNotBlank()) {
                setRequestProperty("Authorization", "Bearer ${config.apiKey}")
            }
        }
        return try {
            connection.outputStream.use { output ->
                output.write(json.encodeToString(JsonObject.serializer(), payload).toByteArray())
            }
            val code = connection.responseCode
            val body = (if (code in 200..299) connection.inputStream else connection.errorStream)
                ?.bufferedReader()
                ?.use { it.readText() }
                ?: return null
            if (code !in 200..299) return null
            val content = json.parseToJsonElement(body)
                .jsonObject["choices"]
                ?.jsonArray
                ?.firstOrNull()
                ?.jsonObject
                ?.get("message")
                ?.jsonObject
                ?.get("content")
                ?.jsonPrimitive
                ?.contentOrNull
            content?.trim()?.takeIf { it.isNotEmpty() }
        } finally {
            connection.disconnect()
        }
    }

    private fun parseDefinition(content: String): AiDictionaryDefinition? {
        val trimmed = content.trim()
        val start = trimmed.indexOf('{')
        val end = trimmed.lastIndexOf('}')
        val candidate = if (start >= 0 && end > start) {
            trimmed.substring(start, end + 1)
        } else {
            trimmed.removePrefix("```json").removePrefix("```").removeSuffix("```").trim()
        }
        val objectValue = runCatching { JSONObject(candidate) }.getOrNull()
        if (objectValue != null) {
            val meaning = firstNonBlank(
                objectValue.optString("meaning"),
                objectValue.optString("definition"),
                objectValue.optString("answer"),
            ) ?: return null
            val synonyms = mutableListOf<String>()
            val array = objectValue.optJSONArray("synonyms")
            if (array != null) {
                for (index in 0 until array.length()) {
                    bareString(array.optString(index))?.let(synonyms::add)
                }
            } else {
                objectValue.optString("synonyms")
                    .split(',')
                    .mapNotNull { bareString(it) }
                    .forEach(synonyms::add)
            }
            val example = firstNonBlank(
                objectValue.optString("example"),
                objectValue.optString("example_sentence"),
            )
            return AiDictionaryDefinition(
                meaning = meaning,
                synonyms = synonyms.take(8),
                example = example,
            )
        }
        val plain = candidate.trim().takeIf { it.isNotEmpty() } ?: return null
        return AiDictionaryDefinition(meaning = plain)
    }

    private fun firstNonBlank(vararg values: String?): String? =
        values.firstOrNull { !it.isNullOrBlank() }?.trim()

    private fun bareString(value: String): String? =
        value.trim().trim('"', '\'').takeIf { it.isNotEmpty() }
}
