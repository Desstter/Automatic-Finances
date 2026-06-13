package com.example.automaticfinances.ui.reports

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.automaticfinances.data.repo.TransactionRepository
import com.example.automaticfinances.data.repo.CategoryRepository
import com.example.automaticfinances.data.repo.AnalyticsRepository
import com.example.automaticfinances.utils.centsToCopString
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.text.NumberFormat
import java.util.*
import kotlin.math.roundToInt
import javax.inject.Inject

@HiltViewModel
class ReportsViewModel @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val categoryRepository: CategoryRepository,
    private val analyticsRepository: AnalyticsRepository
) : ViewModel() {
    
    private val _state = MutableStateFlow(ReportsState())
    val state: StateFlow<ReportsState> = _state.asStateFlow()
    
    fun loadReports() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            
            try {
                val period = _state.value.selectedPeriod
                val (startDate, endDate) = getDateRange(period)
                
                Log.d("ReportsViewModel", "Loading reports for period $period: $startDate to $endDate")
                
                // Load summary data
                loadSummaryData(startDate, endDate, period)
                
                // Load category breakdown
                loadCategoryBreakdown(startDate, endDate)
                
                // Load monthly trends
                loadMonthlyTrends(period)
                
                // Load top transactions
                loadTopTransactions(startDate, endDate)
                
                // Generate insights
                generateInsights()
                
                Log.d("ReportsViewModel", "Reports loaded successfully")
                _state.update { it.copy(isLoading = false) }
            } catch (e: Exception) {
                Log.e("ReportsViewModel", "Error loading reports", e)
                _state.update { 
                    it.copy(
                        error = "Error cargando reportes: ${e.message}",
                        isLoading = false
                    ) 
                }
            }
        }
    }
    
    fun selectPeriod(period: ReportPeriod) {
        _state.update { it.copy(selectedPeriod = period) }
        loadReports()
    }
    
    private suspend fun loadSummaryData(startDate: LocalDate, endDate: LocalDate, period: ReportPeriod) {
        try {
            val startDateStr = startDate.toString()
            val endDateStr = endDate.toString()
            
            Log.d("ReportsViewModel", "Loading summary data for range: $startDateStr to $endDateStr")
            
            // Use first() instead of collect to get single emission and complete
            val transactions = transactionRepository.getByDateRange(startDateStr, endDateStr).first()
            
            // Separate income and expenses for proper reporting
            val expenses = transactions.filter { !it.isIncome }
            val incomes = transactions.filter { it.isIncome }
            
            val totalSpent = expenses.sumOf { it.amountCents }
            val totalIncome = incomes.sumOf { it.amountCents }
            val netBalance = totalIncome - totalSpent
            
            val transactionCount = transactions.size
            val expenseCount = expenses.size
            val incomeCount = incomes.size
            val categoriesUsed = expenses.mapNotNull { it.categoryId }.distinct().size
            
            val daysDiff = java.time.temporal.ChronoUnit.DAYS.between(startDate, endDate).toInt() + 1
            val dailyAverage = if (daysDiff > 0) totalSpent / daysDiff else 0L
            
            // Calculate percentage change
            val percentageChange = calculatePercentageChange(period, totalSpent)
            
            Log.d("ReportsViewModel", "Summary loaded: ${transactions.size} transactions, ${expenses.size} expenses, ${incomes.size} incomes")
            
            val summary = ReportsSummary(
                totalSpentCents = totalSpent,
                totalIncomeCents = totalIncome,
                netBalanceCents = netBalance,
                dailyAverageCents = dailyAverage,
                transactionCount = transactionCount,
                expenseCount = expenseCount,
                incomeCount = incomeCount,
                categoriesUsed = categoriesUsed,
                percentageChange = percentageChange
            )
            
            _state.update { it.copy(summary = summary) }
        } catch (e: Exception) {
            Log.e("ReportsViewModel", "Error loading summary data", e)
            // Handle error, but don't fail the entire load
        }
    }
    
    private suspend fun loadCategoryBreakdown(startDate: LocalDate, endDate: LocalDate) {
        try {
            val startDateStr = startDate.toString()
            val endDateStr = endDate.toString()
            
            Log.d("ReportsViewModel", "Loading category breakdown for range: $startDateStr to $endDateStr")
            
            // Use first() to get single emission and complete
            val transactions = transactionRepository.getByDateRange(startDateStr, endDateStr).first()
            
            // Only consider expenses for category breakdown (not income)
            val expenseTransactions = transactions.filter { !it.isIncome }
            
            val categories = categoryRepository.getAllActiveSync()
            val totalSpent = expenseTransactions.sumOf { it.amountCents }
            
            val breakdown = expenseTransactions
                .groupBy { it.categoryId }
                .map { (categoryId, categoryTransactions) ->
                    val category = categories.find { it.id == categoryId }
                    val amount = categoryTransactions.sumOf { it.amountCents }
                    val percentage = if (totalSpent > 0) (amount.toFloat() / totalSpent.toFloat()) * 100f else 0f
                    
                    CategoryBreakdown(
                        categoryId = categoryId ?: 0L,
                        categoryName = category?.name ?: "Sin categoría",
                        categoryIcon = category?.icon ?: "📂",
                        amountCents = amount,
                        transactionCount = categoryTransactions.size,
                        percentage = percentage
                    )
                }
                .sortedByDescending { it.amountCents }
            
            Log.d("ReportsViewModel", "Category breakdown loaded: ${breakdown.size} categories")
            _state.update { it.copy(categoryBreakdown = breakdown) }
        } catch (e: Exception) {
            Log.e("ReportsViewModel", "Error loading category breakdown", e)
        }
    }
    
    private suspend fun loadMonthlyTrends(period: ReportPeriod) {
        try {
            val monthsToLoad = when (period) {
                ReportPeriod.CURRENT_MONTH -> 1
                ReportPeriod.LAST_MONTH -> 2
                ReportPeriod.LAST_3_MONTHS -> 3
                ReportPeriod.LAST_6_MONTHS -> 6
                ReportPeriod.CURRENT_YEAR -> 12
            }
            
            val trends = mutableListOf<MonthlyTrend>()
            val currentMonth = YearMonth.now()
            val formatter = DateTimeFormatter.ofPattern("MMM yyyy", Locale.forLanguageTag("es-CO"))
            
            for (i in monthsToLoad - 1 downTo 0) {
                val month = currentMonth.minusMonths(i.toLong())
                val monthTotal = transactionRepository.getMonthlySpentTotal(month.year, month.monthValue)
                
                val previousMonthTotal = if (i < monthsToLoad - 1) {
                    transactionRepository.getMonthlySpentTotal(
                        month.minusMonths(1).year,
                        month.minusMonths(1).monthValue
                    )
                } else null
                
                val changePercentage = if (previousMonthTotal != null && previousMonthTotal > 0) {
                    ((monthTotal.toFloat() - previousMonthTotal.toFloat()) / previousMonthTotal.toFloat()) * 100f
                } else null
                
                trends.add(
                    MonthlyTrend(
                        yearMonth = month,
                        monthName = month.format(formatter).replaceFirstChar { it.uppercase() },
                        totalCents = monthTotal,
                        changePercentage = changePercentage
                    )
                )
            }
            
            _state.update { it.copy(monthlyTrends = trends) }
        } catch (e: Exception) {
            // Handle error
        }
    }
    
    private suspend fun loadTopTransactions(startDate: LocalDate, endDate: LocalDate) {
        try {
            val startDateStr = startDate.toString()
            val endDateStr = endDate.toString()
            
            Log.d("ReportsViewModel", "Loading top transactions for range: $startDateStr to $endDateStr")
            
            // Use first() to get single emission and complete
            val transactions = transactionRepository.getByDateRange(startDateStr, endDateStr).first()
            
            val categories = categoryRepository.getAllActiveSync()
            
            // Get top expenses (not including income)
            val topTransactions = transactions
                .filter { !it.isIncome } // Only expenses for "top spending" list
                .sortedByDescending { it.amountCents }
                .take(10)
                .map { transaction ->
                    val category = categories.find { it.id == transaction.categoryId }
                    TopTransaction(
                        id = transaction.id,
                        description = transaction.description,
                        amountCents = transaction.amountCents,
                        timestamp = transaction.ts,
                        categoryName = category?.name ?: "Sin categoría",
                        isIncome = transaction.isIncome
                    )
                }
            
            Log.d("ReportsViewModel", "Top transactions loaded: ${topTransactions.size} transactions")
            _state.update { it.copy(topTransactions = topTransactions) }
        } catch (e: Exception) {
            Log.e("ReportsViewModel", "Error loading top transactions", e)
        }
    }
    
    private suspend fun generateInsights() {
        val state = _state.value
        val insights = mutableListOf<String>()
        val currentMonth = YearMonth.now()
        
        try {
            // ===== EXISTING CATEGORY INSIGHTS (Enhanced) =====
            val topCategory = state.categoryBreakdown.firstOrNull()
            if (topCategory != null && topCategory.percentage > 40) {
                insights.add("🏆 El ${topCategory.percentage.roundToInt()}% de tus gastos están en ${topCategory.categoryName}")
            }
            
            // ===== SPENDING PATTERNS BY DAY OF WEEK =====
            val dayPatterns = analyticsRepository.getSpendingPatternsByDayOfWeek(currentMonth)
            val maxSpendingDay = dayPatterns.maxByOrNull { it.value }
            if (maxSpendingDay != null && maxSpendingDay.value > 0) {
                val totalWeekSpending = dayPatterns.values.sum()
                val dayPercentage = if (totalWeekSpending > 0) {
                    (maxSpendingDay.value.toFloat() / totalWeekSpending) * 100
                } else 0f
                
                if (dayPercentage > 20) {
                    insights.add("📅 Gastas más los ${maxSpendingDay.key.lowercase()} (${dayPercentage.roundToInt()}% de la semana)")
                }
            }
            
            // ===== MERCHANT FREQUENCY ANALYSIS =====
            val merchants = analyticsRepository.getMerchantFrequencyAnalysis(currentMonth)
            val topMerchant = merchants.firstOrNull()
            if (topMerchant != null && topMerchant.transactionCount >= 3) {
                insights.add("🏪 Tu comercio más frecuente es ${topMerchant.merchantName} (${topMerchant.transactionCount} veces)")
            }
            
            // ===== TIME OF DAY PATTERNS =====
            val timePatterns = analyticsRepository.getTimeOfDaySpendingAnalysis(currentMonth)
            val maxSpendingTime = timePatterns.maxByOrNull { it.value }
            if (maxSpendingTime != null && maxSpendingTime.value > 0) {
                val totalTimeSpending = timePatterns.values.sum()
                val timePercentage = if (totalTimeSpending > 0) {
                    (maxSpendingTime.value.toFloat() / totalTimeSpending) * 100
                } else 0f
                
                if (timePercentage > 30) {
                    insights.add("⏰ Gastas más en la ${maxSpendingTime.key.lowercase()}")
                }
            }
            
            // ===== BUDGET PERFORMANCE INSIGHTS =====
            try {
                val budgetComparison = analyticsRepository.getBudgetPerformanceComparison(currentMonth)
                if (budgetComparison.previousMonthTotalCents > 0) {
                    if (budgetComparison.isImprovement) {
                        insights.add("📈 ¡Excelente! Estás ${Math.abs(budgetComparison.changePercentage).roundToInt()}% mejor que el mes pasado")
                    } else if (budgetComparison.changePercentage > 15) {
                        insights.add("⚠️ Tus gastos aumentaron ${budgetComparison.changePercentage.roundToInt()}% comparado con el mes anterior")
                    }
                }
                
                // Budget utilization insights
                val overBudgetCategories = budgetComparison.budgetUtilizations.filter { it.utilizationPercentage > 100 }
                if (overBudgetCategories.isNotEmpty()) {
                    val categoryNames = overBudgetCategories.take(2).map { it.category.name }.joinToString(", ")
                    insights.add("💸 Has excedido el presupuesto en: $categoryNames")
                }
            } catch (e: Exception) {
                // Budget comparison might fail if no budgets are set
                Log.d("ReportsViewModel", "No budget data available for comparison")
            }
            
            // ===== SPENDING PREDICTION =====
            val prediction = analyticsRepository.getSpendingPrediction(currentMonth)
            if (prediction.confidence > 0.5f && prediction.daysRemaining > 0) {
                val projectionDiff = prediction.projectedTotalCents - prediction.currentSpentCents
                if (projectionDiff > 0 && prediction.confidence > 0.7f) {
                    val nf = NumberFormat.getCurrencyInstance(Locale("es", "CO"))
                    insights.add("🔮 A este ritmo, gastarás ${nf.format(prediction.projectedTotalCents / 100.0)} este mes")
                }
            }
            
            // ===== ENHANCED TRANSACTION INSIGHTS =====
            state.summary?.let { summary ->
                val avgPerTransaction = if (summary.transactionCount > 0) {
                    summary.totalSpentCents / summary.transactionCount
                } else 0L
                
                if (avgPerTransaction > 50000) { // 500 COP average
                    insights.add("💳 Tu gasto promedio por transacción es alto: $${avgPerTransaction / 100}")
                }
                
                if (summary.transactionCount > 100) {
                    insights.add("📊 Realizaste ${summary.transactionCount} transacciones en este período")
                } else if (summary.transactionCount > 50) {
                    insights.add("📊 Tuviste ${summary.transactionCount} transacciones - mantienes un buen control")
                }
            }
            
            // ===== CATEGORY TREND ANALYSIS =====
            val topCategoryId = topCategory?.categoryId
            if (topCategoryId != null) {
                try {
                    val trendAnalysis = analyticsRepository.getCategoryTrendAnalysis(topCategoryId, 6)
                    if (trendAnalysis.isIncreasing && Math.abs(trendAnalysis.trendPercentage) > 20) {
                        insights.add("📈 Tus gastos en ${topCategory.categoryName} han aumentado ${Math.abs(trendAnalysis.trendPercentage).roundToInt()}%")
                    } else if (trendAnalysis.isDecreasing && Math.abs(trendAnalysis.trendPercentage) > 20) {
                        insights.add("📉 ¡Bien! Has reducido gastos en ${topCategory.categoryName} un ${Math.abs(trendAnalysis.trendPercentage).roundToInt()}%")
                    }
                } catch (e: Exception) {
                    Log.d("ReportsViewModel", "Could not analyze trend for category $topCategoryId")
                }
            }
            
            // ===== ENHANCED MONTHLY TREND INSIGHTS =====
            val recentTrends = state.monthlyTrends.takeLast(3)
            if (recentTrends.size >= 2) {
                val isIncreasing = recentTrends.zipWithNext().all { (prev, current) ->
                    current.totalCents > prev.totalCents
                }
                val isDecreasing = recentTrends.zipWithNext().all { (prev, current) ->
                    current.totalCents < prev.totalCents
                }
                
                if (isIncreasing) {
                    insights.add("⬆️ Tus gastos han estado aumentando consistentemente")
                } else if (isDecreasing) {
                    insights.add("⬇️ ¡Excelente! Tus gastos han estado disminuyendo")
                }
            }
            
            // ===== CATEGORY DIVERSITY INSIGHTS =====
            if (state.categoryBreakdown.size > 8) {
                insights.add("🎯 Usas muchas categorías diferentes (${state.categoryBreakdown.size}) - considera consolidar algunas")
            } else if (state.categoryBreakdown.size <= 3) {
                insights.add("🎯 Tienes un patrón de gastos simple (${state.categoryBreakdown.size} categorías principales)")
            }
            
            // ===== FALLBACK MESSAGE IF NO INSIGHTS =====
            if (insights.isEmpty()) {
                insights.add("📊 Continúa registrando transacciones para obtener más insights personalizados")
            }
            
        } catch (e: Exception) {
            Log.e("ReportsViewModel", "Error generating enhanced insights", e)
            insights.add("📊 Análisis de gastos disponible próximamente")
        }
        
        _state.update { it.copy(insights = insights) }
    }
    
    private suspend fun calculatePercentageChange(period: ReportPeriod, currentTotal: Long): Float? {
        return try {
            val (prevStartDate, prevEndDate) = getPreviousDateRange(period)
            val prevStartDateStr = prevStartDate.toString()
            val prevEndDateStr = prevEndDate.toString()
            
            // Use first() to get single emission and complete
            val prevTransactions = transactionRepository.getByDateRange(prevStartDateStr, prevEndDateStr).first()
            val prevTotal = prevTransactions.filter { !it.isIncome }.sumOf { it.amountCents } // Only expenses
            
            if (prevTotal > 0) {
                ((currentTotal.toFloat() - prevTotal.toFloat()) / prevTotal.toFloat()) * 100f
            } else null
        } catch (e: Exception) {
            Log.e("ReportsViewModel", "Error calculating percentage change", e)
            null
        }
    }
    
    private fun getDateRange(period: ReportPeriod): Pair<LocalDate, LocalDate> {
        val now = LocalDate.now()
        return when (period) {
            ReportPeriod.CURRENT_MONTH -> {
                val startOfMonth = now.withDayOfMonth(1)
                startOfMonth to now
            }
            ReportPeriod.LAST_MONTH -> {
                val lastMonth = now.minusMonths(1)
                val startOfLastMonth = lastMonth.withDayOfMonth(1)
                val endOfLastMonth = lastMonth.withDayOfMonth(lastMonth.lengthOfMonth())
                startOfLastMonth to endOfLastMonth
            }
            ReportPeriod.LAST_3_MONTHS -> {
                val threeMonthsAgo = now.minusMonths(3).withDayOfMonth(1)
                threeMonthsAgo to now
            }
            ReportPeriod.LAST_6_MONTHS -> {
                val sixMonthsAgo = now.minusMonths(6).withDayOfMonth(1)
                sixMonthsAgo to now
            }
            ReportPeriod.CURRENT_YEAR -> {
                val startOfYear = now.withDayOfYear(1)
                startOfYear to now
            }
        }
    }
    
    private fun getPreviousDateRange(period: ReportPeriod): Pair<LocalDate, LocalDate> {
        val now = LocalDate.now()
        return when (period) {
            ReportPeriod.CURRENT_MONTH -> {
                val lastMonth = now.minusMonths(1)
                val startOfLastMonth = lastMonth.withDayOfMonth(1)
                val endOfLastMonth = lastMonth.withDayOfMonth(lastMonth.lengthOfMonth())
                startOfLastMonth to endOfLastMonth
            }
            ReportPeriod.LAST_MONTH -> {
                val twoMonthsAgo = now.minusMonths(2)
                val startOfTwoMonthsAgo = twoMonthsAgo.withDayOfMonth(1)
                val endOfTwoMonthsAgo = twoMonthsAgo.withDayOfMonth(twoMonthsAgo.lengthOfMonth())
                startOfTwoMonthsAgo to endOfTwoMonthsAgo
            }
            ReportPeriod.LAST_3_MONTHS -> {
                val sixMonthsAgo = now.minusMonths(6).withDayOfMonth(1)
                val threeMonthsAgo = now.minusMonths(3)
                sixMonthsAgo to threeMonthsAgo
            }
            ReportPeriod.LAST_6_MONTHS -> {
                val twelveMonthsAgo = now.minusMonths(12).withDayOfMonth(1)
                val sixMonthsAgo = now.minusMonths(6)
                twelveMonthsAgo to sixMonthsAgo
            }
            ReportPeriod.CURRENT_YEAR -> {
                val lastYear = now.minusYears(1)
                val startOfLastYear = lastYear.withDayOfYear(1)
                val endOfLastYear = lastYear.withDayOfYear(lastYear.lengthOfYear())
                startOfLastYear to endOfLastYear
            }
        }
    }
    
    fun clearError() {
        _state.update { it.copy(error = null) }
    }

    fun refreshData() {
        loadReports()
    }

    /**
     * Serializes the currently loaded report into a CSV string for sharing/export. Operates purely on
     * the in-memory state (no I/O), so it can be called straight from the UI thread when the user taps
     * "Exportar". Amounts are emitted in whole pesos via [centsToCopString].
     */
    fun buildCsvExport(): String {
        val s = _state.value
        val sb = StringBuilder()

        sb.appendLine("Reporte;${periodLabel(s.selectedPeriod)}")
        sb.appendLine()

        s.summary?.let { summary ->
            sb.appendLine("Resumen")
            sb.appendLine("Concepto;Valor")
            sb.appendLine("Total gastado;${summary.totalSpentCents.centsToCopString()}")
            sb.appendLine("Total ingresos;${summary.totalIncomeCents.centsToCopString()}")
            sb.appendLine("Balance neto;${summary.netBalanceCents.centsToCopString()}")
            sb.appendLine("Promedio diario;${summary.dailyAverageCents.centsToCopString()}")
            sb.appendLine("Transacciones;${summary.transactionCount}")
            sb.appendLine("Categorias usadas;${summary.categoriesUsed}")
            sb.appendLine()
        }

        if (s.categoryBreakdown.isNotEmpty()) {
            sb.appendLine("Gastos por categoria")
            sb.appendLine("Categoria;Monto;Transacciones;Porcentaje")
            s.categoryBreakdown.forEach { c ->
                sb.appendLine(
                    "${csv(c.categoryName)};${c.amountCents.centsToCopString()};" +
                        "${c.transactionCount};${c.percentage.roundToInt()}%"
                )
            }
            sb.appendLine()
        }

        if (s.topTransactions.isNotEmpty()) {
            sb.appendLine("Mayores gastos")
            sb.appendLine("Descripcion;Categoria;Monto")
            s.topTransactions.forEach { t ->
                sb.appendLine(
                    "${csv(t.description)};${csv(t.categoryName)};${t.amountCents.centsToCopString()}"
                )
            }
        }

        return sb.toString()
    }

    private fun periodLabel(period: ReportPeriod): String = when (period) {
        ReportPeriod.CURRENT_MONTH -> "Mes actual"
        ReportPeriod.LAST_MONTH -> "Mes pasado"
        ReportPeriod.LAST_3_MONTHS -> "Ultimos 3 meses"
        ReportPeriod.LAST_6_MONTHS -> "Ultimos 6 meses"
        ReportPeriod.CURRENT_YEAR -> "Ano actual"
    }

    // Strip the field separator and newlines so a stray value can't break the CSV layout.
    private fun csv(value: String): String = value.replace(';', ',').replace('\n', ' ').trim()
}

