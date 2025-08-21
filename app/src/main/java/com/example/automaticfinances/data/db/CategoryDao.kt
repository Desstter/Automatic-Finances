package com.example.automaticfinances.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface CategoryDao {
    
    @Query("SELECT * FROM categories WHERE isActive = 1 ORDER BY isDefault DESC, name ASC")
    fun getAllActive(): Flow<List<Category>>
    
    @Query("SELECT * FROM categories WHERE isActive = 1 AND isIncome = :isIncome ORDER BY isDefault DESC, name ASC")
    fun getActiveByType(isIncome: Boolean): Flow<List<Category>>
    
    @Query("SELECT * FROM categories WHERE id = :id")
    suspend fun getById(id: Long): Category?
    
    @Query("SELECT * FROM categories WHERE isActive = 1 ORDER BY isDefault DESC, name ASC")
    suspend fun getAllActiveSync(): List<Category>
    
    @Query("SELECT * FROM categories WHERE isActive = 1 AND isIncome = :isIncome ORDER BY isDefault DESC, name ASC")
    suspend fun getActiveSyncByType(isIncome: Boolean): List<Category>
    
    @Query("SELECT COUNT(*) FROM categories WHERE isDefault = 1")
    suspend fun countDefaultCategories(): Int
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(category: Category): Long
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(categories: List<Category>)
    
    @Update
    suspend fun update(category: Category)
    
    @Delete
    suspend fun delete(category: Category)
    
    @Query("UPDATE categories SET isActive = 0 WHERE id = :id")
    suspend fun softDelete(id: Long)
    
    @Query("""
        SELECT COUNT(*) FROM transactions 
        WHERE categoryId = :categoryId
    """)
    suspend fun countTransactionsInCategory(categoryId: Long): Int
    
    @Query("""
        SELECT c.*, COUNT(t.id) as transactionCount
        FROM categories c
        LEFT JOIN transactions t ON c.id = t.categoryId
        WHERE c.isActive = 1
        GROUP BY c.id
        ORDER BY c.isDefault DESC, c.name ASC
    """)
    fun getCategoriesWithTransactionCount(): Flow<List<CategoryWithCount>>
    
    @Query("""
        SELECT c.*, COUNT(t.id) as transactionCount
        FROM categories c
        LEFT JOIN transactions t ON c.id = t.categoryId
        WHERE c.isActive = 1 AND c.isIncome = :isIncome
        GROUP BY c.id
        ORDER BY c.isDefault DESC, c.name ASC
    """)
    fun getCategoriesWithTransactionCountByType(isIncome: Boolean): Flow<List<CategoryWithCount>>
}

data class CategoryWithCount(
    val id: Long,
    val name: String,
    val color: String,
    val icon: String,
    val isDefault: Boolean,
    val isActive: Boolean,
    val isIncome: Boolean,
    val transactionCount: Int
)