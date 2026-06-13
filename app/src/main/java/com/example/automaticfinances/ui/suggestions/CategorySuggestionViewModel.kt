package com.example.automaticfinances.ui.suggestions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.automaticfinances.data.db.CategorySuggestion
import com.example.automaticfinances.data.repo.CategoryRepository
import com.example.automaticfinances.data.repo.TransactionRepository
import com.example.automaticfinances.data.repo.TransactionWithCategory
import com.example.automaticfinances.data.repo.UserCategoryPreferenceRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import android.util.Log
import javax.inject.Inject

data class SuggestionState(
    val pendingSuggestions: List<TransactionSuggestion> = emptyList(),
    val isLoading: Boolean = false,
    val totalSuggestions: Int = 0,
    val processedSuggestions: Int = 0,
    val accuracy: Float = 0f
)

data class TransactionSuggestion(
    val transaction: TransactionWithCategory,
    val suggestion: CategorySuggestion,
    val isProcessed: Boolean = false
)

@HiltViewModel
class CategorySuggestionViewModel @Inject constructor(
    private val categoryRepository: CategoryRepository,
    private val transactionRepository: TransactionRepository,
    private val preferenceRepository: UserCategoryPreferenceRepository
) : ViewModel() {
    
    private val _state = MutableStateFlow(SuggestionState())
    val state: StateFlow<SuggestionState> = _state.asStateFlow()
    
    init {
        loadPendingSuggestions()
        loadAccuracyStats()
    }
    
    private fun loadPendingSuggestions() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true)
            
            try {
                // Obtener transacciones sin categoría o con baja confianza
                val allTransactions = transactionRepository.getTransactionsWithCategoriesSync()
                val pendingSuggestions = mutableListOf<TransactionSuggestion>()
                
                for (transaction in allTransactions) {
                    // Solo procesar transacciones que podrían beneficiarse de sugerencias
                    val shouldSuggest = transaction.categoryId == null || 
                                       transaction.categoryName == "Otros"
                    
                    if (shouldSuggest) {
                        val suggestion: CategorySuggestion? = categoryRepository.getIntelligentCategorySuggestion(transaction.description, transaction.isIncome)
                        if (suggestion != null && suggestion.confidence > 0.3f) {
                            pendingSuggestions.add(
                                TransactionSuggestion(
                                    transaction = transaction,
                                    suggestion = suggestion
                                )
                            )
                        }
                    }
                }
                
                // Ordenar por confianza descendente
                pendingSuggestions.sortByDescending { it.suggestion.confidence }
                
                _state.value = _state.value.copy(
                    pendingSuggestions = pendingSuggestions,
                    totalSuggestions = pendingSuggestions.size,
                    isLoading = false
                )
                
                Log.d("SuggestionViewModel", "Loaded ${pendingSuggestions.size} pending suggestions")
                
            } catch (e: Exception) {
                Log.e("SuggestionViewModel", "Error loading suggestions", e)
                _state.value = _state.value.copy(isLoading = false)
            }
        }
    }
    
    private fun loadAccuracyStats() {
        viewModelScope.launch {
            try {
                val accuracy = preferenceRepository.getOverallAccuracy()
                _state.value = _state.value.copy(accuracy = accuracy)
            } catch (e: Exception) {
                Log.e("SuggestionViewModel", "Error loading accuracy stats", e)
            }
        }
    }
    
    fun acceptSuggestion(transactionId: String, suggestion: CategorySuggestion) {
        viewModelScope.launch {
            try {
                // Actualizar la transacción con la nueva categoría
                transactionRepository.updateTransactionCategory(transactionId, suggestion.categoryId)
                
                // Aprender de la decisión del usuario
                categoryRepository.learnFromUserCategoryChoice(
                    suggestion.merchantKey, 
                    suggestion.categoryId
                )
                
                // Marcar como procesada
                markSuggestionAsProcessed(transactionId)
                
                Log.d("SuggestionViewModel", "Accepted suggestion for $transactionId: ${suggestion.categoryName}")
                
            } catch (e: Exception) {
                Log.e("SuggestionViewModel", "Error accepting suggestion", e)
            }
        }
    }
    
    fun rejectSuggestion(transactionId: String, suggestion: CategorySuggestion, correctCategoryId: Long?) {
        viewModelScope.launch {
            try {
                if (correctCategoryId != null) {
                    // Usuario proporcionó la categoría correcta
                    transactionRepository.updateTransactionCategory(transactionId, correctCategoryId)
                    
                    // Aprender de la corrección
                    preferenceRepository.markSuggestionAsWrong(
                        suggestion.merchantKey,
                        correctCategoryId
                    )
                }
                
                // Marcar como procesada
                markSuggestionAsProcessed(transactionId)
                
                Log.d("SuggestionViewModel", "Rejected suggestion for $transactionId")
                
            } catch (e: Exception) {
                Log.e("SuggestionViewModel", "Error rejecting suggestion", e)
            }
        }
    }
    
    fun acceptAllSuggestions(suggestions: List<TransactionSuggestion>) {
        viewModelScope.launch {
            try {
                val highConfidenceSuggestions = suggestions.filter { it.suggestion.confidence > 0.7f }
                
                for (suggestionPair in highConfidenceSuggestions) {
                    acceptSuggestion(suggestionPair.transaction.id, suggestionPair.suggestion)
                }
                
                Log.d("SuggestionViewModel", "Accepted ${highConfidenceSuggestions.size} batch suggestions")
                
            } catch (e: Exception) {
                Log.e("SuggestionViewModel", "Error accepting batch suggestions", e)
            }
        }
    }
    
    private fun markSuggestionAsProcessed(transactionId: String) {
        val currentSuggestions = _state.value.pendingSuggestions.toMutableList()
        val index = currentSuggestions.indexOfFirst { it.transaction.id == transactionId }
        
        if (index != -1) {
            currentSuggestions[index] = currentSuggestions[index].copy(isProcessed = true)
            
            _state.value = _state.value.copy(
                pendingSuggestions = currentSuggestions.filter { !it.isProcessed },
                processedSuggestions = _state.value.processedSuggestions + 1
            )
        }
    }
    
    fun refreshSuggestions() {
        loadPendingSuggestions()
        loadAccuracyStats()
    }
    
    fun getHighConfidenceSuggestions(): List<TransactionSuggestion> {
        return _state.value.pendingSuggestions.filter { it.suggestion.confidence > 0.8f }
    }
    
    fun getMediumConfidenceSuggestions(): List<TransactionSuggestion> {
        return _state.value.pendingSuggestions.filter { 
            it.suggestion.confidence in 0.5f..0.8f 
        }
    }
    
    fun getLowConfidenceSuggestions(): List<TransactionSuggestion> {
        return _state.value.pendingSuggestions.filter { it.suggestion.confidence < 0.5f }
    }
}