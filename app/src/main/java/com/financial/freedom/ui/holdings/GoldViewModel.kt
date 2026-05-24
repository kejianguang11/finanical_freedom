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
import com.financial.freedom.domain.settings.DisplaySettings
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.drop
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

enum class ChartTimeRange(val label: String, val days: Int) {
    WEEK("周", 7),
    MONTH("月", 30),
    YEAR("年", 365)
}

data class GoldUiState(
    val holding: Holding? = null,
    val transactions: List<Transaction> = emptyList(),
    val currentPrice: BigDecimal = BigDecimal.ZERO,
    val previousPrice: BigDecimal = BigDecimal.ZERO,
    val totalGrams: String = "0",
    val avgCostPrice: String = "0",
    val totalCost: String = "0",
    val marketValue: String = "0",
    val totalPnL: String = "0",
    val totalPnLPct: String = "0%",
    val todayChange: String = "+0.00",
    val todayChangePct: String = "+0.00%",
    val isUp: Boolean = false,
    val priceHistory: List<PriceSnapshot> = emptyList(),
    val showAddDialog: Boolean = false,
    val showReduceDialog: Boolean = false,
    val showDeleteAllDialog: Boolean = false,
    val editingTransaction: Transaction? = null,
    val displayMultiplier: BigDecimal = BigDecimal.ONE,
    val chartTimeRange: ChartTimeRange = ChartTimeRange.MONTH
)

