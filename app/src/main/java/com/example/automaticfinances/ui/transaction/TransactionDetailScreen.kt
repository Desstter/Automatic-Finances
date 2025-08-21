package com.example.automaticfinances.ui.transaction

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import java.text.NumberFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionDetailScreen(
    transactionId: String,
    onNavigateBack: () -> Unit,
    viewModel: TransactionDetailViewModel = viewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    
    LaunchedEffect(transactionId) {
        viewModel.loadTransaction(transactionId)
    }
    
    val numberFormat = remember { 
        NumberFormat.getCurrencyInstance(Locale("es", "CO"))
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Detalle de Transacción") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                },
                actions = {
                    if (state.isEditMode) {
                        IconButton(
                            onClick = { viewModel.saveChanges() },
                            enabled = !state.isSaving
                        ) {
                            if (state.isSaving) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Icon(Icons.Default.Check, contentDescription = "Guardar")
                            }
                        }
                        IconButton(onClick = { viewModel.cancelEdit() }) {
                            Icon(Icons.Default.Close, contentDescription = "Cancelar")
                        }
                    } else {
                        IconButton(
                            onClick = { viewModel.enableEditMode() },
                            enabled = !state.isDeleting
                        ) {
                            Icon(Icons.Default.Edit, contentDescription = "Editar")
                        }
                        IconButton(
                            onClick = { viewModel.showDeleteConfirmation() },
                            enabled = !state.isDeleting
                        ) {
                            if (state.isDeleting) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = "Eliminar",
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                }
            )
        }
    ) { padding ->
        when {
            state.isLoading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            
            state.error != null -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "Error",
                                style = MaterialTheme.typography.headlineSmall,
                                color = MaterialTheme.colorScheme.error
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = state.error!!,
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(onClick = { viewModel.clearError() }) {
                                Text("Cerrar")
                            }
                        }
                    }
                }
            }
            
            state.transaction != null -> {
                TransactionDetailContent(
                    state = state,
                    numberFormat = numberFormat,
                    viewModel = viewModel,
                    modifier = Modifier.padding(padding)
                )
            }
        }
        
        // Confirmation Dialog
        if (state.showDeleteConfirmation) {
            DeleteConfirmationDialog(
                onConfirm = { viewModel.deleteTransaction() },
                onDismiss = { viewModel.hideDeleteConfirmation() },
                transactionAmount = state.transaction?.let { 
                    numberFormat.format(it.amountCents / 100.0) 
                } ?: "",
                transactionDescription = state.transaction?.description ?: ""
            )
        }
        
        // Deleted Snackbar with Undo
        if (state.isDeleted) {
            LaunchedEffect(state.isDeleted) {
                // Auto-navigate back after a short delay if user doesn't undo
                kotlinx.coroutines.delay(5000)
                if (state.isDeleted) {
                    onNavigateBack()
                }
            }
        }
    }
}

@Composable
fun DeleteConfirmationDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    transactionAmount: String,
    transactionDescription: String
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Eliminar transacción",
                style = MaterialTheme.typography.headlineSmall
            )
        },
        text = {
            Column {
                Text(
                    text = "¿Estás seguro de que deseas eliminar esta transacción?",
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(8.dp))
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp)
                    ) {
                        Text(
                            text = transactionAmount,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Text(
                            text = transactionDescription,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Esta acción actualizará automáticamente todos los cálculos de presupuesto, gráficos y estadísticas.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error
                )
            ) {
                Text("Eliminar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}

@Composable
fun TransactionDetailContent(
    state: TransactionDetailState,
    numberFormat: NumberFormat,
    viewModel: TransactionDetailViewModel,
    modifier: Modifier = Modifier
) {
    val transaction = state.transaction!!
    
    // Show deleted state with undo option
    if (state.isDeleted) {
        DeletedTransactionContent(
            onUndo = { viewModel.undoDelete() },
            transactionAmount = numberFormat.format(transaction.amountCents / 100.0),
            transactionDescription = transaction.description,
            modifier = modifier
        )
        return
    }
    
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Rest of the existing content...
        TransactionDetailCards(
            state = state,
            numberFormat = numberFormat,
            viewModel = viewModel
        )
    }
}

@Composable
fun DeletedTransactionContent(
    onUndo: () -> Unit,
    transactionAmount: String,
    transactionDescription: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    Icons.Default.Check,
                    contentDescription = null,
                    modifier = Modifier.size(48.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Transacción eliminada",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "$transactionAmount - $transactionDescription",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Todos los cálculos se han actualizado automáticamente",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
                Spacer(modifier = Modifier.height(20.dp))
                Button(
                    onClick = onUndo,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Deshacer eliminación")
                }
            }
        }
    }
}

