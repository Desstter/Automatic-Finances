package com.example.automaticfinances.ui.transaction

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.automaticfinances.data.db.Category
import com.example.automaticfinances.data.db.Transaction
import com.example.automaticfinances.data.repo.CategoryRepository
import com.example.automaticfinances.domain.AddTransactionUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import org.apache.commons.codec.digest.DigestUtils
import android.util.Log
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

data class AddTransactionState(
    val amount: String = "",
    val description: String = "",
    val selectedCategoryId: Long? = null,
    val selectedDate: LocalDate = LocalDate.now(),
    val selectedTime: LocalTime = LocalTime.now(),
    val categories: List<Category> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val isSuccess: Boolean = false,
    val amountError: String? = null,
    val descriptionError: String? = null
)

class AddTransactionViewModel(
    private val categoryRepository: CategoryRepository = CategoryRepository(),
    private val addTransactionUseCase: AddTransactionUseCase = AddTransactionUseCase()
) : ViewModel() {

    private val _state = MutableStateFlow(AddTransactionState())
    val state: StateFlow<AddTransactionState> = _state.asStateFlow()

    init {
        loadCategories()
    }

    private fun loadCategories() {
        viewModelScope.launch {
            categoryRepository.getAllActive().collect { categories ->
                _state.value = _state.value.copy(categories = categories)
            }
        }
    }

    fun updateAmount(amount: String) {
        // Solo permitir números y punto decimal
        val cleanAmount = amount.filter { it.isDigit() || it == '.' }
        _state.value = _state.value.copy(
            amount = cleanAmount,
            amountError = null
        )
    }

    fun updateDescription(description: String) {
        _state.value = _state.value.copy(
            description = description,
            descriptionError = null
        )
    }

    fun selectCategory(categoryId: Long) {
        _state.value = _state.value.copy(selectedCategoryId = categoryId)
    }

    fun updateDate(date: LocalDate) {
        _state.value = _state.value.copy(selectedDate = date)
    }

    fun updateTime(time: LocalTime) {
        _state.value = _state.value.copy(selectedTime = time)
    }

    fun saveTransaction(): Boolean {
        val currentState = _state.value
        
        // Validations
        val amountError = when {
            currentState.amount.isBlank() -> "Ingresa un monto"
            currentState.amount.toDoubleOrNull() == null -> "Monto inválido"
            currentState.amount.toDouble() <= 0 -> "El monto debe ser mayor a cero"
            else -> null
        }

        val descriptionError = when {
            currentState.description.isBlank() -> "Ingresa una descripción"
            currentState.description.length > 60 -> "Descripción muy larga (max 60 caracteres)"
            else -> null
        }

        if (amountError != null || descriptionError != null) {
            _state.value = _state.value.copy(
                amountError = amountError,
                descriptionError = descriptionError
            )
            return false
        }

        _state.value = _state.value.copy(isLoading = true, errorMessage = null)

        viewModelScope.launch {
            try {
                val transaction = createTransaction(currentState)
                Log.d("AddTransactionViewModel", "Saving transaction: $transaction")
                addTransactionUseCase(transaction)
                Log.d("AddTransactionViewModel", "Transaction saved successfully")
                _state.value = _state.value.copy(
                    isLoading = false,
                    isSuccess = true
                )
            } catch (e: Exception) {
                Log.e("AddTransactionViewModel", "Error saving transaction", e)
                _state.value = _state.value.copy(
                    isLoading = false,
                    errorMessage = "Error al guardar: ${e.message}"
                )
            }
        }

        return true
    }

    private fun createTransaction(state: AddTransactionState): Transaction {
        val amountCents = (state.amount.toDouble() * 100).toLong()
        val dateTime = state.selectedDate.atTime(state.selectedTime)
        val timestamp = dateTime.atZone(ZoneId.of("America/Bogota")).toInstant().toEpochMilli()
        
        // Crear ID único basado en timestamp, monto y descripción
        val id = DigestUtils.sha256Hex("${timestamp/60000}|$amountCents|MANUAL|${state.description}")
        
        return Transaction.fromTimestamp(
            id = id,
            ts = timestamp,
            type = "MANUAL",
            description = state.description,
            amountCents = amountCents,
            currency = "COP",
            srcLast4 = "CASH",
            dstLast4 = null,
            source = "manual",
            rawPreview = "Gasto manual: ${state.description}",
            categoryId = state.selectedCategoryId
        )
    }

    fun clearError() {
        _state.value = _state.value.copy(errorMessage = null)
    }

    fun resetSuccess() {
        _state.value = _state.value.copy(isSuccess = false)
    }
}