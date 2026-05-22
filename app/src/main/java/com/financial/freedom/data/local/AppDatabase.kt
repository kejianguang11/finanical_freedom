package com.financial.freedom.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.financial.freedom.data.local.dao.AccountDao
import com.financial.freedom.data.local.dao.DailyBreakdownItemDao
import com.financial.freedom.data.local.dao.DailySummaryDao
import com.financial.freedom.data.local.dao.DepositDao
import com.financial.freedom.data.local.dao.ExchangeRateDao
import com.financial.freedom.data.local.dao.HoldingDao
import com.financial.freedom.data.local.dao.PriceSnapshotDao
import com.financial.freedom.data.local.dao.TransactionDao
import com.financial.freedom.data.local.entity.Account
import com.financial.freedom.data.local.entity.DailyBreakdownItem
import com.financial.freedom.data.local.entity.DailySummary
import com.financial.freedom.data.local.entity.Deposit
import com.financial.freedom.data.local.entity.ExchangeRate
import com.financial.freedom.data.local.entity.Holding
import com.financial.freedom.data.local.entity.PriceSnapshot
import com.financial.freedom.data.local.entity.Transaction

@Database(
    entities = [
        Account::class,
        ExchangeRate::class,
        Deposit::class,
        Holding::class,
        PriceSnapshot::class,
        Transaction::class,
        DailySummary::class,
        DailyBreakdownItem::class,
    ],
    version = 4,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun exchangeRateDao(): ExchangeRateDao
    abstract fun depositDao(): DepositDao
    abstract fun holdingDao(): HoldingDao
    abstract fun priceSnapshotDao(): PriceSnapshotDao
    abstract fun transactionDao(): TransactionDao
    abstract fun dailySummaryDao(): DailySummaryDao
    abstract fun dailyBreakdownItemDao(): DailyBreakdownItemDao
    abstract fun accountDao(): AccountDao
}
