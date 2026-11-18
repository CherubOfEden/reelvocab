package app.reelvocab.core.data.enrichment

import app.reelvocab.core.common.AppDispatchers
import java.net.HttpURLConnection
import java.net.URL
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put

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
                output.write(json.encodeToString(kotlinx.serialization.json.JsonObject.serializer(), payload).toByteArray())
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
}
