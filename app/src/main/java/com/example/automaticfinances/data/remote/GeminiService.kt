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
 * Reason a Gemini call failed, so the UI can react appropriately (e.g. fall back to
 * manual entry on quota exhaustion vs. asking the user to retry on a transient network error).
 */
enum class GeminiFailure {
    MISSING_KEY,   // No API key configured in the build
    NETWORK,       // No connectivity / timeout / I/O
    AUTH,          // 401/403 — invalid or unauthorized key
    QUOTA,         // 429 — free-tier daily/minute limit hit
    BLOCKED,       // Safety filter blocked the prompt or response
    EMPTY,         // 2xx but no usable candidate text
    SERVER,        // 5xx
    UNKNOWN,
}

class GeminiException(
    val failure: GeminiFailure,
    message: String,
    cause: Throwable? = null,
) : Exception(message, cause)

/**
 * Thin, transport-only client for the Gemini `generateContent` endpoint. It performs a single
 * structured-output request and returns the raw model JSON text (matching the supplied
 * [responseSchema]); it never interprets transactions. Callers own the schema and deserialize
 * the result. Failures are normalized into [GeminiException] with a typed [GeminiFailure].
 */
@Singleton
class GeminiService @Inject constructor(
    private val client: OkHttpClient,
    private val json: Json,
    @param:Named("geminiApiKey") private val apiKey: String,
    @param:Named("geminiModel") private val model: String,
) : LlmJsonClient {

    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    /**
     * Sends [prompt] (the user transcript) plus a [systemInstruction] and forces the model to
     * answer with JSON matching [responseSchema]. Returns the model's JSON text.
     *
     * @throws GeminiException on any failure, with a [GeminiFailure] the UI can branch on.
     */
    override suspend fun generateStructuredJson(
        systemInstruction: String,
        prompt: String,
        responseSchema: JsonObject,
        temperature: Double,
    ): String = withContext(Dispatchers.IO) {
        if (apiKey.isBlank()) {
            throw GeminiException(GeminiFailure.MISSING_KEY, "No Gemini API key configured")
        }

        val requestBody = GeminiRequest(
            systemInstruction = GeminiContent(parts = listOf(GeminiPart(systemInstruction))),
            contents = listOf(GeminiContent(role = "user", parts = listOf(GeminiPart(prompt)))),
            generationConfig = GeminiGenerationConfig(
                temperature = temperature,
                responseMimeType = "application/json",
                responseSchema = responseSchema,
            ),
        )

        val url = "$BASE_URL/models/$model:generateContent?key=$apiKey"
        val httpRequest = Request.Builder()
            .url(url)
            .post(json.encodeToString(GeminiRequest.serializer(), requestBody).toRequestBody(jsonMediaType))
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
            throw GeminiException(GeminiFailure.NETWORK, "Network error talking to Gemini", e)
        }

        parseCandidateText(responseText)
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
        // gemini-2.0-flash is no longer offered to new API keys; 2.5-flash is the current
        // free-tier flash model. Verified working against the live endpoint.
        const val DEFAULT_MODEL = "gemini-2.5-flash"
    }
}
