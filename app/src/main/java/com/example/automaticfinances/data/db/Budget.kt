package com.example.automaticfinances.data.db

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.YearMonth

@Entity(
    tableName = "budgets",
    foreignKeys = [
        ForeignKey(
            entity = Category::class,
            parentColumns = ["id"],
            childColumns = ["categoryId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["categoryId"]),
        Index(value = ["year", "month"]),
        Index(value = ["categoryId", "year", "month"], unique = true)
    ]
)
data class Budget(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val categoryId: Long,
    val limitAmountCents: Long,      // Límite en centavos
    val year: Int,                   // 2024
    val month: Int,                  // 1-12
    val alertAt50Percent: Boolean = true,
    val alertAt75Percent: Boolean = true,
    val alertAt100Percent: Boolean = true,
    val isActive: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
) {
    companion object {
        fun forCurrentMonth(categoryId: Long, limitAmountCents: Long): Budget {
            val currentMonth = YearMonth.now()
            return Budget(
                categoryId = categoryId,
                limitAmountCents = limitAmountCents,
                year = currentMonth.year,
                month = currentMonth.monthValue
            )
        }
        
        fun getMonthKey(year: Int, month: Int): String = "$year-${month.toString().padStart(2, '0')}"
    }
    
    val monthKey: String get() = getMonthKey(year, month)
    val yearMonth: YearMonth get() = YearMonth.of(year, month)
}

// Data classes for analytics
data class BudgetStatus(
    val budget: Budget,
    val category: Category,
    val currentSpentCents: Long,
    val remainingCents: Long,
    val percentageUsed: Float,
    val projectedSpentCents: Long, // Proyección basada en tendencia actual
    val daysLeftInMonth: Int,
    val isOverBudget: Boolean,
    val alertLevel: BudgetAlertLevel
)

enum class BudgetAlertLevel {
    SAFE,           // < 50%
    WARNING,        // 50-74%
    CRITICAL,       // 75-99%
    OVER_BUDGET     // >= 100%
}

data class BudgetSummary(
    val totalBudgetCents: Long,
    val totalSpentCents: Long,
    val totalRemainingCents: Long,
    val overallPercentageUsed: Float,
    val budgetsCount: Int,
    val overBudgetCount: Int,
    val criticalCount: Int
)