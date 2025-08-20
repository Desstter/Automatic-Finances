package com.example.automaticfinances.data.db

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "financial_goals",
    foreignKeys = [
        ForeignKey(
            entity = Category::class,
            parentColumns = ["id"],
            childColumns = ["categoryId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [
        Index(value = ["categoryId"]),
        Index(value = ["targetDate"]),
        Index(value = ["type"]),
        Index(value = ["isCompleted"])
    ]
)
data class FinancialGoal(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,                        // "Ahorrar para vacaciones"
    val description: String = "",            // Descripción opcional
    val targetAmountCents: Long,             // Meta en centavos
    val currentAmountCents: Long = 0,        // Progreso actual
    val type: GoalType,                      // SAVINGS, EXPENSE_REDUCTION
    val categoryId: Long? = null,            // Para goals específicos de categoría
    val targetDate: Long,                    // Fecha límite (epoch millis)
    val isCompleted: Boolean = false,
    val isActive: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
) {
    val progressPercentage: Float
        get() = if (targetAmountCents == 0L) 0f 
                else (currentAmountCents.toFloat() / targetAmountCents.toFloat()) * 100f
    
    val isOverdue: Boolean
        get() = System.currentTimeMillis() > targetDate && !isCompleted
    
    val remainingAmountCents: Long
        get() = maxOf(0, targetAmountCents - currentAmountCents)
}

enum class GoalType {
    SAVINGS,            // Meta de ahorro
    EXPENSE_REDUCTION   // Meta de reducir gastos en categoría
}

data class GoalProgress(
    val goal: FinancialGoal,
    val category: Category?,
    val weeklyTargetCents: Long,      // Cuánto necesita ahorrar/reducir por semana
    val monthlyTargetCents: Long,     // Cuánto necesita ahorrar/reducir por mes
    val daysRemaining: Int,
    val isOnTrack: Boolean,           // Si va en buen camino para cumplir la meta
    val projectedCompletionDate: Long?, // Proyección de cuándo completará la meta
    val recommendedAction: String     // Sugerencia de acción
)

data class GoalsSummary(
    val totalGoals: Int,
    val activeGoals: Int,
    val completedGoals: Int,
    val overdueGoals: Int,
    val totalTargetCents: Long,
    val totalCurrentCents: Long,
    val averageProgress: Float
)