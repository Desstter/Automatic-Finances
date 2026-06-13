package com.example.automaticfinances.ui.insights

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.automaticfinances.data.models.AdvisorUiState
import com.example.automaticfinances.ui.components.*
import com.example.automaticfinances.ui.components.charts.*
import com.example.automaticfinances.ui.components.common.PremiumEmptyState
import com.example.automaticfinances.ui.components.common.SectionHeader
import com.example.automaticfinances.ui.theme.Spacing
import com.example.automaticfinances.data.db.BudgetStatus
import com.example.automaticfinances.data.db.BudgetSummary
import java.text.NumberFormat
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FinancialDashboardScreen(
    viewModel: FinancialDashboardViewModel,
    onNavigateToBudgetManagement: () -> Unit = {},
    onNavigateToGoals: () -> Unit = {},
    onNavigateToReports: () -> Unit = {},
    onNavigateToBudgetDetail: (budgetId: Long) -> Unit = {},
    onNavigateToSettings: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    
    LaunchedEffect(Unit) {
        viewModel.loadDashboardData()
    }
    
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    
    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Análisis financiero",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                scrollBehavior = scrollBehavior
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onNavigateToBudgetManagement,
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("Presupuesto") },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = Spacing.screen),
            contentPadding = PaddingValues(top = Spacing.md, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(Spacing.md)
        ) {
            // Month selector
            item {
                MonthSelector(
                    currentMonth = state.selectedMonth,
                    onMonthChanged = viewModel::selectMonth
                )
            }
            
            // Quick Actions - prioritize user actions
            item {
                QuickActionsGrid(
                    actions = listOf(
                        QuickAction("Presupuestos", Icons.Default.AccountBalanceWallet, onNavigateToBudgetManagement),
                        QuickAction("Metas",        Icons.Default.Flag,                 onNavigateToGoals),
                        QuickAction("Reportes",     Icons.Default.BarChart,             onNavigateToReports)
                    )
                )
            }
            
            // KPIs Row  
            item {
                KPISection(
                    budgetSummary = state.budgetSummary,
                    monthlySpent = state.monthlySpentCents,
                    monthlyIncome = state.monthlyIncomeCents,
                    incomeVsExpenseComparison = state.incomeVsExpenseComparison,
                    previousMonthSpent = state.previousMonthSpentCents,
                    onBudgetClick = onNavigateToBudgetManagement,
                    onSpendingClick = onNavigateToReports
                )
            }
            
            // AI advisor (DeepSeek → Gemini fallback) — narrative analysis on top of the raw KPIs.
            if (state.aiAdvisor !is AdvisorUiState.Hidden) {
                item {
                    AiAdvisorCard(
                        state = state.aiAdvisor,
                        onRefresh = { viewModel.loadAiInsights(force = true) },
                        onRetry = { viewModel.loadAiInsights(force = true) },
                        onOpenSettings = onNavigateToSettings,
                    )
                }
            }

            // Analysis Mode Tabs
            item {
                AnalysisModeTabs(
                    currentMode = state.analysisMode,
                    onModeChanged = viewModel::setAnalysisMode
                )
            }
            
            // Charts Section Based on Analysis Mode
            when (state.analysisMode) {
                AnalysisMode.EXPENSES -> {
                    item {
                        FinancialChartsSection(
                            chartData = state.chartData,
                            onChartTypeChanged = viewModel::selectChartType,
                            onCategoryClick = {},
                            onBudgetClick = {},
                            initialExpanded = state.isChartsExpanded
                        )
                    }
                }
                AnalysisMode.INCOME -> {
                    item {
                        IncomeChartsSection(
                            incomeChartData = state.incomeChartData,
                            onCategoryClick = {}
                        )
                    }
                }
                AnalysisMode.COMPARISON -> {
                    if (state.incomeVsExpenseComparison != null) {
                        item {
                            IncomeVsExpenseChart(
                                comparison = state.incomeVsExpenseComparison!!
                            )
                        }
                    }
                }
            }
            
            // Budget Status Section
            if (state.budgetStatuses.isNotEmpty()) {
                item {
                    BudgetStatusSectionHeader(
                        budgetCount = state.budgetStatuses.size
                    )
                }
                
                items(
                    items = state.budgetStatuses,
                    key = { budgetStatus -> budgetStatus.budget.id }
                ) { budgetStatus ->
                    BudgetStatusCard(
                        budgetStatus = budgetStatus,
                        onClick = { onNavigateToBudgetDetail(budgetStatus.budget.id) }
                    )
                }
            } else {
                item {
                    EmptyBudgetsCard(
                        onCreateBudget = onNavigateToBudgetManagement
                    )
                }
            }
        }
    }
}

@Composable
private fun MonthSelector(
    currentMonth: YearMonth,
    onMonthChanged: (YearMonth) -> Unit,
    modifier: Modifier = Modifier
) {
    val formatter = DateTimeFormatter.ofPattern("MMMM yyyy", Locale.forLanguageTag("es-CO"))

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceContainer
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.sm, vertical = Spacing.xs),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            FilledIconButton(
                onClick = { onMonthChanged(currentMonth.minusMonths(1)) },
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                    contentColor = MaterialTheme.colorScheme.onSurface
                )
            ) {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Mes anterior")
            }

            Text(
                text = currentMonth.format(formatter).replaceFirstChar { it.uppercase() },
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )

            FilledIconButton(
                onClick = { onMonthChanged(currentMonth.plusMonths(1)) },
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                    contentColor = MaterialTheme.colorScheme.onSurface
                )
            ) {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Mes siguiente")
            }
        }
    }
}

