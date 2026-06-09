package com.example.automaticfinances.ui.transaction

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.automaticfinances.data.db.Account
import com.example.automaticfinances.data.db.Category
import com.example.automaticfinances.data.db.Transaction
import com.example.automaticfinances.data.repo.AccountRepository
import com.example.automaticfinances.data.repo.CategoryRepository
import com.example.automaticfinances.data.repo.MerchantResolutionRepository
import com.example.automaticfinances.data.repo.TransactionRepository
import com.example.automaticfinances.domain.DeleteTransactionUseCase
import com.example.automaticfinances.domain.RestoreTransactionUseCase
import com.example.automaticfinances.domain.UpdateTransactionUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class TransactionDetailState(
    val transaction: Transaction? = null,
    val categories: List<Category> = emptyList(),
    val accounts: List<Account> = emptyList(),
    val selectedCategoryId: Long? = null,
    val selectedAccountId: Long? = null,
    val description: String = "",
    val notes: String = "",
    // Transfer editing: when the opened transaction is one leg of a transfer, both legs are loaded
    // and the user edits the origin/destination accounts rather than a single account.
    val isTransfer: Boolean = false,
    val transferLegs: List<Transaction> = emptyList(),
    val originAccountId: Long? = null,
    val destAccountId: Long? = null,
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val error: String? = null,
    val isEditMode: Boolean = false,
    val isDeleting: Boolean = false,
    val showDeleteConfirmation: Boolean = false,
    val deletedTransactions: List<Transaction> = emptyList(),  // For undo (1 row, or 2 transfer legs)
    val isDeleted: Boolean = false
)

