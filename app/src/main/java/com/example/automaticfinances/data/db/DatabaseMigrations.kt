package com.example.automaticfinances.data.db

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // 1. Crear tabla categories
        db.execSQL("""
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
            db.execSQL("INSERT INTO categories (name, color, icon, isDefault, isActive) VALUES $category")
        }
        
        // 3. Crear nueva tabla transactions temporal
        db.execSQL("""
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
        db.execSQL("""
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
        db.execSQL("DROP TABLE transactions")
        db.execSQL("ALTER TABLE transactions_new RENAME TO transactions")
        
        // 6. Crear índices para mejor performance
        db.execSQL("CREATE INDEX IF NOT EXISTS index_transactions_date ON transactions(date)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_transactions_categoryId ON transactions(categoryId)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_transactions_type ON transactions(type)")
    }
}

val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // Crear tabla user_category_preferences para aprendizaje inteligente
        db.execSQL("""
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
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_user_category_preferences_merchantKey ON user_category_preferences(merchantKey)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_user_category_preferences_categoryId ON user_category_preferences(categoryId)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_user_category_preferences_lastUsed ON user_category_preferences(lastUsed)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_user_category_preferences_frequency ON user_category_preferences(frequency)")
        
        // Poblar con datos existentes de transacciones para inicializar el aprendizaje
        db.execSQL("""
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
    override fun migrate(db: SupportSQLiteDatabase) {
        // 1. Crear tabla budgets
        db.execSQL("""
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
        db.execSQL("""
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
        db.execSQL("CREATE INDEX IF NOT EXISTS index_budgets_categoryId ON budgets(categoryId)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_budgets_year_month ON budgets(year, month)")
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_budgets_category_month ON budgets(categoryId, year, month) WHERE isActive = 1")
        
        db.execSQL("CREATE INDEX IF NOT EXISTS index_financial_goals_categoryId ON financial_goals(categoryId)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_financial_goals_targetDate ON financial_goals(targetDate)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_financial_goals_type ON financial_goals(type)")
    }
}

val MIGRATION_4_5 = object : Migration(4, 5) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // Agregar campo isIncome a la tabla transactions
        db.execSQL("ALTER TABLE transactions ADD COLUMN isIncome INTEGER NOT NULL DEFAULT 0")
        
        // Crear índice para mejor performance en queries de ingresos/gastos
        db.execSQL("CREATE INDEX IF NOT EXISTS index_transactions_isIncome ON transactions(isIncome)")
        
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
            db.execSQL("INSERT INTO categories (name, color, icon, isDefault, isActive) VALUES $category")
        }
    }
}

val MIGRATION_5_6 = object : Migration(5, 6) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // Agregar índices para optimizar performance
        
        // Índices adicionales para transactions (algunos ya existen)
        db.execSQL("CREATE INDEX IF NOT EXISTS index_transactions_isIncome ON transactions(isIncome)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_transactions_date_isIncome ON transactions(date, isIncome)")
        
        // Índices para budgets
        db.execSQL("CREATE INDEX IF NOT EXISTS index_budgets_year_month ON budgets(year, month)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_budgets_category_month ON budgets(categoryId, year, month)")
        
        // Índices para financial_goals
        db.execSQL("CREATE INDEX IF NOT EXISTS index_financial_goals_targetDate ON financial_goals(targetDate)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_financial_goals_type ON financial_goals(type)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_financial_goals_isCompleted ON financial_goals(isCompleted)")
    }
}

val MIGRATION_6_7 = object : Migration(6, 7) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // 1. Crear tabla accounts para balance tracking
        db.execSQL("""
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
        db.execSQL("CREATE INDEX IF NOT EXISTS index_accounts_type ON accounts(type)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_accounts_isActive ON accounts(isActive)")
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_accounts_name ON accounts(name)")
        
        // 3. Crear cuentas por defecto: Banco y Efectivo
        val currentTime = System.currentTimeMillis()
        db.execSQL("""
            INSERT INTO accounts (name, type, balanceCents, isActive, createdAt) 
            VALUES ('Banco', 'BANK', 0, 1, $currentTime)
        """)
        db.execSQL("""
            INSERT INTO accounts (name, type, balanceCents, isActive, createdAt) 
            VALUES ('Efectivo', 'CASH', 0, 1, $currentTime)
        """)
        
        // 4. Obtener IDs de las cuentas creadas
        // Banco = ID 1, Efectivo = ID 2 (por el orden de inserción)
        
        // 5. Agregar accountId a transactions table
        db.execSQL("ALTER TABLE transactions ADD COLUMN accountId INTEGER")
        
        // 6. Asignar cuentas a transacciones existentes
        // SMS transactions (notif:sms) → Banco (ID 1)
        db.execSQL("""
            UPDATE transactions 
            SET accountId = 1 
            WHERE source = 'notif:sms'
        """)
        
        // Manual transactions → Efectivo (ID 2)
        db.execSQL("""
            UPDATE transactions 
            SET accountId = 2 
            WHERE source != 'notif:sms'
        """)
        
        // 7. Calcular balances iniciales basados en transacciones existentes
        
        // Balance del banco (transacciones SMS)
        val bankBalanceCursor = db.query("""
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
        val cashBalanceCursor = db.query("""
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
        db.execSQL("UPDATE accounts SET balanceCents = $bankBalance WHERE name = 'Banco'")
        db.execSQL("UPDATE accounts SET balanceCents = $cashBalance WHERE name = 'Efectivo'")
        
        // 9. Crear índices para accountId en transactions
        db.execSQL("CREATE INDEX IF NOT EXISTS index_transactions_accountId ON transactions(accountId)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_transactions_accountId_date ON transactions(accountId, date)")
        
        // 10. Agregar foreign key constraint (recrear la tabla)
        db.execSQL("""
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
        db.execSQL("""
            INSERT INTO transactions_new 
            SELECT * FROM transactions
        """)
        
        // Eliminar tabla vieja y renombrar
        db.execSQL("DROP TABLE transactions")
        db.execSQL("ALTER TABLE transactions_new RENAME TO transactions")
        
        // Recrear índices
        db.execSQL("CREATE INDEX IF NOT EXISTS index_transactions_categoryId ON transactions(categoryId)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_transactions_accountId ON transactions(accountId)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_transactions_date ON transactions(date)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_transactions_isIncome ON transactions(isIncome)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_transactions_date_isIncome ON transactions(date, isIncome)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_transactions_accountId_date ON transactions(accountId, date)")
    }
}

val MIGRATION_7_8 = object : Migration(7, 8) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // 1. Create opening_balances table
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS opening_balances (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                accountId INTEGER NOT NULL,
                effectiveDate TEXT NOT NULL,
                balanceCents INTEGER NOT NULL,
                note TEXT NOT NULL DEFAULT '',
                isActive INTEGER NOT NULL DEFAULT 1,
                createdAt INTEGER NOT NULL,
                FOREIGN KEY(accountId) REFERENCES accounts(id) ON DELETE CASCADE
            )
        """)
        
        // 2. Create indices for opening_balances
        db.execSQL("CREATE INDEX IF NOT EXISTS index_opening_balances_accountId ON opening_balances(accountId)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_opening_balances_effectiveDate ON opening_balances(effectiveDate)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_opening_balances_isActive ON opening_balances(isActive)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_opening_balances_accountId_effectiveDate ON opening_balances(accountId, effectiveDate)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_opening_balances_accountId_isActive ON opening_balances(accountId, isActive)")
        
        // 3. Optionally initialize opening balances with current account balances
        // This preserves existing balances as the "opening" balance from today
        val currentTime = System.currentTimeMillis()
        val today = java.time.LocalDate.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd"))
        
        // Get all active accounts and create opening balances for them
        val accountsCursor = db.query("SELECT id, balanceCents FROM accounts WHERE isActive = 1")
        
        while (accountsCursor.moveToNext()) {
            val accountId = accountsCursor.getLong(0)
            val balanceCents = accountsCursor.getLong(1)
            
            db.execSQL("""
                INSERT INTO opening_balances (accountId, effectiveDate, balanceCents, note, isActive, createdAt)
                VALUES ($accountId, '$today', $balanceCents, 'Balance inicial automático', 1, $currentTime)
            """)
        }
        accountsCursor.close()
    }
}

val MIGRATION_8_9 = object : Migration(8, 9) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // 1. Add isIncome field to categories table
        db.execSQL("ALTER TABLE categories ADD COLUMN isIncome INTEGER NOT NULL DEFAULT 0")
        
        // 2. Update existing income categories to mark them as income
        // Categories that are clearly income-related
        val incomeKeywords = listOf(
            "Salario", "Freelance", "Ventas", "Regalos", "Inversiones", 
            "Devoluciones", "Bonos", "ingresos"
        )
        
        for (keyword in incomeKeywords) {
            db.execSQL("""
                UPDATE categories 
                SET isIncome = 1 
                WHERE name LIKE '%$keyword%' AND isDefault = 1
            """)
        }
        
        // 3. Remove outdated income categories with emojis in names and create proper ones
        db.execSQL("""
            UPDATE categories 
            SET isActive = 0 
            WHERE name LIKE '💰%' OR name LIKE '💼%' OR name LIKE '🏪%' 
               OR name LIKE '🎁%' OR name LIKE '📈%' OR name LIKE '💸%' 
               OR name LIKE '🎯%' OR name LIKE '📋%'
        """)
        
        // 4. Insert new clean income categories
        val newIncomeCategories = listOf(
            "('Salario', '#4CAF50', '💰', 1, 1, 1)",
            "('Bonos', '#2196F3', '🎁', 1, 1, 1)",
            "('Venta personal', '#FF9800', '🛒', 1, 1, 1)",
            "('Regalo', '#E91E63', '🎀', 1, 1, 1)",
            "('Subsidio', '#9C27B0', '🏛️', 1, 1, 1)",
            "('Otros ingresos', '#607D8B', '💸', 1, 1, 1)"
        )
        
        for (category in newIncomeCategories) {
            db.execSQL("""
                INSERT OR IGNORE INTO categories (name, color, icon, isDefault, isActive, isIncome) 
                VALUES $category
            """)
        }
        
        // 5. Create index for better performance on income/expense queries
        db.execSQL("CREATE INDEX IF NOT EXISTS index_categories_isIncome ON categories(isIncome)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_categories_isIncome_isActive ON categories(isIncome, isActive)")
    }
}

val MIGRATION_10_11 = object : Migration(10, 11) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS merchant_resolutions (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                gatewayMerchant TEXT NOT NULL,
                realMerchant TEXT NOT NULL,
                suggestedCategoryId INTEGER,
                isPrePopulated INTEGER NOT NULL DEFAULT 0,
                usageCount INTEGER NOT NULL DEFAULT 0,
                createdAt INTEGER NOT NULL,
                lastUsedAt INTEGER
            )
        """)
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_merchant_resolutions_gatewayMerchant ON merchant_resolutions(gatewayMerchant)")
    }
}

val MIGRATION_11_12 = object : Migration(11, 12) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // Additive only: a passive diagnostic log of bank messages the parser missed (OBS-1).
        // No existing table is touched, so the financial data is untouched by this migration.
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS unparsed_sms (
                id TEXT PRIMARY KEY NOT NULL,
                text TEXT NOT NULL,
                source TEXT NOT NULL,
                receivedAt INTEGER NOT NULL
            )
        """)
        db.execSQL("CREATE INDEX IF NOT EXISTS index_unparsed_sms_receivedAt ON unparsed_sms(receivedAt)")
    }
}

