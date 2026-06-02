package com.example.automaticfinances.domain

import com.example.automaticfinances.data.db.Transaction
import com.example.automaticfinances.data.repo.OpeningBalanceRepository
import com.example.automaticfinances.data.repo.TransactionRepository
import javax.inject.Inject

/**
 * Deletes a transaction while preserving the financial invariant
 * (opening balance + movements = current balance).
 *
 * The balance side effect is reverted ONLY when a row was actually deleted, mirroring the
 * idempotency guard used when inserting. This is the counterpart of [AddTransactionUseCase]:
 * never revert a balance for a transaction that wasn't really removed.
 */
class DeleteTransactionUseCase @Inject constructor(
    private val transactionRepo: TransactionRepository,
    private val openingBalanceRepo: OpeningBalanceRepository,
    private val transactionRunner: TransactionRunner
) {
    /** @return true if the transaction existed and was deleted. */
    suspend operator fun invoke(transaction: Transaction): Boolean =
        transactionRunner.runInTransaction {
            val deleted = transactionRepo.deleteTransaction(transaction.id)
            if (deleted) {
                // Recompute the affected account's cached balance from source (now without the
                // deleted row), within the same transaction so delete + balance commit atomically.
                transaction.accountId?.let { openingBalanceRepo.recalculateAccountBalance(it) }
            }
            deleted
        }
}
