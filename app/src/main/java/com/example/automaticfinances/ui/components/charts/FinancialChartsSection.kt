package com.example.automaticfinances.ui.components.charts

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.graphics.Color
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.LinearEasing
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.automaticfinances.data.models.ChartData
import com.example.automaticfinances.data.models.ChartType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FinancialChartsSection(
    chartData: ChartData,
    onChartTypeChanged: (ChartType) -> Unit = {},
    onCategoryClick: (Long) -> Unit = {},
    onBudgetClick: (Long) -> Unit = {},
    modifier: Modifier = Modifier,
    initialExpanded: Boolean = false
) {
    var isExpanded by remember { mutableStateOf(initialExpanded) }
    var selectedChartType by remember { mutableStateOf(ChartType.PIE_CATEGORY_SPENDING) }
    
    Card(
        modifier = modifier
            .fillMaxWidth()
            .semantics {
                contentDescription = "Sección de análisis visual de gastos financieros"
                role = Role.Image
                heading()
            },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            // Header with expand/collapse
            ChartsHeaderSection(
                isExpanded = isExpanded,
                chartData = chartData,
                onExpandedChanged = { isExpanded = it }
            )
            
            // Expandable content
            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically(
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioLowBouncy,
                        stiffness = Spring.StiffnessLow
                    )
                ) + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Chart type tabs
                    ChartTypeTabs(
                        selectedType = selectedChartType,
                        onTypeSelected = { 
                            selectedChartType = it
                            onChartTypeChanged(it)
                        }
                    )
                    
                    // Chart content
                    ChartContentSection(
                        chartType = selectedChartType,
                        chartData = chartData,
                        onCategoryClick = onCategoryClick,
                        onBudgetClick = onBudgetClick
                    )
                }
            }
        }
    }
}

@Composable
private fun ChartsHeaderSection(
    isExpanded: Boolean,
    chartData: ChartData,
    onExpandedChanged: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "📊",
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = "Análisis Visual",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }
            
            if (!isExpanded) {
                ChartsSummaryPreview(chartData = chartData)
            }
        }
        
        IconButton(
            onClick = { onExpandedChanged(!isExpanded) },
            modifier = Modifier.semantics {
                contentDescription = if (isExpanded) 
                    "Contraer sección de gráficos financieros" 
                    else "Expandir sección de gráficos financieros para ver análisis detallado"
                role = Role.Button
            }
        ) {
            Icon(
                imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                contentDescription = null // Description is on the button itself
            )
        }
    }
}

