package com.example.automaticfinances.ui.components.charts

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.automaticfinances.data.models.MonthlySpending
import com.example.automaticfinances.data.models.LineChartPoint
import com.example.automaticfinances.ui.components.charts.ChartUtils
import java.text.NumberFormat
import java.time.format.DateTimeFormatter
import java.util.*
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun IncomeTrendChart(
    monthlyIncome: List<MonthlySpending>,
    modifier: Modifier = Modifier,
    onPointClick: (MonthlySpending) -> Unit = {}
) {
    if (monthlyIncome.isEmpty()) {
        EmptyIncomeTrendChart(modifier = modifier)
        return
    }
    
    val processedData = remember(monthlyIncome) {
        monthlyIncome.filter { it.isIncome }.sortedBy { it.yearMonth }
    }
    
    if (processedData.isEmpty()) {
        EmptyIncomeTrendChart(modifier = modifier)
        return
    }
    
    val hapticFeedback = LocalHapticFeedback.current
    var selectedPointIndex by remember { mutableIntStateOf(-1) }
    
    // Animation for line chart entrance
    val animationProgress by animateFloatAsState(
        targetValue = 1f,
        animationSpec = tween(
            durationMillis = 1500,
            easing = EaseInOutCubic
        ),
        label = "income_trend_animation"
    )
    
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Chart Header
        IncomeTrendChartHeader(
            monthlyIncome = processedData,
            modifier = Modifier.fillMaxWidth()
        )
        
        // Main Chart Area
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Chart Canvas
                val chartColors = ChartUtils.rememberChartColors()
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(240.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(
                        modifier = Modifier
                            .fillMaxSize()
                            .clickable(
                                indication = null,
                                interactionSource = remember { MutableInteractionSource() }
                            ) {
                                // Click handling can be improved later with gesture detection
                            }
                    ) {
                        drawIncomeTrendChart(
                            monthlyIncome = processedData,
                            progress = animationProgress,
                            lineColor = chartColors.income,
                            gridColor = chartColors.grid,
                            pointStrokeColor = chartColors.sliceStroke,
                            selectedPointIndex = selectedPointIndex
                        )
                    }
                    
                    // Tooltip for selected point
                    if (selectedPointIndex >= 0 && selectedPointIndex < processedData.size) {
                        IncomeTrendTooltip(
                            monthlyIncome = processedData[selectedPointIndex],
                            onDismiss = { selectedPointIndex = -1 },
                            modifier = Modifier.align(Alignment.TopCenter)
                        )
                    }
                }
                
                // Month Labels
                IncomeMonthLabels(
                    monthlyIncome = processedData,
                    modifier = Modifier.fillMaxWidth()
                )
                
                // Summary Stats
                IncomeTrendSummary(
                    monthlyIncome = processedData,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
private fun EmptyIncomeTrendChart(modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(48.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "📈",
                style = MaterialTheme.typography.displayMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Sin datos de tendencia de ingresos",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun IncomeTrendChartHeader(
    monthlyIncome: List<MonthlySpending>,
    modifier: Modifier = Modifier
) {
    if (monthlyIncome.isEmpty()) return
    
    val currentMonth = monthlyIncome.lastOrNull()
    val previousMonth = if (monthlyIncome.size >= 2) monthlyIncome[monthlyIncome.size - 2] else null
    
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
                    text = "Tendencia de Ingresos",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold
                    )
                )
                Text(
                    text = "Últimos ${monthlyIncome.size} meses",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            if (currentMonth != null) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Actual: ${ChartUtils.formatCurrency(currentMonth.totalCents)}",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.Medium
                        )
                    )
                    
                    if (previousMonth != null) {
                        val change = currentMonth.totalCents - previousMonth.totalCents
                        val isPositive = change > 0
                        val changeText = if (isPositive) "+" else ""
                        
                        Text(
                            text = "$changeText${ChartUtils.formatCurrency(change)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (isPositive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun IncomeMonthLabels(
    monthlyIncome: List<MonthlySpending>,
    modifier: Modifier = Modifier
) {
    val formatter = DateTimeFormatter.ofPattern("MMM", Locale("es", "CO"))
    
    LazyRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        items(monthlyIncome) { income ->
            Text(
                text = income.yearMonth.format(formatter).uppercase(),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun IncomeTrendSummary(
    monthlyIncome: List<MonthlySpending>,
    modifier: Modifier = Modifier
) {
    val average = monthlyIncome.map { it.totalCents }.average().toLong()
    val total = monthlyIncome.sumOf { it.totalCents }
    val highestMonth = monthlyIncome.maxByOrNull { it.totalCents }
    val lowestMonth = monthlyIncome.minByOrNull { it.totalCents }
    
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Resumen de Ingresos",
                style = MaterialTheme.typography.titleSmall.copy(
                    fontWeight = FontWeight.Bold
                )
            )
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "Total",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = ChartUtils.formatCurrency(total),
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.Medium
                        )
                    )
                }
                
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "Promedio",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = ChartUtils.formatCurrency(average),
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.Medium
                        )
                    )
                }
            }
            
            if (highestMonth != null && lowestMonth != null) {
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = "Máximo",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = ChartUtils.formatCurrency(highestMonth.totalCents),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "Mínimo",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = ChartUtils.formatCurrency(lowestMonth.totalCents),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }
    }
}

