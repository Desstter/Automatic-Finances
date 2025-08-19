package com.example.automaticfinances.ui.transaction

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.automaticfinances.data.db.Category
import com.example.automaticfinances.data.repo.CategoryRepository
import com.example.automaticfinances.data.repo.TransactionRepository
import com.example.automaticfinances.data.repo.TransactionWithCategory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import android.util.Log

data class TransactionHistoryState(
    val allTransactions: List<TransactionWithCategory> = emptyList(),
    val filteredTransactions: List<TransactionWithCategory> = emptyList(),
    val categories: List<Category> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val showFilters: Boolean = false,
    val sourceFilter: String? = null, // "manual", "notif", null
    val typeFilter: String? = null,   // "COMPRA", "TRANSFERENCIA", "MANUAL", null
    val categoryFilter: Long? = null
)

class TransactionHistoryViewModel(
    private val transactionRepository: TransactionRepository = TransactionRepository(),
    private val categoryRepository: CategoryRepository = CategoryRepository()
) : ViewModel() {

    private val _state = MutableStateFlow(TransactionHistoryState())
    val state: StateFlow<TransactionHistoryState> = _state.asStateFlow()

    init {
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)
            
            try {
                combine(
                    transactionRepository.getTransactionsWithCategories(),
                    categoryRepository.getAllActive()
                ) { transactions, categories ->
                    Pair(transactions, categories)
                }.collectLatest { (transactions, categories) ->
                    Log.d("TransactionHistoryVM", "Loaded ${transactions.size} transactions and ${categories.size} categories")
                    
                    val filtered = applyFilters(transactions, _state.value)
                    
                    _state.value = _state.value.copy(
                        allTransactions = transactions,
                        filteredTransactions = filtered,
                        categories = categories,
                        isLoading = false
                    )
                }
            } catch (e: Exception) {
                Log.e("TransactionHistoryVM", "Error loading data", e)
                _state.value = _state.value.copy(
                    isLoading = false,
                    error = "Error al cargar historial: ${e.message}"
                )
            }
        }
    }

    private fun applyFilters(
        transactions: List<TransactionWithCategory>,
        currentState: TransactionHistoryState
    ): List<TransactionWithCategory> {
        var filtered = transactions

        // Filtro por origen
        currentState.sourceFilter?.let { sourceFilter ->
            filtered = when (sourceFilter) {
                "manual" -> filtered.filter { it.source == "manual" }
                "notif" -> filtered.filter { it.source?.startsWith("notif") == true }
                else -> filtered
            }
        }

        // Filtro por tipo
        currentState.typeFilter?.let { typeFilter ->
            filtered = filtered.filter { it.type == typeFilter }
        }

        // Filtro por categoría
        currentState.categoryFilter?.let { categoryFilter ->
            filtered = filtered.filter { it.categoryId == categoryFilter }
        }

        Log.d("TransactionHistoryVM", "Applied filters: source=${currentState.sourceFilter}, type=${currentState.typeFilter}, category=${currentState.categoryFilter}")
        Log.d("TransactionHistoryVM", "Filtered from ${transactions.size} to ${filtered.size} transactions")

        return filtered.sortedByDescending { "${it.date} ${it.time}" }
    }

    fun toggleFilters() {
        _state.value = _state.value.copy(showFilters = !_state.value.showFilters)
    }

    fun setSourceFilter(source: String?) {
        val newState = _state.value.copy(sourceFilter = source)
        _state.value = newState.copy(
            filteredTransactions = applyFilters(_state.value.allTransactions, newState)
        )
    }

    fun setTypeFilter(type: String?) {
        val newState = _state.value.copy(typeFilter = type)
        _state.value = newState.copy(
            filteredTransactions = applyFilters(_state.value.allTransactions, newState)
        )
    }

    fun setCategoryFilter(categoryId: Long?) {
        val newState = _state.value.copy(categoryFilter = categoryId)
        _state.value = newState.copy(
            filteredTransactions = applyFilters(_state.value.allTransactions, newState)
        )
    }

    fun clearFilters() {
        val newState = _state.value.copy(
            sourceFilter = null,
            typeFilter = null,
            categoryFilter = null
        )
        _state.value = newState.copy(
            filteredTransactions = applyFilters(_state.value.allTransactions, newState)
        )
    }

    fun refreshData() {
        loadData()
    }
}