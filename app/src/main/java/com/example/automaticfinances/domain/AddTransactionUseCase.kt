package com.example.automaticfinances.domain

import com.example.automaticfinances.data.db.Transaction
import com.example.automaticfinances.data.repo.TransactionRepository
import com.example.automaticfinances.data.repo.AccountRepository
import com.example.automaticfinances.data.repo.CategoryRepository
import javax.inject.Inject

class AddTransactionUseCase @Inject constructor(
    private val transactionRepo: TransactionRepository,
    private val accountRepo: AccountRepository,
    private val categoryRepo: CategoryRepository
) {
    suspend operator fun invoke(tx: Transaction) {
        // Enrich with account + category. The parser produces "pure" transactions (no DB
        // coupling), so account and category are resolved here. Pre-set values (e.g. from a
        // manual entry where the user picked a category) are always preserved.
        val enriched = enrich(tx)

        // Handle special case: RETIRO (ATM withdrawal)
        if (enriched.type == "RETIRO") {
            handleWithdrawalTransfer(enriched)
        } else {
            // Normal transaction processing.
            // Only adjust the balance if the row was actually inserted; a re-delivered
            // notification with the same id is ignored and must NOT touch the balance again.
            val inserted = transactionRepo.insert(enriched)
            if (inserted) {
                accountRepo.applyTransactionToBalance(enriched)
            }
        }
    }

    /**
     * Resolves the account (from source) and category (from type + description) when they
     * are not already provided. Keeps any caller-supplied account/category untouched.
     */
    private suspend fun enrich(tx: Transaction): Transaction {
        val withAccount = if (tx.accountId == null) {
            accountRepo.assignAccountToTransaction(tx)
        } else {
            tx
        }
        return if (withAccount.categoryId == null) {
            withAccount.copy(
                categoryId = categoryRepo.getDefaultCategoryId(withAccount.type, withAccount.description)
            )
        } else {
            withAccount
        }
    }
    
    /**
     * Handles ATM withdrawals as transfers from Bank to Cash
     * Creates two transactions: withdrawal from bank + deposit to cash
     */
    private suspend fun handleWithdrawalTransfer(withdrawal: Transaction) {
        // Get bank and cash accounts
        val bankAccount = accountRepo.getBankAccount()
        val cashAccount = accountRepo.getCashAccount()
        
        if (bankAccount == null || cashAccount == null) {
            // Fallback: treat as normal expense if accounts don't exist
            if (transactionRepo.insert(withdrawal)) {
                accountRepo.applyTransactionToBalance(withdrawal)
            }
            return
        }
        
        // 1. Create withdrawal transaction (from bank)
        val bankWithdrawal = withdrawal.copy(
            id = "${withdrawal.id}_BANK",
            accountId = bankAccount.id,
            type = "GASTO", // Treat as expense from bank
            description = withdrawal.description
        )
        
        // 2. Create corresponding cash deposit transaction
        val cashDeposit = Transaction.fromTimestamp(
            id = "${withdrawal.id}_CASH",
            ts = withdrawal.ts,
            type = "INGRESO",
            description = "Efectivo - ${withdrawal.description}",
            amountCents = withdrawal.amountCents,
            currency = withdrawal.currency,
            srcLast4 = "BANK",
            dstLast4 = "CASH",
            source = withdrawal.source,
            rawPreview = withdrawal.rawPreview,
            categoryId = categoryRepo.getDefaultCategoryId("INGRESO", "Efectivo"),
            accountId = cashAccount.id,
            isIncome = true
        )
        
        // Insert both transactions and only apply each balance change if its row was new.
        if (transactionRepo.insert(bankWithdrawal)) {
            accountRepo.applyTransactionToBalance(bankWithdrawal)
        }
        if (transactionRepo.insert(cashDeposit)) {
            accountRepo.applyTransactionToBalance(cashDeposit)
        }
    }
}