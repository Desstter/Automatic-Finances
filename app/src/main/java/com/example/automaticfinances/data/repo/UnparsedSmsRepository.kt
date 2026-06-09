package com.example.automaticfinances.data.repo

import com.example.automaticfinances.data.db.UnparsedSms
import com.example.automaticfinances.data.db.UnparsedSmsDao
import kotlinx.coroutines.flow.Flow
import org.apache.commons.codec.digest.DigestUtils
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Stores bank-related messages the parser failed to capture (see [UnparsedSms]). Both SMS capture
 * paths funnel their misses here so the user can review which formats are being missed without any
 * data leaving the device.
 */
@Singleton
class UnparsedSmsRepository @Inject constructor(
    private val dao: UnparsedSmsDao
) {
    fun observeAll(): Flow<List<UnparsedSms>> = dao.getAllFlow()

    fun observeCount(): Flow<Int> = dao.countFlow()

    /**
     * Records a message that looked like a transaction but did not parse. Callers should gate on
     * [looksLikeTransaction] first so the log stays focused on probable transactions (not OTPs or
     * promos). Dedup is by a stable hash, so repeated deliveries of the same message are no-ops.
     */
    suspend fun record(text: String, source: String, receivedAt: Long) {
        val normalized = normalize(text)
        if (normalized.isBlank()) return
        dao.insertIgnore(
            UnparsedSms(
                id = stableId(receivedAt, normalized),
                text = text.trim().take(MAX_LEN),
                source = source,
                receivedAt = receivedAt
            )
        )
    }

    suspend fun delete(id: String) = dao.deleteById(id)

    suspend fun clearAll() = dao.clear()

    private fun normalize(text: String): String =
        text.trim().lowercase().replace(Regex("\\s+"), " ").take(MAX_LEN)

    /**
     * Mirrors [com.example.automaticfinances.data.parse.BancolombiaParser]'s dedup philosophy: a
     * minute bucket plus a hash of the normalized text, so the same message at the same minute maps
     * to one row regardless of how many times it is re-delivered.
     */
    private fun stableId(receivedAt: Long, normalizedText: String): String {
        val minute = receivedAt / 60000
        return DigestUtils.sha256Hex("$minute|$normalizedText")
    }

    companion object {
        private const val MAX_LEN = 500

        // A message worth logging mentions a bank/transaction term AND carries an amount-like
        // token. The amount gate is what separates a missed purchase from an OTP or a promo, which
        // pass the keyword gate but should not pollute the diagnostic log.
        // Single source of truth for the "is this bank/transaction text?" keyword gate, shared by
        // both capture paths (SmsReceiver, SmsNotifListener) and this log. Add transaction-specific
        // terms only — never a short substring like "nu" that collides with ordinary words
        // ("nunca", "minuto", "número"); recognized bank APPS are matched by package, not keyword.
        private val BANK_KEYWORDS = listOf(
            "bancolombia", "nequi", "daviplata", "nubank", "compra", "compraste", "pagaste", "pago",
            "transferiste", "transferencia", "transfirieron", "enviaron", "retiro", "retiraste",
            "recibiste", "recibido", "consignación", "consignacion", "depósito", "deposito",
            "débito", "debito", "crédito", "credito"
        )

        // $ / COP prefixing digits, or a number with thousands/decimal separators (12.345 / 12,345
        // / 12.345,67). Deliberately does NOT match a bare integer like a 4-digit OTP code.
        private val AMOUNT_HINT = Regex(
            """(?:\$|cop)\s*\d|\d{1,3}(?:[.,]\d{3})+|\d+[.,]\d{2}\b""",
            RegexOption.IGNORE_CASE
        )

        fun looksLikeTransaction(text: String): Boolean {
            val hasKeyword = BANK_KEYWORDS.any { text.contains(it, ignoreCase = true) }
            return hasKeyword && AMOUNT_HINT.containsMatchIn(text)
        }
    }
}
