package com.example.automaticfinances.ui.components.charts

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.example.automaticfinances.ui.theme.FinanceTheme
import com.example.automaticfinances.data.models.CategorySpending
import com.example.automaticfinances.data.models.MonthlySpending
import com.example.automaticfinances.data.models.BudgetComparison
import com.example.automaticfinances.data.models.ChartData

/**
 * Performance optimizations for chart components.
 * This file contains memoized calculations and efficient data transformations
 * to prevent unnecessary recompositions and improve animation performance.
 */

object ChartPerformance {
    
    /**
     * Memoized calculation for pie chart data processing.
     * Only recalculates when the input data actually changes.
     */
    @Stable
    data class ProcessedPieData(
        val categorySpending: List<CategorySpending>,
        val totalAmount: Long,
        val hasOthersCategory: Boolean,
        val significantCategoriesCount: Int
    )
    
    @Composable
    fun rememberProcessedPieData(
        categorySpending: List<CategorySpending>,
        threshold: Float = 5f
    ): ProcessedPieData {
        return remember(categorySpending, threshold) {
            val sortedData = categorySpending.sortedByDescending { it.amountCents }
            val totalAmount = sortedData.sumOf { it.amountCents }
            
            val significantCategories = mutableListOf<CategorySpending>()
            var othersAmount = 0L
            var othersCount = 0
            
            for (category in sortedData) {
                if (category.percentage >= threshold) {
                    significantCategories.add(category)
                } else {
                    othersAmount += category.amountCents
                    othersCount++
                }
            }
            
            // Add "Others" category if needed
            val processedData = if (othersAmount > 0) {
                val othersPercentage = (othersAmount.toFloat() / totalAmount.toFloat()) * 100f
                val othersCategory = com.example.automaticfinances.data.db.Category(
                    id = -999L,
                    name = "Otros ($othersCount categorías)",
                    color = "#9E9E9E",
                    icon = "📦",
                    isDefault = false
                )
                
                significantCategories + CategorySpending(
                    categoryId = -999L,
                    category = othersCategory,
                    amountCents = othersAmount,
                    percentage = othersPercentage
                )
            } else {
                significantCategories
            }
            
            ProcessedPieData(
                categorySpending = processedData,
                totalAmount = totalAmount,
                hasOthersCategory = othersAmount > 0,
                significantCategoriesCount = significantCategories.size
            )
        }
    }
    
    /**
     * Memoized calculation for line chart points.
     * Includes optimized coordinate calculations and caching.
     */
    @Stable
    data class ProcessedLineData(
        val monthlySpending: List<MonthlySpending>,
        val minValue: Long,
        val maxValue: Long,
        val range: Long,
        val averageValue: Long,
        val hasVariation: Boolean
    )
    
    @Composable
    fun rememberProcessedLineData(
        monthlySpending: List<MonthlySpending>
    ): ProcessedLineData {
        return remember(monthlySpending) {
            val sortedData = monthlySpending.sortedBy { it.yearMonth }
            val amounts = sortedData.map { it.totalCents }
            
            val min = amounts.minOrNull() ?: 0L
            val max = amounts.maxOrNull() ?: 0L
            val range = max - min
            val average = amounts.average().toLong()
            
            ProcessedLineData(
                monthlySpending = sortedData,
                minValue = min,
                maxValue = max,
                range = range,
                averageValue = average,
                hasVariation = range > 0
            )
        }
    }
    
    /**
     * Memoized color calculations with caching for performance.
     * Prevents repeated color parsing and improves Canvas performance.
     */
    @Stable
    data class ColorCache(
        val primary: Color,
        val primaryArgb: Int,
        val secondary: Color,
        val success: Color,
        val warning: Color,
        val error: Color
    )
    
    @Composable
    fun rememberColorCache(
        primaryColor: Color,
        secondaryColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
        successColor: Color = FinanceTheme.colors.profit,
        warningColor: Color = FinanceTheme.colors.warning,
        errorColor: Color = FinanceTheme.colors.loss
    ): ColorCache {
        return remember(primaryColor, secondaryColor, successColor, warningColor, errorColor) {
            ColorCache(
                primary = primaryColor,
                primaryArgb = primaryColor.toArgb(),
                secondary = secondaryColor,
                success = successColor,
                warning = warningColor,
                error = errorColor
            )
        }
    }
    
