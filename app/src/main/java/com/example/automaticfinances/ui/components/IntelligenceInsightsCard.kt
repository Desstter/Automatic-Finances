package com.example.automaticfinances.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.TrackChanges
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
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
        shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        onClick = onViewSuggestions
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Psychology,
                        contentDescription = null,
                        modifier = Modifier.size(22.dp),
                        tint = MaterialTheme.colorScheme.onTertiaryContainer
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

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                val bestCategory = categoryStats.maxByOrNull { it.accuracy }

                QuickStat(
                    icon = Icons.Default.School,
                    label = "Patrones",
                    value = totalPreferences.toString()
                )
                QuickStat(
                    icon = Icons.Default.TrackChanges,
                    label = "Mejor",
                    value = bestCategory?.let { "${(it.accuracy * 100).toInt()}%" } ?: "-"
                )
                QuickStat(
                    icon = Icons.Default.Bolt,
                    label = "Estado",
                    value = when {
                        overallAccuracy > 0.8f -> "Excelente"
                        overallAccuracy > 0.6f -> "Bueno"
                        overallAccuracy > 0.4f -> "Regular"
                        else                   -> "Aprendiendo"
                    }
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

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
                else            -> MaterialTheme.colorScheme.outline
            }
        )
        LinearProgressIndicator(
            progress = { accuracy },
            modifier = Modifier
                .width(32.dp)
                .height(4.dp),
            color = when {
                accuracy > 0.8f -> MaterialTheme.colorScheme.primary
                accuracy > 0.6f -> MaterialTheme.colorScheme.tertiary
                else            -> MaterialTheme.colorScheme.outline
            },
            trackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
        )
    }
}

@Composable
fun QuickStat(
    icon: ImageVector,
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(18.dp),
            tint = MaterialTheme.colorScheme.onTertiaryContainer
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