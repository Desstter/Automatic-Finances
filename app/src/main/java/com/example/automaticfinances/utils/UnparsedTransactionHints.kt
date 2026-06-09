package com.example.automaticfinances.utils

/**
 * Best-effort extraction used to PRE-FILL the manual add flow when the user rescues a message from
 * "Mensajes no reconocidos" (the messages the parser couldn't turn into a transaction). This is not
 * a parser: it only guesses the amount and whether the text reads like income, so the user lands on
 * a form that's already half-filled. The user always reviews and confirms before anything is saved.
 */
object UnparsedTransactionHints {

    // $ / COP prefixing a number, or a number with thousands/decimal separators. Mirrors the gate in
    // UnparsedSmsRepository.AMOUNT_HINT but captures the numeric token so we can pre-fill it.
    private val AMOUNT = Regex(
        """(?:\$|cop)\s*([\d.,]+)|(\d{1,3}(?:[.,]\d{3})+(?:[.,]\d{1,2})?)|(\d+[.,]\d{2})""",
        RegexOption.IGNORE_CASE,
    )

    // Words that mark money coming IN. If none are present we assume an expense (the common case).
    private val INCOME_MARKERS = listOf(
        "recibiste", "recibido", "te enviaron", "transfirieron", "consignación", "consignacion",
        "depósito", "deposito", "nómina", "nomina", "recepcion transferencia",
        "recepción transferencia", "transferencia recibida", "abono",
    )

    /** First amount-like token in the text (e.g. "39.500,00"), or null if none found. */
    fun extractAmount(text: String): String? {
        val m = AMOUNT.find(text) ?: return null
        return m.groupValues.drop(1).firstOrNull { it.isNotBlank() }?.trim()
    }

    fun looksLikeIncome(text: String): Boolean =
        INCOME_MARKERS.any { text.contains(it, ignoreCase = true) }

    /**
     * A short, sanitized description seed for the form. Strips characters the add screens reject
     * (`< > " ' &`) and collapses whitespace; the user can edit it before saving.
     */
    fun suggestedDescription(text: String): String =
        text.replace(Regex("""[<>"'&]"""), " ")
            .replace(Regex("\\s+"), " ")
            .trim()
            .take(60)
}
