package com.example.automaticfinances.ui.income

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.automaticfinances.data.db.Category
import com.example.automaticfinances.data.db.Transaction
import com.example.automaticfinances.data.repo.CategoryRepository
import com.example.automaticfinances.data.repo.TransactionRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.util.*

data class AddIncomeState(
    val amount: String = "",
    val description: String = "",
    val selectedDate: LocalDate = LocalDate.now(),
    val selectedTime: LocalTime = LocalTime.now(),
    val selectedCategoryId: Long? = null,
    val notes: String = "",
    val incomeCategories: List<Category> = emptyList(),
    val isLoading: Boolean = false,
    val isSuccess: Boolean = false,
    val error: String? = null,
    val amountError: String? = null,
    val descriptionError: String? = null
) {
    val canSave: Boolean
        get() = amount.isNotBlank() && 
                description.isNotBlank() && 
                selectedCategoryId != null && 
                amountError == null && 
                descriptionError == null &&
                !isLoading
}

class AddIncomeViewModel : ViewModel() {
    private val transactionRepository = TransactionRepository()
    private val categoryRepository = CategoryRepository()
    
    private val _state = MutableStateFlow(AddIncomeState())
    val state: StateFlow<AddIncomeState> = _state.asStateFlow()
    
    init {
        loadIncomeCategories()
    }
    
    private fun loadIncomeCategories() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true)
            try {
                categoryRepository.getAllActive().collectLatest { categories ->
                    // Filter categories that are typically for income based on their names
                    val incomeCategories = categories.filter { category ->
                        category.name.contains("Salario", ignoreCase = true) ||
                        category.name.contains("Freelance", ignoreCase = true) ||
                        category.name.contains("Ventas", ignoreCase = true) ||
                        category.name.contains("Regalos", ignoreCase = true) ||
                        category.name.contains("Inversiones", ignoreCase = true) ||
                        category.name.contains("Devoluciones", ignoreCase = true) ||
                        category.name.contains("Bonos", ignoreCase = true) ||
                        category.name.contains("ingresos", ignoreCase = true) ||
                        category.icon.contains("💰") ||
                        category.icon.contains("💼") ||
                        category.icon.contains("🏪") ||
                        category.icon.contains("🎁") ||
                        category.icon.contains("📈") ||
                        category.icon.contains("💸") ||
                        category.icon.contains("🎯") ||
                        category.icon.contains("📋")
                    }
                    
                    _state.value = _state.value.copy(
                        incomeCategories = incomeCategories,
                        isLoading = false,
                        selectedCategoryId = if (_state.value.selectedCategoryId == null && incomeCategories.isNotEmpty()) {
                            incomeCategories.firstOrNull { it.name.contains("Salario", ignoreCase = true) }?.id
                                ?: incomeCategories.first().id
                        } else {
                            _state.value.selectedCategoryId
                        }
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
    
    fun setAmount(amount: String) {
        val cleanAmount = amount.filter { it.isDigit() || it == '.' || it == ',' }
        val amountError = validateAmount(cleanAmount)
        
        _state.value = _state.value.copy(
            amount = cleanAmount,
            amountError = amountError
        )
    }
    
    fun setDescription(description: String) {
        val descriptionError = validateDescription(description)
        
        _state.value = _state.value.copy(
            description = description,
            descriptionError = descriptionError
        )
    }
    
    fun setDate(date: LocalDate) {
        _state.value = _state.value.copy(selectedDate = date)
    }
    
    fun setTime(time: LocalTime) {
        _state.value = _state.value.copy(selectedTime = time)
    }
    
    fun setCategory(categoryId: Long) {
        _state.value = _state.value.copy(selectedCategoryId = categoryId)
    }
    
    fun setNotes(notes: String) {
        _state.value = _state.value.copy(notes = notes)
    }
    
    fun saveIncome() {
        val currentState = _state.value
        if (!currentState.canSave) return
        
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)
            
            try {
                val amountCents = parseAmountToCents(currentState.amount)
                val timestamp = currentState.selectedDate
                    .atTime(currentState.selectedTime)
                    .atZone(ZoneId.of("America/Bogota"))
                    .toInstant()
                    .toEpochMilli()
                
                val transactionId = generateTransactionId(
                    timestamp, 
                    amountCents, 
                    currentState.description
                )
                
                val transaction = Transaction.fromTimestamp(
                    id = transactionId,
                    ts = timestamp,
                    type = "INGRESO",
                    description = currentState.description,
                    amountCents = amountCents,
                    currency = "COP",
                    srcLast4 = null,
                    dstLast4 = null,
                    source = "manual",
                    rawPreview = "Ingreso manual: ${currentState.description}",
                    categoryId = currentState.selectedCategoryId,
                    isIncome = true
                )
                
                // Save to database
                transactionRepository.insert(transaction)
                
                // Learn from user category choice for intelligent categorization
                categoryRepository.learnFromUserCategoryChoice(
                    currentState.description.lowercase().trim(),
                    currentState.selectedCategoryId!!
                )
                
                _state.value = _state.value.copy(
                    isLoading = false,
                    isSuccess = true
                )
                
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    isLoading = false,
                    error = "Error al guardar ingreso: ${e.message}"
                )
            }
        }
    }
    
    fun resetSuccess() {
        _state.value = _state.value.copy(isSuccess = false)
    }
    
    private fun validateAmount(amount: String): String? {
        if (amount.isBlank()) {
            return "El monto es requerido"
        }
        
        return try {
            val value = parseAmountToCents(amount)
            if (value <= 0) {
                "El monto debe ser mayor a $0"
            } else if (value > 999999999999L) { // 9.999.999.999,99 COP max
                "El monto es demasiado alto"
            } else {
                null
            }
        } catch (e: Exception) {
            "Monto inválido"
        }
    }
    
    private fun validateDescription(description: String): String? {
        return when {
            description.isBlank() -> "La descripción es requerida"
            description.length < 2 -> "La descripción debe tener al menos 2 caracteres"
            description.length > 60 -> "La descripción no puede superar 60 caracteres"
            else -> null
        }
    }
    
    private fun parseAmountToCents(amount: String): Long {
        // Handle Colombian number format: 1.000.000,50 or 1,000,000.50
        val cleaned = amount.replace(".", "").replace(",", "")
        return (cleaned.toDouble() * 100).toLong()
    }
    
    private fun generateTransactionId(timestamp: Long, amountCents: Long, description: String): String {
        val data = "${timestamp/60000}|$amountCents|INGRESO|manual|${description.take(20)}"
        return org.apache.commons.codec.digest.DigestUtils.sha256Hex(data)
    }
}