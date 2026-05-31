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
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import com.example.automaticfinances.data.models.LineChartPoint
import com.example.automaticfinances.data.models.MonthlySpending
import com.example.automaticfinances.ui.components.charts.ChartUtils
import com.example.automaticfinances.ui.theme.FinanceTheme
import java.text.NumberFormat
import java.time.format.DateTimeFormatter
import java.util.*
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

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

private fun calculateLineChartPoints(
    data: List<MonthlySpending>,
    minValue: Long,
    maxValue: Long
): List<LineChartPoint> {
    if (data.isEmpty()) return emptyList()
    
    val range = maxValue - minValue
    if (range == 0L) {
        // All values are the same, center them
        return data.mapIndexed { index, spending ->
            LineChartPoint(
                monthlySpending = spending,
                x = index.toFloat() / (data.size - 1).toFloat(),
                y = 0.5f
            )
        }
    }
    
    return data.mapIndexed { index, spending ->
        val x = if (data.size == 1) 0.5f else index.toFloat() / (data.size - 1).toFloat()
        val y = 1f - ((spending.totalCents - minValue).toFloat() / range.toFloat())
        
        LineChartPoint(
            monthlySpending = spending,
            x = x,
            y = y
        )
    }
}

private fun DrawScope.drawTrendChart(
    points: List<LineChartPoint>,
    canvasSize: Size,
    primaryColor: Color,
    surfaceColor: Color
) {
    if (points.size < 2) return
    
    val padding = 32.dp.toPx()
    val chartWidth = canvasSize.width - (padding * 2)
    val chartHeight = canvasSize.height - (padding * 2)
    
    // Convert normalized coordinates to canvas coordinates
    val canvasPoints = points.map { point ->
        Offset(
            x = padding + (point.x * chartWidth),
            y = padding + (point.y * chartHeight)
        )
    }
    
    // Create path for line and gradient fill
    val linePath = Path()
    val fillPath = Path()
    
    // Start the paths
    canvasPoints.forEachIndexed { index, point ->
        if (index == 0) {
            linePath.moveTo(point.x, point.y)
            fillPath.moveTo(point.x, canvasSize.height - padding)
            fillPath.lineTo(point.x, point.y)
        } else {
            linePath.lineTo(point.x, point.y)
            fillPath.lineTo(point.x, point.y)
        }
    }
    
    // Close fill path to bottom
    val lastPoint = canvasPoints.last()
    fillPath.lineTo(lastPoint.x, canvasSize.height - padding)
    fillPath.close()
    
    // Draw gradient fill
    val gradient = Brush.verticalGradient(
        colors = listOf(
            primaryColor.copy(alpha = 0.3f),
            primaryColor.copy(alpha = 0.05f)
        ),
        startY = padding,
        endY = canvasSize.height - padding
    )
    
    drawPath(
        path = fillPath,
        brush = gradient
    )
    
    // Draw grid lines
    drawGridLines(canvasSize, padding, primaryColor.copy(alpha = 0.1f))
    
    // Draw line
    drawPath(
        path = linePath,
        color = primaryColor,
        style = Stroke(
            width = 3.dp.toPx(),
            cap = StrokeCap.Round,
            join = StrokeJoin.Round
        )
    )
    
    // Draw data points
    canvasPoints.forEach { point ->
        drawCircle(
            color = primaryColor,
            radius = 4.dp.toPx(),
            center = point
        )
        drawCircle(
            color = surfaceColor,
            radius = 2.dp.toPx(),
            center = point
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

private fun findNearestPoint(
    touchOffset: Offset,
    chartPoints: List<LineChartPoint>,
    canvasSize: Size
): Int {
    val padding = 32.dp.value // Convert to pixels approximately
    val chartWidth = canvasSize.width - (padding * 2)
    val chartHeight = canvasSize.height - (padding * 2)
    
    var nearestIndex = -1
    var minDistance = Float.MAX_VALUE
    val touchThreshold = 40f // 40 pixels tolerance
    
    chartPoints.forEachIndexed { index, point ->
        val pointX = padding + (point.x * chartWidth)
        val pointY = padding + (point.y * chartHeight)
        
        val distance = sqrt(
            (touchOffset.x - pointX) * (touchOffset.x - pointX) + 
            (touchOffset.y - pointY) * (touchOffset.y - pointY)
        )
        
        if (distance < touchThreshold && distance < minDistance) {
            minDistance = distance
            nearestIndex = index
        }
    }
    
    return nearestIndex
}

private fun DrawScope.drawInteractiveTrendChart(
    points: List<LineChartPoint>,
    canvasSize: Size,
    primaryColor: Color,
    surfaceColor: Color,
    lineAnimation: Float,
    pointAnimations: List<Float>,
    selectedPointIndex: Int
) {
    if (points.size < 2) return
    
    val padding = 32.dp.toPx()
    val chartWidth = canvasSize.width - (padding * 2)
    val chartHeight = canvasSize.height - (padding * 2)
    
    // Convert normalized coordinates to canvas coordinates
    val canvasPoints = points.map { point ->
        Offset(
            x = padding + (point.x * chartWidth),
            y = padding + (point.y * chartHeight)
        )
    }
    
    // Create animated path for line
    val animatedPoints = canvasPoints.take((canvasPoints.size * lineAnimation).toInt().coerceAtLeast(2))
    val linePath = ChartUtils.createSmoothPath(animatedPoints)
    val fillPath = Path().apply {
        if (animatedPoints.isNotEmpty()) {
            moveTo(animatedPoints.first().x, canvasSize.height - padding)
            lineTo(animatedPoints.first().x, animatedPoints.first().y)
            
            // Add smooth curve points
            for (i in 1 until animatedPoints.size) {
                lineTo(animatedPoints[i].x, animatedPoints[i].y)
            }
            
            lineTo(animatedPoints.last().x, canvasSize.height - padding)
            close()
        }
    }
    
    // Draw gradient fill
    val gradient = Brush.verticalGradient(
        colors = listOf(
            primaryColor.copy(alpha = 0.3f),
            primaryColor.copy(alpha = 0.05f)
        ),
        startY = padding,
        endY = canvasSize.height - padding
    )
    
    drawPath(
        path = fillPath,
        brush = gradient
    )
    
    // Draw grid lines
    drawGridLines(canvasSize, padding, primaryColor.copy(alpha = 0.1f))
    
    // Draw line
    drawPath(
        path = linePath,
        color = primaryColor,
        style = Stroke(
            width = 3.dp.toPx(),
            cap = StrokeCap.Round,
            join = StrokeJoin.Round
        )
    )
    
    // Draw data points with animations
    canvasPoints.forEachIndexed { index, point ->
        val animationProgress = pointAnimations.getOrElse(index) { 0f }
        val isSelected = index == selectedPointIndex
        val pointRadius = if (isSelected) 6.dp.toPx() else 4.dp.toPx()
        
        if (animationProgress > 0f) {
            // Draw point shadow for selected
            if (isSelected) {
                drawCircle(
                    color = primaryColor.copy(alpha = 0.3f),
                    radius = pointRadius * 1.5f * animationProgress,
                    center = point
                )
            }
            
            // Draw main point
            drawCircle(
                color = primaryColor,
                radius = pointRadius * animationProgress,
                center = point
            )
            
            // Draw surface-colored center (theme-aware "hole")
            drawCircle(
                color = surfaceColor,
                radius = (pointRadius * 0.5f) * animationProgress,
                center = point
            )
        }
    }
}

private fun DrawScope.drawGridLines(
    canvasSize: Size,
    padding: Float,
    gridColor: Color
) {
    val chartWidth = canvasSize.width - (padding * 2)
    val chartHeight = canvasSize.height - (padding * 2)
    
    // Horizontal grid lines
    for (i in 0..4) {
        val y = padding + (i * chartHeight / 4)
        drawLine(
            color = gridColor,
            start = Offset(padding, y),
            end = Offset(canvasSize.width - padding, y),
            strokeWidth = 1.dp.toPx()
        )
    }
    
    // Vertical grid lines (optional, for monthly divisions)
    for (i in 0..5) {
        val x = padding + (i * chartWidth / 5)
        drawLine(
            color = gridColor,
            start = Offset(x, padding),
            end = Offset(x, canvasSize.height - padding),
            strokeWidth = 1.dp.toPx()
        )
    }
}