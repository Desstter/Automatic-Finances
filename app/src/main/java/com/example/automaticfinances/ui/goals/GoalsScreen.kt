package com.example.automaticfinances.ui.goals

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.automaticfinances.data.db.*
import com.example.automaticfinances.data.repo.CategoryRepository
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.max

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GoalsScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val categoryRepository = remember { CategoryRepository() }
    
    val viewModel: GoalsViewModel = viewModel {
        GoalsViewModel(
            goalDao = AppDatabase.get().financialGoalDao(),
            categoryRepository = categoryRepository
        )
    }
    
    val state by viewModel.state.collectAsStateWithLifecycle()
    var showCreateDialog by remember { mutableStateOf(false) }
    var editingGoal by remember { mutableStateOf<FinancialGoal?>(null) }
    
    LaunchedEffect(Unit) {
        viewModel.loadGoals()
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        text = "Metas Financieras",
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Volver")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showCreateDialog = true }
            ) {
                Icon(Icons.Default.Add, contentDescription = "Crear meta")
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Summary card
            state.summary?.let { summary ->
                item {
                    GoalsSummaryCard(summary = summary)
                }
            }
            
            // Filter tabs
            item {
                GoalsFilterTabs(
                    selectedFilter = state.selectedFilter,
                    onFilterChanged = viewModel::setFilter
                )
            }
            
            // Goals list
            if (state.filteredGoals.isNotEmpty()) {
                items(state.filteredGoals) { goalWithCategory ->
                    GoalCard(
                        goalWithCategory = goalWithCategory,
                        onEdit = { editingGoal = goalWithCategory.toFinancialGoal() },
                        onComplete = { viewModel.markGoalAsCompleted(goalWithCategory.id) },
                        onDelete = { viewModel.deleteGoal(goalWithCategory.id) },
                        onUpdateProgress = { goalId, newAmount ->
                            viewModel.updateGoalProgress(goalId, newAmount)
                        }
                    )
                }
            } else {
                item {
                    EmptyGoalsCard(
                        filter = state.selectedFilter,
                        onCreateGoal = { showCreateDialog = true }
                    )
                }
            }
        }
    }
    
    // Create/Edit Goal Dialog
    if (showCreateDialog || editingGoal != null) {
        GoalDialog(
            goal = editingGoal,
            categories = state.categories,
            onDismiss = { 
                showCreateDialog = false
                editingGoal = null
            },
            onSave = { name, description, targetAmount, targetDate, type, categoryId ->
                if (editingGoal != null) {
                    viewModel.updateGoal(
                        editingGoal!!.copy(
                            name = name,
                            description = description,
                            targetAmountCents = targetAmount,
                            targetDate = targetDate,
                            type = type,
                            categoryId = categoryId
                        )
                    )
                } else {
                    viewModel.createGoal(name, description, targetAmount, targetDate, type, categoryId)
                }
                showCreateDialog = false
                editingGoal = null
            }
        )
    }
    
    // Loading state
    if (state.isLoading) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
    }
    
    // Error handling
    state.error?.let { error ->
        LaunchedEffect(error) {
            // Show snackbar or handle error
        }
    }
}

