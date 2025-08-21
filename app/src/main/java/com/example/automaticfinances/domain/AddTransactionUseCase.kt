package com.example.automaticfinances.domain

import com.example.automaticfinances.data.db.Transaction
import com.example.automaticfinances.data.repo.TransactionRepository
import com.example.automaticfinances.data.repo.AccountRepository

class AddTransactionUseCase(
    private val transactionRepo: TransactionRepository = TransactionRepository(),
    private val accountRepo: AccountRepository = AccountRepository()
) {
    suspend operator fun invoke(tx: Transaction) {
        // Ensure transaction has account assigned
        val transactionWithAccount = if (tx.accountId == null) {
            accountRepo.assignAccountToTransaction(tx)
        } else {
            tx
        }
        
        // Insert transaction
        transactionRepo.insert(transactionWithAccount)
        
        // Update account balance
        accountRepo.applyTransactionToBalance(transactionWithAccount)
    }
}