package com.example.automaticfinances.domain

import com.example.automaticfinances.data.repo.CategoryRepository
import com.example.automaticfinances.data.repo.TransactionRepository
import javax.inject.Inject

/**
 * Applies a category chosen from the capture-feedback notification (PROD-2): retag the transaction
 * and feed the choice back into the learning layer ([UserCategoryPreference]) so future captures from
 * the same merchant categorize correctly without asking again.
 *
 * Changing a category never touches the balance — the balance is derived from amount + isIncome +
 * account, all of which stay the same — so this needs no transaction wrapper or balance recompute.
 */
class ApplyCategoryFeedbackUseCase @Inject constructor(
    private val transactionRepo: TransactionRepository,
    private val categoryRepo: CategoryRepository,
) {
    suspend operator fun invoke(transactionId: String, categoryId: Long, merchant: String) {
        // Guard against a stale notification action firing after the row was deleted.
        if (transactionRepo.getById(transactionId) == null) return
        transactionRepo.updateTransactionCategory(transactionId, categoryId)
        if (merchant.isNotBlank()) {
            categoryRepo.learnFromUserCategoryChoice(merchant, categoryId)
        }
    }
}
