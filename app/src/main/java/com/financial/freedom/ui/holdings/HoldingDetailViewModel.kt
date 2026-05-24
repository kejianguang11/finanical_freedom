package com.financial.freedom.ui.holdings

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.financial.freedom.data.local.dao.CashTransactionDao
import com.financial.freedom.data.local.dao.HoldingDao
import com.financial.freedom.data.local.dao.PriceSnapshotDao
import com.financial.freedom.data.local.dao.TransactionDao
import com.financial.freedom.data.local.entity.CashTransaction
import com.financial.freedom.data.local.entity.Holding
import com.financial.freedom.data.local.entity.PriceSnapshot
import com.financial.freedom.data.local.entity.Transaction
import com.financial.freedom.data.remote.PriceService
import com.financial.freedom.data.repository.ExchangeRateRepository
import com.financial.freedom.domain.account.AccountManager
import com.financial.freedom.domain.calculator.BackfillEngine
import com.financial.freedom.domain.calculator.ValuationCalculator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.datetime.Clock
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.todayIn
import java.math.BigDecimal
import java.math.RoundingMode
import javax.inject.Inject

data class DetailUiState(
    val name: String = "",
    val symbol: String = "",
    val type: String = "",
    val status: String = "active",
    val currentPrice: String = "--",
    val priceChange: String = "+0.00",
    val priceChangePct: String = "+0.00%",
    val todayChange: String = "+0.00",
    val todayChangePct: String = "+0.00%",
    val isUp: Boolean = true,
    val totalPnL: String = "+0.00",
    val totalPnLPct: String = "+0.00%",
    val quantity: String = "--",
    val costPrice: String = "--",
    val totalCost: String = "--",
    val marketValue: String = "--",
    val transactions: List<Transaction> = emptyList(),
    val priceHistory: List<PriceSnapshot> = emptyList(),
    val selectedRange: Int = 0,
    val lastUpdateTime: String? = null,
    val showAddDialog: Boolean = false,
    val showReduceDialog: Boolean = false,
    val displayMultiplier: BigDecimal = BigDecimal.ONE
)