val MIGRATION_12_13 = object : Migration(12, 13) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // Additive only: the "Por revisar" queue (PROD-1). Low-confidence captures land here and
        // stay OUT of `transactions` (and therefore out of the balance) until the user confirms.
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS pending_transactions (
                id TEXT PRIMARY KEY NOT NULL,
                ts INTEGER NOT NULL,
                type TEXT NOT NULL,
                description TEXT NOT NULL,
                amountCents INTEGER NOT NULL,
                currency TEXT NOT NULL,
                srcLast4 TEXT,
                dstLast4 TEXT,
                source TEXT NOT NULL,
                isIncome INTEGER NOT NULL,
                rawPreview TEXT NOT NULL,
                capturedAt INTEGER NOT NULL
            )
        """)
    }
}

val MIGRATION_13_14 = object : Migration(13, 14) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // FIN-4: one-shot retro-fix of the opening-balance bias. Data-only, no schema change.
        //
        // Auto-seeded opening balances (note = 'Balance inicial automático', created by
        // MIGRATION_7_8 and by early versions of initializeDefaultOpeningBalances) stored the
        // balance at the END of their effectiveDate — they already baked in that day's movements.
        // calculateCurrentBalanceWithOpening() then re-adds every movement from effectiveDate
        // forward, INCLUSIVE of effectiveDate, so the movements on that one day are counted twice
        // forever, inflating the derived balance.
        //
        // initializeDefaultOpeningBalances now seeds the START-of-day balance (the forward-fix), so
        // new installs are already correct and never run this migration. Here we correct rows
        // already on disk: reseat each active auto-seeded opening balance to the start of its own
        // effectiveDate by subtracting that day's net for its own account. User-configured opening
        // balances (a different note) are deliberately left untouched.
        db.execSQL(
            """
            UPDATE opening_balances
            SET balanceCents = balanceCents - COALESCE((
                SELECT SUM(CASE WHEN t.isIncome = 1 THEN t.amountCents ELSE -t.amountCents END)
                FROM transactions t
                WHERE t.accountId = opening_balances.accountId
                  AND t.date = opening_balances.effectiveDate
            ), 0)
            WHERE note = 'Balance inicial automático'
              AND isActive = 1
            """
        )
    }
}

val MIGRATION_14_15 = object : Migration(14, 15) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // MANT-2: move the hardcoded keyword→category rules into an editable table. Additive only;
        // no existing table is touched, so financial data is untouched. The CREATE TABLE intentionally
        // carries no DEFAULT clauses (the entity declares none — Kotlin constructor defaults are not
        // SQL defaults) so Room's schema-identity check matches the generated schema exactly.
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS category_rules (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                keyword TEXT NOT NULL,
                categoryName TEXT NOT NULL,
                isIncome INTEGER NOT NULL,
                isDefault INTEGER NOT NULL,
                createdAt INTEGER NOT NULL
            )
            """
        )
        db.execSQL(
            "CREATE UNIQUE INDEX IF NOT EXISTS index_category_rules_keyword_isIncome ON category_rules(keyword, isIncome)"
        )

        // Seed the same canonical defaults a fresh install gets, so upgraders keep working
        // categorization. INSERT OR IGNORE respects the unique index if this somehow runs twice.
        val now = System.currentTimeMillis()
        for (rule in DefaultCategoryRules.list) {
            db.execSQL(
                "INSERT OR IGNORE INTO category_rules (keyword, categoryName, isIncome, isDefault, createdAt) VALUES (?, ?, ?, ?, ?)",
                arrayOf<Any?>(
                    rule.keyword,
                    rule.categoryName,
                    if (rule.isIncome) 1 else 0,
                    if (rule.isDefault) 1 else 0,
                    now,
                ),
            )
        }
    }
}

