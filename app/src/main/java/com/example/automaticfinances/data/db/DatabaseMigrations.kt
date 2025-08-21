package com.example.automaticfinances.data.db

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(database: SupportSQLiteDatabase) {
        // 1. Crear tabla categories
        database.execSQL("""
            CREATE TABLE IF NOT EXISTS categories (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                name TEXT NOT NULL,
                color TEXT NOT NULL,
                icon TEXT NOT NULL,
                isDefault INTEGER NOT NULL DEFAULT 0,
                isActive INTEGER NOT NULL DEFAULT 1
            )
        """)
        
        // 2. Poblar categorías por defecto
        val defaultCategories = listOf(
            "('Comida obligatoria', '#4CAF50', '🍽️', 1, 1)",
            "('Arriendo', '#2196F3', '🏠', 1, 1)",
            "('Salud', '#F44336', '🏥', 1, 1)",
            "('Comida por fuera', '#FF9800', '🍔', 1, 1)",
            "('Gasolina', '#795548', '⛽', 1, 1)",
            "('Transporte', '#607D8B', '🚗', 1, 1)",
            "('Entretenimiento', '#9C27B0', '🎬', 1, 1)",
            "('Ropa', '#E91E63', '👕', 1, 1)",
            "('Servicios', '#3F51B5', '🔧', 1, 1)",
            "('Otros', '#9E9E9E', '📦', 1, 1)"
        )
        
        for (category in defaultCategories) {
            database.execSQL("INSERT INTO categories (name, color, icon, isDefault, isActive) VALUES $category")
        }
        
        // 3. Crear nueva tabla transactions temporal
        database.execSQL("""
            CREATE TABLE transactions_new (
                id TEXT PRIMARY KEY NOT NULL,
                ts INTEGER NOT NULL,
                date TEXT NOT NULL,
                time TEXT NOT NULL,
                type TEXT NOT NULL,
                description TEXT NOT NULL,
                amountCents INTEGER NOT NULL,
                currency TEXT NOT NULL,
                srcLast4 TEXT,
                dstLast4 TEXT,
                source TEXT NOT NULL,
                categoryId INTEGER,
                notes TEXT NOT NULL DEFAULT '',
                rawPreview TEXT NOT NULL,
                FOREIGN KEY(categoryId) REFERENCES categories(id) ON DELETE SET NULL
            )
        """)
        
        // 4. Migrar datos existentes
        // Primero obtenemos todos los datos de la tabla vieja
        database.execSQL("""
            INSERT INTO transactions_new (
                id, ts, date, time, type, description, amountCents, 
                currency, srcLast4, dstLast4, source, categoryId, notes, rawPreview
            )
            SELECT 
                id, 
                ts,
                CASE 
                    WHEN ts > 0 THEN date(ts/1000, 'unixepoch', 'localtime')
                    ELSE '2024-01-01'
                END as date,
                CASE 
                    WHEN ts > 0 THEN time(ts/1000, 'unixepoch', 'localtime')
                    ELSE '00:00'
                END as time,
                type,
                COALESCE(merchant, 'Transacción') as description,
                amountCents,
                currency,
                srcLast4,
                dstLast4,
                source,
                NULL as categoryId,
                '' as notes,
                rawPreview
            FROM transactions
        """)
        
        // 5. Eliminar tabla vieja y renombrar
        database.execSQL("DROP TABLE transactions")
        database.execSQL("ALTER TABLE transactions_new RENAME TO transactions")
        
        // 6. Crear índices para mejor performance
        database.execSQL("CREATE INDEX IF NOT EXISTS index_transactions_date ON transactions(date)")
        database.execSQL("CREATE INDEX IF NOT EXISTS index_transactions_categoryId ON transactions(categoryId)")
        database.execSQL("CREATE INDEX IF NOT EXISTS index_transactions_type ON transactions(type)")
    }
}

