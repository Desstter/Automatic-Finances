package com.example.automaticfinances.ui.components.charts

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.example.automaticfinances.data.models.LineChartPoint
import com.example.automaticfinances.data.models.MonthlySpending
import kotlin.math.sqrt

// Pure geometry + Canvas drawing for SpendingTrendChart, separated from the composables so the
// chart UI file stays focused on layout/state. Nothing here touches Compose state.

/** Maps each month to a normalized [0,1] (x,y) point for the line chart. */
internal fun calculateLineChartPoints(
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

/** Returns the index of the chart point nearest the touch, or -1 if none is within tolerance. */
internal fun findNearestPoint(
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

internal fun DrawScope.drawInteractiveTrendChart(
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