@Composable
private fun KPISection(
    budgetSummary: BudgetSummary?,
    monthlySpent: Long,
    monthlyIncome: Long,
    incomeVsExpenseComparison: com.example.automaticfinances.data.models.IncomeVsExpenseComparison?,
    previousMonthSpent: Long?,
    onBudgetClick: () -> Unit,
    onSpendingClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(Spacing.sm)
    ) {
        SectionHeader(title = "Resumen del mes")

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(horizontal = 2.dp)
        ) {
            item {
                SpendingKPICard(
                    monthlySpentCents = monthlySpent,
                    previousMonthSpentCents = previousMonthSpent,
                    onClick = onSpendingClick,
                    modifier = Modifier.width(200.dp)
                )
            }
            
            item {
                IncomeKPICard(
                    monthlyIncomeCents = monthlyIncome,
                    onClick = onSpendingClick,
                    modifier = Modifier.width(200.dp)
                )
            }
            
            incomeVsExpenseComparison?.let { comparison ->
                item {
                    BalanceKPICard(
                        netBalanceCents = comparison.netBalanceCents,
                        hasPositiveBalance = comparison.hasPositiveBalance,
                        savingsRate = if (comparison.totalIncomeCents > 0) 
                            (comparison.netBalanceCents.toFloat() / comparison.totalIncomeCents.toFloat()) * 100f 
                            else 0f,
                        onClick = onSpendingClick,
                        modifier = Modifier.width(200.dp)
                    )
                }
            }
            
            budgetSummary?.let { summary ->
                item {
                    BudgetKPICard(
                        totalBudgetCents = summary.totalBudgetCents,
                        totalSpentCents = summary.totalSpentCents,
                        budgetsCount = summary.budgetsCount,
                        overBudgetCount = summary.overBudgetCount,
                        onClick = onBudgetClick,
                        modifier = Modifier.width(200.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun BudgetStatusSectionHeader(
    budgetCount: Int,
    modifier: Modifier = Modifier
) {
    SectionHeader(
        title = "Estado de presupuestos",
        subtitle = "$budgetCount activos",
        modifier = modifier
    )
}

@Composable
private fun EmptyBudgetsCard(
    onCreateBudget: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceContainer
    ) {
        PremiumEmptyState(
            icon = Icons.Default.AccountBalanceWallet,
            title = "Aún no tienes presupuestos",
            description = "Crea presupuestos para controlar tus gastos y recibir alertas inteligentes.",
            actionLabel = "Crear mi primer presupuesto",
            onAction = onCreateBudget
        )
    }
}


@Composable
private fun AnalysisModeTabs(
    currentMode: AnalysisMode,
    onModeChanged: (AnalysisMode) -> Unit,
    modifier: Modifier = Modifier
) {
    val options = listOf(
        AnalysisMode.EXPENSES to "Gastos",
        AnalysisMode.INCOME to "Ingresos",
        AnalysisMode.COMPARISON to "Comparación"
    )
    SingleChoiceSegmentedButtonRow(modifier = modifier.fillMaxWidth()) {
        options.forEachIndexed { index, (mode, label) ->
            SegmentedButton(
                selected = currentMode == mode,
                onClick = { onModeChanged(mode) },
                shape = SegmentedButtonDefaults.itemShape(index = index, count = options.size),
                colors = SegmentedButtonDefaults.colors(
                    activeContainerColor = MaterialTheme.colorScheme.primaryContainer,
                    activeContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            ) {
                Text(label, style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}

@Composable
private fun IncomeChartsSection(
    incomeChartData: com.example.automaticfinances.data.models.IncomeChartData,
    onCategoryClick: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            SectionHeader(title = "Análisis de ingresos")
            
            if (incomeChartData.isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else if (incomeChartData.error != null) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Text(
                        text = incomeChartData.error!!,
                        modifier = Modifier.padding(16.dp),
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            } else {
                // Income Pie Chart
                if (incomeChartData.categoryIncome.isNotEmpty()) {
                    IncomePieChart(
                        categoryIncome = incomeChartData.categoryIncome,
                        onSectorClick = { spending -> onCategoryClick(spending.categoryId) },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                
                // Income Trend Chart
                if (incomeChartData.incomeVsExpenseTrend.isNotEmpty()) {
                    val incomeTrendData = incomeChartData.incomeVsExpenseTrend.filter { it.isIncome }
                    if (incomeTrendData.isNotEmpty()) {
                        IncomeTrendChart(
                            monthlyIncome = incomeTrendData,
                            onPointClick = { /* Handle point click */ },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun IncomeKPICard(
    monthlyIncomeCents: Long,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val nf = remember { NumberFormat.getCurrencyInstance(Locale("es", "CO")) }
    KPICard(
        title = "Ingresos del Mes",
        currentValue = nf.format(monthlyIncomeCents / 100.0),
        icon = Icons.Default.AccountBalanceWallet,
        trend = null, // Could add trend later
        onClick = onClick,
        modifier = modifier
    )
}

@Composable
private fun BalanceKPICard(
    netBalanceCents: Long,
    hasPositiveBalance: Boolean,
    savingsRate: Float,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val nf = remember { NumberFormat.getCurrencyInstance(Locale("es", "CO")) }
    val trendValue = if (hasPositiveBalance) KPITrend.UP else KPITrend.DOWN
    KPICard(
        title = "Balance Neto",
        currentValue = nf.format(netBalanceCents / 100.0),
        icon = if (hasPositiveBalance) Icons.AutoMirrored.Filled.TrendingUp else Icons.AutoMirrored.Filled.TrendingDown,
        trend = trendValue,
        subtitle = "Tasa de ahorro: ${savingsRate.toInt()}%",
        onClick = onClick,
        modifier = modifier
    )
}