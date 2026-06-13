package com.example.automaticfinances.data.remote

import com.example.automaticfinances.data.remote.dto.GeminiContent
import com.example.automaticfinances.data.remote.dto.GeminiErrorEnvelope
import com.example.automaticfinances.data.remote.dto.GeminiGenerationConfig
import com.example.automaticfinances.data.remote.dto.GeminiPart
import com.example.automaticfinances.data.remote.dto.GeminiRequest
import com.example.automaticfinances.data.remote.dto.GeminiResponse
import kotlinx.coroutines.Dispatchers
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
 * Backwards-compatible aliases. The canonical, provider-neutral types now live in
 * [LlmModels]; Gemini was just the first backend. New code should prefer [LlmFailure] /
 * [LlmException] directly.
 */
typealias GeminiFailure = LlmFailure
typealias GeminiException = LlmException

/**
 * Thin, transport-only client for the Gemini `generateContent` endpoint. Tries each model in
 * [FALLBACK_MODELS] in order; moves to the next on quota or server errors. Callers own the
 * schema and deserialize the result. Failures are normalized into [GeminiException].
 */
@Singleton
class GeminiService @Inject constructor(
    private val client: OkHttpClient,
    private val json: Json,
    @param:Named("geminiApiKey") private val apiKey: String,
) : LlmJsonClient {

    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    override suspend fun generateStructuredJson(
        systemInstruction: String,
        prompt: String,
        responseSchema: JsonObject,
        temperature: Double,
    ): String = withContext(Dispatchers.IO) {
        if (apiKey.isBlank()) {
            throw GeminiException(GeminiFailure.MISSING_KEY, "No Gemini API key configured")
        }

        var lastException: GeminiException? = null
        for (model in FALLBACK_MODELS) {
            try {
                return@withContext callModel(model, systemInstruction, prompt, responseSchema, temperature)
            } catch (e: GeminiException) {
                if (e.failure in RETRYABLE_FAILURES) {
                    lastException = e
                } else {
                    throw e
                }
            }
        }
        throw lastException ?: GeminiException(GeminiFailure.UNKNOWN, "All Gemini models exhausted")
    }

    private fun callModel(
        model: String,
        systemInstruction: String,
        prompt: String,
        responseSchema: JsonObject,
        temperature: Double,
    ): String {
        val requestBody = GeminiRequest(
            systemInstruction = GeminiContent(parts = listOf(GeminiPart(systemInstruction))),
            contents = listOf(GeminiContent(role = "user", parts = listOf(GeminiPart(prompt)))),
            generationConfig = GeminiGenerationConfig(
                temperature = temperature,
                responseMimeType = "application/json",
                responseSchema = responseSchema,
            ),
        )

        val payload = try {
            json.encodeToString(GeminiRequest.serializer(), requestBody)
        } catch (e: Exception) {
            throw GeminiException(GeminiFailure.UNKNOWN, "Could not serialize Gemini request", e)
        }

        // The API key travels in the `x-goog-api-key` header rather than as a `?key=` query
        // parameter, so it does not leak into server/proxy access logs or request history.
        val url = "$BASE_URL/models/$model:generateContent"
        val httpRequest = Request.Builder()
            .url(url)
            .header("x-goog-api-key", apiKey)
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
        } catch (e: GeminiException) {
            throw e
        } catch (e: IOException) {
            throw GeminiException(GeminiFailure.NETWORK, "Network error talking to $model", e)
        }

        return parseCandidateText(responseText)
    }

    private fun parseCandidateText(rawJson: String): String {
        val parsed = try {
            json.decodeFromString(GeminiResponse.serializer(), rawJson)
        } catch (e: Exception) {
            throw GeminiException(GeminiFailure.UNKNOWN, "Could not parse Gemini response", e)
        }

        parsed.promptFeedback?.blockReason?.let { reason ->
            throw GeminiException(GeminiFailure.BLOCKED, "Prompt blocked by safety filter: $reason")
        }

        val candidate = parsed.candidates.firstOrNull()
        if (candidate?.finishReason == "SAFETY") {
            throw GeminiException(GeminiFailure.BLOCKED, "Response blocked by safety filter")
        }

        val text = candidate?.content?.parts?.firstOrNull()?.text?.trim()
        if (text.isNullOrBlank()) {
            throw GeminiException(GeminiFailure.EMPTY, "Gemini returned no content")
        }
        return text
    }

    private fun mapHttpError(code: Int, body: String): GeminiException {
        val apiMessage = runCatching {
            json.decodeFromString(GeminiErrorEnvelope.serializer(), body).error?.message
        }.getOrNull().orEmpty()

        val failure = when (code) {
            401, 403 -> GeminiFailure.AUTH
            429 -> GeminiFailure.QUOTA
            in 500..599 -> GeminiFailure.SERVER
            else -> GeminiFailure.UNKNOWN
        }
        val suffix = if (apiMessage.isNotBlank()) ": $apiMessage" else ""
        return GeminiException(failure, "Gemini HTTP $code$suffix")
    }

    companion object {
        private const val BASE_URL = "https://generativelanguage.googleapis.com/v1beta"

        // Failures where it's worth trying the NEXT model in the list rather than giving up: quota,
        // a 5xx blip, or an UNKNOWN (e.g. a 404 if a model id is ever retired/unavailable). AUTH and
        // BLOCKED still abort immediately — a different model won't fix a bad key or a content block.
        private val RETRYABLE_FAILURES = setOf(
            GeminiFailure.QUOTA,
            GeminiFailure.SERVER,
            GeminiFailure.UNKNOWN,
        )

        // Tried in order: cheapest/fastest first, most capable last.
        val FALLBACK_MODELS = listOf(
            "gemini-3.1-flash-lite",
            "gemma-4-31b-it",
            "gemini-3.5-flash",
            "gemini-2.5-flash",
        )
    }
}
