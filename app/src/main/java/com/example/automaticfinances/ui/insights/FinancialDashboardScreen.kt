package com.example.automaticfinances.ui.insights

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.automaticfinances.ui.components.*
import com.example.automaticfinances.ui.components.charts.*
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
                        text = "Financial Insights",
                        fontWeight = FontWeight.Bold
                    )
                },
                scrollBehavior = scrollBehavior
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onNavigateToBudgetManagement
            ) {
                Icon(Icons.Default.Add, contentDescription = "Crear presupuesto")
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
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
                        QuickAction("Presupuestos", "💰", onNavigateToBudgetManagement),
                        QuickAction("Metas", "🎯", onNavigateToGoals),
                        QuickAction("Reportes", "📊", onNavigateToReports)
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
                            onCategoryClick = viewModel::onCategoryClicked,
                            onBudgetClick = viewModel::onBudgetClicked,
                            initialExpanded = state.isChartsExpanded
                        )
                    }
                }
                AnalysisMode.INCOME -> {
                    item {
                        IncomeChartsSection(
                            incomeChartData = state.incomeChartData,
                            onCategoryClick = viewModel::onCategoryClicked
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
    
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(
                onClick = { onMonthChanged(currentMonth.minusMonths(1)) }
            ) {
                Text("← Anterior")
            }
            
            Text(
                text = currentMonth.format(formatter).replaceFirstChar { it.uppercase() },
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            
            TextButton(
                onClick = { onMonthChanged(currentMonth.plusMonths(1)) }
            ) {
                Text("Siguiente →")
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
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "Resumen del Mes",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
        
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(horizontal = 4.dp)
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
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Estado de Presupuestos",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
        
        Text(
            text = "$budgetCount activos",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun EmptyBudgetsCard(
    onCreateBudget: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "💰",
                style = MaterialTheme.typography.displayMedium
            )
            
            Text(
                text = "No tienes presupuestos activos",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            
            Text(
                text = "Crea presupuestos para controlar tus gastos y recibir alertas inteligentes",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Button(
                onClick = onCreateBudget
            ) {
                Text("Crear mi primer presupuesto")
            }
        }
    }
}


@Composable
private fun AnalysisModeTabs(
    currentMode: AnalysisMode,
    onModeChanged: (AnalysisMode) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        TabRow(
            selectedTabIndex = currentMode.ordinal,
            modifier = Modifier.fillMaxWidth()
        ) {
            Tab(
                selected = currentMode == AnalysisMode.EXPENSES,
                onClick = { onModeChanged(AnalysisMode.EXPENSES) },
                text = { Text("💸 Gastos") }
            )
            Tab(
                selected = currentMode == AnalysisMode.INCOME,
                onClick = { onModeChanged(AnalysisMode.INCOME) },
                text = { Text("💰 Ingresos") }
            )
            Tab(
                selected = currentMode == AnalysisMode.COMPARISON,
                onClick = { onModeChanged(AnalysisMode.COMPARISON) },
                text = { Text("⚖️ Comparación") }
            )
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
            Text(
                text = "📊 Análisis de Ingresos",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold
                )
            )
            
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
        icon = "💰",
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
        icon = if (hasPositiveBalance) "📈" else "📉",
        trend = trendValue,
        subtitle = "Tasa de ahorro: ${savingsRate.toInt()}%",
        onClick = onClick,
        modifier = modifier
    )
}