package com.example.automaticfinances.data.repo

import com.example.automaticfinances.data.db.AppDatabase
import com.example.automaticfinances.data.db.Category
import com.example.automaticfinances.data.db.CategoryWithCount
import com.example.automaticfinances.data.db.DefaultCategories
import kotlinx.coroutines.flow.Flow

class CategoryRepository {
    private val dao = AppDatabase.get().categoryDao()
    
    fun getAllActive(): Flow<List<Category>> = dao.getAllActive()
    
    fun getCategoriesWithCount(): Flow<List<CategoryWithCount>> = dao.getCategoriesWithTransactionCount()
    
    suspend fun getById(id: Long): Category? = dao.getById(id)
    
    suspend fun getAllActiveSync(): List<Category> = dao.getAllActiveSync()
    
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
        val categories = dao.getAllActiveSync()
        
        // Auto-categorización inteligente basada en palabras clave
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
            
            else -> categories.find { it.name == "Otros" }?.id
        }
    }
}