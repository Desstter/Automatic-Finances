package com.example.automaticfinances.data.repo

import com.example.automaticfinances.data.models.AdvisorUiState
import com.example.automaticfinances.data.models.AiFinancialInsights
import com.example.automaticfinances.data.models.AiTip
import com.example.automaticfinances.data.models.AiTone
import com.example.automaticfinances.data.models.InsightsReport
import com.example.automaticfinances.data.preferences.AiPreferences
import com.example.automaticfinances.data.remote.LlmException
import com.example.automaticfinances.data.remote.LlmFailure
import com.example.automaticfinances.data.remote.LlmJsonClient
import com.example.automaticfinances.utils.centsToCopString
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The "intelligence" layer on top of [InsightsRepository]: it takes the deterministic
 * [InsightsReport] (run-rate, subscriptions, anomalies) and asks the LLM ([DeepSeek][LlmJsonClient])
 * to turn the raw numbers into a short, friendly Colombian-Spanish narrative plus a handful of
 * concrete recommendations.
 *
 * Design mirrors [com.example.automaticfinances.data.voice.VoiceTransactionParser]: no DB coupling,
 * the model only sees a compact pre-computed summary (never raw transactions), and the result is a
 * pure domain object. It degrades gracefully — returns `null` when the advisor is disabled, no key
 * is configured, or the call fails — so callers can simply hide the AI card.
 */
