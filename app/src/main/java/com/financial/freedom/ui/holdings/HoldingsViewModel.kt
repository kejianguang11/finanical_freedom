package com.financial.freedom.ui.holdings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.financial.freedom.data.local.dao.PriceSnapshotDao
import com.financial.freedom.data.local.entity.Deposit
import com.financial.freedom.data.local.entity.Holding
import com.financial.freedom.data.repository.DepositRepository
import com.financial.freedom.data.repository.ExchangeRateRepository
import com.financial.freedom.data.repository.HoldingRepository
import com.financial.freedom.domain.account.AccountManager
import com.financial.freedom.domain.calculator.InterestCalculator
import com.financial.freedom.domain.calculator.ValuationCalculator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.todayIn
import kotlinx.datetime.until
import java.math.BigDecimal
import java.math.RoundingMode
import javax.inject.Inject

data class DepositDisplay(
    val id: Long,
    val name: String,
    val bank: String,
    val principal: String,
    val rate: String,
    val accruedInterest: String,
    val holdingDays: Int,
    val totalDays: Int,
    val startDate: String,
    val maturityDate: String,
    val progress: Float,
    val currentValue: String,
    val currency: String,
    val todayInterest: String
)

data class HoldingDisplay(
    val id: Long,
    val type: String,
    val symbol: String,
    val name: String,
    val quantity: String,
    val costPrice: String,
    val currentPrice: String,
    val totalPnL: String,
    val totalPnLPct: String,
    val todayChange: String,
    val isUp: Boolean
)

data class HoldingsUiState(
    val deposits: List<DepositDisplay> = emptyList(),
    val stocks: List<HoldingDisplay> = emptyList(),
    val funds: List<HoldingDisplay> = emptyList(),
    val golds: List<HoldingDisplay> = emptyList()
)