@Composable
fun TransactionDetailCards(
    state: TransactionDetailState,
    numberFormat: NumberFormat,
    viewModel: TransactionDetailViewModel
) {
    val transaction = state.transaction!!
    
    // Card con información básica
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = numberFormat.format(transaction.amountCents / 100.0),
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold
                )
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Text(
                        text = transaction.type,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
            
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "📅",
                    style = MaterialTheme.typography.bodyLarge
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "${transaction.date} a las ${transaction.time}",
                    style = MaterialTheme.typography.bodyLarge
                )
            }
            
            if (transaction.srcLast4 != null || transaction.dstLast4 != null) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "💳",
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = when {
                            transaction.srcLast4 != null && transaction.dstLast4 != null ->
                                "De *${transaction.srcLast4} a *${transaction.dstLast4}"
                            transaction.srcLast4 != null -> "Tarjeta *${transaction.srcLast4}"
                            else -> "A cuenta *${transaction.dstLast4}"
                        },
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }
        }
    }
    
    // Descripción (editable)
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Descripción",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            
            if (state.isEditMode) {
                OutlinedTextField(
                    value = state.description,
                    onValueChange = viewModel::updateDescription,
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    singleLine = true
                )
            } else {
                Text(
                    text = state.description,
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        }
    }
    
    // Categoría (editable)
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Categoría",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            
            if (state.isEditMode) {
                CategorySelector(
                    categories = state.categories,
                    selectedCategoryId = state.selectedCategoryId,
                    onCategorySelected = viewModel::selectCategory
                )
            } else {
                val category = state.selectedCategoryId?.let { viewModel.getCategoryById(it) }
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = category?.icon ?: "📦",
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = category?.name ?: "Sin categoría",
                        style = MaterialTheme.typography.bodyLarge,
                        color = category?.color?.let { 
                            Color(android.graphics.Color.parseColor(it))
                        } ?: MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
    
    // Notas (editable)
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Notas",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            
            if (state.isEditMode) {
                OutlinedTextField(
                    value = state.notes,
                    onValueChange = viewModel::updateNotes,
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Agregar notas...") },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    minLines = 3,
                    maxLines = 5
                )
            } else {
                if (state.notes.isNotBlank()) {
                    Text(
                        text = state.notes,
                        style = MaterialTheme.typography.bodyLarge
                    )
                } else {
                    Text(
                        text = "Sin notas",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
            }
        }
    }
}

@Composable
fun CategorySelector(
    categories: List<com.example.automaticfinances.data.db.Category>,
    selectedCategoryId: Long?,
    onCategorySelected: (Long?) -> Unit
) {
    Column {
        // Opción "Sin categoría"
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onCategorySelected(null) },
            colors = CardDefaults.cardColors(
                containerColor = if (selectedCategoryId == null) {
                    MaterialTheme.colorScheme.primaryContainer
                } else {
                    MaterialTheme.colorScheme.surface
                }
            )
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "❌",
                    style = MaterialTheme.typography.bodyLarge
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Sin categoría",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // Categorías en grid
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(categories) { category ->
                Card(
                    modifier = Modifier.clickable { onCategorySelected(category.id) },
                    colors = CardDefaults.cardColors(
                        containerColor = if (selectedCategoryId == category.id) {
                            Color(android.graphics.Color.parseColor(category.color)).copy(alpha = 0.3f)
                        } else {
                            MaterialTheme.colorScheme.surface
                        }
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = category.icon,
                            style = MaterialTheme.typography.headlineSmall
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = category.name,
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(android.graphics.Color.parseColor(category.color))
                        )
                    }
                }
            }
        }
    }
}