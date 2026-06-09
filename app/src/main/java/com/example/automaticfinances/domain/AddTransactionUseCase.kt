package com.example.automaticfinances.domain

import com.example.automaticfinances.data.db.Transaction
import com.example.automaticfinances.data.repo.TransactionRepository
import com.example.automaticfinances.data.repo.AccountRepository
import com.example.automaticfinances.data.repo.CategoryRepository
import com.example.automaticfinances.data.repo.MerchantResolutionRepository
import com.example.automaticfinances.data.repo.OpeningBalanceRepository
import javax.inject.Inject

class AddTransactionUseCase @Inject constructor(
    private val transactionRepo: TransactionRepository,
    private val accountRepo: AccountRepository,
    private val categoryRepo: CategoryRepository,
    private val merchantResolutionRepo: MerchantResolutionRepository,
    private val openingBalanceRepo: OpeningBalanceRepository,
    private val transactionRunner: TransactionRunner
) {
    /**
     * Persists [tx] (enriched with account + category) and adjusts the affected balance.
     *
     * @return the enriched transaction that was actually inserted, or null if nothing new was
     *   persisted — i.e. a re-delivered duplicate (insert ignored) or a RETIRO (handled as a dual
     *   entry, whose category is fixed and not worth surfacing for feedback). Callers that want to
     *   react to a genuinely-new capture (e.g. the PROD-2 category-chips notification) key off this.
     */
    suspend operator fun invoke(tx: Transaction): Transaction? {
        // Enrich with account + category. The parser produces "pure" transactions (no DB
        // coupling), so account and category are resolved here. Pre-set values (e.g. from a
        // manual entry where the user picked a category) are always preserved.
        val enriched = enrich(tx)

        // The insert and the balance recompute must commit together. Wrapping them in a single
        // transaction guarantees the financial invariant survives a crash/failure mid-operation:
        // we never end up with a persisted row whose balance effect was lost, nor (for a RETIRO)
        // a bank leg applied without its matching cash leg.
        return transactionRunner.runInTransaction {
            // Handle special case: RETIRO (ATM withdrawal)
            if (enriched.type == "RETIRO") {
                handleWithdrawalTransfer(enriched)
                null
            } else {
                // Normal transaction processing.
                // Only recompute the balance if the row was actually inserted; a re-delivered
                // notification with the same id is ignored and must NOT change the balance.
                val inserted = transactionRepo.insert(enriched)
                if (inserted) {
                    enriched.accountId?.let { openingBalanceRepo.recalculateAccountBalance(it) }
                    enriched
                } else {
                    null
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
     * Handles ATM withdrawals as an internal transfer from Bank to Cash, mirroring [TransferUseCase].
     *
     * Creates two legs sharing a [Transaction.transferGroupId]:
     *  - a bank leg (money leaves the bank: `isIncome = false` → balance −), and
     *  - a cash leg (money enters cash: `isIncome = true` → balance +).
     *
     * Both legs are flagged `isTransfer = true`, which (a) excludes them from every income/expense
     * total, category total and chart (all those `TransactionDao` queries filter `isTransfer = 0`) —
     * a withdrawal is neither a real expense nor a real income, just a relocation of money — while
     * (b) still moving each account's derived balance (the balance recompute does NOT filter
     * transfers), and (c) letting the detail screen edit/delete/restore the pair atomically via the
     * shared group id.
     */
    private suspend fun handleWithdrawalTransfer(withdrawal: Transaction) {
        // Get bank and cash accounts
        val bankAccount = accountRepo.getBankAccount()
        val cashAccount = accountRepo.getCashAccount()

        // Stable group id shared by both legs (and used as the leg-id prefix), so a re-delivered
        // notification dedupes to the same pair instead of double-moving money.
        val groupId = withdrawal.id

        if (bankAccount == null || cashAccount == null) {
            // No Banco/Efectivo accounts to relocate between: still record the withdrawal as a
            // transfer-flagged outflow so it moves the balance but never counts as a real expense.
            val fallback = withdrawal.copy(
                type = "TRANSFERENCIA",
                categoryId = null,
                isIncome = false,
                isTransfer = true,
                transferGroupId = groupId
            )
            if (transactionRepo.insert(fallback)) {
                fallback.accountId?.let { openingBalanceRepo.recalculateAccountBalance(it) }
            }
            return
        }

        // 1. Bank leg — money leaves the bank.
        val bankWithdrawal = withdrawal.copy(
            id = "${groupId}_BANK",
            accountId = bankAccount.id,
            type = "TRANSFERENCIA",
            description = withdrawal.description,
            categoryId = null,
            isIncome = false,
            isTransfer = true,
            transferGroupId = groupId
        )

        // 2. Cash leg — the same money enters the cash account.
        val cashDeposit = Transaction.fromTimestamp(
            id = "${groupId}_CASH",
            ts = withdrawal.ts,
            type = "TRANSFERENCIA",
            description = "Efectivo - ${withdrawal.description}",
            amountCents = withdrawal.amountCents,
            currency = withdrawal.currency,
            srcLast4 = "BANK",
            dstLast4 = "CASH",
            source = withdrawal.source,
            rawPreview = withdrawal.rawPreview,
            categoryId = null,
            accountId = cashAccount.id,
            isIncome = true,
            isTransfer = true,
            transferGroupId = groupId
        )

        // Insert both legs, then recompute each affected account's cached balance from source.
        // The recompute reflects whatever legs were actually inserted (re-deliveries are ignored),
        // so it stays idempotent without per-row deltas.
        val bankInserted = transactionRepo.insert(bankWithdrawal)
        val cashInserted = transactionRepo.insert(cashDeposit)
        if (bankInserted) openingBalanceRepo.recalculateAccountBalance(bankAccount.id)
        if (cashInserted) openingBalanceRepo.recalculateAccountBalance(cashAccount.id)
    }
}