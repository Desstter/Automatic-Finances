package com.example.automaticfinances.data.remote

import kotlinx.serialization.json.JsonObject
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

/**
 * Resilience wrapper: tries DeepSeek first and falls back to Gemini when the primary is unavailable
 * (down, rate-limited, out of credit, empty, OR the DeepSeek key is missing/invalid — see
 * [FALLBACK_FAILURES]). Both backends already implement [LlmJsonClient], so this adds no transport
 * code, just the routing. Falling back on MISSING_KEY/AUTH matters because Gemini carries its own
 * build-time key: a user who never pasted a DeepSeek key still gets a working advisor and voice
 * parser via Gemini.
 *
 * Only [LlmFailure.BLOCKED] (content filter) is surfaced immediately — the other provider likely
 * agrees. If the fallback also fails, the *primary* error is re-thrown: it's the provider the user
 * configured, so its message is the actionable one (e.g. "fix your DeepSeek key").
 */
@Singleton
class CompositeLlmClient @Inject constructor(
    @param:Named("primaryLlm") private val primary: LlmJsonClient,
    @param:Named("fallbackLlm") private val fallback: LlmJsonClient,
) : LlmJsonClient {

    override suspend fun generateStructuredJson(
        systemInstruction: String,
        prompt: String,
        responseSchema: JsonObject,
        temperature: Double,
    ): String {
        return try {
            primary.generateStructuredJson(systemInstruction, prompt, responseSchema, temperature)
        } catch (primaryError: LlmException) {
            if (primaryError.failure !in FALLBACK_FAILURES) throw primaryError
            try {
                fallback.generateStructuredJson(systemInstruction, prompt, responseSchema, temperature)
            } catch (_: LlmException) {
                throw primaryError
            }
        }
    }
}
