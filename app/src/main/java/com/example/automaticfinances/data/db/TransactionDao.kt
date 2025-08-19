package com.example.automaticfinances.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.automaticfinances.data.repo.TransactionWithCategory
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIgnore(tx: Transaction)
    
    @Update
    suspend fun update(tx: Transaction)

    @Query("SELECT * FROM transactions ORDER BY date DESC, time DESC")
    fun all(): Flow<List<Transaction>>
    
    @Query("SELECT * FROM transactions WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): Transaction?

    @Query("SELECT * FROM transactions WHERE categoryId = :categoryId ORDER BY date DESC, time DESC")
    fun getByCategoryId(categoryId: Long): Flow<List<Transaction>>
    
    @Query("SELECT * FROM transactions WHERE date BETWEEN :startDate AND :endDate ORDER BY date DESC, time DESC")
    fun getByDateRange(startDate: String, endDate: String): Flow<List<Transaction>>
    
    @Query("""
        SELECT * FROM transactions 
        WHERE categoryId = :categoryId AND date BETWEEN :startDate AND :endDate 
        ORDER BY date DESC, time DESC
    """)
    fun getByCategoryAndDateRange(categoryId: Long, startDate: String, endDate: String): Flow<List<Transaction>>

    @Query("""
        SELECT t.*, c.name as categoryName, c.icon as categoryIcon, c.color as categoryColor
        FROM transactions t
        LEFT JOIN categories c ON t.categoryId = c.id
        ORDER BY t.date DESC, t.time DESC
    """)
    fun getTransactionsWithCategories(): Flow<List<TransactionWithCategory>>
    
    @Query("SELECT COALESCE(SUM(amountCents), 0) FROM transactions WHERE categoryId = :categoryId")
    suspend fun getTotalByCategory(categoryId: Long): Long
    
    @Query("""
        SELECT COALESCE(SUM(amountCents), 0) FROM transactions 
        WHERE categoryId = :categoryId AND date BETWEEN :startDate AND :endDate
    """)
    suspend fun getTotalByCategoryAndDateRange(categoryId: Long, startDate: String, endDate: String): Long
    
    @Query("""
        SELECT COALESCE(SUM(amountCents), 0) FROM transactions 
        WHERE date LIKE printf('%04d-%02d%%', :year, :month)
    """)
    suspend fun getMonthlyTotal(year: Int, month: Int): Long

    @Query("""
        SELECT COALESCE(SUM(amountCents),0) FROM transactions
        WHERE ts BETWEEN :from AND :to AND (type = :type OR :type = 'ALL')
    """)
    fun sumByType(from: Long, to: Long, type: String): Flow<Long>
}