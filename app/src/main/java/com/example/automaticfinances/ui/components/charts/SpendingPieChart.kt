package com.example.automaticfinances.ui.components.charts

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
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
fun SpendingPieChart(
    categorySpending: List<CategorySpending>,
    modifier: Modifier = Modifier,
    onSectorClick: (CategorySpending) -> Unit = {},
    showLegend: Boolean = true
) {
    if (categorySpending.isEmpty()) {
        EmptyPieChart(modifier = modifier)
        return
    }
    
    val processedData = remember(categorySpending) {
        processDataForPieChart(categorySpending)
    }
    
    val sectors = remember(processedData) {
        calculatePieChartSectors(processedData)
    }

    // Parse the category hex colors once per data change, never inside the DrawScope (which runs
    // every animation frame). Bad/missing colors fall back to a neutral grey instead of crashing.
    val sectorColors = remember(sectors) {
        sectors.map { ChartUtils.parseHexColor(it.color) }
    }
    val chartColors = ChartUtils.rememberChartColors()

    // Animation states
    val chartAnimation = ChartUtils.rememberChartAnimationState(
        targetValue = 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessMedium
        )
    )
    
    val sectorAnimations = ChartUtils.rememberStaggeredAnimationState(
        itemCount = sectors.size,
        staggerDelay = 100,
        animationSpec = tween(
            durationMillis = 800,
            easing = FastOutSlowInEasing
        )
    )
    
    val rotationAnimation by animateFloatAsState(
        targetValue = 360f,
        animationSpec = tween(
            durationMillis = 2000,
            easing = LinearEasing
        ),
        label = "PieRotation"
    )
    
    var selectedSectorIndex by remember { mutableStateOf(-1) }
    val haptic = LocalHapticFeedback.current

    // Center text with total
    val totalAmount = remember(categorySpending) { categorySpending.sumOf { it.amountCents } }
    val nf = remember { NumberFormat.getCurrencyInstance(Locale("es", "CO")) }

    // One spoken description of the whole chart for TalkBack, since the slices themselves are
    // drawn on a Canvas and are otherwise invisible to accessibility services.
    val chartDescription = remember(processedData, totalAmount) {
        val parts = processedData.joinToString(", ") {
            "${it.category.name} ${it.percentage.toInt()} por ciento"
        }
        "Gastos por categoría. Total ${nf.format(totalAmount / 100.0)}. $parts"
    }

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Pie Chart
        Box(
            modifier = Modifier
                .size(240.dp)
                .align(Alignment.CenterHorizontally)
                .scale(chartAnimation.value)
                .semantics { contentDescription = chartDescription },
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                rotate(rotationAnimation % 360f) {
                    drawAnimatedPieChart(
                        sectors = sectors,
                        sectorColors = sectorColors,
                        sectorAnimations = sectorAnimations.map { it.value },
                        selectedIndex = selectedSectorIndex,
                        canvasSize = size,
                        neutralColor = chartColors.neutral,
                        strokeColor = chartColors.sliceStroke,
                        highlightColor = chartColors.sliceHighlight
                    )
                }
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Total",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = nf.format(totalAmount / 100.0),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
        
        // Legend
        if (showLegend) {
            AnimatedPieChartLegend(
                categorySpending = processedData,
                sectorAnimations = sectorAnimations.map { it.value },
                onItemClick = { index ->
                    selectedSectorIndex = if (selectedSectorIndex == index) -1 else index
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    if (index >= 0 && index < processedData.size) {
                        onSectorClick(processedData[index])
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun EmptyPieChart(modifier: Modifier = Modifier) {
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
                text = "📊",
                style = MaterialTheme.typography.displayMedium
            )
            Text(
                text = "Sin datos de gastos",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = "No hay transacciones en este período",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun PieChartLegend(
    categorySpending: List<CategorySpending>,
    modifier: Modifier = Modifier
) {
    val nf = remember { NumberFormat.getCurrencyInstance(Locale("es", "CO")) }
    
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        categorySpending.forEach { spending ->
            PieChartLegendItem(
                categorySpending = spending,
                formatter = nf
            )
        }
    }
}

@Composable
private fun PieChartLegendItem(
    categorySpending: CategorySpending,
    formatter: NumberFormat,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Color indicator
        val dotColor = remember(categorySpending.category.color) {
            ChartUtils.parseHexColor(categorySpending.category.color)
        }
        Box(
            modifier = Modifier
                .size(16.dp)
                .clip(CircleShape)
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawCircle(
                    color = dotColor,
                    radius = size.minDimension / 2
                )
            }
        }

        // Category icon and name
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = categorySpending.category.icon,
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = categorySpending.category.name,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        // Percentage and amount
        Column(
            horizontalAlignment = Alignment.End
        ) {
            Text(
                text = "${categorySpending.percentage.toInt()}%",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = formatter.format(categorySpending.amountCents / 100.0),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private fun processDataForPieChart(data: List<CategorySpending>): List<CategorySpending> {
    val sortedData = data.sortedByDescending { it.amountCents }
    
    // Group small categories (< 5%) into "Others"
    val threshold = 5f
    val significantCategories = mutableListOf<CategorySpending>()
    var othersAmount = 0L
    
    for (category in sortedData) {
        if (category.percentage >= threshold) {
            significantCategories.add(category)
        } else {
            othersAmount += category.amountCents
        }
    }
    
    // Add "Others" category if needed
    if (othersAmount > 0) {
        val totalAmount = data.sumOf { it.amountCents }
        val othersPercentage = (othersAmount.toFloat() / totalAmount.toFloat()) * 100f
        
        val othersCategory = com.example.automaticfinances.data.db.Category(
            id = -999L,
            name = "Otros",
            color = "#9E9E9E",
            icon = "📦",
            isDefault = false
        )
        
        significantCategories.add(
            CategorySpending(
                categoryId = -999L,
                category = othersCategory,
                amountCents = othersAmount,
                percentage = othersPercentage
            )
        )
    }
    
    return significantCategories
}

private fun calculatePieChartSectors(data: List<CategorySpending>): List<PieChartSector> {
    var currentAngle = -90f // Start from top
    
    return data.map { categorySpending ->
        val sweepAngle = (categorySpending.percentage / 100f) * 360f
        val sector = PieChartSector(
            categorySpending = categorySpending,
            startAngle = currentAngle,
            sweepAngle = sweepAngle,
            color = categorySpending.category.color
        )
        currentAngle += sweepAngle
        sector
    }
}

@Composable
private fun AnimatedPieChartLegend(
    categorySpending: List<CategorySpending>,
    sectorAnimations: List<Float>,
    onItemClick: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val nf = remember { NumberFormat.getCurrencyInstance(Locale("es", "CO")) }

    // The chart sits inside a height-capped container; without an internal scroll the lower legend
    // rows get clipped when there are many categories. Cap the legend height and let it scroll so
    // every category (including the grouped "Otros" bucket) stays reachable.
    Column(
        modifier = modifier
            .heightIn(max = 280.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        categorySpending.forEachIndexed { index, spending ->
            val animationProgress = sectorAnimations.getOrElse(index) { 0f }

            AnimatedPieChartLegendItem(
                categorySpending = spending,
                formatter = nf,
                animationProgress = animationProgress,
                onClick = { onItemClick(index) },
                modifier = Modifier
                    .scale(
                        scaleX = animationProgress,
                        scaleY = animationProgress
                    )
                    .fillMaxWidth()
            )
        }
    }
}

@Composable
private fun AnimatedPieChartLegendItem(
    categorySpending: CategorySpending,
    formatter: NumberFormat,
    animationProgress: Float,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .clickable { onClick() },
        elevation = CardDefaults.cardElevation(
            defaultElevation = (2.dp * animationProgress)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Color indicator with pulse animation
            val dotColor = remember(categorySpending.category.color) {
                ChartUtils.parseHexColor(categorySpending.category.color)
            }
            Box(
                modifier = Modifier
                    .size(16.dp)
                    .clip(CircleShape)
                    .scale(animationProgress)
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    drawCircle(
                        color = dotColor,
                        radius = size.minDimension / 2
                    )
                }
            }
            
            // Category icon and name
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = categorySpending.category.icon,
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = categorySpending.category.name,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            
            // Percentage and amount with slide-in animation
            Column(
                horizontalAlignment = Alignment.End,
                modifier = Modifier.scale(animationProgress)
            ) {
                Text(
                    text = "${(categorySpending.percentage * animationProgress).toInt()}%",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = formatter.format((categorySpending.amountCents * animationProgress).toLong() / 100.0),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

private fun DrawScope.drawAnimatedPieChart(
    sectors: List<PieChartSector>,
    sectorColors: List<Color>,
    sectorAnimations: List<Float>,
    selectedIndex: Int,
    canvasSize: Size,
    neutralColor: Color,
    strokeColor: Color,
    highlightColor: Color
) {
    val baseRadius = (canvasSize.minDimension * 0.4f)
    val center = Offset(canvasSize.width / 2, canvasSize.height / 2)
    val strokeWidth = 4.dp.toPx()

    sectors.forEachIndexed { index, sector ->
        val animationProgress = sectorAnimations.getOrElse(index) { 0f }
        val isSelected = index == selectedIndex

        // Calculate dynamic radius for selection effect
        val radius = baseRadius * (if (isSelected) 1.1f else 1f) * animationProgress
        val color = sectorColors.getOrElse(index) { neutralColor }
        val animatedSweepAngle = sector.sweepAngle * animationProgress

        // Draw pie slice with animation
        if (animatedSweepAngle > 0) {
            drawArc(
                color = color.copy(alpha = 0.9f + (0.1f * animationProgress)),
                startAngle = sector.startAngle,
                sweepAngle = animatedSweepAngle,
                useCenter = true,
                topLeft = Offset(
                    center.x - radius,
                    center.y - radius
                ),
                size = Size(radius * 2, radius * 2)
            )

            // Draw selection highlight
            if (isSelected) {
                drawArc(
                    color = highlightColor.copy(alpha = 0.3f),
                    startAngle = sector.startAngle,
                    sweepAngle = animatedSweepAngle,
                    useCenter = true,
                    topLeft = Offset(
                        center.x - radius,
                        center.y - radius
                    ),
                    size = Size(radius * 2, radius * 2)
                )
            }

            // Draw border — gap between slices, matches the surface behind the chart
            drawArc(
                color = strokeColor.copy(alpha = animationProgress),
                startAngle = sector.startAngle,
                sweepAngle = animatedSweepAngle,
                useCenter = true,
                topLeft = Offset(
                    center.x - radius,
                    center.y - radius
                ),
                size = Size(radius * 2, radius * 2),
                style = Stroke(width = strokeWidth * animationProgress)
            )
        }
    }
}
