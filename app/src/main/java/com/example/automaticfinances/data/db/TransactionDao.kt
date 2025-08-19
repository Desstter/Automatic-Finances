package com.example.automaticfinances.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIgnore(tx: Transaction)

    @Query("SELECT * FROM transactions ORDER BY ts DESC")
    fun all(): Flow<List<Transaction>>

    @Query("""
        SELECT COALESCE(SUM(amountCents),0) FROM transactions
        WHERE ts BETWEEN :from AND :to AND (type = :type OR :type = 'ALL')
    """)
    fun sumByType(from: Long, to: Long, type: String): Flow<Long>
}