package com.example.automaticfinances.data.voice

import com.example.automaticfinances.data.db.Category
import com.example.automaticfinances.data.remote.GeminiException
import com.example.automaticfinances.data.remote.GeminiFailure
import com.example.automaticfinances.data.remote.LlmJsonClient
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject
import javax.inject.Inject

/**
 * Turns a Colombian-Spanish transcript into a list of [ParsedTransaction] using Gemini with
 * forced structured output. Stays free of any DB/Android coupling: it receives the live category
 * list as input and returns pure domain objects, mirroring the design of [BancolombiaParser].
 *
 * The model only *suggests* a category name from the provided list; resolution to a real id and
 * persistence happen downstream, reusing the existing categorization + idempotency machinery.
 */
class VoiceTransactionParser @Inject constructor(
    private val llmClient: LlmJsonClient,
    private val json: Json,
) {

    /**
     * @param transcript what the user said (already trimmed, non-blank — caller guarantees this).
     * @param categories the live, active categories so Gemini picks a name that actually exists.
     * @return parsed transactions, possibly empty if the model found none.
     * @throws GeminiException on transport/model failure (the UI branches on [GeminiFailure]).
     */
    suspend fun parse(transcript: String, categories: List<Category>): List<ParsedTransaction> {
        val systemInstruction = buildSystemInstruction(categories)
        val rawJson = llmClient.generateStructuredJson(
            systemInstruction = systemInstruction,
            prompt = transcript,
            responseSchema = RESPONSE_SCHEMA,
        )

        val result = try {
            json.decodeFromString(VoiceParseResultDto.serializer(), rawJson)
        } catch (e: Exception) {
            throw GeminiException(GeminiFailure.UNKNOWN, "Gemini returned malformed transaction JSON", e)
        }

        return result.transactions
            .mapNotNull { it.toDomainOrNull() }
    }

    private fun VoiceParsedItemDto.toDomainOrNull(): ParsedTransaction? {
        val description = description.trim()
        if (description.isEmpty()) return null

        // Gemini returns whole pesos; the rest of the app works in cents (COP × 100).
        val cents = amountCop.coerceAtLeast(0L) * 100
        val category = categoryName?.trim()?.takeIf { it.isNotEmpty() }

        // Force a review whenever the model flagged it OR the amount looks unusable, so the
        // user never silently saves a 0-peso or absurd transaction.
        val invalidAmount = amountCop <= 0L || amountCop > MAX_REASONABLE_COP
        return ParsedTransaction(
            description = description,
            amountCents = cents,
            suggestedCategoryName = category,
            isIncome = isIncome,
            needsReview = needsReview || invalidAmount,
        )
    }

    private fun buildSystemInstruction(categories: List<Category>): String {
        val expenseNames = categories.filter { !it.isIncome }.joinToString(", ") { it.name }
        val incomeNames = categories.filter { it.isIncome }.joinToString(", ") { it.name }

        return """
            Eres un asistente que convierte frases habladas en español colombiano en transacciones
            financieras. El usuario dicta uno o varios gastos o ingresos en una sola frase.

            REGLAS DE MONTO:
            - "amountCop" SIEMPRE es un entero en pesos colombianos, sin separadores ni decimales.
              Ejemplos: "120.000 pesos" -> 120000 ; "siete mil" -> 7000 ; "12 mil" -> 12000.
            - Jerga colombiana de miles: "luca"/"lucas", "barras", "palos", "mil" significan miles.
              "tres lucas" -> 3000 ; "dos barras" -> 2000.
            - Un número pequeño y suelto en contexto de compra cotidiana ("tinto 2", "almuerzo 12")
              casi siempre significa miles: 2 -> 2000, 12 -> 12000. Cuando hagas esa inferencia,
              marca "needsReview": true para que el usuario confirme.
            - Si no logras identificar un monto, usa 0 y "needsReview": true.

            REGLAS DE MÚLTIPLES TRANSACCIONES:
            - Una frase puede contener VARIAS transacciones separadas por comas o "y".
              "almuerzo 12, tinto 2" -> dos transacciones independientes.

            REGLAS DE INGRESO vs GASTO:
            - "isIncome": true SOLO si claramente es un ingreso (me pagaron, recibí, me consignaron,
              me entró, me transfirieron). En cualquier otro caso es un gasto: false.

            REGLAS DE CATEGORÍA:
            - "categoryName" debe ser EXACTAMENTE uno de estos nombres, respetando tildes y mayúsculas.
            - Categorías de gasto: $expenseNames.
            - Categorías de ingreso: $incomeNames.
            - Elige la más adecuada según lo que se compró/recibió. Si ninguna encaja, usa
              "Otros gastos" para gastos u "Otros ingresos" para ingresos.

            REGLAS DE DESCRIPCIÓN:
            - "description": breve, en minúsculas, el concepto de lo comprado o recibido SIN el monto.
              "120.000 pesos en galletas para el mercado" -> "galletas para el mercado".

            Devuelve SOLO el JSON con la lista "transactions". Si no hay ninguna transacción
            reconocible, devuelve una lista vacía.
        """.trimIndent()
    }

    // ---- Wire DTOs for the structured-output payload (private to this layer) ----

    @Serializable
    private data class VoiceParseResultDto(
        val transactions: List<VoiceParsedItemDto> = emptyList(),
    )

    @Serializable
    private data class VoiceParsedItemDto(
        val description: String = "",
        val amountCop: Long = 0L,
        val categoryName: String? = null,
        val isIncome: Boolean = false,
        val needsReview: Boolean = false,
    )

    companion object {
        /** Sanity ceiling (~1,000 million COP) above which we force a manual review. */
        private const val MAX_REASONABLE_COP = 1_000_000_000L

        /** OpenAPI-subset schema Gemini uses to constrain its JSON output. */
        private val RESPONSE_SCHEMA: JsonObject = buildJsonObject {
            put("type", "OBJECT")
            putJsonObject("properties") {
                putJsonObject("transactions") {
                    put("type", "ARRAY")
                    putJsonObject("items") {
                        put("type", "OBJECT")
                        putJsonObject("properties") {
                            putJsonObject("description") { put("type", "STRING") }
                            putJsonObject("amountCop") { put("type", "INTEGER") }
                            putJsonObject("categoryName") { put("type", "STRING") }
                            putJsonObject("isIncome") { put("type", "BOOLEAN") }
                            putJsonObject("needsReview") { put("type", "BOOLEAN") }
                        }
                        putJsonArray("required") {
                            add("description"); add("amountCop"); add("isIncome")
                        }
                        putJsonArray("propertyOrdering") {
                            add("description"); add("amountCop"); add("categoryName")
                            add("isIncome"); add("needsReview")
                        }
                    }
                }
            }
            putJsonArray("required") { add("transactions") }
        }
    }
}
