package com.example.automaticfinances.ui.components.charts

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import com.example.automaticfinances.data.models.MonthlySpending
import com.example.automaticfinances.ui.theme.FinanceTheme
import java.text.NumberFormat
import java.time.format.DateTimeFormatter
import java.util.*

@Composable
fun SpendingTrendChart(
    monthlySpending: List<MonthlySpending>,
    modifier: Modifier = Modifier,
    onPointClick: (MonthlySpending) -> Unit = {}
) {
    if (monthlySpending.isEmpty()) {
        EmptyTrendChart(modifier = modifier)
        return
    }
    
    val processedData = remember(monthlySpending) {
        monthlySpending.sortedBy { it.yearMonth }
    }
    
    val (minValue, maxValue) = remember(processedData) {
        val amounts = processedData.map { it.totalCents }
        val min = amounts.minOrNull() ?: 0L
        val max = amounts.maxOrNull() ?: 0L
        Pair(min, max)
    }
    
    val chartPoints = remember(processedData, minValue, maxValue) {
        calculateLineChartPoints(processedData, minValue, maxValue)
    }
    
    // Interactive state
    var selectedPointIndex by remember { mutableStateOf(-1) }
    var touchPosition by remember { mutableStateOf(Offset.Zero) }
    var showTooltip by remember { mutableStateOf(false) }
    val haptic = LocalHapticFeedback.current
    
    // Animation state
    val lineAnimation = ChartUtils.rememberChartAnimationState(
        targetValue = 1f,
        animationSpec = tween(
            durationMillis = 1200,
            easing = FastOutSlowInEasing
        )
    )
    
    val pointAnimations = ChartUtils.rememberStaggeredAnimationState(
        itemCount = chartPoints.size,
        staggerDelay = 150,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        )
    )
    
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Chart Header
        TrendChartHeader(
            monthlySpending = processedData,
            modifier = Modifier.fillMaxWidth()
        )
        
        // Line Chart
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(280.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            val primaryColor = MaterialTheme.colorScheme.primary
            val surfaceColor = MaterialTheme.colorScheme.surface
            
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(chartPoints) {
                            detectTapGestures { offset ->
                                val nearestPointIndex = findNearestPoint(offset, chartPoints, Size(size.width.toFloat(), size.height.toFloat()))
                                if (nearestPointIndex != -1) {
                                    selectedPointIndex = nearestPointIndex
                                    touchPosition = offset
                                    showTooltip = true
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    onPointClick(processedData[nearestPointIndex])
                                } else {
                                    showTooltip = false
                                    selectedPointIndex = -1
                                }
                            }
                        }
                ) {
                    drawInteractiveTrendChart(
                        points = chartPoints,
                        canvasSize = size,
                        primaryColor = primaryColor,
                        surfaceColor = surfaceColor,
                        lineAnimation = lineAnimation.value,
                        pointAnimations = pointAnimations.map { it.value },
                        selectedPointIndex = selectedPointIndex
                    )
                }
                
                // Y-axis labels
                YAxisLabels(
                    minValue = minValue,
                    maxValue = maxValue,
                    modifier = Modifier.fillMaxHeight()
                )
                
                // X-axis labels
                XAxisLabels(
                    monthlySpending = processedData,
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                )
                
                // Tooltip
                if (showTooltip && selectedPointIndex >= 0 && selectedPointIndex < processedData.size) {
                    TrendChartTooltip(
                        monthlySpending = processedData[selectedPointIndex],
                        position = touchPosition,
                        onDismiss = { 
                            showTooltip = false
                            selectedPointIndex = -1
                        }
                    )
                }
            }
        }
        
        // Chart Summary
        TrendChartSummary(
            monthlySpending = processedData,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun EmptyTrendChart(modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "📈",
                style = MaterialTheme.typography.displayMedium
            )
            Text(
                text = "Sin datos de tendencia",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = "Necesitas al menos 2 meses de datos",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun TrendChartHeader(
    monthlySpending: List<MonthlySpending>,
    modifier: Modifier = Modifier
) {
    if (monthlySpending.isEmpty()) return
    
    val currentMonth = monthlySpending.lastOrNull()
    val previousMonth = if (monthlySpending.size >= 2) monthlySpending[monthlySpending.size - 2] else null
    val nf = remember { NumberFormat.getCurrencyInstance(Locale("es", "CO")) }
    
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = "Tendencia de Gastos",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = "Últimos ${monthlySpending.size} meses",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        
        if (currentMonth != null && previousMonth != null) {
            val change = currentMonth.totalCents - previousMonth.totalCents
            val percentageChange = if (previousMonth.totalCents == 0L) 0f 
                                 else (change.toFloat() / previousMonth.totalCents.toFloat()) * 100f
            
            val (icon, color) = when {
                change > 0 -> "↗" to MaterialTheme.colorScheme.error
                change < 0 -> "↘" to FinanceTheme.colors.profit
                else -> "→" to MaterialTheme.colorScheme.onSurfaceVariant
            }
            
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(text = icon, style = MaterialTheme.typography.bodyMedium)
                Text(
                    text = "${if (change >= 0) "+" else ""}${percentageChange.toInt()}%",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = color
                )
            }
        }
    }
}

