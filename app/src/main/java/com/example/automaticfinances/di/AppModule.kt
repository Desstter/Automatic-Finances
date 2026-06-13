package com.example.automaticfinances.di

import android.content.Context
import androidx.room.Room
import com.example.automaticfinances.data.db.AccountDao
import com.example.automaticfinances.data.db.AppDatabase
import com.example.automaticfinances.data.db.BudgetDao
import com.example.automaticfinances.data.db.CategoryDao
import com.example.automaticfinances.data.db.CategoryRuleDao
import com.example.automaticfinances.data.db.FinancialGoalDao
import com.example.automaticfinances.data.db.MIGRATION_1_2
import com.example.automaticfinances.data.db.MIGRATION_2_3
import com.example.automaticfinances.data.db.MIGRATION_3_4
import com.example.automaticfinances.data.db.MIGRATION_4_5
import com.example.automaticfinances.data.db.MIGRATION_5_6
import com.example.automaticfinances.data.db.MIGRATION_6_7
import com.example.automaticfinances.data.db.MIGRATION_7_8
import com.example.automaticfinances.data.db.MIGRATION_8_9
import com.example.automaticfinances.data.db.MIGRATION_9_10
import com.example.automaticfinances.data.db.MIGRATION_10_11
import com.example.automaticfinances.data.db.MIGRATION_11_12
import com.example.automaticfinances.data.db.MIGRATION_12_13
import com.example.automaticfinances.data.db.MIGRATION_13_14
import com.example.automaticfinances.data.db.MIGRATION_14_15
import com.example.automaticfinances.data.db.MIGRATION_15_16
import com.example.automaticfinances.data.db.MerchantResolutionDao
import com.example.automaticfinances.data.db.OpeningBalanceDao
import com.example.automaticfinances.data.db.PendingTransactionDao
import com.example.automaticfinances.data.db.RoomTransactionRunner
import com.example.automaticfinances.data.db.TransactionDao
import com.example.automaticfinances.data.db.UnparsedSmsDao
import com.example.automaticfinances.data.db.UserCategoryPreferenceDao
import com.example.automaticfinances.data.preferences.AiPreferences
import com.example.automaticfinances.data.preferences.InsightsPreferences
import com.example.automaticfinances.data.preferences.ThemeRepository
import com.example.automaticfinances.data.repo.AccountRepository
import com.example.automaticfinances.data.repo.AnalyticsRepository
import com.example.automaticfinances.data.repo.BudgetRepository
import com.example.automaticfinances.data.repo.CategoryRepository
import com.example.automaticfinances.data.repo.InsightsRepository
import com.example.automaticfinances.data.repo.MerchantResolutionRepository
import com.example.automaticfinances.data.repo.OpeningBalanceRepository
import com.example.automaticfinances.data.repo.PendingTransactionRepository
import com.example.automaticfinances.data.repo.TransactionRepository
import com.example.automaticfinances.data.repo.UnparsedSmsRepository
import com.example.automaticfinances.data.repo.UserCategoryPreferenceRepository
import com.example.automaticfinances.domain.AddTransactionUseCase
import com.example.automaticfinances.domain.CaptureFeedbackNotifier
import com.example.automaticfinances.domain.CaptureTransactionUseCase
import com.example.automaticfinances.system.AndroidCaptureFeedbackNotifier
import com.example.automaticfinances.domain.ConfirmPendingTransactionUseCase
import com.example.automaticfinances.domain.TransactionRunner
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context.applicationContext,
            AppDatabase::class.java,
            AppDatabase.DATABASE_NAME
        ).addMigrations(
            MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4,
            MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7,
            MIGRATION_7_8, MIGRATION_8_9, MIGRATION_9_10,
            MIGRATION_10_11, MIGRATION_11_12, MIGRATION_12_13,
            MIGRATION_13_14, MIGRATION_14_15, MIGRATION_15_16
        ).build()
    }

    @Provides
    fun provideTransactionDao(db: AppDatabase): TransactionDao = db.transactionDao()

    @Provides
    fun provideCategoryDao(db: AppDatabase): CategoryDao = db.categoryDao()

    @Provides
    fun provideUserCategoryPreferenceDao(db: AppDatabase): UserCategoryPreferenceDao = db.userCategoryPreferenceDao()

    @Provides
    fun provideBudgetDao(db: AppDatabase): BudgetDao = db.budgetDao()

    @Provides
    fun provideFinancialGoalDao(db: AppDatabase): FinancialGoalDao = db.financialGoalDao()

    @Provides
    fun provideAccountDao(db: AppDatabase): AccountDao = db.accountDao()

    @Provides
    fun provideOpeningBalanceDao(db: AppDatabase): OpeningBalanceDao = db.openingBalanceDao()

    @Provides
    fun provideMerchantResolutionDao(db: AppDatabase): MerchantResolutionDao = db.merchantResolutionDao()

    @Provides
    fun provideUnparsedSmsDao(db: AppDatabase): UnparsedSmsDao = db.unparsedSmsDao()

    @Provides
    fun providePendingTransactionDao(db: AppDatabase): PendingTransactionDao = db.pendingTransactionDao()

    @Provides
    fun provideCategoryRuleDao(db: AppDatabase): CategoryRuleDao = db.categoryRuleDao()

    @Provides
    @Singleton
    fun provideUserCategoryPreferenceRepository(dao: UserCategoryPreferenceDao): UserCategoryPreferenceRepository =
        UserCategoryPreferenceRepository(dao)

    @Provides
    @Singleton
    fun provideCategoryRepository(
        dao: CategoryDao,
        ruleDao: CategoryRuleDao,
        prefRepo: UserCategoryPreferenceRepository,
    ): CategoryRepository =
        CategoryRepository(dao, ruleDao, prefRepo)

    @Provides
    @Singleton
    fun provideTransactionRepository(dao: TransactionDao): TransactionRepository =
        TransactionRepository(dao)

    @Provides
    @Singleton
    fun provideAccountRepository(accountDao: AccountDao, transactionDao: TransactionDao): AccountRepository =
        AccountRepository(accountDao, transactionDao)

    @Provides
    @Singleton
    fun provideBudgetRepository(budgetDao: BudgetDao, transactionDao: TransactionDao, categoryDao: CategoryDao): BudgetRepository =
        BudgetRepository(budgetDao, transactionDao, categoryDao)

    @Provides
    @Singleton
    fun provideOpeningBalanceRepository(openingBalanceDao: OpeningBalanceDao, accountDao: AccountDao, transactionDao: TransactionDao): OpeningBalanceRepository =
        OpeningBalanceRepository(openingBalanceDao, accountDao, transactionDao)

    @Provides
    @Singleton
    fun provideAnalyticsRepository(
        transactionRepo: TransactionRepository,
        budgetRepo: BudgetRepository,
        categoryRepo: CategoryRepository,
        transactionDao: TransactionDao,
        categoryDao: CategoryDao
    ): AnalyticsRepository =
        AnalyticsRepository(transactionRepo, budgetRepo, categoryRepo, transactionDao, categoryDao)

    @Provides
    @Singleton
    fun provideTransactionRunner(db: AppDatabase): TransactionRunner =
        RoomTransactionRunner(db)

    @Provides
    @Singleton
    fun provideAddTransactionUseCase(
        transactionRepo: TransactionRepository,
        accountRepo: AccountRepository,
        categoryRepo: CategoryRepository,
        merchantResolutionRepo: MerchantResolutionRepository,
        openingBalanceRepo: OpeningBalanceRepository,
        transactionRunner: TransactionRunner
    ): AddTransactionUseCase =
        AddTransactionUseCase(transactionRepo, accountRepo, categoryRepo, merchantResolutionRepo, openingBalanceRepo, transactionRunner)

    @Provides
    @Singleton
    fun provideThemeRepository(@ApplicationContext context: Context): ThemeRepository =
        ThemeRepository(context)

    @Provides
    @Singleton
    fun provideInsightsPreferences(@ApplicationContext context: Context): InsightsPreferences =
        InsightsPreferences(context)

    @Provides
    @Singleton
    fun provideAiPreferences(@ApplicationContext context: Context): AiPreferences =
        AiPreferences(context)

    @Provides
    @Singleton
    fun provideInsightsRepository(transactionDao: TransactionDao, categoryDao: CategoryDao): InsightsRepository =
        InsightsRepository(transactionDao, categoryDao)

    @Provides
    @Singleton
    fun provideMerchantResolutionRepository(dao: MerchantResolutionDao, categoryDao: CategoryDao): MerchantResolutionRepository =
        MerchantResolutionRepository(dao, categoryDao)

    @Provides
    @Singleton
    fun provideUnparsedSmsRepository(dao: UnparsedSmsDao): UnparsedSmsRepository =
        UnparsedSmsRepository(dao)

    @Provides
    @Singleton
    fun providePendingTransactionRepository(dao: PendingTransactionDao): PendingTransactionRepository =
        PendingTransactionRepository(dao)

    @Provides
    @Singleton
    fun provideCaptureTransactionUseCase(
        addTransaction: AddTransactionUseCase,
        pendingRepo: PendingTransactionRepository,
        transactionRepo: TransactionRepository,
        categoryRepo: CategoryRepository,
        feedbackNotifier: CaptureFeedbackNotifier
    ): CaptureTransactionUseCase =
        CaptureTransactionUseCase(addTransaction, pendingRepo, transactionRepo, categoryRepo, feedbackNotifier)

    @Provides
    @Singleton
    fun provideCaptureFeedbackNotifier(@ApplicationContext context: Context): CaptureFeedbackNotifier =
        AndroidCaptureFeedbackNotifier(context)

    @Provides
    @Singleton
    fun provideConfirmPendingTransactionUseCase(
        addTransaction: AddTransactionUseCase,
        pendingRepo: PendingTransactionRepository
    ): ConfirmPendingTransactionUseCase =
        ConfirmPendingTransactionUseCase(addTransaction, pendingRepo)
}