enum class ReportPeriod {
    CURRENT_MONTH,
    LAST_MONTH,
    LAST_3_MONTHS,
    LAST_6_MONTHS,
    CURRENT_YEAR
}

data class ReportsState(
    val selectedPeriod: ReportPeriod = ReportPeriod.CURRENT_MONTH,
    val summary: ReportsSummary? = null,
    val categoryBreakdown: List<CategoryBreakdown> = emptyList(),
    val monthlyTrends: List<MonthlyTrend> = emptyList(),
    val topTransactions: List<TopTransaction> = emptyList(),
    val insights: List<String> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

data class ReportsSummary(
    val totalSpentCents: Long,
    val totalIncomeCents: Long = 0L,
    val netBalanceCents: Long = 0L,
    val dailyAverageCents: Long,
    val transactionCount: Int,
    val expenseCount: Int = 0,
    val incomeCount: Int = 0,
    val categoriesUsed: Int,
    val percentageChange: Float? = null
)

data class CategoryBreakdown(
    val categoryId: Long,
    val categoryName: String,
    val categoryIcon: String,
    val amountCents: Long,
    val transactionCount: Int,
    val percentage: Float
)

data class MonthlyTrend(
    val yearMonth: YearMonth,
    val monthName: String,
    val totalCents: Long,
    val changePercentage: Float?
)

data class TopTransaction(
    val id: String,
    val description: String,
    val amountCents: Long,
    val timestamp: Long,
    val categoryName: String,
    val isIncome: Boolean = false
)