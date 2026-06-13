package com.example.automaticfinances.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// ===========================================================================
// Wire DTOs for the DeepSeek `/chat/completions` endpoint (OpenAI-compatible).
// Transport-only: these mirror the API JSON and know nothing about transactions.
// The domain payload (the structured output) is deserialized separately by the
// caller using the schema it provides.
// ===========================================================================

@Serializable
data class DeepSeekRequest(
    val model: String,
    val messages: List<DeepSeekMessage>,
    val temperature: Double? = null,
    val stream: Boolean = false,
    @SerialName("response_format") val responseFormat: DeepSeekResponseFormat? = null,
)

@Serializable
data class DeepSeekMessage(
    val role: String,
    val content: String,
)

@Serializable
data class DeepSeekResponseFormat(
    val type: String,
)

@Serializable
data class DeepSeekResponse(
    val choices: List<DeepSeekChoice> = emptyList(),
)

@Serializable
data class DeepSeekChoice(
    val message: DeepSeekMessage? = null,
    @SerialName("finish_reason") val finishReason: String? = null,
)

/** Shape of the error object DeepSeek returns on non-2xx responses. */
@Serializable
data class DeepSeekErrorEnvelope(
    val error: DeepSeekError? = null,
)

@Serializable
data class DeepSeekError(
    val message: String = "",
    val type: String = "",
    val code: String? = null,
)
