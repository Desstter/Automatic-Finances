package com.example.automaticfinances.data.db

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [Transaction::class, Category::class, UserCategoryPreference::class, Budget::class, FinancialGoal::class, Account::class, OpeningBalance::class, MerchantResolution::class],
    version = 11,
    // Schemas are exported to app/schemas (see room.schemaLocation in build.gradle.kts) so future
    // migrations can be validated with Room's MigrationTestHelper.
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun transactionDao(): TransactionDao
    abstract fun categoryDao(): CategoryDao
    abstract fun userCategoryPreferenceDao(): UserCategoryPreferenceDao
    abstract fun budgetDao(): BudgetDao
    abstract fun financialGoalDao(): FinancialGoalDao
    abstract fun accountDao(): AccountDao
    abstract fun openingBalanceDao(): OpeningBalanceDao
    abstract fun merchantResolutionDao(): MerchantResolutionDao

    companion object {
        const val DATABASE_NAME = "autobook.db"
    }
}