@Composable
private fun ChartsSummaryPreview(
    chartData: ChartData,
    modifier: Modifier = Modifier
) {
    when {
        chartData.isLoading -> {
            Text(
                text = "Cargando datos...",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        chartData.error != null -> {
            Text(
                text = "Error al cargar",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error
            )
        }
        chartData.categorySpending.isNotEmpty() -> {
            val topCategory = chartData.categorySpending.first()
            Text(
                text = "${topCategory.category.icon} ${topCategory.category.name} (${topCategory.percentage.toInt()}%)",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        else -> {
            Text(
                text = "Sin datos para mostrar",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ChartTypeTabs(
    selectedType: ChartType,
    onTypeSelected: (ChartType) -> Unit,
    modifier: Modifier = Modifier
) {
    val tabs = listOf(
        ChartTypeTab(
            type = ChartType.PIE_CATEGORY_SPENDING,
            title = "Distribución",
            icon = Icons.Default.Info,
            description = "Por categoría"
        ),
        ChartTypeTab(
            type = ChartType.LINE_MONTHLY_TREND,
            title = "Tendencia",
            icon = Icons.Default.Info,
            description = "Mensual"
        ),
        ChartTypeTab(
            type = ChartType.BAR_BUDGET_COMPARISON,
            title = "Presupuestos",
            icon = Icons.Default.Info,
            description = "vs. Gastado"
        )
    )
    
    TabRow(
        selectedTabIndex = tabs.indexOfFirst { it.type == selectedType },
        modifier = modifier.semantics {
            contentDescription = "Selector de tipo de gráfico financiero"
            role = Role.Tab
        },
        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
        contentColor = MaterialTheme.colorScheme.onSurface,
        indicator = { tabPositions ->
            if (tabPositions.isNotEmpty()) {
                TabRowDefaults.Indicator(
                    modifier = Modifier
                        .wrapContentSize(Alignment.BottomStart)
                        .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp)),
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    ) {
        tabs.forEach { tab ->
            Tab(
                selected = selectedType == tab.type,
                onClick = { onTypeSelected(tab.type) },
                modifier = Modifier
                    .padding(vertical = 8.dp)
                    .semantics {
                        contentDescription = "${tab.title} - ${tab.description}. " +
                                if (selectedType == tab.type) "Seleccionado" else "No seleccionado"
                        role = Role.Tab
                    }
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = tab.icon,
                        contentDescription = tab.title,
                        tint = if (selectedType == tab.type) 
                               MaterialTheme.colorScheme.primary 
                               else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = tab.title,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (selectedType == tab.type) 
                               MaterialTheme.colorScheme.primary 
                               else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = if (selectedType == tab.type) FontWeight.Medium else FontWeight.Normal
                    )
                    Text(
                        text = tab.description,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
            }
        }
    }
}

@Composable
private fun ChartContentSection(
    chartType: ChartType,
    chartData: ChartData,
    onCategoryClick: (Long) -> Unit,
    onBudgetClick: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 300.dp, max = 600.dp)
            .semantics {
                contentDescription = when (chartType) {
                    ChartType.PIE_CATEGORY_SPENDING -> "Gráfico circular de distribución de gastos por categoría"
                    ChartType.LINE_MONTHLY_TREND -> "Gráfico de líneas mostrando tendencia de gastos mensuales"
                    ChartType.BAR_BUDGET_COMPARISON -> "Gráfico de barras comparando presupuestos vs gastos reales"
                }
                role = Role.Image
            }
    ) {
        when {
            chartData.isLoading -> {
                LoadingChartState(
                    modifier = Modifier.align(Alignment.Center)
                )
            }
            chartData.error != null -> {
                ErrorChartState(
                    error = chartData.error,
                    modifier = Modifier.align(Alignment.Center)
                )
            }
            else -> {
                when (chartType) {
                    ChartType.PIE_CATEGORY_SPENDING -> {
                        SpendingPieChart(
                            categorySpending = chartData.categorySpending,
                            onSectorClick = { categorySpending ->
                                onCategoryClick(categorySpending.categoryId)
                            }
                        )
                    }
                    ChartType.LINE_MONTHLY_TREND -> {
                        SpendingTrendChart(
                            monthlySpending = chartData.monthlyTrend,
                            onPointClick = { /* Handle month click if needed */ }
                        )
                    }
                    ChartType.BAR_BUDGET_COMPARISON -> {
                        BudgetComparisonChart(
                            budgetComparisons = chartData.budgetComparisons,
                            onBudgetClick = { budgetComparison ->
                                onBudgetClick(budgetComparison.budgetStatus.budget.id)
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LoadingChartState(
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "ShimmerTransition")
    val shimmerAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.7f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 1000,
                easing = FastOutSlowInEasing
            ),
            repeatMode = RepeatMode.Reverse
        ),
        label = "ShimmerAlpha"
    )
    
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Shimmer chart placeholder
        ShimmerChartPlaceholder(
            shimmerAlpha = shimmerAlpha,
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
        )
        
        // Shimmer legend placeholders
        repeat(3) { index ->
            ShimmerLegendItem(
                shimmerAlpha = shimmerAlpha,
                delay = index * 100,
                modifier = Modifier.fillMaxWidth()
            )
        }
        
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(16.dp),
                color = MaterialTheme.colorScheme.primary,
                strokeWidth = 2.dp
            )
            Text(
                text = "Analizando datos financieros...",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ShimmerChartPlaceholder(
    shimmerAlpha: Float,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val shimmerColor = Color.Gray.copy(alpha = shimmerAlpha)
            
            // Draw shimmer chart shape (pie chart style)
            drawCircle(
                color = shimmerColor,
                radius = size.minDimension * 0.3f,
                center = center
            )
            
            // Draw shimmer center
            drawCircle(
                color = Color.White.copy(alpha = 0.8f),
                radius = size.minDimension * 0.15f,
                center = center
            )
        }
    }
}

@Composable
private fun ShimmerLegendItem(
    shimmerAlpha: Float,
    delay: Int = 0,
    modifier: Modifier = Modifier
) {
    val delayedAlpha by animateFloatAsState(
        targetValue = shimmerAlpha,
        animationSpec = tween(
            durationMillis = 300,
            delayMillis = delay,
            easing = LinearEasing
        ),
        label = "DelayedShimmer"
    )
    
    Row(
        modifier = modifier.padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Color dot
        Box(
            modifier = Modifier
                .size(16.dp)
                .clip(CircleShape)
                .background(Color.Gray.copy(alpha = delayedAlpha))
        )
        
        // Text placeholders
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.6f)
                    .height(14.dp)
                    .clip(RoundedCornerShape(7.dp))
                    .background(Color.Gray.copy(alpha = delayedAlpha))
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.4f)
                    .height(10.dp)
                    .clip(RoundedCornerShape(5.dp))
                    .background(Color.Gray.copy(alpha = delayedAlpha * 0.7f))
            )
        }
        
        // Amount placeholder
        Box(
            modifier = Modifier
                .width(80.dp)
                .height(16.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Color.Gray.copy(alpha = delayedAlpha))
        )
    }
}

@Composable
private fun ErrorChartState(
    error: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "⚠️",
            style = MaterialTheme.typography.displayMedium
        )
        Text(
            text = "Error al cargar gráficos",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Medium
        )
        Text(
            text = error,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

// Data class for chart type tabs
private data class ChartTypeTab(
    val type: ChartType,
    val title: String,
    val icon: ImageVector,
    val description: String
)