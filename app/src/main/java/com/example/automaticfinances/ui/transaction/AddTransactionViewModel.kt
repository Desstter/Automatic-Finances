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
        // Input sanitization: solo números, punto decimal, y comas
        val cleanAmount = amount.filter { it.isDigit() || it == '.' || it == ',' }
            .take(15) // Limitar longitud para evitar overflow
        
        // Real-time validation
        val error = validateAmount(cleanAmount)
        
        _state.value = _state.value.copy(
            amount = cleanAmount,
            amountError = if (cleanAmount.isNotEmpty()) error else null
        )
    }

    fun updateDescription(description: String) {
        // Input sanitization: trimear y limitar longitud
        val cleanDescription = description.trim().take(100)
        
        // Real-time validation
        val error = validateDescription(cleanDescription)
        
        _state.value = _state.value.copy(
            description = cleanDescription,
            descriptionError = if (cleanDescription.isNotEmpty()) error else null
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
        
        // Comprehensive validation
        val amountError = validateAmount(currentState.amount, isFinal = true)
        val descriptionError = validateDescription(currentState.description, isFinal = true)
        val categoryError = validateCategory(currentState.selectedCategoryId)
        val dateError = validateDate(currentState.selectedDate)
        
        val hasErrors = listOf(amountError, descriptionError, categoryError, dateError).any { it != null }

        if (hasErrors) {
            _state.value = _state.value.copy(
                amountError = amountError,
                descriptionError = descriptionError,
                errorMessage = categoryError ?: dateError
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
        val amountDouble = parseAmount(state.amount) ?: 0.0
        val amountCents = (amountDouble * 100).toLong()
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
    
    // =======================================
    // VALIDATION FUNCTIONS
    // =======================================
    
    private fun validateAmount(amount: String, isFinal: Boolean = false): String? {
        return when {
            isFinal && amount.isBlank() -> "Ingresa un monto"
            amount.isBlank() -> null // Allow empty during typing
            
            // Check for invalid characters or format
            !amount.matches(Regex("^\\d*[.,]?\\d*$")) -> "Solo números y punto/coma decimal"
            
            // Check for multiple decimal separators
            amount.count { it == '.' || it == ',' } > 1 -> "Solo un separador decimal"
            
            // Try to parse as number
            parseAmount(amount) == null -> "Formato de monto inválido"
            
            // Check range
            parseAmount(amount)!! <= 0 -> "El monto debe ser mayor a cero"
            parseAmount(amount)!! > 999_999_999.99 -> "Monto excede límite máximo"
            
            else -> null
        }
    }
    
    private fun validateDescription(description: String, isFinal: Boolean = false): String? {
        return when {
            isFinal && description.isBlank() -> "Ingresa una descripción"
            description.isBlank() -> null // Allow empty during typing
            
            description.length < 3 && isFinal -> "Descripción muy corta (min 3 caracteres)"
            description.length > 100 -> "Descripción muy larga (max 100 caracteres)"
            
            // Check for suspicious patterns
            description.matches(Regex("^\\s*$")) -> "Descripción no puede estar vacía"
            description.contains(Regex("[<>\"'&]")) -> "Caracteres no permitidos: < > \" ' &"
            
            // Check for SQL injection patterns (basic)
            description.lowercase().contains("select ") || 
            description.lowercase().contains("drop ") ||
            description.lowercase().contains("delete ") -> "Contenido no permitido"
            
            else -> null
        }
    }
    
    private fun validateCategory(categoryId: Long?): String? {
        return when {
            categoryId == null -> "Selecciona una categoría"
            categoryId <= 0 -> "Categoría inválida"
            else -> null
        }
    }
    
    private fun validateDate(date: LocalDate): String? {
        val today = LocalDate.now()
        return when {
            date.isAfter(today.plusDays(1)) -> "La fecha no puede ser futura"
            date.isBefore(today.minusYears(5)) -> "Fecha muy antigua (max 5 años)"
            else -> null
        }
    }
    
    /**
     * Parse amount string handling Colombian format (comma as decimal separator)
     * Supports: "1000", "1.000", "1000,50", "1.000,50"
     */
    private fun parseAmount(amount: String): Double? {
        if (amount.isBlank()) return null
        
        return try {
            // Handle Colombian format: 1.000,50 or 1000,50
            val normalized = if (amount.contains(',')) {
                // Has comma - treat as decimal separator
                val parts = amount.split(',')
                if (parts.size != 2) return null
                
                val integerPart = parts[0].replace(".", "") // Remove thousand separators
                val decimalPart = parts[1]
                
                "$integerPart.$decimalPart".toDouble()
            } else {
                // No comma - could be 1000 or 1000.50 (US format)
                amount.replace(".", "").toDouble() / 
                    if (amount.count { it == '.' } == 1 && amount.substringAfter('.').length <= 2) 1.0 else 1.0
            }
            
            normalized
        } catch (e: NumberFormatException) {
            null
        }
    }
}