package com.example.automaticfinances

import com.example.automaticfinances.data.db.DefaultCategories
import com.example.automaticfinances.data.remote.GeminiException
import com.example.automaticfinances.data.remote.GeminiFailure
import com.example.automaticfinances.data.remote.LlmJsonClient
import com.example.automaticfinances.data.voice.VoiceTransactionParser
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Canned LLM seam so the parser's JSON->domain mapping is tested without any network. */
private class FakeLlmJsonClient(
    var response: String = "",
    var toThrow: Throwable? = null,
) : LlmJsonClient {
    override suspend fun generateStructuredJson(
        systemInstruction: String,
        prompt: String,
        responseSchema: JsonObject,
        temperature: Double,
    ): String {
        toThrow?.let { throw it }
        return response
    }
}

class VoiceTransactionParserTest {

    private val json = Json { ignoreUnknownKeys = true }
    private val categories = DefaultCategories.list
    private val fakeClient = FakeLlmJsonClient()
    private val parser = VoiceTransactionParser(fakeClient, json)

    @Test
    fun parsesMultipleTransactions_andConvertsPesosToCents() = runBlocking {
        fakeClient.response = """
            {"transactions":[
              {"description":"almuerzo","amountCop":12000,"categoryName":"Comida por fuera","isIncome":false,"needsReview":false},
              {"description":"tinto","amountCop":2000,"categoryName":"Comida por fuera","isIncome":false,"needsReview":true}
            ]}
        """.trimIndent()

        val result = parser.parse("almuerzo 12 mil, tinto 2 mil", categories)

        assertEquals(2, result.size)
        assertEquals(1_200_000L, result[0].amountCents) // 12.000 COP -> cents
        assertEquals("almuerzo", result[0].description)
        assertEquals(200_000L, result[1].amountCents)
        assertTrue(result[1].needsReview)
    }

    @Test
    fun blankDescriptionItems_areDropped() = runBlocking {
        fakeClient.response = """
            {"transactions":[
              {"description":"   ","amountCop":5000,"categoryName":"Otros gastos","isIncome":false},
              {"description":"pan","amountCop":7000,"categoryName":"Comida obligatoria","isIncome":false}
            ]}
        """.trimIndent()

        val result = parser.parse("pan 7 mil", categories)

        assertEquals(1, result.size)
        assertEquals("pan", result[0].description)
    }

    @Test
    fun zeroAmount_forcesReview() = runBlocking {
        fakeClient.response = """
            {"transactions":[{"description":"algo","amountCop":0,"categoryName":"Otros gastos","isIncome":false,"needsReview":false}]}
        """.trimIndent()

        val result = parser.parse("algo", categories)

        assertEquals(0L, result[0].amountCents)
        assertTrue("A zero amount must always be flagged for review", result[0].needsReview)
    }

    @Test
    fun absurdlyLargeAmount_forcesReview() = runBlocking {
        fakeClient.response = """
            {"transactions":[{"description":"casa","amountCop":5000000000,"categoryName":"Otros gastos","isIncome":false,"needsReview":false}]}
        """.trimIndent()

        val result = parser.parse("cinco mil millones", categories)

        assertTrue(result[0].needsReview)
    }

    @Test
    fun incomeFlag_isPreserved() = runBlocking {
        fakeClient.response = """
            {"transactions":[{"description":"me pagaron","amountCop":1500000,"categoryName":"Salario","isIncome":true,"needsReview":false}]}
        """.trimIndent()

        val result = parser.parse("me pagaron millon y medio", categories)

        assertTrue(result[0].isIncome)
        assertFalse(result[0].needsReview)
        assertEquals(150_000_000L, result[0].amountCents)
    }

    @Test
    fun emptyTransactionList_returnsEmpty() = runBlocking {
        fakeClient.response = """{"transactions":[]}"""
        assertTrue(parser.parse("hola", categories).isEmpty())
    }

    @Test(expected = GeminiException::class)
    fun malformedJson_throwsGeminiException() = runBlocking {
        fakeClient.response = "no soy json"
        parser.parse("algo", categories)
        Unit
    }

    @Test
    fun transportFailure_propagatesAsGeminiException() = runBlocking {
        fakeClient.toThrow = GeminiException(GeminiFailure.QUOTA, "limit")
        try {
            parser.parse("algo", categories)
            assertTrue("should have thrown", false)
        } catch (e: GeminiException) {
            assertEquals(GeminiFailure.QUOTA, e.failure)
        }
    }
}
