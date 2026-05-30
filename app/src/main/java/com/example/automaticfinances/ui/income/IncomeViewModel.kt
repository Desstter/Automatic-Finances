package com.example.automaticfinances.ui.income

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.automaticfinances.data.repo.TransactionRepository
import com.example.automaticfinances.data.repo.TransactionWithCategory
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject

data class IncomeState(
    val incomes: List<TransactionWithCategory> = emptyList(),
    val totalIncome: Long = 0L,
    val monthlyIncome: Long = 0L,
    val incomeCount: Int = 0,
    val isLoading: Boolean = true,
    val error: String? = null
)

@HiltViewModel
class IncomeViewModel @Inject constructor(
    private val transactionRepository: TransactionRepository
) : ViewModel() {
    
    private val _state = MutableStateFlow(IncomeState())
    val state: StateFlow<IncomeState> = _state.asStateFlow()
    
    init {
        loadIncomes()
    }
    
    private fun loadIncomes() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)
            
            try {
                // Combine income data streams
                combine(
                    transactionRepository.getIncomesWithCategories(),
                    flowOf(getCurrentMonth()),
                    flowOf(getCurrentYear())
                ) { incomes, month, year ->
                    Triple(incomes, month, year)
                }.collectLatest { (incomes, month, year) ->
                    // Calculate totals
                    val totalIncome = incomes.sumOf { it.amountCents }
                    val monthlyIncome = transactionRepository.getMonthlyIncomeTotal(year, month)
                    val incomeCount = incomes.size
                    
                    _state.value = IncomeState(
                        incomes = incomes,
                        totalIncome = totalIncome,
                        monthlyIncome = monthlyIncome,
                        incomeCount = incomeCount,
                        isLoading = false
                    )
                }
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    isLoading = false,
                    error = "Error al cargar ingresos: ${e.message}"
                )
            }
        }
    }
    
    fun refreshIncomes() {
        loadIncomes()
    }
    
    private fun getCurrentMonth(): Int {
        return LocalDate.now().monthValue
    }
    
    private fun getCurrentYear(): Int {
        return LocalDate.now().year
    }
}