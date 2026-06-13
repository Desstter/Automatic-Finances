package com.example.automaticfinances

import com.example.automaticfinances.data.remote.CompositeLlmClient
import com.example.automaticfinances.data.remote.LlmException
import com.example.automaticfinances.data.remote.LlmFailure
import com.example.automaticfinances.data.remote.LlmJsonClient
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import org.junit.Assert.assertEquals
import org.junit.Assert.fail
import org.junit.Test

/** Records calls so the test can assert whether the fallback was reached. */
private class StubLlmClient(
    private val response: String? = null,
    private val failure: LlmFailure? = null,
) : LlmJsonClient {
    var calls = 0
        private set

    override suspend fun generateStructuredJson(
        systemInstruction: String,
        prompt: String,
        responseSchema: JsonObject,
        temperature: Double,
    ): String {
        calls++
        failure?.let { throw LlmException(it, "stub $it") }
        return response ?: error("StubLlmClient needs a response or a failure")
    }
}

/**
 * Guards the DeepSeek -> Gemini routing in [CompositeLlmClient], in particular the regression fix:
 * a missing/invalid DeepSeek key must fall back to Gemini (which has its own build-time key) instead
 * of breaking the advisor and voice parsing.
 */
class CompositeLlmClientTest {

    private val emptySchema: JsonObject = buildJsonObject {}

    private suspend fun CompositeLlmClient.run() =
        generateStructuredJson("sys", "prompt", emptySchema, 0.2)

    @Test
    fun primarySucceeds_fallbackNeverCalled() = runBlocking {
        val primary = StubLlmClient(response = "{\"ok\":true}")
        val fallback = StubLlmClient(response = "{\"from\":\"gemini\"}")

        val out = CompositeLlmClient(primary, fallback).run()

        assertEquals("{\"ok\":true}", out)
        assertEquals(1, primary.calls)
        assertEquals(0, fallback.calls)
    }

    @Test
    fun missingDeepSeekKey_fallsBackToGemini() = runBlocking {
        val primary = StubLlmClient(failure = LlmFailure.MISSING_KEY)
        val fallback = StubLlmClient(response = "{\"from\":\"gemini\"}")

        val out = CompositeLlmClient(primary, fallback).run()

        assertEquals("{\"from\":\"gemini\"}", out)
        assertEquals(1, fallback.calls)
    }

    @Test
    fun invalidDeepSeekKey_fallsBackToGemini() = runBlocking {
        val primary = StubLlmClient(failure = LlmFailure.AUTH)
        val fallback = StubLlmClient(response = "{\"from\":\"gemini\"}")

        val out = CompositeLlmClient(primary, fallback).run()

        assertEquals("{\"from\":\"gemini\"}", out)
        assertEquals(1, fallback.calls)
    }

    @Test
    fun quotaOnPrimary_fallsBackToGemini() = runBlocking {
        val primary = StubLlmClient(failure = LlmFailure.QUOTA)
        val fallback = StubLlmClient(response = "{\"from\":\"gemini\"}")

        val out = CompositeLlmClient(primary, fallback).run()

        assertEquals("{\"from\":\"gemini\"}", out)
        assertEquals(1, fallback.calls)
    }

    @Test
    fun blockedOnPrimary_doesNotFallBack() = runBlocking {
        val primary = StubLlmClient(failure = LlmFailure.BLOCKED)
        val fallback = StubLlmClient(response = "{\"from\":\"gemini\"}")

        try {
            CompositeLlmClient(primary, fallback).run()
            fail("BLOCKED must surface immediately without trying the fallback")
        } catch (e: LlmException) {
            assertEquals(LlmFailure.BLOCKED, e.failure)
        }
        assertEquals(0, fallback.calls)
    }

    @Test
    fun bothFail_reThrowsPrimaryError() = runBlocking {
        // Primary key missing AND Gemini also down -> the user should see the actionable primary error.
        val primary = StubLlmClient(failure = LlmFailure.MISSING_KEY)
        val fallback = StubLlmClient(failure = LlmFailure.SERVER)

        try {
            CompositeLlmClient(primary, fallback).run()
            fail("should have thrown")
        } catch (e: LlmException) {
            assertEquals(LlmFailure.MISSING_KEY, e.failure)
        }
        assertEquals(1, fallback.calls)
    }
}
