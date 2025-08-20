package com.example.automaticfinances.data.db

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface FinancialGoalDao {
    
    @Query("SELECT * FROM financial_goals WHERE isActive = 1 ORDER BY targetDate ASC")
    fun getAllActiveGoals(): Flow<List<FinancialGoal>>
    
    @Query("SELECT * FROM financial_goals WHERE isCompleted = 0 AND isActive = 1 ORDER BY targetDate ASC")
    fun getActiveIncompleteGoals(): Flow<List<FinancialGoal>>
    
    @Query("SELECT * FROM financial_goals WHERE id = :id")
    suspend fun getGoalById(id: Long): FinancialGoal?
    
    @Query("SELECT * FROM financial_goals WHERE type = :type AND isActive = 1")
    fun getGoalsByType(type: GoalType): Flow<List<FinancialGoal>>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGoal(goal: FinancialGoal): Long
    
    @Update
    suspend fun updateGoal(goal: FinancialGoal)
    
    @Query("UPDATE financial_goals SET currentAmountCents = :newAmount WHERE id = :id")
    suspend fun updateGoalProgress(id: Long, newAmount: Long)
    
    @Query("UPDATE financial_goals SET isCompleted = 1 WHERE id = :id")
    suspend fun markGoalAsCompleted(id: Long)
    
    @Query("UPDATE financial_goals SET isActive = 0 WHERE id = :id")
    suspend fun deactivateGoal(id: Long)
    
    @Query("DELETE FROM financial_goals WHERE id = :id")
    suspend fun deleteGoal(id: Long)
    
    // Analytics queries
    @Query("""
        SELECT fg.id, fg.name, fg.description, fg.targetAmountCents, fg.currentAmountCents, 
               fg.type, fg.categoryId, fg.targetDate, fg.isCompleted,
               c.name as categoryName, c.color as categoryColor, c.icon as categoryIcon
        FROM financial_goals fg
        LEFT JOIN categories c ON fg.categoryId = c.id
        WHERE fg.isActive = 1
        ORDER BY fg.targetDate ASC
    """)
    fun getGoalsWithCategories(): Flow<List<GoalWithCategory>>
    
    @Query("""
        SELECT 
            COUNT(*) as totalGoals,
            SUM(CASE WHEN isCompleted = 0 THEN 1 ELSE 0 END) as activeGoals,
            SUM(CASE WHEN isCompleted = 1 THEN 1 ELSE 0 END) as completedGoals,
            SUM(CASE WHEN targetDate < :currentTime AND isCompleted = 0 THEN 1 ELSE 0 END) as overdueGoals,
            SUM(targetAmountCents) as totalTargetCents,
            SUM(currentAmountCents) as totalCurrentCents
        FROM financial_goals 
        WHERE isActive = 1
    """)
    suspend fun getGoalsSummary(currentTime: Long): GoalsSummaryRaw?
    
    @Query("""
        SELECT * FROM financial_goals 
        WHERE targetDate < :currentTime AND isCompleted = 0 AND isActive = 1
        ORDER BY targetDate ASC
    """)
    suspend fun getOverdueGoals(currentTime: Long): List<FinancialGoal>
}

// Data classes for query results
data class GoalWithCategory(
    val id: Long,
    val name: String,
    val description: String,
    val targetAmountCents: Long,
    val currentAmountCents: Long,
    val type: GoalType,
    val categoryId: Long?,
    val targetDate: Long,
    val isCompleted: Boolean,
    val categoryName: String?,
    val categoryColor: String?,
    val categoryIcon: String?
)

data class GoalsSummaryRaw(
    val totalGoals: Int,
    val activeGoals: Int,
    val completedGoals: Int,
    val overdueGoals: Int,
    val totalTargetCents: Long,
    val totalCurrentCents: Long
)