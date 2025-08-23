package com.example.automaticfinances.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BookmarkAdded
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.automaticfinances.data.db.Category
import kotlinx.coroutines.delay
import java.text.NumberFormat

data class FilterSummary(
    val categoryName: String? = null,
    val categoryIcon: String? = null,
    val dateRange: String? = null,
    val amountRange: String? = null,
    val searchQuery: String? = null,
    val totalFilters: Int = 0,
    val resultCount: Int = 0
)

data class SavedFilter(
    val id: String,
    val name: String,
    val description: String,
    val categoryId: Long?,
    val dateStart: String?,
    val dateEnd: String?,
    val minAmount: Long?,
    val maxAmount: Long?,
    val searchQuery: String?,
    val isFavorite: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val usageCount: Int = 0
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilterPreviewCard(
    summary: FilterSummary,
    onClearFilter: () -> Unit,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = summary.totalFilters > 0,
        enter = expandHorizontally(
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessLow
            )
        ) + fadeIn(),
        exit = shrinkHorizontally(
            animationSpec = tween(300)
        ) + fadeOut()
    ) {
        Card(
            modifier = modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier.padding(12.dp)
            ) {
                // Header row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.FilterList,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "${summary.totalFilters} filtro${if (summary.totalFilters > 1) "s" else ""} activo${if (summary.totalFilters > 1) "s" else ""}",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                    
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            color = MaterialTheme.colorScheme.primary,
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = "${summary.resultCount} resultados",
                                style = MaterialTheme.typography.labelSmall,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        }
                        
                        Spacer(modifier = Modifier.width(8.dp))
                        
                        IconButton(
                            onClick = onClearFilter,
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                Icons.Default.Clear,
                                contentDescription = "Limpiar filtros",
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }
                
                // Filter details
                if (summary.categoryName != null || summary.dateRange != null || 
                    summary.amountRange != null || summary.searchQuery != null) {
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        summary.categoryName?.let { categoryName ->
                            item {
                                FilterDetailChip(
                                    label = "${summary.categoryIcon ?: "📂"} $categoryName",
                                    color = MaterialTheme.colorScheme.secondary
                                )
                            }
                        }
                        
                        summary.dateRange?.let { range ->
                            item {
                                FilterDetailChip(
                                    label = "📅 $range",
                                    color = MaterialTheme.colorScheme.tertiary
                                )
                            }
                        }
                        
                        summary.amountRange?.let { range ->
                            item {
                                FilterDetailChip(
                                    label = "💰 $range",
                                    color = MaterialTheme.colorScheme.secondary
                                )
                            }
                        }
                        
                        summary.searchQuery?.let { query ->
                            if (query.isNotBlank()) {
                                item {
                                    FilterDetailChip(
                                        label = "🔍 \"$query\"",
                                        color = MaterialTheme.colorScheme.tertiary
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FilterDetailChip(
    label: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        color = color.copy(alpha = 0.2f),
        shape = RoundedCornerShape(8.dp),
        modifier = modifier
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            color = color,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SavedFiltersSection(
    savedFilters: List<SavedFilter>,
    onApplyFilter: (SavedFilter) -> Unit,
    onDeleteFilter: (SavedFilter) -> Unit,
    onToggleFavorite: (SavedFilter) -> Unit,
    modifier: Modifier = Modifier
) {
    if (savedFilters.isEmpty()) return
    
    Column(modifier = modifier) {
        Text(
            text = "Filtros Guardados",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(savedFilters) { filter ->
                SavedFilterCard(
                    filter = filter,
                    onApply = { onApplyFilter(filter) },
                    onDelete = { onDeleteFilter(filter) },
                    onToggleFavorite = { onToggleFavorite(filter) }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SavedFilterCard(
    filter: SavedFilter,
    onApply: () -> Unit,
    onDelete: () -> Unit,
    onToggleFavorite: () -> Unit
) {
    var showDeleteConfirmation by remember { mutableStateOf(false) }
    
    Card(
        onClick = onApply,
        modifier = Modifier.width(200.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (filter.isFavorite) {
                MaterialTheme.colorScheme.secondaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            }
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            // Header with name and favorite toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = filter.name,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                
                IconButton(
                    onClick = onToggleFavorite,
                    modifier = Modifier.size(20.dp)
                ) {
                    Icon(
                        if (filter.isFavorite) Icons.Default.BookmarkAdded else Icons.Default.BookmarkBorder,
                        contentDescription = if (filter.isFavorite) "Quitar de favoritos" else "Agregar a favoritos",
                        modifier = Modifier.size(16.dp),
                        tint = if (filter.isFavorite) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            // Description
            if (filter.description.isNotBlank()) {
                Text(
                    text = filter.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(vertical = 4.dp)
                )
            }
            
            // Usage stats
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Usado ${filter.usageCount} ${if (filter.usageCount == 1) "vez" else "veces"}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                IconButton(
                    onClick = { showDeleteConfirmation = true },
                    modifier = Modifier.size(20.dp)
                ) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Eliminar filtro",
                        modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
    
    // Delete confirmation dialog
    if (showDeleteConfirmation) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmation = false },
            title = { Text("Eliminar filtro") },
            text = { Text("¿Estás seguro de que quieres eliminar el filtro \"${filter.name}\"?") },
            confirmButton = {
                Button(
                    onClick = {
                        onDelete()
                        showDeleteConfirmation = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Eliminar")
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { showDeleteConfirmation = false }
                ) {
                    Text("Cancelar")
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SaveFilterDialog(
    isOpen: Boolean,
    onDismiss: () -> Unit,
    onSave: (name: String, description: String, isFavorite: Boolean) -> Unit
) {
    var filterName by remember { mutableStateOf("") }
    var filterDescription by remember { mutableStateOf("") }
    var isFavorite by remember { mutableStateOf(false) }
    
    if (isOpen) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { 
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Save,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Guardar Filtro")
                }
            },
            text = {
                Column {
                    OutlinedTextField(
                        value = filterName,
                        onValueChange = { filterName = it },
                        label = { Text("Nombre del filtro") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    OutlinedTextField(
                        value = filterDescription,
                        onValueChange = { filterDescription = it },
                        label = { Text("Descripción (opcional)") },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 2
                    )
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = isFavorite,
                            onCheckedChange = { isFavorite = it }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Marcar como favorito",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (filterName.isNotBlank()) {
                            onSave(filterName.trim(), filterDescription.trim(), isFavorite)
                            filterName = ""
                            filterDescription = ""
                            isFavorite = false
                        }
                    },
                    enabled = filterName.isNotBlank()
                ) {
                    Text("Guardar")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = onDismiss) {
                    Text("Cancelar")
                }
            }
        )
    }
}

@Composable
fun FilterStatistics(
    totalTransactions: Int,
    filteredTransactions: Int,
    categories: List<Category>,
    modifier: Modifier = Modifier
) {
    val filterPercentage = if (totalTransactions > 0) {
        (filteredTransactions.toFloat() / totalTransactions * 100).toInt()
    } else 0
    
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            Text(
                text = "Estadísticas de Filtros",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatisticItem(
                    label = "Total",
                    value = totalTransactions.toString(),
                    color = MaterialTheme.colorScheme.primary
                )
                
                StatisticItem(
                    label = "Filtrados",
                    value = filteredTransactions.toString(),
                    color = MaterialTheme.colorScheme.secondary
                )
                
                StatisticItem(
                    label = "Mostrados",
                    value = "$filterPercentage%",
                    color = MaterialTheme.colorScheme.tertiary
                )
            }
        }
    }
}

@Composable
private fun StatisticItem(
    label: String,
    value: String,
    color: Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = color
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun QuickFilterActions(
    onSaveCurrentFilter: () -> Unit,
    onShowFilterHistory: () -> Unit,
    hasActiveFilters: Boolean,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        OutlinedButton(
            onClick = onSaveCurrentFilter,
            enabled = hasActiveFilters,
            modifier = Modifier.weight(1f)
        ) {
            Icon(
                Icons.Default.Save,
                contentDescription = null,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text("Guardar", maxLines = 1)
        }
        
        OutlinedButton(
            onClick = onShowFilterHistory,
            modifier = Modifier.weight(1f)
        ) {
            Icon(
                Icons.Default.Star,
                contentDescription = null,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text("Guardados", maxLines = 1)
        }
    }
}