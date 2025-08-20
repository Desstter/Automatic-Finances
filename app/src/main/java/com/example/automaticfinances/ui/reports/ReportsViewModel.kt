package com.example.automaticfinances.ui.reports

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.automaticfinances.data.repo.TransactionRepository
import com.example.automaticfinances.data.repo.CategoryRepository
import com.example.automaticfinances.data.repo.AnalyticsRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.*
import kotlin.math.roundToInt

class ReportsViewModel(
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
                
                _state.update { it.copy(isLoading = false) }
            } catch (e: Exception) {
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
            
            // Collect transactions as Flow
            transactionRepository.getByDateRange(startDateStr, endDateStr)
                .collect { transactions ->
                    val totalSpent = transactions.sumOf { it.amountCents }
                    val transactionCount = transactions.size
                    val categoriesUsed = transactions.mapNotNull { it.categoryId }.distinct().size
                    
                    val daysDiff = java.time.temporal.ChronoUnit.DAYS.between(startDate, endDate).toInt() + 1
                    val dailyAverage = if (daysDiff > 0) totalSpent / daysDiff else 0L
                    
                    // Calculate percentage change
                    val percentageChange = calculatePercentageChange(period, totalSpent)
                    
                    val summary = ReportsSummary(
                        totalSpentCents = totalSpent,
                        dailyAverageCents = dailyAverage,
                        transactionCount = transactionCount,
                        categoriesUsed = categoriesUsed,
                        percentageChange = percentageChange
                    )
                    
                    _state.update { it.copy(summary = summary) }
                }
        } catch (e: Exception) {
            // Handle error, but don't fail the entire load
        }
    }
    
    private suspend fun loadCategoryBreakdown(startDate: LocalDate, endDate: LocalDate) {
        try {
            val startDateStr = startDate.toString()
            val endDateStr = endDate.toString()
            
            transactionRepository.getByDateRange(startDateStr, endDateStr)
                .collect { transactions ->
                    val categories = categoryRepository.getAllActiveSync()
                    val totalSpent = transactions.sumOf { it.amountCents }
                    
                    val breakdown = transactions
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
                    
                    _state.update { it.copy(categoryBreakdown = breakdown) }
                }
        } catch (e: Exception) {
            // Handle error
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
            
            transactionRepository.getByDateRange(startDateStr, endDateStr)
                .collect { transactions ->
                    val categories = categoryRepository.getAllActiveSync()
                    
                    val topTransactions = transactions
                        .sortedByDescending { it.amountCents }
                        .take(10)
                        .map { transaction ->
                            val category = categories.find { it.id == transaction.categoryId }
                            TopTransaction(
                                id = transaction.id,
                                description = transaction.description,
                                amountCents = transaction.amountCents,
                                timestamp = transaction.ts,
                                categoryName = category?.name ?: "Sin categoría"
                            )
                        }
                    
                    _state.update { it.copy(topTransactions = topTransactions) }
                }
        } catch (e: Exception) {
            // Handle error
        }
    }
    
    private fun generateInsights() {
        val state = _state.value
        val insights = mutableListOf<String>()
        
        // Category insights
        val topCategory = state.categoryBreakdown.firstOrNull()
        if (topCategory != null && topCategory.percentage > 40) {
            insights.add("El ${topCategory.percentage.roundToInt()}% de tus gastos están en ${topCategory.categoryName}")
        }
        
        // Transaction count insights
        state.summary?.let { summary ->
            val avgPerTransaction = if (summary.transactionCount > 0) {
                summary.totalSpentCents / summary.transactionCount
            } else 0L
            
            if (avgPerTransaction > 50000) { // 500 COP average
                insights.add("Tu gasto promedio por transacción es alto: $${avgPerTransaction / 100}")
            }
            
            if (summary.transactionCount > 100) {
                insights.add("Realizaste ${summary.transactionCount} transacciones en este período")
            }
        }
        
        // Trend insights
        val recentTrends = state.monthlyTrends.takeLast(3)
        if (recentTrends.size >= 2) {
            val isIncreasing = recentTrends.zipWithNext().all { (prev, current) ->
                current.totalCents > prev.totalCents
            }
            val isDecreasing = recentTrends.zipWithNext().all { (prev, current) ->
                current.totalCents < prev.totalCents
            }
            
            if (isIncreasing) {
                insights.add("Tus gastos han estado aumentando consistentemente")
            } else if (isDecreasing) {
                insights.add("¡Bien! Tus gastos han estado disminuyendo")
            }
        }
        
        // Add general insights
        if (state.categoryBreakdown.size > 8) {
            insights.add("Usas muchas categorías diferentes (${state.categoryBreakdown.size})")
        }
        
        _state.update { it.copy(insights = insights) }
    }
    
    private suspend fun calculatePercentageChange(period: ReportPeriod, currentTotal: Long): Float? {
        return try {
            val (prevStartDate, prevEndDate) = getPreviousDateRange(period)
            val prevStartDateStr = prevStartDate.toString()
            val prevEndDateStr = prevEndDate.toString()
            
            var prevTotal = 0L
            transactionRepository.getByDateRange(prevStartDateStr, prevEndDateStr)
                .collect { transactions ->
                    prevTotal = transactions.sumOf { it.amountCents }
                }
            
            if (prevTotal > 0) {
                ((currentTotal.toFloat() - prevTotal.toFloat()) / prevTotal.toFloat()) * 100f
            } else null
        } catch (e: Exception) {
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
    val dailyAverageCents: Long,
    val transactionCount: Int,
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
    val categoryName: String
)