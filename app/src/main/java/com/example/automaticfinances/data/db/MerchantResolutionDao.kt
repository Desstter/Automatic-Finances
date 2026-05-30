package com.example.automaticfinances.data.db

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface MerchantResolutionDao {

    /**
     * Obtiene todas las resoluciones de merchants.
     */
    @Query("SELECT * FROM merchant_resolutions ORDER BY usageCount DESC, realMerchant ASC")
    fun getAll(): Flow<List<MerchantResolution>>

    /**
     * Obtiene una resolución específica por gateway merchant.
     * Usado para buscar si ya existe un mapping para un merchant dado.
     */
    @Query("SELECT * FROM merchant_resolutions WHERE gatewayMerchant = :gatewayMerchant LIMIT 1")
    suspend fun getByGatewayMerchant(gatewayMerchant: String): MerchantResolution?

    /**
     * Obtiene todas las resoluciones pre-pobladas.
     */
    @Query("SELECT * FROM merchant_resolutions WHERE isPrePopulated = 1 ORDER BY realMerchant ASC")
    fun getPrePopulated(): Flow<List<MerchantResolution>>

    /**
     * Obtiene todas las resoluciones creadas por el usuario.
     */
    @Query("SELECT * FROM merchant_resolutions WHERE isPrePopulated = 0 ORDER BY usageCount DESC, realMerchant ASC")
    fun getUserCreated(): Flow<List<MerchantResolution>>

    /**
     * Obtiene las resoluciones más usadas (top 10).
     */
    @Query("SELECT * FROM merchant_resolutions WHERE usageCount > 0 ORDER BY usageCount DESC LIMIT 10")
    fun getTopUsed(): Flow<List<MerchantResolution>>

    /**
     * Cuenta cuántas resoluciones existen.
     */
    @Query("SELECT COUNT(*) FROM merchant_resolutions")
    suspend fun count(): Int

    /**
     * Cuenta cuántas resoluciones pre-pobladas existen.
     */
    @Query("SELECT COUNT(*) FROM merchant_resolutions WHERE isPrePopulated = 1")
    suspend fun countPrePopulated(): Int

    /**
     * Inserta una nueva resolución.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(resolution: MerchantResolution): Long

    /**
     * Inserta múltiples resoluciones (usado para pre-población).
     */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(resolutions: List<MerchantResolution>)

    /**
     * Actualiza una resolución existente.
     */
    @Update
    suspend fun update(resolution: MerchantResolution)

    /**
     * Elimina una resolución.
     */
    @Delete
    suspend fun delete(resolution: MerchantResolution)

    /**
     * Elimina una resolución por ID.
     */
    @Query("DELETE FROM merchant_resolutions WHERE id = :id")
    suspend fun deleteById(id: Long)

    /**
     * Incrementa el contador de uso y actualiza lastUsedAt.
     * Llamado cada vez que se usa un mapping para resolver un merchant.
     */
    @Query("""
        UPDATE merchant_resolutions
        SET usageCount = usageCount + 1,
            lastUsedAt = :timestamp
        WHERE gatewayMerchant = :gatewayMerchant
    """)
    suspend fun incrementUsage(gatewayMerchant: String, timestamp: Long = System.currentTimeMillis())

    /**
     * Busca resoluciones similares por patrón (para sugerencias).
     * Ejemplo: "PAYU*FON" podría encontrar "PAYU*FONYOU"
     */
    @Query("SELECT * FROM merchant_resolutions WHERE gatewayMerchant LIKE :pattern ORDER BY usageCount DESC LIMIT 5")
    suspend fun findSimilar(pattern: String): List<MerchantResolution>

    /**
     * Obtiene resoluciones por categoría sugerida.
     */
    @Query("SELECT * FROM merchant_resolutions WHERE suggestedCategoryId = :categoryId ORDER BY usageCount DESC")
    fun getByCategory(categoryId: Long): Flow<List<MerchantResolution>>

    /**
     * Actualiza la categoría sugerida para un gateway merchant.
     */
    @Query("UPDATE merchant_resolutions SET suggestedCategoryId = :categoryId WHERE gatewayMerchant = :gatewayMerchant")
    suspend fun updateSuggestedCategory(gatewayMerchant: String, categoryId: Long)
}
