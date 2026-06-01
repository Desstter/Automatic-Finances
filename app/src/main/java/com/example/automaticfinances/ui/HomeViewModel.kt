package com.example.automaticfinances.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.automaticfinances.data.db.Category
import com.example.automaticfinances.data.db.CategoryAccuracy
import com.example.automaticfinances.data.repo.CategoryRepository
import com.example.automaticfinances.data.repo.TransactionRepository
import com.example.automaticfinances.data.repo.TransactionWithCategory
import com.example.automaticfinances.data.repo.UserCategoryPreferenceRepository
import com.example.automaticfinances.data.repo.AccountRepository
import com.example.automaticfinances.data.repo.OpeningBalanceRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import android.util.Log
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject

/**
 * State for the Home dashboard. Home is a summary surface: balances, monthly snapshot,
 * intelligence and a quick text search over recent movements. Advanced filtering
 * (category/date/amount) lives on the Movimientos screen, not here.
 */
data class HomeState(
    val transactions: List<TransactionWithCategory> = emptyList(),
    val categories: List<Category> = emptyList(),
    val monthlyIncome: Long = 0L,
    val monthlyExpenses: Long = 0L,
    // Account balance tracking
    val bankBalanceCents: Long = 0L,
    val cashBalanceCents: Long = 0L,
    val totalBalanceCents: Long = 0L,
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val error: String? = null,
    // Quick search
    val searchQuery: String = "",
    // Intelligence data
    val intelligenceActive: Boolean = false,
    val totalPreferences: Int = 0,
    val overallAccuracy: Float = 0f,
    val categoryAccuracyStats: List<CategoryAccuracy> = emptyList()
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val categoryRepository: CategoryRepository,
    private val preferenceRepository: UserCategoryPreferenceRepository,
    private val accountRepository: AccountRepository,
    private val openingBalanceRepository: OpeningBalanceRepository
) : ViewModel() {

    private val _state = MutableStateFlow(HomeState())
    val state: StateFlow<HomeState> = _state.asStateFlow()

    init {
        initializeAccounts()
        observeData()
        loadIntelligenceData()
    }

    // Unfiltered source data, cached so that search changes re-derive the visible list in
    // memory instead of re-querying the database on every keystroke.
    private var allTransactions: List<TransactionWithCategory> = emptyList()

    private fun initializeAccounts() {
        viewModelScope.launch {
            accountRepository.initializeDefaultAccounts()
        }
    }

    /**
     * Single long-lived collector of categories + transactions. Room emits a new value whenever
     * the underlying tables change, so monthly totals and balances stay live without manual
     * reloads. The quick search runs in memory via [reapplyFilters].
     */
    private fun observeData() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)
            try {
                combine(
                    categoryRepository.getAllActive(),
                    transactionRepository.getTransactionsWithCategories()
                ) { categories, transactions ->
                    Pair(categories, transactions)
                }.collectLatest { (categories, transactions) ->
                    allTransactions = transactions

                    val currentDate = LocalDate.now()
                    val monthStart = currentDate.withDayOfMonth(1).format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
                    val monthEnd = currentDate.withDayOfMonth(currentDate.lengthOfMonth()).format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))

                    val monthlyTransactions = transactions.filter { it.date in monthStart..monthEnd }
                    val monthlyIncome = monthlyTransactions.filter { it.isIncome }.sumOf { it.amountCents }
                    val monthlyExpenses = monthlyTransactions.filter { !it.isIncome }.sumOf { it.amountCents }

                    val balanceSummary = openingBalanceRepository.getOpeningBalanceSummary()

                    _state.value = _state.value.copy(
                        categories = categories,
                        transactions = filterTransactions(transactions),
                        monthlyIncome = monthlyIncome,
                        monthlyExpenses = monthlyExpenses,
                        bankBalanceCents = balanceSummary.bankCurrentBalanceCents,
                        cashBalanceCents = balanceSummary.cashCurrentBalanceCents,
                        totalBalanceCents = balanceSummary.totalCurrentBalanceCents,
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

    /** Re-derives the visible transaction list from the in-memory cache — no DB round-trip. */
    private fun reapplyFilters() {
        _state.value = _state.value.copy(transactions = filterTransactions(allTransactions))
    }

    private fun filterTransactions(transactions: List<TransactionWithCategory>): List<TransactionWithCategory> {
        val query = _state.value.searchQuery
        if (query.isBlank()) return transactions
        return transactions.filter { it.description.contains(query, ignoreCase = true) }
    }

    fun updateSearchQuery(query: String) {
        _state.value = _state.value.copy(searchQuery = query)
        reapplyFilters()
    }

    fun clearSearch() {
        updateSearchQuery("")
    }

    fun refreshData() {
        // The data flow is already live; a manual refresh just re-reads the balance summary
        // (which is not part of the flow) and re-applies the search. No collector restart.
        viewModelScope.launch {
            _state.value = _state.value.copy(isRefreshing = true)
            try {
                val balanceSummary = openingBalanceRepository.getOpeningBalanceSummary()
                _state.value = _state.value.copy(
                    transactions = filterTransactions(allTransactions),
                    bankBalanceCents = balanceSummary.bankCurrentBalanceCents,
                    cashBalanceCents = balanceSummary.cashCurrentBalanceCents,
                    totalBalanceCents = balanceSummary.totalCurrentBalanceCents,
                    isRefreshing = false
                )
            } catch (e: Exception) {
                Log.e("HomeViewModel", "Error refreshing data", e)
                _state.value = _state.value.copy(isRefreshing = false)
            }
        }
    }

    /**
     * Silent re-read of the balance summary only. Transactions/categories already update live via
     * the Room flow, but opening balances live in a separate table that the flow doesn't observe,
     * so this is called when returning to Home (e.g. after editing an opening balance).
     */
    fun refreshBalances() {
        viewModelScope.launch {
            try {
                val balanceSummary = openingBalanceRepository.getOpeningBalanceSummary()
                _state.value = _state.value.copy(
                    bankBalanceCents = balanceSummary.bankCurrentBalanceCents,
                    cashBalanceCents = balanceSummary.cashCurrentBalanceCents,
                    totalBalanceCents = balanceSummary.totalCurrentBalanceCents
                )
            } catch (e: Exception) {
                Log.e("HomeViewModel", "Error refreshing balances", e)
            }
        }
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
            } catch (e: Exception) {
                Log.e("HomeViewModel", "Error loading intelligence data", e)
            }
        }
    }
}
