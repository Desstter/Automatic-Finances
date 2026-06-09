package com.example.automaticfinances.data.parse

import com.example.automaticfinances.data.db.Transaction
import org.apache.commons.codec.digest.DigestUtils
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * Pure SMS/notification parser. It only performs regex extraction and produces a
 * [Transaction] with `categoryId` and `accountId` left null — those are resolved downstream
 * by [com.example.automaticfinances.domain.AddTransactionUseCase], which owns the DB
 * dependencies. Keeping the parser free of database/DI coupling makes it deterministic and
 * unit-testable, and removes the fragile static `AppDatabase.get()` singleton dependency.
 */
object BancolombiaParser {

    // -------------------------------------------
    // Flags y utilidades para Regex
    // -------------------------------------------
    private val RX_FLAGS = setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)

    // Cola de merchant reutilizable: captura el nombre COMPLETO del comercio hasta el primer
    // delimitador estructural (marcadores "el/por/con", puntuación, salto de línea o fin de
    // texto). Reemplaza al antiguo `\ben\s+(.+?)\b` que solo capturaba la primera palabra.
    private const val MERCHANT_TAIL = """(.+?)(?=\s+(?:el|por|con|usando|a\s+las)\b|[.,;\n]|$)"""

    // -------------------------------------------
    // Filtros de mensajes inválidos (rechazos, anulaciones, etc.)
    // -------------------------------------------
    private val NEGATIVE_MARKERS = listOf(
        "rechazada", "no aprobada", "reversada", "anulada",
        "fallida", "cancelada", "declinada", "bloqueada",
        "preautoriz", "pre-autoriz", "intento de compra"
    )

    private fun looksInvalid(text: String): Boolean {
        val t = text.lowercase()
        return NEGATIVE_MARKERS.any { t.contains(it) }
    }

    // -------------------------------------------
    // REGEX — Compras / Transferencias (Bancolombia)
    // -------------------------------------------

    // Compra SMS clásica Bancolombia. El verbo cubre "Compraste" y "Pagaste" (Bancolombia usa ambos
    // para débitos por compra/pago con tarjeta; mismo formato de tarjeta + fecha).
    private val compraRegex = Regex(
        """Bancolombia:\s*(?:Compraste|Pagaste)\s*(?:COP|\${'$'}COP|\${'$'})?\s*([\d\.,]+).*?\ben\s+(.+?)\s+con\s+tu\s+(?:T\.?\s*(?:Cred(?:ito)?|Cr[eé]dito)|T\.?\s*(?:D[eé]bito)|Tarjeta\s+(?:Cr[eé]dito|D[eé]bito))\s*[*Xx]{0,4}(\d{4}),?\s+el\s+(\d{2}[/-]\d{2}[/-]\d{4})\s+a\s+las\s+(\d{2}:\d{2})""",
        RX_FLAGS
    )

    // Compra SMS nuevo formato Bancolombia (fecha antes de la tarjeta):
    // "Compraste COP{amount} en {merchant}, el {date} a las {time}. Esta compra esta asociada a T.Cred *{last4}."
    private val compraNuevoFormatoRegex = Regex(
        """Bancolombia:\s*(?:Compraste|Pagaste)\s*(?:COP|\${'$'}COP|\${'$'})?\s*([\d\.,]+)\s+en\s+(.+?),\s+el\s+(\d{2}[/-]\d{2}[/-]\d{4})\s+a\s+las\s+(\d{2}:\d{2}).*?T\.?\s*(?:Cred(?:ito)?|Cr[eé]dito|D[eé]bito)\s*[*Xx]{0,4}(\d{4})""",
        RX_FLAGS
    )

    // Transferencia SMS clásica
    private val transRegex = Regex(
        """Bancolombia:\s*Transferiste\s*(?:COP|\${'$'}COP|\${'$'})?\s*([\d\.,]+).*?desde\s+tu\s+cuenta.*?[*Xx]{0,4}(\d{4}).*?(?:hacia|a)\s+la\s+cuenta.*?[*Xx]{0,4}(\d+)\s+el\s+(\d{2}[/-]\d{2}[/-]\d{4})\s+a\s+las\s+(\d{2}:\d{2})""",
        RX_FLAGS
    )

    // App: compra
    private val appCompraRegex = Regex(
        """Compra\s+(?:por\s+)?(?:COP|\${'$'}COP|\${'$'})?\s*([\d\.,]+).*?(?:en\s+|en\s+el\s+comercio\s+)(.+?)\s+(?:con\s+tu\s+tarjeta\s+terminada\s+en\s+|[*Xx]?)(\d{4})\b""",
        RX_FLAGS
    )

    // App: transferencia
    private val appTransRegex = Regex(
        """Transferencia\s+(?:por\s+)?(?:COP|\${'$'}COP|\${'$'})?\s*([\d\.,]+).*?desde\s+[*Xx]?0*(\d{4}).*?(?:hacia|a)\s+[*Xx]?0*(\d+)""",
        RX_FLAGS
    )

    // Formato “Bancolombia le informa Transferencia por $... desde cta *XXXX a cta NNN... . dd/mm/yyyy hh:mm”
    private val smsTransLeInformaRegex = Regex(
        """Bancolombia\s+le\s+informa\s+Transferencia\s+por\s+(?:COP|\${'$'}COP|\${'$'})?\s*([\d\.,]+).*?desde\s+(?:cta|cuenta)\s+[*Xx]?0*(\d{4})\s+a\s+(?:cta|cuenta)\s+(\d+)\.\s+(\d{2}/\d{2}/\d{4})\s+(\d{2}:\d{2})""",
        RX_FLAGS
    )

    // Retiro cajero (ej: Retiraste $50.000,00 en METR_LA70_1 de tu T.Deb **6045 el 19/08/2025 a las 16:11)
    private val retiroCajeroRegex = Regex(
        """Bancolombia:\s*Retiraste\s*(?:COP|\${'$'}COP|\${'$'})?\s*([\d\.,]+)\s*.*?\ben\s+(.+?)\s+de\s+tu\s+(?:T\.?\s*Deb(?:ito)?|Tarjeta\s+D[eé]bito|T\.?\s*Cred(?:ito)?|Tarjeta\s+Cr[eé]dito)\s*[*Xx]{0,4}(\d{4})\s+el\s+(\d{2}[/-]\d{2}[/-]\d{4})\s+a\s+las\s+(\d{2}:\d{2})""",
        RX_FLAGS
    )

    // -------------------------------------------
    // REGEX — Ingresos (Bancolombia)
    // -------------------------------------------

    // Nómina (sin last4, y con "en tu cuenta de Ahorros ... el dd/mm/yyyy a las hh:mm")
    private val ingresoNominaRegex = Regex(
        """Bancolombia:\s*Recibiste\s+un\s+pago\s+de\s+N[oó]mina\s+de\s+(.+?)\s+por\s+(?:COP|\${'$'}COP|\${'$'})?\s*([\d\.,]+).*?en\s+tu\s+cuenta(?:\s+de\s+\w+)?(?:\s+\*?(\d{4}))?.*?el\s+(\d{2}/\d{2}/\d{4})\s+a\s+las\s+(\d{2}:\d{2})""",
        RX_FLAGS
    )

    // Transferencia recibida (formato “Bancolombia te informa recepción transferencia de ... por $... en la cuenta *XXXX. dd/mm/yyyy hh:mm”)
    private val smsIngresoRecepcionRegex = Regex(
        """Bancolombia\s+te\s+informa\s+recep(?:ci[oó]n)?\s+transferencia\s+de\s+(.+?)\s+por\s+(?:COP|\${'$'}COP|\${'$'})?\s*([\d\.,]+)\s+en\s+la\s+cuenta\s+[*Xx]?0*(\d{4})\.\s+(\d{2}/\d{2}/\d{4})\s+(\d{2}:\d{2})""",
        RX_FLAGS
    )

    // Ingreso transferencia con fecha/hora (versión genérica)
    private val ingresoTransferenciaRegex = Regex(
        """Bancolombia:\s*(?:Recibiste|Te\s+transfirieron)\s*(?:COP|\${'$'}COP|\${'$'})?\s*([\d\.,]+).*?a\s+tu\s+cuenta\s+[*Xx]?0*(\d{4})\s+el\s+(\d{2}[/-]\d{2}[/-]\d{4})\s+a\s+las\s+(\d{2}:\d{2})""",
        RX_FLAGS
    )

    // Depósito/Consignación con fecha/hora
    private val ingresoDepositoRegex = Regex(
        """Bancolombia:\s*(?:Dep[oó]sito|Consignaci[oó]n|Consignaste)\s*(?:COP|\${'$'}COP|\${'$'})?\s*([\d\.,]+).*?en\s+tu\s+cuenta\s+[*Xx]?0*(\d{4})\s+el\s+(\d{2}[/-]\d{2}[/-]\d{4})\s+a\s+las\s+(\d{2}:\d{2})""",
        RX_FLAGS
    )

    // App ingreso (sin fecha/hora)
    private val appIngresoRegex = Regex(
        """(?:Transferencia\s+recibida|Dep[oó]sito)\s+(?:por\s+)?(?:COP|\${'$'}COP|\${'$'})?\s*([\d\.,]+).*?en\s+[*Xx]?0*(\d{4})\b""",
        RX_FLAGS
    )

    // -------------------------------------------
    // REGEX — Otros (Nequi / DaviPlata)
    // -------------------------------------------
    private val nequiRegex = Regex(
        """Nequi:\s*(?:Pagaste|Pago)\s+(?:COP|\${'$'}COP|\${'$'})?\s*([\d\.,]+).*?\ben\s+$MERCHANT_TAIL""",
        RX_FLAGS
    )

    private val nequiIngresoRegex = Regex(
        """Nequi:\s*(?:Recibiste|Te\s+enviaron)\s+(?:COP|\${'$'}COP|\${'$'})?\s*([\d\.,]+).*?\bde\s+$MERCHANT_TAIL""",
        RX_FLAGS
    )

    private val daviRegex = Regex(
        """DaviPlata:\s*(?:Compraste|Pago)\s+(?:COP|\${'$'}COP|\${'$'})?\s*([\d\.,]+).*?\ben\s+$MERCHANT_TAIL""",
        RX_FLAGS
    )

    private val daviIngresoRegex = Regex(
        """DaviPlata:\s*(?:Recibiste|Te\s+enviaron)\s+(?:COP|\${'$'}COP|\${'$'})?\s*([\d\.,]+).*?\bde\s+$MERCHANT_TAIL""",
        RX_FLAGS
    )

    // -------------------------------------------
    // REGEX — Nu (Nubank Colombia)
    // -------------------------------------------
    // Nu identifica su app por paquete (com.nu.production), pero el texto del push/SMS suele
    // mencionar "Nu"/"Nubank". Gate por palabra completa para no chocar con "nunca", "minuto", etc.
    private val nuWordRegex = Regex("""\bNu\b""", setOf(RegexOption.IGNORE_CASE))

    // Egreso Nu: "Compraste/Pagaste/Pago/Realizaste una compra de $X en COMERCIO".
    // NOTA: formato aproximado a falta del texto exacto del usuario; afínalo con un mensaje real.
    // La red de seguridad (Mensajes no reconocidos accionables) cubre cualquier formato que no calce.
    private val nuEgresoRegex = Regex(
        """\b(?:Compraste|Pagaste|Pago|Realizaste\s+(?:una\s+)?compra\s+de)\s+(?:COP|\${'$'}COP|\${'$'})?\s*([\d\.,]+).*?\ben\s+$MERCHANT_TAIL""",
        RX_FLAGS
    )

    // Ingreso Nu: "Recibiste/Te enviaron/Te transfirieron $X (de REMITENTE)". El remitente es opcional.
    private val nuIngresoRegex = Regex(
        """\b(?:Recibiste|Te\s+enviaron|Te\s+transfirieron)\s+(?:un[a]?\s+(?:pago|transferencia)\s+de\s+)?(?:COP|\${'$'}COP|\${'$'})?\s*([\d\.,]+)(?:.*?\bde\s+$MERCHANT_TAIL)?""",
        RX_FLAGS
    )

    // Nu "Pago aprobado": el monto está en la primera frase y el comercio en la segunda.
    // Formato real: "Pago aprobado por $23.319,91\nPageste en COMERCIO con tu cuenta de ahorros."
    private val nuPagoAprobadoRegex = Regex(
        """Pago\s+aprobado\s+por\s+(?:\${'$'})?([\d\.,]+).*?Pagaste\s+en\s+(.+?)\s+con\s+tu\b""",
        RX_FLAGS
    )

    // -------------------------------------------
    // REGEX — Retiro de efectivo en cajero (genérico, multi-emisor)
    // -------------------------------------------
    // Push de retiro sin tarjeta ni fecha, p.ej. Nequi: título "Retiro en Cajero" + cuerpo
    // "Sacaste $200000". El texto ensamblado por el listener NO contiene la palabra "Nequi"
    // (eso es solo el nombre de la app), por eso este detector es genérico y se basa en los
    // verbos "Sacaste"/"Retiro en cajero"/"Retiraste". Produce type = "RETIRO" para que
    // AddTransactionUseCase lo registre como movimiento interno Banco -> Efectivo (doble pierna,
    // isTransfer), sin contar como gasto ni ingreso.
    private val retiroEfectivoRegex = Regex(
        """(?:Sacaste|Retiro\s+en\s+(?:el\s+)?cajero|Retiraste)\b.*?(?:COP|\${'$'}COP|\${'$'})?\s*([\d\.,]+)""",
        RX_FLAGS
    )

    // -------------------------------------------
    // REGEX — Fallback genérico (lee todos los mensajes)
    // -------------------------------------------
    private val genericCompraRegex = Regex(
        """\b(Compra(?:ste)?|Pagaste|Pago)\b.*?(?:COP|\${'$'}COP|\${'$'})?\s*([\d\.,]+).*?(?:en\s+|en\s+el\s+comercio\s+)(.+?)\s+(?:con\s+tu\s+.*?(?:terminada\s+en\s+)?|[*Xx]?)(\d{4})\b""",
        RX_FLAGS
    )

    // -------------------------------------------
    // ENTRYPOINTS
    // -------------------------------------------
    /**
     * @param now timestamp to use for notifications that don't carry their own date/time
     *            (app pushes, Nequi, DaviPlata, generic fallback). Pass the notification's
     *            postTime so re-delivered notifications yield the SAME stable id (no duplicates).
     *            Defaults to the wall clock for callers that don't have a postTime.
     */
    fun tryParse(text: String, now: Long = System.currentTimeMillis()): Transaction? {
        if (looksInvalid(text)) return null

        return try {
            // Orden: Bancolombia → Nequi → DaviPlata → Ingresos → Fallback genérico
            tryParseBancolombia(text, now)
                ?: tryParseNequi(text, now)
                ?: tryParseDaviPlata(text, now)
                ?: tryParseIngresosBancolombia(text, now)
                ?: tryParseIngresosNequi(text, now)
                ?: tryParseIngresosDaviPlata(text, now)
                ?: tryParseNu(text, now)
                ?: tryParseRetiroEfectivo(text, now)
                ?: tryParseFallbackGenerico(text, now)
        } catch (e: Exception) {
            android.util.Log.e("BancolombiaParser", "Error parsing text", e)
            null
        }
    }

    // -------------------------------------------
    // Parsers por emisor
    // -------------------------------------------
    private fun tryParseBancolombia(text: String, now: Long): Transaction? {
        // Heurística de activación
        if (
            !text.contains("Bancolombia", ignoreCase = true) &&
            !text.contains("Compra", ignoreCase = true) &&
            !text.contains("Pagaste", ignoreCase = true) &&
            !text.contains("Transferencia", ignoreCase = true) &&
            !text.contains("Retiraste", ignoreCase = true)
        ) return null

        // Retiro cajero
        retiroCajeroRegex.find(text)?.let { m ->
            val amount = toCents(m.groupValues[1])
            val atm = norm(m.groupValues[2])
            val last4 = m.groupValues[3]
            val ts = toEpoch(m.groupValues[4], m.groupValues[5])
            val id = stableId(ts, amount, "RETIRO", last4, atm, text)

            return Transaction.fromTimestamp(
                id = id,
                ts = ts,
                type = "RETIRO",
                description = "Retiro cajero $atm",
                amountCents = amount,
                currency = "COP",
                srcLast4 = last4,
                dstLast4 = null,
                source = "notif:sms",
                rawPreview = text.take(140)
            )
        }

        // “Bancolombia le informa Transferencia...”
        smsTransLeInformaRegex.find(text)?.let { m ->
            val amount = toCents(m.groupValues[1])
            val src = m.groupValues[2]
            val dst = m.groupValues[3].takeLast(4)
            val ts = toEpoch(m.groupValues[4], m.groupValues[5])
            val id = stableId(ts, amount, "TRANSFERENCIA", src, null, text)

            return Transaction.fromTimestamp(
                id = id,
                ts = ts,
                type = "TRANSFERENCIA",
                description = "Transferencia a *$dst",
                amountCents = amount,
                currency = "COP",
                srcLast4 = src,
                dstLast4 = dst,
                source = "notif:sms",
                rawPreview = text.take(140)
            )
        }

        // Compra SMS tradicional
        compraRegex.find(text)?.let { m ->
            val amount = toCents(m.groupValues[1])
            val merchant = norm(m.groupValues[2])
            val last4 = m.groupValues[3]
            val ts = toEpoch(m.groupValues[4], m.groupValues[5])
            val id = stableId(ts, amount, "COMPRA", last4, merchant, text)

            return Transaction.fromTimestamp(
                id = id,
                ts = ts,
                type = "COMPRA",
                description = merchant,
                amountCents = amount,
                currency = "COP",
                srcLast4 = last4,
                dstLast4 = null,
                source = "notif:sms",
                rawPreview = text.take(140)
            )
        }

        // Compra SMS nuevo formato (fecha antes que la tarjeta)
        compraNuevoFormatoRegex.find(text)?.let { m ->
            val amount = toCents(m.groupValues[1])
            val merchant = norm(m.groupValues[2])
            val ts = toEpoch(m.groupValues[3], m.groupValues[4])
            val last4 = m.groupValues[5]
            val id = stableId(ts, amount, "COMPRA", last4, merchant, text)

            return Transaction.fromTimestamp(
                id = id,
                ts = ts,
                type = "COMPRA",
                description = merchant,
                amountCents = amount,
                currency = "COP",
                srcLast4 = last4,
                dstLast4 = null,
                source = "notif:sms",
                rawPreview = text.take(140)
            )
        }

        // Transferencia SMS tradicional
        transRegex.find(text)?.let { m ->
            val amount = toCents(m.groupValues[1])
            val src = m.groupValues[2]
            val dst = m.groupValues[3].takeLast(4)
            val ts = toEpoch(m.groupValues[4], m.groupValues[5])
            val id = stableId(ts, amount, "TRANSFERENCIA", src, null, text)

            return Transaction.fromTimestamp(
                id = id,
                ts = ts,
                type = "TRANSFERENCIA",
                description = "Transferencia a *$dst",
                amountCents = amount,
                currency = "COP",
                srcLast4 = src,
                dstLast4 = dst,
                source = "notif:sms",
                rawPreview = text.take(140)
            )
        }

        // Notificación app: compra
        appCompraRegex.find(text)?.let { m ->
            val amount = toCents(m.groupValues[1])
            val merchant = norm(m.groupValues[2])
            val last4 = m.groupValues[3]
            val ts = now
            val id = stableId(ts, amount, "COMPRA", last4, merchant, text)

            return Transaction.fromTimestamp(
                id = id,
                ts = ts,
                type = "COMPRA",
                description = merchant,
                amountCents = amount,
                currency = "COP",
                srcLast4 = last4,
                dstLast4 = null,
                source = "notif:app",
                rawPreview = text.take(140)
            )
        }

        // Notificación app: transferencia
        appTransRegex.find(text)?.let { m ->
            val amount = toCents(m.groupValues[1])
            val src = m.groupValues[2]
            val dst = m.groupValues[3].takeLast(4)
            val ts = now
            val id = stableId(ts, amount, "TRANSFERENCIA", src, null, text)

            return Transaction.fromTimestamp(
                id = id,
                ts = ts,
                type = "TRANSFERENCIA",
                description = "Transferencia a *$dst",
                amountCents = amount,
                currency = "COP",
                srcLast4 = src,
                dstLast4 = dst,
                source = "notif:app",
                rawPreview = text.take(140)
            )
        }

        return null
    }

    private fun tryParseNequi(text: String, now: Long): Transaction? {
        if (!text.contains("Nequi", ignoreCase = true)) return null

        nequiRegex.find(text)?.let { m ->
            val amount = toCents(m.groupValues[1])
            val merchant = norm(m.groupValues[2])
            val ts = now
            val id = stableId(ts, amount, "COMPRA", "NEQU", merchant, text)

            return Transaction.fromTimestamp(
                id = id,
                ts = ts,
                type = "COMPRA",
                description = merchant,
                amountCents = amount,
                currency = "COP",
                srcLast4 = "NEQU",
                dstLast4 = null,
                source = "notif:nequi",
                rawPreview = text.take(140)
            )
        }

        return null
    }

    private fun tryParseDaviPlata(text: String, now: Long): Transaction? {
        if (!text.contains("DaviPlata", ignoreCase = true)) return null

        daviRegex.find(text)?.let { m ->
            val amount = toCents(m.groupValues[1])
            val merchant = norm(m.groupValues[2])
            val ts = now
            val id = stableId(ts, amount, "COMPRA", "DAVI", merchant, text)

            return Transaction.fromTimestamp(
                id = id,
                ts = ts,
                type = "COMPRA",
                description = merchant,
                amountCents = amount,
                currency = "COP",
                srcLast4 = "DAVI",
                dstLast4 = null,
                source = "notif:daviPlata",
                rawPreview = text.take(140)
            )
        }

        return null
    }

    private fun tryParseIngresosBancolombia(text: String, now: Long): Transaction? {
        if (
            !text.contains("Bancolombia", ignoreCase = true) &&
            !text.contains("Recibiste", ignoreCase = true) &&
            !text.contains("Depósito", ignoreCase = true) &&
            !text.contains("Consignación", ignoreCase = true) &&
            !text.contains("recepcion transferencia", ignoreCase = true)
        ) return null

        // Nómina
        ingresoNominaRegex.find(text)?.let { m ->
            val employer = norm(m.groupValues[1])
            val amount = toCents(m.groupValues[2])
            val last4Opt = m.groupValues.getOrNull(3)?.takeIf { it.isNotBlank() }
            val ts = toEpoch(m.groupValues[4], m.groupValues[5])
            val id = stableId(ts, amount, "INGRESO_NOMINA", last4Opt, employer, text)

            return Transaction.fromTimestamp(
                id = id,
                ts = ts,
                type = "INGRESO",
                description = "Nómina $employer",
                amountCents = amount,
                currency = "COP",
                srcLast4 = null,
                dstLast4 = last4Opt,
                source = "notif:sms",
                rawPreview = text.take(140),
                isIncome = true
            )
        }

        // Recepción transferencia (“te informa”)
        smsIngresoRecepcionRegex.find(text)?.let { m ->
            val sender = norm(m.groupValues[1])
            val amount = toCents(m.groupValues[2])
            val dstLast4 = m.groupValues[3]
            val ts = toEpoch(m.groupValues[4], m.groupValues[5])
            val id = stableId(ts, amount, "INGRESO_TRANSFERENCIA", dstLast4, sender, text)

            return Transaction.fromTimestamp(
                id = id,
                ts = ts,
                type = "INGRESO",
                description = "Recibido de $sender",
                amountCents = amount,
                currency = "COP",
                srcLast4 = null,
                dstLast4 = dstLast4,
                source = "notif:sms",
                rawPreview = text.take(140),
                isIncome = true
            )
        }

        // Transferencia recibida genérica con fecha/hora
        ingresoTransferenciaRegex.find(text)?.let { m ->
            val amount = toCents(m.groupValues[1])
            val dstLast4 = m.groupValues[2]
            val ts = toEpoch(m.groupValues[3], m.groupValues[4])
            val id = stableId(ts, amount, "INGRESO_TRANSFERENCIA", dstLast4, null, text)

            return Transaction.fromTimestamp(
                id = id,
                ts = ts,
                type = "INGRESO",
                description = "Transferencia recibida",
                amountCents = amount,
                currency = "COP",
                srcLast4 = null,
                dstLast4 = dstLast4,
                source = "notif:sms",
                rawPreview = text.take(140),
                isIncome = true
            )
        }

        // Depósito/Consignación con fecha/hora
        ingresoDepositoRegex.find(text)?.let { m ->
            val amount = toCents(m.groupValues[1])
            val dstLast4 = m.groupValues[2]
            val ts = toEpoch(m.groupValues[3], m.groupValues[4])
            val id = stableId(ts, amount, "INGRESO_DEPOSITO", dstLast4, null, text)

            return Transaction.fromTimestamp(
                id = id,
                ts = ts,
                type = "INGRESO",
                description = "Depósito",
                amountCents = amount,
                currency = "COP",
                srcLast4 = null,
                dstLast4 = dstLast4,
                source = "notif:sms",
                rawPreview = text.take(140),
                isIncome = true
            )
        }

        // Ingreso app (sin fecha/hora)
        appIngresoRegex.find(text)?.let { m ->
            val amount = toCents(m.groupValues[1])
            val dstLast4 = m.groupValues[2]
            val ts = now
            val id = stableId(ts, amount, "INGRESO_APP", dstLast4, null, text)

            return Transaction.fromTimestamp(
                id = id,
                ts = ts,
                type = "INGRESO",
                description = "Transferencia recibida",
                amountCents = amount,
                currency = "COP",
                srcLast4 = null,
                dstLast4 = dstLast4,
                source = "notif:app",
                rawPreview = text.take(140),
                isIncome = true
            )
        }

        return null
    }

    private fun tryParseIngresosNequi(text: String, now: Long): Transaction? {
        if (!text.contains("Nequi", ignoreCase = true) || !text.contains("Recibiste", ignoreCase = true)) return null

        nequiIngresoRegex.find(text)?.let { m ->
            val amount = toCents(m.groupValues[1])
            val sender = norm(m.groupValues[2])
            val ts = now
            val id = stableId(ts, amount, "INGRESO_NEQUI", "NEQU", sender, text)

            return Transaction.fromTimestamp(
                id = id,
                ts = ts,
                type = "INGRESO",
                description = "Recibido de $sender",
                amountCents = amount,
                currency = "COP",
                srcLast4 = "NEQU",
                dstLast4 = null,
                source = "notif:nequi",
                rawPreview = text.take(140),
                isIncome = true
            )
        }

        return null
    }

    private fun tryParseIngresosDaviPlata(text: String, now: Long): Transaction? {
        if (!text.contains("DaviPlata", ignoreCase = true) || !text.contains("Recibiste", ignoreCase = true)) return null

        daviIngresoRegex.find(text)?.let { m ->
            val amount = toCents(m.groupValues[1])
            val sender = norm(m.groupValues[2])
            val ts = now
            val id = stableId(ts, amount, "INGRESO_DAVI", "DAVI", sender, text)

            return Transaction.fromTimestamp(
                id = id,
                ts = ts,
                type = "INGRESO",
                description = "Recibido de $sender",
                amountCents = amount,
                currency = "COP",
                srcLast4 = "DAVI",
                dstLast4 = null,
                source = "notif:daviPlata",
                rawPreview = text.take(140),
                isIncome = true
            )
        }

        return null
    }

    private fun tryParseNu(text: String, now: Long): Transaction? {
        if (!text.contains("nubank", ignoreCase = true) && !nuWordRegex.containsMatchIn(text)) return null

        // Ingreso primero: los verbos no se solapan con los de egreso, así que el orden es seguro.
        nuIngresoRegex.find(text)?.let { m ->
            val amount = toCents(m.groupValues[1])
            val sender = m.groupValues.getOrNull(2)?.takeIf { it.isNotBlank() }?.let { norm(it) }
            val ts = now
            val id = stableId(ts, amount, "INGRESO_NU", "NU", sender, text)

            return Transaction.fromTimestamp(
                id = id,
                ts = ts,
                type = "INGRESO",
                description = sender?.let { "Recibido de $it" } ?: "Transferencia recibida",
                amountCents = amount,
                currency = "COP",
                srcLast4 = "NU",
                dstLast4 = null,
                source = "notif:nu",
                rawPreview = text.take(140),
                isIncome = true
            )
        }

        // "Pago aprobado por $X ... Pagaste en COMERCIO con tu cuenta" (formato push Nu)
        nuPagoAprobadoRegex.find(text)?.let { m ->
            val amount = toCents(m.groupValues[1])
            val merchant = norm(m.groupValues[2])
            val ts = now
            val id = stableId(ts, amount, "COMPRA", "NU", merchant, text)

            return Transaction.fromTimestamp(
                id = id,
                ts = ts,
                type = "COMPRA",
                description = merchant,
                amountCents = amount,
                currency = "COP",
                srcLast4 = "NU",
                dstLast4 = null,
                source = "notif:nu",
                rawPreview = text.take(140)
            )
        }

        nuEgresoRegex.find(text)?.let { m ->
            val amount = toCents(m.groupValues[1])
            val merchant = norm(m.groupValues[2])
            val ts = now
            val id = stableId(ts, amount, "COMPRA", "NU", merchant, text)

            return Transaction.fromTimestamp(
                id = id,
                ts = ts,
                type = "COMPRA",
                description = merchant,
                amountCents = amount,
                currency = "COP",
                srcLast4 = "NU",
                dstLast4 = null,
                source = "notif:nu",
                rawPreview = text.take(140)
            )
        }

        return null
    }

    /**
     * Retiro de efectivo en cajero sin tarjeta ni fecha (p.ej. push de Nequi "Retiro en Cajero /
     * Sacaste $200000"). Se ejecuta DESPUÉS de los parsers por emisor: los retiros Bancolombia con
     * formato completo (tarjeta + fecha) ya los captura `retiroCajeroRegex`; esto es la red para
     * los pushes que solo traen verbo + monto. Emite type = "RETIRO" para que
     * [com.example.automaticfinances.domain.AddTransactionUseCase] lo trate como movimiento interno
     * Banco -> Efectivo (doble pierna marcada isTransfer), excluido de gastos/ingresos/charts.
     */
    private fun tryParseRetiroEfectivo(text: String, now: Long): Transaction? {
        val hasRetiroMarker = text.contains("Sacaste", ignoreCase = true) ||
            text.contains("Retiro en", ignoreCase = true) ||
            text.contains("Retiraste", ignoreCase = true)
        if (!hasRetiroMarker) return null

        retiroEfectivoRegex.find(text)?.let { m ->
            val amount = toCents(m.groupValues[1])
            if (amount <= 0) return null
            val ts = now
            val id = stableId(ts, amount, "RETIRO", null, "Cajero", text)

            return Transaction.fromTimestamp(
                id = id,
                ts = ts,
                type = "RETIRO",
                description = "Retiro cajero",
                amountCents = amount,
                currency = "COP",
                srcLast4 = null,
                dstLast4 = null,
                source = "notif:retiro",
                rawPreview = text.take(140)
            )
        }

        return null
    }

    private fun tryParseFallbackGenerico(text: String, now: Long): Transaction? {
        genericCompraRegex.find(text)?.let { m ->
            val amount = toCents(m.groupValues[2])
            val merchant = norm(m.groupValues[3])
            val last4 = m.groupValues[4]
            val ts = now
            val id = stableId(ts, amount, "COMPRA", last4, merchant, text)

            return Transaction.fromTimestamp(
                id = id,
                ts = ts,
                type = "COMPRA",
                description = merchant,
                amountCents = amount,
                currency = "COP",
                srcLast4 = last4,
                dstLast4 = null,
                source = "notif:generic",
                rawPreview = text.take(140)
            )
        }
        return null
    }

    // -------------------------------------------
    // Helpers
    // -------------------------------------------

    /**
     * Convierte un monto en string a centavos COP de forma robusta.
     *
     * Soporta:
     * - "39.500,00" (ES-CO) -> 3_950_000
     * - "39,500.00" (EN)    -> 3_950_000
     * - "50.000"            -> 5_000_000
     * - "50,000"            -> 5_000_000
     * - "50.000,5"          -> 5_000_050  (un decimal => se rellena a 2)
     */
    private fun toCents(s: String): Long {
        val raw = s.filter { it.isDigit() || it == '.' || it == ',' }
        if (raw.isEmpty()) return 0L

        val hasDot = raw.contains('.')
        val hasComma = raw.contains(',')

        if (!hasDot && !hasComma) {
            // Solo dígitos => no hay decimales explícitos
            return raw.toLong() * 100
        }

        val lastSepIdx = raw.indexOfLast { it == '.' || it == ',' }
        val rightDigits = raw.substring(lastSepIdx + 1).count(Char::isDigit)
        val both = hasDot && hasComma

        val isDecimalSeparator = when {
            both -> true // Si hay ambos, el último separador casi siempre es el decimal
            rightDigits in 1..2 -> true // 1–2 dígitos a la derecha => decimal
            else -> false // probablemente separador de miles
        }

        return if (isDecimalSeparator) {
            val intPart = raw.substring(0, lastSepIdx).filter(Char::isDigit).ifEmpty { "0" }
            val frac = raw.substring(lastSepIdx + 1).filter(Char::isDigit).padEnd(2, '0').take(2)
            (intPart + frac).toLong()
        } else {
            raw.filter(Char::isDigit).toLong() * 100
        }
    }

    private fun toEpoch(date: String, time: String): Long {
        val patterns = listOf("dd/MM/yyyy HH:mm", "dd-MM-yyyy HH:mm")
        val ldt = patterns.asSequence()
            .map { DateTimeFormatter.ofPattern(it) }
            .mapNotNull { fmt -> runCatching { LocalDateTime.parse("$date $time", fmt) }.getOrNull() }
            .firstOrNull()
            ?: return System.currentTimeMillis() // Fallback defensivo
        return ldt.atZone(ZoneId.of("America/Bogota")).toInstant().toEpochMilli()
    }

    private fun norm(x: String) = x.trim().replace(Regex("\\s+"), " ").take(60)

    private fun hash(x: String) = DigestUtils.sha256Hex(x)

    /**
     * ID estable que mezcla minuto, datos clave y un hash del preview normalizado.
     * Reduce duplicados por reprocesamiento con distinta hora exacta.
     */
    private fun stableId(
        ts: Long,
        amount: Long,
        kind: String,
        last4: String?,
        merchantOrNote: String?,
        raw: String
    ): String {
        val minute = ts / 60000
        val previewHash = DigestUtils.sha256Hex(norm(raw).take(120))
        return hash("$minute|$amount|$kind|${last4.orEmpty()}|${merchantOrNote.orEmpty()}|$previewHash")
    }
}