val MIGRATION_15_16 = object : Migration(15, 16) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // Additive only: internal transfers between accounts (Banco <-> Efectivo). Both legs of a
        // transfer are marked isTransfer = 1 and share a transferGroupId. A transfer is not a real
        // income/expense, so it is excluded from income/expense totals (see TransactionDao) while
        // still moving each account's balance. No existing row is rewritten, so financial data is
        // untouched. The CREATE-less ALTERs carry SQL DEFAULTs only where the entity has a non-null
        // default (isTransfer); transferGroupId is nullable with no default, matching the entity.
        db.execSQL("ALTER TABLE transactions ADD COLUMN isTransfer INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE transactions ADD COLUMN transferGroupId TEXT")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_transactions_transferGroupId ON transactions(transferGroupId)")
    }
}

val MIGRATION_9_10 = object : Migration(9, 10) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // Remove budget alert fields and simplify budget management
        
        // 1. Create new budgets table without alert fields
        db.execSQL("""
            CREATE TABLE budgets_new (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                categoryId INTEGER NOT NULL,
                limitAmountCents INTEGER NOT NULL,
                year INTEGER NOT NULL,
                month INTEGER NOT NULL,
                isActive INTEGER NOT NULL DEFAULT 1,
                createdAt INTEGER NOT NULL,
                FOREIGN KEY(categoryId) REFERENCES categories(id) ON DELETE CASCADE
            )
        """)
        
        // 2. Copy existing data without alert fields
        db.execSQL("""
            INSERT INTO budgets_new (id, categoryId, limitAmountCents, year, month, isActive, createdAt)
            SELECT id, categoryId, limitAmountCents, year, month, isActive, createdAt
            FROM budgets
        """)
        
        // 3. Drop old table and rename new one
        db.execSQL("DROP TABLE budgets")
        db.execSQL("ALTER TABLE budgets_new RENAME TO budgets")
        
        // 4. Recreate indexes
        db.execSQL("CREATE INDEX IF NOT EXISTS index_budgets_categoryId ON budgets(categoryId)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_budgets_year_month ON budgets(year, month)")
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_budgets_category_month ON budgets(categoryId, year, month) WHERE isActive = 1")
    }
}