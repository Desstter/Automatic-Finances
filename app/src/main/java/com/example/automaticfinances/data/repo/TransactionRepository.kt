package com.example.automaticfinances.data.repo

import com.example.automaticfinances.data.db.AppDatabase
import com.example.automaticfinances.data.db.Transaction
import kotlinx.coroutines.flow.Flow

class TransactionRepository {
    private val dao = AppDatabase.get().transactionDao()
    
    suspend fun insert(tx: Transaction) = dao.insertIgnore(tx)
    
    suspend fun update(tx: Transaction) = dao.update(tx)
    
    fun all(): Flow<List<Transaction>> = dao.all()
    
    fun getByCategory(categoryId: Long): Flow<List<Transaction>> = dao.getByCategoryId(categoryId)
    
    fun getByDateRange(startDate: String, endDate: String): Flow<List<Transaction>> = 
        dao.getByDateRange(startDate, endDate)
    
    fun getByCategoryAndDateRange(categoryId: Long, startDate: String, endDate: String): Flow<List<Transaction>> = 
        dao.getByCategoryAndDateRange(categoryId, startDate, endDate)
    
    suspend fun getById(id: String): Transaction? = dao.getById(id)
    
    fun getTransactionsWithCategories(): Flow<List<TransactionWithCategory>> = dao.getTransactionsWithCategories()
    
    suspend fun getTotalByCategory(categoryId: Long): Long = dao.getTotalByCategory(categoryId)
    
    suspend fun getTotalByCategoryAndDateRange(categoryId: Long, startDate: String, endDate: String): Long = 
        dao.getTotalByCategoryAndDateRange(categoryId, startDate, endDate)
    
    suspend fun getMonthlyTotal(year: Int, month: Int): Long = dao.getMonthlyTotal(year, month)
    
    suspend fun getTransactionsWithCategoriesSync(): List<TransactionWithCategory> = dao.getTransactionsWithCategoriesSync()
    
    suspend fun updateTransactionCategory(transactionId: String, categoryId: Long) {
        dao.updateCategory(transactionId, categoryId)
    }
    
    // New methods for Financial Insights
    suspend fun getMonthlySpentTotal(year: Int, month: Int): Long {
        return dao.getMonthlyTotal(year, month)
    }
    
    suspend fun getSpentByCategoryInMonth(categoryId: Long, year: Int, month: Int): Long {
        val startDate = String.format("%04d-%02d-01", year, month)
        val endDate = String.format("%04d-%02d-31", year, month) // Simplified, good enough for 20/80
        return dao.getTotalByCategoryAndDateRange(categoryId, startDate, endDate)
    }
    
    // ======= INCOME/EXPENSE SPECIFIC METHODS =======
    
    fun getIncomes(): Flow<List<Transaction>> = dao.getIncomes()
    
    fun getExpenses(): Flow<List<Transaction>> = dao.getExpenses()
    
    fun getIncomesWithCategories(): Flow<List<TransactionWithCategory>> = dao.getIncomesWithCategories()
    
    fun getExpensesWithCategories(): Flow<List<TransactionWithCategory>> = dao.getExpensesWithCategories()
    
    suspend fun getMonthlyIncomeTotal(year: Int, month: Int): Long = dao.getMonthlyIncomeTotal(year, month)
    
    suspend fun getMonthlyExpenseTotal(year: Int, month: Int): Long = dao.getMonthlyExpenseTotal(year, month)
    
    suspend fun getMonthlyNetBalance(year: Int, month: Int): Long {
        val income = getMonthlyIncomeTotal(year, month)
        val expense = getMonthlyExpenseTotal(year, month)
        return income - expense
    }
    
    suspend fun getIncomeTotalForDateRange(startDate: String, endDate: String): Long = 
        dao.getIncomeTotalForDateRange(startDate, endDate)
    
    suspend fun getExpenseTotalForDateRange(startDate: String, endDate: String): Long = 
        dao.getExpenseTotalForDateRange(startDate, endDate)
    
    suspend fun getNetBalanceForDateRange(startDate: String, endDate: String): Long {
        val income = getIncomeTotalForDateRange(startDate, endDate)
        val expense = getExpenseTotalForDateRange(startDate, endDate)
        return income - expense
    }
    
    suspend fun getIncomeCountForDateRange(startDate: String, endDate: String): Int = 
        dao.getIncomeCountForDateRange(startDate, endDate)
    
    suspend fun getExpenseCountForDateRange(startDate: String, endDate: String): Int = 
        dao.getExpenseCountForDateRange(startDate, endDate)
}

data class TransactionWithCategory(
    val id: String,
    val ts: Long,
    val date: String,
    val time: String,
    val type: String,
    val description: String,
    val amountCents: Long,
    val currency: String,
    val srcLast4: String?,
    val dstLast4: String?,
    val source: String,
    val categoryId: Long?,
    val notes: String,
    val isIncome: Boolean,
    val rawPreview: String,
    val categoryName: String?,
    val categoryIcon: String?,
    val categoryColor: String?
)