    /**
     * Efficient data change detection to prevent unnecessary animations.
     */
    @Composable
    fun rememberDataChangeKey(chartData: ChartData): String {
        return remember(chartData) {
            buildString {
                append("${chartData.categorySpending.size}-")
                append("${chartData.monthlyTrend.size}-")
                append("${chartData.budgetComparisons.size}-")
                append("${chartData.totalSpentCents}-")
                append("${chartData.selectedMonth.monthValue}${chartData.selectedMonth.year}")
            }
        }
    }
    
    /**
     * Optimized animation state management to prevent animation restarts.
     */
    @Stable
    class ChartAnimationState(
        val isAnimating: Boolean,
        val progress: Float,
        val shouldAnimate: Boolean
    )
    
    @Composable
    fun rememberOptimizedAnimationState(
        dataKey: String,
        enabled: Boolean = true
    ): ChartAnimationState {
        var lastDataKey by remember { mutableStateOf(dataKey) }
        var shouldAnimate by remember { mutableStateOf(false) }
        
        // Only trigger animation when data actually changes
        LaunchedEffect(dataKey) {
            if (dataKey != lastDataKey && enabled) {
                shouldAnimate = true
                lastDataKey = dataKey
            }
        }
        
        return remember(shouldAnimate) {
            ChartAnimationState(
                isAnimating = shouldAnimate,
                progress = if (shouldAnimate) 1f else 0f,
                shouldAnimate = shouldAnimate
            )
        }
    }
    
    /**
     * Memory-efficient data sampling for large datasets.
     * Reduces the number of data points while maintaining visual accuracy.
     */
    fun sampleDataPoints(
        data: List<MonthlySpending>,
        maxPoints: Int = 50
    ): List<MonthlySpending> {
        if (data.size <= maxPoints) return data
        
        val step = data.size.toFloat() / maxPoints
        val sampledData = mutableListOf<MonthlySpending>()
        
        for (i in 0 until maxPoints) {
            val index = (i * step).toInt().coerceAtMost(data.size - 1)
            sampledData.add(data[index])
        }
        
        return sampledData
    }
    
    /**
     * Optimized Canvas drawing state to reduce allocations.
     */
    @Stable
    data class CanvasDrawingState(
        val centerX: Float,
        val centerY: Float,
        val chartWidth: Float,
        val chartHeight: Float,
        val radius: Float,
        val padding: Float
    )
    
    @Composable
    fun rememberCanvasState(
        canvasWidth: Float,
        canvasHeight: Float,
        paddingRatio: Float = 0.1f
    ): CanvasDrawingState {
        return remember(canvasWidth, canvasHeight, paddingRatio) {
            val padding = minOf(canvasWidth, canvasHeight) * paddingRatio
            val chartWidth = canvasWidth - (padding * 2)
            val chartHeight = canvasHeight - (padding * 2)
            val radius = minOf(chartWidth, chartHeight) * 0.4f
            
            CanvasDrawingState(
                centerX = canvasWidth / 2,
                centerY = canvasHeight / 2,
                chartWidth = chartWidth,
                chartHeight = chartHeight,
                radius = radius,
                padding = padding
            )
        }
    }
    
    /**
     * Accessibility content generation with caching.
     */
    @Composable
    fun rememberAccessibilityContent(
        chartData: ChartData,
        chartType: String
    ): String {
        return remember(chartData, chartType) {
            when {
                chartData.isLoading -> "Cargando datos del gráfico $chartType"
                chartData.error != null -> "Error en el gráfico $chartType: ${chartData.error}"
                chartData.categorySpending.isEmpty() -> "No hay datos disponibles para el gráfico $chartType"
                else -> {
                    when (chartType) {
                        "pie" -> {
                            val topCategory = chartData.categorySpending.firstOrNull()
                            "Gráfico circular con ${chartData.categorySpending.size} categorías. " +
                            "La categoría principal es ${topCategory?.category?.name} con ${topCategory?.percentage?.toInt()}% del total."
                        }
                        "line" -> {
                            val lastMonth = chartData.monthlyTrend.lastOrNull()
                            "Gráfico de tendencia con ${chartData.monthlyTrend.size} meses de datos. " +
                            "El gasto más reciente fue de ${ChartUtils.formatCurrency(lastMonth?.totalCents ?: 0L)}."
                        }
                        "bar" -> {
                            val overBudget = chartData.budgetComparisons.count { it.isOverBudget }
                            "Gráfico de presupuestos con ${chartData.budgetComparisons.size} categorías. " +
                            "$overBudget categorías han excedido su presupuesto."
                        }
                        else -> "Gráfico financiero con datos del ${chartData.selectedMonth}"
                    }
                }
            }
        }
    }
}