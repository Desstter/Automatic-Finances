package com.example.automaticfinances.ui.transaction

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.automaticfinances.data.db.Category
import com.example.automaticfinances.data.db.Transaction
import com.example.automaticfinances.data.repo.CategoryRepository
import com.example.automaticfinances.data.repo.TransactionRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class TransactionDetailState(
    val transaction: Transaction? = null,
    val categories: List<Category> = emptyList(),
    val selectedCategoryId: Long? = null,
    val description: String = "",
    val notes: String = "",
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val error: String? = null,
    val isEditMode: Boolean = false
)

class TransactionDetailViewModel : ViewModel() {
    private val transactionRepository = TransactionRepository()
    private val categoryRepository = CategoryRepository()
    
    private val _state = MutableStateFlow(TransactionDetailState())
    val state: StateFlow<TransactionDetailState> = _state.asStateFlow()
    
    fun loadTransaction(transactionId: String) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)
            
            try {
                val transaction = transactionRepository.getById(transactionId)
                
                if (transaction != null) {
                    // Filter categories based on transaction type (income vs expense)
                    val categories = categoryRepository.getActiveSyncByType(transaction.isIncome)
                    _state.value = _state.value.copy(
                        transaction = transaction,
                        categories = categories,
                        selectedCategoryId = transaction.categoryId,
                        description = transaction.description,
                        notes = transaction.notes,
                        isLoading = false,
                        isEditMode = false
                    )
                } else {
                    _state.value = _state.value.copy(
                        isLoading = false,
                        error = "Transacción no encontrada"
                    )
                }
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    isLoading = false,
                    error = "Error al cargar transacción: ${e.message}"
                )
            }
        }
    }
    
    fun enableEditMode() {
        _state.value = _state.value.copy(isEditMode = true)
    }
    
    fun cancelEdit() {
        val transaction = _state.value.transaction ?: return
        _state.value = _state.value.copy(
            isEditMode = false,
            selectedCategoryId = transaction.categoryId,
            description = transaction.description,
            notes = transaction.notes,
            error = null
        )
    }
    
    fun updateDescription(newDescription: String) {
        _state.value = _state.value.copy(description = newDescription)
    }
    
    fun updateNotes(newNotes: String) {
        _state.value = _state.value.copy(notes = newNotes)
    }
    
    fun selectCategory(categoryId: Long?) {
        _state.value = _state.value.copy(selectedCategoryId = categoryId)
    }
    
    fun saveChanges() {
        val currentState = _state.value
        val transaction = currentState.transaction ?: return
        
        if (currentState.description.isBlank()) {
            _state.value = currentState.copy(error = "La descripción no puede estar vacía")
            return
        }
        
        viewModelScope.launch {
            _state.value = currentState.copy(isSaving = true, error = null)
            
            try {
                val updatedTransaction = transaction.copy(
                    description = currentState.description.trim(),
                    notes = currentState.notes.trim(),
                    categoryId = currentState.selectedCategoryId
                )
                
                transactionRepository.update(updatedTransaction)
                
                _state.value = _state.value.copy(
                    transaction = updatedTransaction,
                    isSaving = false,
                    isEditMode = false
                )
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    isSaving = false,
                    error = "Error al guardar cambios: ${e.message}"
                )
            }
        }
    }
    
    fun clearError() {
        _state.value = _state.value.copy(error = null)
    }
    
    fun getCategoryById(categoryId: Long): Category? {
        return _state.value.categories.find { it.id == categoryId }
    }
}