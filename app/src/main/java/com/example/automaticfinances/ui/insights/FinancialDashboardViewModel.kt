package com.example.automaticfinances.ui.insights

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.automaticfinances.data.db.*
import com.example.automaticfinances.data.repo.BudgetRepository
import com.example.automaticfinances.data.repo.TransactionRepository
import com.example.automaticfinances.data.repo.AnalyticsRepository
import com.example.automaticfinances.data.repo.CategoryRepository
import com.example.automaticfinances.data.models.ChartData
import com.example.automaticfinances.data.models.ChartType
import com.example.automaticfinances.data.models.IncomeVsExpenseComparison
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.YearMonth
import javax.inject.Inject

@HiltViewModel
class FinancialDashboardViewModel @Inject constructor(
    private val budgetRepository: BudgetRepository,
    private val transactionRepository: TransactionRepository,
    private val categoryRepository: CategoryRepository,
    private val analyticsRepository: AnalyticsRepository
) : ViewModel() {
    
    private val _state = MutableStateFlow(FinancialDashboardState())
    val state: StateFlow<FinancialDashboardState> = _state.asStateFlow()
    
    init {
        loadDashboardData()
        loadChartData()
    }
    
    fun loadDashboardData() {
        viewModelScope.launch {
            val currentMonth = _state.value.selectedMonth
            
            // Load budget statuses
            budgetRepository.getBudgetStatusForMonth(currentMonth)
                .collect { budgetStatuses ->
                    _state.update { it.copy(budgetStatuses = budgetStatuses) }
                }
        }
        
        viewModelScope.launch {
            val currentMonth = _state.value.selectedMonth
            
            // Load budget summary
            try {
                val summary = budgetRepository.getBudgetSummaryForMonth(currentMonth)
                _state.update { it.copy(budgetSummary = summary) }
            } catch (e: Exception) {
                // Handle error
                _state.update { it.copy(budgetSummary = null) }
            }
        }
        
        viewModelScope.launch {
            val currentMonth = _state.value.selectedMonth
            
            // Load monthly spending (expenses only)
            try {
                val monthlySpent = transactionRepository.getMonthlyExpenseTotal(
                    currentMonth.year, 
                    currentMonth.monthValue
                )
                
                val previousMonth = currentMonth.minusMonths(1)
                val previousMonthSpent = transactionRepository.getMonthlyExpenseTotal(
                    previousMonth.year, 
                    previousMonth.monthValue
                )
                
                _state.update { 
                    it.copy(
                        monthlySpentCents = monthlySpent,
                        previousMonthSpentCents = previousMonthSpent
                    ) 
                }
            } catch (e: Exception) {
                // Handle error
                _state.update { 
                    it.copy(
                        monthlySpentCents = 0L,
                        previousMonthSpentCents = null
                    ) 
                }
            }
        }
        
        // Load income vs expense data
        viewModelScope.launch {
            val currentMonth = _state.value.selectedMonth
            
            try {
                val incomeVsExpenseComparison = analyticsRepository.getIncomeVsExpenseComparison(currentMonth)
                val monthlyIncome = transactionRepository.getMonthlyIncomeTotal(
                    currentMonth.year,
                    currentMonth.monthValue
                )
                
                _state.update { 
                    it.copy(
                        incomeVsExpenseComparison = incomeVsExpenseComparison,
                        monthlyIncomeCents = monthlyIncome
                    ) 
                }
            } catch (e: Exception) {
                // Handle error
                _state.update { 
                    it.copy(
                        incomeVsExpenseComparison = null,
                        monthlyIncomeCents = 0L
                    ) 
                }
            }
        }
    }
    
    fun selectMonth(yearMonth: YearMonth) {
        _state.update { it.copy(selectedMonth = yearMonth) }
        loadDashboardData()
        loadChartData()
    }
    
    fun refreshData() {
        loadDashboardData()
        loadChartData()
    }
    
    // Chart-related methods
    fun loadChartData() {
        viewModelScope.launch {
            val currentMonth = _state.value.selectedMonth
            
            analyticsRepository.getChartDataForMonth(currentMonth)
                .collect { chartData ->
                    _state.update { it.copy(chartData = chartData) }
                }
        }
    }
    
    fun selectChartType(chartType: ChartType) {
        _state.update { 
            it.copy(selectedChartType = chartType) 
        }
    }
    
    fun onCategoryClicked(categoryId: Long) {
        _state.update { 
            it.copy(selectedCategoryId = categoryId) 
        }
        // Additional logic can be added here for category-specific actions
    }
    
    fun onBudgetClicked(budgetId: Long) {
        _state.update { 
            it.copy(selectedBudgetId = budgetId) 
        }
        // Additional logic can be added here for budget-specific actions
    }
    
    fun toggleChartsExpanded() {
        _state.update { 
            it.copy(isChartsExpanded = !it.isChartsExpanded) 
        }
    }
    
    fun setAnalysisMode(mode: AnalysisMode) {
        _state.update { 
            it.copy(analysisMode = mode) 
        }
        if (mode == AnalysisMode.INCOME) {
            loadIncomeChartData()
        }
    }
    
    private fun loadIncomeChartData() {
        viewModelScope.launch {
            val currentMonth = _state.value.selectedMonth
            
            try {
                val categoryIncome = analyticsRepository.getIncomeSpendingForMonth(currentMonth)
                val incomeVsExpenseTrend = analyticsRepository.getIncomeVsExpenseTrend()
                val incomeVsExpenseComparison = analyticsRepository.getIncomeVsExpenseComparison(currentMonth)
                val totalIncome = categoryIncome.sumOf { it.amountCents }
                
                val incomeChartData = com.example.automaticfinances.data.models.IncomeChartData(
                    categoryIncome = categoryIncome,
                    incomeVsExpenseTrend = incomeVsExpenseTrend,
                    incomeVsExpenseComparison = incomeVsExpenseComparison,
                    selectedMonth = currentMonth,
                    totalIncomeCents = totalIncome,
                    isLoading = false
                )
                
                _state.update { 
                    it.copy(incomeChartData = incomeChartData) 
                }
            } catch (e: Exception) {
                _state.update { 
                    it.copy(
                        incomeChartData = com.example.automaticfinances.data.models.IncomeChartData(
                            selectedMonth = currentMonth,
                            isLoading = false,
                            error = "Error al cargar datos de ingresos: ${e.message}"
                        )
                    ) 
                }
            }
        }
    }
}

enum class AnalysisMode {
    EXPENSES,
    INCOME,
    COMPARISON
}

data class FinancialDashboardState(
    val selectedMonth: YearMonth = YearMonth.now(),
    val budgetStatuses: List<BudgetStatus> = emptyList(),
    val budgetSummary: BudgetSummary? = null,
    val monthlySpentCents: Long = 0L,
    val previousMonthSpentCents: Long? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
    // Chart-related state
    val chartData: ChartData = ChartData(),
    val selectedChartType: ChartType = ChartType.PIE_CATEGORY_SPENDING,
    val isChartsExpanded: Boolean = false,
    // Selection state
    val selectedCategoryId: Long? = null,
    val selectedBudgetId: Long? = null,
    // Income data
    val incomeVsExpenseComparison: IncomeVsExpenseComparison? = null,
    val monthlyIncomeCents: Long = 0L,
    // Analysis mode
    val analysisMode: AnalysisMode = AnalysisMode.EXPENSES,
    val incomeChartData: com.example.automaticfinances.data.models.IncomeChartData = com.example.automaticfinances.data.models.IncomeChartData()
)