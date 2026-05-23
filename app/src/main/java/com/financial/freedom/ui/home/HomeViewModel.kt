package com.financial.freedom.ui.home

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.financial.freedom.data.local.dao.DailyBreakdownItemDao
import com.financial.freedom.data.local.dao.DepositDao
import com.financial.freedom.data.local.dao.ExchangeRateDao
import com.financial.freedom.data.local.dao.HoldingDao
import com.financial.freedom.data.local.dao.PriceSnapshotDao
import com.financial.freedom.data.local.dao.TransactionDao
import com.financial.freedom.data.local.dao.DailySummaryDao
import com.financial.freedom.data.local.entity.DailyBreakdownItem
import com.financial.freedom.data.local.entity.DailySummary
import com.financial.freedom.data.local.entity.ExchangeRate
import com.financial.freedom.data.local.entity.PriceSnapshot
import com.financial.freedom.data.remote.PriceService
import com.financial.freedom.data.repository.CashRepository
import com.financial.freedom.data.repository.DebtRepository
import com.financial.freedom.data.repository.ReceivableRepository
import com.financial.freedom.data.repository.SummaryRepository
import com.financial.freedom.domain.account.AccountManager
import com.financial.freedom.domain.calculator.BackfillEngine
import com.financial.freedom.domain.calculator.DataVerifier
import com.financial.freedom.domain.calculator.InterestCalculator
import com.financial.freedom.domain.calculator.ValuationCalculator
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.todayIn
import java.math.BigDecimal
import java.math.RoundingMode
import javax.inject.Inject

data class HomeUiState(
    val totalValueCNY: String = "--,--,--.--",
    val todayChange: String = "+0.00",
    val todayChangePct: String = "+0.00%",
    val isUp: Boolean = true,
    val depositValue: String = "--,--",
    val depositChange: String = "+0",
    val stockValue: String = "--,--",
    val stockChange: String = "+0",
    val fundValue: String = "--,--",
    val fundChange: String = "+0",
    val goldValue: String = "--,--",
    val goldChange: String = "+0",
    val cashBalance: String = "0",
    val receivablesTotal: String = "0",
    val debtsTotal: String = "0",
    val netWorth: String = "--,--,--.--",
    val netWorthRaw: BigDecimal = BigDecimal.ZERO,
    val todayGainRaw: BigDecimal = BigDecimal.ZERO,
    val cumulativeContributions: String = "--,--",
    val cumulativeReturn: String = "+0",
    val cumulativeReturnPct: String = "+0.00%",
    val trendData: List<DailySummary> = emptyList(),
    val selectedTrendRange: TrendRange = TrendRange.WEEK,
    val isRefreshing: Boolean = false,
    val lastUpdateTime: String? = null,
    // Dopamine-hit fields
    val hoursSinceLastOpen: Int = 0,
    val passiveIncomeSinceLastOpen: String = "+0.00",
    val crossedMilestone: String? = null,
    val isAllTimeHigh: Boolean = false,
    // Investment breakdown for trend chart secondary line
    val investmentBreakdownMap: Map<LocalDate, BigDecimal> = emptyMap(),
    val displayMultiplier: BigDecimal = BigDecimal.ONE
)

