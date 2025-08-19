package com.example.automaticfinances.data.repo

import com.example.automaticfinances.data.db.AppDatabase
import com.example.automaticfinances.data.db.Transaction

class TransactionRepository {
    private val dao = AppDatabase.get().transactionDao()
    suspend fun insert(tx: Transaction) = dao.insertIgnore(tx)
    fun all() = dao.all()
}