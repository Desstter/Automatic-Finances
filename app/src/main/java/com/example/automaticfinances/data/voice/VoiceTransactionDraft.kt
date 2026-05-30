package com.example.automaticfinances.data.voice

import java.util.UUID

/**
 * A review-ready, editable draft shown on the voice confirmation screen. One per detected
 * transaction. [draftId] is a stable UI key (for list diffing across edits) and is unrelated to
 * the final persisted transaction hash, which is derived from the content at save time.
 *
 * [categoryId] is pre-resolved (Gemini suggestion → exact match → keyword/learned fallback) so the
 * screen shows a sensible category that the user can still change.
 */
data class VoiceTransactionDraft(
    val draftId: String = UUID.randomUUID().toString(),
    val description: String,
    val amountCents: Long,
    val categoryId: Long?,
    val isIncome: Boolean,
    val needsReview: Boolean,
)
