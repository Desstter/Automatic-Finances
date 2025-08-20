package com.example.automaticfinances.data.repo

import com.example.automaticfinances.data.db.*
import com.example.automaticfinances.data.models.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.first
import java.time.YearMonth
import java.time.format.DateTimeFormatter

class AnalyticsRepository(
    private val transactionRepository: TransactionRepository,
    private val budgetRepository: BudgetRepository,
    private val categoryRepository: CategoryRepository
) {
    private val transactionDao = AppDatabase.get().transactionDao()
    private val categoryDao = AppDatabase.get().categoryDao()
    
    suspend fun getCategorySpendingForMonth(yearMonth: YearMonth): List<CategorySpending> {
        val year = yearMonth.year
        val month = yearMonth.monthValue
        
        // Get all active categories
        val categories = categoryRepository.getAllActiveSync()
        val categorySpendingList = mutableListOf<CategorySpending>()
        var totalSpent = 0L
        
        // Calculate spending per category
        for (category in categories) {
            val spent = transactionRepository.getSpentByCategoryInMonth(category.id, year, month)
            if (spent > 0) {
                totalSpent += spent
                categorySpendingList.add(
                    CategorySpending(
                        categoryId = category.id,
                        category = category,
                        amountCents = spent,
                        percentage = 0f // Will be calculated below
                    )
                )
            }
        }
        
        // Handle uncategorized transactions
        val uncategorizedSpent = getUncategorizedSpentForMonth(year, month)
        if (uncategorizedSpent > 0) {
            totalSpent += uncategorizedSpent
            val uncategorizedCategory = Category(
                id = -1L,
                name = "Sin categoría",
                color = "#9E9E9E",
                icon = "❓",
                isDefault = false
            )
            categorySpendingList.add(
                CategorySpending(
                    categoryId = -1L,
                    category = uncategorizedCategory,
                    amountCents = uncategorizedSpent,
                    percentage = 0f
                )
            )
        }
        
        // Calculate percentages
        return categorySpendingList.map { categorySpending ->
            val percentage = if (totalSpent == 0L) 0f 
                           else (categorySpending.amountCents.toFloat() / totalSpent.toFloat()) * 100f
            categorySpending.copy(percentage = percentage)
        }.sortedByDescending { it.amountCents }
    }
    
    suspend fun getMonthlySpendingTrend(monthsBack: Int = 6): List<MonthlySpending> {
        val currentMonth = YearMonth.now()
        val monthlySpendingList = mutableListOf<MonthlySpending>()
        
        for (i in (monthsBack - 1) downTo 0) {
            val targetMonth = currentMonth.minusMonths(i.toLong())
            val spent = transactionRepository.getMonthlySpentTotal(
                targetMonth.year, 
                targetMonth.monthValue
            )
            val transactionCount = getTransactionCountForMonth(targetMonth.year, targetMonth.monthValue)
            
            monthlySpendingList.add(
                MonthlySpending.create(
                    yearMonth = targetMonth,
                    totalCents = spent,
                    transactionCount = transactionCount
                )
            )
        }
        
        return monthlySpendingList
    }
    
    suspend fun getBudgetComparisonsForMonth(yearMonth: YearMonth): List<BudgetComparison> {
        val budgetStatuses = budgetRepository.getBudgetStatusForMonth(yearMonth)
        
        return budgetStatuses.first().map { budgetStatus ->
            BudgetComparison.fromBudgetStatus(budgetStatus)
        }.sortedByDescending { budgetComparison -> budgetComparison.utilizationPercentage }
    }
    
    fun getChartDataForMonth(yearMonth: YearMonth): Flow<ChartData> = flow {
        emit(ChartData(isLoading = true, selectedMonth = yearMonth))
        
        try {
            val categorySpending = getCategorySpendingForMonth(yearMonth)
            val monthlyTrend = getMonthlySpendingTrend()
            val budgetComparisons = getBudgetComparisonsForMonth(yearMonth)
            val totalSpent = categorySpending.sumOf { it.amountCents }
            
            emit(
                ChartData(
                    categorySpending = categorySpending,
                    monthlyTrend = monthlyTrend,
                    budgetComparisons = budgetComparisons,
                    selectedMonth = yearMonth,
                    totalSpentCents = totalSpent,
                    isLoading = false
                )
            )
        } catch (e: Exception) {
            emit(
                ChartData(
                    selectedMonth = yearMonth,
                    isLoading = false,
                    error = "Error al cargar datos: ${e.message}"
                )
            )
        }
    }
    
    private suspend fun getUncategorizedSpentForMonth(year: Int, month: Int): Long {
        val startDate = String.format("%04d-%02d-01", year, month)
        val endDate = String.format("%04d-%02d-31", year, month)
        return transactionDao.getUncategorizedTotalForDateRange(startDate, endDate) ?: 0L
    }
    
    private suspend fun getTransactionCountForMonth(year: Int, month: Int): Int {
        val startDate = String.format("%04d-%02d-01", year, month)
        val endDate = String.format("%04d-%02d-31", year, month)
        return transactionDao.getTransactionCountForDateRange(startDate, endDate)
    }
    
    // Summary statistics for dashboard
    suspend fun getSpendingSummary(yearMonth: YearMonth): SpendingSummary {
        val categorySpending = getCategorySpendingForMonth(yearMonth)
        val totalSpent = categorySpending.sumOf { it.amountCents }
        val topCategory = categorySpending.maxByOrNull { it.amountCents }
        val categoryCount = categorySpending.size
        
        return SpendingSummary(
            totalSpentCents = totalSpent,
            topCategory = topCategory?.category,
            topCategoryAmountCents = topCategory?.amountCents ?: 0L,
            categoriesUsed = categoryCount,
            averagePerCategory = if (categoryCount > 0) totalSpent / categoryCount else 0L
        )
    }
}

data class SpendingSummary(
    val totalSpentCents: Long,
    val topCategory: Category?,
    val topCategoryAmountCents: Long,
    val categoriesUsed: Int,
    val averagePerCategory: Long
)