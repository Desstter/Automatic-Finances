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
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.YearMonth

class FinancialDashboardViewModel(
    private val budgetRepository: BudgetRepository,
    private val transactionRepository: TransactionRepository,
    private val categoryRepository: CategoryRepository
) : ViewModel() {
    
    // Create analytics repository
    private val analyticsRepository = AnalyticsRepository(
        transactionRepository = transactionRepository,
        budgetRepository = budgetRepository,
        categoryRepository = categoryRepository
    )
    
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
            
            // Load monthly spending
            try {
                val monthlySpent = transactionRepository.getMonthlySpentTotal(
                    currentMonth.year, 
                    currentMonth.monthValue
                )
                
                val previousMonth = currentMonth.minusMonths(1)
                val previousMonthSpent = transactionRepository.getMonthlySpentTotal(
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
        // Handle category click - could navigate to category details
        // Implementation depends on navigation requirements
    }
    
    fun onBudgetClicked(budgetId: Long) {
        // Handle budget click - could navigate to budget management
        // Implementation depends on navigation requirements
    }
    
    fun toggleChartsExpanded() {
        _state.update { 
            it.copy(isChartsExpanded = !it.isChartsExpanded) 
        }
    }
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
    val isChartsExpanded: Boolean = false
)