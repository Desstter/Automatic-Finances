package com.example.automaticfinances.data.repo

import com.example.automaticfinances.data.db.CategoryAccuracy
import com.example.automaticfinances.data.db.CategorySuggestion
import com.example.automaticfinances.data.db.UserCategoryPreference
import com.example.automaticfinances.data.db.UserCategoryPreferenceDao
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class UserCategoryPreferenceRepository @Inject constructor(
    private val dao: UserCategoryPreferenceDao
) {
    
    fun getAllActive(): Flow<List<UserCategoryPreference>> = dao.getAllActive()
    
    suspend fun getPreferenceForMerchant(merchantKey: String): UserCategoryPreference? {
        return dao.getPreferenceForMerchant(normalizeMerchantKey(merchantKey))
    }
    
    suspend fun getSuggestionsForMerchant(merchantKey: String): List<CategorySuggestion> {
        return dao.getSuggestionsForMerchant(normalizeMerchantKey(merchantKey))
    }
    
    suspend fun learnFromUserChoice(merchant: String, categoryId: Long) {
        val merchantKey = normalizeMerchantKey(merchant)
        val existing = dao.getPreferenceForMerchant(merchantKey)
        
        if (existing != null) {
            // Reforzar preferencia existente
            dao.reinforcePreference(merchantKey, categoryId)
            // Penalizar otras preferencias para este merchant
            dao.penalizeWrongPreferences(merchantKey, categoryId)
        } else {
            // Crear nueva preferencia
            val preference = UserCategoryPreference(
                merchantKey = merchantKey,
                categoryId = categoryId,
                confidence = 0.8f,
                frequency = 1,
                source = "user"
            )
            dao.insert(preference)
        }
    }
    
    suspend fun suggestCategory(merchant: String): CategorySuggestion? {
        val merchantKey = normalizeMerchantKey(merchant)
        val suggestions = dao.getSuggestionsForMerchant(merchantKey)
        
        return suggestions.firstOrNull()?.copy(
            reason = when {
                suggestions.first().confidence > 0.8f -> "Aprendizaje previo (${(suggestions.first().confidence * 100).toInt()}% confianza)"
                suggestions.first().confidence > 0.6f -> "Patrón frecuente detectado"
                else -> "Sugerencia basada en historial"
            }
        )
    }
    
    suspend fun markSuggestionAsCorrect(merchantKey: String, categoryId: Long) {
        dao.reinforcePreference(normalizeMerchantKey(merchantKey), categoryId)
    }
    
    suspend fun markSuggestionAsWrong(merchantKey: String, correctCategoryId: Long) {
        val normalizedKey = normalizeMerchantKey(merchantKey)
        // Penalizar todas las preferencias distintas a la correcta para este merchant
        dao.penalizeWrongPreferences(normalizedKey, correctCategoryId)
        // Aprender la opción correcta
        learnFromUserChoice(merchantKey, correctCategoryId)
    }
    
    // Analytics y reportes
    suspend fun getCategoryAccuracyStats(): List<CategoryAccuracy> = dao.getCategoryAccuracyStats()
    
    suspend fun getTotalPreferences(): Int = dao.getTotalPreferences()
    
    suspend fun getTopMerchants(limit: Int = 10): List<UserCategoryPreference> = dao.getTopMerchants(limit)
    
    suspend fun getOverallAccuracy(): Float {
        val stats = getCategoryAccuracyStats()
        if (stats.isEmpty()) return 0f
        
        val totalPredictions = stats.sumOf { it.totalPredictions }
        val totalCorrect = stats.sumOf { it.correctPredictions }
        
        return if (totalPredictions > 0) totalCorrect.toFloat() / totalPredictions else 0f
    }
    
    // Utilidades
    private fun normalizeMerchantKey(merchant: String): String {
        return merchant.lowercase()
            .trim()
            .replace(Regex("\\s+"), " ")  // Múltiples espacios → un espacio
            .replace(Regex("[^a-z0-9 ]"), "")  // Solo letras, números y espacios
            .take(50)  // Limitar longitud
    }
}