val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(database: SupportSQLiteDatabase) {
        // Crear tabla user_category_preferences para aprendizaje inteligente
        database.execSQL("""
            CREATE TABLE IF NOT EXISTS user_category_preferences (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                merchantKey TEXT NOT NULL,
                categoryId INTEGER NOT NULL,
                confidence REAL NOT NULL DEFAULT 1.0,
                frequency INTEGER NOT NULL DEFAULT 1,
                lastUsed INTEGER NOT NULL DEFAULT ${System.currentTimeMillis()},
                source TEXT NOT NULL DEFAULT 'user',
                isActive INTEGER NOT NULL DEFAULT 1,
                FOREIGN KEY(categoryId) REFERENCES categories(id) ON DELETE CASCADE
            )
        """)
        
        // Crear índices para performance optimizada
        database.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_user_category_preferences_merchantKey ON user_category_preferences(merchantKey)")
        database.execSQL("CREATE INDEX IF NOT EXISTS index_user_category_preferences_categoryId ON user_category_preferences(categoryId)")
        database.execSQL("CREATE INDEX IF NOT EXISTS index_user_category_preferences_lastUsed ON user_category_preferences(lastUsed)")
        database.execSQL("CREATE INDEX IF NOT EXISTS index_user_category_preferences_frequency ON user_category_preferences(frequency)")
        
        // Poblar con datos existentes de transacciones para inicializar el aprendizaje
        database.execSQL("""
            INSERT OR IGNORE INTO user_category_preferences (merchantKey, categoryId, frequency, source)
            SELECT 
                LOWER(TRIM(REPLACE(description, '  ', ' '))) as merchantKey,
                categoryId,
                COUNT(*) as frequency,
                'auto' as source
            FROM transactions 
            WHERE categoryId IS NOT NULL 
            GROUP BY LOWER(TRIM(REPLACE(description, '  ', ' '))), categoryId
            HAVING COUNT(*) >= 2
        """)
    }
}

val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(database: SupportSQLiteDatabase) {
        // 1. Crear tabla budgets
        database.execSQL("""
            CREATE TABLE IF NOT EXISTS budgets (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                categoryId INTEGER NOT NULL,
                limitAmountCents INTEGER NOT NULL,
                year INTEGER NOT NULL,
                month INTEGER NOT NULL,
                alertAt50Percent INTEGER NOT NULL DEFAULT 1,
                alertAt75Percent INTEGER NOT NULL DEFAULT 1,
                alertAt100Percent INTEGER NOT NULL DEFAULT 1,
                isActive INTEGER NOT NULL DEFAULT 1,
                createdAt INTEGER NOT NULL,
                FOREIGN KEY(categoryId) REFERENCES categories(id) ON DELETE CASCADE
            )
        """)
        
        // 2. Crear tabla financial_goals
        database.execSQL("""
            CREATE TABLE IF NOT EXISTS financial_goals (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                name TEXT NOT NULL,
                description TEXT NOT NULL DEFAULT '',
                targetAmountCents INTEGER NOT NULL,
                currentAmountCents INTEGER NOT NULL DEFAULT 0,
                type TEXT NOT NULL,
                categoryId INTEGER,
                targetDate INTEGER NOT NULL,
                isCompleted INTEGER NOT NULL DEFAULT 0,
                isActive INTEGER NOT NULL DEFAULT 1,
                createdAt INTEGER NOT NULL,
                FOREIGN KEY(categoryId) REFERENCES categories(id) ON DELETE SET NULL
            )
        """)
        
        // 3. Crear índices para performance
        database.execSQL("CREATE INDEX IF NOT EXISTS index_budgets_categoryId ON budgets(categoryId)")
        database.execSQL("CREATE INDEX IF NOT EXISTS index_budgets_year_month ON budgets(year, month)")
        database.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_budgets_category_month ON budgets(categoryId, year, month) WHERE isActive = 1")
        
        database.execSQL("CREATE INDEX IF NOT EXISTS index_financial_goals_categoryId ON financial_goals(categoryId)")
        database.execSQL("CREATE INDEX IF NOT EXISTS index_financial_goals_targetDate ON financial_goals(targetDate)")
        database.execSQL("CREATE INDEX IF NOT EXISTS index_financial_goals_type ON financial_goals(type)")
    }
}