@Composable
private fun YAxisLabels(
    minValue: Long,
    maxValue: Long,
    modifier: Modifier = Modifier
) {
    val nf = remember { NumberFormat.getInstance(Locale("es", "CO")) }
    val steps = 4
    val range = maxValue - minValue
    val stepValue = range / steps
    
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        for (i in steps downTo 0) {
            val value = minValue + (stepValue * i)
            Text(
                text = nf.format(value / 100.0),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 10.sp
            )
        }
    }
}

@Composable
private fun XAxisLabels(
    monthlySpending: List<MonthlySpending>,
    modifier: Modifier = Modifier
) {
    val formatter = DateTimeFormatter.ofPattern("MMM", Locale("es", "CO"))
    
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        monthlySpending.forEach { spending ->
            Text(
                text = spending.yearMonth.format(formatter).uppercase(),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 10.sp
            )
        }
    }
}

@Composable
private fun TrendChartSummary(
    monthlySpending: List<MonthlySpending>,
    modifier: Modifier = Modifier
) {
    val nf = remember { NumberFormat.getCurrencyInstance(Locale("es", "CO")) }
    val average = monthlySpending.map { it.totalCents }.average().toLong()
    val total = monthlySpending.sumOf { it.totalCents }
    val highestMonth = monthlySpending.maxByOrNull { it.totalCents }
    val lowestMonth = monthlySpending.minByOrNull { it.totalCents }
    
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        TrendSummaryItem(
            label = "Promedio",
            value = nf.format(average / 100.0),
            icon = "📊"
        )
        TrendSummaryItem(
            label = "Máximo",
            value = nf.format((highestMonth?.totalCents ?: 0L) / 100.0),
            icon = "📈"
        )
        TrendSummaryItem(
            label = "Mínimo", 
            value = nf.format((lowestMonth?.totalCents ?: 0L) / 100.0),
            icon = "📉"
        )
    }
}

@Composable
private fun TrendSummaryItem(
    label: String,
    value: String,
    icon: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = icon,
            style = MaterialTheme.typography.bodyMedium
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun TrendChartTooltip(
    monthlySpending: MonthlySpending,
    position: Offset,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val nf = remember { NumberFormat.getCurrencyInstance(Locale("es", "CO")) }
    val formatter = DateTimeFormatter.ofPattern("MMMM yyyy", Locale("es", "CO"))
    
    LaunchedEffect(Unit) {
        // Auto-dismiss after 3 seconds
        kotlinx.coroutines.delay(3000)
        onDismiss()
    }
    
    Popup(
        offset = androidx.compose.ui.unit.IntOffset(
            x = (position.x - 100).toInt(), // Center the tooltip
            y = (position.y - 80).toInt()   // Position above the touch point
        ),
        onDismissRequest = onDismiss,
        properties = PopupProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = true
        )
    ) {
        Card(
            modifier = modifier
                .shadow(8.dp, RoundedCornerShape(8.dp))
                .clip(RoundedCornerShape(8.dp)),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            )
        ) {
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = monthlySpending.yearMonth.format(formatter).replaceFirstChar { it.uppercase() },
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Text(
                    text = nf.format(monthlySpending.totalCents / 100.0),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                if (monthlySpending.transactionCount > 0) {
                    Text(
                        text = "${monthlySpending.transactionCount} transacciones",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                    )
                }
            }
        }
    }
}

