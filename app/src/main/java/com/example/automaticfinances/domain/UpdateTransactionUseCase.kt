package com.example.automaticfinances.domain

import com.example.automaticfinances.data.db.Transaction
import com.example.automaticfinances.data.repo.OpeningBalanceRepository
import com.example.automaticfinances.data.repo.TransactionRepository
import javax.inject.Inject

/**
 * Updates an existing transaction (or both legs of a transfer) while preserving the single source
 * of truth for balances.
 *
 * The transaction's `id` (its dedup hash / primary key) is kept stable on edit: Room's `@Update`
 * matches by primary key, and the hash only matters for de-duplicating SMS-sourced inserts. What
 * an edit can change is the **account** a movement belongs to, which moves money between balances —
 * so this use case recomputes every affected account (old and new) from source, atomically with
 * the row update, exactly like add/delete do (invariant ARQ-1).
 */
class UpdateTransactionUseCase @Inject constructor(
    private val transactionRepo: TransactionRepository,
    private val openingBalanceRepo: OpeningBalanceRepository,
    private val transactionRunner: TransactionRunner,
) {
    /**
     * Persists [updated] (same id as [original]) and recomputes the balances of the original and
     * the new account if they differ.
     */
    suspend operator fun invoke(original: Transaction, updated: Transaction) {
        applyAtomic(listOf(original to updated))
    }

    /**
     * Updates several rows at once (e.g. both legs of a transfer when the user reassigns the origin
     * and/or destination account) and recomputes every account touched — by either the old or the
     * new value — once, atomically.
     */
    suspend fun updateGroup(pairs: List<Pair<Transaction, Transaction>>) {
        applyAtomic(pairs)
    }

    private suspend fun applyAtomic(pairs: List<Pair<Transaction, Transaction>>) {
        transactionRunner.runInTransaction {
            val affectedAccounts = mutableSetOf<Long>()
            for ((original, updated) in pairs) {
                transactionRepo.update(updated)
                original.accountId?.let { affectedAccounts += it }
                updated.accountId?.let { affectedAccounts += it }
            }
            affectedAccounts.forEach { openingBalanceRepo.recalculateAccountBalance(it) }
        }
    }
}
