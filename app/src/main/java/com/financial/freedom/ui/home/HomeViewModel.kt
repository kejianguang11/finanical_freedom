package com.financial.freedom.ui.home

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.financial.freedom.data.local.dao.DepositDao
import com.financial.freedom.data.local.dao.DailyBreakdownItemDao
import com.financial.freedom.data.local.dao.ExchangeRateDao
import com.financial.freedom.data.local.dao.HoldingDao
import com.financial.freedom.data.local.dao.PriceSnapshotDao
import com.financial.freedom.data.local.entity.DailyBreakdownItem
import com.financial.freedom.data.local.entity.DailySummary
import com.financial.freedom.data.local.entity.ExchangeRate
import com.financial.freedom.data.local.entity.PriceSnapshot
import com.financial.freedom.data.remote.PriceService
import com.financial.freedom.data.repository.SummaryRepository
import com.financial.freedom.domain.account.AccountManager
import com.financial.freedom.domain.calculator.BackfillEngine
import com.financial.freedom.domain.calculator.InterestCalculator
import com.financial.freedom.domain.calculator.ValuationCalculator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
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
    val trendData: List<DailySummary> = emptyList(),
    val selectedTrendRange: TrendRange = TrendRange.WEEK,
    val isRefreshing: Boolean = false,
    val lastUpdateTime: String? = null
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
    private val backfillEngine: BackfillEngine,
    private val valuationCalculator: ValuationCalculator,
    private val interestCalculator: InterestCalculator,
    private val priceService: PriceService,
    private val accountManager: AccountManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private var needsRecompute = false
    private var initStarted = false
    private var cacheCleared = false

    init {
        viewModelScope.launch {
            if (initStarted) return@launch
            initStarted = true

            val accountId = accountManager.currentAccountId.value ?: return@launch
            val today = Clock.System.todayIn(TimeZone.currentSystemDefault())

            // 一次性清理旧的黄金缓存价格，让新 API 生效
            if (!cacheCleared) {
                clearGoldPriceCache(accountId)
                cacheCleared = true
            }

            // 1. 立即用缓存数据渲染，不等待网络
            seedExchangeRatesIfNeeded()
            refreshData(accountId)
            loadTrendData(_uiState.value.selectedTrendRange, accountId)

            // 2. 后台补齐缺失数据 + 拉取最新价格
            try {
                backfillEngine.backfillIfNeeded(accountId)
                fetchLivePrices(accountId)
                if (needsRecompute) {
                    computeFromEntities(accountId, today)
                }
                refreshData(accountId)
                loadTrendData(_uiState.value.selectedTrendRange, accountId)
            } catch (e: Exception) {
                Log.w("HomeVM", "Background fetch failed: ${e.message}")
            }
        }
    }

    private suspend fun clearGoldPriceCache(accountId: Long) {
        try {
            val goldHoldings = holdingDao.getAllList(accountId).filter { it.type == "GOLD" }
            for (h in goldHoldings) {
                val latest = priceSnapshotDao.getLatest(h.id, accountId)
                // 旧黄金缓存可能价格偏高，清除后强制重新拉取
                if (latest != null) {
                    Log.w("HomeVM", "Deleting stale gold cache for holding ${h.id}: price=${latest.unitPrice}")
                    priceSnapshotDao.deleteByHoldingId(h.id, accountId)
                }
            }
        } catch (_: Exception) { }
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
            fetchLivePrices(accountId)
            if (needsRecompute) {
                computeFromEntities(accountId, Clock.System.todayIn(TimeZone.currentSystemDefault()))
            }
            loadTrendData(_uiState.value.selectedTrendRange, accountId)
            _uiState.value = _uiState.value.copy(isRefreshing = false)
        }
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
            }
        } catch (_: Exception) { }

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

        val todaySummary = summaryRepository.getByDate(today, accountId)

        if (todaySummary != null && todaySummary.totalValueCNY > BigDecimal.ZERO) {
            val breakdown = summaryRepository.getBreakdown(today, accountId)
            _uiState.value = _uiState.value.copy(
                totalValueCNY = formatMoney(todaySummary.totalValueCNY),
                todayChange = if (todaySummary.dayChange >= BigDecimal.ZERO) "+${formatMoney(todaySummary.dayChange)}" else formatMoney(todaySummary.dayChange),
                todayChangePct = if (todaySummary.dayChangePct >= BigDecimal.ZERO) "+${todaySummary.dayChangePct}%" else "${todaySummary.dayChangePct}%",
                isUp = todaySummary.dayChange >= BigDecimal.ZERO,
                depositValue = breakdownValue(breakdown, "DEPOSIT"),
                depositChange = breakdownChange(breakdown, "DEPOSIT"),
                stockValue = breakdownValue(breakdown, "STOCK"),
                stockChange = breakdownChange(breakdown, "STOCK"),
                fundValue = breakdownValue(breakdown, "FUND"),
                fundChange = breakdownChange(breakdown, "FUND"),
                goldValue = breakdownValue(breakdown, "GOLD"),
                goldChange = breakdownChange(breakdown, "GOLD")
            )
        } else {
            computeFromEntities(accountId, today)
        }
    }

    private suspend fun computeFromEntities(accountId: Long, asOfDate: LocalDate) {
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

        for (h in holdings) {
            val price = priceSnapshotDao.getLatest(h.id, accountId)?.unitPrice ?: continue
            val rate = getOrFetchRate(h.currency, "CNY", asOfDate, rates)
            val valCNY = valuationCalculator.calcHoldingValueCNY(h, price, rate)
            when (h.type) {
                "STOCK" -> stockTotal += valCNY
                "FUND" -> fundTotal += valCNY
                "GOLD" -> goldTotal += valCNY
            }
        }

        val totalCNY = depositTotal + stockTotal + fundTotal + goldTotal

        val yesterdayTotal = summaryRepository.getByDate(
            asOfDate.minus(1, kotlinx.datetime.DateTimeUnit.DAY), accountId
        )?.totalValueCNY
        val dayChange = if (yesterdayTotal != null) {
            totalCNY.subtract(yesterdayTotal).setScale(2, RoundingMode.HALF_UP)
        } else BigDecimal.ZERO
        val dayChangePct = if (yesterdayTotal != null && yesterdayTotal > BigDecimal.ZERO) {
            dayChange.divide(yesterdayTotal, 6, RoundingMode.HALF_UP)
                .multiply(BigDecimal(100)).setScale(2, RoundingMode.HALF_UP)
        } else BigDecimal.ZERO

        val yesterdayDate = asOfDate.minus(1, kotlinx.datetime.DateTimeUnit.DAY)
        val yesterdayBreakdown = summaryRepository.getBreakdown(yesterdayDate, accountId)
        val yesterdayByType = yesterdayBreakdown.associateBy { it.type }

        fun yesterdayValue(type: String): BigDecimal =
            yesterdayByType[type]?.valueCNY ?: BigDecimal.ZERO

        val depositChange = depositTotal.subtract(yesterdayValue("DEPOSIT")).setScale(0, RoundingMode.HALF_UP)
        val stockChange = stockTotal.subtract(yesterdayValue("STOCK")).setScale(0, RoundingMode.HALF_UP)
        val fundChange = fundTotal.subtract(yesterdayValue("FUND")).setScale(0, RoundingMode.HALF_UP)
        val goldChange = goldTotal.subtract(yesterdayValue("GOLD")).setScale(0, RoundingMode.HALF_UP)

        _uiState.value = _uiState.value.copy(
            totalValueCNY = formatMoney(totalCNY),
            todayChange = if (dayChange >= BigDecimal.ZERO) "+${formatMoney(dayChange)}" else formatMoney(dayChange),
            todayChangePct = if (dayChangePct >= BigDecimal.ZERO) "+${dayChangePct}%" else "${dayChangePct}%",
            isUp = dayChange >= BigDecimal.ZERO,
            depositValue = formatMoney(depositTotal),
            depositChange = formatSignedChange(depositChange),
            stockValue = formatMoney(stockTotal),
            stockChange = formatSignedChange(stockChange),
            fundValue = formatMoney(fundTotal),
            fundChange = formatSignedChange(fundChange),
            goldValue = formatMoney(goldTotal),
            goldChange = formatSignedChange(goldChange)
        )

        // Persist today's summary to DB
        val summary = DailySummary(
            date = asOfDate, totalValueCNY = totalCNY, dayChange = dayChange,
            dayChangePct = dayChangePct, accountId = accountId
        )
        val breakdowns = listOf(
            DailyBreakdownItem(date = asOfDate, type = "DEPOSIT", valueCNY = depositTotal, changeCNY = depositChange, accountId = accountId),
            DailyBreakdownItem(date = asOfDate, type = "STOCK", valueCNY = stockTotal, changeCNY = stockChange, accountId = accountId),
            DailyBreakdownItem(date = asOfDate, type = "FUND", valueCNY = fundTotal, changeCNY = fundChange, accountId = accountId),
            DailyBreakdownItem(date = asOfDate, type = "GOLD", valueCNY = goldTotal, changeCNY = goldChange, accountId = accountId)
        )
        summaryRepository.saveTodaySummary(summary, breakdowns)
        needsRecompute = false
    }

    private fun formatSignedChange(change: BigDecimal): String {
        return if (change >= BigDecimal.ZERO) "+${formatMoney(change)}" else formatMoney(change)
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
        _uiState.value = _uiState.value.copy(trendData = data)
    }

    private fun breakdownValue(items: List<DailyBreakdownItem>, type: String): String {
        val item = items.firstOrNull { it.type == type }
        return item?.let { formatMoney(it.valueCNY) } ?: "--,--"
    }

    private fun breakdownChange(items: List<DailyBreakdownItem>, type: String): String {
        val item = items.firstOrNull { it.type == type }
        return item?.let {
            val change = it.changeCNY
            if (change >= BigDecimal.ZERO) "+${formatMoney(change)}" else formatMoney(change)
        } ?: "+0"
    }

    private fun formatMoney(value: BigDecimal): String {
        val abs = value.abs()
        val integer = abs.toBigInteger().toString()
        val formatted = integer.reversed().chunked(3).joinToString(",").reversed()
        return if (value < BigDecimal.ZERO) "-$formatted" else formatted
    }
}
