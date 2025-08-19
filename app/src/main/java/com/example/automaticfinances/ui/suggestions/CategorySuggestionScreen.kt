package com.example.automaticfinances.ui.suggestions

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.automaticfinances.ui.components.BatchSuggestionCard
import com.example.automaticfinances.ui.components.CategorySuggestionCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategorySuggestionScreen(
    onNavigateBack: () -> Unit,
    viewModel: CategorySuggestionViewModel = viewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Star,
                            contentDescription = null,
                            modifier = Modifier.size(24.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Sugerencias Inteligentes")
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Volver")
                    }
                }
            )
        }
    ) { padding ->
        
        if (state.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator()
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Analizando transacciones...")
                }
            }
        } else if (state.pendingSuggestions.isEmpty()) {
            EmptySuggestionsState(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                accuracy = state.accuracy,
                processedCount = state.processedSuggestions,
                onRefresh = { viewModel.refreshSuggestions() }
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Stats header
                item {
                    SuggestionsStatsCard(
                        totalSuggestions = state.totalSuggestions,
                        processedSuggestions = state.processedSuggestions,
                        accuracy = state.accuracy
                    )
                }
                
                // Batch suggestion card para alta confianza
                val highConfidenceSuggestions = viewModel.getHighConfidenceSuggestions()
                if (highConfidenceSuggestions.isNotEmpty()) {
                    item {
                        BatchSuggestionCard(
                            suggestions = highConfidenceSuggestions.map { it.suggestion },
                            onAcceptAll = {
                                viewModel.acceptAllSuggestions(highConfidenceSuggestions)
                            },
                            onReviewIndividually = {
                                // Scroll to individual suggestions
                            }
                        )
                    }
                }
                
                // Individual suggestions
                items(state.pendingSuggestions) { suggestionPair ->
                    CategorySuggestionCard(
                        suggestion = suggestionPair.suggestion,
                        onAccept = {
                            viewModel.acceptSuggestion(
                                suggestionPair.transaction.id,
                                suggestionPair.suggestion
                            )
                        },
                        onReject = {
                            viewModel.rejectSuggestion(
                                suggestionPair.transaction.id,
                                suggestionPair.suggestion,
                                null // Por ahora sin categoría alternativa
                            )
                        },
                        onDismiss = {
                            viewModel.rejectSuggestion(
                                suggestionPair.transaction.id,
                                suggestionPair.suggestion,
                                null
                            )
                        }
                    )
                    
                    // Información de la transacción
                    TransactionPreviewCard(transaction = suggestionPair.transaction)
                }
            }
        }
    }
}

@Composable
fun SuggestionsStatsCard(
    totalSuggestions: Int,
    processedSuggestions: Int,
    accuracy: Float,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "📊 Estadísticas de Aprendizaje",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                StatsItem(
                    label = "Pendientes",
                    value = totalSuggestions.toString(),
                    icon = "⏳"
                )
                StatsItem(
                    label = "Procesadas",
                    value = processedSuggestions.toString(),
                    icon = "✅"
                )
                StatsItem(
                    label = "Precisión",
                    value = "${(accuracy * 100).toInt()}%",
                    icon = "🎯"
                )
            }
        }
    }
}

@Composable
fun StatsItem(
    label: String,
    value: String,
    icon: String,
    modifier: Modifier = Modifier
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
    ) {
        Text(
            text = icon,
            style = MaterialTheme.typography.titleMedium
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
        )
    }
}

@Composable
fun TransactionPreviewCard(
    transaction: com.example.automaticfinances.data.repo.TransactionWithCategory,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = transaction.description,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "${transaction.date} • ${transaction.time}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
            
            Text(
                text = "$${transaction.amountCents / 100}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
fun EmptySuggestionsState(
    accuracy: Float,
    processedCount: Int,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "🎉",
            style = MaterialTheme.typography.displayMedium
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = "¡Todo categorizado!",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = "No hay sugerencias pendientes. El sistema está aprendiendo de tus preferencias.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
        )
        
        if (processedCount > 0) {
            Spacer(modifier = Modifier.height(16.dp))
            
            Card {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Procesadas: $processedCount",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = "Precisión: ${(accuracy * 100).toInt()}%",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Button(onClick = onRefresh) {
            Text("Buscar nuevas sugerencias")
        }
    }
}