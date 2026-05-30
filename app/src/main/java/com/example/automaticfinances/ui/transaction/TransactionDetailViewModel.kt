package com.example.automaticfinances.ui.transaction

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.automaticfinances.data.db.Category
import com.example.automaticfinances.data.db.Transaction
import com.example.automaticfinances.data.repo.CategoryRepository
import com.example.automaticfinances.data.repo.TransactionRepository
import com.example.automaticfinances.domain.DeleteTransactionUseCase
import com.example.automaticfinances.domain.RestoreTransactionUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class TransactionDetailState(
    val transaction: Transaction? = null,
    val categories: List<Category> = emptyList(),
    val selectedCategoryId: Long? = null,
    val description: String = "",
    val notes: String = "",
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val error: String? = null,
    val isEditMode: Boolean = false,
    val isDeleting: Boolean = false,
    val showDeleteConfirmation: Boolean = false,
    val deletedTransaction: Transaction? = null,  // For undo functionality
    val isDeleted: Boolean = false
)

@HiltViewModel
class TransactionDetailViewModel @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val categoryRepository: CategoryRepository,
    private val deleteTransactionUseCase: DeleteTransactionUseCase,
    private val restoreTransactionUseCase: RestoreTransactionUseCase
) : ViewModel() {
    
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
    
    // ================ DELETION METHODS ================
    
    fun showDeleteConfirmation() {
        _state.value = _state.value.copy(showDeleteConfirmation = true)
    }
    
    fun hideDeleteConfirmation() {
        _state.value = _state.value.copy(showDeleteConfirmation = false)
    }
    
    fun deleteTransaction() {
        val currentState = _state.value
        val transaction = currentState.transaction ?: return
        
        viewModelScope.launch {
            _state.value = currentState.copy(
                isDeleting = true,
                showDeleteConfirmation = false,
                error = null
            )
            
            try {
                val success = deleteTransactionUseCase(transaction)

                if (success) {
                    _state.value = _state.value.copy(
                        isDeleting = false,
                        isDeleted = true,
                        deletedTransaction = transaction  // Store for undo
                    )
                } else {
                    _state.value = _state.value.copy(
                        isDeleting = false,
                        error = "No se pudo eliminar la transacción"
                    )
                }
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    isDeleting = false,
                    error = "Error al eliminar: ${e.message}"
                )
            }
        }
    }
    
    fun undoDelete() {
        val deletedTransaction = _state.value.deletedTransaction ?: return
        
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)
            
            try {
                restoreTransactionUseCase(deletedTransaction)

                _state.value = _state.value.copy(
                    transaction = deletedTransaction,
                    isLoading = false,
                    isDeleted = false,
                    deletedTransaction = null
                )
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    isLoading = false,
                    error = "Error al restaurar transacción: ${e.message}"
                )
            }
        }
    }
}