@HiltViewModel
class GoldViewModel @Inject constructor(
    private val holdingDao: HoldingDao,
    private val transactionDao: TransactionDao,
    private val priceSnapshotDao: PriceSnapshotDao,
    private val cashTransactionDao: CashTransactionDao,
    private val priceService: PriceService,
    private val exchangeRateRepository: ExchangeRateRepository,
    private val valuationCalculator: ValuationCalculator,
    private val accountManager: AccountManager,
    private val displaySettings: DisplaySettings,
    private val backfillEngine: BackfillEngine
) : ViewModel() {

    private val _uiState = MutableStateFlow(GoldUiState())
    val uiState: StateFlow<GoldUiState> = _uiState.asStateFlow()

    companion object {
        private const val TAG = "GoldVM"
    }

    init {
        viewModelScope.launch {
            displaySettings.multiplierFlow.collect { multiplier ->
                _uiState.value = _uiState.value.copy(displayMultiplier = multiplier)
            }
        }
        viewModelScope.launch {
            val accountId = accountManager.currentAccountId.value ?: return@launch

            // 监听黄金 Holding 变化
            combine(
                holdingDao.getByType("GOLD", accountId),
                displaySettings.multiplierFlow
            ) { holdings, _ ->
                holdings.firstOrNull()
            }.collect { holding ->
                if (holding != null) {
                    fetchTransactionsAndRender(holding, accountId)
                } else {
                    _uiState.value = GoldUiState()
                }
            }
        }
    }

    private suspend fun fetchTransactionsAndRender(holding: Holding, accountId: Long) {
        val today = Clock.System.todayIn(TimeZone.currentSystemDefault())

        // 先本地渲染
        val txs = transactionDao.getByHoldingList(holding.id, accountId)
        render(holding, txs, accountId, today)

        // 后台拉取最新价格
        withContext(Dispatchers.IO) {
            try {
                val result = priceService.fetchPrice("GOLD", "XAU", "", today)
                if (result != null) {
                    priceSnapshotDao.insert(
                        PriceSnapshot(
                            holdingId = holding.id, date = result.date, unitPrice = result.price,
                            currency = result.currency, accountId = accountId
                        )
                    )
                }
                // 补齐历史价格（按当前时间范围），含回填确保日收益数据包含黄金估值
                val rangeDays = _uiState.value.chartTimeRange.days
                val rangeStart = today.minus(rangeDays, DateTimeUnit.DAY)
                val history = priceService.fetchHistory("GOLD", "XAU", "", rangeStart, today)
                if (history.isNotEmpty()) {
                    priceSnapshotDao.insertAll(
                        history.map { r ->
                            PriceSnapshot(
                                holdingId = holding.id, date = r.date, unitPrice = r.price,
                                currency = r.currency, accountId = accountId
                            )
                        }
                    )
                    backfillEngine.markDirtyAndBackfill(rangeStart, accountId)
                }
            } catch (e: Exception) {
                Log.w(TAG, "Failed to fetch gold price", e)
            }
        }

        // 用最新数据重新渲染
        val updatedTxs = transactionDao.getByHoldingList(holding.id, accountId)
        render(holding, updatedTxs, accountId, today)
    }

    private suspend fun render(
        holding: Holding,
        txs: List<Transaction>,
        accountId: Long,
        today: LocalDate
    ) {
        val yesterday = today.minus(1, DateTimeUnit.DAY)
        val todaySnapshot = priceSnapshotDao.getByHoldingAndDate(holding.id, today, accountId)
        val yesterdaySnapshot = priceSnapshotDao.getByHoldingAndDate(holding.id, yesterday, accountId)

        val currentPrice = todaySnapshot?.unitPrice
            ?: priceSnapshotDao.getLatest(holding.id, accountId)?.unitPrice
            ?: holding.costPrice

        val prevPrice = yesterdaySnapshot?.unitPrice
            ?: priceSnapshotDao.getLatestBefore(holding.id, today, accountId)?.unitPrice
            ?: currentPrice

        val rate = BigDecimal.ONE // Gold is always CNY

        val marketValue = currentPrice.multiply(holding.quantity).setScale(2, RoundingMode.HALF_UP)
        val totalCost = holding.quantity.multiply(holding.costPrice).setScale(2, RoundingMode.HALF_UP)
        val pnl = marketValue.subtract(totalCost).setScale(2, RoundingMode.HALF_UP)
        val pnlPct = if (totalCost > BigDecimal.ZERO) {
            pnl.divide(totalCost, 4, RoundingMode.HALF_UP).multiply(BigDecimal(100)).setScale(2, RoundingMode.HALF_UP)
        } else BigDecimal.ZERO

        val todayChg = currentPrice.subtract(prevPrice).multiply(holding.quantity).setScale(2, RoundingMode.HALF_UP)
        val todayChgPct = if (prevPrice > BigDecimal.ZERO) {
            currentPrice.subtract(prevPrice).divide(prevPrice, 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal(100)).setScale(2, RoundingMode.HALF_UP)
        } else BigDecimal.ZERO

        // 价格历史（按当前时间范围）
        val rangeDays = _uiState.value.chartTimeRange.days
        val rangeStart = today.minus(rangeDays, DateTimeUnit.DAY)
        val priceHistory = priceSnapshotDao.getByDateRange(holding.id, rangeStart, today, accountId)
            .sortedBy { it.date }

        val m = _uiState.value.displayMultiplier
        _uiState.value = _uiState.value.copy(
            holding = holding,
            transactions = txs.sortedByDescending { it.date },
            currentPrice = currentPrice,
            previousPrice = prevPrice,
            totalGrams = formatDecimal(holding.quantity, m),
            avgCostPrice = formatMoney(holding.costPrice, m),
            totalCost = formatMoney(totalCost, m),
            marketValue = formatMoney(marketValue, m),
            totalPnL = formatSigned(pnl, m),
            totalPnLPct = "${pnlPct}%",
            todayChange = formatSigned(todayChg, m),
            todayChangePct = "${if (todayChgPct >= BigDecimal.ZERO) "+" else ""}${todayChgPct}%",
            isUp = pnl >= BigDecimal.ZERO,
            priceHistory = priceHistory
        )
    }

    // --- 对话框控制 ---
    fun setChartTimeRange(range: ChartTimeRange) {
        if (_uiState.value.chartTimeRange == range) return
        _uiState.value = _uiState.value.copy(chartTimeRange = range)
        val accountId = accountManager.currentAccountId.value ?: return
        val holding = _uiState.value.holding ?: return
        viewModelScope.launch {
            fetchTransactionsAndRender(holding, accountId)
        }
    }

    fun showAddDialog() { _uiState.value = _uiState.value.copy(showAddDialog = true) }
    fun hideAddDialog() { _uiState.value = _uiState.value.copy(showAddDialog = false, editingTransaction = null) }
    fun showReduceDialog() { _uiState.value = _uiState.value.copy(showReduceDialog = true) }
    fun hideReduceDialog() { _uiState.value = _uiState.value.copy(showReduceDialog = false) }
    fun showDeleteAllDialog() { _uiState.value = _uiState.value.copy(showDeleteAllDialog = true) }
    fun hideDeleteAllDialog() { _uiState.value = _uiState.value.copy(showDeleteAllDialog = false) }

    fun startEdit(tx: Transaction) {
        _uiState.value = _uiState.value.copy(showAddDialog = true, editingTransaction = tx)
    }

    fun addPurchase(date: LocalDate, unitPrice: BigDecimal, grams: BigDecimal, deductFromCash: Boolean) {
        val accountId = accountManager.currentAccountId.value ?: return
        viewModelScope.launch {
            var holding = holdingDao.getByTypeList("GOLD", accountId).firstOrNull()

            if (holding == null) {
                // 首次买入：创建 Holding
                holding = Holding(
                    type = "GOLD", symbol = "XAU", name = "黄金", market = "",
                    currency = "CNY", quantity = grams, costPrice = unitPrice,
                    costDate = date, accountId = accountId
                )
                holdingDao.insert(holding)
            } else {
                // 加仓：重算总量和加权均价
                val newTotalGrams = holding.quantity.add(grams)
                val newTotalCost = holding.quantity.multiply(holding.costPrice)
                    .add(grams.multiply(unitPrice))
                val newAvgCost = newTotalCost.divide(newTotalGrams, 6, RoundingMode.HALF_UP)
                holding = holding.copy(
                    quantity = newTotalGrams,
                    costPrice = newAvgCost,
                    costDate = date
                )
                holdingDao.update(holding)
            }

            // 创建买入交易记录
            transactionDao.insert(
                Transaction(
                    holdingId = holding.id,
                    accountId = accountId,
                    type = "BUY",
                    date = date,
                    price = unitPrice,
                    quantity = grams
                )
            )

            // 从现金扣除
            if (deductFromCash) {
                val totalCost = grams.multiply(unitPrice)
                cashTransactionDao.insert(
                    CashTransaction(
                        accountId = accountId,
                        date = date,
                        amount = totalCost.negate(),
                        type = "LEND",
                        note = "买入黄金 ${grams}克 × ¥${unitPrice}",
                        relatedId = holding.id
                    )
                )
            }
            // 无论是否从现金扣除，都要回填历史收益，确保日收益数据包含黄金
            withContext(Dispatchers.IO) {
                backfillEngine.markDirtyAndBackfill(date, accountId)
            }

            _uiState.value = _uiState.value.copy(showAddDialog = false, editingTransaction = null)
        }
    }

    fun updatePurchase(tx: Transaction, date: LocalDate, unitPrice: BigDecimal, grams: BigDecimal) {
        val accountId = accountManager.currentAccountId.value ?: return
        viewModelScope.launch {
            val holding = holdingDao.getByTypeList("GOLD", accountId).firstOrNull() ?: return@launch

            // 更新交易
            transactionDao.update(tx.copy(date = date, price = unitPrice, quantity = grams))

            // 重算 Holding
            val allTxs = transactionDao.getByHoldingList(holding.id, accountId)
            if (allTxs.isEmpty()) return@launch

            val newTotalGrams = allTxs.fold(BigDecimal.ZERO) { acc, t -> acc.add(t.quantity) }
            val newTotalCost = allTxs.fold(BigDecimal.ZERO) { acc, t ->
                acc.add(t.quantity.multiply(t.price))
            }
            val newAvgCost = newTotalCost.divide(newTotalGrams, 6, RoundingMode.HALF_UP)
            holdingDao.update(holding.copy(
                quantity = newTotalGrams,
                costPrice = newAvgCost,
                costDate = allTxs.maxBy { it.date }.date
            ))

            withContext(Dispatchers.IO) {
                backfillEngine.markDirtyAndBackfill(date, accountId)
            }

            _uiState.value = _uiState.value.copy(showAddDialog = false, editingTransaction = null)
        }
    }

    fun deletePurchase(tx: Transaction) {
        val accountId = accountManager.currentAccountId.value ?: return
        viewModelScope.launch {
            val holding = holdingDao.getByTypeList("GOLD", accountId).firstOrNull() ?: return@launch
            transactionDao.delete(tx)

            val remainingTxs = transactionDao.getByHoldingList(holding.id, accountId)
            if (remainingTxs.isEmpty()) {
                // 没有买入记录了，删除 Holding
                priceSnapshotDao.deleteByHoldingId(holding.id, accountId)
                holdingDao.delete(holding)
            } else {
                // 重算
                val newTotalGrams = remainingTxs.fold(BigDecimal.ZERO) { acc, t -> acc.add(t.quantity) }
                val newTotalCost = remainingTxs.fold(BigDecimal.ZERO) { acc, t ->
                    acc.add(t.quantity.multiply(t.price))
                }
                val newAvgCost = newTotalCost.divide(newTotalGrams, 6, RoundingMode.HALF_UP)
                holdingDao.update(holding.copy(
                    quantity = newTotalGrams,
                    costPrice = newAvgCost,
                    costDate = remainingTxs.maxBy { it.date }.date
                ))
            }

            withContext(Dispatchers.IO) {
                backfillEngine.markDirtyAndBackfill(tx.date, accountId)
            }

            _uiState.value = _uiState.value.copy(showAddDialog = false, editingTransaction = null)
        }
    }

    fun reducePosition(date: LocalDate, unitPrice: BigDecimal, grams: BigDecimal, addToCash: Boolean) {
        val accountId = accountManager.currentAccountId.value ?: return
        viewModelScope.launch {
            val holding = holdingDao.getByTypeList("GOLD", accountId).firstOrNull() ?: return@launch
            val newQty = holding.quantity.subtract(grams)
            val proceeds = grams.multiply(unitPrice)

            if (newQty <= BigDecimal.ZERO) {
                // 全部卖出 → 删除 Holding
                priceSnapshotDao.deleteByHoldingId(holding.id, accountId)
                transactionDao.deleteByHoldingId(holding.id, accountId)
                holdingDao.delete(holding)
            } else {
                // 部分减仓 → 更新 Holding（成本价不变）
                holdingDao.update(holding.copy(quantity = newQty, costDate = date))
            }

            // 创建卖出交易记录
            transactionDao.insert(
                Transaction(
                    holdingId = holding.id,
                    accountId = accountId,
                    type = "SELL",
                    date = date,
                    price = unitPrice,
                    quantity = grams
                )
            )

            // 加回现金
            if (addToCash) {
                cashTransactionDao.insert(
                    CashTransaction(
                        accountId = accountId,
                        date = date,
                        amount = proceeds,
                        type = "RECYCLE",
                        note = "卖出黄金 ${grams}克 × ¥${unitPrice}",
                        relatedId = holding.id
                    )
                )
            }
            withContext(Dispatchers.IO) {
                backfillEngine.markDirtyAndBackfill(date, accountId)
            }

            _uiState.value = _uiState.value.copy(showReduceDialog = false)
        }
    }

    fun deleteAllHoldings(onDeleted: () -> Unit) {
        val accountId = accountManager.currentAccountId.value ?: return
        viewModelScope.launch {
            val holding = holdingDao.getByTypeList("GOLD", accountId).firstOrNull() ?: return@launch
            val costDate = holding.costDate
            priceSnapshotDao.deleteByHoldingId(holding.id, accountId)
            transactionDao.deleteByHoldingId(holding.id, accountId)
            holdingDao.delete(holding)
            withContext(Dispatchers.IO) {
                backfillEngine.markDirtyAndBackfill(costDate, accountId)
            }
            _uiState.value = _uiState.value.copy(showDeleteAllDialog = false)
            onDeleted()
        }
    }

    private fun formatMoney(value: BigDecimal, multiplier: BigDecimal): String =
        com.financial.freedom.ui.common.FormatUtils.formatMoney(value, multiplier)

    private fun formatSigned(value: BigDecimal, multiplier: BigDecimal): String =
        com.financial.freedom.ui.common.FormatUtils.formatSignedChange(value, multiplier)

    private fun formatDecimal(value: BigDecimal, multiplier: BigDecimal): String {
        val adjusted = value.multiply(multiplier)
        return com.financial.freedom.ui.common.FormatUtils.formatWithDecimals(adjusted)
    }
}
