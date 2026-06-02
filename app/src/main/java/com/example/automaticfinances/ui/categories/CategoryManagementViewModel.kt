package com.example.automaticfinances.ui.categories

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.automaticfinances.data.db.Category
import com.example.automaticfinances.data.db.CategoryWithCount
import com.example.automaticfinances.data.repo.CategoryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CategoryManagementState(
    val categories: List<CategoryWithCount> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val showAddDialog: Boolean = false,
    val showEditDialog: Boolean = false,
    val showDeleteDialog: Boolean = false,
    val selectedCategory: Category? = null,
    val newCategoryName: String = "",
    val newCategoryIcon: String = "📦",
    val newCategoryColor: String = "#9E9E9E",
    val deleteConfirmationMessage: String = ""
)

@HiltViewModel
class CategoryManagementViewModel @Inject constructor(
    private val categoryRepository: CategoryRepository
) : ViewModel() {
    
    private val _state = MutableStateFlow(CategoryManagementState())
    val state: StateFlow<CategoryManagementState> = _state.asStateFlow()
    
    // Lista de colores predefinidos para categorías
    val availableColors = listOf(
        "#F44336", "#E91E63", "#9C27B0", "#673AB7",
        "#3F51B5", "#2196F3", "#03A9F4", "#00BCD4",
        "#009688", "#4CAF50", "#8BC34A", "#CDDC39",
        "#FFEB3B", "#FFC107", "#FF9800", "#FF5722",
        "#795548", "#9E9E9E", "#607D8B"
    )
    
    // Paleta de iconos. Se usan emojis (renderizados por la fuente del sistema, sin
    // assets ni imports que mantener) y se agrupan por tema para que el selector sea
    // fácil de explorar. Ampliada de 24 a 70+ opciones.
    val availableIcons = listOf(
        // Comida y bebida
        "🍽️", "🍔", "🍕", "🍣", "🥗", "🍎", "🛒", "☕", "🍺", "🍷",
        // Hogar y servicios
        "🏠", "🏢", "🛋️", "🔧", "🧹", "💡", "💧", "🔥", "🌐", "📞",
        // Transporte
        "🚗", "⛽", "🚕", "🚌", "🚇", "🚲", "✈️", "🛵", "🅿️", "🛣️",
        // Salud y cuidado personal
        "🏥", "💊", "🩺", "🦷", "💇", "💅", "🧴", "🏋️", "🏃", "🧘",
        // Compras y ropa
        "👕", "👗", "👟", "👜", "🛍️", "💄", "⌚", "💍", "🧥", "🎁",
        // Ocio y educación
        "🎬", "🎮", "🎵", "📚", "🎨", "🎟️", "🎤", "🏖️", "🎓", "🐶",
        // Tecnología
        "📱", "💻", "🖥️", "🎧", "📷", "🖨️", "🔌", "🕹️",
        // Dinero e ingresos
        "💰", "💵", "💳", "🏦", "📈", "📉", "💸", "🪙", "🤝", "🎯",
        // Otros / genéricos
        "📦", "📋", "⭐", "❤️", "🔔", "🌍", "🐾", "👶", "🎉", "🧾"
    )
    
    init {
        loadCategories()
    }
    
    private fun loadCategories() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)
            
            try {
                categoryRepository.getCategoriesWithCount().collectLatest { categories ->
                    _state.value = _state.value.copy(
                        categories = categories,
                        isLoading = false
                    )
                }
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    isLoading = false,
                    error = "Error al cargar categorías: ${e.message}"
                )
            }
        }
    }
    
    fun showAddDialog() {
        _state.value = _state.value.copy(
            showAddDialog = true,
            newCategoryName = "",
            newCategoryIcon = "📦",
            newCategoryColor = "#9E9E9E"
        )
    }
    
    fun hideAddDialog() {
        _state.value = _state.value.copy(showAddDialog = false)
    }
    
    fun showEditDialog(category: Category) {
        _state.value = _state.value.copy(
            showEditDialog = true,
            selectedCategory = category,
            newCategoryName = category.name,
            newCategoryIcon = category.icon,
            newCategoryColor = category.color
        )
    }
    
    fun hideEditDialog() {
        _state.value = _state.value.copy(
            showEditDialog = false,
            selectedCategory = null
        )
    }
    
    fun showDeleteDialog(category: Category) {
        viewModelScope.launch {
            val (canDelete, message) = categoryRepository.canDelete(category.id)
            _state.value = _state.value.copy(
                showDeleteDialog = true,
                selectedCategory = category,
                deleteConfirmationMessage = message
            )
        }
    }
    
    fun hideDeleteDialog() {
        _state.value = _state.value.copy(
            showDeleteDialog = false,
            selectedCategory = null,
            deleteConfirmationMessage = ""
        )
    }
    
    fun updateNewCategoryName(name: String) {
        _state.value = _state.value.copy(newCategoryName = name)
    }
    
    fun updateNewCategoryIcon(icon: String) {
        _state.value = _state.value.copy(newCategoryIcon = icon)
    }
    
    fun updateNewCategoryColor(color: String) {
        _state.value = _state.value.copy(newCategoryColor = color)
    }
    
    fun addCategory() {
        val currentState = _state.value
        
        if (currentState.newCategoryName.isBlank()) {
            _state.value = currentState.copy(error = "El nombre de la categoría no puede estar vacío")
            return
        }
        
        viewModelScope.launch {
            try {
                val newCategory = Category(
                    name = currentState.newCategoryName.trim(),
                    icon = currentState.newCategoryIcon,
                    color = currentState.newCategoryColor,
                    isDefault = false,
                    isActive = true
                )
                
                categoryRepository.insert(newCategory)
                
                _state.value = currentState.copy(
                    showAddDialog = false,
                    newCategoryName = "",
                    error = null
                )
            } catch (e: Exception) {
                _state.value = currentState.copy(
                    error = "Error al crear categoría: ${e.message}"
                )
            }
        }
    }
    
    fun updateCategory() {
        val currentState = _state.value
        val selectedCategory = currentState.selectedCategory ?: return
        
        if (currentState.newCategoryName.isBlank()) {
            _state.value = currentState.copy(error = "El nombre de la categoría no puede estar vacío")
            return
        }
        
        viewModelScope.launch {
            try {
                val updatedCategory = selectedCategory.copy(
                    name = currentState.newCategoryName.trim(),
                    icon = currentState.newCategoryIcon,
                    color = currentState.newCategoryColor
                )
                
                categoryRepository.update(updatedCategory)
                
                _state.value = currentState.copy(
                    showEditDialog = false,
                    selectedCategory = null,
                    error = null
                )
            } catch (e: Exception) {
                _state.value = currentState.copy(
                    error = "Error al actualizar categoría: ${e.message}"
                )
            }
        }
    }
    
    fun deleteCategory() {
        val currentState = _state.value
        val selectedCategory = currentState.selectedCategory ?: return
        
        viewModelScope.launch {
            try {
                categoryRepository.delete(selectedCategory.id)
                
                _state.value = currentState.copy(
                    showDeleteDialog = false,
                    selectedCategory = null,
                    deleteConfirmationMessage = "",
                    error = null
                )
            } catch (e: Exception) {
                _state.value = currentState.copy(
                    error = "Error al eliminar categoría: ${e.message}"
                )
            }
        }
    }
    
    fun clearError() {
        _state.value = _state.value.copy(error = null)
    }
}