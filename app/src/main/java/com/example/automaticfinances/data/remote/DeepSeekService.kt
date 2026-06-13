package com.example.automaticfinances.data.remote

import com.example.automaticfinances.data.preferences.AiPreferences
import com.example.automaticfinances.data.remote.dto.DeepSeekErrorEnvelope
import com.example.automaticfinances.data.remote.dto.DeepSeekMessage
import com.example.automaticfinances.data.remote.dto.DeepSeekRequest
import com.example.automaticfinances.data.remote.dto.DeepSeekResponse
import com.example.automaticfinances.data.remote.dto.DeepSeekResponseFormat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

/**
 * Transport-only client for the DeepSeek `/chat/completions` endpoint (OpenAI-compatible). This is
 * the app's single LLM backend: it powers both voice-transaction parsing and the financial advisor.
 *
 * The API key and model are read from [AiPreferences] on every call so the user can change them in
 * Settings without restarting the app; a [BuildConfig] value (CI/local.properties) is used as a
 * fallback when the user has not configured one. Failures are normalized into [LlmException].
 *
 * DeepSeek's JSON mode (`response_format = json_object`) guarantees syntactically valid JSON but,
 * unlike Gemini, does not accept a formal JSON Schema. We therefore describe the expected shape in
 * the system instruction (the caller-provided [JsonObject] schema is serialized inline) and the word
 * "json" is always present, which the API requires to enable JSON mode.
 */
@Singleton
class DeepSeekService @Inject constructor(
    private val client: OkHttpClient,
    private val json: Json,
    private val aiPreferences: AiPreferences,
    @param:Named("deepseekApiKey") private val fallbackApiKey: String,
) : LlmJsonClient {

    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    override suspend fun generateStructuredJson(
        systemInstruction: String,
        prompt: String,
        responseSchema: JsonObject,
        temperature: Double,
    ): String = withContext(Dispatchers.IO) {
        val apiKey = aiPreferences.getApiKey().ifBlank { fallbackApiKey }
        if (apiKey.isBlank()) {
            throw LlmException(LlmFailure.MISSING_KEY, "No DeepSeek API key configured")
        }
        val model = aiPreferences.getModel()

        val fullSystem = buildString {
            append(systemInstruction.trim())
            append("\n\n")
            append("Responde ÚNICAMENTE con un objeto JSON válido que cumpla EXACTAMENTE este esquema ")
            append("(sin texto adicional, sin markdown, sin explicaciones):\n")
            append(responseSchema.toString())
        }

        val requestBody = DeepSeekRequest(
            model = model,
            messages = listOf(
                DeepSeekMessage(role = "system", content = fullSystem),
                DeepSeekMessage(role = "user", content = prompt),
            ),
            temperature = temperature,
            stream = false,
            responseFormat = DeepSeekResponseFormat(type = "json_object"),
        )

        val payload = try {
            json.encodeToString(DeepSeekRequest.serializer(), requestBody)
        } catch (e: Exception) {
            throw LlmException(LlmFailure.UNKNOWN, "Could not serialize DeepSeek request", e)
        }

        // Retry transient blips (cold connection / 5xx) with a short exponential backoff; surface
        // everything else immediately so the caller can react (fix key, fall back, etc.).
        var attempt = 0
        while (true) {
            try {
                return@withContext callOnce(apiKey, payload)
            } catch (e: LlmException) {
                if (e.failure in TRANSIENT_FAILURES && attempt < MAX_RETRIES) {
                    delay(BASE_BACKOFF_MS shl attempt)
                    attempt++
                } else {
                    throw e
                }
            }
        }
        @Suppress("UNREACHABLE_CODE") error("unreachable")
    }

    private fun callOnce(apiKey: String, payload: String): String {
        val httpRequest = Request.Builder()
            .url("$BASE_URL/chat/completions")
            .header("Authorization", "Bearer $apiKey")
            .post(payload.toRequestBody(jsonMediaType))
            .build()

        val responseText = try {
            client.newCall(httpRequest).execute().use { response ->
                val raw = response.body?.string().orEmpty()
                if (!response.isSuccessful) {
                    throw mapHttpError(response.code, raw)
                }
                raw
            }
        } catch (e: LlmException) {
            throw e
        } catch (e: IOException) {
            throw LlmException(LlmFailure.NETWORK, "Network error talking to DeepSeek", e)
        }

        return parseContent(responseText)
    }

    private fun parseContent(rawJson: String): String {
        val parsed = try {
            json.decodeFromString(DeepSeekResponse.serializer(), rawJson)
        } catch (e: Exception) {
            throw LlmException(LlmFailure.UNKNOWN, "Could not parse DeepSeek response", e)
        }

        val choice = parsed.choices.firstOrNull()
        if (choice?.finishReason == "content_filter") {
            throw LlmException(LlmFailure.BLOCKED, "Response blocked by content filter")
        }

        val text = choice?.message?.content?.trim()
        if (text.isNullOrBlank()) {
            throw LlmException(LlmFailure.EMPTY, "DeepSeek returned no content")
        }
        return text
    }

    private fun mapHttpError(code: Int, body: String): LlmException {
        val apiMessage = runCatching {
            json.decodeFromString(DeepSeekErrorEnvelope.serializer(), body).error?.message
        }.getOrNull().orEmpty()

        val failure = when (code) {
            401, 403 -> LlmFailure.AUTH
            402, 429 -> LlmFailure.QUOTA // 402 = insufficient balance, 429 = rate limit
            in 500..599 -> LlmFailure.SERVER
            else -> LlmFailure.UNKNOWN
        }
        val suffix = if (apiMessage.isNotBlank()) ": $apiMessage" else ""
        return LlmException(failure, "DeepSeek HTTP $code$suffix")
    }

    companion object {
        private const val BASE_URL = "https://api.deepseek.com"
        private const val MAX_RETRIES = 2
        private const val BASE_BACKOFF_MS = 400L // 400ms, then 800ms
    }
}
