package com.example.automaticfinances.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [Transaction::class, Category::class, UserCategoryPreference::class], 
    version = 3, 
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun transactionDao(): TransactionDao
    abstract fun categoryDao(): CategoryDao
    abstract fun userCategoryPreferenceDao(): UserCategoryPreferenceDao

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null
        fun init(ctx: Context) {
            if (INSTANCE == null) {
                INSTANCE = Room.databaseBuilder(ctx, AppDatabase::class.java, "autobook.db")
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                    .build()
            }
        }
        fun get(): AppDatabase = INSTANCE
            ?: error("AppDatabase not initialized")
    }
}