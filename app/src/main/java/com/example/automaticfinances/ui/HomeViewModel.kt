package com.example.automaticfinances.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.automaticfinances.data.db.Category
import com.example.automaticfinances.data.repo.CategoryRepository
import com.example.automaticfinances.data.repo.TransactionRepository
import com.example.automaticfinances.data.repo.TransactionWithCategory
import com.example.automaticfinances.data.repo.UserCategoryPreferenceRepository
import com.example.automaticfinances.data.db.CategoryAccuracy
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
    val isRefreshing: Boolean = false,
    val error: String? = null,
    val currentMonth: String = LocalDate.now().format(DateTimeFormatter.ofPattern("MMMM yyyy")),
    val showFilters: Boolean = false,
    val searchQuery: String = "",
    val dateFilterStart: String? = null,
    val dateFilterEnd: String? = null,
    val minAmountFilter: Long? = null,
    val maxAmountFilter: Long? = null,
    // Intelligence data
    val intelligenceActive: Boolean = false,
    val totalPreferences: Int = 0,
    val overallAccuracy: Float = 0f,
    val categoryAccuracyStats: List<CategoryAccuracy> = emptyList()
)

class HomeViewModel : ViewModel() {
    private val transactionRepository = TransactionRepository()
    private val categoryRepository = CategoryRepository()
    private val preferenceRepository = UserCategoryPreferenceRepository()
    
    private val _state = MutableStateFlow(HomeState())
    val state: StateFlow<HomeState> = _state.asStateFlow()
    
    init {
        loadData()
        loadIntelligenceData()
    }
    
    private fun loadData(isRefresh: Boolean = false) {
        viewModelScope.launch {
            if (isRefresh) {
                _state.value = _state.value.copy(isRefreshing = true, error = null)
            } else {
                _state.value = _state.value.copy(isLoading = true, error = null)
            }
            
            try {
                // Combinar categorías y transacciones en un solo flow
                combine(
                    categoryRepository.getAllActive(),
                    transactionRepository.getTransactionsWithCategories()
                ) { categories, transactions ->
                    Pair(categories, transactions)
                }.collectLatest { (categories, allTransactions) ->
                    Log.d("HomeViewModel", "Received ${categories.size} categories and ${allTransactions.size} transactions")
                    
                    val currentDate = LocalDate.now()
                    val monthStart = currentDate.withDayOfMonth(1).format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
                    val monthEnd = currentDate.withDayOfMonth(currentDate.lengthOfMonth()).format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
                    
                    // Calcular total del mes
                    val monthlyTransactions = allTransactions.filter { transaction ->
                        transaction.date >= monthStart && transaction.date <= monthEnd
                    }
                    val monthlyTotal = monthlyTransactions.sumOf { it.amountCents }
                    
                    // Aplicar todos los filtros
                    val filteredTransactions = applyFilters(allTransactions)
                    
                    Log.d("HomeViewModel", "Filtered transactions: ${filteredTransactions.size}")
                    
                    _state.value = _state.value.copy(
                        categories = categories,
                        transactions = filteredTransactions,
                        totalMonthCOP = monthlyTotal,
                        isLoading = false,
                        isRefreshing = false
                    )
                }
                
            } catch (e: Exception) {
                Log.e("HomeViewModel", "Error loading data", e)
                _state.value = _state.value.copy(
                    isLoading = false,
                    isRefreshing = false,
                    error = "Error al cargar datos: ${e.message}"
                )
            }
        }
    }
    
    private fun applyFilters(transactions: List<TransactionWithCategory>): List<TransactionWithCategory> {
        var filtered = transactions
        
        // Filtro de categoría
        _state.value.selectedCategoryFilter?.let { categoryId ->
            filtered = filtered.filter { it.categoryId == categoryId }
        }
        
        // Filtro de búsqueda
        if (_state.value.searchQuery.isNotBlank()) {
            filtered = filtered.filter { 
                it.description.contains(_state.value.searchQuery, ignoreCase = true)
            }
        }
        
        // Filtro de fecha
        _state.value.dateFilterStart?.let { startDate ->
            filtered = filtered.filter { it.date >= startDate }
        }
        _state.value.dateFilterEnd?.let { endDate ->
            filtered = filtered.filter { it.date <= endDate }
        }
        
        // Filtro de monto
        _state.value.minAmountFilter?.let { minAmount ->
            filtered = filtered.filter { it.amountCents >= minAmount }
        }
        _state.value.maxAmountFilter?.let { maxAmount ->
            filtered = filtered.filter { it.amountCents <= maxAmount }
        }
        
        return filtered
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
        loadData(isRefresh = true)
    }

    fun forceRefresh() {
        Log.d("HomeViewModel", "Force refresh - clearing state and reloading")
        _state.value = HomeState()
        loadData()
    }
    
    // Nuevas funciones para búsqueda y filtros
    fun updateSearchQuery(query: String) {
        _state.value = _state.value.copy(searchQuery = query)
        loadData() // Recargar con el nuevo filtro
    }
    
    fun setDateFilter(startDate: String?, endDate: String?) {
        _state.value = _state.value.copy(
            dateFilterStart = startDate,
            dateFilterEnd = endDate
        )
        loadData()
    }
    
    fun setAmountFilter(minAmount: Long?, maxAmount: Long?) {
        _state.value = _state.value.copy(
            minAmountFilter = minAmount,
            maxAmountFilter = maxAmount
        )
        loadData()
    }
    
    fun clearAllFilters() {
        _state.value = _state.value.copy(
            selectedCategoryFilter = null,
            searchQuery = "",
            dateFilterStart = null,
            dateFilterEnd = null,
            minAmountFilter = null,
            maxAmountFilter = null,
            showFilters = false
        )
        loadData()
    }
    
    private fun loadIntelligenceData() {
        viewModelScope.launch {
            try {
                val totalPreferences = preferenceRepository.getTotalPreferences()
                val overallAccuracy = preferenceRepository.getOverallAccuracy()
                val categoryStats = categoryRepository.getCategoryAccuracyStats()
                
                _state.value = _state.value.copy(
                    intelligenceActive = totalPreferences > 0,
                    totalPreferences = totalPreferences,
                    overallAccuracy = overallAccuracy,
                    categoryAccuracyStats = categoryStats
                )
                
                Log.d("HomeViewModel", "Intelligence data loaded: $totalPreferences preferences, ${(overallAccuracy * 100).toInt()}% accuracy")
                
            } catch (e: Exception) {
                Log.e("HomeViewModel", "Error loading intelligence data", e)
            }
        }
    }
}