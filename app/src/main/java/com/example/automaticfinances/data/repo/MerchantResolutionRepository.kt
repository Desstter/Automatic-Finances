package com.example.automaticfinances.data.repo

import com.example.automaticfinances.data.db.CategoryDao
import com.example.automaticfinances.data.db.MerchantResolution
import com.example.automaticfinances.data.db.MerchantResolutionDao
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repositorio para gestionar la resolución de merchants de gateway a nombres reales.
 */
@Singleton
class MerchantResolutionRepository @Inject constructor(
    private val dao: MerchantResolutionDao,
    private val categoryDao: CategoryDao
) {

    fun getAll(): Flow<List<MerchantResolution>> = dao.getAll()

    fun getPrePopulated(): Flow<List<MerchantResolution>> = dao.getPrePopulated()

    fun getUserCreated(): Flow<List<MerchantResolution>> = dao.getUserCreated()

    fun getTopUsed(): Flow<List<MerchantResolution>> = dao.getTopUsed()

    fun getByCategory(categoryId: Long): Flow<List<MerchantResolution>> = dao.getByCategory(categoryId)

    suspend fun resolve(gatewayMerchant: String): MerchantResolution? {
        val normalized = normalizeGatewayMerchant(gatewayMerchant)
        val resolution = dao.getByGatewayMerchant(normalized)
        if (resolution != null) {
            dao.incrementUsage(normalized)
        }
        return resolution
    }

    suspend fun learn(gatewayMerchant: String, realMerchant: String, categoryId: Long? = null) {
        val normalized = normalizeGatewayMerchant(gatewayMerchant)
        val existing = dao.getByGatewayMerchant(normalized)

        if (existing != null) {
            val updated = existing.copy(
                realMerchant = realMerchant,
                suggestedCategoryId = categoryId ?: existing.suggestedCategoryId,
                usageCount = existing.usageCount + 1,
                lastUsedAt = System.currentTimeMillis()
            )
            dao.update(updated)
        } else {
            val newResolution = MerchantResolution(
                gatewayMerchant = normalized,
                realMerchant = realMerchant,
                suggestedCategoryId = categoryId,
                isPrePopulated = false,
                usageCount = 1,
                lastUsedAt = System.currentTimeMillis()
            )
            dao.insert(newResolution)
        }
    }

    suspend fun findSimilar(gatewayMerchant: String): List<MerchantResolution> {
        val normalized = normalizeGatewayMerchant(gatewayMerchant)
        val pattern = if (normalized.contains("*")) {
            "${normalized.substringBefore("*")}*%"
        } else {
            "$normalized%"
        }
        return dao.findSimilar(pattern)
    }

    suspend fun updateSuggestedCategory(gatewayMerchant: String, categoryId: Long) {
        dao.updateSuggestedCategory(normalizeGatewayMerchant(gatewayMerchant), categoryId)
    }

    suspend fun insert(resolution: MerchantResolution): Long = dao.insert(resolution)

    suspend fun update(resolution: MerchantResolution) = dao.update(resolution)

    suspend fun delete(resolution: MerchantResolution) = dao.delete(resolution)

    suspend fun deleteById(id: Long) = dao.deleteById(id)

    suspend fun initializeDefaultResolutions() {
        val count = dao.countPrePopulated()
        if (count == 0) {
            val categories = categoryDao.getAllActiveSync()
            val categoryMap = categories.associate { it.name to it.id }
            val defaultResolutions = com.example.automaticfinances.data.db.DefaultMerchantResolutions.getList(categoryMap)
            dao.insertAll(defaultResolutions)
        }
    }

    private fun normalizeGatewayMerchant(merchant: String): String {
        return merchant.uppercase().trim().replace(Regex("\\s+"), " ")
    }

    fun isGatewayMerchant(merchant: String): Boolean {
        val normalized = merchant.uppercase()
        return normalized.startsWith("PAYU*") ||
               normalized.startsWith("MERCPAGO*") ||
               normalized.startsWith("MERCADOPAGO*") ||
               normalized.startsWith("BOLD*") ||
               normalized.startsWith("WOMPI*") ||
               normalized.startsWith("EPAYCO*") ||
               normalized.startsWith("PSE*")
    }

    fun extractGatewayBase(merchant: String): String {
        val normalized = normalizeGatewayMerchant(merchant)
        if (normalized.contains("*")) {
            val prefix = normalized.substringBefore("*")
            val firstWord = normalized.substringAfter("*").split(" ").firstOrNull() ?: ""
            return "$prefix*$firstWord"
        }
        return normalized
    }
}
