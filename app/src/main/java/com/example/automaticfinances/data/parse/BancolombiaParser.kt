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

    // Regex para otros patrones bancarios comunes (Nequi, DaviPlata, etc.)
    private val nequiRegex = Regex(
        """Nequi:\s*Pagaste\s+(?:COP|\${'$'}COP|\${'$'})?\s*([\d\.,]+).*?en\s+(.+)""",
        setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)
    )

    private val daviRegex = Regex(
        """DaviPlata:\s*Compraste\s+(?:COP|\${'$'}COP|\${'$'})?\s*([\d\.,]+).*?en\s+(.+)""",
        setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)
    )
    
    private val categoryRepository = CategoryRepository()

    suspend fun tryParse(text: String): Transaction? {
        // Primero intentar parsers específicos por contenido
        return tryParseBancolombia(text) ?: 
               tryParseNequi(text) ?: 
               tryParseDaviPlata(text)
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