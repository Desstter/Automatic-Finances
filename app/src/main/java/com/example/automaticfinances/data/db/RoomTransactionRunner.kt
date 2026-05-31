package com.example.automaticfinances.data.db

import androidx.room.withTransaction
import com.example.automaticfinances.domain.TransactionRunner
import javax.inject.Inject

/**
 * Room-backed [TransactionRunner]. Delegates to [androidx.room.withTransaction], which runs the
 * suspending block on the database's transaction dispatcher and rolls back every write if the
 * block throws.
 */
class RoomTransactionRunner @Inject constructor(
    private val db: AppDatabase
) : TransactionRunner {
    override suspend fun <R> runInTransaction(block: suspend () -> R): R =
        db.withTransaction { block() }
}
