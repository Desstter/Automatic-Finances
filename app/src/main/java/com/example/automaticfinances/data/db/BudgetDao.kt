package com.example.automaticfinances.data.db

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface BudgetDao {
    
    @Query("SELECT * FROM budgets WHERE isActive = 1 ORDER BY year DESC, month DESC")
    fun getAllActiveBudgets(): Flow<List<Budget>>
    
    @Query("SELECT * FROM budgets WHERE year = :year AND month = :month AND isActive = 1")
    fun getBudgetsForMonth(year: Int, month: Int): Flow<List<Budget>>
    
    @Query("SELECT * FROM budgets WHERE categoryId = :categoryId AND year = :year AND month = :month AND isActive = 1")
    suspend fun getBudgetForCategoryAndMonth(categoryId: Long, year: Int, month: Int): Budget?
    
    @Query("SELECT * FROM budgets WHERE id = :id")
    suspend fun getBudgetById(id: Long): Budget?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBudget(budget: Budget): Long
    
    @Update
    suspend fun updateBudget(budget: Budget)
    
    @Query("UPDATE budgets SET isActive = 0 WHERE id = :id")
    suspend fun deactivateBudget(id: Long)
    
    @Query("DELETE FROM budgets WHERE id = :id")
    suspend fun deleteBudget(id: Long)
    
    // Analytics queries
    @Query("""
        SELECT b.*, c.name as categoryName, c.color as categoryColor, c.icon as categoryIcon,
               COALESCE(spent.totalSpent, 0) as currentSpentCents
        FROM budgets b 
        INNER JOIN categories c ON b.categoryId = c.id
        LEFT JOIN (
            SELECT categoryId, SUM(amountCents) as totalSpent
            FROM transactions 
            WHERE strftime('%Y', date) = :year AND strftime('%m', date) = :month
            GROUP BY categoryId
        ) spent ON b.categoryId = spent.categoryId
        WHERE b.year = :year AND b.month = :month AND b.isActive = 1
        ORDER BY (COALESCE(spent.totalSpent, 0) * 1.0 / b.limitAmountCents) DESC
    """)
    fun getBudgetStatusForMonth(year: String, month: String): Flow<List<BudgetWithSpending>>
    
    @Query("""
        SELECT 
            SUM(limitAmountCents) as totalBudgetCents,
            SUM(COALESCE(spent.totalSpent, 0)) as totalSpentCents,
            COUNT(*) as budgetsCount
        FROM budgets b
        LEFT JOIN (
            SELECT categoryId, SUM(amountCents) as totalSpent
            FROM transactions 
            WHERE strftime('%Y', date) = :year AND strftime('%m', date) = :month
            GROUP BY categoryId
        ) spent ON b.categoryId = spent.categoryId
        WHERE b.year = :yearInt AND b.month = :monthInt AND b.isActive = 1
    """)
    suspend fun getBudgetSummaryForMonth(year: String, month: String, yearInt: Int, monthInt: Int): BudgetSummaryRaw?
}

// Data classes for query results
data class BudgetWithSpending(
    val id: Long,
    val categoryId: Long,
    val limitAmountCents: Long,
    val year: Int,
    val month: Int,
    val alertAt50Percent: Boolean,
    val alertAt75Percent: Boolean,
    val alertAt100Percent: Boolean,
    val categoryName: String,
    val categoryColor: String,
    val categoryIcon: String,
    val currentSpentCents: Long
)

data class BudgetSummaryRaw(
    val totalBudgetCents: Long,
    val totalSpentCents: Long,
    val budgetsCount: Int
)