@Composable
private fun GoalsSummaryCard(
    summary: GoalsSummary,
    modifier: Modifier = Modifier
) {
    val nf = remember { NumberFormat.getCurrencyInstance(Locale("es", "CO")) }
    
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Resumen de Metas",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                GoalsSummaryItem(
                    label = "Activas",
                    value = "${summary.activeGoals}",
                    icon = "🎯"
                )
                
                GoalsSummaryItem(
                    label = "Completadas",
                    value = "${summary.completedGoals}",
                    icon = "✅"
                )
                
                GoalsSummaryItem(
                    label = "Vencidas",
                    value = "${summary.overdueGoals}",
                    icon = "⚠️"
                )
            }
            
            // Progress bar
            val progressPercentage = summary.averageProgress
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Progreso general",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "${progressPercentage.toInt()}%",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                
                Spacer(modifier = Modifier.height(4.dp))
                
                LinearProgressIndicator(
                    progress = { progressPercentage / 100f },
                    modifier = Modifier.fillMaxWidth(),
                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                
                Text(
                    text = "${nf.format(summary.totalCurrentCents / 100.0)} de ${nf.format(summary.totalTargetCents / 100.0)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun GoalsSummaryItem(
    label: String,
    value: String,
    icon: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = icon,
            style = MaterialTheme.typography.titleMedium
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun GoalsFilterTabs(
    selectedFilter: GoalsFilter,
    onFilterChanged: (GoalsFilter) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        GoalsFilter.values().forEach { filter ->
            FilterChip(
                selected = selectedFilter == filter,
                onClick = { onFilterChanged(filter) },
                label = { 
                    Text(
                        text = when (filter) {
                            GoalsFilter.ALL -> "Todas"
                            GoalsFilter.ACTIVE -> "Activas"
                            GoalsFilter.COMPLETED -> "Completadas"
                            GoalsFilter.OVERDUE -> "Vencidas"
                            GoalsFilter.SAVINGS -> "Ahorros"
                            GoalsFilter.EXPENSE_REDUCTION -> "Reducir Gastos"
                        }
                    )
                },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun GoalCard(
    goalWithCategory: GoalWithCategory,
    onEdit: () -> Unit,
    onComplete: () -> Unit,
    onDelete: () -> Unit,
    onUpdateProgress: (Long, Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val nf = remember { NumberFormat.getCurrencyInstance(Locale("es", "CO")) }
    val dateFormat = remember { SimpleDateFormat("dd/MM/yyyy", Locale("es", "CO")) }
    
    val progressPercentage = if (goalWithCategory.targetAmountCents == 0L) 0f 
                            else (goalWithCategory.currentAmountCents.toFloat() / goalWithCategory.targetAmountCents.toFloat()) * 100f
    
    val isOverdue = System.currentTimeMillis() > goalWithCategory.targetDate && !goalWithCategory.isCompleted
    val remainingAmount = max(0, goalWithCategory.targetAmountCents - goalWithCategory.currentAmountCents)
    
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = when {
                goalWithCategory.isCompleted -> MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
                isOverdue -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
                else -> MaterialTheme.colorScheme.surface
            }
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = when (goalWithCategory.type) {
                            GoalType.SAVINGS -> "💰"
                            GoalType.EXPENSE_REDUCTION -> "📉"
                        },
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = goalWithCategory.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        if (goalWithCategory.description.isNotEmpty()) {
                            Text(
                                text = goalWithCategory.description,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                
                // Status badge
                Surface(
                    color = when {
                        goalWithCategory.isCompleted -> Color(0xFF4CAF50).copy(alpha = 0.1f)
                        isOverdue -> MaterialTheme.colorScheme.error.copy(alpha = 0.1f)
                        else -> MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                    },
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = when {
                            goalWithCategory.isCompleted -> "Completada"
                            isOverdue -> "Vencida"
                            else -> "En progreso"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = when {
                            goalWithCategory.isCompleted -> Color(0xFF4CAF50)
                            isOverdue -> MaterialTheme.colorScheme.error
                            else -> MaterialTheme.colorScheme.primary
                        },
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Category if applicable
            goalWithCategory.categoryName?.let { categoryName ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = goalWithCategory.categoryIcon ?: "📂",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = categoryName,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
            }
            
            // Progress
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = nf.format(goalWithCategory.currentAmountCents / 100.0),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = nf.format(goalWithCategory.targetAmountCents / 100.0),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                Spacer(modifier = Modifier.height(4.dp))
                
                LinearProgressIndicator(
                    progress = { (progressPercentage / 100f).coerceIn(0f, 1f) },
                    modifier = Modifier.fillMaxWidth(),
                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "${progressPercentage.toInt()}% completado",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Fecha límite: ${dateFormat.format(Date(goalWithCategory.targetDate))}",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isOverdue) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (!goalWithCategory.isCompleted) {
                    OutlinedButton(
                        onClick = onEdit,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Editar")
                    }
                    
                    if (progressPercentage >= 100f) {
                        Button(
                            onClick = onComplete,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Completar")
                        }
                    }
                }
                
                TextButton(
                    onClick = onDelete,
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Eliminar")
                }
            }
        }
    }
}

@Composable
private fun EmptyGoalsCard(
    filter: GoalsFilter,
    onCreateGoal: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "🎯",
                style = MaterialTheme.typography.displayMedium
            )
            
            Text(
                text = when (filter) {
                    GoalsFilter.ALL -> "No tienes metas financieras"
                    GoalsFilter.ACTIVE -> "No tienes metas activas"
                    GoalsFilter.COMPLETED -> "No has completado ninguna meta"
                    GoalsFilter.OVERDUE -> "No tienes metas vencidas"
                    GoalsFilter.SAVINGS -> "No tienes metas de ahorro"
                    GoalsFilter.EXPENSE_REDUCTION -> "No tienes metas de reducción de gastos"
                },
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            
            Text(
                text = "Crea metas para alcanzar tus objetivos financieros",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            if (filter == GoalsFilter.ALL || filter == GoalsFilter.ACTIVE) {
                Button(onClick = onCreateGoal) {
                    Text("Crear primera meta")
                }
            }
        }
    }
}

// Extension function to convert GoalWithCategory to FinancialGoal
private fun GoalWithCategory.toFinancialGoal(): FinancialGoal {
    return FinancialGoal(
        id = id,
        name = name,
        description = description,
        targetAmountCents = targetAmountCents,
        currentAmountCents = currentAmountCents,
        type = type,
        categoryId = categoryId,
        targetDate = targetDate,
        isCompleted = isCompleted
    )
}