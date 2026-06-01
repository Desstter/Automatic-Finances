package com.example.automaticfinances.ui.transaction

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.automaticfinances.data.db.Category
import com.example.automaticfinances.data.repo.CategoryRepository
import com.example.automaticfinances.data.repo.TransactionRepository
import com.example.automaticfinances.data.repo.TransactionWithCategory
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import android.util.Log
import javax.inject.Inject

/**
 * State for the full-history screen. This is the single advanced-filter surface in the app:
 * "Origen"/"Tipo" are exposed as quick chips, while category/date/amount/search are driven by
 * the shared [com.example.automaticfinances.ui.components.FilterBottomSheet].
 */
data class TransactionHistoryState(
    val allTransactions: List<TransactionWithCategory> = emptyList(),
    val filteredTransactions: List<TransactionWithCategory> = emptyList(),
    val categories: List<Category> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val sourceFilter: String? = null, // "manual", "notif", null
    val typeFilter: String? = null,   // "COMPRA", "TRANSFERENCIA", "MANUAL", null
    val categoryFilter: Long? = null,
    val searchQuery: String = "",
    val dateStart: String? = null,
    val dateEnd: String? = null,
    val minAmount: Long? = null,
    val maxAmount: Long? = null
) {
    val hasActiveFilters: Boolean
        get() = sourceFilter != null || typeFilter != null || categoryFilter != null ||
            searchQuery.isNotBlank() || dateStart != null || dateEnd != null ||
            minAmount != null || maxAmount != null
}

@HiltViewModel
class TransactionHistoryViewModel @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val categoryRepository: CategoryRepository
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
                    _state.value = _state.value.copy(
                        allTransactions = transactions,
                        filteredTransactions = applyFilters(transactions, _state.value),
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
        s: TransactionHistoryState
    ): List<TransactionWithCategory> {
        var filtered = transactions

        s.sourceFilter?.let { source ->
            filtered = when (source) {
                "manual" -> filtered.filter { it.source == "manual" }
                "notif" -> filtered.filter { it.source.startsWith("notif") }
                else -> filtered
            }
        }
        s.typeFilter?.let { type -> filtered = filtered.filter { it.type == type } }
        s.categoryFilter?.let { id -> filtered = filtered.filter { it.categoryId == id } }
        if (s.searchQuery.isNotBlank()) {
            filtered = filtered.filter { it.description.contains(s.searchQuery, ignoreCase = true) }
        }
        s.dateStart?.let { start -> filtered = filtered.filter { it.date >= start } }
        s.dateEnd?.let { end -> filtered = filtered.filter { it.date <= end } }
        s.minAmount?.let { min -> filtered = filtered.filter { it.amountCents >= min } }
        s.maxAmount?.let { max -> filtered = filtered.filter { it.amountCents <= max } }

        return filtered.sortedByDescending { "${it.date} ${it.time}" }
    }

    private fun update(newState: TransactionHistoryState) {
        _state.value = newState.copy(
            filteredTransactions = applyFilters(newState.allTransactions, newState)
        )
    }

    fun setSourceFilter(source: String?) = update(_state.value.copy(sourceFilter = source))

    fun setTypeFilter(type: String?) = update(_state.value.copy(typeFilter = type))

    /** Commit category/date/amount/search from the shared filter sheet. */
    fun applyAdvancedFilters(
        categoryId: Long?,
        search: String,
        dateStart: String?,
        dateEnd: String?,
        minAmount: Long?,
        maxAmount: Long?
    ) = update(
        _state.value.copy(
            categoryFilter = categoryId,
            searchQuery = search,
            dateStart = dateStart,
            dateEnd = dateEnd,
            minAmount = minAmount,
            maxAmount = maxAmount
        )
    )

    /** Count how many transactions a pending sheet selection would match (live preview). */
    fun countMatching(
        categoryId: Long?,
        search: String,
        dateStart: String?,
        dateEnd: String?,
        minAmount: Long?,
        maxAmount: Long?
    ): Int = applyFilters(
        _state.value.allTransactions,
        _state.value.copy(
            categoryFilter = categoryId,
            searchQuery = search,
            dateStart = dateStart,
            dateEnd = dateEnd,
            minAmount = minAmount,
            maxAmount = maxAmount
        )
    ).size

    fun clearFilters() = update(
        _state.value.copy(
            sourceFilter = null,
            typeFilter = null,
            categoryFilter = null,
            searchQuery = "",
            dateStart = null,
            dateEnd = null,
            minAmount = null,
            maxAmount = null
        )
    )

    fun refreshData() = loadData()
}
