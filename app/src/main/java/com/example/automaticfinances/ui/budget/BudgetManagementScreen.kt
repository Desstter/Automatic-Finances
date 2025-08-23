package com.example.automaticfinances.ui.budget

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.automaticfinances.data.db.Budget
import com.example.automaticfinances.data.db.Category
import com.example.automaticfinances.data.db.AppDatabase
import com.example.automaticfinances.data.repo.BudgetRepository
import com.example.automaticfinances.data.repo.CategoryRepository
import java.text.NumberFormat
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BudgetManagementScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val budgetRepository = remember {
        BudgetRepository(
            budgetDao = AppDatabase.get().budgetDao(),
            transactionDao = AppDatabase.get().transactionDao(),
            categoryDao = AppDatabase.get().categoryDao()
        )
    }
    
    val categoryRepository = remember { CategoryRepository() }
    
    val viewModel: BudgetManagementViewModel = viewModel {
        BudgetManagementViewModel(budgetRepository, categoryRepository)
    }
    
    val state by viewModel.state.collectAsStateWithLifecycle()
    var showCreateDialog by remember { mutableStateOf(false) }
    var editingBudget by remember { mutableStateOf<Budget?>(null) }
    
    LaunchedEffect(Unit) {
        viewModel.loadBudgets()
    }
    
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    
    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        text = "Gestión de Presupuestos",
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                },
                scrollBehavior = scrollBehavior
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showCreateDialog = true }
            ) {
                Icon(Icons.Default.Add, contentDescription = "Crear presupuesto")
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Month selector
            item {
                MonthSelector(
                    currentMonth = state.selectedMonth,
                    onMonthChanged = viewModel::selectMonth
                )
            }
            
            // Summary card
            if (state.budgets.isNotEmpty()) {
                item {
                    BudgetSummaryCard(
                        budgets = state.budgets,
                        selectedMonth = state.selectedMonth
                    )
                }
            }
            
            // Budgets list
            if (state.budgets.isNotEmpty()) {
                item {
                    Text(
                        text = "Presupuestos Activos (${state.budgets.size})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                
                items(state.budgets) { budget ->
                    BudgetManagementCard(
                        budget = budget,
                        category = state.categories.find { it.id == budget.categoryId },
                        onEdit = { editingBudget = budget },
                        onDelete = { viewModel.deleteBudget(budget.id) },
                        onToggleActive = { viewModel.toggleBudgetActive(budget.id) }
                    )
                }
            } else {
                item {
                    EmptyBudgetsCard(
                        onCreateBudget = { showCreateDialog = true }
                    )
                }
            }
        }
    }
    
    // Create/Edit Budget Dialog
    if (showCreateDialog || editingBudget != null) {
        BudgetDialog(
            budget = editingBudget,
            categories = state.categories,
            selectedMonth = state.selectedMonth,
            onDismiss = { 
                showCreateDialog = false
                editingBudget = null
            },
            onSave = { categoryId, amount ->
                if (editingBudget != null) {
                    viewModel.updateBudget(editingBudget!!.id, amount)
                } else {
                    viewModel.createBudget(categoryId, amount, state.selectedMonth)
                }
                showCreateDialog = false
                editingBudget = null
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
private fun MonthSelector(
    currentMonth: YearMonth,
    onMonthChanged: (YearMonth) -> Unit,
    modifier: Modifier = Modifier
) {
    val formatter = DateTimeFormatter.ofPattern("MMMM yyyy", Locale.forLanguageTag("es-CO"))
    
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(
                onClick = { onMonthChanged(currentMonth.minusMonths(1)) }
            ) {
                Text("← Anterior")
            }
            
            Text(
                text = currentMonth.format(formatter).replaceFirstChar { it.uppercase() },
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            
            TextButton(
                onClick = { onMonthChanged(currentMonth.plusMonths(1)) }
            ) {
                Text("Siguiente →")
            }
        }
    }
}

@Composable
private fun BudgetSummaryCard(
    budgets: List<Budget>,
    selectedMonth: YearMonth,
    modifier: Modifier = Modifier
) {
    val nf = remember { NumberFormat.getCurrencyInstance(Locale.forLanguageTag("es-CO")) }
    val totalBudget = budgets.sumOf { it.limitAmountCents }
    val activeBudgets = budgets.count { it.isActive }
    
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Resumen del Mes",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "Total Presupuestado",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = nf.format(totalBudget / 100.0),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "Presupuestos Activos",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "$activeBudgets de ${budgets.size}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun BudgetManagementCard(
    budget: Budget,
    category: Category?,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onToggleActive: () -> Unit,
    modifier: Modifier = Modifier
) {
    val nf = remember { NumberFormat.getCurrencyInstance(Locale.forLanguageTag("es-CO")) }
    
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (budget.isActive) 
                MaterialTheme.colorScheme.surface 
            else 
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Header with category info
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = category?.icon ?: "💰",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = category?.name ?: "Categoría",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        if (!budget.isActive) {
                            Text(
                                text = "Inactivo",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                
                Row {
                    TextButton(onClick = onEdit) {
                        Text("Editar")
                    }
                    TextButton(onClick = onToggleActive) {
                        Text(if (budget.isActive) "Desactivar" else "Activar")
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
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Budget amount and status
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Límite: ${nf.format(budget.limitAmountCents / 100.0)}",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
                
                if (budget.isActive) {
                    AssistChip(
                        onClick = { /* No action needed */ },
                        label = { Text("Activo") },
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            labelColor = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyBudgetsCard(
    onCreateBudget: () -> Unit,
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
                text = "💰",
                style = MaterialTheme.typography.displayMedium
            )
            
            Text(
                text = "No tienes presupuestos para este mes",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            
            Text(
                text = "Crea presupuestos para controlar tus gastos mensuales",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Button(onClick = onCreateBudget) {
                Text("Crear primer presupuesto")
            }
        }
    }
}