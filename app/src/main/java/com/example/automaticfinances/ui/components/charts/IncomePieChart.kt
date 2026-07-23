package com.example.automaticfinances.ui.components.charts

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.automaticfinances.data.models.CategorySpending
import com.example.automaticfinances.data.models.PieChartSector
import com.example.automaticfinances.ui.components.charts.ChartUtils
import java.text.NumberFormat
import java.util.*
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun IncomePieChart(
    categoryIncome: List<CategorySpending>,
    modifier: Modifier = Modifier,
    onSectorClick: (CategorySpending) -> Unit = {},
    showLegend: Boolean = true
) {
    if (categoryIncome.isEmpty()) {
        EmptyIncomePieChart(modifier = modifier)
        return
    }
    
    val processedData = remember(categoryIncome) {
        processDataForIncomePieChart(categoryIncome)
    }

    // Compute sectors and parse hex colors once per data change — never inside the DrawScope,
    // which runs on every animation frame.
    val sectors = remember(processedData) { calculateIncomePieChartSectors(processedData) }
    val sectorColors = remember(sectors) { sectors.map { ChartUtils.parseHexColor(it.color) } }
    val totalIncomeAmount = remember(categoryIncome) { categoryIncome.sumOf { it.amountCents } }
    val chartDescription = remember(processedData, totalIncomeAmount) {
        val parts = processedData.joinToString(", ") {
            "${it.category.name} ${it.percentage.toInt()} por ciento"
        }
        "Distribución de ingresos. Total ${ChartUtils.formatCurrency(totalIncomeAmount)}. $parts"
    }

    // Animation for pie chart entrance
    val animationProgress by animateFloatAsState(
        targetValue = 1f,
        animationSpec = tween(
            durationMillis = 1200,
            easing = EaseInOutCubic
        ),
        label = "pie_chart_animation"
    )

    val chartColors = ChartUtils.rememberChartColors()

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Chart Header
        IncomePieChartHeader(
            categoryIncome = processedData,
            modifier = Modifier.fillMaxWidth()
        )
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Pie Chart
            Box(
                modifier = Modifier
                    .size(200.dp)
                    .weight(1f)
                    .semantics { contentDescription = chartDescription },
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    drawIncomePieChart(
                        sectors, sectorColors, animationProgress,
                        neutralColor = chartColors.neutral,
                        strokeColor = chartColors.sliceStroke
                    )
                }

                // Center text with total income
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "💰",
                        style = MaterialTheme.typography.headlineMedium
                    )
                    Text(
                        text = ChartUtils.formatCurrency(totalIncomeAmount),
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold
                        )
                    )
                    Text(
                        text = "Ingresos",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            // Legend
            if (showLegend) {
                IncomePieChartLegend(
                    categoryIncome = processedData,
                    modifier = Modifier.weight(1.2f),
                    onItemClick = onSectorClick
                )
            }
        }
    }
}

@Composable
private fun EmptyIncomePieChart(modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "💰",
                style = MaterialTheme.typography.displayMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Sin datos de ingresos",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun IncomePieChartLegend(
    categoryIncome: List<CategorySpending>,
    modifier: Modifier = Modifier,
    onItemClick: (CategorySpending) -> Unit = {}
) {
    // Cap the height and scroll so all income categories stay reachable inside the height-capped
    // chart container (mirrors SpendingPieChart's legend).
    Column(
        modifier = modifier
            .heightIn(max = 280.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        categoryIncome.forEach { income ->
            IncomePieChartLegendItem(
                categoryIncome = income,
                onClick = { onItemClick(income) }
            )
        }
    }
}

@Composable
private fun IncomePieChartLegendItem(
    categoryIncome: CategorySpending,
    onClick: () -> Unit = {}
) {
    val hapticFeedback = LocalHapticFeedback.current
    val formatter = remember { 
        NumberFormat.getCurrencyInstance(Locale("es", "CO")).apply {
            maximumFractionDigits = 0
        }
    }
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onClick()
            },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Color indicator
            val dotColor = remember(categoryIncome.category.color) {
                ChartUtils.parseHexColor(categoryIncome.category.color)
            }
            Box(
                modifier = Modifier
                    .size(16.dp)
                    .clip(CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    drawCircle(
                        color = dotColor,
                        radius = size.minDimension / 2
                    )
                }
            }
            
            // Category icon
            Text(
                text = categoryIncome.category.icon,
                style = MaterialTheme.typography.titleMedium
            )
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = categoryIncome.category.name,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.Medium
                    )
                )
                
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${categoryIncome.percentage.toInt()}%",
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontWeight = FontWeight.Bold
                        ),
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = formatter.format(categoryIncome.amountCents / 100.0),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

