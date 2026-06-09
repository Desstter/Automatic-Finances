package com.example.automaticfinances.domain

import com.example.automaticfinances.data.db.Transaction
import com.example.automaticfinances.data.repo.AccountRepository
import com.example.automaticfinances.data.repo.OpeningBalanceRepository
import com.example.automaticfinances.data.repo.TransactionRepository
import org.apache.commons.codec.digest.DigestUtils
import javax.inject.Inject

/**
 * Records an internal transfer of money between two of the user's own accounts (e.g. Banco ->
 * Efectivo) as a **dual entry**, mirroring the ATM-withdrawal pattern in [AddTransactionUseCase].
 *
 * Why dual entry instead of a dedicated table: the single source of truth for a balance is
 * `opening snapshot + Σ(movements)`, where each movement is `+amount` when `isIncome` and
 * `-amount` otherwise (see [OpeningBalanceRepository]). Representing a transfer as a negative leg
 * on the origin and a positive leg on the destination keeps that engine untouched — the two legs
 * net to zero across accounts, each account's balance updates correctly, and no special-casing
 * leaks into the balance math.
 *
 * Both legs are flagged `isTransfer = true` and share a `transferGroupId`, which (a) excludes them
 * from every income/expense total and analysis (see `TransactionDao`) since a transfer is neither
 * income nor expense, and (b) lets edit/delete/restore operate on the pair atomically.
 */
class TransferUseCase @Inject constructor(
    private val transactionRepo: TransactionRepository,
    private val accountRepo: AccountRepository,
    private val openingBalanceRepo: OpeningBalanceRepository,
    private val transactionRunner: TransactionRunner,
) {

    sealed interface Result {
        /** Both legs were created (or one if a re-run only filled a missing leg). */
        data object Success : Result
        data class Failure(val message: String) : Result
    }

    /**
     * @param originAccountId account the money leaves.
     * @param destAccountId account the money enters (must differ from origin).
     * @param amountCents positive magnitude in COP cents.
     * @param ts epoch millis the transfer happened.
     * @param note optional free text stored on both legs.
     */
    suspend operator fun invoke(
        originAccountId: Long,
        destAccountId: Long,
        amountCents: Long,
        ts: Long,
        note: String = "",
    ): Result {
        if (originAccountId == destAccountId) {
            return Result.Failure("La cuenta de origen y destino deben ser diferentes")
        }
        if (amountCents <= 0) {
            return Result.Failure("El monto debe ser mayor a cero")
        }

        val origin = accountRepo.getAccountById(originAccountId)
            ?: return Result.Failure("Cuenta de origen no encontrada")
        val dest = accountRepo.getAccountById(destAccountId)
            ?: return Result.Failure("Cuenta de destino no encontrada")

        // Stable group id: same (minute, amount, origin, dest) maps to the same transfer, so a
        // double tap / re-delivery is deduped by insertIgnore instead of double-moving money.
        val groupId = DigestUtils.sha256Hex(
            "TRANSFER|${ts / 60000}|$amountCents|$originAccountId|$destAccountId"
        )

        val outLeg = Transaction.fromTimestamp(
            id = "${groupId}_OUT",
            ts = ts,
            type = "TRANSFERENCIA",
            description = "Transferencia a ${dest.name}",
            amountCents = amountCents,
            currency = "COP",
            srcLast4 = null,
            dstLast4 = null,
            source = "transfer",
            rawPreview = "Transferencia ${origin.name} -> ${dest.name}",
            categoryId = null,
            accountId = originAccountId,
            isIncome = false,
            isTransfer = true,
            transferGroupId = groupId,
        ).copy(notes = note)

        val inLeg = Transaction.fromTimestamp(
            id = "${groupId}_IN",
            ts = ts,
            type = "TRANSFERENCIA",
            description = "Transferencia desde ${origin.name}",
            amountCents = amountCents,
            currency = "COP",
            srcLast4 = null,
            dstLast4 = null,
            source = "transfer",
            rawPreview = "Transferencia ${origin.name} -> ${dest.name}",
            categoryId = null,
            accountId = destAccountId,
            isIncome = true,
            isTransfer = true,
            transferGroupId = groupId,
        ).copy(notes = note)

        // Insert both legs and recompute each affected balance from source, atomically: the two
        // legs and their balance effects commit together or not at all (financial invariant).
        return transactionRunner.runInTransaction {
            val outInserted = transactionRepo.insert(outLeg)
            val inInserted = transactionRepo.insert(inLeg)
            if (outInserted) openingBalanceRepo.recalculateAccountBalance(originAccountId)
            if (inInserted) openingBalanceRepo.recalculateAccountBalance(destAccountId)
            Result.Success
        }
    }
}
