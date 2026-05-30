package com.example.automaticfinances.ui.budget

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.automaticfinances.data.db.Budget
import com.example.automaticfinances.data.db.Category
import com.example.automaticfinances.data.repo.BudgetRepository
import com.example.automaticfinances.data.repo.CategoryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.YearMonth
import javax.inject.Inject

@HiltViewModel
class BudgetManagementViewModel @Inject constructor(
    private val budgetRepository: BudgetRepository,
    private val categoryRepository: CategoryRepository
) : ViewModel() {
    
    private val _state = MutableStateFlow(BudgetManagementState())
    val state: StateFlow<BudgetManagementState> = _state.asStateFlow()
    
    init {
        loadCategories()
        loadBudgets()
    }
    
    fun loadCategories() {
        viewModelScope.launch {
            try {
                categoryRepository.getAllActive()
                    .collect { categories ->
                        _state.update { it.copy(categories = categories) }
                    }
            } catch (e: Exception) {
                _state.update { 
                    it.copy(
                        error = "Error cargando categorías: ${e.message}",
                        isLoading = false
                    ) 
                }
            }
        }
    }
    
    fun loadBudgets() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            
            try {
                budgetRepository.getBudgetsForMonth(_state.value.selectedMonth)
                    .collect { budgets ->
                        _state.update { 
                            it.copy(
                                budgets = budgets,
                                isLoading = false
                            ) 
                        }
                    }
            } catch (e: Exception) {
                _state.update { 
                    it.copy(
                        error = "Error cargando presupuestos: ${e.message}",
                        isLoading = false
                    ) 
                }
            }
        }
    }
    
    fun selectMonth(yearMonth: YearMonth) {
        _state.update { it.copy(selectedMonth = yearMonth) }
        loadBudgets()
    }
    
    fun createBudget(categoryId: Long, amountCents: Long, yearMonth: YearMonth) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            
            try {
                budgetRepository.createBudget(categoryId, amountCents, yearMonth)
                // loadBudgets() will be called automatically through the Flow
                _state.update { it.copy(isLoading = false) }
            } catch (e: Exception) {
                val errorMessage = when {
                    e.message?.contains("Ya existe un presupuesto") == true -> 
                        "Ya existe un presupuesto para esta categoría en este mes"
                    else -> "Error creando presupuesto: ${e.message}"
                }
                _state.update { 
                    it.copy(
                        error = errorMessage,
                        isLoading = false
                    ) 
                }
            }
        }
    }
    
    fun updateBudget(budgetId: Long, newAmountCents: Long) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            
            try {
                val existingBudget = budgetRepository.getBudgetById(budgetId)
                if (existingBudget != null) {
                    val updatedBudget = existingBudget.copy(limitAmountCents = newAmountCents)
                    budgetRepository.updateBudget(updatedBudget)
                    _state.update { it.copy(isLoading = false) }
                } else {
                    _state.update { 
                        it.copy(
                            error = "Presupuesto no encontrado",
                            isLoading = false
                        ) 
                    }
                }
            } catch (e: Exception) {
                _state.update { 
                    it.copy(
                        error = "Error actualizando presupuesto: ${e.message}",
                        isLoading = false
                    ) 
                }
            }
        }
    }
    
    fun deleteBudget(budgetId: Long) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            
            try {
                budgetRepository.deleteBudget(budgetId)
                _state.update { it.copy(isLoading = false) }
            } catch (e: Exception) {
                _state.update { 
                    it.copy(
                        error = "Error eliminando presupuesto: ${e.message}",
                        isLoading = false
                    ) 
                }
            }
        }
    }
    
    fun toggleBudgetActive(budgetId: Long) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            
            try {
                val existingBudget = budgetRepository.getBudgetById(budgetId)
                if (existingBudget != null) {
                    val updatedBudget = existingBudget.copy(isActive = !existingBudget.isActive)
                    budgetRepository.updateBudget(updatedBudget)
                    _state.update { it.copy(isLoading = false) }
                } else {
                    _state.update { 
                        it.copy(
                            error = "Presupuesto no encontrado",
                            isLoading = false
                        ) 
                    }
                }
            } catch (e: Exception) {
                _state.update { 
                    it.copy(
                        error = "Error cambiando estado del presupuesto: ${e.message}",
                        isLoading = false
                    ) 
                }
            }
        }
    }
    
    fun clearError() {
        _state.update { it.copy(error = null) }
    }
    
    fun refreshData() {
        loadCategories()
        loadBudgets()
    }
}

data class BudgetManagementState(
    val selectedMonth: YearMonth = YearMonth.now(),
    val budgets: List<Budget> = emptyList(),
    val categories: List<Category> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)