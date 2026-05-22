package com.financial.freedom.ui.holdings

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.financial.freedom.data.local.dao.HoldingDao
import com.financial.freedom.data.local.dao.PriceSnapshotDao
import com.financial.freedom.data.local.dao.TransactionDao
import com.financial.freedom.data.local.entity.Holding
import com.financial.freedom.data.local.entity.PriceSnapshot
import com.financial.freedom.data.local.entity.Transaction
import com.financial.freedom.data.remote.PriceService
import com.financial.freedom.domain.account.AccountManager
import com.financial.freedom.domain.calculator.BackfillEngine
import com.financial.freedom.domain.calculator.ValuationCalculator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.DateTimeUnit
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
    val currentPrice: String = "--",
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
    val lastUpdateTime: String? = null
)

@HiltViewModel
class HoldingDetailViewModel @Inject constructor(
    private val holdingDao: HoldingDao,
    private val priceSnapshotDao: PriceSnapshotDao,
    private val transactionDao: TransactionDao,
    private val priceService: PriceService,
    private val valuationCalculator: ValuationCalculator,
    private val backfillEngine: BackfillEngine,
    private val accountManager: AccountManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(DetailUiState())
    val uiState: StateFlow<DetailUiState> = _uiState.asStateFlow()

    private var holding: Holding? = null

    fun loadHolding(holdingId: Long) {
        viewModelScope.launch {
            val accountId = accountManager.currentAccountId.value ?: return@launch
            val h = holdingDao.getById(holdingId, accountId) ?: return@launch
            holding = h

            val today = Clock.System.todayIn(TimeZone.currentSystemDefault())

            // 先用缓存数据立即渲染
            renderWithCache(h, today, accountId)

            // 后台拉取最新价格
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
            } catch (e: Exception) {
                Log.w("DetailVM", "Failed to fetch live price for ${h.symbol}", e)
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
            ?: priceSnapshotDao.getPrevious(h.id, accountId)?.unitPrice
            ?: currentPrice

        val todayChg = if (prevPrice > BigDecimal.ZERO) {
            currentPrice.subtract(prevPrice).multiply(h.quantity).setScale(2, RoundingMode.HALF_UP)
        } else BigDecimal.ZERO
        val todayChgPct = if (prevPrice > BigDecimal.ZERO) {
            currentPrice.subtract(prevPrice).divide(prevPrice, 6, RoundingMode.HALF_UP)
                .multiply(BigDecimal(100)).setScale(2, RoundingMode.HALF_UP)
        } else BigDecimal.ZERO

        val isUp = todayChg >= BigDecimal.ZERO

        val rate = BigDecimal.ONE // holdings in test data are CNY
        val pnL = valuationCalculator.calcHoldingPnL(h, currentPrice, rate)
        val pnlPct = valuationCalculator.calcHoldingPnLPct(h, currentPrice, rate)
        val marketValue = valuationCalculator.calcHoldingValueCNY(h, currentPrice, rate)
        val costTotal = h.quantity.multiply(h.costPrice)

        // 使用 first() 而非 collect() — Room Flow 永不 complete，collect 会永久阻塞
        val txns = transactionDao.getByHolding(h.id, accountId).first()

        val history = priceSnapshotDao.getByHoldingAndDateRange(
            h.id, today.minus(30, DateTimeUnit.DAY), today, accountId
        ).first()

        _uiState.value = DetailUiState(
            name = h.name,
            symbol = h.symbol,
            type = h.type,
            currentPrice = formatMoney(currentPrice),
            todayChange = if (todayChg >= BigDecimal.ZERO) "+${formatMoney(todayChg)}" else formatMoney(todayChg),
            todayChangePct = if (todayChgPct >= BigDecimal.ZERO) "+${todayChgPct}%" else "${todayChgPct}%",
            isUp = isUp,
            totalPnL = if (pnL >= BigDecimal.ZERO) "+${formatMoney(pnL)}" else formatMoney(pnL),
            totalPnLPct = if (pnlPct >= BigDecimal.ZERO) "+${pnlPct}%" else "${pnlPct}%",
            quantity = "${h.quantity}",
            costPrice = formatMoney(h.costPrice),
            totalCost = formatMoney(costTotal),
            marketValue = formatMoney(marketValue),
            transactions = txns,
            priceHistory = history,
            lastUpdateTime = updateTime
        )
    }

    fun deleteHolding(onDeleted: () -> Unit) {
        viewModelScope.launch {
            val h = holding ?: return@launch
            val costDate = h.costDate
            val accountId = h.accountId
            holdingDao.delete(h)
            // 从持仓成本日期重新回填
            backfillEngine.markDirtyAndBackfill(costDate, accountId)
            onDeleted()
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
        val abs = value.abs().setScale(2, RoundingMode.HALF_UP)
        val intPart = abs.toBigInteger().toString()
        val formatted = intPart.reversed().chunked(3).joinToString(",").reversed()
        val decimal = abs.subtract(BigDecimal(abs.toBigInteger())).toPlainString()
            .removePrefix("0").take(4)
        val full = if (decimal.isNotEmpty() && decimal != ".00") "$formatted$decimal" else formatted
        return if (value < BigDecimal.ZERO) "-$full" else full
    }
}
