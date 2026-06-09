package com.example.automaticfinances.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * A low-confidence auto-captured transaction held for user review (PROD-1). It deliberately lives in
 * its own table — NOT in `transactions` — so it can never reach the balance math until the user
 * confirms it. That is the whole point: a false positive from the generic fallback parser must not
 * silently move the saldo, and the ML must not learn from a dirty, unreviewed datum.
 *
 * On confirm, the row is rebuilt into a real [Transaction] and goes through the normal
 * `AddTransactionUseCase` path (enrich → insert → recompute balance); on discard it is just deleted.
 *
 * [id] is the parser's stable dedup hash, identical to the id the confirmed [Transaction] will get,
 * so re-deliveries collapse via INSERT IGNORE and a captured-then-confirmed message can't reappear.
 */
@Entity(tableName = "pending_transactions")
data class PendingTransaction(
    @PrimaryKey val id: String,
    val ts: Long,
    val type: String,
    val description: String,
    val amountCents: Long,
    val currency: String,
    val srcLast4: String?,
    val dstLast4: String?,
    val source: String,
    val isIncome: Boolean,
    val rawPreview: String,
    val capturedAt: Long = System.currentTimeMillis()
) {
    /**
     * Rebuilds the real [Transaction] for confirmation. [categoryId] is the user's choice (or null
     * to let `AddTransactionUseCase.enrich` resolve it); a preset category is always preserved by
     * the use case, per the financial invariant.
     */
    fun toTransaction(categoryId: Long?): Transaction = Transaction.fromTimestamp(
        id = id,
        ts = ts,
        type = type,
        description = description,
        amountCents = amountCents,
        currency = currency,
        srcLast4 = srcLast4,
        dstLast4 = dstLast4,
        source = source,
        rawPreview = rawPreview,
        categoryId = categoryId,
        isIncome = isIncome
    )

    companion object {
        fun from(tx: Transaction): PendingTransaction = PendingTransaction(
            id = tx.id,
            ts = tx.ts,
            type = tx.type,
            description = tx.description,
            amountCents = tx.amountCents,
            currency = tx.currency,
            srcLast4 = tx.srcLast4,
            dstLast4 = tx.dstLast4,
            source = tx.source,
            isIncome = tx.isIncome,
            rawPreview = tx.rawPreview
        )
    }
}
