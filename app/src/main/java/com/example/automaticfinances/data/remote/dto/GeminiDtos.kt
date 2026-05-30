package com.example.automaticfinances.data.remote.dto

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

// ===========================================================================
// Wire DTOs for the Gemini `generateContent` REST endpoint.
// Transport-only: these mirror the API JSON and know nothing about transactions.
// The domain payload (the structured output) is deserialized separately by the
// caller using the schema it provides.
// ===========================================================================

@Serializable
data class GeminiRequest(
    val systemInstruction: GeminiContent? = null,
    val contents: List<GeminiContent>,
    val generationConfig: GeminiGenerationConfig? = null,
)

@Serializable
data class GeminiContent(
    val role: String? = null,
    val parts: List<GeminiPart>,
)

@Serializable
data class GeminiPart(
    val text: String,
)

@Serializable
data class GeminiGenerationConfig(
    val temperature: Double? = null,
    val responseMimeType: String? = null,
    val responseSchema: JsonObject? = null,
)

@Serializable
data class GeminiResponse(
    val candidates: List<GeminiCandidate> = emptyList(),
    val promptFeedback: GeminiPromptFeedback? = null,
)

@Serializable
data class GeminiCandidate(
    val content: GeminiContent? = null,
    val finishReason: String? = null,
)

@Serializable
data class GeminiPromptFeedback(
    val blockReason: String? = null,
)

/** Shape of the top-level error object Gemini returns on non-2xx responses. */
@Serializable
data class GeminiErrorEnvelope(
    val error: GeminiError? = null,
)

@Serializable
data class GeminiError(
    val code: Int = 0,
    val message: String = "",
    val status: String = "",
)