val MIGRATION_4_5 = object : Migration(4, 5) {
    override fun migrate(database: SupportSQLiteDatabase) {
        // Agregar campo isIncome a la tabla transactions
        database.execSQL("ALTER TABLE transactions ADD COLUMN isIncome INTEGER NOT NULL DEFAULT 0")
        
        // Crear índice para mejor performance en queries de ingresos/gastos
        database.execSQL("CREATE INDEX IF NOT EXISTS index_transactions_isIncome ON transactions(isIncome)")
        
        // Crear categorías predefinidas para ingresos
        val incomeCategories = listOf(
            "('💰 Salario', '#4CAF50', '💰', 1, 1)",
            "('💼 Freelance', '#2196F3', '💼', 1, 1)", 
            "('🏪 Ventas', '#FF9800', '🏪', 1, 1)",
            "('🎁 Regalos', '#E91E63', '🎁', 1, 1)",
            "('📈 Inversiones', '#9C27B0', '📈', 1, 1)",
            "('💸 Devoluciones', '#607D8B', '💸', 1, 1)",
            "('🎯 Bonos', '#795548', '🎯', 1, 1)",
            "('📋 Otros ingresos', '#9E9E9E', '📋', 1, 1)"
        )
        
        for (category in incomeCategories) {
            database.execSQL("INSERT INTO categories (name, color, icon, isDefault, isActive) VALUES $category")
        }
    }
}

val MIGRATION_5_6 = object : Migration(5, 6) {
    override fun migrate(database: SupportSQLiteDatabase) {
        // Agregar índices para optimizar performance
        
        // Índices adicionales para transactions (algunos ya existen)
        database.execSQL("CREATE INDEX IF NOT EXISTS index_transactions_isIncome ON transactions(isIncome)")
        database.execSQL("CREATE INDEX IF NOT EXISTS index_transactions_date_isIncome ON transactions(date, isIncome)")
        
        // Índices para budgets
        database.execSQL("CREATE INDEX IF NOT EXISTS index_budgets_year_month ON budgets(year, month)")
        database.execSQL("CREATE INDEX IF NOT EXISTS index_budgets_category_month ON budgets(categoryId, year, month)")
        
        // Índices para financial_goals
        database.execSQL("CREATE INDEX IF NOT EXISTS index_financial_goals_targetDate ON financial_goals(targetDate)")
        database.execSQL("CREATE INDEX IF NOT EXISTS index_financial_goals_type ON financial_goals(type)")
        database.execSQL("CREATE INDEX IF NOT EXISTS index_financial_goals_isCompleted ON financial_goals(isCompleted)")
    }
}

