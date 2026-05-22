package com.financial.freedom.domain.calculator

import android.util.Log
import com.financial.freedom.data.local.dao.DailyBreakdownItemDao
import com.financial.freedom.data.local.dao.DailySummaryDao
import com.financial.freedom.data.local.dao.DepositDao
import com.financial.freedom.data.local.dao.ExchangeRateDao
import com.financial.freedom.data.local.dao.HoldingDao
import com.financial.freedom.data.local.dao.PriceSnapshotDao
import com.financial.freedom.data.local.entity.DailyBreakdownItem
import com.financial.freedom.data.local.entity.DailySummary
import com.financial.freedom.data.local.entity.PriceSnapshot
import com.financial.freedom.data.remote.PriceService
import kotlinx.datetime.Clock
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.plus
import kotlinx.datetime.todayIn
import java.math.BigDecimal
import java.math.RoundingMode
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BackfillEngine @Inject constructor(
    private val depositDao: DepositDao,
    private val holdingDao: HoldingDao,
    private val summaryDao: DailySummaryDao,
    private val breakdownDao: DailyBreakdownItemDao,
    private val exchangeRateDao: ExchangeRateDao,
    private val priceSnapshotDao: PriceSnapshotDao,
    private val priceService: PriceService,
    private val valuationCalculator: ValuationCalculator,
    private val interestCalculator: InterestCalculator
) {
    companion object {
        private const val TAG = "BackfillEngine"
    }

    suspend fun backfillIfNeeded(accountId: Long) {
        val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
        val lastDate = summaryDao.getLatestDate(accountId)

        Log.w(TAG, "backfillIfNeeded called, today=$today, lastDate=$lastDate, accountId=$accountId")

        if (lastDate != null && lastDate >= today) {
            Log.w(TAG, "Backfill not needed")
            return
        }

        val startDate = if (lastDate == null) {
            val deposits = depositDao.getAllList(accountId)
            val holdings = holdingDao.getAllList(accountId)
            val depositMin = deposits.minOfOrNull { it.startDate }
            val holdingMin = holdings.minOfOrNull { it.costDate }
            Log.w(TAG, "deposits=${deposits.size}, holdings=${holdings.size}, depositMin=$depositMin, holdingMin=$holdingMin")
            minOfNotNull(depositMin, holdingMin) ?: today
        } else {
            lastDate
        }

        Log.w(TAG, "Backfilling from $startDate to $today")
        backfillRange(startDate, today, accountId)
        Log.w(TAG, "Backfill complete")
    }

    /**
     * 标记从 fromDate 开始的数据为脏，删除后重新回填。
     * 当用户新增/编辑/删除资产时调用，确保历史收益数据正确。
     */
    suspend fun markDirtyAndBackfill(fromDate: LocalDate, accountId: Long) {
        val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
        Log.w(TAG, "markDirtyAndBackfill from=$fromDate, today=$today, accountId=$accountId")

        // 删除 fromDate 及之后的所有汇总和分类数据
        summaryDao.deleteFromDate(fromDate, accountId)
        breakdownDao.deleteFromDate(fromDate, accountId)
        Log.w(TAG, "Deleted summaries and breakdowns from $fromDate onwards")

        // 从 fromDate 的前一天开始回填（需要前一天的 totalValueCNY 计算 dayChange）
        val safeStart = fromDate
        backfillRange(safeStart, today, accountId)
        Log.w(TAG, "markDirtyAndBackfill complete")
    }

    private suspend fun backfillRange(start: LocalDate, end: LocalDate, accountId: Long) {
        val previousSummary = summaryDao.getByDate(start, accountId)
        val prevBreakdown = breakdownDao.getByDate(start, accountId)

        var previousTotal = previousSummary?.totalValueCNY
        var prevDeposit = prevBreakdown.firstOrNull { it.type == "DEPOSIT" }?.valueCNY
        var prevStock = prevBreakdown.firstOrNull { it.type == "STOCK" }?.valueCNY
        var prevFund = prevBreakdown.firstOrNull { it.type == "FUND" }?.valueCNY
        var prevGold = prevBreakdown.firstOrNull { it.type == "GOLD" }?.valueCNY

        var currentDate = start

        while (currentDate <= end) {
            try {
                val (summary, breakdowns) = backfillDay(
                    currentDate, previousTotal, prevDeposit, prevStock, prevFund, prevGold, accountId
                )
                summaryDao.insert(summary)
                breakdownDao.insertAll(breakdowns)
                previousTotal = summary.totalValueCNY
                prevDeposit = breakdowns.firstOrNull { it.type == "DEPOSIT" }?.valueCNY
                prevStock = breakdowns.firstOrNull { it.type == "STOCK" }?.valueCNY
                prevFund = breakdowns.firstOrNull { it.type == "FUND" }?.valueCNY
                prevGold = breakdowns.firstOrNull { it.type == "GOLD" }?.valueCNY
            } catch (e: Exception) {
                Log.e(TAG, "Backfill failed for $currentDate", e)
            }
            currentDate = currentDate.plus(1, kotlinx.datetime.DateTimeUnit.DAY)
        }
    }

    private suspend fun backfillDay(
        date: LocalDate,
        previousTotalCNY: BigDecimal?,
        prevDepositCNY: BigDecimal?,
        prevStockCNY: BigDecimal?,
        prevFundCNY: BigDecimal?,
        prevGoldCNY: BigDecimal?,
        accountId: Long
    ): Pair<DailySummary, List<DailyBreakdownItem>> {
        val deposits = depositDao.getActiveList(accountId)
        val holdings = holdingDao.getAllList(accountId)

        val exchangeRates = mutableMapOf<String, BigDecimal>()
        for (deposit in deposits) {
            if (deposit.currency != "CNY" && deposit.currency !in exchangeRates) {
                val rate = getExchangeRate(deposit.currency, "CNY", date)
                exchangeRates[deposit.currency] = rate
            }
        }

        var depositValue = BigDecimal.ZERO
        for (deposit in deposits) {
            val rate = if (deposit.currency == "CNY") BigDecimal.ONE
            else exchangeRates[deposit.currency] ?: BigDecimal.ONE

            val valCNY = valuationCalculator.calcDepositValueCNY(deposit, rate, date)
            depositValue += valCNY
        }

        var stockValue = BigDecimal.ZERO
        var fundValue = BigDecimal.ZERO
        var goldValue = BigDecimal.ZERO

        for (holding in holdings) {
            val price = getOrFetchPrice(holding.id, holding.type, holding.symbol, holding.market, date, accountId)
            if (price == null) continue

            val rate = if (holding.currency == "CNY") BigDecimal.ONE
            else getExchangeRate(holding.currency, "CNY", date)

            val valCNY = valuationCalculator.calcHoldingValueCNY(holding, price, rate)
            when (holding.type) {
                "STOCK" -> stockValue += valCNY
                "FUND" -> fundValue += valCNY
                "GOLD" -> goldValue += valCNY
            }
        }

        val totalValueCNY = depositValue + stockValue + fundValue + goldValue

        val dayChange = if (previousTotalCNY != null) {
            totalValueCNY.subtract(previousTotalCNY).setScale(2, RoundingMode.HALF_UP)
        } else BigDecimal.ZERO

        val dayChangePct = if (previousTotalCNY != null && previousTotalCNY > BigDecimal.ZERO) {
            dayChange.divide(previousTotalCNY, 6, RoundingMode.HALF_UP)
                .multiply(BigDecimal(100)).setScale(2, RoundingMode.HALF_UP)
        } else BigDecimal.ZERO

        val summary = DailySummary(
            date = date, totalValueCNY = totalValueCNY, dayChange = dayChange,
            dayChangePct = dayChangePct, accountId = accountId
        )

        val depositChange = if (prevDepositCNY != null) depositValue.subtract(prevDepositCNY).setScale(2, RoundingMode.HALF_UP) else BigDecimal.ZERO
        val stockChange = if (prevStockCNY != null) stockValue.subtract(prevStockCNY).setScale(2, RoundingMode.HALF_UP) else BigDecimal.ZERO
        val fundChange = if (prevFundCNY != null) fundValue.subtract(prevFundCNY).setScale(2, RoundingMode.HALF_UP) else BigDecimal.ZERO
        val goldChange = if (prevGoldCNY != null) goldValue.subtract(prevGoldCNY).setScale(2, RoundingMode.HALF_UP) else BigDecimal.ZERO

        val breakdowns = listOf(
            DailyBreakdownItem(date = date, type = "DEPOSIT", valueCNY = depositValue, changeCNY = depositChange, accountId = accountId),
            DailyBreakdownItem(date = date, type = "STOCK", valueCNY = stockValue, changeCNY = stockChange, accountId = accountId),
            DailyBreakdownItem(date = date, type = "FUND", valueCNY = fundValue, changeCNY = fundChange, accountId = accountId),
            DailyBreakdownItem(date = date, type = "GOLD", valueCNY = goldValue, changeCNY = goldChange, accountId = accountId)
        )

        return Pair(summary, breakdowns)
    }

    private suspend fun getOrFetchPrice(
        holdingId: Long,
        type: String,
        symbol: String,
        market: String,
        date: LocalDate,
        accountId: Long
    ): BigDecimal? {
        // 优先查该日期的快照
        val exact = priceSnapshotDao.getByHoldingAndDate(holdingId, date, accountId)
        if (exact != null) return exact.unitPrice

        // 降级：取最近缓存（不触发网络）
        val cached = priceSnapshotDao.getLatest(holdingId, accountId)
        if (cached != null) return cached.unitPrice

        // 无本地缓存时才尝试网络拉取
        val result = priceService.fetchPrice(type, symbol, market, date)
        if (result != null) {
            priceSnapshotDao.insert(
                PriceSnapshot(
                    holdingId = holdingId, date = result.date, unitPrice = result.price,
                    currency = result.currency, accountId = accountId
                )
            )
            return result.price
        }

        return null
    }

    private suspend fun getExchangeRate(from: String, to: String, date: LocalDate): BigDecimal {
        if (from == to) return BigDecimal.ONE

        val cached = exchangeRateDao.getRate(from, to, date)
        if (cached != null) return cached.rate

        val latest = exchangeRateDao.getLatestRates().firstOrNull { it.fromCurrency == from && it.toCurrency == to }
        return latest?.rate ?: BigDecimal.ONE
    }
}

private fun <T : Comparable<T>> minOfNotNull(a: T?, b: T?): T? = when {
    a == null -> b
    b == null -> a
    else -> minOf(a, b)
}
