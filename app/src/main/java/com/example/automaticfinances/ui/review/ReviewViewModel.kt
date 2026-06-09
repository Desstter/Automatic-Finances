package com.example.automaticfinances.ui.review

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.automaticfinances.data.db.Category
import com.example.automaticfinances.data.db.PendingTransaction
import com.example.automaticfinances.data.repo.CategoryRepository
import com.example.automaticfinances.data.repo.PendingTransactionRepository
import com.example.automaticfinances.domain.ConfirmPendingTransactionUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ReviewItemUi(
    val pending: PendingTransaction,
    val suggestedCategoryId: Long?,
    val categories: List<Category>
)

data class ReviewState(
    val items: List<ReviewItemUi> = emptyList(),
    val isLoading: Boolean = true
)

/**
 * Drives the "Por revisar" queue (PROD-1). Confirming runs the normal persistence path (balance
 * moves here, never at capture); discarding just drops the draft.
 */
@HiltViewModel
class ReviewViewModel @Inject constructor(
    private val pendingRepo: PendingTransactionRepository,
    private val categoryRepo: CategoryRepository,
    private val confirmUseCase: ConfirmPendingTransactionUseCase
) : ViewModel() {

    val state: StateFlow<ReviewState> = pendingRepo.observeAll()
        .map { list -> ReviewState(items = list.map { toUi(it) }, isLoading = false) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ReviewState())

    val count: StateFlow<Int> = pendingRepo.observeCount()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    private suspend fun toUi(p: PendingTransaction): ReviewItemUi = ReviewItemUi(
        pending = p,
        suggestedCategoryId = categoryRepo.getDefaultCategoryId(p.type, p.description),
        categories = categoryRepo.getActiveSyncByType(p.isIncome)
    )

    fun confirm(pending: PendingTransaction, selectedCategoryId: Long?, suggestedCategoryId: Long?) {
        viewModelScope.launch {
            confirmUseCase(pending, selectedCategoryId)
            // Feed the learning model only when the user actually corrected the suggestion, so the
            // preference store stays clean (an accepted default carries no signal).
            if (selectedCategoryId != null && selectedCategoryId != suggestedCategoryId) {
                categoryRepo.learnFromUserCategoryChoice(pending.description, selectedCategoryId)
            }
        }
    }

    fun discard(pending: PendingTransaction) {
        viewModelScope.launch { pendingRepo.delete(pending.id) }
    }
}
