package com.example.automaticfinances.data.parse

import com.example.automaticfinances.data.db.Transaction
import com.example.automaticfinances.data.repo.CategoryRepository
import kotlinx.coroutines.runBlocking
import org.apache.commons.codec.digest.DigestUtils
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

object BancolombiaParser {

    // Regex para SMS tradicionales
    private val compraRegex = Regex(
        """Bancolombia:\s*Compraste\s*(?:COP|\${'$'}COP|\${'$'})?\s*([\d\.,]+).*?\sen\s+(.+?)\s+con\s+tu\s+T\.?Cred\s+\*(\d{4}),\s+el\s+(\d{2}/\d{2}/\d{4})\s+a\s+las\s+(\d{2}:\d{2})""",
        setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)
    )

    private val transRegex = Regex(
        """Bancolombia:\s*Transferiste\s*(?:COP|\${'$'}COP|\${'$'})?\s*([\d\.,]+).*?desde\s+tu\s+cuenta\s+\*(\d{4})\s+a\s+la\s+cuenta\s+\*(\d+)\s+el\s+(\d{2}/\d{2}/\d{4})\s+a\s+las\s+(\d{2}:\d{2})""",
        setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)
    )

    // Regex para notificaciones de la app Bancolombia (formato más simple)
    private val appCompraRegex = Regex(
        """Compra\s+(?:por\s+)?(?:COP|\${'$'}COP|\${'$'})?\s*([\d\.,]+).*?en\s+(.+?)\s+\*(\d{4})""",
        setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)
    )

    private val appTransRegex = Regex(
        """Transferencia\s+(?:por\s+)?(?:COP|\${'$'}COP|\${'$'})?\s*([\d\.,]+).*?desde\s+\*(\d{4})\s+hacia\s+\*(\d+)""",
        setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)
    )

    // Regex para ingresos - transferencias recibidas
    private val ingresoTransferenciaRegex = Regex(
        """Bancolombia:\s*(?:Recibiste|Te\s+transfirieron)\s*(?:COP|\${'$'}COP|\${'$'})?\s*([\d\.,]+).*?desde\s+.*?\s+a\s+tu\s+cuenta\s+\*(\d{4})\s+el\s+(\d{2}/\d{2}/\d{4})\s+a\s+las\s+(\d{2}:\d{2})""",
        setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)
    )

    // Regex para depósitos/consignaciones
    private val ingresoDepositoRegex = Regex(
        """Bancolombia:\s*(?:Depósito|Consignación|Consignaste)\s*(?:COP|\${'$'}COP|\${'$'})?\s*([\d\.,]+).*?en\s+tu\s+cuenta\s+\*(\d{4})\s+el\s+(\d{2}/\d{2}/\d{4})\s+a\s+las\s+(\d{2}:\d{2})""",
        setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)
    )

    // Regex para ingresos de app Bancolombia
    private val appIngresoRegex = Regex(
        """(?:Transferencia\s+recibida|Depósito)\s+(?:por\s+)?(?:COP|\${'$'}COP|\${'$'})?\s*([\d\.,]+).*?en\s+\*(\d{4})""",
        setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)
    )

    // Regex para otros patrones bancarios comunes (Nequi, DaviPlata, etc.)
    private val nequiRegex = Regex(
        """Nequi:\s*Pagaste\s+(?:COP|\${'$'}COP|\${'$'})?\s*([\d\.,]+).*?en\s+(.+)""",
        setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)
    )

    private val nequiIngresoRegex = Regex(
        """Nequi:\s*(?:Recibiste|Te\s+enviaron)\s+(?:COP|\${'$'}COP|\${'$'})?\s*([\d\.,]+).*?de\s+(.+)""",
        setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)
    )

    private val daviRegex = Regex(
        """DaviPlata:\s*Compraste\s+(?:COP|\${'$'}COP|\${'$'})?\s*([\d\.,]+).*?en\s+(.+)""",
        setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)
    )

    private val daviIngresoRegex = Regex(
        """DaviPlata:\s*(?:Recibiste|Te\s+enviaron)\s+(?:COP|\${'$'}COP|\${'$'})?\s*([\d\.,]+).*?de\s+(.+)""",
        setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)
    )
    
    private val categoryRepository = CategoryRepository()

    suspend fun tryParse(text: String): Transaction? {
        // Primero intentar parsers específicos por contenido
        return tryParseBancolombia(text) ?: 
               tryParseNequi(text) ?: 
               tryParseDaviPlata(text) ?:
               tryParseIngresosBancolombia(text) ?:
               tryParseIngresosNequi(text) ?:
               tryParseIngresosDaviPlata(text)
    }

    private suspend fun tryParseBancolombia(text: String): Transaction? {
        if (!text.contains("Bancolombia", ignoreCase = true) && 
            !text.contains("Compra", ignoreCase = true) && 
            !text.contains("Transferencia", ignoreCase = true)) return null

        // SMS tradicional con formato completo
        compraRegex.find(text)?.let { m ->
            val amount = toCents(m.groupValues[1])
            val merchant = norm(m.groupValues[2])
            val last4 = m.groupValues[3]
            val ts = toEpoch(m.groupValues[4], m.groupValues[5])
            val id = hash("${ts/60000}|$amount|COMPRA|$last4|$merchant")
            val categoryId = categoryRepository.getDefaultCategoryId("COMPRA", merchant)
            
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
                rawPreview = text.take(140),
                categoryId = categoryId
            )
        }

        transRegex.find(text)?.let { m ->
            val amount = toCents(m.groupValues[1])
            val src = m.groupValues[2]
            val dst = m.groupValues[3].takeLast(4)
            val ts = toEpoch(m.groupValues[4], m.groupValues[5])
            val id = hash("${ts/60000}|$amount|TRANSFERENCIA|$src")
            val categoryId = categoryRepository.getDefaultCategoryId("TRANSFERENCIA", "Transferencia")
            
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
                rawPreview = text.take(140),
                categoryId = categoryId
            )
        }

        // Notificación de app (sin fecha/hora, usar timestamp actual)
        appCompraRegex.find(text)?.let { m ->
            val amount = toCents(m.groupValues[1])
            val merchant = norm(m.groupValues[2])
            val last4 = m.groupValues[3]
            val ts = System.currentTimeMillis()
            val id = hash("${ts/60000}|$amount|COMPRA|$last4|$merchant")
            val categoryId = categoryRepository.getDefaultCategoryId("COMPRA", merchant)
            
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
                rawPreview = text.take(140),
                categoryId = categoryId
            )
        }

        appTransRegex.find(text)?.let { m ->
            val amount = toCents(m.groupValues[1])
            val src = m.groupValues[2]
            val dst = m.groupValues[3].takeLast(4)
            val ts = System.currentTimeMillis()
            val id = hash("${ts/60000}|$amount|TRANSFERENCIA|$src")
            val categoryId = categoryRepository.getDefaultCategoryId("TRANSFERENCIA", "Transferencia")
            
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
                rawPreview = text.take(140),
                categoryId = categoryId
            )
        }

        return null
    }

    private suspend fun tryParseNequi(text: String): Transaction? {
        if (!text.contains("Nequi", ignoreCase = true)) return null

        nequiRegex.find(text)?.let { m ->
            val amount = toCents(m.groupValues[1])
            val merchant = norm(m.groupValues[2])
            val ts = System.currentTimeMillis()
            val id = hash("${ts/60000}|$amount|COMPRA|nequi|$merchant")
            val categoryId = categoryRepository.getDefaultCategoryId("COMPRA", merchant)
            
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
                rawPreview = text.take(140),
                categoryId = categoryId
            )
        }

        return null
    }

    private suspend fun tryParseDaviPlata(text: String): Transaction? {
        if (!text.contains("DaviPlata", ignoreCase = true)) return null

        daviRegex.find(text)?.let { m ->
            val amount = toCents(m.groupValues[1])
            val merchant = norm(m.groupValues[2])
            val ts = System.currentTimeMillis()
            val id = hash("${ts/60000}|$amount|COMPRA|davi|$merchant")
            val categoryId = categoryRepository.getDefaultCategoryId("COMPRA", merchant)
            
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
                rawPreview = text.take(140),
                categoryId = categoryId
            )
        }

        return null
    }

    private suspend fun tryParseIngresosBancolombia(text: String): Transaction? {
        if (!text.contains("Bancolombia", ignoreCase = true) && 
            !text.contains("Recibiste", ignoreCase = true) && 
            !text.contains("Depósito", ignoreCase = true) &&
            !text.contains("Consignación", ignoreCase = true)) return null

        // Transferencia recibida con fecha/hora
        ingresoTransferenciaRegex.find(text)?.let { m ->
            val amount = toCents(m.groupValues[1])
            val dstLast4 = m.groupValues[2]
            val ts = toEpoch(m.groupValues[3], m.groupValues[4])
            val id = hash("${ts/60000}|$amount|INGRESO_TRANSFERENCIA|$dstLast4")
            val categoryId = categoryRepository.getDefaultCategoryId("INGRESO", "Transferencia recibida")
            
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
                categoryId = categoryId,
                isIncome = true
            )
        }

        // Depósito/Consignación con fecha/hora
        ingresoDepositoRegex.find(text)?.let { m ->
            val amount = toCents(m.groupValues[1])
            val dstLast4 = m.groupValues[2]
            val ts = toEpoch(m.groupValues[3], m.groupValues[4])
            val id = hash("${ts/60000}|$amount|INGRESO_DEPOSITO|$dstLast4")
            val categoryId = categoryRepository.getDefaultCategoryId("INGRESO", "Depósito")
            
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
                categoryId = categoryId,
                isIncome = true
            )
        }

        // Ingreso desde app (sin fecha/hora, usar timestamp actual)
        appIngresoRegex.find(text)?.let { m ->
            val amount = toCents(m.groupValues[1])
            val dstLast4 = m.groupValues[2]
            val ts = System.currentTimeMillis()
            val id = hash("${ts/60000}|$amount|INGRESO_APP|$dstLast4")
            val categoryId = categoryRepository.getDefaultCategoryId("INGRESO", "Transferencia recibida")
            
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
                categoryId = categoryId,
                isIncome = true
            )
        }

        return null
    }

    private suspend fun tryParseIngresosNequi(text: String): Transaction? {
        if (!text.contains("Nequi", ignoreCase = true) || 
            !text.contains("Recibiste", ignoreCase = true)) return null

        nequiIngresoRegex.find(text)?.let { m ->
            val amount = toCents(m.groupValues[1])
            val sender = norm(m.groupValues[2])
            val ts = System.currentTimeMillis()
            val id = hash("${ts/60000}|$amount|INGRESO_NEQUI|$sender")
            val categoryId = categoryRepository.getDefaultCategoryId("INGRESO", "Transferencia recibida")
            
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
                categoryId = categoryId,
                isIncome = true
            )
        }

        return null
    }

    private suspend fun tryParseIngresosDaviPlata(text: String): Transaction? {
        if (!text.contains("DaviPlata", ignoreCase = true) || 
            !text.contains("Recibiste", ignoreCase = true)) return null

        daviIngresoRegex.find(text)?.let { m ->
            val amount = toCents(m.groupValues[1])
            val sender = norm(m.groupValues[2])
            val ts = System.currentTimeMillis()
            val id = hash("${ts/60000}|$amount|INGRESO_DAVI|$sender")
            val categoryId = categoryRepository.getDefaultCategoryId("INGRESO", "Transferencia recibida")
            
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
                categoryId = categoryId,
                isIncome = true
            )
        }

        return null
    }

    // Versión síncrona para compatibilidad con código existente
    fun tryParseSync(text: String): Transaction? = runBlocking { tryParse(text) }

    private fun toCents(s: String): Long {
        // "39,500.00" -> "3950000" (centavos)
        val clean = s.replace(".", "").replace(",", "")
        // Bancolombia suele mandar .00 (2 decimales). Si no, ajusta aquí.
        return clean.toLong()
    }

    private fun toEpoch(date: String, time: String): Long {
        val fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")
        val ldt = LocalDateTime.parse("$date $time", fmt)
        return ldt.atZone(ZoneId.of("America/Bogota")).toInstant().toEpochMilli()
    }

    private fun norm(x: String) = x.trim().replace(Regex("\\s+"), " ").take(60)

    private fun hash(x: String) = DigestUtils.sha256Hex(x)
}