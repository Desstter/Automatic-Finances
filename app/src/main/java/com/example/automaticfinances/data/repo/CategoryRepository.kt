package com.example.automaticfinances.data.repo

import com.example.automaticfinances.data.db.Category
import com.example.automaticfinances.data.db.CategoryDao
import com.example.automaticfinances.data.db.CategoryWithCount
import com.example.automaticfinances.data.db.CategorySuggestion
import com.example.automaticfinances.data.db.DefaultCategories
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class CategoryRepository @Inject constructor(
    private val dao: CategoryDao,
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
        val category = dao.getById(categoryId) ?: return Pair(false, "Categoría no encontrada")
        
        if (category.isDefault) {
            return Pair(false, "No se puede eliminar una categoría predefinida")
        }
        
        val transactionCount = dao.countTransactionsInCategory(categoryId)
        return if (transactionCount > 0) {
            Pair(true, "Esta categoría tiene $transactionCount transacciones asociadas. Se desactivará pero no se eliminará.")
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
    
    suspend fun getDefaultCategoryId(transactionType: String, description: String): Long? {
        // 1. PRIORIDAD: Verificar si el usuario ya tiene una preferencia aprendida
        getLearnedCategoryId(description)?.let { return it }

        // 2. FALLBACK: Usar sistema de reglas por palabras clave
        return if (transactionType == "INGRESO") {
            getIncomeKeywordBasedCategoryId(description)
        } else {
            getExpenseKeywordBasedCategoryId(description)
        }
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
    
    suspend fun getIntelligentCategorySuggestion(description: String): CategorySuggestion? {
        // Primero verificar aprendizaje del usuario
        val suggestion = preferenceRepo.suggestCategory(description)
        if (suggestion != null && suggestion.confidence > 0.5f) {
            return suggestion
        }
        
        // Si no hay aprendizaje, usar palabras clave pero con menor confianza
        val keywordCategoryId = getExpenseKeywordBasedCategoryId(description)
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
    
    suspend fun learnFromUserCategoryChoice(merchant: String, categoryId: Long) {
        preferenceRepo.learnFromUserChoice(merchant, categoryId)
    }
    
    suspend fun getCategoryAccuracyStats() = preferenceRepo.getCategoryAccuracyStats()
    
    private suspend fun getExpenseKeywordBasedCategoryId(description: String): Long? {
        val categories = dao.getAllActiveSync()
        val descriptionLower = description.lowercase()
        
        return when {
            // Comida por fuera
            descriptionLower.contains("rappi") || 
            descriptionLower.contains("uber eats") || 
            descriptionLower.contains("domicilio") ||
            descriptionLower.contains("restaurant") ||
            descriptionLower.contains("pizza") ||
            descriptionLower.contains("burger") -> 
                categories.find { it.name == "Comida por fuera" }?.id
            
            // Gasolina/Transporte
            descriptionLower.contains("estacion") ||
            descriptionLower.contains("gasolina") ||
            descriptionLower.contains("combustible") ||
            descriptionLower.contains("esso") ||
            descriptionLower.contains("mobil") -> 
                categories.find { it.name == "Gasolina" }?.id
            
            descriptionLower.contains("taxi") ||
            descriptionLower.contains("uber") ||
            descriptionLower.contains("beat") ||
            descriptionLower.contains("transporte") -> 
                categories.find { it.name == "Transporte" }?.id
            
            // Salud
            descriptionLower.contains("farmacia") ||
            descriptionLower.contains("drogas") ||
            descriptionLower.contains("clinica") ||
            descriptionLower.contains("hospital") ||
            descriptionLower.contains("medico") -> 
                categories.find { it.name == "Salud" }?.id
            
            // Entretenimiento
            descriptionLower.contains("cine") ||
            descriptionLower.contains("netflix") ||
            descriptionLower.contains("spotify") ||
            descriptionLower.contains("juego") -> 
                categories.find { it.name == "Entretenimiento" }?.id
            
            // Servicios
            descriptionLower.contains("agua") ||
            descriptionLower.contains("luz") ||
            descriptionLower.contains("gas") ||
            descriptionLower.contains("internet") ||
            descriptionLower.contains("telefono") -> 
                categories.find { it.name == "Servicios" }?.id
            
            // Supermercado/Comida obligatoria
            descriptionLower.contains("exito") ||
            descriptionLower.contains("carulla") ||
            descriptionLower.contains("olimpica") ||
            descriptionLower.contains("supermercado") ||
            descriptionLower.contains("mercado") -> 
                categories.find { it.name == "Comida obligatoria" }?.id
            
            // Ropa
            descriptionLower.contains("zara") ||
            descriptionLower.contains("h&m") ||
            descriptionLower.contains("ropa") ||
            descriptionLower.contains("nike") ||
            descriptionLower.contains("adidas") -> 
                categories.find { it.name == "Ropa" }?.id
            
            else -> categories.find { it.name == "Otros gastos" }?.id
        }
    }
    
    private suspend fun getIncomeKeywordBasedCategoryId(description: String): Long? {
        val categories = dao.getActiveSyncByType(isIncome = true)
        val descriptionLower = description.lowercase()
        
        return when {
            // Salario/Nómina
            descriptionLower.contains("salario") ||
            descriptionLower.contains("nomina") ||
            descriptionLower.contains("sueldo") ||
            descriptionLower.contains("pago laboral") -> 
                categories.find { it.name == "Salario" }?.id
                
            // Freelance/Trabajo independiente (maps to Venta personal)
            descriptionLower.contains("freelance") ||
            descriptionLower.contains("honorarios") ||
            descriptionLower.contains("consultoria") ||
            descriptionLower.contains("trabajo independiente") -> 
                categories.find { it.name == "Venta personal" }?.id
                
            // Ventas
            descriptionLower.contains("venta") ||
            descriptionLower.contains("vendido") ||
            descriptionLower.contains("comercio") ||
            descriptionLower.contains("negocio") -> 
                categories.find { it.name == "Venta personal" }?.id
                
            // Regalos/Donaciones
            descriptionLower.contains("regalo") ||
            descriptionLower.contains("donacion") ||
            descriptionLower.contains("obsequio") ||
            descriptionLower.contains("familiar") -> 
                categories.find { it.name == "Regalo" }?.id
                
            // Subsidios
            descriptionLower.contains("subsidio") ||
            descriptionLower.contains("auxilio") ||
            descriptionLower.contains("ayuda") ||
            descriptionLower.contains("apoyo gobierno") -> 
                categories.find { it.name == "Subsidio" }?.id
                
            // Bonos/Premios
            descriptionLower.contains("bono") ||
            descriptionLower.contains("premio") ||
            descriptionLower.contains("incentivo") ||
            descriptionLower.contains("comision") -> 
                categories.find { it.name == "Bonos" }?.id
                
            // Transferencia recibida (genérico)
            descriptionLower.contains("transferencia") ||
            descriptionLower.contains("recibido") ||
            descriptionLower.contains("deposito") ||
            descriptionLower.contains("consignacion") -> 
                categories.find { it.name == "Salario" }?.id // Default para transferencias
                
            else -> categories.find { it.name == "Otros ingresos" }?.id
        }
    }
}