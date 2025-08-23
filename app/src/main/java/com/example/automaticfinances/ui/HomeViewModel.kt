package com.example.automaticfinances.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.automaticfinances.data.db.Category
import com.example.automaticfinances.data.repo.CategoryRepository
import com.example.automaticfinances.data.repo.TransactionRepository
import com.example.automaticfinances.data.repo.TransactionWithCategory
import com.example.automaticfinances.data.repo.UserCategoryPreferenceRepository
import com.example.automaticfinances.data.repo.AccountRepository
import com.example.automaticfinances.data.repo.OpeningBalanceRepository
import com.example.automaticfinances.data.db.AppDatabase
import com.example.automaticfinances.data.db.CategoryAccuracy
import com.example.automaticfinances.ui.components.FilterSummary
import com.example.automaticfinances.ui.components.SavedFilter
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
    val monthlyIncome: Long = 0L,
    val monthlyExpenses: Long = 0L,
    val monthlyBalance: Long = 0L,
    // Account balance tracking
    val bankBalanceCents: Long = 0L,
    val cashBalanceCents: Long = 0L,
    val totalBalanceCents: Long = 0L,
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
    val categoryAccuracyStats: List<CategoryAccuracy> = emptyList(),
    // Service status tracking
    val serviceIssuesLastFixedAt: Long = 0L,
    // Advanced filter state
    val savedFilters: List<SavedFilter> = emptyList(),
    val filterStatistics: FilterSummary? = null
)

class HomeViewModel : ViewModel() {
    private val transactionRepository = TransactionRepository()
    private val categoryRepository = CategoryRepository()
    private val preferenceRepository = UserCategoryPreferenceRepository()
    private val accountRepository = AccountRepository()
    private val openingBalanceRepository = OpeningBalanceRepository(
        openingBalanceDao = AppDatabase.get().openingBalanceDao(),
        accountDao = AppDatabase.get().accountDao(),
        transactionDao = AppDatabase.get().transactionDao()
    )
    
    private val _state = MutableStateFlow(HomeState())
    val state: StateFlow<HomeState> = _state.asStateFlow()
    
    init {
        initializeAccounts()
        loadData()
        loadIntelligenceData()
    }
    
