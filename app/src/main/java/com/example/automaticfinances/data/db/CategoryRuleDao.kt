package com.example.automaticfinances.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface CategoryRuleDao {

    @Query("SELECT * FROM category_rules ORDER BY isIncome ASC, keyword ASC")
    fun getAll(): Flow<List<CategoryRule>>

    /** Lookup path: all rules for one side (income/expense). The table is tiny, so the longest-match
     *  selection is done in memory by the repository rather than in SQL. */
    @Query("SELECT * FROM category_rules WHERE isIncome = :isIncome")
    suspend fun getByType(isIncome: Boolean): List<CategoryRule>

    @Query("SELECT COUNT(*) FROM category_rules")
    suspend fun count(): Int

    // IGNORE so re-seeding (or a user adding a duplicate keyword for the same side) is a harmless
    // no-op thanks to the unique (keyword, isIncome) index, never a crash.
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(rule: CategoryRule): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(rules: List<CategoryRule>)

    @Update
    suspend fun update(rule: CategoryRule)

    @Query("DELETE FROM category_rules WHERE id = :id")
    suspend fun deleteById(id: Long)
}
