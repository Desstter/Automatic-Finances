package com.example.automaticfinances.data.repo

import com.example.automaticfinances.data.db.PendingTransaction
import com.example.automaticfinances.data.db.PendingTransactionDao
import com.example.automaticfinances.data.db.Transaction
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The "Por revisar" queue: low-confidence auto-captures awaiting user confirmation. These never
 * affect any balance — they are not in the `transactions` table — until confirmed. See
 * [PendingTransaction] and `CaptureTransactionUseCase`.
 */
@Singleton
class PendingTransactionRepository @Inject constructor(
    private val dao: PendingTransactionDao
) {
    fun observeAll(): Flow<List<PendingTransaction>> = dao.getAllFlow()

    fun observeCount(): Flow<Int> = dao.countFlow()

    /** Returns true if the draft was newly recorded (false if a same-id draft already existed). */
    suspend fun record(tx: Transaction): Boolean =
        dao.insertIgnore(PendingTransaction.from(tx)) != -1L

    suspend fun getById(id: String): PendingTransaction? = dao.getById(id)

    suspend fun delete(id: String) = dao.deleteById(id)

    suspend fun clearAll() = dao.clear()
}
