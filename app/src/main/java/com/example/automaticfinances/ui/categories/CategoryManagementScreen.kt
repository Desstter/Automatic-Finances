package com.example.automaticfinances.ui.categories

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import com.example.automaticfinances.ui.components.common.PremiumEmptyState
import com.example.automaticfinances.ui.theme.Spacing
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryManagementScreen(
    onNavigateBack: () -> Unit
) {
    val viewModel: CategoryManagementViewModel = hiltViewModel()
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    // Surface errors instead of dropping them: show, then clear so the same
    // error can fire again later.
    LaunchedEffect(state.error) {
        state.error?.let { error ->
            snackbarHostState.showSnackbar(error)
            viewModel.clearError()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Categorías", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { viewModel.showAddDialog() },
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("Categoría") },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
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
            
            else -> {
                if (state.categories.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                        PremiumEmptyState(
                            icon = Icons.Default.Category,
                            title = "Sin categorías",
                            description = "Crea categorías para organizar y clasificar tus transacciones.",
                            actionLabel = "Crear categoría",
                            onAction = { viewModel.showAddDialog() }
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(padding)
                            .padding(horizontal = Spacing.screen),
                        contentPadding = PaddingValues(top = Spacing.md, bottom = 96.dp),
                        verticalArrangement = Arrangement.spacedBy(Spacing.sm)
                    ) {
                        items(state.categories) { categoryWithCount ->
                            CategoryItem(
                                categoryWithCount = categoryWithCount,
                                onEdit = { viewModel.showEditDialog(it) },
                                onDelete = { viewModel.showDeleteDialog(it) }
                            )
                        }
                    }
                }
            }
        }
    }
    
    // Dialogs
    if (state.showAddDialog) {
        CategoryAddEditDialog(
            title = "Nueva categoría",
            name = state.newCategoryName,
            icon = state.newCategoryIcon,
            color = state.newCategoryColor,
            availableColors = viewModel.availableColors,
            availableIcons = viewModel.availableIcons,
            onNameChange = viewModel::updateNewCategoryName,
            onIconChange = viewModel::updateNewCategoryIcon,
            onColorChange = viewModel::updateNewCategoryColor,
            onSave = viewModel::addCategory,
            onDismiss = viewModel::hideAddDialog
        )
    }
    
    if (state.showEditDialog && state.selectedCategory != null) {
        CategoryAddEditDialog(
            title = "Editar categoría",
            name = state.newCategoryName,
            icon = state.newCategoryIcon,
            color = state.newCategoryColor,
            availableColors = viewModel.availableColors,
            availableIcons = viewModel.availableIcons,
            onNameChange = viewModel::updateNewCategoryName,
            onIconChange = viewModel::updateNewCategoryIcon,
            onColorChange = viewModel::updateNewCategoryColor,
            onSave = viewModel::updateCategory,
            onDismiss = viewModel::hideEditDialog
        )
    }
    
    if (state.showDeleteDialog && state.selectedCategory != null) {
        AlertDialog(
            onDismissRequest = viewModel::hideDeleteDialog,
            title = { Text("Eliminar Categoría") },
            text = { 
                Column {
                    Text("¿Estás seguro de que quieres eliminar la categoría \"${state.selectedCategory!!.name}\"?")
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = state.deleteConfirmationMessage,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = viewModel::deleteCategory) {
                    Text("Eliminar")
                }
            },
            dismissButton = {
                TextButton(onClick = viewModel::hideDeleteDialog) {
                    Text("Cancelar")
                }
            }
        )
    }
}

@Composable
fun CategoryItem(
    categoryWithCount: com.example.automaticfinances.data.db.CategoryWithCount,
    onEdit: (com.example.automaticfinances.data.db.Category) -> Unit,
    onDelete: (com.example.automaticfinances.data.db.Category) -> Unit
) {
    val category = com.example.automaticfinances.data.db.Category(
        id = categoryWithCount.id,
        name = categoryWithCount.name,
        color = categoryWithCount.color,
        icon = categoryWithCount.icon,
        isDefault = categoryWithCount.isDefault,
        isActive = categoryWithCount.isActive
    )
    
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icono de categoría con color de fondo
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(
                        Color(android.graphics.Color.parseColor(categoryWithCount.color)).copy(alpha = 0.2f)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = categoryWithCount.icon,
                    style = MaterialTheme.typography.headlineSmall
                )
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            // Información de la categoría
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = categoryWithCount.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color(android.graphics.Color.parseColor(categoryWithCount.color))
                )
                Text(
                    text = "${categoryWithCount.transactionCount} transacciones",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
                if (categoryWithCount.isDefault) {
                    Text(
                        text = "Predefinida",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
            
            // Botones de acción — todas las categorías (incluidas las predefinidas)
            // pueden editarse y eliminarse.
            Row {
                IconButton(onClick = { onEdit(category) }) {
                    Icon(Icons.Default.Edit, contentDescription = "Editar")
                }

                IconButton(onClick = { onDelete(category) }) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Eliminar",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

@Composable
fun CategoryAddEditDialog(
    title: String,
    name: String,
    icon: String,
    color: String,
    availableColors: List<String>,
    availableIcons: List<String>,
    onNameChange: (String) -> Unit,
    onIconChange: (String) -> Unit,
    onColorChange: (String) -> Unit,
    onSave: () -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Preview de la categoría
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .background(
                                Color(android.graphics.Color.parseColor(color)).copy(alpha = 0.2f)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = icon,
                            style = MaterialTheme.typography.headlineLarge
                        )
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(
                        text = name.ifBlank { "Nombre de categoría" },
                        style = MaterialTheme.typography.titleMedium,
                        color = Color(android.graphics.Color.parseColor(color))
                    )
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // Campo de nombre
                OutlinedTextField(
                    value = name,
                    onValueChange = onNameChange,
                    label = { Text("Nombre") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Selección de icono
                Text(
                    text = "Icono",
                    style = MaterialTheme.typography.labelLarge
                )
                Spacer(modifier = Modifier.height(8.dp))
                LazyVerticalGrid(
                    columns = GridCells.Fixed(6),
                    modifier = Modifier.height(200.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(availableIcons) { availableIcon ->
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(
                                    if (availableIcon == icon) {
                                        MaterialTheme.colorScheme.primaryContainer
                                    } else {
                                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                                    }
                                )
                                .clickable { onIconChange(availableIcon) },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = availableIcon,
                                style = MaterialTheme.typography.titleMedium
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Selección de color
                Text(
                    text = "Color",
                    style = MaterialTheme.typography.labelLarge
                )
                Spacer(modifier = Modifier.height(8.dp))
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(availableColors) { availableColor ->
                        val swatchColor = Color(android.graphics.Color.parseColor(availableColor))
                        // Contrast for the check sits on an arbitrary DB color, so derive it
                        // from luminance rather than a theme token.
                        val onSwatch = if (swatchColor.luminance() > 0.5f) Color.Black else Color.White
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(swatchColor)
                                .clickable { onColorChange(availableColor) }
                        ) {
                            if (availableColor == color) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .clip(CircleShape)
                                        .background(onSwatch.copy(alpha = 0.25f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        tint = onSwatch,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
                
                // Botones
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancelar")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = onSave,
                        enabled = name.isNotBlank()
                    ) {
                        Text("Guardar")
                    }
                }
            }
        }
    }
}