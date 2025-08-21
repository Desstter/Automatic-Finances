package com.example.automaticfinances.ui.reports

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.automaticfinances.data.db.AppDatabase
import com.example.automaticfinances.data.repo.TransactionRepository
import com.example.automaticfinances.data.repo.CategoryRepository
import com.example.automaticfinances.data.repo.BudgetRepository
import com.example.automaticfinances.data.repo.AnalyticsRepository
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportsScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val transactionRepository = remember { TransactionRepository() }
    val categoryRepository = remember { CategoryRepository() }
    val budgetRepository = remember { 
        BudgetRepository(
            budgetDao = AppDatabase.get().budgetDao(),
            transactionDao = AppDatabase.get().transactionDao(),
            categoryDao = AppDatabase.get().categoryDao()
        )
    }
    val analyticsRepository = remember { 
        AnalyticsRepository(
            transactionRepository = transactionRepository,
            budgetRepository = budgetRepository,
            categoryRepository = categoryRepository
        )
    }
    
    val viewModel: ReportsViewModel = viewModel {
        ReportsViewModel(
            transactionRepository = transactionRepository,
            categoryRepository = categoryRepository,
            analyticsRepository = analyticsRepository
        )
    }
    
    val state by viewModel.state.collectAsStateWithLifecycle()
    
    LaunchedEffect(Unit) {
        viewModel.loadReports()
    }
    
    // Error handling with retry
    state.error?.let { error ->
        LaunchedEffect(error) {
            // Auto-dismiss error after showing it briefly
            kotlinx.coroutines.delay(5000)
            viewModel.clearError()
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        text = "Reportes Financieros",
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                },
                actions = {
                    TextButton(onClick = { /* TODO: Export functionality */ }) {
                        Text("Exportar")
                    }
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Time period selector with quick filters
            item {
                TimePeriodSelector(
                    selectedPeriod = state.selectedPeriod,
                    onPeriodChanged = viewModel::selectPeriod
                )
            }
            
            // Error state with retry
            state.error?.let { error ->
                item {
                    ErrorCard(
                        error = error,
                        onRetry = { viewModel.loadReports() },
                        onDismiss = { viewModel.clearError() }
                    )
                }
            }
            
            // Summary cards or empty state
            if (state.isLoading) {
                item {
                    SummarySkeletonLoader()
                }
            } else if (state.summary != null) {
                item {
                    ReportsSummarySection(
                        summary = state.summary,
                        selectedPeriod = state.selectedPeriod
                    )
                }
            } else if (!state.isLoading && state.error == null) {
                item {
                    EmptyReportsCard(
                        onRetry = { viewModel.loadReports() }
                    )
                }
            }
            
            // Category breakdown
            if (state.isLoading) {
                item {
                    CategorySkeletonLoader()
                }
            } else if (state.categoryBreakdown.isNotEmpty()) {
                item {
                    CategoryBreakdownSection(
                        breakdown = state.categoryBreakdown
                    )
                }
            }
            
            // Monthly trends
            if (state.isLoading) {
                item {
                    TrendsSkeletonLoader()
                }
            } else if (state.monthlyTrends.isNotEmpty()) {
                item {
                    MonthlyTrendsSection(
                        trends = state.monthlyTrends
                    )
                }
            }
            
            // Top transactions
            if (state.isLoading) {
                item {
                    TransactionsSkeletonLoader()
                }
            } else if (state.topTransactions.isNotEmpty()) {
                item {
                    TopTransactionsSection(
                        transactions = state.topTransactions
                    )
                }
            }
            
            // Additional insights
            item {
                InsightsSection(
                    insights = state.insights
                )
            }
        }
    }
    
    // Loading overlay for initial load only
    if (state.isLoading && state.summary == null) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                CircularProgressIndicator()
                Text(
                    text = "Cargando reportes...",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun TimePeriodSelector(
    selectedPeriod: ReportPeriod,
    onPeriodChanged: (ReportPeriod) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "Período de análisis",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ReportPeriod.values().forEach { period ->
                    FilterChip(
                        selected = selectedPeriod == period,
                        onClick = { onPeriodChanged(period) },
                        label = { 
                            Text(
                                text = when (period) {
                                    ReportPeriod.CURRENT_MONTH -> "Mes actual"
                                    ReportPeriod.LAST_MONTH -> "Mes pasado"
                                    ReportPeriod.LAST_3_MONTHS -> "3 meses"
                                    ReportPeriod.LAST_6_MONTHS -> "6 meses"
                                    ReportPeriod.CURRENT_YEAR -> "Año actual"
                                }
                            )
                        },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun ReportsSummarySection(
    summary: ReportsSummary?,
    selectedPeriod: ReportPeriod,
    modifier: Modifier = Modifier
) {
    val nf = remember { NumberFormat.getCurrencyInstance(Locale.forLanguageTag("es-CO")) }
    
    summary?.let { 
        Card(
            modifier = modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Resumen del período",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    SummaryItem(
                        label = "Total gastado",
                        value = nf.format(summary.totalSpentCents / 100.0),
                        icon = "💸",
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.weight(1f)
                    )
                    
                    SummaryItem(
                        label = "Total ingresos",
                        value = nf.format(summary.totalIncomeCents / 100.0),
                        icon = "💰",
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.weight(1f)
                    )
                }
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    SummaryItem(
                        label = "Balance neto",
                        value = nf.format(summary.netBalanceCents / 100.0),
                        icon = if (summary.netBalanceCents >= 0) "📈" else "📉",
                        color = if (summary.netBalanceCents >= 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                        modifier = Modifier.weight(1f)
                    )
                    
                    SummaryItem(
                        label = "Promedio diario",
                        value = nf.format(summary.dailyAverageCents / 100.0),
                        icon = "📊",
                        color = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.weight(1f)
                    )
                }
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    SummaryItem(
                        label = "Gastos",
                        value = "${summary.expenseCount}",
                        icon = "📄",
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.weight(1f)
                    )
                    
                    SummaryItem(
                        label = "Ingresos",
                        value = "${summary.incomeCount}",
                        icon = "💵",
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.weight(1f)
                    )
                }
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    SummaryItem(
                        label = "Total transacciones",
                        value = "${summary.transactionCount}",
                        icon = "📊",
                        color = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.weight(1f)
                    )
                    
                    SummaryItem(
                        label = "Categorías activas",
                        value = "${summary.categoriesUsed}",
                        icon = "📂",
                        color = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.weight(1f)
                    )
                }
                
                if (summary.percentageChange != null) {
                    val isIncrease = summary.percentageChange > 0
                    Surface(
                        color = if (isIncrease) 
                            MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
                        else 
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (isIncrease) "📈" else "📉",
                                style = MaterialTheme.typography.titleMedium
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "${if (isIncrease) "+" else ""}${summary.percentageChange.toInt()}% respecto al período anterior",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SummaryItem(
    label: String,
    value: String,
    icon: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = icon,
            style = MaterialTheme.typography.titleMedium
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = color
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun CategoryBreakdownSection(
    breakdown: List<CategoryBreakdown>,
    modifier: Modifier = Modifier
) {
    val nf = remember { NumberFormat.getCurrencyInstance(Locale.forLanguageTag("es-CO")) }
    
    Card(
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "Gastos por categoría",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            breakdown.take(5).forEach { item -> // Show top 5
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = item.categoryIcon,
                            style = MaterialTheme.typography.titleMedium
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = item.categoryName,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = "${item.transactionCount} transacciones",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = nf.format(item.amountCents / 100.0),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${item.percentage.toInt()}%",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                
                if (item != breakdown.last()) {
                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 4.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun MonthlyTrendsSection(
    trends: List<MonthlyTrend>,
    modifier: Modifier = Modifier
) {
    val nf = remember { NumberFormat.getCurrencyInstance(Locale.forLanguageTag("es-CO")) }
    
    Card(
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "Tendencia mensual",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            trends.takeLast(6).forEach { trend -> // Show last 6 months
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = trend.monthName,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                    
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = nf.format(trend.totalCents / 100.0),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold
                        )
                        
                        Spacer(modifier = Modifier.width(8.dp))
                        
                        trend.changePercentage?.let { change ->
                            val isIncrease = change > 0
                            Text(
                                text = if (isIncrease) "↗️" else "↘️",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TopTransactionsSection(
    transactions: List<TopTransaction>,
    modifier: Modifier = Modifier
) {
    val nf = remember { NumberFormat.getCurrencyInstance(Locale.forLanguageTag("es-CO")) }
    val dateFormat = remember { SimpleDateFormat("dd/MM", Locale.forLanguageTag("es-CO")) }
    
    Card(
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "Transacciones más altas",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            transactions.take(5).forEach { transaction ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = transaction.description,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "${dateFormat.format(Date(transaction.timestamp))} • ${transaction.categoryName}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    
                    Text(
                        text = nf.format(transaction.amountCents / 100.0),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

@Composable
private fun InsightsSection(
    insights: List<String>,
    modifier: Modifier = Modifier
) {
    if (insights.isNotEmpty()) {
        val categorizedInsights = remember(insights) { categorizeInsights(insights) }
        var expandedSections by remember(categorizedInsights) { 
            // Initialize with the first category expanded
            val firstCategory = categorizedInsights.keys.firstOrNull()
            mutableStateOf(if (firstCategory != null) setOf(firstCategory) else setOf<String>()) 
        }
        
        Card(
            modifier = modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.3f)
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header with summary
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "🔍 Análisis Inteligente",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Surface(
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                        shape = CircleShape
                    ) {
                        Text(
                            text = "${insights.size}",
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                
                // Categorized insight sections
                categorizedInsights.forEach { (category, categoryInsights) ->
                    if (categoryInsights.isNotEmpty()) {
                        val isExpanded = category in expandedSections
                        val (categoryIcon, categoryTitle, categoryColor) = getCategoryStyle(category)
                        
                        EnhancedInsightCategory(
                            title = categoryTitle,
                            icon = categoryIcon,
                            color = categoryColor,
                            insights = categoryInsights,
                            isExpanded = isExpanded,
                            onToggle = {
                                expandedSections = if (isExpanded) {
                                    expandedSections - category
                                } else {
                                    expandedSections + category
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EnhancedInsightCategory(
    title: String,
    icon: String,
    color: Color,
    insights: List<String>,
    isExpanded: Boolean,
    onToggle: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = color.copy(alpha = 0.1f)
        ),
        shape = RoundedCornerShape(12.dp),
        onClick = onToggle
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Category header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = icon,
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = color
                    )
                    Surface(
                        color = color.copy(alpha = 0.2f),
                        shape = CircleShape
                    ) {
                        Text(
                            text = "${insights.size}",
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = color
                        )
                    }
                }
                
                Icon(
                    imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = if (isExpanded) "Contraer" else "Expandir",
                    tint = color,
                    modifier = Modifier.size(20.dp)
                )
            }
            
            // Expandable content
            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(
                    modifier = Modifier.padding(top = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    insights.forEach { insight ->
                        InsightItem(
                            insight = insight,
                            color = color
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun InsightItem(
    insight: String,
    color: Color
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Surface(
            color = color.copy(alpha = 0.2f),
            shape = CircleShape,
            modifier = Modifier.size(6.dp).align(Alignment.CenterVertically)
        ) {}
        
        Text(
            text = insight,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )
    }
}

private fun categorizeInsights(insights: List<String>): Map<String, List<String>> {
    val categorized = mutableMapOf<String, MutableList<String>>()
    
    insights.forEach { insight ->
        val category = when {
            insight.contains("🏆") || insight.contains("📊") || insight.contains("🎯") -> "overview"
            insight.contains("📅") || insight.contains("⏰") -> "patterns"
            insight.contains("🏪") || insight.contains("💳") -> "merchants"
            insight.contains("📈") || insight.contains("📉") || insight.contains("⬆️") || insight.contains("⬇️") -> "trends"
            insight.contains("💸") || insight.contains("⚠️") -> "warnings"
            insight.contains("🔮") -> "predictions"
            else -> "general"
        }
        
        categorized.getOrPut(category) { mutableListOf() }.add(insight)
    }
    
    return categorized
}

private fun getCategoryStyle(category: String): Triple<String, String, Color> {
    return when (category) {
        "overview" -> Triple("📊", "Resumen General", Color(0xFF2196F3))
        "patterns" -> Triple("📅", "Patrones de Gasto", Color(0xFF4CAF50))
        "merchants" -> Triple("🏪", "Comercios y Transacciones", Color(0xFF9C27B0))
        "trends" -> Triple("📈", "Tendencias", Color(0xFFFF9800))
        "warnings" -> Triple("⚠️", "Alertas", Color(0xFFF44336))
        "predictions" -> Triple("🔮", "Predicciones", Color(0xFF3F51B5))
        else -> Triple("💡", "Otros Insights", Color(0xFF607D8B))
    }
}

@Composable
private fun ErrorCard(
    error: String,
    onRetry: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "⚠️ Error",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
                
                IconButton(onClick = onDismiss) {
                    Text(
                        text = "✕",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }
            
            Text(
                text = error,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onErrorContainer
            )
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = onRetry) {
                    Text(
                        text = "Reintentar",
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyReportsCard(
    onRetry: () -> Unit,
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
                text = "📊",
                style = MaterialTheme.typography.displayMedium
            )
            
            Text(
                text = "No hay datos disponibles",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            
            Text(
                text = "No se encontraron transacciones para el período seleccionado",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Button(onClick = onRetry) {
                Text("Actualizar")
            }
        }
    }
}

@Composable
private fun SummarySkeletonLoader(
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .height(20.dp)
                    .fillMaxWidth(0.4f)
                    .background(
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f),
                        RoundedCornerShape(4.dp)
                    )
            )
            
            repeat(3) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    repeat(2) {
                        Column(
                            modifier = Modifier.weight(1f),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .background(
                                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f),
                                        RoundedCornerShape(12.dp)
                                    )
                            )
                            Box(
                                modifier = Modifier
                                    .height(16.dp)
                                    .fillMaxWidth(0.7f)
                                    .background(
                                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f),
                                        RoundedCornerShape(4.dp)
                                    )
                            )
                            Box(
                                modifier = Modifier
                                    .height(14.dp)
                                    .fillMaxWidth(0.5f)
                                    .background(
                                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f),
                                        RoundedCornerShape(4.dp)
                                    )
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CategorySkeletonLoader(
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .height(20.dp)
                    .fillMaxWidth(0.5f)
                    .background(
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f),
                        RoundedCornerShape(4.dp)
                    )
            )
            
            repeat(5) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .background(
                                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f),
                                    RoundedCornerShape(12.dp)
                                )
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Box(
                                modifier = Modifier
                                    .height(16.dp)
                                    .width(80.dp)
                                    .background(
                                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f),
                                        RoundedCornerShape(4.dp)
                                    )
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Box(
                                modifier = Modifier
                                    .height(12.dp)
                                    .width(60.dp)
                                    .background(
                                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f),
                                        RoundedCornerShape(4.dp)
                                    )
                            )
                        }
                    }
                    
                    Column(horizontalAlignment = Alignment.End) {
                        Box(
                            modifier = Modifier
                                .height(16.dp)
                                .width(70.dp)
                                .background(
                                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f),
                                    RoundedCornerShape(4.dp)
                                )
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Box(
                            modifier = Modifier
                                .height(12.dp)
                                .width(30.dp)
                                .background(
                                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f),
                                    RoundedCornerShape(4.dp)
                                )
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TrendsSkeletonLoader(
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .height(20.dp)
                    .fillMaxWidth(0.4f)
                    .background(
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f),
                        RoundedCornerShape(4.dp)
                    )
            )
            
            repeat(6) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .height(16.dp)
                            .width(80.dp)
                            .background(
                                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f),
                                RoundedCornerShape(4.dp)
                            )
                    )
                    
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .height(16.dp)
                                .width(70.dp)
                                .background(
                                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f),
                                    RoundedCornerShape(4.dp)
                                )
                        )
                        
                        Spacer(modifier = Modifier.width(8.dp))
                        
                        Box(
                            modifier = Modifier
                                .size(16.dp)
                                .background(
                                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f),
                                    RoundedCornerShape(8.dp)
                                )
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TransactionsSkeletonLoader(
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .height(20.dp)
                    .fillMaxWidth(0.6f)
                    .background(
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f),
                        RoundedCornerShape(4.dp)
                    )
            )
            
            repeat(5) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Box(
                            modifier = Modifier
                                .height(16.dp)
                                .fillMaxWidth(0.7f)
                                .background(
                                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f),
                                    RoundedCornerShape(4.dp)
                                )
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Box(
                            modifier = Modifier
                                .height(12.dp)
                                .fillMaxWidth(0.5f)
                                .background(
                                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f),
                                    RoundedCornerShape(4.dp)
                                )
                        )
                    }
                    
                    Box(
                        modifier = Modifier
                            .height(16.dp)
                            .width(70.dp)
                            .background(
                                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f),
                                RoundedCornerShape(4.dp)
                            )
                    )
                }
            }
        }
    }
}