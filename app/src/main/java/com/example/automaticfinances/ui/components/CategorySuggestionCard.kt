package com.example.automaticfinances.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.automaticfinances.data.db.CategorySuggestion

@Composable
fun CategorySuggestionCard(
    suggestion: CategorySuggestion,
    onAccept: () -> Unit,
    onReject: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            // Header con icono de inteligencia
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "🤖",
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Sugerencia Inteligente",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = suggestion.reason,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                    )
                }
                
                // Confidence indicator
                ConfidenceIndicator(confidence = suggestion.confidence)
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Sugerencia de categoría
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = suggestion.categoryIcon,
                    style = MaterialTheme.typography.titleLarge
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "¿Categorizar como:",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                    )
                    Text(
                        text = suggestion.categoryName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Botones de acción
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                // Botón Rechazar
                OutlinedButton(
                    onClick = onReject,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        Icons.Default.Close, 
                        contentDescription = null, 
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("No")
                }
                
                // Botón Aceptar
                Button(
                    onClick = onAccept,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        Icons.Default.Check, 
                        contentDescription = null, 
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Sí")
                }
            }
        }
    }
}

@Composable
fun ConfidenceIndicator(
    confidence: Float,
    modifier: Modifier = Modifier
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
    ) {
        Text(
            text = "${(confidence * 100).toInt()}%",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = when {
                confidence > 0.8f -> MaterialTheme.colorScheme.primary
                confidence > 0.6f -> MaterialTheme.colorScheme.tertiary
                else -> MaterialTheme.colorScheme.outline
            }
        )
        LinearProgressIndicator(
            progress = confidence,
            modifier = Modifier
                .width(24.dp)
                .height(2.dp),
            color = when {
                confidence > 0.8f -> MaterialTheme.colorScheme.primary
                confidence > 0.6f -> MaterialTheme.colorScheme.tertiary
                else -> MaterialTheme.colorScheme.outline
            },
            trackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
        )
    }
}

@Composable
fun BatchSuggestionCard(
    suggestions: List<CategorySuggestion>,
    onAcceptAll: () -> Unit,
    onReviewIndividually: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "🎯",
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Múltiples Sugerencias",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${suggestions.size} transacciones pueden ser categorizadas automáticamente",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Preview de algunas sugerencias
            suggestions.take(3).forEach { suggestion ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(vertical = 2.dp)
                ) {
                    Text(
                        text = suggestion.categoryIcon,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "${suggestion.merchantKey} → ${suggestion.categoryName}",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.weight(1f)
                    )
                    ConfidenceIndicator(confidence = suggestion.confidence)
                }
            }
            
            if (suggestions.size > 3) {
                Text(
                    text = "... y ${suggestions.size - 3} más",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.6f),
                    modifier = Modifier.padding(start = 32.dp)
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedButton(
                    onClick = onReviewIndividually,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Revisar")
                }
                
                Button(
                    onClick = onAcceptAll,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Aceptar Todas")
                }
            }
        }
    }
}