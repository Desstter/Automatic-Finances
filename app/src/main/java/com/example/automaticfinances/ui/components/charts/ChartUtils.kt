package com.example.automaticfinances.ui.components.charts

import androidx.compose.animation.core.*
import androidx.compose.runtime.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import java.text.NumberFormat
import java.util.*
import kotlin.math.*

object ChartUtils {
    
    // Colombian peso formatter
    val colombianPesoFormatter: NumberFormat = NumberFormat.getCurrencyInstance(Locale("es", "CO"))
    val compactFormatter: NumberFormat = NumberFormat.getCompactNumberInstance(Locale("es", "CO"), NumberFormat.Style.SHORT)
    
    // Common chart colors
    object Colors {
        val safe = Color(0xFF4CAF50)
        val warning = Color(0xFFFFC107)  
        val critical = Color(0xFFFF9800)
        val error = Color(0xFFF44336)
        val neutral = Color(0xFF9E9E9E)
        val grid = Color(0xFF000000).copy(alpha = 0.1f)
    }
    
    // Animation configurations
    object Animations {
        val defaultDuration = 800
        val staggerDelay = 100
        val fastDuration = 400
        val slowDuration = 1200
        
        val bounceEasing = tween<Float>(
            durationMillis = defaultDuration,
            easing = FastOutSlowInEasing
        )
        
        val smoothEasing = tween<Float>(
            durationMillis = defaultDuration,
            easing = CubicBezierEasing(0.4f, 0.0f, 0.2f, 1.0f)
        )
        
        val elasticEasing = keyframes<Float> {
            durationMillis = defaultDuration
            0f at 0 with LinearOutSlowInEasing
            0.8f at (durationMillis * 0.6).toInt() with OvershootInterpolator().toEasing()
            1f at durationMillis
        }
    }
    
    // Format amounts in Colombian pesos
    fun formatCurrency(amountCents: Long): String {
        return colombianPesoFormatter.format(amountCents / 100.0)
    }
    
    fun formatCompactCurrency(amountCents: Long): String {
        return compactFormatter.format(amountCents / 100.0)
    }
    
    // Format percentages
    fun formatPercentage(percentage: Float): String {
        return "${percentage.toInt()}%"
    }
    
    // Color interpolation for gradients
    fun interpolateColor(color1: Color, color2: Color, fraction: Float): Color {
        val clampedFraction = fraction.coerceIn(0f, 1f)
        return Color(
            red = lerp(color1.red, color2.red, clampedFraction),
            green = lerp(color1.green, color2.green, clampedFraction),
            blue = lerp(color1.blue, color2.blue, clampedFraction),
            alpha = lerp(color1.alpha, color2.alpha, clampedFraction)
        )
    }
    
    // Create gradient brush for charts
    fun createVerticalGradient(
        topColor: Color,
        bottomColor: Color,
        startY: Float = 0f,
        endY: Float = 1f
    ): Brush {
        return Brush.verticalGradient(
            colors = listOf(topColor, bottomColor),
            startY = startY,
            endY = endY
        )
    }
    
    // Calculate safe padding for charts
    fun calculateChartPadding(canvasSize: Size): ChartPadding {
        val minDimension = minOf(canvasSize.width, canvasSize.height)
        val basePadding = (minDimension * 0.1f).coerceAtLeast(16f)
        
        return ChartPadding(
            left = basePadding,
            top = basePadding,
            right = basePadding,
            bottom = basePadding * 1.5f // Extra space for labels
        )
    }
    
    // Convert percentage to angle for pie charts
    fun percentageToAngle(percentage: Float): Float {
        return (percentage / 100f) * 360f
    }
    
    // Convert angle to coordinates for pie charts
    fun angleToOffset(
        center: Offset,
        radius: Float,
        angleInDegrees: Float
    ): Offset {
        val angleInRadians = Math.toRadians(angleInDegrees.toDouble())
        return Offset(
            x = center.x + radius * cos(angleInRadians).toFloat(),
            y = center.y + radius * sin(angleInRadians).toFloat()
        )
    }
    
    // Draw smooth path through points
    fun createSmoothPath(points: List<Offset>): Path {
        val path = Path()
        if (points.isEmpty()) return path
        
        if (points.size == 1) {
            path.moveTo(points[0].x, points[0].y)
            return path
        }
        
        path.moveTo(points[0].x, points[0].y)
        
        for (i in 1 until points.size) {
            val current = points[i]
            val previous = points[i - 1]
            
            if (i == 1) {
                // First curve
                val controlPoint = Offset(
                    (previous.x + current.x) / 2,
                    previous.y
                )
                path.quadraticBezierTo(
                    controlPoint.x, controlPoint.y,
                    current.x, current.y
                )
            } else {
                // Smooth curves for subsequent points
                val next = if (i < points.size - 1) points[i + 1] else current
                val cp1 = Offset(
                    previous.x + (current.x - points[i - 2].x) * 0.1f,
                    previous.y + (current.y - points[i - 2].y) * 0.1f
                )
                val cp2 = Offset(
                    current.x - (next.x - previous.x) * 0.1f,
                    current.y - (next.y - previous.y) * 0.1f
                )
                
                path.cubicTo(
                    cp1.x, cp1.y,
                    cp2.x, cp2.y,
                    current.x, current.y
                )
            }
        }
        
        return path
    }
    
