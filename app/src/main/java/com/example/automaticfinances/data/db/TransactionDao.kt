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
    
    @Query("""
        SELECT t.*, c.name as categoryName, c.icon as categoryIcon, c.color as categoryColor
        FROM transactions t
        LEFT JOIN categories c ON t.categoryId = c.id
        ORDER BY t.date DESC, t.time DESC
    """)
    suspend fun getTransactionsWithCategoriesSync(): List<TransactionWithCategory>
    
    @Query("UPDATE transactions SET categoryId = :categoryId WHERE id = :transactionId")
    suspend fun updateCategory(transactionId: String, categoryId: Long)
    
    @Query("""
        SELECT COALESCE(SUM(amountCents), 0) FROM transactions 
        WHERE categoryId IS NULL AND date BETWEEN :startDate AND :endDate
    """)
    suspend fun getUncategorizedTotalForDateRange(startDate: String, endDate: String): Long?
    
    @Query("""
        SELECT COALESCE(SUM(amountCents), 0) FROM transactions 
        WHERE isIncome = 0 AND categoryId IS NULL AND date BETWEEN :startDate AND :endDate
    """)
    suspend fun getUncategorizedExpenseTotalForDateRange(startDate: String, endDate: String): Long?
    
    @Query("""
        SELECT COUNT(*) FROM transactions 
        WHERE date BETWEEN :startDate AND :endDate
    """)
    suspend fun getTransactionCountForDateRange(startDate: String, endDate: String): Int
    
    // ======= INCOME/EXPENSE SPECIFIC QUERIES =======
    
    @Query("SELECT * FROM transactions WHERE isIncome = 1 ORDER BY date DESC, time DESC")
    fun getIncomes(): Flow<List<Transaction>>
    
    @Query("SELECT * FROM transactions WHERE isIncome = 0 ORDER BY date DESC, time DESC")
    fun getExpenses(): Flow<List<Transaction>>
    
    @Query("""
        SELECT t.*, c.name as categoryName, c.icon as categoryIcon, c.color as categoryColor
        FROM transactions t
        LEFT JOIN categories c ON t.categoryId = c.id
        WHERE t.isIncome = 1
        ORDER BY t.date DESC, t.time DESC
    """)
    fun getIncomesWithCategories(): Flow<List<TransactionWithCategory>>
    
    @Query("""
        SELECT t.*, c.name as categoryName, c.icon as categoryIcon, c.color as categoryColor
        FROM transactions t
        LEFT JOIN categories c ON t.categoryId = c.id
        WHERE t.isIncome = 0
        ORDER BY t.date DESC, t.time DESC
    """)
    fun getExpensesWithCategories(): Flow<List<TransactionWithCategory>>
    
    @Query("""
        SELECT COALESCE(SUM(amountCents), 0) FROM transactions 
        WHERE isIncome = 1 AND date LIKE printf('%04d-%02d%%', :year, :month)
    """)
    suspend fun getMonthlyIncomeTotal(year: Int, month: Int): Long
    
    @Query("""
        SELECT COALESCE(SUM(amountCents), 0) FROM transactions 
        WHERE isIncome = 0 AND date LIKE printf('%04d-%02d%%', :year, :month)
    """)
    suspend fun getMonthlyExpenseTotal(year: Int, month: Int): Long
    
    @Query("""
        SELECT COALESCE(SUM(amountCents), 0) FROM transactions 
        WHERE isIncome = 1 AND date BETWEEN :startDate AND :endDate
    """)
    suspend fun getIncomeTotalForDateRange(startDate: String, endDate: String): Long
    
    @Query("""
        SELECT COALESCE(SUM(amountCents), 0) FROM transactions 
        WHERE isIncome = 0 AND date BETWEEN :startDate AND :endDate
    """)
    suspend fun getExpenseTotalForDateRange(startDate: String, endDate: String): Long
    
    @Query("""
        SELECT COUNT(*) FROM transactions 
        WHERE isIncome = 1 AND date BETWEEN :startDate AND :endDate
    """)
    suspend fun getIncomeCountForDateRange(startDate: String, endDate: String): Int
    
    @Query("""
        SELECT COUNT(*) FROM transactions 
        WHERE isIncome = 0 AND date BETWEEN :startDate AND :endDate
    """)
    suspend fun getExpenseCountForDateRange(startDate: String, endDate: String): Int
    
    @Query("""
        SELECT COALESCE(SUM(amountCents), 0) FROM transactions 
        WHERE isIncome = 0 AND categoryId = :categoryId AND date BETWEEN :startDate AND :endDate
    """)
    suspend fun getExpenseTotalByCategoryAndDateRange(categoryId: Long, startDate: String, endDate: String): Long
    
    @Query("""
        SELECT COALESCE(SUM(amountCents), 0) FROM transactions 
        WHERE isIncome = 1 AND categoryId = :categoryId AND date BETWEEN :startDate AND :endDate
    """)
    suspend fun getIncomeTotalByCategoryAndDateRange(categoryId: Long, startDate: String, endDate: String): Long
    
    @Query("""
        SELECT COUNT(*) FROM transactions 
        WHERE isIncome = 1 AND categoryId = :categoryId AND date BETWEEN :startDate AND :endDate
    """)
    suspend fun getIncomeCountByCategoryAndDateRange(categoryId: Long, startDate: String, endDate: String): Int
    
    @Query("""
        SELECT COUNT(*) FROM transactions 
        WHERE isIncome = 0 AND categoryId = :categoryId AND date BETWEEN :startDate AND :endDate
    """)
    suspend fun getExpenseCountByCategoryAndDateRange(categoryId: Long, startDate: String, endDate: String): Int
    
    // ================ OPENING BALANCE SUPPORT METHODS ================
    
    @Query("""
        SELECT * FROM transactions 
        WHERE accountId = :accountId AND date BETWEEN :startDate AND :endDate
        ORDER BY date ASC, time ASC
    """)
    suspend fun getByAccountAndDateRangeSync(accountId: Long, startDate: String, endDate: String): List<Transaction>
    
    @Query("""
        SELECT COUNT(*) FROM transactions 
        WHERE accountId = :accountId AND date BETWEEN :startDate AND :endDate
    """)
    suspend fun getTransactionCountByAccountAndDateRange(accountId: Long, startDate: String, endDate: String): Int
    
    @Query("""
        SELECT * FROM transactions 
        WHERE accountId = :accountId AND date >= :fromDate
        ORDER BY date ASC, time ASC
    """)
    suspend fun getByAccountFromDate(accountId: Long, fromDate: String): List<Transaction>
    
    @Query("""
        SELECT COUNT(*) FROM transactions 
        WHERE accountId = :accountId AND date >= :fromDate
    """)
    suspend fun getTransactionCountByAccountFromDate(accountId: Long, fromDate: String): Int
}