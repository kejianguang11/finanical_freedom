package com.financial.freedom.data

import com.financial.freedom.data.local.dao.DepositDao
import com.financial.freedom.data.local.dao.ExchangeRateDao
import com.financial.freedom.data.local.dao.HoldingDao
import com.financial.freedom.data.local.dao.PriceSnapshotDao
import com.financial.freedom.data.local.dao.TransactionDao
import com.financial.freedom.data.local.entity.Deposit
import com.financial.freedom.data.local.entity.ExchangeRate
import com.financial.freedom.data.local.entity.Holding
import com.financial.freedom.data.local.entity.PriceSnapshot
import com.financial.freedom.data.local.entity.Transaction
import com.financial.freedom.domain.account.AccountManager
import com.financial.freedom.domain.calculator.BackfillEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.datetime.Clock
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlinx.datetime.todayIn
import java.math.BigDecimal
import java.math.RoundingMode
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TestDataGenerator @Inject constructor(
    private val depositDao: DepositDao,
    private val holdingDao: HoldingDao,
    private val priceSnapshotDao: PriceSnapshotDao,
    private val transactionDao: TransactionDao,
    private val exchangeRateDao: ExchangeRateDao,
    private val backfillEngine: BackfillEngine,
    private val accountManager: AccountManager
) {
    suspend fun generate() {
        val accountId = accountManager.currentAccountId.value ?: return
        withContext(Dispatchers.IO) {
            seedExchangeRates()
            generateDeposits(accountId)
            generateHoldings(accountId)

            // 找到最早的资产日期，从那里开始标记脏数据并重新回填
            val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
            val deposits = depositDao.getAllList(accountId)
            val holdings = holdingDao.getAllList(accountId)
            val depositMin = deposits.minOfOrNull { it.startDate }
            val holdingMin = holdings.minOfOrNull { it.costDate }
            val earliest = when {
                depositMin == null -> holdingMin
                holdingMin == null -> depositMin
                else -> minOf(depositMin, holdingMin)
            } ?: today

            backfillEngine.markDirtyAndBackfill(earliest, accountId)
        }
    }

    private suspend fun seedExchangeRates() {
        val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
        if (exchangeRateDao.getLatestRates().isNotEmpty()) return

        val rates = listOf(
            ExchangeRate(
                fromCurrency = "USD", toCurrency = "CNY",
                date = today, rate = BigDecimal("7.15")
            ),
            ExchangeRate(
                fromCurrency = "HKD", toCurrency = "CNY",
                date = today, rate = BigDecimal("0.92")
            )
        )
        exchangeRateDao.insertAll(rates)
    }

    private suspend fun generateDeposits(accountId: Long) {
        val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
        val deposits = listOf(
            Deposit(
                accountId = accountId,
                name = "定期存款-工商银行",
                bank = "工商银行",
                currency = "CNY",
                principal = BigDecimal("500000"),
                interestRate = BigDecimal("0.0275"),
                startDate = today.minus(180, DateTimeUnit.DAY),
                maturityDate = today.plus(185, DateTimeUnit.DAY),
                status = "active",
                note = "一年期定期"
            ),
            Deposit(
                accountId = accountId,
                name = "大额存单-招商银行",
                bank = "招商银行",
                currency = "CNY",
                principal = BigDecimal("300000"),
                interestRate = BigDecimal("0.0325"),
                startDate = today.minus(90, DateTimeUnit.DAY),
                maturityDate = today.plus(275, DateTimeUnit.DAY),
                status = "active",
                note = "大额存单 3.25%"
            ),
            Deposit(
                accountId = accountId,
                name = "美元定期-中国银行",
                bank = "中国银行",
                currency = "USD",
                principal = BigDecimal("20000"),
                interestRate = BigDecimal("0.045"),
                startDate = today.minus(120, DateTimeUnit.DAY),
                maturityDate = today.plus(245, DateTimeUnit.DAY),
                status = "active",
                note = "美元一年期"
            )
        )

        deposits.forEach { depositDao.insert(it) }
    }

    private suspend fun generateHoldings(accountId: Long) {
        val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
        // 茅台
        val moutaiCostDate = today.minus(90, DateTimeUnit.DAY)
        val moutaiCostPrice = BigDecimal("1680.00")
        val moutaiQuantity = BigDecimal("200")
        val moutaiHolding = Holding(
            accountId = accountId,
            type = "STOCK",
            symbol = "600519",
            name = "贵州茅台",
            market = "CN",
            currency = "CNY",
            quantity = moutaiQuantity,
            costPrice = moutaiCostPrice,
            costDate = moutaiCostDate,
            note = "白酒龙头"
        )
        val moutaiId = holdingDao.insert(moutaiHolding)

        // 腾讯
        val tencentCostDate = today.minus(60, DateTimeUnit.DAY)
        val tencentCostPrice = BigDecimal("380.00")
        val tencentQuantity = BigDecimal("500")
        val tencentHolding = Holding(
            accountId = accountId,
            type = "STOCK",
            symbol = "00700",
            name = "腾讯控股",
            market = "HK",
            currency = "HKD",
            quantity = tencentQuantity,
            costPrice = tencentCostPrice,
            costDate = tencentCostDate,
            note = "港股科技"
        )
        val tencentId = holdingDao.insert(tencentHolding)

        // 沪深300 ETF
        val fundCostDate = today.minus(45, DateTimeUnit.DAY)
        val fundCostPrice = BigDecimal("3.85")
        val fundQuantity = BigDecimal("50000")
        val fundHolding = Holding(
            accountId = accountId,
            type = "FUND",
            symbol = "510300",
            name = "沪深300ETF",
            market = "CN",
            currency = "CNY",
            quantity = fundQuantity,
            costPrice = fundCostPrice,
            costDate = fundCostDate,
            note = "指数基金定投"
        )
        val fundId = holdingDao.insert(fundHolding)

        // 黄金
        val goldCostDate = today.minus(30, DateTimeUnit.DAY)
        val goldCostPrice = BigDecimal("470.00")
        val goldQuantity = BigDecimal("100")
        val goldHolding = Holding(
            accountId = accountId,
            type = "GOLD",
            symbol = "AU9999",
            name = "黄金9999",
            market = "CN",
            currency = "CNY",
            quantity = goldQuantity,
            costPrice = goldCostPrice,
            costDate = goldCostDate,
            note = "实物黄金"
        )
        val goldId = holdingDao.insert(goldHolding)

        // 生成交易记录
        transactionDao.insert(Transaction(
            holdingId = moutaiId, accountId = accountId,
            type = "BUY", date = moutaiCostDate, price = moutaiCostPrice,
            quantity = moutaiQuantity, fee = BigDecimal("50.40")
        ))
        transactionDao.insert(Transaction(
            holdingId = tencentId, accountId = accountId,
            type = "BUY", date = tencentCostDate, price = tencentCostPrice,
            quantity = tencentQuantity, fee = BigDecimal("28.50")
        ))
        transactionDao.insert(Transaction(
            holdingId = fundId, accountId = accountId,
            type = "BUY", date = fundCostDate, price = fundCostPrice,
            quantity = fundQuantity, fee = BigDecimal("3.85")
        ))
        transactionDao.insert(Transaction(
            holdingId = goldId, accountId = accountId,
            type = "BUY", date = goldCostDate, price = goldCostPrice,
            quantity = goldQuantity, fee = BigDecimal("0")
        ))

        // 生成价格快照（90 天历史）
        generatePriceSnapshots(moutaiId, accountId, moutaiCostPrice, moutaiCostDate, today, 0.0003, 0.015)
        generatePriceSnapshots(tencentId, accountId, tencentCostPrice, tencentCostDate, today, 0.0002, 0.018)
        generatePriceSnapshots(fundId, accountId, fundCostPrice, fundCostDate, today, 0.0001, 0.008)
        generatePriceSnapshots(goldId, accountId, goldCostPrice, goldCostDate, today, 0.0002, 0.010)
    }

    private suspend fun generatePriceSnapshots(
        holdingId: Long,
        accountId: Long,
        startPrice: BigDecimal,
        startDate: LocalDate,
        endDate: LocalDate,
        dailyDrift: Double,
        dailyVolatility: Double
    ) {
        val rng = java.util.Random(holdingId * 31 + accountId)
        var price = startPrice.toDouble()
        var currentDate = startDate

        while (currentDate <= endDate) {
            val return_ = dailyDrift + rng.nextGaussian() * dailyVolatility
            price *= (1.0 + return_)
            if (price < 1.0) price = 1.0

            priceSnapshotDao.insert(
                PriceSnapshot(
                    holdingId = holdingId,
                    accountId = accountId,
                    date = currentDate,
                    unitPrice = BigDecimal(price).setScale(4, RoundingMode.HALF_UP),
                    currency = "CNY"
                )
            )
            currentDate = currentDate.plus(1, DateTimeUnit.DAY)
        }
    }
}
