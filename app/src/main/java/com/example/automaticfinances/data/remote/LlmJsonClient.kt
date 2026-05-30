package com.example.automaticfinances.data.remote

import kotlinx.serialization.json.JsonObject

/**
 * Transport seam for an LLM that returns JSON constrained by a schema. Abstracting the concrete
 * provider ([GeminiService]) keeps the NLP layer ([com.example.automaticfinances.data.voice.VoiceTransactionParser])
 * testable with a fake and swappable if the backend changes.
 *
 * Implementations throw [GeminiException] (with a typed [GeminiFailure]) on failure.
 */
interface LlmJsonClient {
    suspend fun generateStructuredJson(
        systemInstruction: String,
        prompt: String,
        responseSchema: JsonObject,
        temperature: Double = 0.2,
    ): String
}