private fun processDataForIncomePieChart(data: List<CategorySpending>): List<CategorySpending> {
    val sortedData = data.sortedByDescending { it.amountCents }
    val total = data.sumOf { it.amountCents }
    
    if (total == 0L) return emptyList()
    
    val significantCategories = mutableListOf<CategorySpending>()
    var othersTotalCents = 0L
    
    for (income in sortedData) {
        val percentage = (income.amountCents.toFloat() / total.toFloat()) * 100f
        if (percentage >= 5f || significantCategories.size < 3) {
            significantCategories.add(income.copy(percentage = percentage))
        } else {
            othersTotalCents += income.amountCents
        }
    }
    
    // Add "Others" category if needed
    if (othersTotalCents > 0) {
        val othersPercentage = (othersTotalCents.toFloat() / total.toFloat()) * 100f
        val othersCategory = com.example.automaticfinances.data.db.Category(
            id = -1L,
            name = "Otros",
            color = "#9E9E9E",
            icon = "💼",
            isDefault = false
        )
        significantCategories.add(
            CategorySpending(
                categoryId = -1L,
                category = othersCategory,
                amountCents = othersTotalCents,
                percentage = othersPercentage
            )
        )
    }
    
    return significantCategories
}

private fun calculateIncomePieChartSectors(data: List<CategorySpending>): List<PieChartSector> {
    var currentAngle = 0f
    
    return data.map { categoryIncome ->
        val sweepAngle = (categoryIncome.percentage / 100f) * 360f
        val sector = PieChartSector(
            categorySpending = categoryIncome,
            startAngle = currentAngle,
            sweepAngle = sweepAngle,
            color = categoryIncome.category.color
        )
        currentAngle += sweepAngle
        sector
    }
}

@Composable
private fun IncomePieChartHeader(
    categoryIncome: List<CategorySpending>,
    modifier: Modifier = Modifier
) {
    if (categoryIncome.isEmpty()) return
    
    val currentMonth = categoryIncome.lastOrNull()
    val totalIncome = categoryIncome.sumOf { it.amountCents }
    val categoriesCount = categoryIncome.size
    
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Distribución de Ingresos",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold
                    )
                )
                Text(
                    text = "$categoriesCount fuentes",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Text(
                text = "Total: ${ChartUtils.formatCurrency(totalIncome)}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private fun DrawScope.drawIncomePieChart(
    sectors: List<PieChartSector>,
    sectorColors: List<Color>,
    progress: Float,
    neutralColor: Color,
    strokeColor: Color
) {
    if (sectors.isEmpty()) return

    val center = Offset(size.width / 2f, size.height / 2f)
    val radius = size.minDimension / 2f * 0.85f

    // Draw sectors with entrance animation
    sectors.forEachIndexed { index, sector ->
        val sectorProgress = ((progress * sectors.size) - index).coerceIn(0f, 1f)
        val animatedSweepAngle = sector.sweepAngle * sectorProgress

        if (animatedSweepAngle > 0f) {
            drawArc(
                color = sectorColors.getOrElse(index) { neutralColor },
                startAngle = sector.startAngle - 90f, // Start from top
                sweepAngle = animatedSweepAngle,
                useCenter = true,
                topLeft = Offset(center.x - radius, center.y - radius),
                size = Size(radius * 2, radius * 2)
            )

            // Draw stroke — gap between slices, matches the surface behind the chart
            drawArc(
                color = strokeColor,
                startAngle = sector.startAngle - 90f,
                sweepAngle = animatedSweepAngle,
                useCenter = true,
                topLeft = Offset(center.x - radius, center.y - radius),
                size = Size(radius * 2, radius * 2),
                style = Stroke(width = 3.dp.toPx())
            )
        }
    }
}