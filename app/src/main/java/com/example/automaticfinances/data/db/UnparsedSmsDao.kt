package com.example.automaticfinances.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface UnparsedSmsDao {

    /**
     * Dedup-friendly insert: a re-delivered notification (same stable [UnparsedSms.id]) is a
     * harmless no-op, so replaying the notification shade never spams the diagnostic log.
     */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIgnore(item: UnparsedSms): Long

    @Query("SELECT * FROM unparsed_sms ORDER BY receivedAt DESC")
    fun getAllFlow(): Flow<List<UnparsedSms>>

    @Query("SELECT COUNT(*) FROM unparsed_sms")
    fun countFlow(): Flow<Int>

    @Query("DELETE FROM unparsed_sms WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM unparsed_sms")
    suspend fun clear()
}
