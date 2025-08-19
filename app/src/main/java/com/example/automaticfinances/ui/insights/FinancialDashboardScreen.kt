package com.example.automaticfinances.ui.insights

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.automaticfinances.ui.components.*
import com.example.automaticfinances.data.db.BudgetStatus
import com.example.automaticfinances.data.db.BudgetSummary
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
    modifier: Modifier = Modifier
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    
    LaunchedEffect(Unit) {
        viewModel.loadDashboardData()
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        text = "Financial Insights",
                        fontWeight = FontWeight.Bold
                    )
                },
                actions = {
                    IconButton(onClick = onNavigateToBudgetManagement) {
                        Icon(Icons.Default.Settings, contentDescription = "Configurar presupuestos")
                    }
                }
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
                .padding(paddingValues),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Month selector
            item {
                MonthSelector(
                    currentMonth = state.selectedMonth,
                    onMonthChanged = viewModel::selectMonth
                )
            }
            
            // KPIs Row
            item {
                KPISection(
                    budgetSummary = state.budgetSummary,
                    monthlySpent = state.monthlySpentCents,
                    previousMonthSpent = state.previousMonthSpentCents,
                    onBudgetClick = onNavigateToBudgetManagement,
                    onSpendingClick = onNavigateToReports
                )
            }
            
            // Budget Status Section
            if (state.budgetStatuses.isNotEmpty()) {
                item {
                    BudgetStatusSection(
                        budgetStatuses = state.budgetStatuses,
                        onBudgetClick = { /* Navigate to budget detail */ }
                    )
                }
            } else {
                item {
                    EmptyBudgetsCard(
                        onCreateBudget = onNavigateToBudgetManagement
                    )
                }
            }
            
            // Quick Actions
            item {
                QuickActionsSection(
                    onManageBudgets = onNavigateToBudgetManagement,
                    onViewGoals = onNavigateToGoals,
                    onViewReports = onNavigateToReports
                )
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
    val formatter = DateTimeFormatter.ofPattern("MMMM yyyy", Locale("es", "CO"))
    
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
private fun BudgetStatusSection(
    budgetStatuses: List<BudgetStatus>,
    onBudgetClick: (BudgetStatus) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Estado de Presupuestos",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            
            Text(
                text = "${budgetStatuses.size} activos",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        
        BudgetStatusList(
            budgetStatuses = budgetStatuses,
            onBudgetClick = onBudgetClick
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
private fun QuickActionsSection(
    onManageBudgets: () -> Unit,
    onViewGoals: () -> Unit,
    onViewReports: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "Acciones Rápidas",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            QuickActionCard(
                title = "Presupuestos",
                icon = "💰",
                onClick = onManageBudgets,
                modifier = Modifier.weight(1f)
            )
            
            QuickActionCard(
                title = "Metas",
                icon = "🎯",
                onClick = onViewGoals,
                modifier = Modifier.weight(1f)
            )
            
            QuickActionCard(
                title = "Reportes",
                icon = "📊",
                onClick = onViewReports,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun QuickActionCard(
    title: String,
    icon: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        onClick = onClick,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = icon,
                style = MaterialTheme.typography.headlineMedium
            )
            
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Medium
            )
        }
    }
}