@HiltViewModel
class HoldingsViewModel @Inject constructor(
    private val depositRepository: DepositRepository,
    private val holdingRepository: HoldingRepository,
    private val exchangeRateRepository: ExchangeRateRepository,
    private val priceSnapshotDao: PriceSnapshotDao,
    private val valuationCalculator: ValuationCalculator,
    private val interestCalculator: InterestCalculator,
    private val accountManager: AccountManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(HoldingsUiState())
    val uiState: StateFlow<HoldingsUiState> = _uiState.asStateFlow()

    init {
        val accountId = accountManager.currentAccountId.value
        if (accountId != null) {
            val today = Clock.System.todayIn(TimeZone.currentSystemDefault())

            viewModelScope.launch {
                depositRepository.getAll(accountId).collect { deposits ->
                    _uiState.value = _uiState.value.copy(
                        deposits = deposits.map { d -> toDepositDisplay(d, today) }
                    )
                }
            }
            viewModelScope.launch {
                holdingRepository.getByType("STOCK", accountId).collect { stocks ->
                    updateHoldingDisplays("STOCK", stocks, accountId)
                }
            }
            viewModelScope.launch {
                holdingRepository.getByType("FUND", accountId).collect { funds ->
                    updateHoldingDisplays("FUND", funds, accountId)
                }
            }
            viewModelScope.launch {
                holdingRepository.getByType("GOLD", accountId).collect { golds ->
                    updateHoldingDisplays("GOLD", golds, accountId)
                }
            }
        }
    }

    suspend fun deleteDeposit(deposit: Deposit) = depositRepository.delete(deposit)

    suspend fun deleteHolding(holding: Holding) = holdingRepository.delete(holding)

    private suspend fun updateHoldingDisplays(type: String, holdings: List<Holding>, accountId: Long) {
        val displays = holdings.map { h -> toHoldingDisplay(h, accountId) }
        _uiState.value = when (type) {
            "STOCK" -> _uiState.value.copy(stocks = displays)
            "FUND" -> _uiState.value.copy(funds = displays)
            "GOLD" -> _uiState.value.copy(golds = displays)
            else -> _uiState.value
        }
    }

    private suspend fun toDepositDisplay(d: Deposit, today: kotlinx.datetime.LocalDate): DepositDisplay {
        val holdingDays = interestCalculator.holdingDays(d.startDate, d.maturityDate, today)
        val interest = interestCalculator.accruedInterest(d.principal, d.interestRate, d.startDate, d.maturityDate, today)
        val rate = if (d.currency == "CNY") BigDecimal.ONE
        else exchangeRateRepository.getRate(d.currency, "CNY", today) ?: BigDecimal.ONE

        val totalDays = d.startDate.until(d.maturityDate, kotlinx.datetime.DateTimeUnit.DAY).toInt().coerceAtLeast(1)
        val progress = (holdingDays.toFloat() / totalDays).coerceIn(0f, 1f)

        // 今日利息 = 本金 × 年化利率 ÷ 365
        val dailyInterest = d.principal.multiply(d.interestRate)
            .divide(BigDecimal(365), 6, RoundingMode.HALF_UP)
            .multiply(rate).setScale(2, RoundingMode.HALF_UP)

        return DepositDisplay(
            id = d.id,
            name = d.name,
            bank = d.bank,
            principal = "${d.principal} ${d.currency}",
            rate = "${d.interestRate.multiply(BigDecimal(100)).setScale(2, RoundingMode.HALF_UP)}%",
            accruedInterest = formatMoney(interest),
            holdingDays = holdingDays,
            totalDays = totalDays,
            startDate = d.startDate.toString(),
            maturityDate = d.maturityDate.toString(),
            progress = progress,
            currentValue = formatMoney(valuationCalculator.calcDepositValueCNY(d, rate, today)),
            currency = d.currency,
            todayInterest = "+${formatMoney(dailyInterest)}"
        )
    }

    private suspend fun toHoldingDisplay(h: Holding, accountId: Long): HoldingDisplay {
        val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
        val yesterday = today.minus(1, DateTimeUnit.DAY)
        val latestSnapshot = priceSnapshotDao.getLatest(h.id, accountId)
        val currentPrice = latestSnapshot?.unitPrice ?: h.costPrice

        val rate = if (h.currency == "CNY") BigDecimal.ONE
        else exchangeRateRepository.getRate(h.currency, "CNY", today) ?: BigDecimal.ONE

        val totalPnL = valuationCalculator.calcHoldingPnL(h, currentPrice, rate)
        val totalPnLPct = valuationCalculator.calcHoldingPnLPct(h, currentPrice, rate)

        // 今日涨跌 = 最新价格 vs 昨日价格
        val yesterdaySnapshot = priceSnapshotDao.getByHoldingAndDate(h.id, yesterday, accountId)
        val prevPrice = yesterdaySnapshot?.unitPrice
            ?: priceSnapshotDao.getPrevious(h.id, accountId)?.unitPrice
            ?: currentPrice
        val todayChange = currentPrice.subtract(prevPrice).multiply(h.quantity).multiply(rate)

        return HoldingDisplay(
            id = h.id,
            type = h.type,
            symbol = h.symbol,
            name = h.name,
            quantity = "${h.quantity}",
            costPrice = formatMoney(h.costPrice.multiply(rate)),
            currentPrice = formatMoney(currentPrice.multiply(rate)),
            totalPnL = if (totalPnL >= BigDecimal.ZERO) "+${formatMoney(totalPnL)}" else formatMoney(totalPnL),
            totalPnLPct = if (totalPnLPct >= BigDecimal.ZERO) "+${totalPnLPct.setScale(2, RoundingMode.HALF_UP)}%"
            else "${totalPnLPct.setScale(2, RoundingMode.HALF_UP)}%",
            todayChange = if (todayChange >= BigDecimal.ZERO) "+${formatMoney(todayChange)}" else formatMoney(todayChange),
            isUp = totalPnL >= BigDecimal.ZERO
        )
    }

    private fun formatMoney(value: BigDecimal): String {
        val abs = value.abs()
        val intPart = abs.toBigInteger().toString()
        val formatted = intPart.reversed().chunked(3).joinToString(",").reversed()
        val sign = if (value < BigDecimal.ZERO) "-" else ""
        return "$sign$formatted"
    }
}
