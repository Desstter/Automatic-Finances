package com.example.automaticfinances.ui.goals

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
import com.example.automaticfinances.data.db.Category
import com.example.automaticfinances.data.db.FinancialGoal
import com.example.automaticfinances.data.db.GoalType
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GoalDialog(
    goal: FinancialGoal?,
    categories: List<Category>,
    onDismiss: () -> Unit,
    onSave: (name: String, description: String, targetAmountCents: Long, targetDate: Long, type: GoalType, categoryId: Long?) -> Unit,
    modifier: Modifier = Modifier
) {
    var name by remember { mutableStateOf(goal?.name ?: "") }
    var description by remember { mutableStateOf(goal?.description ?: "") }
    var amountText by remember { 
        mutableStateOf(
            if (goal != null) (goal.targetAmountCents / 100).toString() else ""
        ) 
    }
    var selectedType by remember { mutableStateOf(goal?.type ?: GoalType.SAVINGS) }
    var selectedCategoryId by remember { mutableLongStateOf(goal?.categoryId ?: 0L) }
    var selectedDate by remember { mutableStateOf(Date(goal?.targetDate ?: System.currentTimeMillis() + (30L * 24 * 60 * 60 * 1000))) } // Default to 30 days from now
    
    var expandedType by remember { mutableStateOf(false) }
    var expandedCategory by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showError by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }
    
    val nf = remember { NumberFormat.getCurrencyInstance(Locale.forLanguageTag("es-CO")) }
    val dateFormat = remember { SimpleDateFormat("dd/MM/yyyy", Locale.forLanguageTag("es-CO")) }
    
    val selectedCategory = categories.find { it.id == selectedCategoryId }
    val isEdit = goal != null
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (isEdit) "Editar Meta" else "Crear Meta Financiera",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Goal name
                OutlinedTextField(
                    value = name,
                    onValueChange = { 
                        name = it
                        showError = false
                    },
                    label = { Text("Nombre de la meta") },
                    placeholder = { Text("Ej: Ahorrar para vacaciones") },
                    isError = showError && name.isBlank(),
                    modifier = Modifier.fillMaxWidth()
                )
                
                // Goal description
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Descripción (opcional)") },
                    placeholder = { Text("Detalles adicionales sobre tu meta") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 2
                )
                
                // Goal type selection
                Column {
                    Text(
                        text = "Tipo de meta",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Medium
                    )
                    
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    ExposedDropdownMenuBox(
                        expanded = expandedType,
                        onExpandedChange = { expandedType = !expandedType }
                    ) {
                        OutlinedTextField(
                            value = when (selectedType) {
                                GoalType.SAVINGS -> "💰 Meta de ahorro"
                                GoalType.EXPENSE_REDUCTION -> "📉 Reducir gastos"
                            },
                            onValueChange = { },
                            readOnly = true,
                            trailingIcon = {
                                ExposedDropdownMenuDefaults.TrailingIcon(
                                    expanded = expandedType
                                )
                            },
                            modifier = Modifier
                                .menuAnchor(MenuAnchorType.PrimaryNotEditable, enabled = true)
                                .fillMaxWidth(),
                            colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors()
                        )
                        
                        ExposedDropdownMenu(
                            expanded = expandedType,
                            onDismissRequest = { expandedType = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("💰 Meta de ahorro") },
                                onClick = {
                                    selectedType = GoalType.SAVINGS
                                    expandedType = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("📉 Reducir gastos") },
                                onClick = {
                                    selectedType = GoalType.EXPENSE_REDUCTION
                                    expandedType = false
                                }
                            )
                        }
                    }
                }
                
                // Category selection (optional)
                if (selectedType == GoalType.EXPENSE_REDUCTION) {
                    Column {
                        Text(
                            text = "Categoría (opcional)",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Medium
                        )
                        
                        Spacer(modifier = Modifier.height(4.dp))
                        
                        ExposedDropdownMenuBox(
                            expanded = expandedCategory,
                            onExpandedChange = { expandedCategory = !expandedCategory }
                        ) {
                            OutlinedTextField(
                                value = if (selectedCategoryId == 0L) "Ninguna" else selectedCategory?.name ?: "",
                                onValueChange = { },
                                readOnly = true,
                                leadingIcon = if (selectedCategoryId != 0L) {
                                    {
                                        Text(
                                            text = selectedCategory?.icon ?: "📂",
                                            style = MaterialTheme.typography.titleMedium
                                        )
                                    }
                                } else null,
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
                                DropdownMenuItem(
                                    text = { Text("Ninguna") },
                                    onClick = {
                                        selectedCategoryId = 0L
                                        expandedCategory = false
                                    }
                                )
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
                }
                
                // Target amount
                Column {
                    Text(
                        text = when (selectedType) {
                            GoalType.SAVINGS -> "Cantidad a ahorrar"
                            GoalType.EXPENSE_REDUCTION -> "Cantidad a reducir"
                        },
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
                        placeholder = { Text("Ej: 1000000") },
                        prefix = { Text("$") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        isError = showError && amountText.toLongOrNull() == null,
                        supportingText = amountText.toLongOrNull()?.let { amount ->
                            { Text("Equivale a ${nf.format(amount)}") }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                
                // Target date
                Column {
                    Text(
                        text = "Fecha límite",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Medium
                    )
                    
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    OutlinedTextField(
                        value = dateFormat.format(selectedDate),
                        onValueChange = { },
                        readOnly = true,
                        label = { Text("Fecha") },
                        trailingIcon = {
                            TextButton(onClick = { showDatePicker = true }) {
                                Text("Cambiar")
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                
                // Show error message if any
                if (showError && errorMessage.isNotEmpty()) {
                    Text(
                        text = errorMessage,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amount = amountText.toLongOrNull()
                    when {
                        name.isBlank() -> {
                            showError = true
                            errorMessage = "El nombre es requerido"
                        }
                        amount == null || amount <= 0 -> {
                            showError = true
                            errorMessage = "Ingresa una cantidad válida"
                        }
                        selectedDate.time <= System.currentTimeMillis() -> {
                            showError = true
                            errorMessage = "La fecha límite debe ser futura"
                        }
                        else -> {
                            onSave(
                                name,
                                description,
                                amount * 100, // Convert to cents
                                selectedDate.time,
                                selectedType,
                                if (selectedCategoryId == 0L) null else selectedCategoryId
                            )
                        }
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
    
    // Date picker
    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = selectedDate.time
        )
        
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            selectedDate = Date(millis)
                        }
                        showDatePicker = false
                    }
                ) {
                    Text("Aceptar")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Cancelar")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}