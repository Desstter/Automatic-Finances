package com.example.automaticfinances.data.repo

import com.example.automaticfinances.data.db.Category
import com.example.automaticfinances.data.db.CategoryDao
import com.example.automaticfinances.data.db.CategoryRule
import com.example.automaticfinances.data.db.CategoryRuleDao
import com.example.automaticfinances.data.db.CategoryWithCount
import com.example.automaticfinances.data.db.CategorySuggestion
import com.example.automaticfinances.data.db.DefaultCategories
import com.example.automaticfinances.data.db.DefaultCategoryRules
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class CategoryRepository @Inject constructor(
    private val dao: CategoryDao,
    private val ruleDao: CategoryRuleDao,
    private val preferenceRepo: UserCategoryPreferenceRepository
) {

    fun getAllActive(): Flow<List<Category>> = dao.getAllActive()

    fun getActiveByType(isIncome: Boolean): Flow<List<Category>> = dao.getActiveByType(isIncome)

    fun getCategoriesWithCount(): Flow<List<CategoryWithCount>> = dao.getCategoriesWithTransactionCount()

    fun getCategoriesWithCountByType(isIncome: Boolean): Flow<List<CategoryWithCount>> = dao.getCategoriesWithTransactionCountByType(isIncome)

    suspend fun getById(id: Long): Category? = dao.getById(id)

    suspend fun getAllActiveSync(): List<Category> = dao.getAllActiveSync()

    suspend fun getActiveSyncByType(isIncome: Boolean): List<Category> = dao.getActiveSyncByType(isIncome)

    suspend fun insert(category: Category): Long = dao.insert(category)

    suspend fun update(category: Category) = dao.update(category)

    suspend fun delete(categoryId: Long) {
        // Verificar si hay transacciones asociadas antes de eliminar
        val transactionCount = dao.countTransactionsInCategory(categoryId)
        if (transactionCount > 0) {
            // Si hay transacciones, solo desactivar la categoría
            dao.softDelete(categoryId)
        } else {
            // Si no hay transacciones, eliminar completamente
            val category = dao.getById(categoryId)
            category?.let { dao.delete(it) }
        }
    }

    suspend fun canDelete(categoryId: Long): Pair<Boolean, String> {
        dao.getById(categoryId) ?: return Pair(false, "Categoría no encontrada")

        // Any category may be removed, including the predefined ones. If transactions are
        // attached we soft-delete (deactivate) to preserve their history; otherwise the row
        // is removed for good. Predefined categories are not re-seeded once the user has
        // deleted some, so the user's intent is respected.
        val transactionCount = dao.countTransactionsInCategory(categoryId)
        return if (transactionCount > 0) {
            Pair(true, "Esta categoría tiene $transactionCount transacciones asociadas. Se ocultará pero su historial se conservará.")
        } else {
            Pair(true, "Esta categoría se eliminará permanentemente.")
        }
    }

    suspend fun initializeDefaultCategories() {
        val defaultCount = dao.countDefaultCategories()
        if (defaultCount == 0) {
            dao.insertAll(DefaultCategories.list)
        }
    }

    /**
     * Seeds the default keyword rules on a fresh install (MANT-2). Fresh installs create the schema
     * at the current version and never run migrations, so the rule table starts empty — only when it
     * is empty do we populate it. After the user deletes some defaults the table is no longer empty,
     * so we never re-add them (mirrors [initializeDefaultCategories]'s intent-respecting behavior).
     */
    suspend fun initializeDefaultRules() {
        if (ruleDao.count() == 0) {
            ruleDao.insertAll(DefaultCategoryRules.list)
        }
    }

    // ---- Editable rule management (surfaced in the rule-editor UI) ----

    fun getAllRules(): Flow<List<CategoryRule>> = ruleDao.getAll()

    suspend fun addRule(keyword: String, categoryName: String, isIncome: Boolean) {
        val normalized = normalizeKeyword(keyword)
        if (normalized.isBlank()) return
        ruleDao.insert(
            CategoryRule(
                keyword = normalized,
                categoryName = categoryName,
                isIncome = isIncome,
                isDefault = false,
            )
        )
    }

    suspend fun deleteRule(id: Long) = ruleDao.deleteById(id)

    suspend fun getDefaultCategoryId(transactionType: String, description: String): Long? {
        // 1. PRIORIDAD: Verificar si el usuario ya tiene una preferencia aprendida
        getLearnedCategoryId(description)?.let { return it }

        // 2. FALLBACK: Usar sistema de reglas por palabras clave (tabla editable).
        // Toda variante de ingreso ("INGRESO", "INGRESO_NOMINA", "INGRESO_TRANSFERENCIA", …) se
        // resuelve contra las reglas de ingreso; el resto contra las de gasto.
        return keywordBasedCategoryId(description, isIncomeType(transactionType))
    }

    /**
     * Devuelve únicamente la categoría aprendida de las correcciones del usuario
     * (confianza > 0.6), o null si no hay una preferencia suficientemente confiable.
     * Expuesto para que la resolución de gateway pueda dar prioridad a las correcciones
     * del usuario por encima de la categoría sugerida del mapping de comercios.
     */
    suspend fun getLearnedCategoryId(description: String): Long? {
        val userPreference = preferenceRepo.getPreferenceForMerchant(description)
        return if (userPreference != null && userPreference.confidence > 0.6f) {
            userPreference.categoryId
        } else {
            null
        }
    }

    suspend fun getIntelligentCategorySuggestion(description: String, isIncome: Boolean = false): CategorySuggestion? {
        // Primero verificar aprendizaje del usuario
        val suggestion = preferenceRepo.suggestCategory(description)
        if (suggestion != null && suggestion.confidence > 0.5f) {
            return suggestion
        }

        // Si no hay aprendizaje, usar palabras clave pero con menor confianza. Ahora respeta el tipo
        // (ingreso/gasto) en vez de mirar siempre reglas de gasto.
        val keywordCategoryId = keywordBasedCategoryId(description, isIncome)
        if (keywordCategoryId != null) {
            val categories = dao.getAllActiveSync()
            val category = categories.find { it.id == keywordCategoryId }
            if (category != null) {
                return CategorySuggestion(
                    categoryId = category.id,
                    categoryName = category.name,
                    categoryIcon = category.icon,
                    confidence = 0.4f, // Menor confianza para reglas
                    reason = "Palabras clave detectadas",
                    merchantKey = description
                )
            }
        }

        return null
    }

    /**
     * Builds the 2–3 category chips offered in the capture-feedback notification (PROD-2): the
     * auto-assigned category first (so a tap confirms it), then the user's most-used categories of the
     * same type as alternatives. Returns distinct categories, capped at [limit].
     */
    suspend fun suggestCategoriesForCapture(assignedId: Long?, isIncome: Boolean, limit: Int = 3): List<Category> {
        val byUsage = dao.getCategoriesWithCountByTypeSync(isIncome).map { it.toCategory() }
        if (byUsage.isEmpty()) return emptyList()
        val assigned = assignedId?.let { id -> byUsage.find { it.id == id } }
        val ordered = buildList {
            assigned?.let { add(it) }
            addAll(byUsage.filter { it.id != assigned?.id })
        }
        return ordered.take(limit)
    }

    suspend fun learnFromUserCategoryChoice(merchant: String, categoryId: Long) {
        preferenceRepo.learnFromUserChoice(merchant, categoryId)
    }

    suspend fun getCategoryAccuracyStats() = preferenceRepo.getCategoryAccuracyStats()

    private fun isIncomeType(transactionType: String): Boolean =
        transactionType.startsWith("INGRESO", ignoreCase = true)

    private fun normalizeKeyword(keyword: String): String =
        keyword.lowercase().trim().replace(Regex("\\s+"), " ")

    /**
     * Table-driven categorization (MANT-2). Picks the rule whose keyword appears in [description] and
     * is the most specific (longest keyword wins; ties broken by lowest id for determinism), but only
     * if it points at a currently-active category of the requested type. Falls back to the type's
     * default bucket ("Otros gastos" / "Otros ingresos").
     */
    private suspend fun keywordBasedCategoryId(description: String, isIncome: Boolean): Long? {
        val categories = dao.getActiveSyncByType(isIncome)
        val descriptionLower = description.lowercase()

        val match = ruleDao.getByType(isIncome)
            .filter { rule -> rule.keyword.isNotBlank() && descriptionLower.contains(rule.keyword) }
            .sortedWith(compareByDescending<CategoryRule> { it.keyword.length }.thenBy { it.id })
            .firstNotNullOfOrNull { rule -> categories.find { it.name == rule.categoryName }?.id }

        if (match != null) return match

        val defaultName = if (isIncome) "Otros ingresos" else "Otros gastos"
        return categories.find { it.name == defaultName }?.id
    }
}

private fun CategoryWithCount.toCategory(): Category = Category(
    id = id,
    name = name,
    color = color,
    icon = icon,
    isDefault = isDefault,
    isActive = isActive,
    isIncome = isIncome,
)