    private fun initializeAccounts() {
        viewModelScope.launch {
            accountRepository.initializeDefaultAccounts()
        }
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
                    
                    // Calcular totales del mes separados por tipo
                    val monthlyTransactions = allTransactions.filter { transaction ->
                        transaction.date >= monthStart && transaction.date <= monthEnd
                    }
                    val monthlyIncome = monthlyTransactions.filter { it.isIncome }.sumOf { it.amountCents }
                    val monthlyExpenses = monthlyTransactions.filter { !it.isIncome }.sumOf { it.amountCents }
                    val monthlyTotal = monthlyExpenses // Mantener compatibilidad
                    val monthlyBalance = monthlyIncome - monthlyExpenses
                    
                    // Aplicar todos los filtros
                    val filteredTransactions = applyFilters(allTransactions)
                    
                    Log.d("HomeViewModel", "Filtered transactions: ${filteredTransactions.size}")
                    
                    // Get correct balances using OpeningBalanceRepository
                    val balanceSummary = openingBalanceRepository.getOpeningBalanceSummary()
                    
                    _state.value = _state.value.copy(
                        categories = categories,
                        transactions = filteredTransactions,
                        totalMonthCOP = monthlyTotal,
                        monthlyIncome = monthlyIncome,
                        monthlyExpenses = monthlyExpenses,
                        // Update account balances using correct calculation
                        bankBalanceCents = balanceSummary.bankCurrentBalanceCents,
                        cashBalanceCents = balanceSummary.cashCurrentBalanceCents,
                        totalBalanceCents = balanceSummary.totalCurrentBalanceCents,
                        monthlyBalance = monthlyBalance,
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
        Log.d("HomeViewModel", "Cleared all filters")
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
    
    /**
     * Called when service issues are detected as resolved
     * Updates timestamp for auto-hide grace period
     */
    fun markServiceIssuesResolved() {
        _state.value = _state.value.copy(
            serviceIssuesLastFixedAt = System.currentTimeMillis()
        )
        Log.d("HomeViewModel", "Service issues marked as resolved, starting grace period")
    }
    
    /**
     * Reset service issues timestamp
     * Called when issues are detected
     */
    fun markServiceIssuesDetected() {
        _state.value = _state.value.copy(
            serviceIssuesLastFixedAt = 0L
        )
        Log.d("HomeViewModel", "Service issues detected, resetting timestamp")
    }
    
    // Advanced Filter Methods
    
    /**
     * Apply multiple filters at once
     */
    fun applyFilters(
        categoryId: Long? = null,
        searchQuery: String = "",
        dateStart: String? = null,
        dateEnd: String? = null,
        minAmount: Long? = null,
        maxAmount: Long? = null
    ) {
        _state.value = _state.value.copy(
            selectedCategoryFilter = categoryId,
            searchQuery = searchQuery,
            dateFilterStart = dateStart,
            dateFilterEnd = dateEnd,
            minAmountFilter = minAmount,
            maxAmountFilter = maxAmount
        )
        loadData()
        Log.d("HomeViewModel", "Applied multiple filters: category=$categoryId, search=$searchQuery, dates=$dateStart-$dateEnd, amounts=$minAmount-$maxAmount")
    }
    
    /**
     * Get current filter summary for preview
     */
    fun getCurrentFilterSummary(): FilterSummary {
        val currentState = _state.value
        val selectedCategory = currentState.categories.find { it.id == currentState.selectedCategoryFilter }
        
        return FilterSummary(
            categoryName = selectedCategory?.name,
            categoryIcon = selectedCategory?.icon,
            dateRange = if (currentState.dateFilterStart != null || currentState.dateFilterEnd != null) {
                "${currentState.dateFilterStart ?: "..."} - ${currentState.dateFilterEnd ?: "..."}"
            } else null,
            amountRange = if (currentState.minAmountFilter != null || currentState.maxAmountFilter != null) {
                val nf = java.text.NumberFormat.getCurrencyInstance(java.util.Locale("es", "CO"))
                "${if (currentState.minAmountFilter != null) nf.format(currentState.minAmountFilter / 100.0) else "Sin mínimo"} - ${if (currentState.maxAmountFilter != null) nf.format(currentState.maxAmountFilter / 100.0) else "Sin máximo"}"
            } else null,
            searchQuery = currentState.searchQuery.takeIf { it.isNotBlank() },
            totalFilters = listOfNotNull(
                currentState.selectedCategoryFilter,
                currentState.searchQuery.takeIf { it.isNotBlank() },
                currentState.dateFilterStart,
                currentState.dateFilterEnd,
                currentState.minAmountFilter,
                currentState.maxAmountFilter
            ).size,
            resultCount = currentState.transactions.size
        )
    }
    
    /**
     * Save current filter configuration
     */
    fun saveCurrentFilter(name: String, description: String, isFavorite: Boolean) {
        val currentState = _state.value
        val savedFilter = SavedFilter(
            id = java.util.UUID.randomUUID().toString(),
            name = name,
            description = description,
            categoryId = currentState.selectedCategoryFilter,
            dateStart = currentState.dateFilterStart,
            dateEnd = currentState.dateFilterEnd,
            minAmount = currentState.minAmountFilter,
            maxAmount = currentState.maxAmountFilter,
            searchQuery = currentState.searchQuery.takeIf { it.isNotBlank() },
            isFavorite = isFavorite
        )
        
        val updatedSavedFilters = currentState.savedFilters + savedFilter
        _state.value = currentState.copy(savedFilters = updatedSavedFilters)
        
        Log.d("HomeViewModel", "Saved filter: $name with ${getCurrentFilterSummary().totalFilters} active filters")
    }
    
    /**
     * Apply a saved filter
     */
    fun applySavedFilter(savedFilter: SavedFilter) {
        applyFilters(
            categoryId = savedFilter.categoryId,
            searchQuery = savedFilter.searchQuery ?: "",
            dateStart = savedFilter.dateStart,
            dateEnd = savedFilter.dateEnd,
            minAmount = savedFilter.minAmount,
            maxAmount = savedFilter.maxAmount
        )
        
        // Update usage count
        val updatedFilters = _state.value.savedFilters.map { filter ->
            if (filter.id == savedFilter.id) {
                filter.copy(usageCount = filter.usageCount + 1)
            } else filter
        }
        _state.value = _state.value.copy(savedFilters = updatedFilters)
        
        Log.d("HomeViewModel", "Applied saved filter: ${savedFilter.name}")
    }
    
    /**
     * Delete a saved filter
     */
    fun deleteSavedFilter(savedFilter: SavedFilter) {
        val updatedFilters = _state.value.savedFilters.filterNot { it.id == savedFilter.id }
        _state.value = _state.value.copy(savedFilters = updatedFilters)
        Log.d("HomeViewModel", "Deleted saved filter: ${savedFilter.name}")
    }
    
    /**
     * Toggle favorite status of a saved filter
     */
    fun toggleSavedFilterFavorite(savedFilter: SavedFilter) {
        val updatedFilters = _state.value.savedFilters.map { filter ->
            if (filter.id == savedFilter.id) {
                filter.copy(isFavorite = !filter.isFavorite)
            } else filter
        }
        _state.value = _state.value.copy(savedFilters = updatedFilters)
        Log.d("HomeViewModel", "Toggled favorite for filter: ${savedFilter.name} to ${!savedFilter.isFavorite}")
    }
    
    /**
     * Get filter statistics for display
     */
    fun getFilterStatistics(): Triple<Int, Int, Int> {
        // Total transactions, filtered transactions, filter percentage
        val allTransactionsCount = _state.value.transactions.size // This would need to be adjusted to get total count
        val filteredCount = _state.value.transactions.size
        val percentage = if (allTransactionsCount > 0) {
            (filteredCount.toFloat() / allTransactionsCount * 100).toInt()
        } else 0
        
        return Triple(allTransactionsCount, filteredCount, percentage)
    }
    
    /**
     * Check if there are active filters
     */
    fun hasActiveFilters(): Boolean {
        val currentState = _state.value
        return currentState.selectedCategoryFilter != null ||
               currentState.searchQuery.isNotBlank() ||
               currentState.dateFilterStart != null ||
               currentState.dateFilterEnd != null ||
               currentState.minAmountFilter != null ||
               currentState.maxAmountFilter != null
    }
    
    /**
     * Get quick filter suggestions based on recent transactions
     */
    fun getQuickFilterSuggestions(): List<String> {
        val recentTransactions = _state.value.transactions.take(10)
        val suggestions = mutableSetOf<String>()
        
        // Add common merchants
        recentTransactions.forEach { transaction ->
            val words = transaction.description.split(" ")
            words.forEach { word ->
                if (word.length > 3 && !word.matches(Regex(".*\\d.*"))) {
                    suggestions.add(word.trim())
                }
            }
        }
        
        return suggestions.take(5).toList()
    }
}