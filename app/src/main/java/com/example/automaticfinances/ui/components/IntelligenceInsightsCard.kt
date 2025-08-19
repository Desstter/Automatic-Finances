package com.example.automaticfinances.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.automaticfinances.data.db.CategoryAccuracy

@Composable
fun IntelligenceInsightsCard(
    totalPreferences: Int,
    overallAccuracy: Float,
    categoryStats: List<CategoryAccuracy>,
    onViewSuggestions: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (totalPreferences == 0) return
    
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer
        ),
        onClick = onViewSuggestions
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "🧠",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "Inteligencia Activa",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "Aprendiendo de tus patrones",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.7f)
                        )
                    }
                }
                
                AccuracyIndicator(accuracy = overallAccuracy)
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Stats preview
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                QuickStat(
                    icon = "📚",
                    label = "Patrones",
                    value = totalPreferences.toString()
                )
                
                val bestCategory = categoryStats.maxByOrNull { it.accuracy }
                QuickStat(
                    icon = bestCategory?.let { "🎯" } ?: "📊",
                    label = "Mejor",
                    value = bestCategory?.let { "${(it.accuracy * 100).toInt()}%" } ?: "-"
                )
                
                QuickStat(
                    icon = "⚡",
                    label = "Estado",
                    value = when {
                        overallAccuracy > 0.8f -> "Excelente"
                        overallAccuracy > 0.6f -> "Bueno"
                        overallAccuracy > 0.4f -> "Regular"
                        else -> "Aprendiendo"
                    }
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Action hint
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    Icons.Default.Info,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "Toca para ver sugerencias y estadísticas",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
fun AccuracyIndicator(
    accuracy: Float,
    modifier: Modifier = Modifier
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
    ) {
        Text(
            text = "${(accuracy * 100).toInt()}%",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = when {
                accuracy > 0.8f -> MaterialTheme.colorScheme.primary
                accuracy > 0.6f -> MaterialTheme.colorScheme.tertiary
                else -> MaterialTheme.colorScheme.outline
            }
        )
        LinearProgressIndicator(
            progress = accuracy,
            modifier = Modifier
                .width(32.dp)
                .height(3.dp),
            color = when {
                accuracy > 0.8f -> MaterialTheme.colorScheme.primary
                accuracy > 0.6f -> MaterialTheme.colorScheme.tertiary
                else -> MaterialTheme.colorScheme.outline
            },
            trackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
        )
    }
}

@Composable
fun QuickStat(
    icon: String,
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
    ) {
        Text(
            text = icon,
            style = MaterialTheme.typography.bodyMedium
        )
        Text(
            text = value,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.7f)
        )
    }
}

@Composable
fun LearningProgressCard(
    recentLearnings: Int,
    improvementTrend: Float,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = if (improvementTrend > 0) "📈" else "📊",
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = "Aprendizaje Reciente",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "$recentLearnings nuevos patrones",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                }
            }
            
            if (improvementTrend != 0f) {
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = if (improvementTrend > 0) "+${(improvementTrend * 100).toInt()}%" else "${(improvementTrend * 100).toInt()}%",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (improvementTrend > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                    )
                    Text(
                        text = "vs anterior",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
            }
        }
    }
}