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
        
        // Calculate spending per category (expenses only)
        for (category in categories) {
            val spent = transactionRepository.getExpenseByCategoryInMonth(category.id, year, month)
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
            val spent = transactionRepository.getMonthlyExpenseTotal(
                targetMonth.year, 
                targetMonth.monthValue
            )
            val transactionCount = getExpenseCountForMonth(targetMonth.year, targetMonth.monthValue)
            
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
        return transactionDao.getUncategorizedExpenseTotalForDateRange(startDate, endDate) ?: 0L
    }
    
    private suspend fun getTransactionCountForMonth(year: Int, month: Int): Int {
        val startDate = String.format("%04d-%02d-01", year, month)
        val endDate = String.format("%04d-%02d-31", year, month)
        return transactionDao.getTransactionCountForDateRange(startDate, endDate)
    }
    
    private suspend fun getExpenseCountForMonth(year: Int, month: Int): Int {
        val startDate = String.format("%04d-%02d-01", year, month)
        val endDate = String.format("%04d-%02d-31", year, month)
        return transactionRepository.getExpenseCountForDateRange(startDate, endDate)
    }
    
    // ======= INCOME ANALYTICS METHODS =======
    
    suspend fun getIncomeSpendingForMonth(yearMonth: YearMonth): List<CategorySpending> {
        val year = yearMonth.year
        val month = yearMonth.monthValue
        
        // Get all active categories
        val categories = categoryRepository.getAllActiveSync()
        val categoryIncomeList = mutableListOf<CategorySpending>()
        var totalIncome = 0L
        
        // Calculate income per category using the proper income filtering
        for (category in categories) {
            val income = transactionRepository.getIncomeByCategoryInMonth(category.id, year, month)
            if (income > 0) {
                totalIncome += income
                categoryIncomeList.add(
                    CategorySpending(
                        categoryId = category.id,
                        category = category,
                        amountCents = income,
                        percentage = 0f, // Will be calculated below
                        transactionCount = getIncomeCountByCategoryInMonth(category.id, year, month)
                    )
                )
            }
        }
        
        // Calculate percentages and create updated list
        val finalList = if (totalIncome > 0) {
            categoryIncomeList.map { categorySpending ->
                categorySpending.copy(
                    percentage = (categorySpending.amountCents.toFloat() / totalIncome) * 100
                )
            }
        } else {
            categoryIncomeList
        }
        
        return finalList.sortedByDescending { it.amountCents }
    }
    
    suspend fun getIncomeVsExpenseComparison(yearMonth: YearMonth): IncomeVsExpenseComparison {
        val year = yearMonth.year
        val month = yearMonth.monthValue
        
        val totalIncome = transactionRepository.getMonthlyIncomeTotal(year, month)
        val totalExpenses = transactionRepository.getMonthlyExpenseTotal(year, month)
        val netBalance = totalIncome - totalExpenses
        
        return IncomeVsExpenseComparison(
            yearMonth = yearMonth,
            totalIncomeCents = totalIncome,
            totalExpensesCents = totalExpenses,
            netBalanceCents = netBalance,
            incomePercentage = if (totalIncome + totalExpenses > 0) {
                (totalIncome.toFloat() / (totalIncome + totalExpenses)) * 100
            } else 0f,
            expensePercentage = if (totalIncome + totalExpenses > 0) {
                (totalExpenses.toFloat() / (totalIncome + totalExpenses)) * 100
            } else 0f
        )
    }
    
    suspend fun getIncomeVsExpenseTrend(): List<MonthlySpending> {
        val monthlyData = mutableListOf<MonthlySpending>()
        val currentYearMonth = YearMonth.now()
        
        // Get data for last 6 months
        for (i in 5 downTo 0) {
            val yearMonth = currentYearMonth.minusMonths(i.toLong())
            val year = yearMonth.year
            val month = yearMonth.monthValue
            
            val income = transactionRepository.getMonthlyIncomeTotal(year, month)
            val expenses = transactionRepository.getMonthlyExpenseTotal(year, month)
            val incomeCount = transactionRepository.getIncomeCountForDateRange(
                String.format("%04d-%02d-01", year, month),
                String.format("%04d-%02d-31", year, month)
            )
            val expenseCount = transactionRepository.getExpenseCountForDateRange(
                String.format("%04d-%02d-01", year, month),
                String.format("%04d-%02d-31", year, month)
            )
            
            // Add income data point
            monthlyData.add(
                MonthlySpending(
                    yearMonth = yearMonth,
                    totalCents = income,
                    transactionCount = incomeCount,
                    averageDailySpending = if (yearMonth.lengthOfMonth() > 0) income / yearMonth.lengthOfMonth() else 0L,
                    isIncome = true
                )
            )
            
            // Add expense data point
            monthlyData.add(
                MonthlySpending(
                    yearMonth = yearMonth,
                    totalCents = expenses,
                    transactionCount = expenseCount,
                    averageDailySpending = if (yearMonth.lengthOfMonth() > 0) expenses / yearMonth.lengthOfMonth() else 0L,
                    isIncome = false
                )
            )
        }
        
        return monthlyData
    }
    
    private suspend fun getIncomeCountByCategoryInMonth(categoryId: Long, year: Int, month: Int): Int {
        // Simplified implementation - could be enhanced with proper transaction counting
        val income = transactionRepository.getIncomeByCategoryInMonth(categoryId, year, month)
        return if (income > 0) 1 else 0
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
    
    // ======= ADVANCED ANALYTICS METHODS FOR ENHANCED INSIGHTS =======
    
    /**
     * Analyzes spending patterns by day of week
     * Returns map of day name to total spending amount
     */
    suspend fun getSpendingPatternsByDayOfWeek(yearMonth: YearMonth): Map<String, Long> {
        val year = yearMonth.year
        val month = yearMonth.monthValue
        val startDate = String.format("%04d-%02d-01", year, month)
        val endDate = String.format("%04d-%02d-31", year, month)
        
        // Get all transactions for the month
        val transactions = transactionRepository.getByDateRange(startDate, endDate).first()
        val daySpending = mutableMapOf(
            "Lunes" to 0L,
            "Martes" to 0L,
            "Miércoles" to 0L,
            "Jueves" to 0L,
            "Viernes" to 0L,
            "Sábado" to 0L,
            "Domingo" to 0L
        )
        
        transactions.forEach { transaction ->
            if (transaction.amountCents < 0) { // Only expenses
                val dayOfWeek = getDayOfWeekSpanish(transaction.date)
                daySpending[dayOfWeek] = daySpending.getOrDefault(dayOfWeek, 0L) + Math.abs(transaction.amountCents)
            }
        }
        
        return daySpending
    }
    
    /**
     * Analyzes merchant frequency and spending
     * Returns list of merchants with total spending and transaction count
     */
    suspend fun getMerchantFrequencyAnalysis(yearMonth: YearMonth): List<MerchantAnalysis> {
        val year = yearMonth.year
        val month = yearMonth.monthValue
        val startDate = String.format("%04d-%02d-01", year, month)
        val endDate = String.format("%04d-%02d-31", year, month)
        
        val transactions = transactionRepository.getByDateRange(startDate, endDate).first()
        val merchantMap = mutableMapOf<String, MerchantData>()
        
        transactions.forEach { transaction ->
            if (transaction.amountCents < 0) { // Only expenses
                val merchant = cleanMerchantName(transaction.description)
                val currentData = merchantMap[merchant] ?: MerchantData(0L, 0)
                merchantMap[merchant] = MerchantData(
                    currentData.totalSpent + Math.abs(transaction.amountCents),
                    currentData.transactionCount + 1
                )
            }
        }
        
        return merchantMap.map { (merchant, data) ->
            MerchantAnalysis(
                merchantName = merchant,
                totalSpentCents = data.totalSpent,
                transactionCount = data.transactionCount,
                averageSpentCents = if (data.transactionCount > 0) data.totalSpent / data.transactionCount else 0L
            )
        }.sortedByDescending { it.totalSpentCents }
    }
    
    /**
     * Analyzes spending patterns by time of day
     * Returns map of time period to spending amount
     */
    suspend fun getTimeOfDaySpendingAnalysis(yearMonth: YearMonth): Map<String, Long> {
        val year = yearMonth.year
        val month = yearMonth.monthValue
        val startDate = String.format("%04d-%02d-01", year, month)
        val endDate = String.format("%04d-%02d-31", year, month)
        
        val transactions = transactionRepository.getByDateRange(startDate, endDate).first()
        val timeSpending = mutableMapOf(
            "Madrugada (00-06)" to 0L,
            "Mañana (06-12)" to 0L,
            "Tarde (12-18)" to 0L,
            "Noche (18-24)" to 0L
        )
        
        transactions.forEach { transaction ->
            if (transaction.amountCents < 0) { // Only expenses
                val timePeriod = getTimePeriodSpanish(transaction.time)
                timeSpending[timePeriod] = timeSpending.getOrDefault(timePeriod, 0L) + Math.abs(transaction.amountCents)
            }
        }
        
        return timeSpending
    }
    
    /**
     * Compares current month budget performance with previous month
     */
    suspend fun getBudgetPerformanceComparison(currentMonth: YearMonth): BudgetPerformanceComparison {
        val previousMonth = currentMonth.minusMonths(1)
        
        val currentSpending = getCategorySpendingForMonth(currentMonth)
        val previousSpending = getCategorySpendingForMonth(previousMonth)
        
        val currentTotal = currentSpending.sumOf { it.amountCents }
        val previousTotal = previousSpending.sumOf { it.amountCents }
        
        val changePercentage = if (previousTotal > 0) {
            ((currentTotal - previousTotal).toFloat() / previousTotal) * 100
        } else 0f
        
        val budgets = budgetRepository.getAllActiveBudgets().first()
        val budgetUtilization = budgets.mapNotNull { budget ->
            val spending = currentSpending.find { it.categoryId == budget.categoryId }
            if (spending != null) {
                BudgetUtilization(
                    category = spending.category,
                    budgetAmountCents = budget.limitAmountCents,
                    spentAmountCents = spending.amountCents,
                    utilizationPercentage = (spending.amountCents.toFloat() / budget.limitAmountCents) * 100
                )
            } else null
        }
        
        return BudgetPerformanceComparison(
            currentMonthTotalCents = currentTotal,
            previousMonthTotalCents = previousTotal,
            changePercentage = changePercentage,
            budgetUtilizations = budgetUtilization,
            isImprovement = changePercentage < 0
        )
    }
    
    /**
     * Analyzes category spending trends over multiple months
     */
    suspend fun getCategoryTrendAnalysis(categoryId: Long, monthsBack: Int = 6): CategoryTrendAnalysis {
        val currentMonth = YearMonth.now()
        val monthlyData = mutableListOf<MonthlySpending>()
        
        for (i in 0 until monthsBack) {
            val month = currentMonth.minusMonths(i.toLong())
            val spending = transactionRepository.getExpenseByCategoryInMonth(categoryId, month.year, month.monthValue)
            // Get transaction count by filtering the spent amount > 0 
            val count = if (spending > 0) 1 else 0 // Simplified - could be enhanced later
            
            monthlyData.add(
                MonthlySpending(
                    yearMonth = month,
                    totalCents = spending,
                    transactionCount = count,
                    averageDailySpending = if (month.lengthOfMonth() > 0) spending / month.lengthOfMonth() else 0L
                )
            )
        }
        
        // Calculate trend
        val recentThreeMonths = monthlyData.take(3)
        val olderThreeMonths = monthlyData.drop(3)
        
        val recentAverage = if (recentThreeMonths.isNotEmpty()) {
            recentThreeMonths.sumOf { it.totalCents } / recentThreeMonths.size
        } else 0L
        
        val olderAverage = if (olderThreeMonths.isNotEmpty()) {
            olderThreeMonths.sumOf { it.totalCents } / olderThreeMonths.size
        } else 0L
        
        val trendPercentage = if (olderAverage > 0) {
            ((recentAverage - olderAverage).toFloat() / olderAverage) * 100
        } else 0f
        
        return CategoryTrendAnalysis(
            categoryId = categoryId,
            monthlyData = monthlyData.reversed(),
            trendPercentage = trendPercentage,
            isIncreasing = trendPercentage > 5f,
            isDecreasing = trendPercentage < -5f
        )
    }
    
    /**
     * Predicts end-of-month spending based on current daily rate
     */
    suspend fun getSpendingPrediction(currentMonth: YearMonth): SpendingPrediction {
        val year = currentMonth.year
        val month = currentMonth.monthValue
        val today = java.time.LocalDate.now()
        val daysElapsed = if (currentMonth == YearMonth.from(today)) today.dayOfMonth else currentMonth.lengthOfMonth()
        
        val monthToDate = transactionRepository.getMonthlyExpenseTotal(year, month)
        val dailyAverage = if (daysElapsed > 0) monthToDate / daysElapsed else 0L
        val daysRemaining = currentMonth.lengthOfMonth() - daysElapsed
        val projectedTotal = monthToDate + (dailyAverage * daysRemaining)
        
        val previousMonth = currentMonth.minusMonths(1)
        val previousMonthTotal = transactionRepository.getMonthlyExpenseTotal(previousMonth.year, previousMonth.monthValue)
        
        val changeFromPrevious = if (previousMonthTotal > 0) {
            ((projectedTotal - previousMonthTotal).toFloat() / previousMonthTotal) * 100
        } else 0f
        
        return SpendingPrediction(
            currentSpentCents = monthToDate,
            projectedTotalCents = projectedTotal,
            dailyAverageCents = dailyAverage,
            daysRemaining = daysRemaining,
            changeFromPreviousMonth = changeFromPrevious,
            confidence = if (daysElapsed >= 10) 0.8f else (daysElapsed.toFloat() / 10f) * 0.8f
        )
    }
    
    // ======= HELPER METHODS =======
    
    private fun getDayOfWeekSpanish(dateString: String): String {
        // Parse date and return Spanish day name
        // This is a simplified version - in production you'd use proper date parsing
        val dayNumber = try {
            java.time.LocalDate.parse(dateString).dayOfWeek.value
        } catch (e: Exception) {
            1
        }
        
        return when (dayNumber) {
            1 -> "Lunes"
            2 -> "Martes" 
            3 -> "Miércoles"
            4 -> "Jueves"
            5 -> "Viernes"
            6 -> "Sábado"
            7 -> "Domingo"
            else -> "Lunes"
        }
    }
    
    private fun getTimePeriodSpanish(timeString: String): String {
        val hour = try {
            val time = timeString.split(":")[0].toInt()
            time
        } catch (e: Exception) {
            12
        }
        
        return when (hour) {
            in 0..5 -> "Madrugada (00-06)"
            in 6..11 -> "Mañana (06-12)"
            in 12..17 -> "Tarde (12-18)"
            else -> "Noche (18-24)"
        }
    }
    
    private fun cleanMerchantName(description: String): String {
        // Extract merchant name from transaction description
        // Remove common prefixes/suffixes and normalize
        return description
            .replace(Regex("^(Bancolombia: )?Compraste.*en\\s+"), "")
            .replace(Regex("\\s+con tu T\\.Cred.*$"), "")
            .replace(Regex("\\s*\\*\\d+.*$"), "")
            .trim()
            .take(30) // Limit length
            .ifEmpty { "Comercio desconocido" }
    }
    
    // ======= DATA CLASSES FOR NEW ANALYTICS =======
    
    data class MerchantData(val totalSpent: Long, val transactionCount: Int)
    
    data class MerchantAnalysis(
        val merchantName: String,
        val totalSpentCents: Long,
        val transactionCount: Int,
        val averageSpentCents: Long
    )
    
    data class BudgetPerformanceComparison(
        val currentMonthTotalCents: Long,
        val previousMonthTotalCents: Long,
        val changePercentage: Float,
        val budgetUtilizations: List<BudgetUtilization>,
        val isImprovement: Boolean
    )
    
    data class BudgetUtilization(
        val category: Category,
        val budgetAmountCents: Long,
        val spentAmountCents: Long,
        val utilizationPercentage: Float
    )
    
    data class CategoryTrendAnalysis(
        val categoryId: Long,
        val monthlyData: List<MonthlySpending>,
        val trendPercentage: Float,
        val isIncreasing: Boolean,
        val isDecreasing: Boolean
    )
    
    data class SpendingPrediction(
        val currentSpentCents: Long,
        val projectedTotalCents: Long,
        val dailyAverageCents: Long,
        val daysRemaining: Int,
        val changeFromPreviousMonth: Float,
        val confidence: Float
    )
}

data class SpendingSummary(
    val totalSpentCents: Long,
    val topCategory: Category?,
    val topCategoryAmountCents: Long,
    val categoriesUsed: Int,
    val averagePerCategory: Long
)