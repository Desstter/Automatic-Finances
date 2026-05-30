package com.example.automaticfinances.domain

import com.example.automaticfinances.data.db.Transaction
import com.example.automaticfinances.data.repo.CategoryRepository
import com.example.automaticfinances.data.voice.ParsedTransaction
import com.example.automaticfinances.data.voice.VoiceTransactionDraft
import org.apache.commons.codec.digest.DigestUtils
import javax.inject.Inject

/**
 * Bridges the NLP layer and persistence for voice entries.
 *
 *  - [toDrafts] resolves each [ParsedTransaction] into an editable [VoiceTransactionDraft],
 *    pre-selecting a category (exact-name match → keyword/learned fallback) so the review screen
 *    starts from a sensible state.
 *  - [buildTransaction] turns a *reviewed* draft into a persistable [Transaction]. It deliberately
 *    does NOT insert anything: the caller hands the result to [AddTransactionUseCase], which owns
 *    enrichment, account assignment and the balance-idempotency invariant.
 */
class BuildVoiceTransactionsUseCase @Inject constructor(
    private val categoryRepository: CategoryRepository,
) {

    suspend fun toDrafts(parsed: List<ParsedTransaction>): List<VoiceTransactionDraft> =
        parsed.map { item ->
            VoiceTransactionDraft(
                description = item.description,
                amountCents = item.amountCents,
                categoryId = resolveCategoryId(item),
                isIncome = item.isIncome,
                needsReview = item.needsReview,
            )
        }

    /**
     * Builds the persistable transaction from final, user-confirmed values. The id mirrors the
     * manual-entry scheme (minute-bucketed hash) so a double-tap within the same minute dedupes
     * via [Transaction] primary key, preserving the balance invariant.
     */
    fun buildTransaction(
        draft: VoiceTransactionDraft,
        timestampMillis: Long = System.currentTimeMillis(),
    ): Transaction {
        val type = if (draft.isIncome) TYPE_INCOME else TYPE_EXPENSE
        val id = DigestUtils.sha256Hex(
            "${timestampMillis / 60_000}|${draft.amountCents}|$SOURCE|${draft.description}"
        )
        return Transaction.fromTimestamp(
            id = id,
            ts = timestampMillis,
            type = type,
            description = draft.description,
            amountCents = draft.amountCents,
            currency = "COP",
            srcLast4 = null,
            dstLast4 = null,
            source = SOURCE,
            rawPreview = "Voz: ${draft.description}".take(140),
            categoryId = draft.categoryId,
            isIncome = draft.isIncome,
        )
    }

    /**
     * Resolution order: Gemini's suggested name (exact, accent/case-insensitive, within the right
     * income/expense set) → the existing keyword + learned-preference resolver. Returns null only
     * if even the fallback can't decide, in which case [AddTransactionUseCase] resolves it later.
     */
    private suspend fun resolveCategoryId(item: ParsedTransaction): Long? {
        val candidates = categoryRepository.getActiveSyncByType(item.isIncome)

        val byName = item.suggestedCategoryName?.let { suggested ->
            val normalized = normalize(suggested)
            candidates.firstOrNull { normalize(it.name) == normalized }
        }
        if (byName != null) return byName.id

        val type = if (item.isIncome) TYPE_INCOME else TYPE_EXPENSE
        return categoryRepository.getDefaultCategoryId(type, item.description)
    }

    private fun normalize(s: String): String = s.trim().lowercase()

    companion object {
        const val SOURCE = "voice"
        private const val TYPE_INCOME = "INGRESO"
        private const val TYPE_EXPENSE = "GASTO"
    }
}
