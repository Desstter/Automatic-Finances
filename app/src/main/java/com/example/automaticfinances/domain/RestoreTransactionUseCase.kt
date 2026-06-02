package com.example.automaticfinances.domain

import com.example.automaticfinances.data.db.Transaction
import com.example.automaticfinances.data.repo.OpeningBalanceRepository
import com.example.automaticfinances.data.repo.TransactionRepository
import javax.inject.Inject

/**
 * Restores a previously deleted transaction (undo) while preserving the financial invariant.
 *
 * Re-inserts the exact transaction and re-applies its balance effect. The balance is
 * re-applied ONLY when the row was actually inserted (insertIgnore returns a real rowId),
 * so a double undo or a still-present row cannot double count.
 *
 * Note: this intentionally does NOT go through [AddTransactionUseCase] — a restore must put
 * back the exact stored row (e.g. a single leg of a withdrawal split) without re-running the
 * RETIRO transfer logic or re-deriving account/category.
 */
class RestoreTransactionUseCase @Inject constructor(
    private val transactionRepo: TransactionRepository,
    private val openingBalanceRepo: OpeningBalanceRepository,
    private val transactionRunner: TransactionRunner
) {
    /** @return true if the transaction was re-inserted (false if it still existed). */
    suspend operator fun invoke(transaction: Transaction): Boolean =
        transactionRunner.runInTransaction {
            val inserted = transactionRepo.insert(transaction)
            if (inserted) {
                // Recompute the affected account's cached balance from source (now with the
                // restored row). A double undo inserts nothing, so the balance is untouched.
                transaction.accountId?.let { openingBalanceRepo.recalculateAccountBalance(it) }
            }
            inserted
        }
}
