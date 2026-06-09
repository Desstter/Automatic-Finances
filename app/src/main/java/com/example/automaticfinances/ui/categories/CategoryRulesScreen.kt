package com.example.automaticfinances.ui.categories

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.RuleFolder
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.automaticfinances.data.db.Category
import com.example.automaticfinances.ui.components.common.PremiumEmptyState
import com.example.automaticfinances.ui.components.common.SectionCard
import com.example.automaticfinances.ui.components.common.SectionHeader
import com.example.automaticfinances.ui.theme.Sizes
import com.example.automaticfinances.ui.theme.Spacing

/**
 * Editable keyword→category rules (MANT-2). The list the automatic categorizer consults: each rule
 * says "a description containing this word is this category". Replaces what used to be a hardcoded
 * `when` block, so the user can teach the app their own merchants without touching code.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryRulesScreen(
    onNavigateBack: () -> Unit,
    viewModel: CategoryRulesViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var showAddDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Reglas de categoría",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = "Agregar regla")
            }
        },
    ) { padding ->
        val isEmpty = state.expenseRules.isEmpty() && state.incomeRules.isEmpty()
        if (isEmpty && !state.isLoading) {
            PremiumEmptyState(
                modifier = Modifier.padding(padding),
                icon = Icons.Default.RuleFolder,
                title = "Sin reglas",
                description = "Crea una regla para que las transacciones con cierta palabra se clasifiquen automáticamente.",
                actionLabel = "Crear regla",
                onAction = { showAddDialog = true },
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(
                    start = Spacing.screen,
                    end = Spacing.screen,
                    top = Spacing.md,
                    bottom = Spacing.xxxl,
                ),
                verticalArrangement = Arrangement.spacedBy(Spacing.lg),
            ) {
                item {
                    Text(
                        "La palabra se busca dentro de la descripción del movimiento. Gana la coincidencia más específica (la palabra más larga).",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (state.expenseRules.isNotEmpty()) {
                    item { SectionHeader(title = "Gastos") }
                    item {
                        SectionCard(contentPadding = Spacing.none) {
                            state.expenseRules.forEachIndexed { index, row ->
                                if (index > 0) RuleDivider()
                                RuleRowItem(row = row, onDelete = { viewModel.deleteRule(row.rule.id) })
                            }
                        }
                    }
                }
                if (state.incomeRules.isNotEmpty()) {
                    item { SectionHeader(title = "Ingresos") }
                    item {
                        SectionCard(contentPadding = Spacing.none) {
                            state.incomeRules.forEachIndexed { index, row ->
                                if (index > 0) RuleDivider()
                                RuleRowItem(row = row, onDelete = { viewModel.deleteRule(row.rule.id) })
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AddRuleDialog(
            expenseCategories = state.expenseCategories,
            incomeCategories = state.incomeCategories,
            onDismiss = { showAddDialog = false },
            onConfirm = { keyword, categoryName, isIncome ->
                viewModel.addRule(keyword, categoryName, isIncome)
                showAddDialog = false
            },
        )
    }
}

@Composable
private fun RuleRowItem(
    row: CategoryRulesViewModel.RuleRow,
    onDelete: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(Spacing.card),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "\"${row.rule.keyword}\"",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (row.categoryExists) {
                    row.categoryIcon?.let {
                        Text(it, style = MaterialTheme.typography.bodySmall)
                        Spacer(Modifier.size(Spacing.xs))
                    }
                    Text(
                        text = "→ ${row.rule.categoryName}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    Icon(
                        Icons.Default.Warning,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(Sizes.iconSm),
                    )
                    Spacer(Modifier.size(Spacing.xs))
                    Text(
                        text = "→ ${row.rule.categoryName} (categoría no encontrada)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
        }
        IconButton(onClick = onDelete) {
            Icon(
                Icons.Default.Delete,
                contentDescription = "Eliminar regla",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun RuleDivider() {
    androidx.compose.material3.HorizontalDivider(
        modifier = Modifier.padding(horizontal = Spacing.card),
        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddRuleDialog(
    expenseCategories: List<Category>,
    incomeCategories: List<Category>,
    onDismiss: () -> Unit,
    onConfirm: (keyword: String, categoryName: String, isIncome: Boolean) -> Unit,
) {
    var keyword by remember { mutableStateOf("") }
    var isIncome by remember { mutableStateOf(false) }
    var selectedCategory by remember { mutableStateOf<Category?>(null) }
    var categoryMenuExpanded by remember { mutableStateOf(false) }

    val categories = if (isIncome) incomeCategories else expenseCategories
    // Drop a stale selection when switching sides so we never store an expense category on an
    // income rule (or vice-versa).
    if (selectedCategory != null && categories.none { it.id == selectedCategory!!.id }) {
        selectedCategory = null
    }

    val canConfirm = keyword.isNotBlank() && selectedCategory != null

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Default.RuleFolder, contentDescription = null) },
        title = { Text("Nueva regla") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    SegmentedButton(
                        selected = !isIncome,
                        onClick = { isIncome = false },
                        shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                    ) { Text("Gasto") }
                    SegmentedButton(
                        selected = isIncome,
                        onClick = { isIncome = true },
                        shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                    ) { Text("Ingreso") }
                }

                OutlinedTextField(
                    value = keyword,
                    onValueChange = { keyword = it },
                    label = { Text("Palabra clave") },
                    placeholder = { Text("ej. rappi") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )

                ExposedDropdownMenuBox(
                    expanded = categoryMenuExpanded,
                    onExpandedChange = { categoryMenuExpanded = it },
                ) {
                    OutlinedTextField(
                        value = selectedCategory?.let { "${it.icon} ${it.name}" } ?: "",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Categoría") },
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryMenuExpanded)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(androidx.compose.material3.MenuAnchorType.PrimaryNotEditable),
                    )
                    ExposedDropdownMenu(
                        expanded = categoryMenuExpanded,
                        onDismissRequest = { categoryMenuExpanded = false },
                    ) {
                        categories.forEach { category ->
                            DropdownMenuItem(
                                text = { Text("${category.icon} ${category.name}") },
                                onClick = {
                                    selectedCategory = category
                                    categoryMenuExpanded = false
                                },
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = canConfirm,
                onClick = {
                    val cat = selectedCategory ?: return@TextButton
                    onConfirm(keyword, cat.name, isIncome)
                },
            ) { Text("Guardar") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        },
    )
}
