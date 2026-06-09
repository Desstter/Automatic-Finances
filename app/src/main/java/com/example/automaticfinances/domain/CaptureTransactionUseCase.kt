package com.example.automaticfinances.domain

import com.example.automaticfinances.data.db.Transaction
import com.example.automaticfinances.data.repo.CategoryRepository
import com.example.automaticfinances.data.repo.PendingTransactionRepository
import com.example.automaticfinances.data.repo.TransactionRepository
import javax.inject.Inject

/**
 * Single decision point for auto-captured (SMS / notification) transactions: confirm directly, or
 * quarantine in the "Por revisar" queue (PROD-1). User-initiated entries (voice, manual) bypass this
 * and go straight to [AddTransactionUseCase] — only machine-captured ones are subject to review.
 *
 * Policy: transactions parsed by the generic fallback (`source == "notif:generic"`) are the only
 * ones prone to false positives (FIN-2), so they are the ones held for review. Specific bank-format
 * matches (`notif:sms`, `notif:app`, `notif:nequi`, …) are high-confidence and auto-confirm as before.
 *
 * On a genuinely-new high-confidence capture it also posts the category-chips feedback notification
 * (PROD-2). This lives here, not in [AddTransactionUseCase], so voice/manual entries (which already
 * let the user pick a category in-app) and re-deliveries never trigger it.
 */
class CaptureTransactionUseCase @Inject constructor(
    private val addTransaction: AddTransactionUseCase,
    private val pendingRepo: PendingTransactionRepository,
    private val transactionRepo: TransactionRepository,
    private val categoryRepo: CategoryRepository,
    private val feedbackNotifier: CaptureFeedbackNotifier,
) {
    suspend operator fun invoke(tx: Transaction) {
        // Re-delivery of a message the user already confirmed: the real row exists, so do nothing
        // (never re-create a pending draft for an already-accepted transaction).
        if (transactionRepo.getById(tx.id) != null) return

        if (needsReview(tx)) {
            // INSERT IGNORE in the repo dedups repeated deliveries while it sits in the queue.
            pendingRepo.record(tx)
        } else {
            // addTransaction returns the persisted transaction only when a new row was actually
            // inserted (null for duplicates / RETIRO dual-entries), which is exactly when offering
            // category feedback makes sense.
            val saved = addTransaction(tx) ?: return
            offerCategoryChips(saved)
        }
    }

    private suspend fun offerCategoryChips(saved: Transaction) {
        val chips = categoryRepo.suggestCategoriesForCapture(saved.categoryId, saved.isIncome)
        if (chips.isNotEmpty()) feedbackNotifier.notifyCaptured(saved, chips)
    }

    private fun needsReview(tx: Transaction): Boolean = tx.source == SOURCE_GENERIC

    companion object {
        const val SOURCE_GENERIC = "notif:generic"
    }
}
