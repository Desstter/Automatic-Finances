package com.example.automaticfinances.domain

import com.example.automaticfinances.data.db.Transaction
import com.example.automaticfinances.data.repo.TransactionRepository
import com.example.automaticfinances.data.repo.AccountRepository
import com.example.automaticfinances.data.repo.CategoryRepository

class AddTransactionUseCase(
    private val transactionRepo: TransactionRepository = TransactionRepository(),
    private val accountRepo: AccountRepository = AccountRepository(),
    private val categoryRepo: CategoryRepository = CategoryRepository()
) {
    suspend operator fun invoke(tx: Transaction) {
        // Ensure transaction has account assigned
        val transactionWithAccount = if (tx.accountId == null) {
            accountRepo.assignAccountToTransaction(tx)
        } else {
            tx
        }
        
        // Handle special case: RETIRO (ATM withdrawal)
        if (transactionWithAccount.type == "RETIRO") {
            handleWithdrawalTransfer(transactionWithAccount)
        } else {
            // Normal transaction processing
            transactionRepo.insert(transactionWithAccount)
            accountRepo.applyTransactionToBalance(transactionWithAccount)
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
            transactionRepo.insert(withdrawal)
            accountRepo.applyTransactionToBalance(withdrawal)
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
        
        // Insert both transactions
        transactionRepo.insert(bankWithdrawal)
        transactionRepo.insert(cashDeposit)
        
        // Apply to balances
        accountRepo.applyTransactionToBalance(bankWithdrawal)
        accountRepo.applyTransactionToBalance(cashDeposit)
    }
}