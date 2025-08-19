package com.example.automaticfinances.data.db

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface UserCategoryPreferenceDao {
    
    @Query("SELECT * FROM user_category_preferences WHERE isActive = 1 ORDER BY frequency DESC, lastUsed DESC")
    fun getAllActive(): Flow<List<UserCategoryPreference>>
    
    @Query("SELECT * FROM user_category_preferences WHERE merchantKey = :merchantKey AND isActive = 1 ORDER BY frequency DESC LIMIT 1")
    suspend fun getPreferenceForMerchant(merchantKey: String): UserCategoryPreference?
    
    @Query("""
        SELECT ucp.categoryId, c.name as categoryName, c.icon as categoryIcon, 
               ucp.confidence, 'Aprendizaje previo' as reason, ucp.merchantKey
        FROM user_category_preferences ucp
        INNER JOIN categories c ON ucp.categoryId = c.id
        WHERE ucp.merchantKey = :merchantKey AND ucp.isActive = 1
        ORDER BY ucp.frequency DESC, ucp.lastUsed DESC
    """)
    suspend fun getSuggestionsForMerchant(merchantKey: String): List<CategorySuggestion>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(preference: UserCategoryPreference): Long
    
    @Update
    suspend fun update(preference: UserCategoryPreference)
    
    @Query("UPDATE user_category_preferences SET isActive = 0 WHERE id = :preferenceId")
    suspend fun softDelete(preferenceId: Long)
    
    @Query("DELETE FROM user_category_preferences WHERE merchantKey = :merchantKey")
    suspend fun deleteAllForMerchant(merchantKey: String)
    
    // Analytics y reportes
    @Query("""
        SELECT 
            c.id as categoryId,
            c.name as categoryName,
            COUNT(*) as totalPredictions,
            SUM(CASE WHEN ucp.source = 'user' THEN 1 ELSE 0 END) as correctPredictions,
            CASE 
                WHEN COUNT(*) > 0 THEN CAST(SUM(CASE WHEN ucp.source = 'user' THEN 1 ELSE 0 END) AS REAL) / COUNT(*)
                ELSE 0.0 
            END as accuracy
        FROM user_category_preferences ucp
        INNER JOIN categories c ON ucp.categoryId = c.id
        WHERE ucp.isActive = 1
        GROUP BY c.id, c.name
        ORDER BY totalPredictions DESC
    """)
    suspend fun getCategoryAccuracyStats(): List<CategoryAccuracy>
    
    @Query("SELECT COUNT(*) FROM user_category_preferences WHERE isActive = 1")
    suspend fun getTotalPreferences(): Int
    
    @Query("""
        SELECT *
        FROM user_category_preferences 
        WHERE isActive = 1 
        ORDER BY frequency DESC 
        LIMIT :limit
    """)
    suspend fun getTopMerchants(limit: Int = 10): List<UserCategoryPreference>
    
    // Aprendizaje y mejora continua
    @Query("""
        UPDATE user_category_preferences 
        SET frequency = frequency + 1, lastUsed = :timestamp, confidence = CASE 
            WHEN confidence < 1.0 THEN MIN(1.0, confidence + 0.1)
            ELSE confidence 
        END
        WHERE merchantKey = :merchantKey AND categoryId = :categoryId AND isActive = 1
    """)
    suspend fun reinforcePreference(merchantKey: String, categoryId: Long, timestamp: Long = System.currentTimeMillis())
    
    @Query("""
        UPDATE user_category_preferences 
        SET confidence = MAX(0.1, confidence - 0.2)
        WHERE merchantKey = :merchantKey AND categoryId != :correctCategoryId AND isActive = 1
    """)
    suspend fun penalizeWrongPreferences(merchantKey: String, correctCategoryId: Long)
}