@HiltViewModel
class TransactionDetailViewModel @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val categoryRepository: CategoryRepository,
    private val accountRepository: AccountRepository,
    private val merchantResolutionRepository: MerchantResolutionRepository,
    private val deleteTransactionUseCase: DeleteTransactionUseCase,
    private val restoreTransactionUseCase: RestoreTransactionUseCase,
    private val updateTransactionUseCase: UpdateTransactionUseCase
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
                    val accounts = accountRepository.getAllActiveAccounts()

                    // For a transfer, load both legs so origin/destination can be edited as a pair.
                    val legs = transaction.transferGroupId
                        ?.let { transactionRepository.getByTransferGroupId(it) }
                        ?.takeIf { it.isNotEmpty() }
                        .orEmpty()
                    val isTransfer = transaction.isTransfer && legs.size == 2
                    val originLeg = legs.firstOrNull { !it.isIncome }
                    val destLeg = legs.firstOrNull { it.isIncome }

                    _state.value = _state.value.copy(
                        transaction = transaction,
                        categories = categories,
                        accounts = accounts,
                        selectedCategoryId = transaction.categoryId,
                        selectedAccountId = transaction.accountId,
                        description = transaction.description,
                        notes = transaction.notes,
                        isTransfer = isTransfer,
                        transferLegs = legs,
                        originAccountId = originLeg?.accountId,
                        destAccountId = destLeg?.accountId,
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
        val originLeg = _state.value.transferLegs.firstOrNull { !it.isIncome }
        val destLeg = _state.value.transferLegs.firstOrNull { it.isIncome }
        _state.value = _state.value.copy(
            isEditMode = false,
            selectedCategoryId = transaction.categoryId,
            selectedAccountId = transaction.accountId,
            originAccountId = originLeg?.accountId,
            destAccountId = destLeg?.accountId,
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

    fun selectAccount(accountId: Long) {
        _state.value = _state.value.copy(selectedAccountId = accountId)
    }

    fun selectOriginAccount(accountId: Long) {
        val s = _state.value
        val newDest = if (s.destAccountId == accountId) s.originAccountId else s.destAccountId
        _state.value = s.copy(originAccountId = accountId, destAccountId = newDest)
    }

    fun selectDestAccount(accountId: Long) {
        val s = _state.value
        val newOrigin = if (s.originAccountId == accountId) s.destAccountId else s.originAccountId
        _state.value = s.copy(destAccountId = accountId, originAccountId = newOrigin)
    }

    fun accountName(accountId: Long?): String =
        _state.value.accounts.find { it.id == accountId }?.name ?: "Cuenta"

    fun saveChanges() {
        val currentState = _state.value
        val transaction = currentState.transaction ?: return

        if (currentState.isTransfer) {
            saveTransferChanges(currentState)
            return
        }

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
                    categoryId = currentState.selectedCategoryId,
                    accountId = currentState.selectedAccountId
                )

                // Persist + recompute balances of the old and new account (if the account changed),
                // atomically, keeping the derived-balance invariant.
                updateTransactionUseCase(transaction, updatedTransaction)

                // If this transaction still carries a raw gateway name (an unknown gateway we
                // couldn't resolve at insert time), remember the user's correction so future
                // charges from the same gateway resolve to the real merchant + category
                // automatically. Known gateways were already rewritten to the real merchant on
                // insert, so isGatewayMerchant is false for them and this is a no-op.
                if (merchantResolutionRepository.isGatewayMerchant(transaction.description)) {
                    merchantResolutionRepository.learn(
                        gatewayMerchant = transaction.description,
                        realMerchant = updatedTransaction.description,
                        categoryId = updatedTransaction.categoryId
                    )
                }

                _state.value = _state.value.copy(
                    transaction = updatedTransaction,
                    selectedAccountId = updatedTransaction.accountId,
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

    private fun saveTransferChanges(currentState: TransactionDetailState) {
        val originId = currentState.originAccountId
        val destId = currentState.destAccountId
        val originLeg = currentState.transferLegs.firstOrNull { !it.isIncome }
        val destLeg = currentState.transferLegs.firstOrNull { it.isIncome }

        if (originId == null || destId == null || originLeg == null || destLeg == null) {
            _state.value = currentState.copy(error = "Transferencia incompleta")
            return
        }
        if (originId == destId) {
            _state.value = currentState.copy(error = "El origen y el destino deben ser diferentes")
            return
        }

        viewModelScope.launch {
            _state.value = currentState.copy(isSaving = true, error = null)
            try {
                val originName = accountName(originId)
                val destName = accountName(destId)
                val note = currentState.notes.trim()

                val updatedOrigin = originLeg.copy(
                    accountId = originId,
                    description = "Transferencia a $destName",
                    notes = note
                )
                val updatedDest = destLeg.copy(
                    accountId = destId,
                    description = "Transferencia desde $originName",
                    notes = note
                )

                updateTransactionUseCase.updateGroup(
                    listOf(originLeg to updatedOrigin, destLeg to updatedDest)
                )

                // Reflect the edited leg back in the header. The opened transaction is whichever leg
                // the user navigated in on; keep showing that same leg.
                val refreshedShown = if (currentState.transaction?.isIncome == true) updatedDest else updatedOrigin
                _state.value = _state.value.copy(
                    transaction = refreshedShown,
                    transferLegs = listOf(updatedOrigin, updatedDest),
                    notes = note,
                    isSaving = false,
                    isEditMode = false
                )
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    isSaving = false,
                    error = "Error al guardar transferencia: ${e.message}"
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

        // Delete both legs together for a transfer so the two account balances stay consistent.
        val toDelete = if (currentState.isTransfer && currentState.transferLegs.size == 2) {
            currentState.transferLegs
        } else {
            listOf(transaction)
        }

        viewModelScope.launch {
            _state.value = currentState.copy(
                isDeleting = true,
                showDeleteConfirmation = false,
                error = null
            )

            try {
                val success = deleteTransactionUseCase.deleteGroup(toDelete)

                if (success) {
                    _state.value = _state.value.copy(
                        isDeleting = false,
                        isDeleted = true,
                        deletedTransactions = toDelete  // Store for undo
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
        val deleted = _state.value.deletedTransactions
        if (deleted.isEmpty()) return

        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)

            try {
                restoreTransactionUseCase.restoreGroup(deleted)

                _state.value = _state.value.copy(
                    transaction = _state.value.transaction,
                    isLoading = false,
                    isDeleted = false,
                    deletedTransactions = emptyList()
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
