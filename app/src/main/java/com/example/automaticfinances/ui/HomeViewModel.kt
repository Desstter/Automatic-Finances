package com.example.automaticfinances.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.automaticfinances.data.db.Category
import com.example.automaticfinances.data.db.CategoryAccuracy
import com.example.automaticfinances.data.repo.CategoryRepository
import com.example.automaticfinances.data.repo.PendingTransactionRepository
import com.example.automaticfinances.data.repo.TransactionRepository
import com.example.automaticfinances.data.repo.TransactionWithCategory
import com.example.automaticfinances.data.repo.UserCategoryPreferenceRepository
import com.example.automaticfinances.data.repo.AccountRepository
import com.example.automaticfinances.data.repo.AnalyticsRepository
import com.example.automaticfinances.data.repo.OpeningBalanceRepository
import com.example.automaticfinances.data.preferences.ThemeRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.YearMonth
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
    val categoryAccuracyStats: List<CategoryAccuracy> = emptyList(),
    // Low-confidence captures awaiting review (PROD-1). Drives the Home banner.
    val pendingReviewCount: Int = 0,
    // Personalization: user's name (drives the greeting) + proactive month-pace insight.
    val userName: String = "",
    val proactiveInsight: ProactiveInsight? = null
)

/**
 * A short, personal nudge shown on the dashboard about how this month's spending is pacing
 * versus the previous month. [isPositive] tints it (profit green vs warning amber).
 */
data class ProactiveInsight(
    val message: String,
    val isPositive: Boolean
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val categoryRepository: CategoryRepository,
    private val preferenceRepository: UserCategoryPreferenceRepository,
    private val accountRepository: AccountRepository,
    private val openingBalanceRepository: OpeningBalanceRepository,
    private val pendingTransactionRepository: PendingTransactionRepository,
    private val analyticsRepository: AnalyticsRepository,
    private val themeRepository: ThemeRepository
) : ViewModel() {

    private val _state = MutableStateFlow(HomeState())
    val state: StateFlow<HomeState> = _state.asStateFlow()

    init {
        initializeAccounts()
        observeData()
        observePendingReview()
        observeUserName()
        loadIntelligenceData()
    }

    /** Keeps the greeting name in sync with whatever the user set in Ajustes. */
    private fun observeUserName() {
        viewModelScope.launch {
            themeRepository.userName.collect { name ->
                _state.value = _state.value.copy(userName = name)
            }
        }
    }

    /**
     * Builds the proactive month-pace insight from the spending projection. Returns null when
     * there isn't enough signal yet (no projected spend) so the card simply doesn't show.
     */
    private suspend fun buildProactiveInsight(): ProactiveInsight? {
        return try {
            val prediction = analyticsRepository.getSpendingPrediction(YearMonth.now())
            if (prediction.projectedTotalCents <= 0L) return null
            val projected = formatCents(prediction.projectedTotalCents)
            val pct = prediction.changeFromPreviousMonth
            val absPct = kotlin.math.abs(pct).toInt()
            when {
                // No baseline yet (first month of use): keep it simple and neutral.
                pct == 0f -> ProactiveInsight(
                    message = "Vas en $projected proyectado para cerrar el mes.",
                    isPositive = true
                )
                pct <= -10f -> ProactiveInsight(
                    message = "Buen ritmo: a este paso cerrarás en $projected, $absPct% menos que el mes pasado.",
                    isPositive = true
                )
                pct >= 10f -> ProactiveInsight(
                    message = "Ojo al gasto: a este ritmo cerrarás en $projected, $absPct% más que el mes pasado.",
                    isPositive = false
                )
                else -> ProactiveInsight(
                    message = "Vas en $projected proyectado este mes, en línea con el anterior.",
                    isPositive = true
                )
            }
        } catch (e: Exception) {
            Log.e("HomeViewModel", "Error building proactive insight", e)
            null
        }
    }

    private fun formatCents(cents: Long): String =
        java.text.NumberFormat.getCurrencyInstance(java.util.Locale("es", "CO")).format(cents / 100.0)

    /** Live count of captures waiting in the "Por revisar" queue, for the Home banner. */
    private fun observePendingReview() {
        viewModelScope.launch {
            pendingTransactionRepository.observeCount().collect { count ->
                _state.value = _state.value.copy(pendingReviewCount = count)
            }
        }
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

                    // Transfers move money between accounts; they are neither income nor expense,
                    // so they are excluded from the monthly snapshot (they still affect balances).
                    val monthlyTransactions = transactions
                        .filter { it.date in monthStart..monthEnd && !it.isTransfer }
                    val monthlyIncome = monthlyTransactions.filter { it.isIncome }.sumOf { it.amountCents }
                    val monthlyExpenses = monthlyTransactions.filter { !it.isIncome }.sumOf { it.amountCents }

                    val balanceSummary = openingBalanceRepository.getOpeningBalanceSummary()
                    val insight = buildProactiveInsight()

                    _state.value = _state.value.copy(
                        categories = categories,
                        transactions = filterTransactions(transactions),
                        monthlyIncome = monthlyIncome,
                        monthlyExpenses = monthlyExpenses,
                        bankBalanceCents = balanceSummary.bankCurrentBalanceCents,
                        cashBalanceCents = balanceSummary.cashCurrentBalanceCents,
                        totalBalanceCents = balanceSummary.totalCurrentBalanceCents,
                        proactiveInsight = insight,
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
