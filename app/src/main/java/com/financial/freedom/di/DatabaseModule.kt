package com.financial.freedom.di

import android.content.Context
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.financial.freedom.data.local.AppDatabase
import com.financial.freedom.data.local.dao.AccountDao
import com.financial.freedom.data.local.dao.CashTransactionDao
import com.financial.freedom.data.local.dao.DailyBreakdownItemDao
import com.financial.freedom.data.local.dao.DailySummaryDao
import com.financial.freedom.data.local.dao.DebtDao
import com.financial.freedom.data.local.dao.DepositDao
import com.financial.freedom.data.local.dao.ExchangeRateDao
import com.financial.freedom.data.local.dao.HoldingDao
import com.financial.freedom.data.local.dao.PriceSnapshotDao
import com.financial.freedom.data.local.dao.ReceivableDao
import com.financial.freedom.data.local.dao.TransactionDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, "financial_freedom.db")
            .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7, MIGRATION_7_8)
            .build()

    @Provides fun provideDepositDao(db: AppDatabase): DepositDao = db.depositDao()

    @Provides fun provideHoldingDao(db: AppDatabase): HoldingDao = db.holdingDao()

    @Provides fun provideDailySummaryDao(db: AppDatabase): DailySummaryDao = db.dailySummaryDao()

    @Provides fun provideDailyBreakdownItemDao(db: AppDatabase): DailyBreakdownItemDao = db.dailyBreakdownItemDao()

    @Provides fun providePriceSnapshotDao(db: AppDatabase): PriceSnapshotDao = db.priceSnapshotDao()

    @Provides fun provideExchangeRateDao(db: AppDatabase): ExchangeRateDao = db.exchangeRateDao()

    @Provides fun provideTransactionDao(db: AppDatabase): TransactionDao = db.transactionDao()

    @Provides fun provideAccountDao(db: AppDatabase): AccountDao = db.accountDao()

    @Provides fun provideCashTransactionDao(db: AppDatabase): CashTransactionDao = db.cashTransactionDao()

    @Provides fun provideReceivableDao(db: AppDatabase): ReceivableDao = db.receivableDao()

    @Provides fun provideDebtDao(db: AppDatabase): DebtDao = db.debtDao()

    private val MIGRATION_1_2 = object : Migration(1, 2) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE deposits ADD COLUMN accountId INTEGER NOT NULL DEFAULT 0")
            db.execSQL("ALTER TABLE holdings ADD COLUMN accountId INTEGER NOT NULL DEFAULT 0")
            db.execSQL("ALTER TABLE price_snapshots ADD COLUMN accountId INTEGER NOT NULL DEFAULT 0")
            db.execSQL("ALTER TABLE exchange_rates ADD COLUMN accountId INTEGER NOT NULL DEFAULT 0")
            db.execSQL("ALTER TABLE daily_summaries ADD COLUMN accountId INTEGER NOT NULL DEFAULT 0")
            db.execSQL("ALTER TABLE daily_breakdown_items ADD COLUMN accountId INTEGER NOT NULL DEFAULT 0")
            db.execSQL("ALTER TABLE transactions ADD COLUMN accountId INTEGER NOT NULL DEFAULT 0")
        }
    }

    private val MIGRATION_2_3 = object : Migration(2, 3) {
        override fun migrate(db: SupportSQLiteDatabase) {
            // Schema v2→v3: placeholder for future changes
        }
    }

    private val MIGRATION_3_4 = object : Migration(3, 4) {
        override fun migrate(db: SupportSQLiteDatabase) {
            // 删除重复行（保留最早的 id），然后创建唯一索引
            db.execSQL("""
                DELETE FROM exchange_rates WHERE id NOT IN (
                    SELECT MIN(id) FROM exchange_rates GROUP BY fromCurrency, toCurrency, date
                )
            """)
            db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_exchange_rates_from_to_date ON exchange_rates (fromCurrency, toCurrency, date)")
        }
    }

    private val MIGRATION_4_5 = object : Migration(4, 5) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("""
                CREATE TABLE IF NOT EXISTS cash_transactions (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    accountId INTEGER NOT NULL,
                    date TEXT NOT NULL,
                    amount TEXT NOT NULL,
                    type TEXT NOT NULL,
                    note TEXT NOT NULL DEFAULT ''
                )
            """)
            db.execSQL("""
                CREATE TABLE IF NOT EXISTS receivables (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    accountId INTEGER NOT NULL,
                    name TEXT NOT NULL,
                    amount TEXT NOT NULL,
                    date TEXT NOT NULL,
                    expectedDate TEXT,
                    note TEXT NOT NULL DEFAULT ''
                )
            """)
            db.execSQL("""
                CREATE TABLE IF NOT EXISTS debts (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    accountId INTEGER NOT NULL,
                    name TEXT NOT NULL,
                    amount TEXT NOT NULL,
                    date TEXT NOT NULL,
                    interestRate TEXT,
                    note TEXT NOT NULL DEFAULT ''
                )
            """)
        }
    }

    private val MIGRATION_5_6 = object : Migration(5, 6) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE daily_summaries ADD COLUMN netInflow TEXT NOT NULL DEFAULT '0'")
            db.execSQL("ALTER TABLE daily_breakdown_items ADD COLUMN contribution TEXT NOT NULL DEFAULT '0'")
        }
    }

    private val MIGRATION_6_7 = object : Migration(6, 7) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE holdings ADD COLUMN status TEXT NOT NULL DEFAULT 'active'")
        }
    }

    private val MIGRATION_7_8 = object : Migration(7, 8) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE daily_summaries ADD COLUMN netWorth TEXT NOT NULL DEFAULT '0'")
            db.execSQL("ALTER TABLE daily_summaries ADD COLUMN cashBalance TEXT NOT NULL DEFAULT '0'")
        }
    }
}
