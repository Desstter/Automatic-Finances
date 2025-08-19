package com.example.automaticfinances.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.automaticfinances.data.db.Category
import com.example.automaticfinances.data.repo.CategoryRepository
import com.example.automaticfinances.data.repo.TransactionRepository
import com.example.automaticfinances.data.repo.TransactionWithCategory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import android.util.Log
import java.time.LocalDate
import java.time.format.DateTimeFormatter

data class HomeState(
    val transactions: List<TransactionWithCategory> = emptyList(),
    val categories: List<Category> = emptyList(),
    val totalMonthCOP: Long = 0L,
    val selectedCategoryFilter: Long? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
    val currentMonth: String = LocalDate.now().format(DateTimeFormatter.ofPattern("MMMM yyyy")),
    val showFilters: Boolean = false
)

class HomeViewModel : ViewModel() {
    private val transactionRepository = TransactionRepository()
    private val categoryRepository = CategoryRepository()
    
    private val _state = MutableStateFlow(HomeState())
    val state: StateFlow<HomeState> = _state.asStateFlow()
    
    init {
        loadData()
    }
    
    private fun loadData() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)
            
            try {
                // Combinar categorías y transacciones en un solo flow
                combine(
                    categoryRepository.getAllActive(),
                    transactionRepository.getTransactionsWithCategories()
                ) { categories, transactions ->
                    Pair(categories, transactions)
                }.collectLatest { (categories, transactions) ->
                    Log.d("HomeViewModel", "Received ${categories.size} categories and ${transactions.size} transactions")
                    transactions.forEach { tx ->
                        Log.d("HomeViewModel", "Transaction: ${tx.id}, type=${tx.type}, source=${tx.source}, date=${tx.date}, amount=${tx.amountCents}")
                    }
                    
                    val currentDate = LocalDate.now()
                    val monthStart = currentDate.withDayOfMonth(1).format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
                    val monthEnd = currentDate.withDayOfMonth(currentDate.lengthOfMonth()).format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
                    
                    Log.d("HomeViewModel", "Month range: $monthStart to $monthEnd")
                    
                    // Calcular total del mes
                    val monthlyTransactions = transactions.filter { transaction ->
                        transaction.date >= monthStart && transaction.date <= monthEnd
                    }
                    Log.d("HomeViewModel", "Monthly transactions: ${monthlyTransactions.size}")
                    val monthlyTotal = monthlyTransactions.sumOf { it.amountCents }
                    Log.d("HomeViewModel", "Monthly total: $monthlyTotal cents")
                    
                    // Aplicar filtro de categoría si está seleccionado
                    val filteredTransactions = if (_state.value.selectedCategoryFilter != null) {
                        transactions.filter { it.categoryId == _state.value.selectedCategoryFilter }
                    } else {
                        transactions
                    }
                    
                    Log.d("HomeViewModel", "Filtered transactions: ${filteredTransactions.size}")
                    
                    _state.value = _state.value.copy(
                        categories = categories,
                        transactions = filteredTransactions,
                        totalMonthCOP = monthlyTotal,
                        isLoading = false
                    )
                }
                
            } catch (e: Exception) {
                Log.e("HomeViewModel", "Error loading data", e)
                _state.value = _state.value.copy(
                    isLoading = false,
                    error = "Error al cargar datos: ${e.message}"
                )
            }
        }
    }
    
    fun filterByCategory(categoryId: Long?) {
        _state.value = _state.value.copy(selectedCategoryFilter = categoryId)
        loadData() // Recargar con el nuevo filtro
    }
    
    fun clearCategoryFilter() {
        filterByCategory(null)
    }
    
    fun toggleFilters() {
        _state.value = _state.value.copy(showFilters = !_state.value.showFilters)
    }
    
    fun clearError() {
        _state.value = _state.value.copy(error = null)
    }
    
    fun getCategoryById(categoryId: Long): Category? {
        return _state.value.categories.find { it.id == categoryId }
    }
    
    fun refreshData() {
        Log.d("HomeViewModel", "Manual refresh requested")
        loadData()
    }

    fun forceRefresh() {
        Log.d("HomeViewModel", "Force refresh - clearing state and reloading")
        _state.value = HomeState()
        loadData()
    }
}