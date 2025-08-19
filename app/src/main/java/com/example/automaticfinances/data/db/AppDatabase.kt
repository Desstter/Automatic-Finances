package com.example.automaticfinances.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [Transaction::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun transactionDao(): TransactionDao

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null
        fun init(ctx: Context) {
            if (INSTANCE == null) {
                INSTANCE = Room.databaseBuilder(ctx, AppDatabase::class.java, "autobook.db")
                    .fallbackToDestructiveMigration()
                    .build()
            }
        }
        fun get(): AppDatabase = INSTANCE
            ?: error("AppDatabase not initialized")
    }
}