enum class TrendRange { WEEK, MONTH, YEAR }

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val summaryRepository: SummaryRepository,
    private val depositDao: DepositDao,
    private val holdingDao: HoldingDao,
    private val priceSnapshotDao: PriceSnapshotDao,
    private val exchangeRateDao: ExchangeRateDao,
    private val breakdownDao: DailyBreakdownItemDao,
    private val dailySummaryDao: DailySummaryDao,
    private val backfillEngine: BackfillEngine,
    private val dataVerifier: DataVerifier,
    private val valuationCalculator: ValuationCalculator,
    private val interestCalculator: InterestCalculator,
    private val priceService: PriceService,
    private val cashRepository: CashRepository,
    private val receivableRepository: ReceivableRepository,
    private val debtRepository: DebtRepository,
    private val accountManager: AccountManager,
    @ApplicationContext private val context: Context,
    private val displaySettings: com.financial.freedom.domain.settings.DisplaySettings,
    private val transactionDao: TransactionDao
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private var needsRecompute = false
    private var initStarted = false
    private var badSymbolsFixed = false

    init {
        viewModelScope.launch {
            displaySettings.multiplierFlow.collect { multiplier ->
                _uiState.value = _uiState.value.copy(displayMultiplier = multiplier)
            }
        }
        // multiplier 变化时重新格式化所有显示值（跳过初始发射避免与 init 中的 refreshData 重复）
        viewModelScope.launch {
            displaySettings.multiplierFlow.drop(1).collect { _ ->
                val accountId = accountManager.currentAccountId.value ?: return@collect
                refreshData(accountId)
            }
        }
        viewModelScope.launch {
            if (initStarted) return@launch
            initStarted = true

            val accountId = accountManager.currentAccountId.value ?: return@launch
            val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
            Log.d("HomeVM", "init fast path start")

            // 0. 检查数据版本：计算逻辑变更时强制全量回填
            val dataVersionCurrent = 2
            val prefs = context.getSharedPreferences("account_prefs", Context.MODE_PRIVATE)
            val dataVersionStored = prefs.getInt("data_version", 0)
            val needsDataMigration = dataVersionStored < dataVersionCurrent

            // 1. 修正已知错误数据，再渲染本地缓存
            val symbolsFixed = fixBadSymbols(accountId)
            seedExchangeRatesIfNeeded()
            if (!symbolsFixed) {
                // 正常路径：用本地数据快速渲染
                refreshData(accountId)
                loadTrendData(_uiState.value.selectedTrendRange, accountId)
            }
            Log.d("HomeVM", "init fast path done — UI should have data now")

            // 2. 慢操作：回填 + 网络 + 快感数据
            try {
                Log.d("HomeVM", "starting backfill on IO...")
                val backfillStart = System.currentTimeMillis()
                withContext(Dispatchers.IO) {
                    if (symbolsFixed || needsDataMigration) {
                        // 修正了错误代码或数据版本升级后强制全量回填，不能用 backfillIfNeeded
                        // 因为 refreshData 可能已插入占位 summary 导致 backfillIfNeeded 跳过
                        val deposits = depositDao.getAllList(accountId)
                        val holdings = holdingDao.getAllList(accountId)
                        val depositMin = deposits.minOfOrNull { it.startDate }
                        val holdingMin = holdings.minOfOrNull { it.costDate }
                        val earliest = listOfNotNull(depositMin, holdingMin).minOrNull() ?: today
                        backfillEngine.markDirtyAndBackfill(earliest, accountId)
                        if (needsDataMigration) {
                            prefs.edit().putInt("data_version", dataVersionCurrent).apply()
                            Log.w("HomeVM", "Data migration complete: version $dataVersionStored → $dataVersionCurrent")
                        }
                    } else {
                        backfillEngine.backfillIfNeeded(accountId)
                    }
                }
                Log.d("HomeVM", "backfill done in ${System.currentTimeMillis() - backfillStart}ms")

                withContext(Dispatchers.IO) {
                    fetchLivePrices(accountId)
                }
                if (needsRecompute) {
                    computeFromEntities(accountId, today)
                }
                refreshData(accountId)
                loadTrendData(_uiState.value.selectedTrendRange, accountId)
            } catch (e: Exception) {
                Log.w("HomeVM", "Background fetch failed: ${e.message}")
            }

            updateDopamineState(accountId, today)

            // 3. 验证收益数据一致性
            try {
                dataVerifier.verifyAll(accountId)
            } catch (e: Exception) {
                Log.w("HomeVM", "Data verification failed: ${e.message}")
            }

            Log.d("HomeVM", "init fully complete")
        }
    }

    /**
     * 修正已知的错误股票代码（如 NETS → NTES）。
     * 清除旧快照和旧汇总数据，强制触发完整回填以使用正确价格。
     */
    private suspend fun fixBadSymbols(accountId: Long): Boolean {
        if (badSymbolsFixed) return false
        badSymbolsFixed = true
        var fixed = false
        try {
            val knownBad = mapOf("NETS" to "NTES")
            val allHoldings = holdingDao.getAllList(accountId)
            for (h in allHoldings) {
                val correct = knownBad[h.symbol] ?: continue
                Log.w("HomeVM", "Fixing bad symbol: ${h.symbol} → $correct for holding ${h.id}")
                priceSnapshotDao.deleteByHoldingId(h.id, accountId)
                holdingDao.updateSymbol(h.id, correct)
                dailySummaryDao.deleteFromDate(h.costDate, accountId)
                breakdownDao.deleteFromDate(h.costDate, accountId)
                Log.w("HomeVM", "Deleted summaries from ${h.costDate} to trigger full backfill for fixed holding")
                needsRecompute = true
                fixed = true
            }
        } catch (_: Exception) {}
        return fixed
    }

    private suspend fun seedExchangeRatesIfNeeded() {
        if (exchangeRateDao.getLatestRates().isNotEmpty()) return
        val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
        exchangeRateDao.insert(ExchangeRate(fromCurrency = "USD", toCurrency = "CNY", date = today, rate = BigDecimal("7.15")))
        exchangeRateDao.insert(ExchangeRate(fromCurrency = "HKD", toCurrency = "CNY", date = today, rate = BigDecimal("0.92")))
    }

    fun refresh() {
        viewModelScope.launch {
            val accountId = accountManager.currentAccountId.value ?: return@launch
            _uiState.value = _uiState.value.copy(isRefreshing = true)
            try {
                withContext(Dispatchers.IO) {
                    backfillEngine.backfillIfNeeded(accountId)
                    fetchLivePrices(accountId)
                }
            } catch (_: Exception) {}
            if (needsRecompute) {
                computeFromEntities(accountId, Clock.System.todayIn(TimeZone.currentSystemDefault()))
            }
            refreshData(accountId)
            loadTrendData(_uiState.value.selectedTrendRange, accountId)
            _uiState.value = _uiState.value.copy(isRefreshing = false)

            try {
                dataVerifier.verifyAll(accountId)
            } catch (e: Exception) {
                Log.w("HomeVM", "Data verification failed: ${e.message}")
            }
        }
    }

    fun dismissMilestone() {
        _uiState.value = _uiState.value.copy(crossedMilestone = null)
    }

    fun dismissAllTimeHigh() {
        _uiState.value = _uiState.value.copy(isAllTimeHigh = false)
    }

    fun selectTrendRange(range: TrendRange) {
        viewModelScope.launch {
            val accountId = accountManager.currentAccountId.value ?: return@launch
            _uiState.value = _uiState.value.copy(selectedTrendRange = range)
            loadTrendData(range, accountId)
        }
    }

    private suspend fun fetchLivePrices(accountId: Long) {
        val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
        val holdings = holdingDao.getAllList(accountId)
        var anySuccess = false

        for (h in holdings) {
            try {
                Log.d("HomeVM", "Fetching ${h.type} ${h.symbol}...")
                val result = priceService.fetchPrice(h.type, h.symbol, h.market, today)
                if (result != null) {
                    priceSnapshotDao.insert(
                        PriceSnapshot(
                            holdingId = h.id, date = result.date, unitPrice = result.price,
                            currency = result.currency, accountId = accountId
                        )
                    )
                    if (h.name.isBlank() && result.name.isNotBlank()) {
                        holdingDao.updateName(h.id, result.name)
                    }
                    anySuccess = true
                    Log.d("HomeVM", "Live price OK: ${h.symbol} = ${result.price} ${result.currency}")
                } else {
                    Log.w("HomeVM", "Live price NULL: ${h.symbol}")
                }
            } catch (e: Exception) {
                Log.w("HomeVM", "Live price FAIL: ${h.symbol} — ${e.message}")
            }
        }

        // Also try to fetch latest exchange rate
        try {
            val rateResult = priceService.fetchLatestRate("USD", "CNY")
            if (rateResult != null) {
                exchangeRateDao.insert(rateResult)
                Log.d("HomeVM", "Exchange rate updated: USD/CNY = ${rateResult.rate} (date=${rateResult.date})")
            } else {
                Log.w("HomeVM", "Exchange rate fetch returned null — cached rate may be stale")
            }
        } catch (e: Exception) {
            Log.w("HomeVM", "Exchange rate fetch FAILED: ${e.message}")
        }

        if (anySuccess) {
            needsRecompute = true
        }

        val now = System.currentTimeMillis()
        val hour = (now / 3600000 % 24).toInt()
        val min = (now / 60000 % 60).toInt()
        _uiState.value = _uiState.value.copy(
            lastUpdateTime = "${hour}:${min.toString().padStart(2, '0')}"
        )
        Log.d("HomeVM", "fetchLivePrices done. Success=$anySuccess, needsRecompute=$needsRecompute")
    }

    private suspend fun refreshData(accountId: Long) {
        val today = Clock.System.todayIn(TimeZone.currentSystemDefault())

        // 当日数据始终实时重算，不读缓存（因为当日价格/汇率/新增资产可能变化）
        computeFromEntities(accountId, today)
    }

    private suspend fun computeFromEntities(accountId: Long, asOfDate: LocalDate) {
        val yesterdayDate = asOfDate.minus(1, kotlinx.datetime.DateTimeUnit.DAY)
        val deposits = depositDao.getActiveList(accountId)
        val holdings = holdingDao.getAllList(accountId)

        val rates = mutableMapOf<String, BigDecimal>()

        var depositTotal = BigDecimal.ZERO
        for (d in deposits) {
            val rate = getOrFetchRate(d.currency, "CNY", asOfDate, rates)
            depositTotal += valuationCalculator.calcDepositValueCNY(d, rate, asOfDate)
        }

        var stockTotal = BigDecimal.ZERO
        var fundTotal = BigDecimal.ZERO
        var goldTotal = BigDecimal.ZERO

        // 用昨日真实快照价格算昨日总值，避免与 DailySummary 中可能存在的过期数据不连续
        var yesterdayStockTotal = BigDecimal.ZERO
        var yesterdayFundTotal = BigDecimal.ZERO
        var yesterdayGoldTotal = BigDecimal.ZERO

        for (h in holdings) {
            // 无价格快照时用成本价兜底，与 BackfillEngine 保持一致
            val todayPrice = priceSnapshotDao.getLatest(h.id, accountId)?.unitPrice ?: h.costPrice
            val rate = getOrFetchRate(h.currency, "CNY", asOfDate, rates)
            val todayVal = valuationCalculator.calcHoldingValueCNY(h, todayPrice, rate)

            // 成本日之前不参与昨日估值，与 BackfillEngine 保持一致
            val yesterdayVal = if (h.costDate <= yesterdayDate) {
                val yesterdayPrice = priceSnapshotDao.getByHoldingAndDate(h.id, yesterdayDate, accountId)?.unitPrice
                    ?: priceSnapshotDao.getLatestBefore(h.id, asOfDate, accountId)?.unitPrice
                    ?: h.costPrice
                valuationCalculator.calcHoldingValueCNY(h, yesterdayPrice, rate)
            } else {
                BigDecimal.ZERO
            }

            when (h.type) {
                "STOCK" -> { stockTotal += todayVal; yesterdayStockTotal += yesterdayVal }
                "FUND" -> { fundTotal += todayVal; yesterdayFundTotal += yesterdayVal }
                "GOLD" -> { goldTotal += todayVal; yesterdayGoldTotal += yesterdayVal }
            }
        }

        val totalCNY = depositTotal + stockTotal + fundTotal + goldTotal

        // 计算当日净入金（新增存款本金 + 股票买入成本），不计入收益率
        var netInflow = BigDecimal.ZERO
        for (d in deposits) {
            if (d.startDate == asOfDate) {
                val rate = getOrFetchRate(d.currency, "CNY", asOfDate, rates)
                netInflow += d.principal.multiply(rate).setScale(2, RoundingMode.HALF_UP)
            }
        }
        // 买入交易成本计入 netInflow，同时按类型拆分避免各分类变动包含成本基数
        val txs = withContext(Dispatchers.IO) { transactionDao.getAllList(accountId) }.filter { it.date == asOfDate && it.type == "BUY" }
        var stockBuyCost = BigDecimal.ZERO
        var fundBuyCost = BigDecimal.ZERO
        var goldBuyCost = BigDecimal.ZERO
        for (tx in txs) {
            val h = holdings.firstOrNull { it.id == tx.holdingId } ?: continue
            val rate = getOrFetchRate(h.currency, "CNY", asOfDate, rates)
            val costCNY = tx.price.multiply(tx.quantity).add(tx.fee).multiply(rate).setScale(2, RoundingMode.HALF_UP)
            netInflow += costCNY
            when (h.type) {
                "STOCK" -> stockBuyCost += costCNY
                "FUND" -> fundBuyCost += costCNY
                "GOLD" -> goldBuyCost += costCNY
            }
        }

        // 昨日存款总值从 breakdown 读取（存款利息计算稳定，无随机波动问题）
        val yesterdayBreakdown = summaryRepository.getBreakdown(yesterdayDate, accountId)
        val yesterdayByType = yesterdayBreakdown.associateBy { it.type }
        // 优先用昨日 breakdown；若无，则直接用 deposit 数据重算昨日存款估值，避免 fallback 到今日值
        val yesterdayDepositTotal = yesterdayByType["DEPOSIT"]?.valueCNY ?: run {
            var total = BigDecimal.ZERO
            for (d in deposits) {
                val rate = getOrFetchRate(d.currency, "CNY", yesterdayDate, rates)
                total += valuationCalculator.calcDepositValueCNY(d, rate, yesterdayDate)
            }
            total
        }

        val yesterdayTotal = yesterdayDepositTotal + yesterdayStockTotal + yesterdayFundTotal + yesterdayGoldTotal

        // 一次 setScale 避免双重舍入导致 dayChange 与 breakdown 之和不一致
        val dayChange = if (yesterdayTotal > BigDecimal.ZERO) {
            totalCNY.subtract(yesterdayTotal).subtract(netInflow).setScale(2, RoundingMode.HALF_UP)
        } else BigDecimal.ZERO
        val dayChangePct = if (yesterdayTotal > BigDecimal.ZERO) {
            dayChange.divide(yesterdayTotal, 6, RoundingMode.HALF_UP)
                .multiply(BigDecimal(100)).setScale(2, RoundingMode.HALF_UP)
        } else BigDecimal.ZERO

        // 存款变动使用直接公式（日利息），与持仓页/BackfillEngine 保持一致
        var directDepositChange = BigDecimal.ZERO
        for (d in deposits) {
            val rate = getOrFetchRate(d.currency, "CNY", asOfDate, rates)
            directDepositChange += d.principal.multiply(d.interestRate)
                .divide(BigDecimal(365), 6, RoundingMode.HALF_UP)
                .multiply(rate)
        }
        directDepositChange = directDepositChange.setScale(2, RoundingMode.HALF_UP)

        // stock/fund/gold 各自独立舍入，扣除当日买入成本避免成本基数计为收益
        val stockChange = stockTotal.subtract(yesterdayStockTotal).subtract(stockBuyCost).setScale(2, RoundingMode.HALF_UP)
        val fundChange = fundTotal.subtract(yesterdayFundTotal).subtract(fundBuyCost).setScale(2, RoundingMode.HALF_UP)
        val goldChange = goldTotal.subtract(yesterdayGoldTotal).subtract(goldBuyCost).setScale(2, RoundingMode.HALF_UP)

        val fxResidual = dayChange.subtract(directDepositChange).subtract(stockChange).subtract(fundChange).subtract(goldChange)

        // 累计投入 = 所有存款本金 + BUY 交易成本（CNY 折算）
        var cumulativeContributions = deposits.fold(BigDecimal.ZERO) { acc, d ->
            val rate = getOrFetchRate(d.currency, "CNY", asOfDate, rates)
            acc.add(d.principal.multiply(rate)).setScale(2, RoundingMode.HALF_UP)
        }
        val allTxs = withContext(Dispatchers.IO) { transactionDao.getAllList(accountId) }
        for (tx in allTxs) {
            if (tx.type != "BUY") continue
            val h = holdings.firstOrNull { it.id == tx.holdingId } ?: continue
            val rate = getOrFetchRate(h.currency, "CNY", asOfDate, rates)
            cumulativeContributions += tx.price.multiply(tx.quantity).add(tx.fee).multiply(rate).setScale(2, RoundingMode.HALF_UP)
        }
        val cumulativeReturn = totalCNY.subtract(cumulativeContributions).setScale(2, RoundingMode.HALF_UP)
        val cumulativeReturnPct = if (cumulativeContributions > BigDecimal.ZERO) {
            cumulativeReturn.divide(cumulativeContributions, 6, RoundingMode.HALF_UP)
                .multiply(BigDecimal(100)).setScale(2, RoundingMode.HALF_UP)
        } else BigDecimal.ZERO

        // Load cash/credit for net worth
        val cashBal = cashRepository.getBalance(accountId)
        val rcvTotalNw = receivableRepository.getTotal(accountId)
        val dbtTotalNw = debtRepository.getTotal(accountId)
        val netWorth = totalCNY.add(cashBal).add(rcvTotalNw).subtract(dbtTotalNw)

        // 今日收益 = 4 个可见分类变动之和（不含 FX 残差）
        val displayDayChange = directDepositChange + stockChange + fundChange + goldChange
        val displayDayChangePct = if (totalCNY > BigDecimal.ZERO) {
            displayDayChange.divide(totalCNY, 6, RoundingMode.HALF_UP)
                .multiply(BigDecimal(100)).setScale(2, RoundingMode.HALF_UP)
        } else BigDecimal.ZERO

        _uiState.value = _uiState.value.copy(
            totalValueCNY = formatMoneyShort(totalCNY),
            todayChange = formatSignedChange(displayDayChange),
            todayChangePct = "${displayDayChangePct}%",
            isUp = displayDayChange >= BigDecimal.ZERO,
            depositValue = formatMoney(depositTotal),
            depositChange = formatSignedChange(directDepositChange),
            stockValue = formatMoney(stockTotal),
            stockChange = formatSignedChange(stockChange),
            fundValue = formatMoney(fundTotal),
            fundChange = formatSignedChange(fundChange),
            goldValue = formatMoney(goldTotal),
            goldChange = formatSignedChange(goldChange),
            cashBalance = formatMoney(cashBal),
            receivablesTotal = formatMoney(rcvTotalNw),
            debtsTotal = formatMoney(dbtTotalNw),
            netWorth = formatMoney(netWorth),
            netWorthRaw = netWorth,
            todayGainRaw = displayDayChange,
            cumulativeContributions = formatMoney(cumulativeContributions),
            cumulativeReturn = formatSignedChange(cumulativeReturn),
            cumulativeReturnPct = "${cumulativeReturnPct}%"
        )

        // Persist today's summary to DB
        val summary = DailySummary(
            date = asOfDate, totalValueCNY = totalCNY, dayChange = dayChange,
            dayChangePct = dayChangePct, netInflow = netInflow, accountId = accountId,
            netWorth = netWorth, cashBalance = cashBal
        )
        val breakdowns = mutableListOf(
            DailyBreakdownItem(date = asOfDate, type = "DEPOSIT", valueCNY = depositTotal, changeCNY = directDepositChange, contribution = netInflow, accountId = accountId),
            DailyBreakdownItem(date = asOfDate, type = "STOCK", valueCNY = stockTotal, changeCNY = stockChange, accountId = accountId),
            DailyBreakdownItem(date = asOfDate, type = "FUND", valueCNY = fundTotal, changeCNY = fundChange, accountId = accountId),
            DailyBreakdownItem(date = asOfDate, type = "GOLD", valueCNY = goldTotal, changeCNY = goldChange, accountId = accountId)
        )
        if (fxResidual.abs() > BigDecimal.ZERO) {
            breakdowns += DailyBreakdownItem(date = asOfDate, type = "FX", valueCNY = BigDecimal.ZERO, changeCNY = fxResidual, accountId = accountId)
        }
        summaryRepository.saveTodaySummary(summary, breakdowns)
        needsRecompute = false
    }

    private fun formatSignedChange(change: BigDecimal): String {
        val multiplier = _uiState.value.displayMultiplier
        return com.financial.freedom.ui.common.FormatUtils.formatSignedChange(change, multiplier)
    }

    private fun formatMoneyShort(value: BigDecimal): String {
        val multiplier = _uiState.value.displayMultiplier
        return com.financial.freedom.ui.common.FormatUtils.formatMoneyShort(value, multiplier)
    }

    private suspend fun getOrFetchRate(from: String, to: String, date: LocalDate, cache: MutableMap<String, BigDecimal>): BigDecimal {
        if (from == to) return BigDecimal.ONE
        cache[from]?.let { return it }
        val cached = exchangeRateDao.getRate(from, to, date)
        if (cached != null) {
            cache[from] = cached.rate
            return cached.rate
        }
        val latest = exchangeRateDao.getLatestRates().firstOrNull { it.fromCurrency == from && it.toCurrency == to }
        val rate = latest?.rate ?: BigDecimal.ONE
        cache[from] = rate
        return rate
    }

    private suspend fun loadTrendData(range: TrendRange, accountId: Long) {
        val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
        val start = when (range) {
            TrendRange.WEEK -> today.minus(7, kotlinx.datetime.DateTimeUnit.DAY)
            TrendRange.MONTH -> today.minus(30, kotlinx.datetime.DateTimeUnit.DAY)
            TrendRange.YEAR -> today.minus(365, kotlinx.datetime.DateTimeUnit.DAY)
        }

        val data = summaryRepository.getListByDateRange(start, today, accountId)
        val invBreakdown = loadInvestmentBreakdown(start, today, accountId)
        _uiState.value = _uiState.value.copy(
            trendData = data,
            investmentBreakdownMap = invBreakdown
        )
    }

    private suspend fun loadInvestmentBreakdown(start: LocalDate, end: LocalDate, accountId: Long): Map<LocalDate, BigDecimal> {
        return breakdownDao.getByDateRange(start, end, accountId)
            .filter { it.type == "STOCK" || it.type == "FUND" || it.type == "GOLD" }
            .groupBy { it.date }
            .mapValues { (_, items) -> items.sumOf { it.valueCNY } }
    }

    private suspend fun updateDopamineState(accountId: Long, today: LocalDate) {
        // C1: "Since last open" tracking
        val prefs = context.getSharedPreferences("account_prefs", Context.MODE_PRIVATE)
        val now = System.currentTimeMillis()
        val lastOpen = prefs.getLong("last_open_time", 0L)
        val hoursSinceLastOpen = if (lastOpen > 0) ((now - lastOpen) / 3600000).toInt() else 0

        var passiveIncome = "+0.00"
        if (hoursSinceLastOpen >= 1) {
            val lastOpenDate = try {
                Clock.System.todayIn(TimeZone.currentSystemDefault())
                    .minus((hoursSinceLastOpen / 24).coerceAtLeast(1), kotlinx.datetime.DateTimeUnit.DAY)
            } catch (_: Exception) { today }
            val cumChange = dailySummaryDao.getCumulativeDayChange(accountId, lastOpenDate, today)
            if (cumChange > BigDecimal.ZERO) {
                passiveIncome = formatMoney(cumChange.abs())
            }
        }

        // C2: Milestone detection
        val currentNetWorth = _uiState.value.netWorthRaw
        val thresholds = listOf(
            BigDecimal("500000") to "50万",
            BigDecimal("1000000") to "100万",
            BigDecimal("1500000") to "150万",
            BigDecimal("2000000") to "200万",
            BigDecimal("3000000") to "300万",
            BigDecimal("5000000") to "500万",
            BigDecimal("10000000") to "1000万"
        )
        val yesterdaySummary = summaryRepository.getByDate(today.minus(1, kotlinx.datetime.DateTimeUnit.DAY), accountId)
        val yesterdayNetWorth = yesterdaySummary?.netWorth ?: BigDecimal.ZERO
        var crossedMilestone: String? = null
        for ((threshold, label) in thresholds) {
            if (yesterdayNetWorth < threshold && currentNetWorth >= threshold) {
                crossedMilestone = label
                break
            }
        }

        // C2: All-time high detection
        val allDailyGains = dailySummaryDao.getListByDateRange(
            today.minus(365, kotlinx.datetime.DateTimeUnit.DAY), today, accountId
        )
        val historicalMaxGain = allDailyGains.maxOfOrNull { it.dayChange } ?: BigDecimal.ZERO
        val todayGain = _uiState.value.todayGainRaw
        val isAllTimeHigh = todayGain > BigDecimal.ZERO && todayGain > historicalMaxGain

        prefs.edit().putLong("last_open_time", now).apply()

        _uiState.value = _uiState.value.copy(
            hoursSinceLastOpen = hoursSinceLastOpen,
            passiveIncomeSinceLastOpen = passiveIncome,
            crossedMilestone = crossedMilestone,
            isAllTimeHigh = isAllTimeHigh
        )
    }

    private fun breakdownValue(items: List<DailyBreakdownItem>, type: String): String {
        val item = items.firstOrNull { it.type == type }
        return item?.let { formatMoney(it.valueCNY) } ?: "--,--"
    }

    private fun breakdownChange(items: List<DailyBreakdownItem>, type: String): String {
        val item = items.firstOrNull { it.type == type }
        return item?.let {
            formatMoney(it.changeCNY.abs())
        } ?: "0"
    }

    private fun formatMoney(value: BigDecimal): String {
        val multiplier = _uiState.value.displayMultiplier
        return com.financial.freedom.ui.common.FormatUtils.formatMoney(value, multiplier)
    }
}
