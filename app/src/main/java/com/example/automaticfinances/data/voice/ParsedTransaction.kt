package com.example.automaticfinances.data.voice

/**
 * One transaction extracted from a spoken phrase by the NLP layer, before it is resolved into a
 * persistable [com.example.automaticfinances.data.db.Transaction]. Amounts are already normalized
 * to cents (COP × 100). [suggestedCategoryName] is Gemini's best guess against the live category
 * list; the actual category id is resolved downstream so the NLP layer stays DB-agnostic.
 *
 * [needsReview] is set when the model (or our own validation) is unsure — typically an ambiguous
 * bare amount like "tinto 2" — so the UI can highlight the field for confirmation.
 */
data class ParsedTransaction(
    val description: String,
    val amountCents: Long,
    val suggestedCategoryName: String?,
    val isIncome: Boolean,
    val needsReview: Boolean,
)
