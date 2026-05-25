package com.financial.freedom.domain.calculator

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
import com.financial.freedom.data.local.entity.PriceSnapshot
import com.financial.freedom.data.remote.PriceService
import kotlinx.datetime.Clock
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlinx.datetime.todayIn
import kotlinx.datetime.until
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
    private val cashTransactionDao: CashTransactionDao,
    private val receivableDao: ReceivableDao,
    private val debtDao: DebtDao,
    private val transactionDao: TransactionDao,
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
        val count = summaryDao.countByAccountId(accountId)

        Log.w(TAG, "backfillIfNeeded called, today=$today, lastDate=$lastDate, count=$count, accountId=$accountId")

        if (lastDate != null && lastDate >= today && count >= 5) {
            // refreshData 在 backfill 之前插入了今日数据，需验证数据连续性
            val yesterday = today.minus(1, DateTimeUnit.DAY)
            val yesterdayExists = summaryDao.getByDate(yesterday, accountId) != null
            if (yesterdayExists) {
                Log.w(TAG, "Backfill not needed")
                return
            }
            Log.w(TAG, "Gap detected: yesterday missing despite lastDate=$lastDate, forcing backfill")
        }

        if (count < 5) {
            Log.w(TAG, "Only $count summary rows — forcing full backfill")
        }

        val forceFull = lastDate == null || count < 5
        val startDate = if (forceFull) {
            val deposits = depositDao.getAllList(accountId)
            val holdings = holdingDao.getAllList(accountId)
            val depositMin = deposits.minOfOrNull { it.startDate }
            val holdingMin = holdings.minOfOrNull { it.costDate }
            Log.w(TAG, "deposits=${deposits.size}, holdings=${holdings.size}, depositMin=$depositMin, holdingMin=$holdingMin")
            minOfNotNull(depositMin, holdingMin) ?: today
        } else {
            // 数据断层时从昨天的真实最后日期开始回填，而非从今天
            val realLastDate = summaryDao.getLatestDateBefore(today, accountId) ?: lastDate
            Log.w(TAG, "Partial backfill from realLastDate=$realLastDate")
            realLastDate
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

        // 从前一天开始回填，确保 fromDate 当天有正确的 previousTotalCNY 来计算 dayChange
        val safeStart = fromDate.minus(1, DateTimeUnit.DAY)
        backfillRange(safeStart, today, accountId)
        Log.w(TAG, "markDirtyAndBackfill complete")
    }

    private suspend fun backfillRange(start: LocalDate, end: LocalDate, accountId: Long) {
        val today = Clock.System.todayIn(TimeZone.currentSystemDefault())

        // 预先拉取所有持仓的历史价格，避免逐日 fallback 到 fetchPrice（返回实时价导致所有日期同价）
        val holdings = holdingDao.getAllList(accountId)
        val prefetchFailedSymbols = mutableSetOf<String>()
        for (h in holdings) {
            try {
                val historyResults = priceService.fetchHistory(h.type, h.symbol, h.market, start, end)
                if (historyResults.isNotEmpty()) {
                    val snapshots = historyResults.map { r ->
                        PriceSnapshot(
                            holdingId = h.id, date = r.date, unitPrice = r.price,
                            currency = r.currency, accountId = accountId
                        )
                    }
                    priceSnapshotDao.insertAll(snapshots)
                    if (h.name.isBlank()) {
                        val nameFromApi = historyResults.firstOrNull { it.name.isNotBlank() }?.name
                        if (nameFromApi != null) {
                            holdingDao.updateName(h.id, nameFromApi)
                        }
                    }
                    Log.w(TAG, "Pre-fetched ${snapshots.size} history snapshots for ${h.symbol}")
                } else {
                    Log.w(TAG, "WARNING: History pre-fetch returned EMPTY for ${h.symbol} (${h.type}/${h.market}) — prices will fall back to costPrice")
                    prefetchFailedSymbols.add(h.symbol)
                }
            } catch (e: Exception) {
                Log.w(TAG, "WARNING: History pre-fetch FAILED for ${h.symbol}: ${e.message} — prices will fall back to costPrice")
                prefetchFailedSymbols.add(h.symbol)
            }
        }

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

        // 到期存款处理移至此：确保历史 backfill 完成后才标记 settled，
        // 避免 getActiveList() 在 backfill 循环中漏掉已到期但历史期仍应估值的存款
        processDepositMaturities(end, accountId)

        if (prefetchFailedSymbols.isNotEmpty()) {
            Log.w(TAG, "Backfill complete with ${prefetchFailedSymbols.size} symbols missing price history: $prefetchFailedSymbols — affected holdings use costPrice fallback")
        }
        Log.w(TAG, "Backfill range $start ~ $end complete")
    }

    /**
     * 处理到期存款：maturityDate <= today 且 status=active 的存款
     * → status 变为 matured → 自动生成现金流水 → status 变为 settled
     */
    suspend fun processDepositMaturities(asOfDate: LocalDate, accountId: Long) {
        val activeDeposits = depositDao.getActiveList(accountId)
        for (d in activeDeposits) {
            if (d.maturityDate <= asOfDate) {
                // 计算累计利息
                val totalInterest = interestCalculator.accruedInterest(
                    d.principal, d.interestRate, d.startDate, d.maturityDate, d.maturityDate
                )
                val totalAmount = d.principal.add(totalInterest)

                // 更新状态为 matured
                depositDao.update(d.copy(status = "matured"))

                // 生成现金流水：存款到期入账
                cashTransactionDao.insert(
                    CashTransaction(
                        accountId = accountId,
                        date = d.maturityDate,
                        amount = totalAmount,
                        type = "DEPOSIT_MATURITY",
                        note = "${d.name}到期入账，本金${d.principal} ${d.currency}，利息${totalInterest} ${d.currency}"
                    )
                )

                // 更新状态为 settled
                depositDao.update(d.copy(status = "settled"))

                Log.w(TAG, "Deposit matured: ${d.name}, amount=$totalAmount, date=${d.maturityDate}")
            }
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
        val deposits = depositDao.getAllList(accountId).filter { it.startDate <= date && date <= it.maturityDate }
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
            // 成本日之前不参与估值，避免用未来价格回填历史
            if (date < holding.costDate) continue
            // 无价格快照时用成本价兜底，避免历史估值为 0 导致价格出现后单日跳变
            val price = getOrFetchPrice(holding.id, holding.type, holding.symbol, holding.market, date, accountId)
                ?: holding.costPrice

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

        // 计算当日净入金（新增存款本金 + 股票买入成本），不计入收益率
        var netInflow = BigDecimal.ZERO
        var depositContribution = BigDecimal.ZERO
        for (deposit in deposits) {
            if (deposit.startDate == date) {
                val rate = if (deposit.currency == "CNY") BigDecimal.ONE
                else exchangeRates[deposit.currency] ?: BigDecimal.ONE
                val principalCNY = deposit.principal.multiply(rate).setScale(2, RoundingMode.HALF_UP)
                netInflow += principalCNY
                depositContribution += principalCNY
            }
        }
        // 买入交易成本计入 netInflow，同时按类型拆分，避免各分类变动包含成本基数
        val txs = transactionDao.getAllList(accountId).filter { it.date == date && it.type == "BUY" }
        var stockBuyCost = BigDecimal.ZERO
        var fundBuyCost = BigDecimal.ZERO
        var goldBuyCost = BigDecimal.ZERO
        for (tx in txs) {
            val h = holdings.firstOrNull { it.id == tx.holdingId } ?: continue
            val rate = if (h.currency == "CNY") BigDecimal.ONE
            else getExchangeRate(h.currency, "CNY", date)
            val costCNY = tx.price.multiply(tx.quantity).add(tx.fee).multiply(rate).setScale(2, RoundingMode.HALF_UP)
            netInflow += costCNY
            when (h.type) {
                "STOCK" -> stockBuyCost += costCNY
                "FUND" -> fundBuyCost += costCNY
                "GOLD" -> goldBuyCost += costCNY
            }
        }

        // 一次 setScale 避免双重舍入导致 dayChange 与 breakdown 之和不一致
        val dayChange = if (previousTotalCNY != null) {
            totalValueCNY.subtract(previousTotalCNY).subtract(netInflow).setScale(2, RoundingMode.HALF_UP)
        } else BigDecimal.ZERO

        val dayChangePct = if (previousTotalCNY != null && previousTotalCNY > BigDecimal.ZERO) {
            dayChange.divide(previousTotalCNY, 6, RoundingMode.HALF_UP)
                .multiply(BigDecimal(100)).setScale(2, RoundingMode.HALF_UP)
        } else BigDecimal.ZERO

        val cashBal = cashTransactionDao.getCumulativeBalance(accountId, date)
        val rcvTotal = receivableDao.getAllList(accountId).sumOf { it.amount }
        val dbtTotal = debtDao.getAllList(accountId).sumOf { it.amount }
        val netWorth = totalValueCNY.add(cashBal).add(rcvTotal).subtract(dbtTotal)

        val summary = DailySummary(
            date = date, totalValueCNY = totalValueCNY, dayChange = dayChange,
            dayChangePct = dayChangePct, netInflow = netInflow, accountId = accountId,
            netWorth = netWorth, cashBalance = cashBal
        )

        // 存款变动使用直接公式（日利息），与持仓页保持一致
        var directDepositChange = BigDecimal.ZERO
        for (deposit in deposits) {
            val rate = if (deposit.currency == "CNY") BigDecimal.ONE
            else exchangeRates[deposit.currency] ?: BigDecimal.ONE
            directDepositChange += deposit.principal.multiply(deposit.interestRate)
                .divide(BigDecimal(365), 6, RoundingMode.HALF_UP)
                .multiply(rate)
        }
        directDepositChange = directDepositChange.setScale(2, RoundingMode.HALF_UP)

        // stock/fund/gold 各自独立舍入，扣除当日买入成本避免成本基数计为收益
        val stockChange = if (prevStockCNY != null) stockValue.subtract(prevStockCNY).subtract(stockBuyCost).setScale(2, RoundingMode.HALF_UP) else BigDecimal.ZERO
        val fundChange = if (prevFundCNY != null) fundValue.subtract(prevFundCNY).subtract(fundBuyCost).setScale(2, RoundingMode.HALF_UP) else BigDecimal.ZERO
        val goldChange = if (prevGoldCNY != null) goldValue.subtract(prevGoldCNY).subtract(goldBuyCost).setScale(2, RoundingMode.HALF_UP) else BigDecimal.ZERO

        val fxResidual = dayChange.subtract(directDepositChange).subtract(stockChange).subtract(fundChange).subtract(goldChange)

        val breakdowns = mutableListOf(
            DailyBreakdownItem(date = date, type = "DEPOSIT", valueCNY = depositValue, changeCNY = directDepositChange, contribution = netInflow, accountId = accountId),
            DailyBreakdownItem(date = date, type = "STOCK", valueCNY = stockValue, changeCNY = stockChange, accountId = accountId),
            DailyBreakdownItem(date = date, type = "FUND", valueCNY = fundValue, changeCNY = fundChange, accountId = accountId),
            DailyBreakdownItem(date = date, type = "GOLD", valueCNY = goldValue, changeCNY = goldChange, accountId = accountId)
        )
        if (fxResidual.abs() > BigDecimal.ZERO) {
            breakdowns += DailyBreakdownItem(date = date, type = "FX", valueCNY = BigDecimal.ZERO, changeCNY = fxResidual, accountId = accountId)
        }

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

        // 降级：取该日期之前最近的价格，不用未来价格回填历史
        val cached = priceSnapshotDao.getLatestBefore(holdingId, date, accountId)
        if (cached != null) return cached.unitPrice

        // 仅对「今天」尝试网络拉取实时价；历史日期已在 pre-fetch 阶段用 fetchHistory 获取，
        // 若仍未命中缓存则说明该日期无真实历史数据，跳过比用实时价冒充历史价更好
        val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
        if (date == today) {
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
