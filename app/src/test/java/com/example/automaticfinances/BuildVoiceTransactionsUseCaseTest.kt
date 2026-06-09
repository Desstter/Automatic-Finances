package com.example.automaticfinances

import com.example.automaticfinances.data.repo.CategoryRepository
import com.example.automaticfinances.data.repo.UserCategoryPreferenceRepository
import com.example.automaticfinances.data.voice.ParsedTransaction
import com.example.automaticfinances.data.voice.VoiceTransactionDraft
import com.example.automaticfinances.domain.BuildVoiceTransactionsUseCase
import com.example.automaticfinances.fakes.FakeCategoryDao
import com.example.automaticfinances.fakes.FakeCategoryRuleDao
import com.example.automaticfinances.fakes.FakeUserCategoryPreferenceDao
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class BuildVoiceTransactionsUseCaseTest {

    private lateinit var categoryRepository: CategoryRepository
    private lateinit var useCase: BuildVoiceTransactionsUseCase

    @Before
    fun setup() {
        categoryRepository = CategoryRepository(
            dao = FakeCategoryDao(),
            ruleDao = FakeCategoryRuleDao(),
            preferenceRepo = UserCategoryPreferenceRepository(FakeUserCategoryPreferenceDao()),
        )
        useCase = BuildVoiceTransactionsUseCase(categoryRepository)
    }

    @Test
    fun toDrafts_exactCategoryName_resolvesToThatCategory() = runBlocking {
        val expected = categoryRepository.getActiveSyncByType(false).first { it.name == "Comida por fuera" }
        val parsed = ParsedTransaction(
            description = "almuerzo",
            amountCents = 1_200_000L,
            suggestedCategoryName = "Comida por fuera",
            isIncome = false,
            needsReview = false,
        )

        val drafts = useCase.toDrafts(listOf(parsed))

        assertEquals(1, drafts.size)
        assertEquals(expected.id, drafts[0].categoryId)
        assertEquals(1_200_000L, drafts[0].amountCents)
    }

    @Test
    fun toDrafts_unknownCategoryName_fallsBackToKeywordResolver() = runBlocking {
        // No category literally named like this; resolver should fall back on the description.
        val parsed = ParsedTransaction(
            description = "rappi mcdonalds",
            amountCents = 3_000_000L,
            suggestedCategoryName = "No existe esta categoria",
            isIncome = false,
            needsReview = false,
        )

        val drafts = useCase.toDrafts(listOf(parsed))

        val comidaPorFuera = categoryRepository.getActiveSyncByType(false).first { it.name == "Comida por fuera" }
        assertEquals(comidaPorFuera.id, drafts[0].categoryId)
    }

    @Test
    fun toDrafts_incomeSuggestion_resolvesWithinIncomeCategories() = runBlocking {
        val salario = categoryRepository.getActiveSyncByType(true).first { it.name == "Salario" }
        val parsed = ParsedTransaction(
            description = "me pagaron",
            amountCents = 150_000_000L,
            suggestedCategoryName = "Salario",
            isIncome = true,
            needsReview = false,
        )

        val drafts = useCase.toDrafts(listOf(parsed))

        assertEquals(salario.id, drafts[0].categoryId)
        assertTrue(drafts[0].isIncome)
    }

    @Test
    fun buildTransaction_setsVoiceSourceAndExpenseType() {
        val draft = VoiceTransactionDraft(
            description = "galletas",
            amountCents = 1_200_000L,
            categoryId = 4L,
            isIncome = false,
            needsReview = false,
        )

        val tx = useCase.buildTransaction(draft, timestampMillis = 1_700_000_000_000L)

        assertEquals("voice", tx.source)
        assertEquals("GASTO", tx.type)
        assertEquals(1_200_000L, tx.amountCents)
        assertEquals(4L, tx.categoryId)
        assertEquals("galletas", tx.description)
        assertEquals(false, tx.isIncome)
        assertEquals("COP", tx.currency)
        assertNotNull(tx.id)
    }

    @Test
    fun buildTransaction_incomeUsesIngresoType() {
        val draft = VoiceTransactionDraft(
            description = "venta",
            amountCents = 5_000_000L,
            categoryId = 11L,
            isIncome = true,
            needsReview = false,
        )

        val tx = useCase.buildTransaction(draft, timestampMillis = 1_700_000_000_000L)

        assertEquals("INGRESO", tx.type)
        assertTrue(tx.isIncome)
    }

    @Test
    fun buildTransaction_isDeterministic_forSameContentAndMinute() {
        val draft = VoiceTransactionDraft(
            description = "pan",
            amountCents = 700_000L,
            categoryId = 1L,
            isIncome = false,
            needsReview = false,
        )

        val a = useCase.buildTransaction(draft, timestampMillis = 1_700_000_000_000L)
        val b = useCase.buildTransaction(draft, timestampMillis = 1_700_000_000_000L)

        // Same content within the same minute -> same id, preserving idempotent dedupe.
        assertEquals(a.id, b.id)
    }
}
