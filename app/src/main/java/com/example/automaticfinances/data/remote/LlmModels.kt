package com.example.automaticfinances.data.remote

/**
 * Provider-neutral failure taxonomy for any [LlmJsonClient] implementation (DeepSeek, Gemini, …).
 * The UI branches on this to offer the right recovery (retry on a transient error vs. ask the user
 * to fix their key / fall back to manual entry on quota exhaustion).
 */
enum class LlmFailure {
    MISSING_KEY,   // No API key configured
    NETWORK,       // No connectivity / timeout / I/O
    AUTH,          // 401/403 — invalid or unauthorized key
    QUOTA,         // 429 / insufficient balance — rate or credit limit hit
    BLOCKED,       // Safety / content filter blocked the prompt or response
    EMPTY,         // 2xx but no usable content
    SERVER,        // 5xx
    UNKNOWN,
}

/** Single typed exception every LLM transport throws so callers can pattern-match on [failure]. */
class LlmException(
    val failure: LlmFailure,
    message: String,
    cause: Throwable? = null,
) : Exception(message, cause)

/**
 * Blips worth an immediate same-endpoint retry: a cold mobile connection or a transient 5xx. AUTH /
 * MISSING_KEY / BLOCKED are excluded — retrying the same call won't fix them. QUOTA is excluded too:
 * an immediate retry rarely helps, and the [CompositeLlmClient] handles it by switching providers.
 */
val TRANSIENT_FAILURES: Set<LlmFailure> = setOf(LlmFailure.NETWORK, LlmFailure.SERVER)

/**
 * Failures where it is worth falling back to a *different* provider (DeepSeek → Gemini). Includes the
 * primary being down/rate-limited/out of credit/empty AND the key being missing or invalid
 * (MISSING_KEY / AUTH): Gemini has its own independent build-time key, so a DeepSeek key the user
 * never set (the default install state) must not break the advisor or voice parsing. If the fallback
 * also fails, [CompositeLlmClient] re-throws the *primary* error so the user still sees the actionable
 * "fix your DeepSeek key" message. Only BLOCKED is excluded (content filter — the other provider
 * likely agrees, and switching achieves nothing).
 */
val FALLBACK_FAILURES: Set<LlmFailure> = setOf(
    LlmFailure.NETWORK,
    LlmFailure.SERVER,
    LlmFailure.QUOTA,
    LlmFailure.EMPTY,
    LlmFailure.UNKNOWN,
    LlmFailure.MISSING_KEY,
    LlmFailure.AUTH,
)
