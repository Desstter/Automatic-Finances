package com.example.automaticfinances.ui.insights

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.automaticfinances.data.db.*
import com.example.automaticfinances.data.repo.BudgetRepository
import com.example.automaticfinances.data.repo.TransactionRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.YearMonth

class FinancialDashboardViewModel(
    private val budgetRepository: BudgetRepository,
    private val transactionRepository: TransactionRepository
) : ViewModel() {
    
    private val _state = MutableStateFlow(FinancialDashboardState())
    val state: StateFlow<FinancialDashboardState> = _state.asStateFlow()
    
    init {
        loadDashboardData()
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
    }
    
    fun refreshData() {
        loadDashboardData()
    }
}

data class FinancialDashboardState(
    val selectedMonth: YearMonth = YearMonth.now(),
    val budgetStatuses: List<BudgetStatus> = emptyList(),
    val budgetSummary: BudgetSummary? = null,
    val monthlySpentCents: Long = 0L,
    val previousMonthSpentCents: Long? = null,
    val isLoading: Boolean = false,
    val error: String? = null
)