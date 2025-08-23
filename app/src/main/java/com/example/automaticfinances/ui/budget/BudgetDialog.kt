package com.example.automaticfinances.ui.budget

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.material3.MenuAnchorType
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.automaticfinances.data.db.Budget
import com.example.automaticfinances.data.db.Category
import java.text.NumberFormat
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BudgetDialog(
    budget: Budget?,
    categories: List<Category>,
    selectedMonth: YearMonth,
    onDismiss: () -> Unit,
    onSave: (categoryId: Long, amountCents: Long) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedCategoryId by remember { 
        mutableLongStateOf(budget?.categoryId ?: categories.firstOrNull()?.id ?: 0L) 
    }
    var amountText by remember { 
        mutableStateOf(
            if (budget != null) (budget.limitAmountCents / 100).toString() else ""
        ) 
    }
    var expandedCategory by remember { mutableStateOf(false) }
    var showError by remember { mutableStateOf(false) }
    
    val nf = remember { NumberFormat.getCurrencyInstance(Locale.forLanguageTag("es-CO")) }
    val formatter = DateTimeFormatter.ofPattern("MMMM yyyy", Locale.forLanguageTag("es-CO"))
    
    val selectedCategory = categories.find { it.id == selectedCategoryId }
    val isEdit = budget != null
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text(
                    text = if (isEdit) "Editar Presupuesto" else "Crear Presupuesto",
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = selectedMonth.format(formatter).replaceFirstChar { it.uppercase() },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Category selection (only for new budgets)
                if (!isEdit) {
                    Column {
                        Text(
                            text = "Categoría",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Medium
                        )
                        
                        Spacer(modifier = Modifier.height(4.dp))
                        
                        ExposedDropdownMenuBox(
                            expanded = expandedCategory,
                            onExpandedChange = { expandedCategory = !expandedCategory }
                        ) {
                            OutlinedTextField(
                                value = selectedCategory?.name ?: "",
                                onValueChange = { },
                                readOnly = true,
                                leadingIcon = {
                                    Text(
                                        text = selectedCategory?.icon ?: "💰",
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                },
                                trailingIcon = {
                                    ExposedDropdownMenuDefaults.TrailingIcon(
                                        expanded = expandedCategory
                                    )
                                },
                                modifier = Modifier
                                    .menuAnchor(MenuAnchorType.PrimaryNotEditable, enabled = true)
                                    .fillMaxWidth(),
                                colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors()
                            )
                            
                            ExposedDropdownMenu(
                                expanded = expandedCategory,
                                onDismissRequest = { expandedCategory = false }
                            ) {
                                categories.forEach { category ->
                                    DropdownMenuItem(
                                        text = {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = category.icon,
                                                    style = MaterialTheme.typography.titleMedium
                                                )
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text(category.name)
                                            }
                                        },
                                        onClick = {
                                            selectedCategoryId = category.id
                                            expandedCategory = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                } else {
                    // Show selected category for editing
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = selectedCategory?.icon ?: "💰",
                                style = MaterialTheme.typography.titleMedium
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = selectedCategory?.name ?: "Categoría",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
                
                // Amount input
                Column {
                    Text(
                        text = "Límite mensual",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Medium
                    )
                    
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    OutlinedTextField(
                        value = amountText,
                        onValueChange = { 
                            amountText = it.filter { char -> char.isDigit() }
                            showError = false
                        },
                        label = { Text("Cantidad en COP") },
                        placeholder = { Text("Ej: 500000") },
                        prefix = { Text("$") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        isError = showError,
                        supportingText = if (showError) {
                            { Text("Por favor ingresa una cantidad válida") }
                        } else {
                            amountText.toLongOrNull()?.let { amount ->
                                { Text("Equivale a ${nf.format(amount)}") }
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amount = amountText.toLongOrNull()
                    if (amount != null && amount > 0 && selectedCategoryId > 0) {
                        onSave(selectedCategoryId, amount * 100) // Convert to cents
                    } else {
                        showError = true
                    }
                }
            ) {
                Text(if (isEdit) "Actualizar" else "Crear")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}