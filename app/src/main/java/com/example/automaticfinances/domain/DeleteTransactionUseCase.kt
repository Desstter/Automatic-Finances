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
 *
 * Transfers are dual-entry (two legs sharing a `transferGroupId`). Use [deleteGroup] to remove
 * both legs in a single transaction so the two affected account balances can never be left
 * half-updated (CLAUDE.md invariant #4).
 */
class DeleteTransactionUseCase @Inject constructor(
    private val transactionRepo: TransactionRepository,
    private val openingBalanceRepo: OpeningBalanceRepository,
    private val transactionRunner: TransactionRunner
) {
    /** @return true if the transaction existed and was deleted. */
    suspend operator fun invoke(transaction: Transaction): Boolean =
        deleteAllAtomic(listOf(transaction))

    /**
     * Deletes several rows (e.g. both legs of a transfer) atomically and recomputes every affected
     * account's balance once. @return true if at least one row was deleted.
     */
    suspend fun deleteGroup(transactions: List<Transaction>): Boolean =
        deleteAllAtomic(transactions)

    private suspend fun deleteAllAtomic(transactions: List<Transaction>): Boolean =
        transactionRunner.runInTransaction {
            var anyDeleted = false
            for (tx in transactions) {
                if (transactionRepo.deleteTransaction(tx.id)) anyDeleted = true
            }
            if (anyDeleted) {
                // Recompute each affected account's cached balance from source (now without the
                // deleted rows), within the same transaction so delete + balance commit atomically.
                transactions.mapNotNull { it.accountId }.distinct()
                    .forEach { openingBalanceRepo.recalculateAccountBalance(it) }
            }
            anyDeleted
        }
}
