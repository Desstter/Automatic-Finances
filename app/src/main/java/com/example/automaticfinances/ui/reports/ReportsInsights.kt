package com.example.automaticfinances.ui.reports

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Store
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.automaticfinances.ui.theme.FinanceTheme

// "Análisis inteligente" block of the Reports screen: groups raw insight strings into expandable
// categories. Self-contained so the section's own expand/collapse state stays here and never
// recomposes the rest of the report.

@Composable
internal fun InsightsSection(
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
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Insights,
                            contentDescription = null,
                            modifier = Modifier.size(22.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Análisis inteligente",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
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
    icon: ImageVector,
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
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = color,
                        modifier = Modifier.size(20.dp)
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

@Composable
private fun getCategoryStyle(category: String): Triple<ImageVector, String, Color> {
    val scheme = MaterialTheme.colorScheme
    val finance = FinanceTheme.colors
    return when (category) {
        "overview" -> Triple(Icons.Default.Assessment, "Resumen General", finance.info)
        "patterns" -> Triple(Icons.Default.CalendarMonth, "Patrones de Gasto", scheme.secondary)
        "merchants" -> Triple(Icons.Default.Store, "Comercios y Transacciones", scheme.tertiary)
        "trends" -> Triple(Icons.AutoMirrored.Filled.TrendingUp, "Tendencias", scheme.primary)
        "warnings" -> Triple(Icons.Default.WarningAmber, "Alertas", finance.warning)
        "predictions" -> Triple(Icons.Default.AutoAwesome, "Predicciones", finance.info)
        else -> Triple(Icons.Default.Lightbulb, "Otros Insights", scheme.onSurfaceVariant)
    }
}
