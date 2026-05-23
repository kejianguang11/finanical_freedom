package com.financial.freedom.domain.backup

import android.util.Log
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
import com.financial.freedom.data.local.entity.CashTransaction
import com.financial.freedom.data.local.entity.DailyBreakdownItem
import com.financial.freedom.data.local.entity.DailySummary
import com.financial.freedom.data.local.entity.Debt
import com.financial.freedom.data.local.entity.Deposit
import com.financial.freedom.data.local.entity.ExchangeRate
import com.financial.freedom.data.local.entity.Holding
import com.financial.freedom.data.local.entity.PriceSnapshot
import com.financial.freedom.data.local.entity.Receivable
import com.financial.freedom.data.local.entity.Transaction
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

@Serializable
data class BackupData(
    val version: Int = 1,
    val exportedAt: String,
    val accountId: Long,
    val deposits: List<Deposit>,
    val holdings: List<Holding>,
    val priceSnapshots: List<PriceSnapshot>,
    val transactions: List<Transaction>,
    val dailySummaries: List<DailySummary>,
    val dailyBreakdownItems: List<DailyBreakdownItem>,
    val cashTransactions: List<CashTransaction>,
    val receivables: List<Receivable>,
    val debts: List<Debt>,
    val exchangeRates: List<ExchangeRate>
)

@Singleton
class BackupManager @Inject constructor(
    private val depositDao: DepositDao,
    private val holdingDao: HoldingDao,
    private val priceSnapshotDao: PriceSnapshotDao,
    private val transactionDao: TransactionDao,
    private val dailySummaryDao: DailySummaryDao,
    private val dailyBreakdownItemDao: DailyBreakdownItemDao,
    private val cashTransactionDao: CashTransactionDao,
    private val receivableDao: ReceivableDao,
    private val debtDao: DebtDao,
    private val exchangeRateDao: ExchangeRateDao
) {
    companion object {
        private const val TAG = "BackupManager"
        private const val CURRENT_VERSION = 1
    }

    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
    }

    fun parseBackup(jsonString: String): BackupData {
        val data = json.decodeFromString<BackupData>(jsonString)
        require(data.version == CURRENT_VERSION) { "备份文件版本不兼容: ${data.version} (需要 $CURRENT_VERSION)" }
        return data
    }

    suspend fun exportJson(accountId: Long): String {
        val now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
        val data = BackupData(
            version = CURRENT_VERSION,
            exportedAt = now.toString(),
            accountId = accountId,
            deposits = depositDao.getAllList(accountId),
            holdings = holdingDao.getAllList(accountId),
            priceSnapshots = priceSnapshotDao.getAllList(accountId),
            transactions = transactionDao.getAllList(accountId),
            dailySummaries = dailySummaryDao.getListByDateRange(
                kotlinx.datetime.LocalDate.parse("2000-01-01"),
                kotlinx.datetime.LocalDate.parse("2100-01-01"),
                accountId
            ),
            dailyBreakdownItems = dailyBreakdownItemDao.getByDateRange(
                kotlinx.datetime.LocalDate.parse("2000-01-01"),
                kotlinx.datetime.LocalDate.parse("2100-01-01"),
                accountId
            ),
            cashTransactions = cashTransactionDao.getAllList(accountId),
            receivables = receivableDao.getAllList(accountId),
            debts = debtDao.getAllList(accountId),
            exchangeRates = exchangeRateDao.getAllList()
        )
        val result = json.encodeToString(data)
        Log.w(TAG, "Exported backup: ${result.length} chars, accountId=$accountId")
        return result
    }

    suspend fun restoreFromJson(jsonString: String, targetAccountId: Long): BackupData {
        val data = json.decodeFromString<BackupData>(jsonString)

        require(data.version == CURRENT_VERSION) { "备份文件版本不兼容: ${data.version} (需要 $CURRENT_VERSION)" }
        require(data.accountId == targetAccountId) {
            "备份数据账号(${data.accountId})与当前账号($targetAccountId)不匹配"
        }

        Log.w(TAG, "Restoring backup: version=${data.version}, accountId=${data.accountId}, " +
                "deposits=${data.deposits.size}, holdings=${data.holdings.size}, " +
                "snapshots=${data.priceSnapshots.size}, txns=${data.transactions.size}, " +
                "summaries=${data.dailySummaries.size}, breakdowns=${data.dailyBreakdownItems.size}, " +
                "cashTxns=${data.cashTransactions.size}, receivables=${data.receivables.size}, " +
                "debts=${data.debts.size}, rates=${data.exchangeRates.size}")

        // Clear all current-account data
        dailyBreakdownItemDao.deleteByAccountId(targetAccountId)
        dailySummaryDao.deleteByAccountId(targetAccountId)
        transactionDao.deleteByAccountId(targetAccountId)
        priceSnapshotDao.deleteByAccountId(targetAccountId)
        holdingDao.deleteByAccountId(targetAccountId)
        depositDao.deleteByAccountId(targetAccountId)
        cashTransactionDao.deleteByAccountId(targetAccountId)
        receivableDao.deleteByAccountId(targetAccountId)
        debtDao.deleteByAccountId(targetAccountId)
        exchangeRateDao.deleteAll()

        // Restore
        if (data.exchangeRates.isNotEmpty()) exchangeRateDao.insertAll(data.exchangeRates)
        if (data.deposits.isNotEmpty()) depositDao.insertAll(data.deposits)
        if (data.holdings.isNotEmpty()) holdingDao.insertAll(data.holdings)
        if (data.priceSnapshots.isNotEmpty()) priceSnapshotDao.insertAll(data.priceSnapshots)
        if (data.transactions.isNotEmpty()) transactionDao.insertAll(data.transactions)
        if (data.dailySummaries.isNotEmpty()) dailySummaryDao.insertAll(data.dailySummaries)
        if (data.dailyBreakdownItems.isNotEmpty()) dailyBreakdownItemDao.insertAll(data.dailyBreakdownItems)
        if (data.cashTransactions.isNotEmpty()) cashTransactionDao.insertAll(data.cashTransactions)
        if (data.receivables.isNotEmpty()) receivableDao.insertAll(data.receivables)
        if (data.debts.isNotEmpty()) debtDao.insertAll(data.debts)

        Log.w(TAG, "Restore complete")
        return data
    }
}
