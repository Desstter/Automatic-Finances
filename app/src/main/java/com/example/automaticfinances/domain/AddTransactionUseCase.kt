package com.example.automaticfinances.domain

import com.example.automaticfinances.data.db.Transaction
import com.example.automaticfinances.data.repo.TransactionRepository
import com.example.automaticfinances.data.repo.AccountRepository
import com.example.automaticfinances.data.repo.CategoryRepository
import com.example.automaticfinances.data.repo.MerchantResolutionRepository
import javax.inject.Inject

class AddTransactionUseCase @Inject constructor(
    private val transactionRepo: TransactionRepository,
    private val accountRepo: AccountRepository,
    private val categoryRepo: CategoryRepository,
    private val merchantResolutionRepo: MerchantResolutionRepository,
    private val transactionRunner: TransactionRunner
) {
    suspend operator fun invoke(tx: Transaction) {
        // Enrich with account + category. The parser produces "pure" transactions (no DB
        // coupling), so account and category are resolved here. Pre-set values (e.g. from a
        // manual entry where the user picked a category) are always preserved.
        val enriched = enrich(tx)

        // The insert and its balance adjustment must commit together. Wrapping them in a single
        // transaction guarantees the financial invariant survives a crash/failure mid-operation:
        // we never end up with a persisted row whose balance effect was lost, nor (for a RETIRO)
        // a bank leg applied without its matching cash leg.
        transactionRunner.runInTransaction {
            // Handle special case: RETIRO (ATM withdrawal)
            if (enriched.type == "RETIRO") {
                handleWithdrawalTransfer(enriched)
            } else {
                // Normal transaction processing.
                // Only adjust the balance if the row was actually inserted; a re-delivered
                // notification with the same id is ignored and must NOT touch the balance again.
                val inserted = transactionRepo.insert(enriched)
                if (inserted) {
                    accountRepo.applyTransactionToBalance(enriched)
                }
            }
        }
    }

    /**
     * Resolves the account (from source) and category (from type + description) when they
     * are not already provided. Keeps any caller-supplied account/category untouched.
     */
    private suspend fun enrich(tx: Transaction): Transaction {
        val withAccount = if (tx.accountId == null) {
            accountRepo.assignAccountToTransaction(tx)
        } else {
            tx
        }

        // Gateway resolution: a charge routed through a payment gateway shows the gateway in the
        // SMS (e.g. "PAYU*NETFLIX") instead of the real merchant. Translate it to the real
        // merchant name so reports read cleanly, and reuse the curated category it carries.
        // We try the full normalized name first, then fall back to the gateway base (prefix +
        // first word) so "PAYU*NETFLIX BOGOTA" still resolves to the "PAYU*NETFLIX" mapping.
        val resolution = if (merchantResolutionRepo.isGatewayMerchant(withAccount.description)) {
            merchantResolutionRepo.resolve(withAccount.description)
                ?: merchantResolutionRepo.resolve(
                    merchantResolutionRepo.extractGatewayBase(withAccount.description)
                )
        } else {
            null
        }
        val withMerchant = if (resolution != null) {
            withAccount.copy(description = resolution.realMerchant)
        } else {
            withAccount
        }

        // Category resolution only when the caller didn't already set one (e.g. manual entry).
        if (withMerchant.categoryId != null) return withMerchant

        val categoryId = if (resolution?.suggestedCategoryId != null) {
            // A learned user correction (keyed on the real merchant) always wins over the
            // curated gateway category; otherwise fall back to the curated suggestion.
            categoryRepo.getLearnedCategoryId(withMerchant.description)
                ?: resolution.suggestedCategoryId
        } else {
            categoryRepo.getDefaultCategoryId(withMerchant.type, withMerchant.description)
        }
        return withMerchant.copy(categoryId = categoryId)
    }
    
    /**
     * Handles ATM withdrawals as transfers from Bank to Cash
     * Creates two transactions: withdrawal from bank + deposit to cash
     */
    private suspend fun handleWithdrawalTransfer(withdrawal: Transaction) {
        // Get bank and cash accounts
        val bankAccount = accountRepo.getBankAccount()
        val cashAccount = accountRepo.getCashAccount()
        
        if (bankAccount == null || cashAccount == null) {
            // Fallback: treat as normal expense if accounts don't exist
            if (transactionRepo.insert(withdrawal)) {
                accountRepo.applyTransactionToBalance(withdrawal)
            }
            return
        }
        
        // 1. Create withdrawal transaction (from bank)
        val bankWithdrawal = withdrawal.copy(
            id = "${withdrawal.id}_BANK",
            accountId = bankAccount.id,
            type = "GASTO", // Treat as expense from bank
            description = withdrawal.description
        )
        
        // 2. Create corresponding cash deposit transaction
        val cashDeposit = Transaction.fromTimestamp(
            id = "${withdrawal.id}_CASH",
            ts = withdrawal.ts,
            type = "INGRESO",
            description = "Efectivo - ${withdrawal.description}",
            amountCents = withdrawal.amountCents,
            currency = withdrawal.currency,
            srcLast4 = "BANK",
            dstLast4 = "CASH",
            source = withdrawal.source,
            rawPreview = withdrawal.rawPreview,
            categoryId = categoryRepo.getDefaultCategoryId("INGRESO", "Efectivo"),
            accountId = cashAccount.id,
            isIncome = true
        )
        
        // Insert both transactions and only apply each balance change if its row was new.
        if (transactionRepo.insert(bankWithdrawal)) {
            accountRepo.applyTransactionToBalance(bankWithdrawal)
        }
        if (transactionRepo.insert(cashDeposit)) {
            accountRepo.applyTransactionToBalance(cashDeposit)
        }
    }
}