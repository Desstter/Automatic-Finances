package com.example.automaticfinances.domain

import com.example.automaticfinances.data.db.PendingTransaction
import com.example.automaticfinances.data.repo.PendingTransactionRepository
import javax.inject.Inject

/**
 * Promotes a reviewed draft into a real transaction (PROD-1). It runs the exact same persistence
 * path as any other capture — [AddTransactionUseCase] enriches, inserts idempotently and recomputes
 * the balance in one DB transaction — then removes the draft. The balance only ever moves here, on
 * an explicit user confirmation, never at capture time.
 *
 * @param categoryId the user's chosen category, or null to let enrichment resolve it. A preset
 *   category is preserved by [AddTransactionUseCase], per the financial invariant.
 */
class ConfirmPendingTransactionUseCase @Inject constructor(
    private val addTransaction: AddTransactionUseCase,
    private val pendingRepo: PendingTransactionRepository
) {
    suspend operator fun invoke(pending: PendingTransaction, categoryId: Long?) {
        addTransaction(pending.toTransaction(categoryId))
        pendingRepo.delete(pending.id)
    }
}