val MIGRATION_6_7 = object : Migration(6, 7) {
    override fun migrate(database: SupportSQLiteDatabase) {
        // 1. Crear tabla accounts para balance tracking
        database.execSQL("""
            CREATE TABLE IF NOT EXISTS accounts (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                name TEXT NOT NULL,
                type TEXT NOT NULL,
                balanceCents INTEGER NOT NULL,
                isActive INTEGER NOT NULL DEFAULT 1,
                createdAt INTEGER NOT NULL
            )
        """)
        
        // 2. Crear índices para accounts
        database.execSQL("CREATE INDEX IF NOT EXISTS index_accounts_type ON accounts(type)")
        database.execSQL("CREATE INDEX IF NOT EXISTS index_accounts_isActive ON accounts(isActive)")
        database.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_accounts_name ON accounts(name)")
        
        // 3. Crear cuentas por defecto: Banco y Efectivo
        val currentTime = System.currentTimeMillis()
        database.execSQL("""
            INSERT INTO accounts (name, type, balanceCents, isActive, createdAt) 
            VALUES ('Banco', 'BANK', 0, 1, $currentTime)
        """)
        database.execSQL("""
            INSERT INTO accounts (name, type, balanceCents, isActive, createdAt) 
            VALUES ('Efectivo', 'CASH', 0, 1, $currentTime)
        """)
        
        // 4. Obtener IDs de las cuentas creadas
        // Banco = ID 1, Efectivo = ID 2 (por el orden de inserción)
        
        // 5. Agregar accountId a transactions table
        database.execSQL("ALTER TABLE transactions ADD COLUMN accountId INTEGER")
        
        // 6. Asignar cuentas a transacciones existentes
        // SMS transactions (notif:sms) → Banco (ID 1)
        database.execSQL("""
            UPDATE transactions 
            SET accountId = 1 
            WHERE source = 'notif:sms'
        """)
        
        // Manual transactions → Efectivo (ID 2)
        database.execSQL("""
            UPDATE transactions 
            SET accountId = 2 
            WHERE source != 'notif:sms'
        """)
        
        // 7. Calcular balances iniciales basados en transacciones existentes
        
        // Balance del banco (transacciones SMS)
        val bankBalanceCursor = database.query("""
            SELECT COALESCE(
                SUM(CASE 
                    WHEN isIncome = 1 THEN amountCents 
                    ELSE -amountCents 
                END), 0
            ) as balance
            FROM transactions 
            WHERE source = 'notif:sms'
        """)
        
        var bankBalance = 0L
        if (bankBalanceCursor.moveToFirst()) {
            bankBalance = bankBalanceCursor.getLong(0)
        }
        bankBalanceCursor.close()
        
        // Balance del efectivo (transacciones manuales)
        val cashBalanceCursor = database.query("""
            SELECT COALESCE(
                SUM(CASE 
                    WHEN isIncome = 1 THEN amountCents 
                    ELSE -amountCents 
                END), 0
            ) as balance
            FROM transactions 
            WHERE source != 'notif:sms'
        """)
        
        var cashBalance = 0L
        if (cashBalanceCursor.moveToFirst()) {
            cashBalance = cashBalanceCursor.getLong(0)
        }
        cashBalanceCursor.close()
        
        // 8. Actualizar balances calculados
        database.execSQL("UPDATE accounts SET balanceCents = $bankBalance WHERE name = 'Banco'")
        database.execSQL("UPDATE accounts SET balanceCents = $cashBalance WHERE name = 'Efectivo'")
        
        // 9. Crear índices para accountId en transactions
        database.execSQL("CREATE INDEX IF NOT EXISTS index_transactions_accountId ON transactions(accountId)")
        database.execSQL("CREATE INDEX IF NOT EXISTS index_transactions_accountId_date ON transactions(accountId, date)")
        
        // 10. Agregar foreign key constraint (recrear la tabla)
        database.execSQL("""
            CREATE TABLE transactions_new (
                id TEXT PRIMARY KEY NOT NULL,
                ts INTEGER NOT NULL,
                date TEXT NOT NULL,
                time TEXT NOT NULL,
                type TEXT NOT NULL,
                description TEXT NOT NULL,
                amountCents INTEGER NOT NULL,
                currency TEXT NOT NULL,
                srcLast4 TEXT,
                dstLast4 TEXT,
                source TEXT NOT NULL,
                categoryId INTEGER,
                accountId INTEGER,
                notes TEXT NOT NULL DEFAULT '',
                isIncome INTEGER NOT NULL DEFAULT 0,
                rawPreview TEXT NOT NULL,
                FOREIGN KEY(categoryId) REFERENCES categories(id) ON DELETE SET NULL,
                FOREIGN KEY(accountId) REFERENCES accounts(id) ON DELETE SET NULL
            )
        """)
        
        // Migrar todos los datos a la nueva tabla
        database.execSQL("""
            INSERT INTO transactions_new 
            SELECT * FROM transactions
        """)
        
        // Eliminar tabla vieja y renombrar
        database.execSQL("DROP TABLE transactions")
        database.execSQL("ALTER TABLE transactions_new RENAME TO transactions")
        
        // Recrear índices
        database.execSQL("CREATE INDEX IF NOT EXISTS index_transactions_categoryId ON transactions(categoryId)")
        database.execSQL("CREATE INDEX IF NOT EXISTS index_transactions_accountId ON transactions(accountId)")
        database.execSQL("CREATE INDEX IF NOT EXISTS index_transactions_date ON transactions(date)")
        database.execSQL("CREATE INDEX IF NOT EXISTS index_transactions_isIncome ON transactions(isIncome)")
        database.execSQL("CREATE INDEX IF NOT EXISTS index_transactions_date_isIncome ON transactions(date, isIncome)")
        database.execSQL("CREATE INDEX IF NOT EXISTS index_transactions_accountId_date ON transactions(accountId, date)")
    }
}