@HiltViewModel
class HoldingDetailViewModel @Inject constructor(
    private val holdingDao: HoldingDao,
    private val priceSnapshotDao: PriceSnapshotDao,
    private val transactionDao: TransactionDao,
    private val cashTransactionDao: CashTransactionDao,
    private val priceService: PriceService,
    private val valuationCalculator: ValuationCalculator,
    private val backfillEngine: BackfillEngine,
    private val accountManager: AccountManager,
    private val exchangeRateRepository: ExchangeRateRepository,
    private val displaySettings: com.financial.freedom.domain.settings.DisplaySettings
) : ViewModel() {

    private val _uiState = MutableStateFlow(DetailUiState())
    val uiState: StateFlow<DetailUiState> = _uiState.asStateFlow()

    private var mainHolding: Holding? = null
    private var allSymbolHoldings: List<Holding> = emptyList()

    init {
        viewModelScope.launch {
            displaySettings.multiplierFlow.collect { multiplier ->
                _uiState.value = _uiState.value.copy(displayMultiplier = multiplier)
            }
        }
        // multiplier 变化时重新格式化所有显示值（跳过初始发射）
        viewModelScope.launch {
            displaySettings.multiplierFlow.drop(1).collect { _ ->
                val h = mainHolding ?: return@collect
                val accountId = accountManager.currentAccountId.value ?: return@collect
                val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
                renderWithCache(h, today, accountId)
            }
        }
    }

    fun loadHolding(holdingId: Long) {
        viewModelScope.launch {
            val accountId = accountManager.currentAccountId.value ?: return@launch
            val h = holdingDao.getById(holdingId, accountId) ?: return@launch
            mainHolding = h

            // 查找所有同 symbol 的持仓（多笔买入后存在多条记录）
            allSymbolHoldings = holdingDao.getAllList(accountId).filter { it.symbol == h.symbol }

            val today = Clock.System.todayIn(TimeZone.currentSystemDefault())

            // 先用缓存数据立即渲染
            renderWithCache(h, today, accountId)

            // 后台拉取最新价格 + 历史价格（IO 线程，不阻塞 UI）
            withContext(Dispatchers.IO) {
                for (holding in allSymbolHoldings) {
                    try {
                        val result = priceService.fetchPrice(holding.type, holding.symbol, holding.market, today)
                        if (result != null) {
                            priceSnapshotDao.insert(
                                PriceSnapshot(
                                    holdingId = holding.id, date = result.date, unitPrice = result.price,
                                    currency = result.currency, accountId = accountId
                                )
                            )
                            Log.d("DetailVM", "Live price fetched for ${holding.symbol}: ${result.price}")
                        }

                        val thirtyDaysAgo = today.minus(30, DateTimeUnit.DAY)
                        val historyResults = priceService.fetchHistory(holding.type, holding.symbol, holding.market, thirtyDaysAgo, today)
                        if (historyResults.isNotEmpty()) {
                            val snapshots = historyResults.map { r ->
                                PriceSnapshot(
                                    holdingId = holding.id, date = r.date, unitPrice = r.price,
                                    currency = r.currency, accountId = accountId
                                )
                            }
                            priceSnapshotDao.insertAll(snapshots)
                            Log.d("DetailVM", "Backfilled ${snapshots.size} history snapshots for ${holding.symbol}")
                        }
                    } catch (e: Exception) {
                        Log.w("DetailVM", "Failed to fetch price data for ${holding.symbol}", e)
                    }
                }
            }

            val now = System.currentTimeMillis()
            val timeStr = "${now / 3600000 % 24}:${String.format("%02d", now / 60000 % 60)}"

            // 用最新数据重新渲染
            renderWithCache(h, today, accountId, timeStr)
        }
    }

    private suspend fun renderWithCache(
        h: Holding,
        today: kotlinx.datetime.LocalDate,
        accountId: Long,
        updateTime: String? = null
    ) {
        val group = allSymbolHoldings
        val rate = if (h.currency == "CNY") BigDecimal.ONE
            else exchangeRateRepository.getRate(h.currency, "CNY", today) ?: BigDecimal.ONE

        // 遍历组内所有持仓找最新价格快照
        var latestSnapshot: PriceSnapshot? = null
        for (holding in group) {
            val todaySnap = priceSnapshotDao.getByHoldingAndDate(holding.id, today, accountId)
            val snap = todaySnap ?: priceSnapshotDao.getLatest(holding.id, accountId)
            if (snap != null && (latestSnapshot == null || snap.date > latestSnapshot!!.date)) {
                latestSnapshot = snap
            }
        }
        val currentPrice = latestSnapshot?.unitPrice ?: h.costPrice

        // 遍历组内所有持仓找昨日/最近历史价格
        val yesterdayDate = today.minus(1, DateTimeUnit.DAY)
        var prevSnapshot: PriceSnapshot? = null
        for (holding in group) {
            val snap = priceSnapshotDao.getByHoldingAndDate(holding.id, yesterdayDate, accountId)
                ?: priceSnapshotDao.getLatestBefore(holding.id, today, accountId)
            if (snap != null && (prevSnapshot == null || snap.date > prevSnapshot!!.date)) {
                prevSnapshot = snap
            }
        }
        val prevPrice = prevSnapshot?.unitPrice ?: currentPrice

        val priceDiff = currentPrice.subtract(prevPrice)
        val priceChg = if (prevPrice > BigDecimal.ZERO) {
            priceDiff.multiply(rate).setScale(2, RoundingMode.HALF_UP)
        } else BigDecimal.ZERO
        val priceChgPct = if (prevPrice > BigDecimal.ZERO) {
            priceDiff.divide(prevPrice, 6, RoundingMode.HALF_UP)
                .multiply(BigDecimal(100)).setScale(2, RoundingMode.HALF_UP)
        } else BigDecimal.ZERO

        // 聚合所有同 symbol 持仓
        val totalQuantity = group.sumOf { it.quantity }
        val totalCost = group.sumOf { it.quantity.multiply(it.costPrice) }
        val totalCostCNY = totalCost.multiply(rate).setScale(2, RoundingMode.HALF_UP)

        val totalMarketValue = currentPrice.multiply(totalQuantity).multiply(rate).setScale(2, RoundingMode.HALF_UP)
        val totalPnL = totalMarketValue.subtract(totalCostCNY)
        val totalPnLPct = if (totalCostCNY > BigDecimal.ZERO)
            totalPnL.divide(totalCostCNY, 4, RoundingMode.HALF_UP).multiply(BigDecimal(100))
        else BigDecimal.ZERO

        val todayChg = priceDiff.multiply(totalQuantity).multiply(rate).setScale(2, RoundingMode.HALF_UP)
        val todayChgPct = priceChgPct
        val isUp = todayChg >= BigDecimal.ZERO

        // 加权平均成本
        val avgCost = if (totalQuantity > BigDecimal.ZERO)
            totalCost.divide(totalQuantity, 4, RoundingMode.HALF_UP)
        else h.costPrice

        // 合并所有持仓的交易记录
        val allTxns = mutableListOf<Transaction>()
        for (holding in group) {
            try {
                val txns = transactionDao.getByHolding(holding.id, accountId).first()
                allTxns.addAll(txns)
            } catch (_: Exception) { }
        }
        allTxns.sortByDescending { it.date }

        // 合并所有持仓的价格历史，按日期去重取最新
        val allSnapshots = mutableListOf<PriceSnapshot>()
        for (holding in group) {
            try {
                val snaps = priceSnapshotDao.getByHoldingAndDateRange(
                    holding.id, today.minus(30, DateTimeUnit.DAY), today, accountId
                ).first()
                allSnapshots.addAll(snaps)
            } catch (_: Exception) { }
        }
        val history = allSnapshots.groupBy { it.date }.map { (_, snaps) ->
            snaps.maxByOrNull { it.date }!!
        }.sortedBy { it.date }

        _uiState.value = DetailUiState(
            name = h.name,
            symbol = h.symbol,
            type = h.type,
            status = h.status,
            currentPrice = formatMoney(currentPrice.multiply(rate)),
            priceChange = formatMoney(priceChg.abs()),
            priceChangePct = "${priceChgPct.abs()}%",
            todayChange = formatMoney(todayChg.abs()),
            todayChangePct = "${todayChgPct.abs()}%",
            isUp = isUp,
            totalPnL = formatMoney(totalPnL.abs()),
            totalPnLPct = "${totalPnLPct.abs()}%",
            quantity = "${totalQuantity}",
            costPrice = formatMoney(avgCost.multiply(rate)),
            totalCost = formatMoney(totalCostCNY),
            marketValue = formatMoney(totalMarketValue),
            transactions = allTxns,
            priceHistory = history,
            lastUpdateTime = updateTime
        )
    }

    fun showAddDialog() { _uiState.value = _uiState.value.copy(showAddDialog = true) }
    fun hideAddDialog() { _uiState.value = _uiState.value.copy(showAddDialog = false) }
    fun showReduceDialog() { _uiState.value = _uiState.value.copy(showReduceDialog = true) }
    fun hideReduceDialog() { _uiState.value = _uiState.value.copy(showReduceDialog = false) }

    fun addPosition(
        tradeQty: BigDecimal,
        tradePrice: BigDecimal,
        tradeDate: LocalDate,
        deductFromCash: Boolean,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            try {
                val h = mainHolding ?: return@launch
                val oldQty = h.quantity
                val oldCost = h.costPrice
                val newQty = oldQty.add(tradeQty)
                val newAvgCost = oldQty.multiply(oldCost).add(tradeQty.multiply(tradePrice))
                    .divide(newQty, 4, RoundingMode.HALF_UP)

                val updated = h.copy(quantity = newQty, costPrice = newAvgCost)
                holdingDao.update(updated)
                mainHolding = updated
                transactionDao.insert(
                    Transaction(
                        holdingId = h.id, accountId = h.accountId,
                        type = "BUY", date = tradeDate, price = tradePrice,
                        quantity = tradeQty
                    )
                )
                if (deductFromCash) {
                    val totalCost = tradeQty.multiply(tradePrice).setScale(2, RoundingMode.HALF_UP)
                    cashTransactionDao.insert(
                        CashTransaction(
                            accountId = h.accountId, date = tradeDate,
                            amount = -totalCost, type = "ASSET_PURCHASE",
                            note = "加仓${h.name}: ${tradeQty}×${formatMoney(tradePrice)}"
                        )
                    )
                }
                hold_refresh()
                onSuccess()
                viewModelScope.launch(Dispatchers.IO) {
                    backfillEngine.markDirtyAndBackfill(tradeDate, h.accountId)
                }
            } catch (e: Exception) {
                onError(e.message ?: "未知错误")
            }
        }
    }

    fun reducePosition(
        tradeQty: BigDecimal,
        tradePrice: BigDecimal,
        tradeDate: LocalDate,
        addToCash: Boolean,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            try {
                val h = mainHolding ?: return@launch
                val oldQty = h.quantity
                val newQty = oldQty.subtract(tradeQty)
                val isClosed = newQty <= BigDecimal.ZERO

                val updatedHolding = if (isClosed) {
                    h.copy(quantity = BigDecimal.ZERO, status = "closed")
                } else {
                    h.copy(quantity = newQty)
                }
                holdingDao.update(updatedHolding)
                if (isClosed) {
                    mainHolding = updatedHolding
                }

                val realizedPnL = tradePrice.subtract(h.costPrice).multiply(tradeQty).setScale(2, RoundingMode.HALF_UP)
                transactionDao.insert(
                    Transaction(
                        holdingId = h.id, accountId = h.accountId,
                        type = "SELL", date = tradeDate, price = tradePrice,
                        quantity = tradeQty
                    )
                )
                if (addToCash) {
                    val totalProceeds = tradeQty.multiply(tradePrice).setScale(2, RoundingMode.HALF_UP)
                    cashTransactionDao.insert(
                        CashTransaction(
                            accountId = h.accountId, date = tradeDate,
                            amount = totalProceeds, type = "ASSET_SALE",
                            note = "减仓${h.name}: ${tradeQty}×${formatMoney(tradePrice)}, 盈亏${formatMoney(realizedPnL)}"
                        )
                    )
                }
                hold_refresh()
                onSuccess()
                viewModelScope.launch(Dispatchers.IO) {
                    backfillEngine.markDirtyAndBackfill(tradeDate, h.accountId)
                }
            } catch (e: Exception) {
                onError(e.message ?: "未知错误")
            }
        }
    }

    private suspend fun hold_refresh() {
        val h = mainHolding ?: return
        val accountId = h.accountId
        // 重新加载所有同 symbol 持仓
        allSymbolHoldings = holdingDao.getAllList(accountId).filter { it.symbol == h.symbol }
        mainHolding = allSymbolHoldings.firstOrNull { it.id == h.id } ?: allSymbolHoldings.firstOrNull() ?: return
        val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
        renderWithCache(mainHolding!!, today, accountId)
    }

    fun deleteHolding(onDeleted: () -> Unit) {
        viewModelScope.launch {
            val h = mainHolding ?: return@launch
            val costDate = h.costDate
            val accountId = h.accountId
            holdingDao.delete(h)
            onDeleted()
            viewModelScope.launch(Dispatchers.IO) {
                backfillEngine.markDirtyAndBackfill(costDate, accountId)
            }
        }
    }

    fun selectRange(index: Int) {
        viewModelScope.launch {
            val accountId = accountManager.currentAccountId.value ?: return@launch
            _uiState.value = _uiState.value.copy(selectedRange = index)
            val h = mainHolding ?: return@launch
            val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
            val days = when (index) {
                0 -> 30; 1 -> 90; 2 -> 365; else -> 30
            }
            val startDate = today.minus(days, DateTimeUnit.DAY)
            // 聚合所有同 symbol 持仓的价格历史
            val allSnapshots = mutableListOf<PriceSnapshot>()
            for (holding in allSymbolHoldings) {
                try {
                    val snaps = priceSnapshotDao.getByHoldingAndDateRange(
                        holding.id, startDate, today, accountId
                    ).first()
                    allSnapshots.addAll(snaps)
                } catch (_: Exception) { }
            }
            val history = allSnapshots.groupBy { it.date }.map { (_, snaps) ->
                snaps.maxByOrNull { it.date }!!
            }.sortedBy { it.date }
            _uiState.value = _uiState.value.copy(priceHistory = history)
        }
    }

    private fun formatMoney(value: BigDecimal): String {
        val multiplier = _uiState.value.displayMultiplier
        return com.financial.freedom.ui.common.FormatUtils.formatMoney(value, multiplier)
    }
}
