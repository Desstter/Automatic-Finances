package com.example.automaticfinances.ui.transaction

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.automaticfinances.data.db.Account
import com.example.automaticfinances.data.db.AccountType
import com.example.automaticfinances.data.db.Category
import com.example.automaticfinances.data.db.Transaction
import com.example.automaticfinances.data.repo.AccountRepository
import com.example.automaticfinances.data.repo.CategoryRepository
import com.example.automaticfinances.domain.AddTransactionUseCase
import com.example.automaticfinances.utils.parseColombiaCents
import dagger.hilt.android.lifecycle.HiltViewModel
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
import javax.inject.Inject

data class AddTransactionState(
    val amount: String = "",
    val description: String = "",
    val selectedCategoryId: Long? = null,
    val selectedDate: LocalDate = LocalDate.now(),
    val selectedTime: LocalTime = LocalTime.now(),
    val categories: List<Category> = emptyList(),
    val accounts: List<Account> = emptyList(),
    val selectedAccountId: Long? = null,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val isSuccess: Boolean = false,
    val amountError: String? = null,
    val descriptionError: String? = null
) {
    val selectedAccount: Account?
        get() = accounts.find { it.id == selectedAccountId }
}

@HiltViewModel
class AddTransactionViewModel @Inject constructor(
    private val categoryRepository: CategoryRepository,
    private val accountRepository: AccountRepository,
    private val addTransactionUseCase: AddTransactionUseCase
) : ViewModel() {

    private val _state = MutableStateFlow(AddTransactionState())
    val state: StateFlow<AddTransactionState> = _state.asStateFlow()

    init {
        loadCategories()
        loadAccounts()
    }

    private fun loadCategories() {
        viewModelScope.launch {
            // Manual expenses only apply to expense categories.
            categoryRepository.getActiveByType(isIncome = false).collect { categories ->
                _state.value = _state.value.copy(categories = categories)
            }
        }
    }

    private fun loadAccounts() {
        viewModelScope.launch {
            try {
                val accounts = accountRepository.getAllActiveAccounts()
                _state.value = _state.value.copy(
                    accounts = accounts,
                    // Default to the cash account to preserve the previous behaviour
                    // (this screen used to record cash-only expenses).
                    selectedAccountId = _state.value.selectedAccountId
                        ?: accounts.firstOrNull { it.type == AccountType.CASH }?.id
                        ?: accounts.firstOrNull()?.id
                )
            } catch (e: Exception) {
                Log.e("AddTransactionViewModel", "Error loading accounts", e)
            }
        }
    }

    fun selectAccount(accountId: Long) {
        _state.value = _state.value.copy(selectedAccountId = accountId)
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
        // Only cap the length here. Do NOT trim() on every keystroke: trimming as the user
        // types swallows the trailing space, making it impossible to type multi-word
        // descriptions. Leading/trailing whitespace is trimmed once, at save time.
        val cleanDescription = description.take(100)

        // Real-time validation
        val error = validateDescription(cleanDescription)

        _state.value = _state.value.copy(
            description = cleanDescription,
            descriptionError = if (cleanDescription.isNotBlank()) error else null
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
        
        // Comprehensive validation (description validated trimmed — leading/trailing
        // spaces shouldn't count toward the min length nor be flagged as content).
        val amountError = validateAmount(currentState.amount, isFinal = true)
        val descriptionError = validateDescription(currentState.description.trim(), isFinal = true)
        val categoryError = validateCategory(currentState.selectedCategoryId)
        val accountError = validateAccount(currentState.selectedAccountId)
        val dateError = validateDate(currentState.selectedDate)

        val hasErrors = listOf(amountError, descriptionError, categoryError, accountError, dateError).any { it != null }

        if (hasErrors) {
            _state.value = _state.value.copy(
                amountError = amountError,
                descriptionError = descriptionError,
                errorMessage = categoryError ?: accountError ?: dateError
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
        val amountCents = state.amount.parseColombiaCents() ?: 0L
        val description = state.description.trim()
        val dateTime = state.selectedDate.atTime(state.selectedTime)
        val timestamp = dateTime.atZone(ZoneId.of("America/Bogota")).toInstant().toEpochMilli()

        // The account is part of the dedup hash so the same expense recorded against two
        // different accounts (e.g. cash vs. bank) produces distinct rows.
        val account = state.selectedAccount
        val srcLast4 = if (account?.type == AccountType.BANK) "BANK" else "CASH"

        // Crear ID único basado en timestamp, monto, cuenta y descripción
        val id = DigestUtils.sha256Hex(
            "${timestamp / 60000}|$amountCents|MANUAL|${state.selectedAccountId}|$description"
        )

        return Transaction.fromTimestamp(
            id = id,
            ts = timestamp,
            type = "MANUAL",
            description = description,
            amountCents = amountCents,
            currency = "COP",
            srcLast4 = srcLast4,
            dstLast4 = null,
            source = "manual",
            rawPreview = "Gasto manual: $description",
            categoryId = state.selectedCategoryId,
            // Pass the chosen account through explicitly so AddTransactionUseCase keeps it
            // instead of auto-assigning by source. This is what lets a manual expense be
            // tied to a bank account rather than always landing in cash.
            accountId = state.selectedAccountId
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

    private fun validateAccount(accountId: Long?): String? {
        return when {
            accountId == null -> "Selecciona una cuenta"
            accountId <= 0 -> "Cuenta inválida"
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
     * Amount in pesos for validation only. Delegates to [parseColombiaCents] (the single
     * source of truth for COP parsing) and converts cents back to pesos. Returns null when
     * the input can't be parsed.
     */
    private fun parseAmount(amount: String): Double? {
        return amount.parseColombiaCents()?.let { it / 100.0 }
    }
}