@Singleton
class FinancialAdvisorRepository @Inject constructor(
    private val llmClient: LlmJsonClient,
    private val aiPreferences: AiPreferences,
    private val json: Json,
) {

    /**
     * Turns the deterministic [report] into a narrative. Never throws: the outcome is encoded in the
     * returned [AdvisorUiState] so the dashboard can hide the section ([Hidden][AdvisorUiState.Hidden])
     * or offer the right recovery ([Error][AdvisorUiState.Error] carries the typed [LlmFailure]).
     */
    suspend fun advise(report: InsightsReport, userName: String? = null, force: Boolean = false): AdvisorUiState {
        if (!aiPreferences.isAdvisorEnabled()) return AdvisorUiState.Hidden
        // Nothing meaningful to analyze yet.
        if (report.digest.transactionCount == 0) return AdvisorUiState.Hidden

        // Unless the user explicitly asked for a refresh, reuse the persisted narrative when the
        // month's figures haven't moved. This is what keeps the advisor from calling the LLM on every
        // single app open — only a genuine change in the data (new signature) triggers a fresh call.
        if (!force) {
            cachedAdviceFor(report)?.let { return it }
        }

        return try {
            val raw = llmClient.generateStructuredJson(
                systemInstruction = systemInstruction(userName),
                prompt = buildSummaryPrompt(report),
                responseSchema = RESPONSE_SCHEMA,
                temperature = 0.4,
            )
            val insights = json.decodeFromString(AdvisorResultDto.serializer(), raw).toDomainOrNull()
            if (insights != null) {
                aiPreferences.setAdvisorCache(signatureFor(report), raw)
                AdvisorUiState.Success(insights)
            } else AdvisorUiState.Error(LlmFailure.EMPTY)
        } catch (e: LlmException) {
            AdvisorUiState.Error(e.failure)
        } catch (e: Exception) {
            AdvisorUiState.Error(LlmFailure.UNKNOWN)
        }
    }

    /**
     * The advice cached for [report] if its signature still matches, else null. Lets a caller (the
     * dashboard) show the prior narrative instantly on open without a `Loading` flash or a network
     * round-trip. Returns null when the advisor is disabled or there's nothing to analyze.
     */
    suspend fun cachedAdviceFor(report: InsightsReport): AdvisorUiState.Success? {
        if (!aiPreferences.isAdvisorEnabled() || report.digest.transactionCount == 0) return null
        val (sig, cachedJson) = aiPreferences.getAdvisorCache() ?: return null
        if (sig != signatureFor(report)) return null
        return try {
            json.decodeFromString(AdvisorResultDto.serializer(), cachedJson).toDomainOrNull()
                ?.let { AdvisorUiState.Success(it) }
        } catch (e: Exception) {
            null
        }
    }

    /**
     * A compact fingerprint of the figures the advice depends on. Same signature → same narrative, so
     * we can safely reuse the cache; any movement in spend/income/top-category/counts invalidates it.
     */
    fun signatureFor(report: InsightsReport): String {
        val d = report.digest
        return "${d.monthLabel}|${d.spentMtdCents}|${d.incomeMtdCents}|${d.topCategoryCents}|" +
            "${d.expenseCount}|${report.subscriptions.size}|${report.anomalies.size}"
    }

    private fun systemInstruction(userName: String?): String {
        val who = userName?.trim()?.takeIf { it.isNotEmpty() }?.let { " El usuario se llama $it; puedes tutearlo por su nombre." } ?: ""
        return """
            Eres un asesor financiero personal para un usuario en Colombia. Recibes un resumen ya
            calculado de las finanzas del mes (montos en pesos colombianos, COP).$who

            Tu tarea: explicar de forma breve, cercana y útil cómo va el mes y dar recomendaciones
            accionables. Habla en español colombiano, en segunda persona, claro y sin tecnicismos.

            REGLAS:
            - "summary": 1 o 2 frases que resuman el estado del mes (ritmo de gasto, balance).
            - "tips": entre 2 y 4 recomendaciones. Sé CONCRETO: cuando el resumen lo permita, menciona
              la categoría o el comercio exacto y su monto (ej. "gastaste $120.000 en Rappi"), en vez
              de consejos genéricos. Prioriza lo más relevante: la categoría/comercio donde más se va
              la plata, suscripciones que se acumulan o cargos raros. Cada tip con:
                - "title": titular muy corto (máx 6 palabras).
                - "body": una frase concreta y accionable (máx 25 palabras).
                - "tone": "POSITIVE" si es algo que va bien (ahorro, vas por debajo del mes pasado),
                  "WARNING" si es algo a vigilar (sobregasto, suscripciones, cargos raros),
                  "INFO" para contexto neutral.
            - Usa SOLO las cifras, categorías y comercios del resumen; no inventes datos. No juzgues
              ni moralices sobre en qué se gasta, solo analiza. No prometas rendimientos ni inversiones.
            - No uses emojis. No uses markdown. Responde SOLO con el JSON.
        """.trimIndent()
    }

    private fun buildSummaryPrompt(report: InsightsReport): String {
        val d = report.digest
        val sb = StringBuilder()
        sb.appendLine("RESUMEN FINANCIERO DEL MES (${d.monthLabel}):")
        sb.appendLine("- Gastado hasta hoy: ${d.spentMtdCents.centsToCopString()} en ${d.expenseCount} movimientos.")
        sb.appendLine("- Ingresos del mes: ${d.incomeMtdCents.centsToCopString()}.")
        sb.appendLine("- Balance (ingresos - gastos): ${d.netBalanceCents.centsToCopString()}.")
        sb.appendLine("- Proyección de cierre de mes a este ritmo: ${d.projectedMonthEndCents.centsToCopString()}.")
        if (d.lastMonthTotalCents > 0) {
            val dir = when {
                d.projectedVsLastMonthPct > 0 -> "${d.projectedVsLastMonthPct}% MÁS"
                d.projectedVsLastMonthPct < 0 -> "${-d.projectedVsLastMonthPct}% MENOS"
                else -> "igual"
            }
            sb.appendLine("- El mes pasado gastaste ${d.lastMonthTotalCents.centsToCopString()} (proyección: $dir que el mes pasado).")
        }
        d.topCategoryName?.let {
            if (d.topCategoryCents > 0) sb.appendLine("- Categoría con más gasto: $it (${d.topCategoryCents.centsToCopString()}).")
        }

        if (d.topCategories.isNotEmpty()) {
            sb.appendLine("- Gasto por categoría (de mayor a menor):")
            d.topCategories.forEach { sb.appendLine("    • ${it.name}: ${it.amountCents.centsToCopString()}.") }
        }
        if (report.topMerchants.isNotEmpty()) {
            sb.appendLine("- Dónde más gastaste (comercios):")
            report.topMerchants.forEach { sb.appendLine("    • ${it.name}: ${it.amountCents.centsToCopString()}.") }
        }

        if (report.subscriptions.isNotEmpty()) {
            sb.appendLine("- Suscripciones/cargos recurrentes detectados (${report.subscriptionsMonthlyTotalCents.centsToCopString()}/mes):")
            report.subscriptions.take(8).forEach {
                sb.appendLine("    • ${it.merchantName}: ${it.monthlyAmountCents.centsToCopString()}/mes.")
            }
        }
        if (report.anomalies.isNotEmpty()) {
            sb.appendLine("- Cargos que llamaron la atención:")
            report.anomalies.take(5).forEach { sb.appendLine("    • ${it.message}") }
        }
        return sb.toString().trim()
    }

    // ---- Wire DTOs for the structured-output payload (private to this layer) ----

    @Serializable
    private data class AdvisorResultDto(
        val summary: String = "",
        val tips: List<TipDto> = emptyList(),
    ) {
        fun toDomainOrNull(): AiFinancialInsights? {
            val cleanSummary = summary.trim()
            val cleanTips = tips.mapNotNull { it.toDomainOrNull() }
            if (cleanSummary.isEmpty() && cleanTips.isEmpty()) return null
            return AiFinancialInsights(summary = cleanSummary, tips = cleanTips)
        }
    }

    @Serializable
    private data class TipDto(
        val title: String = "",
        val body: String = "",
        val tone: String = "INFO",
    ) {
        fun toDomainOrNull(): AiTip? {
            val t = title.trim()
            val b = body.trim()
            if (t.isEmpty() && b.isEmpty()) return null
            val tone = when (tone.trim().uppercase()) {
                "POSITIVE" -> AiTone.POSITIVE
                "WARNING" -> AiTone.WARNING
                else -> AiTone.INFO
            }
            return AiTip(title = t.ifEmpty { b.take(40) }, body = b, tone = tone)
        }
    }

    companion object {
        /** JSON-mode hint embedded in the prompt (DeepSeek doesn't enforce a formal schema). */
        private val RESPONSE_SCHEMA: JsonObject = buildJsonObject {
            put("type", "OBJECT")
            putJsonObject("properties") {
                putJsonObject("summary") { put("type", "STRING") }
                putJsonObject("tips") {
                    put("type", "ARRAY")
                    putJsonObject("items") {
                        put("type", "OBJECT")
                        putJsonObject("properties") {
                            putJsonObject("title") { put("type", "STRING") }
                            putJsonObject("body") { put("type", "STRING") }
                            putJsonObject("tone") {
                                put("type", "STRING")
                                putJsonArray("enum") { add("POSITIVE"); add("WARNING"); add("INFO") }
                            }
                        }
                        putJsonArray("required") { add("title"); add("body"); add("tone") }
                    }
                }
            }
            putJsonArray("required") { add("summary"); add("tips") }
        }
    }
}
