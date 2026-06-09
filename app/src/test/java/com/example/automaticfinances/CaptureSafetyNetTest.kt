package com.example.automaticfinances

import com.example.automaticfinances.data.repo.UnparsedSmsRepository
import com.example.automaticfinances.utils.UnparsedTransactionHints
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Guards the universal "never drop a probable transaction" gate and the rescue extractor used by
 * "Mensajes no reconocidos". These are the two pieces that, together with the package whitelist,
 * guarantee a bank message is either parsed or kept for one-tap registration — never lost.
 */
class CaptureSafetyNetTest {

    // --- looksLikeTransaction: the single gate shared by SmsReceiver, the listener and the log ---

    @Test
    fun looksLikeTransaction_acceptsPaymentMessages() {
        assertTrue(UnparsedSmsRepository.looksLikeTransaction("Nu: Pagaste \$30.000 en STARBUCKS"))
        assertTrue(UnparsedSmsRepository.looksLikeTransaction("Bancolombia: Pagaste \$40.000 en EXITO"))
    }

    @Test
    fun looksLikeTransaction_acceptsReceivedTransfer() {
        assertTrue(UnparsedSmsRepository.looksLikeTransaction("Recibiste \$80.000 de Maria"))
        assertTrue(UnparsedSmsRepository.looksLikeTransaction("Te transfirieron \$1.000.000"))
    }

    @Test
    fun looksLikeTransaction_rejectsOtpAndChat() {
        assertFalse("OTP code is not a transaction", UnparsedSmsRepository.looksLikeTransaction("Tu codigo es 4821"))
        assertFalse("plain chat is not a transaction", UnparsedSmsRepository.looksLikeTransaction("Nos vemos a las 5"))
    }

    @Test
    fun looksLikeTransaction_doesNotMatchBareNuSubstring() {
        // "nu" must never be a keyword: it would match everyday words. Only real transaction terms.
        assertFalse(UnparsedSmsRepository.looksLikeTransaction("Nunca pierdas un minuto, ahorra \$10.000"))
    }

    // --- Rescue extractor used to pre-fill the manual add flow ---

    @Test
    fun extractAmount_findsColombianFormats() {
        assertEquals("30.000", UnparsedTransactionHints.extractAmount("Nu: Pagaste \$30.000 en STARBUCKS"))
        assertEquals("1.234.567,89", UnparsedTransactionHints.extractAmount("Recibiste \$1.234.567,89 hoy"))
    }

    @Test
    fun extractAmount_returnsNullWhenNoAmount() {
        assertNull(UnparsedTransactionHints.extractAmount("Mensaje sin monto"))
    }

    @Test
    fun looksLikeIncome_distinguishesDirection() {
        assertTrue(UnparsedTransactionHints.looksLikeIncome("Recibiste \$80.000 de Maria"))
        assertTrue(UnparsedTransactionHints.looksLikeIncome("Consignación por \$50.000"))
        assertFalse(UnparsedTransactionHints.looksLikeIncome("Pagaste \$30.000 en STARBUCKS"))
    }

    @Test
    fun suggestedDescription_stripsRejectedCharsAndTrims() {
        val seed = UnparsedTransactionHints.suggestedDescription("  Compra en  <Tienda> \"X\" & Co  ")
        assertFalse(seed.contains("<"))
        assertFalse(seed.contains("\""))
        assertFalse(seed.contains("&"))
        assertTrue(seed.length <= 60)
        assertEquals(seed, seed.trim())
    }
}
