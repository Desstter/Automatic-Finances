package com.example.automaticfinances.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface PendingTransactionDao {

    /**
     * @return the new rowId, or -1 if a draft with the same id already existed (re-delivery).
     */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIgnore(item: PendingTransaction): Long

    @Query("SELECT * FROM pending_transactions ORDER BY capturedAt DESC")
    fun getAllFlow(): Flow<List<PendingTransaction>>

    @Query("SELECT COUNT(*) FROM pending_transactions")
    fun countFlow(): Flow<Int>

    @Query("SELECT * FROM pending_transactions WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): PendingTransaction?

    @Query("DELETE FROM pending_transactions WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM pending_transactions")
    suspend fun clear()
}
