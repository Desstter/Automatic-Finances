package com.example.automaticfinances.data.parse

import com.example.automaticfinances.data.db.Transaction
import org.apache.commons.codec.digest.DigestUtils
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

object BancolombiaParser {

    private val compraRegex = Regex(
        """Bancolombia:\s*Compraste\s*(?:COP|\${'$'}COP|\${'$'})?\s*([\d\.,]+).*?\sen\s+(.+?)\s+con\s+tu\s+T\.?Cred\s+\*(\d{4}),\s+el\s+(\d{2}/\d{2}/\d{4})\s+a\s+las\s+(\d{2}:\d{2})""",
        setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)
    )

    private val transRegex = Regex(
        """Bancolombia:\s*Transferiste\s*(?:COP|\${'$'}COP|\${'$'})?\s*([\d\.,]+).*?desde\s+tu\s+cuenta\s+\*(\d{4})\s+a\s+la\s+cuenta\s+\*(\d+)\s+el\s+(\d{2}/\d{2}/\d{4})\s+a\s+las\s+(\d{2}:\d{2})""",
        setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)
    )

    fun tryParse(text: String): Transaction? {
        if (!text.contains("Bancolombia:", ignoreCase = true)) return null

        compraRegex.find(text)?.let { m ->
            val amount = toCents(m.groupValues[1])
            val merchant = norm(m.groupValues[2])
            val last4 = m.groupValues[3]
            val ts = toEpoch(m.groupValues[4], m.groupValues[5])
            val id = hash("${ts/60000}|$amount|COMPRA|$last4|$merchant")
            return Transaction(id, ts, "COMPRA", amount, "COP", merchant, last4, null, "notif:sms", text.take(140))
        }

        transRegex.find(text)?.let { m ->
            val amount = toCents(m.groupValues[1])
            val src = m.groupValues[2]
            val dst = m.groupValues[3].takeLast(4)
            val ts = toEpoch(m.groupValues[4], m.groupValues[5])
            val id = hash("${ts/60000}|$amount|TRANSFERENCIA|$src")
            return Transaction(id, ts, "TRANSFERENCIA", amount, "COP", null, src, dst, "notif:sms", text.take(140))
        }

        return null
    }

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