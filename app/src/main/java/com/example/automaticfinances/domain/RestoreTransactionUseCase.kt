package com.example.automaticfinances.domain

import com.example.automaticfinances.data.db.Transaction
import com.example.automaticfinances.data.repo.OpeningBalanceRepository
import com.example.automaticfinances.data.repo.TransactionRepository
import javax.inject.Inject

/**
 * Restores previously deleted transaction(s) (undo) while preserving the financial invariant.
 *
 * Re-inserts the exact stored rows and re-applies their balance effect. The balance is re-applied
 * ONLY for rows actually inserted (insertIgnore returns a real rowId), so a double undo or a
 * still-present row cannot double count.
 *
 * Note: this intentionally does NOT go through [AddTransactionUseCase] — a restore must put back
 * the exact stored rows (e.g. both legs of a transfer or a single leg of a withdrawal split)
 * without re-running any transfer/RETIRO logic or re-deriving account/category. Use [restoreGroup]
 * to undo a transfer as a pair, atomically.
 */
class RestoreTransactionUseCase @Inject constructor(
    private val transactionRepo: TransactionRepository,
    private val openingBalanceRepo: OpeningBalanceRepository,
    private val transactionRunner: TransactionRunner
) {
    /** @return true if the transaction was re-inserted (false if it still existed). */
    suspend operator fun invoke(transaction: Transaction): Boolean =
        restoreAllAtomic(listOf(transaction))

    /** Re-inserts several rows (e.g. both legs of a transfer) atomically. */
    suspend fun restoreGroup(transactions: List<Transaction>): Boolean =
        restoreAllAtomic(transactions)

    private suspend fun restoreAllAtomic(transactions: List<Transaction>): Boolean =
        transactionRunner.runInTransaction {
            val insertedAccounts = mutableSetOf<Long>()
            var anyInserted = false
            for (tx in transactions) {
                if (transactionRepo.insert(tx)) {
                    anyInserted = true
                    tx.accountId?.let { insertedAccounts += it }
                }
            }
            // Recompute only the accounts whose rows were actually restored. A double undo inserts
            // nothing, so balances are untouched.
            insertedAccounts.forEach { openingBalanceRepo.recalculateAccountBalance(it) }
            anyInserted
        }
}
