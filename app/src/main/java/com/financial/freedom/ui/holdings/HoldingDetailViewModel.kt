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

    private var holding: Holding? = null

    init {
        viewModelScope.launch {
            displaySettings.multiplierFlow.collect { multiplier ->
                _uiState.value = _uiState.value.copy(displayMultiplier = multiplier)
            }
        }
        // multiplier 变化时重新格式化所有显示值（跳过初始发射）
        viewModelScope.launch {
            displaySettings.multiplierFlow.drop(1).collect { _ ->
                val h = holding ?: return@collect
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
            holding = h

            val today = Clock.System.todayIn(TimeZone.currentSystemDefault())

            // 先用缓存数据立即渲染
            renderWithCache(h, today, accountId)

            // 后台拉取最新价格 + 历史价格
            try {
                val result = priceService.fetchPrice(h.type, h.symbol, h.market, today)
                if (result != null) {
                    priceSnapshotDao.insert(
                        PriceSnapshot(
                            holdingId = h.id, date = result.date, unitPrice = result.price,
                            currency = result.currency, accountId = accountId
                        )
                    )
                    Log.d("DetailVM", "Live price fetched for ${h.symbol}: ${result.price}")
                }

                // 补齐历史价格数据（最近30天），确保走势图有数据
                val thirtyDaysAgo = today.minus(30, DateTimeUnit.DAY)
                val historyResults = priceService.fetchHistory(h.type, h.symbol, h.market, thirtyDaysAgo, today)
                if (historyResults.isNotEmpty()) {
                    val snapshots = historyResults.map { r ->
                        PriceSnapshot(
                            holdingId = h.id, date = r.date, unitPrice = r.price,
                            currency = r.currency, accountId = accountId
                        )
                    }
                    priceSnapshotDao.insertAll(snapshots)
                    Log.d("DetailVM", "Backfilled ${snapshots.size} history snapshots for ${h.symbol}")
                }
            } catch (e: Exception) {
                Log.w("DetailVM", "Failed to fetch price data for ${h.symbol}", e)
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
        val todaySnapshot = priceSnapshotDao.getByHoldingAndDate(h.id, today, accountId)
        val yesterdayDate = today.minus(1, DateTimeUnit.DAY)
        val yesterdaySnapshot = priceSnapshotDao.getByHoldingAndDate(h.id, yesterdayDate, accountId)

        val currentPrice = todaySnapshot?.unitPrice
            ?: priceSnapshotDao.getLatest(h.id, accountId)?.unitPrice
            ?: h.costPrice

        val prevPrice = yesterdaySnapshot?.unitPrice
            ?: priceSnapshotDao.getLatestBefore(h.id, today, accountId)?.unitPrice
            ?: currentPrice

        val rate = if (h.currency == "CNY") BigDecimal.ONE
            else exchangeRateRepository.getRate(h.currency, "CNY", today) ?: BigDecimal.ONE

        val priceDiff = currentPrice.subtract(prevPrice)
        val priceChg = if (prevPrice > BigDecimal.ZERO) {
            priceDiff.multiply(rate).setScale(2, RoundingMode.HALF_UP)
        } else BigDecimal.ZERO
        val priceChgPct = if (prevPrice > BigDecimal.ZERO) {
            priceDiff.divide(prevPrice, 6, RoundingMode.HALF_UP)
                .multiply(BigDecimal(100)).setScale(2, RoundingMode.HALF_UP)
        } else BigDecimal.ZERO

        val todayChg = priceDiff.multiply(h.quantity).multiply(rate).setScale(2, RoundingMode.HALF_UP)
        val todayChgPct = priceChgPct

        val isUp = todayChg >= BigDecimal.ZERO

        val pnL = valuationCalculator.calcHoldingPnL(h, currentPrice, rate)
        val pnlPct = valuationCalculator.calcHoldingPnLPct(h, currentPrice, rate)
        val marketValue = valuationCalculator.calcHoldingValueCNY(h, currentPrice, rate)
        val costTotal = h.quantity.multiply(h.costPrice).multiply(rate)

        // 使用 first() 而非 collect() — Room Flow 永不 complete，collect 会永久阻塞
        val txns = transactionDao.getByHolding(h.id, accountId).first()

        val history = priceSnapshotDao.getByHoldingAndDateRange(
            h.id, today.minus(30, DateTimeUnit.DAY), today, accountId
        ).first()

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
            totalPnL = formatMoney(pnL.abs()),
            totalPnLPct = "${pnlPct.abs()}%",
            quantity = "${h.quantity}",
            costPrice = formatMoney(h.costPrice.multiply(rate)),
            totalCost = formatMoney(costTotal),
            marketValue = formatMoney(marketValue),
            transactions = txns,
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
                val h = holding ?: return@launch
                val oldQty = h.quantity
                val oldCost = h.costPrice
                val newQty = oldQty.add(tradeQty)
                val newAvgCost = oldQty.multiply(oldCost).add(tradeQty.multiply(tradePrice))
                    .divide(newQty, 4, RoundingMode.HALF_UP)

                val updated = h.copy(quantity = newQty, costPrice = newAvgCost)
                holdingDao.update(updated)
                holding = updated
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
                val h = holding ?: return@launch
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
                    holding = updatedHolding
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
        val h = holding ?: return
        val accountId = h.accountId
        val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
        renderWithCache(h, today, accountId)
    }

    fun deleteHolding(onDeleted: () -> Unit) {
        viewModelScope.launch {
            val h = holding ?: return@launch
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
            val h = holding ?: return@launch
            val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
            val days = when (index) {
                0 -> 30; 1 -> 90; 2 -> 365; else -> 30
            }
            val history = priceSnapshotDao.getByHoldingAndDateRange(
                h.id, today.minus(days, DateTimeUnit.DAY), today, accountId
            ).first()
            _uiState.value = _uiState.value.copy(priceHistory = history)
        }
    }

    private fun formatMoney(value: BigDecimal): String {
        val multiplier = _uiState.value.displayMultiplier
        return com.financial.freedom.ui.common.FormatUtils.formatMoney(value, multiplier)
    }
}