private fun calculateIncomeChartPoints(
    data: List<MonthlySpending>,
    canvasSize: Size
): List<LineChartPoint> {
    if (data.isEmpty()) return emptyList()
    
    val maxValue = data.maxOfOrNull { it.totalCents } ?: 0L
    val minValue = data.minOfOrNull { it.totalCents } ?: 0L
    val range = (maxValue - minValue).toFloat()
    
    if (range == 0f) {
        return data.mapIndexed { index, income ->
            LineChartPoint(
                monthlySpending = income,
                x = (index / (data.size - 1).toFloat()) * canvasSize.width,
                y = canvasSize.height / 2f
            )
        }
    }
    
    return data.mapIndexed { index, income ->
        val x = (index / (data.size - 1).toFloat()) * canvasSize.width
        val y = canvasSize.height - ((income.totalCents - minValue).toFloat() / range * canvasSize.height)
        
        LineChartPoint(
            monthlySpending = income,
            x = x,
            y = y
        )
    }
}

private fun findNearestPoint(clickOffset: Offset, points: List<LineChartPoint>): Int {
    var nearestIndex = -1
    var minDistance = Float.MAX_VALUE
    
    points.forEachIndexed { index, point ->
        val distance = kotlin.math.sqrt(
            (clickOffset.x - point.x) * (clickOffset.x - point.x) +
            (clickOffset.y - point.y) * (clickOffset.y - point.y)
        )
        
        if (distance < minDistance && distance < 50f) { // 50px tolerance
            minDistance = distance
            nearestIndex = index
        }
    }
    
    return nearestIndex
}

private fun DrawScope.drawIncomeTrendChart(
    monthlyIncome: List<MonthlySpending>,
    progress: Float,
    lineColor: Color,
    gridColor: Color,
    pointStrokeColor: Color,
    selectedPointIndex: Int = -1
) {
    if (monthlyIncome.isEmpty()) return
    
    val points = calculateIncomeChartPoints(monthlyIncome, size)
    val progressIndex = (progress * points.size).toInt()
    val visiblePoints = points.take(progressIndex)
    
    if (visiblePoints.size < 2) return
    
    // Draw grid lines
    val padding = ChartUtils.calculateChartPadding(size)
    with(ChartUtils) {
        drawGrid(
            canvasSize = size,
            padding = padding,
            color = gridColor.copy(alpha = 0.4f)
        )
    }

    // Draw income line with gradient
    val path = Path()
    val gradientPath = Path()

    path.moveTo(visiblePoints.first().x, visiblePoints.first().y)
    gradientPath.moveTo(visiblePoints.first().x, size.height)
    gradientPath.lineTo(visiblePoints.first().x, visiblePoints.first().y)

    for (i in 1 until visiblePoints.size) {
        val point = visiblePoints[i]
        path.lineTo(point.x, point.y)
        gradientPath.lineTo(point.x, point.y)
    }

    // Close gradient path
    if (visiblePoints.isNotEmpty()) {
        gradientPath.lineTo(visiblePoints.last().x, size.height)
        gradientPath.close()
    }

    // Draw gradient fill
    drawPath(
        path = gradientPath,
        color = lineColor.copy(alpha = 0.3f)
    )

    // Draw main line
    drawPath(
        path = path,
        color = lineColor,
        style = Stroke(width = 3.dp.toPx())
    )

    // Draw points
    visiblePoints.forEachIndexed { index, point ->
        val isSelected = index == selectedPointIndex
        val pointRadius = if (isSelected) 8.dp.toPx() else 5.dp.toPx()

        // Outer circle (surface-colored background ring)
        drawCircle(
            color = pointStrokeColor,
            radius = pointRadius + 2.dp.toPx(),
            center = Offset(point.x, point.y)
        )

        // Inner circle (colored)
        drawCircle(
            color = lineColor,
            radius = pointRadius,
            center = Offset(point.x, point.y)
        )

        // Selection highlight
        if (isSelected) {
            drawCircle(
                color = lineColor.copy(alpha = 0.3f),
                radius = pointRadius + 8.dp.toPx(),
                center = Offset(point.x, point.y)
            )
        }
    }
}

@Composable
private fun IncomeTrendTooltip(
    monthlyIncome: MonthlySpending,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val formatter = DateTimeFormatter.ofPattern("MMMM yyyy", Locale("es", "CO"))
    val nf = NumberFormat.getCurrencyInstance(Locale("es", "CO"))
    nf.maximumFractionDigits = 0
    
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(3000)
        onDismiss()
    }
    
    Card(
        modifier = modifier
            .padding(8.dp)
            .clip(RoundedCornerShape(8.dp))
            .clickable { onDismiss() },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.inverseSurface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = monthlyIncome.yearMonth.format(formatter).replaceFirstChar { it.uppercase() },
                style = MaterialTheme.typography.bodySmall.copy(
                    fontWeight = FontWeight.Medium
                ),
                color = MaterialTheme.colorScheme.inverseOnSurface
            )
            Text(
                text = nf.format(monthlyIncome.totalCents / 100.0),
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.Bold
                ),
                color = MaterialTheme.colorScheme.inverseOnSurface
            )
            if (monthlyIncome.transactionCount > 0) {
                Text(
                    text = "${monthlyIncome.transactionCount} ingresos",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.inverseOnSurface.copy(alpha = 0.8f)
                )
            }
        }
    }
}