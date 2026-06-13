package com.example.automaticfinances.ui.insights

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.automaticfinances.data.db.*
import com.example.automaticfinances.data.repo.BudgetRepository
import com.example.automaticfinances.data.repo.TransactionRepository
import com.example.automaticfinances.data.repo.AnalyticsRepository
import com.example.automaticfinances.data.repo.CategoryRepository
import com.example.automaticfinances.data.repo.FinancialAdvisorRepository
import com.example.automaticfinances.data.repo.InsightsRepository
import com.example.automaticfinances.data.models.AdvisorUiState
import com.example.automaticfinances.data.models.ChartData
import com.example.automaticfinances.data.models.ChartType
import com.example.automaticfinances.data.models.IncomeVsExpenseComparison
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import javax.inject.Inject

@HiltViewModel
class FinancialDashboardViewModel @Inject constructor(
    private val budgetRepository: BudgetRepository,
    private val transactionRepository: TransactionRepository,
    private val categoryRepository: CategoryRepository,
    private val analyticsRepository: AnalyticsRepository,
    private val insightsRepository: InsightsRepository,
    private val financialAdvisorRepository: FinancialAdvisorRepository
) : ViewModel() {

    private val _state = MutableStateFlow(FinancialDashboardState())
    val state: StateFlow<FinancialDashboardState> = _state.asStateFlow()

    /** Report signature (month + key figures) the current advice was generated for; gates re-querying. */
    private var lastAdvisedSignature: String? = null

    init {
        loadDashboardData()
        loadChartData()
        loadAiInsights()
    }

    /**
     * Generates the deterministic insights report for the selected month and asks the AI advisor to
     * narrate it. Best-effort and never throws: the outcome is encoded in [FinancialDashboardState.aiAdvisor].
     * Skips the LLM round-trip when the report hasn't changed since the last successful advice unless
     * [force] is set (the card's refresh / retry actions pass `force = true`).
     */
    fun loadAiInsights(force: Boolean = false) {
        viewModelScope.launch {
            val month = _state.value.selectedMonth
            // For a finished month, end-of-month makes daysElapsed == lengthOfMonth, so the digest's
            // run-rate collapses to the real total; for the current month it stays month-to-date.
            val asOf = if (month == YearMonth.now()) LocalDate.now(BOGOTA) else month.atEndOfMonth()

            val report = try {
                insightsRepository.generateReport(asOf)
            } catch (e: Exception) {
                _state.update { it.copy(aiAdvisor = AdvisorUiState.Error(com.example.automaticfinances.data.remote.LlmFailure.UNKNOWN)) }
                return@launch
            }

            val signature = "$month|${report.digest.spentMtdCents}|${report.digest.incomeMtdCents}|" +
                "${report.digest.topCategoryCents}|${report.digest.expenseCount}|" +
                "${report.subscriptions.size}|${report.anomalies.size}"

            if (!force && signature == lastAdvisedSignature && _state.value.aiAdvisor is AdvisorUiState.Success) {
                return@launch
            }

            _state.update { it.copy(aiAdvisor = AdvisorUiState.Loading) }
            val result = financialAdvisorRepository.advise(report)
            if (result is AdvisorUiState.Success) lastAdvisedSignature = signature
            _state.update { it.copy(aiAdvisor = result) }
        }
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
            
            // Load monthly spending (expenses only)
            try {
                val monthlySpent = transactionRepository.getMonthlyExpenseTotal(
                    currentMonth.year, 
                    currentMonth.monthValue
                )
                
                val previousMonth = currentMonth.minusMonths(1)
                val previousMonthSpent = transactionRepository.getMonthlyExpenseTotal(
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
        
        // Load income vs expense data
        viewModelScope.launch {
            val currentMonth = _state.value.selectedMonth
            
            try {
                val incomeVsExpenseComparison = analyticsRepository.getIncomeVsExpenseComparison(currentMonth)
                val monthlyIncome = transactionRepository.getMonthlyIncomeTotal(
                    currentMonth.year,
                    currentMonth.monthValue
                )
                
                _state.update { 
                    it.copy(
                        incomeVsExpenseComparison = incomeVsExpenseComparison,
                        monthlyIncomeCents = monthlyIncome
                    ) 
                }
            } catch (e: Exception) {
                // Handle error
                _state.update { 
                    it.copy(
                        incomeVsExpenseComparison = null,
                        monthlyIncomeCents = 0L
                    ) 
                }
            }
        }
    }
    
    fun selectMonth(yearMonth: YearMonth) {
        _state.update { it.copy(selectedMonth = yearMonth) }
        loadDashboardData()
        loadChartData()
        loadAiInsights()
    }
    
    fun refreshData() {
        loadDashboardData()
        loadChartData()
    }
    
    // Chart-related methods
    fun loadChartData() {
        viewModelScope.launch {
            val currentMonth = _state.value.selectedMonth
            
            analyticsRepository.getChartDataForMonth(currentMonth)
                .collect { chartData ->
                    _state.update { it.copy(chartData = chartData) }
                }
        }
    }
    
    fun selectChartType(chartType: ChartType) {
        _state.update { 
            it.copy(selectedChartType = chartType) 
        }
    }
    
    fun toggleChartsExpanded() {
        _state.update { 
            it.copy(isChartsExpanded = !it.isChartsExpanded) 
        }
    }
    
    fun setAnalysisMode(mode: AnalysisMode) {
        _state.update { 
            it.copy(analysisMode = mode) 
        }
        if (mode == AnalysisMode.INCOME) {
            loadIncomeChartData()
        }
    }
    
    private fun loadIncomeChartData() {
        viewModelScope.launch {
            val currentMonth = _state.value.selectedMonth
            
            try {
                val categoryIncome = analyticsRepository.getIncomeSpendingForMonth(currentMonth)
                val incomeVsExpenseTrend = analyticsRepository.getIncomeVsExpenseTrend()
                val incomeVsExpenseComparison = analyticsRepository.getIncomeVsExpenseComparison(currentMonth)
                val totalIncome = categoryIncome.sumOf { it.amountCents }
                
                val incomeChartData = com.example.automaticfinances.data.models.IncomeChartData(
                    categoryIncome = categoryIncome,
                    incomeVsExpenseTrend = incomeVsExpenseTrend,
                    incomeVsExpenseComparison = incomeVsExpenseComparison,
                    selectedMonth = currentMonth,
                    totalIncomeCents = totalIncome,
                    isLoading = false
                )
                
                _state.update { 
                    it.copy(incomeChartData = incomeChartData) 
                }
            } catch (e: Exception) {
                _state.update {
                    it.copy(
                        incomeChartData = com.example.automaticfinances.data.models.IncomeChartData(
                            selectedMonth = currentMonth,
                            isLoading = false,
                            error = "Error al cargar datos de ingresos: ${e.message}"
                        )
                    )
                }
            }
        }
    }

    private companion object {
        private val BOGOTA: ZoneId = ZoneId.of("America/Bogota")
    }
}

enum class AnalysisMode {
    EXPENSES,
    INCOME,
    COMPARISON
}

data class FinancialDashboardState(
    val selectedMonth: YearMonth = YearMonth.now(),
    val budgetStatuses: List<BudgetStatus> = emptyList(),
    val budgetSummary: BudgetSummary? = null,
    val monthlySpentCents: Long = 0L,
    val previousMonthSpentCents: Long? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
    // Chart-related state
    val chartData: ChartData = ChartData(),
    val selectedChartType: ChartType = ChartType.PIE_CATEGORY_SPENDING,
    val isChartsExpanded: Boolean = false,
    // Income data
    val incomeVsExpenseComparison: IncomeVsExpenseComparison? = null,
    val monthlyIncomeCents: Long = 0L,
    // Analysis mode
    val analysisMode: AnalysisMode = AnalysisMode.EXPENSES,
    val incomeChartData: com.example.automaticfinances.data.models.IncomeChartData = com.example.automaticfinances.data.models.IncomeChartData(),
    // AI advisor (DeepSeek → Gemini fallback). Hidden when disabled / nothing to analyze.
    val aiAdvisor: AdvisorUiState = AdvisorUiState.Hidden
)