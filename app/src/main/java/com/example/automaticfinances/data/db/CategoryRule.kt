package com.example.automaticfinances.data.db

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * A user-editable keyword → category rule (MANT-2). Replaces the old hardcoded `when` block in
 * [com.example.automaticfinances.data.repo.CategoryRepository]: each row says "if a transaction
 * description contains [keyword], categorize it as the category named [categoryName]".
 *
 * Why store [categoryName] and not a categoryId: the entire categorization layer already resolves
 * categories by name (the default rules and gateway mappings all do `categories.find { it.name == X }`),
 * and a name survives the category being deleted-and-recreated. Matching resolves the name against the
 * live, active categories at lookup time, so a rule that points at a missing category is simply skipped.
 *
 * [isIncome] scopes the rule to income or expense descriptions so the same keyword can mean different
 * things on each side, and so income transactions finally get keyword categorization too (previously
 * `getIntelligentCategorySuggestion` only ever looked at expense keywords).
 */
@Entity(
    tableName = "category_rules",
    indices = [Index(value = ["keyword", "isIncome"], unique = true)],
)
data class CategoryRule(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val keyword: String,        // normalized lowercase substring to look for in the description
    val categoryName: String,   // name of the category this keyword maps to
    val isIncome: Boolean,      // true → matches income descriptions, false → expenses
    val isDefault: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
)

/**
 * Canonical seed of keyword rules, ported from the original hardcoded resolver plus a handful of
 * well-known Colombian merchants. Single source of truth for BOTH the upgrade migration
 * (MIGRATION_14_15, for existing installs) and the fresh-install seeding in
 * [com.example.automaticfinances.data.repo.CategoryRepository.initializeDefaultRules] (fresh installs
 * create the schema at the current version and never run migrations, so they must be seeded in code).
 *
 * Order is not significant: lookup picks the longest matching keyword (ties → lowest id), so a more
 * specific keyword like "uber eats" always wins over "uber".
 */
object DefaultCategoryRules {
    private fun expense(category: String, vararg keywords: String) =
        keywords.map { CategoryRule(keyword = it, categoryName = category, isIncome = false, isDefault = true) }

    private fun income(category: String, vararg keywords: String) =
        keywords.map { CategoryRule(keyword = it, categoryName = category, isIncome = true, isDefault = true) }

    val list: List<CategoryRule> = buildList {
        // ---- Gastos ----
        addAll(expense("Comida por fuera", "rappi", "uber eats", "domicilio", "restaurant", "pizza", "burger", "mcdonald", "kfc", "sandwich"))
        addAll(expense("Gasolina", "gasolina", "combustible", "estacion", "esso", "mobil", "terpel", "texaco", "petrobras", "primax", "biomax"))
        addAll(expense("Transporte", "taxi", "uber", "didi", "cabify", "beat", "transporte", "transmilenio", "metro", "peaje"))
        addAll(expense("Salud", "farmacia", "drogas", "drogueria", "clinica", "hospital", "medico", "cruz verde", "farmatodo", "copidrogas"))
        addAll(expense("Entretenimiento", "cine", "netflix", "spotify", "juego", "disney", "teatro", "concierto"))
        addAll(expense("Servicios", "agua", "luz", "gas", "internet", "telefono", "claro", "movistar", "tigo"))
        addAll(expense("Comida obligatoria", "exito", "carulla", "olimpica", "supermercado", "mercado", "jumbo", "makro"))
        addAll(expense("Arriendo", "arriendo", "alquiler", "canon"))
        addAll(expense("Ropa", "zara", "ropa", "nike", "adidas", "falabella", "koaj"))

        // ---- Ingresos ----
        addAll(income("Salario", "salario", "nomina", "sueldo", "pago laboral"))
        addAll(income("Venta personal", "freelance", "honorarios", "consultoria", "trabajo independiente", "venta", "vendido", "comercio", "negocio"))
        addAll(income("Regalo", "regalo", "obsequio", "donacion", "familiar"))
        addAll(income("Subsidio", "subsidio", "auxilio", "ayuda", "apoyo gobierno"))
        addAll(income("Bonos", "bono", "premio", "incentivo", "comision"))
    }
}
