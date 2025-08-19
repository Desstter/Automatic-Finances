package com.example.automaticfinances.domain

import com.example.automaticfinances.data.db.Transaction
import com.example.automaticfinances.data.repo.TransactionRepository

class AddTransactionUseCase(private val repo: TransactionRepository = TransactionRepository()) {
    suspend operator fun invoke(tx: Transaction) = repo.insert(tx)
}