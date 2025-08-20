package com.example.automaticfinances.data.models

import com.example.automaticfinances.data.db.Budget
import com.example.automaticfinances.data.db.BudgetStatus
import com.example.automaticfinances.data.db.Category
import java.time.YearMonth

data class CategorySpending(
    val categoryId: Long,
    val category: Category,
    val amountCents: Long,
    val percentage: Float,
    val transactionCount: Int = 0
) {
    companion object {
        fun fromCategoryAndAmount(category: Category, amountCents: Long, totalCents: Long): CategorySpending {
            val percentage = if (totalCents == 0L) 0f else (amountCents.toFloat() / totalCents.toFloat()) * 100f
            return CategorySpending(
                categoryId = category.id,
                category = category,
                amountCents = amountCents,
                percentage = percentage
            )
        }
    }
}

data class MonthlySpending(
    val yearMonth: YearMonth,
    val totalCents: Long,
    val transactionCount: Int,
    val averageDailySpending: Long = 0L
) {
    companion object {
        fun create(yearMonth: YearMonth, totalCents: Long, transactionCount: Int): MonthlySpending {
            val daysInMonth = yearMonth.lengthOfMonth()
            val averageDaily = totalCents / daysInMonth
            return MonthlySpending(
                yearMonth = yearMonth,
                totalCents = totalCents,
                transactionCount = transactionCount,
                averageDailySpending = averageDaily
            )
        }
    }
}

data class BudgetComparison(
    val budgetStatus: BudgetStatus,
    val utilizationPercentage: Float,
    val projectedVsActual: Float,
    val isOverBudget: Boolean,
    val remainingDays: Int
) {
    companion object {
        fun fromBudgetStatus(budgetStatus: BudgetStatus): BudgetComparison {
            val utilization = budgetStatus.percentageUsed
            val projected = if (budgetStatus.budget.limitAmountCents == 0L) 0f
                           else (budgetStatus.projectedSpentCents.toFloat() / budgetStatus.budget.limitAmountCents.toFloat()) * 100f
            
            return BudgetComparison(
                budgetStatus = budgetStatus,
                utilizationPercentage = utilization,
                projectedVsActual = projected,
                isOverBudget = budgetStatus.isOverBudget,
                remainingDays = budgetStatus.daysLeftInMonth
            )
        }
    }
}

data class ChartData(
    val categorySpending: List<CategorySpending> = emptyList(),
    val monthlyTrend: List<MonthlySpending> = emptyList(),
    val budgetComparisons: List<BudgetComparison> = emptyList(),
    val selectedMonth: YearMonth = YearMonth.now(),
    val totalSpentCents: Long = 0L,
    val isLoading: Boolean = false,
    val error: String? = null
)

enum class ChartType {
    PIE_CATEGORY_SPENDING,
    LINE_MONTHLY_TREND, 
    BAR_BUDGET_COMPARISON
}

data class ChartColors(
    val primary: String = "#1976D2",
    val secondary: String = "#424242", 
    val success: String = "#4CAF50",
    val warning: String = "#FF9800",
    val error: String = "#F44336",
    val surface: String = "#FAFAFA"
)

data class PieChartSector(
    val categorySpending: CategorySpending,
    val startAngle: Float,
    val sweepAngle: Float,
    val color: String,
    val isHighlighted: Boolean = false
)

data class LineChartPoint(
    val monthlySpending: MonthlySpending,
    val x: Float,
    val y: Float,
    val isSelected: Boolean = false
)

data class BarChartItem(
    val budgetComparison: BudgetComparison,
    val budgetBarWidth: Float,
    val spentBarWidth: Float,
    val yPosition: Float,
    val color: String
)