    // Draw grid lines
    fun DrawScope.drawGrid(
        canvasSize: Size,
        padding: ChartPadding,
        horizontalLines: Int = 5,
        verticalLines: Int = 5,
        color: Color = Colors.grid,
        strokeWidth: Float = 1f
    ) {
        val chartWidth = canvasSize.width - padding.left - padding.right
        val chartHeight = canvasSize.height - padding.top - padding.bottom
        
        // Horizontal grid lines
        for (i in 0..horizontalLines) {
            val y = padding.top + (i * chartHeight / horizontalLines)
            drawLine(
                color = color,
                start = Offset(padding.left, y),
                end = Offset(canvasSize.width - padding.right, y),
                strokeWidth = strokeWidth
            )
        }
        
        // Vertical grid lines
        for (i in 0..verticalLines) {
            val x = padding.left + (i * chartWidth / verticalLines)
            drawLine(
                color = color,
                start = Offset(x, padding.top),
                end = Offset(x, canvasSize.height - padding.bottom),
                strokeWidth = strokeWidth
            )
        }
    }
    
    // Normalize data points to fit in chart area
    fun normalizeDataPoints(
        values: List<Long>,
        minValue: Long? = null,
        maxValue: Long? = null
    ): List<Float> {
        if (values.isEmpty()) return emptyList()
        
        val min = minValue ?: values.minOrNull() ?: 0L
        val max = maxValue ?: values.maxOrNull() ?: 0L
        val range = max - min
        
        if (range == 0L) {
            return values.map { 0.5f } // Center all points if no variation
        }
        
        return values.map { value ->
            ((value - min).toFloat() / range.toFloat()).coerceIn(0f, 1f)
        }
    }
    
    // Calculate optimal label positions to avoid overlap
    fun calculateLabelPositions(
        points: List<Offset>,
        labelWidths: List<Float>,
        minSpacing: Float = 16f
    ): List<Offset> {
        if (points.isEmpty()) return emptyList()
        
        val adjustedPositions = mutableListOf<Offset>()
        
        points.forEachIndexed { index, point ->
            var adjustedX = point.x
            val labelWidth = labelWidths.getOrElse(index) { 0f }
            
            // Check for overlap with previous labels
            for (i in adjustedPositions.indices) {
                val prevPosition = adjustedPositions[i]
                val prevWidth = labelWidths.getOrElse(i) { 0f }
                
                val overlap = (prevPosition.x + prevWidth / 2 + minSpacing) - (adjustedX - labelWidth / 2)
                if (overlap > 0 && abs(point.y - prevPosition.y) < minSpacing) {
                    adjustedX += overlap
                }
            }
            
            adjustedPositions.add(Offset(adjustedX, point.y))
        }
        
        return adjustedPositions
    }
    
    // Animation state for charts
    @Composable
    fun rememberChartAnimationState(
        targetValue: Float = 1f,
        animationSpec: AnimationSpec<Float> = Animations.smoothEasing
    ): State<Float> {
        return animateFloatAsState(
            targetValue = targetValue,
            animationSpec = animationSpec,
            label = "ChartAnimation"
        )
    }
    
    // Staggered animation for multiple elements
    @Composable
    fun rememberStaggeredAnimationState(
        itemCount: Int,
        staggerDelay: Int = Animations.staggerDelay,
        animationSpec: AnimationSpec<Float> = Animations.smoothEasing
    ): List<State<Float>> {
        return (0 until itemCount).map { index ->
            val delay = index * staggerDelay
            animateFloatAsState(
                targetValue = 1f,
                animationSpec = tween(
                    durationMillis = animationSpec.let { 
                        if (it is TweenSpec) it.durationMillis else Animations.defaultDuration 
                    },
                    delayMillis = delay,
                    easing = FastOutSlowInEasing
                ),
                label = "StaggeredAnimation$index"
            )
        }
    }
    
    // Helper to draw rounded progress bar
    fun DrawScope.drawRoundedProgressBar(
        progress: Float,
        backgroundColor: Color,
        progressColor: Color,
        cornerRadius: Float,
        canvasSize: Size,
        strokeWidth: Float = 0f
    ) {
        // Background
        drawRoundRect(
            color = backgroundColor,
            size = canvasSize,
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(cornerRadius)
        )
        
        // Progress
        val progressWidth = canvasSize.width * progress.coerceIn(0f, 1f)
        if (progressWidth > 0) {
            drawRoundRect(
                color = progressColor,
                size = Size(progressWidth, canvasSize.height),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(cornerRadius)
            )
        }
        
        // Stroke if specified
        if (strokeWidth > 0) {
            drawRoundRect(
                color = progressColor,
                size = canvasSize,
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(cornerRadius),
                style = Stroke(width = strokeWidth)
            )
        }
    }
}

// Data classes for chart utilities
data class ChartPadding(
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float
)

// Extension function for OvershootInterpolator
private fun OvershootInterpolator(tension: Float = 1.0f) = object {
    fun toEasing(): Easing = Easing { fraction ->
        val adjustedFraction = fraction - 1.0f
        adjustedFraction * adjustedFraction * ((tension + 1) * adjustedFraction + tension) + 1.0f
    }
}

// Linear interpolation helper
private fun lerp(start: Float, stop: Float, fraction: Float): Float {
    return start + fraction * (stop - start)
}