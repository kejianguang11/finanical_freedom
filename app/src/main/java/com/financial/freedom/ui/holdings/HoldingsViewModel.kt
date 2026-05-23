package com.financial.freedom.ui.holdings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.financial.freedom.data.local.dao.PriceSnapshotDao
import com.financial.freedom.data.local.dao.TransactionDao
import com.financial.freedom.data.local.entity.Deposit
import com.financial.freedom.data.local.entity.Holding
import com.financial.freedom.data.local.entity.Transaction
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
import kotlinx.coroutines.flow.first
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
    val isUp: Boolean,
    val marketValue: String,
    val currency: String
)

// v17: 银行组展示数据
data class BankGroupDisplay(
    val bank: String,
    val depositCount: Int,
    val totalPrincipal: String,
    val totalCurrentValue: String,
    val todayTotalInterest: String,
    val isInterestUp: Boolean,
    val weightedProgress: Float,
    val nearestMaturity: String,
    val currency: String
)

// v17: 买入记录展示数据
data class BuyRecordDisplay(
    val transactionId: Long,
    val holdingId: Long,
    val date: String,
    val type: String,
    val quantity: String,
    val price: String,
    val cost: String,
    val currentValue: String,
    val pnl: String,
    val pnlPct: String,
    val isUp: Boolean
)

// v17: 股票/基金组展示数据
data class HoldingGroupDisplay(
    val symbol: String,
    val name: String,
    val market: String,
    val type: String,
    val totalQuantity: String,
    val avgCost: String,
    val currentPrice: String,
    val totalPnL: String,
    val totalPnLPct: String,
    val todayChange: String,
    val todayChangePct: String,
    val isUp: Boolean,
    val marketValue: String,
    val currency: String,
    val buyRecords: List<BuyRecordDisplay>,
    val mainHoldingId: Long,
    val priceHistory: List<BigDecimal> = emptyList()
)

data class HoldingsUiState(
    // 原有扁平列表（SectionIndicator 汇总用）
    val deposits: List<DepositDisplay> = emptyList(),
    val maturedDeposits: List<DepositDisplay> = emptyList(),
    val stocks: List<HoldingDisplay> = emptyList(),
    val funds: List<HoldingDisplay> = emptyList(),
    val golds: List<HoldingDisplay> = emptyList(),

    // v17: 分类汇总（CategoryNavStrip 用）
    val investmentTotalValue: String = "",
    val investmentTodayChange: String = "",
    val investmentIsUp: Boolean = true,
    val depositTotalValue: String = "",
    val depositTodayInterest: String = "",

    // v17: 子类汇总（SectionIndicator 用）
    val stockTotalValue: String = "",
    val stockTodayChange: String = "",
    val stockIsUp: Boolean = true,
    val fundTotalValue: String = "",
    val fundTodayChange: String = "",
    val fundIsUp: Boolean = true,
    val goldTotalValue: String = "",
    val goldTodayChange: String = "",
    val goldIsUp: Boolean = true,
    val activeDepositTotalValue: String = "",
    val activeDepositTodayInterest: String = "",
    val maturedDepositTotalValue: String = "",

    // v17: 分组列表
    val bankGroups: List<BankGroupDisplay> = emptyList(),
    val maturedBankGroups: List<BankGroupDisplay> = emptyList(),
    val stockGroups: List<HoldingGroupDisplay> = emptyList(),
    val fundGroups: List<HoldingGroupDisplay> = emptyList(),
    val displayMultiplier: java.math.BigDecimal = java.math.BigDecimal.ONE
)

@HiltViewModel
class HoldingsViewModel @Inject constructor(
    private val depositRepository: DepositRepository,
    private val holdingRepository: HoldingRepository,
    private val exchangeRateRepository: ExchangeRateRepository,
    private val priceSnapshotDao: PriceSnapshotDao,
    private val transactionDao: TransactionDao,
    private val valuationCalculator: ValuationCalculator,
    private val interestCalculator: InterestCalculator,
    private val accountManager: AccountManager,
    private val displaySettings: com.financial.freedom.domain.settings.DisplaySettings
) : ViewModel() {

    private val _uiState = MutableStateFlow(HoldingsUiState())
    val uiState: StateFlow<HoldingsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            displaySettings.multiplierFlow.collect { multiplier ->
                _uiState.value = _uiState.value.copy(displayMultiplier = multiplier)
            }
        }
        val accountId = accountManager.currentAccountId.value
        if (accountId != null) {
            val today = Clock.System.todayIn(TimeZone.currentSystemDefault())

            viewModelScope.launch {
                depositRepository.getAll(accountId).collect { deposits ->
                    val displays = deposits.map { d -> toDepositDisplay(d, today) }
                    _uiState.value = _uiState.value.copy(
                        deposits = displays,
                        bankGroups = toBankGroupList(deposits, today),
                        activeDepositTotalValue = computeDepositCategoryValue(displays),
                        activeDepositTodayInterest = computeDepositInterestSum(deposits, today),
                        depositTotalValue = computeDepositCategoryValue(displays),
                        depositTodayInterest = computeDepositInterestSum(deposits, today)
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
            viewModelScope.launch {
                depositRepository.getInactiveList(accountId).collect { matured ->
                    val displays = matured.map { d -> toDepositDisplay(d, today) }
                    _uiState.value = _uiState.value.copy(
                        maturedDeposits = displays,
                        maturedBankGroups = toBankGroupList(matured, today),
                        maturedDepositTotalValue = computeDepositCategoryValue(displays)
                    )
                }
            }
        }
    }

    suspend fun deleteDeposit(deposit: Deposit) = depositRepository.delete(deposit)

    suspend fun deleteHolding(holding: Holding) = holdingRepository.delete(holding)

    private suspend fun updateHoldingDisplays(type: String, holdings: List<Holding>, accountId: Long) {
        val displays = holdings.map { h -> toHoldingDisplay(h, accountId) }
        val groups = toHoldingGroupList(holdings, accountId)
        val totalValue = computeHoldingCategoryValue(displays)
        val todayChange = computeHoldingTodaySum(holdings, accountId)

        val state = _uiState.value
        _uiState.value = when (type) {
            "STOCK" -> state.copy(
                stocks = displays,
                stockGroups = groups,
                stockTotalValue = totalValue,
                stockTodayChange = todayChange.first,
                stockIsUp = todayChange.second,
                investmentTotalValue = computeInvestmentTotal(state.copy(stocks = displays)),
                investmentTodayChange = computeInvestmentTodayChange(state.copy(stocks = displays)),
                investmentIsUp = !computeInvestmentTodayChange(state.copy(stocks = displays)).startsWith("-")
            )
            "FUND" -> state.copy(
                funds = displays,
                fundGroups = groups,
                fundTotalValue = totalValue,
                fundTodayChange = todayChange.first,
                fundIsUp = todayChange.second,
                investmentTotalValue = computeInvestmentTotal(state.copy(funds = displays)),
                investmentTodayChange = computeInvestmentTodayChange(state.copy(funds = displays)),
                investmentIsUp = !computeInvestmentTodayChange(state.copy(funds = displays)).startsWith("-")
            )
            "GOLD" -> state.copy(
                golds = displays,
                goldTotalValue = totalValue,
                goldTodayChange = todayChange.first,
                goldIsUp = todayChange.second,
                investmentTotalValue = computeInvestmentTotal(state.copy(golds = displays)),
                investmentTodayChange = computeInvestmentTodayChange(state.copy(golds = displays)),
                investmentIsUp = !computeInvestmentTodayChange(state.copy(golds = displays)).startsWith("-")
            )
            else -> state
        }
    }

    // v17: 银行分组
    private suspend fun toBankGroupList(
        deposits: List<Deposit>,
        today: kotlinx.datetime.LocalDate
    ): List<BankGroupDisplay> {
        return deposits.groupBy { it.bank }.map { (bank, group) ->
            val totalPrincipalRaw = group.sumOf { d ->
                val rate = if (d.currency == "CNY") BigDecimal.ONE
                else exchangeRateRepository.getRate(d.currency, "CNY", today) ?: BigDecimal.ONE
                d.principal.multiply(rate)
            }
            val totalInterestRaw = group.sumOf { d ->
                val rate = if (d.currency == "CNY") BigDecimal.ONE
                else exchangeRateRepository.getRate(d.currency, "CNY", today) ?: BigDecimal.ONE
                val dailyInterest = d.principal.multiply(d.interestRate)
                    .divide(BigDecimal(365), 6, RoundingMode.HALF_UP)
                dailyInterest.multiply(rate)
            }
            val totalValueRaw = group.sumOf { d ->
                val rate = if (d.currency == "CNY") BigDecimal.ONE
                else exchangeRateRepository.getRate(d.currency, "CNY", today) ?: BigDecimal.ONE
                valuationCalculator.calcDepositValueCNY(d, rate, today)
            }

            // 本金加权进度
            val weightedProgress = if (totalPrincipalRaw.compareTo(BigDecimal.ZERO) > 0) {
                group.sumOf { d ->
                    val holdingDays = interestCalculator.holdingDays(d.startDate, d.maturityDate, today)
                    val totalDays = d.startDate.until(d.maturityDate, kotlinx.datetime.DateTimeUnit.DAY).toInt().coerceAtLeast(1)
                    val progress = holdingDays.toFloat() / totalDays
                    val rate = if (d.currency == "CNY") BigDecimal.ONE
                    else exchangeRateRepository.getRate(d.currency, "CNY", today) ?: BigDecimal.ONE
                    BigDecimal(progress.toDouble()).multiply(d.principal.multiply(rate))
                }.divide(totalPrincipalRaw, 4, RoundingMode.HALF_UP).toFloat()
            } else 0f

            val nearestMaturity = group.minOf { it.maturityDate }

            BankGroupDisplay(
                bank = bank,
                depositCount = group.size,
                totalPrincipal = formatMoney(totalPrincipalRaw.setScale(0, RoundingMode.HALF_UP)),
                totalCurrentValue = formatMoney(totalValueRaw),
                todayTotalInterest = if (totalInterestRaw >= BigDecimal.ZERO)
                    "+${formatMoney(totalInterestRaw)}" else formatMoney(totalInterestRaw),
                isInterestUp = totalInterestRaw >= BigDecimal.ZERO,
                weightedProgress = weightedProgress.coerceIn(0f, 1f),
                nearestMaturity = nearestMaturity.toString(),
                currency = "CNY"
            )
        }.sortedByDescending { parseMoneyValue(it.totalCurrentValue) }
    }

    // v17: 股票/基金分组
    private suspend fun toHoldingGroupList(
        holdings: List<Holding>,
        accountId: Long
    ): List<HoldingGroupDisplay> {
        val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
        val yesterday = today.minus(1, DateTimeUnit.DAY)

        return holdings.groupBy { it.symbol }.map { (symbol, group) ->
            val first = group.first()
            val totalQuantity = group.sumOf { it.quantity }
            val totalCost = group.sumOf { it.quantity.multiply(it.costPrice) }
            val avgCost = if (totalQuantity.compareTo(BigDecimal.ZERO) > 0)
                totalCost.divide(totalQuantity, 4, RoundingMode.HALF_UP)
            else first.costPrice

            val latestSnapshot = priceSnapshotDao.getLatest(first.id, accountId)
            val currentPrice = latestSnapshot?.unitPrice ?: first.costPrice

            val rate = if (first.currency == "CNY") BigDecimal.ONE
            else exchangeRateRepository.getRate(first.currency, "CNY", today) ?: BigDecimal.ONE

            val marketValue = currentPrice.multiply(totalQuantity).multiply(rate)
                .setScale(2, RoundingMode.HALF_UP)
            val totalPnL = marketValue.subtract(totalCost.multiply(rate).setScale(2, RoundingMode.HALF_UP))
            val totalPnLPct = if (totalCost.compareTo(BigDecimal.ZERO) > 0)
                totalPnL.divide(totalCost.multiply(rate), 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal(100))
            else BigDecimal.ZERO

            val prevPrice = priceSnapshotDao.getByHoldingAndDate(first.id, yesterday, accountId)?.unitPrice
                ?: priceSnapshotDao.getLatestBefore(first.id, today, accountId)?.unitPrice
                ?: currentPrice
            val todayChange = currentPrice.subtract(prevPrice).multiply(totalQuantity).multiply(rate)
            val todayChangePct = if (prevPrice.compareTo(BigDecimal.ZERO) > 0)
                currentPrice.subtract(prevPrice).divide(prevPrice, 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal(100))
            else BigDecimal.ZERO

            // 买入记录
            val buyRecords = mutableListOf<BuyRecordDisplay>()
            for (h in group) {
                val transactions = try {
                    transactionDao.getByHolding(h.id, accountId).first()
                } catch (_: Exception) {
                    emptyList()
                }
                for (t in transactions.filter { it.type == "BUY" }.sortedByDescending { it.date }) {
                    val cost = t.price.multiply(t.quantity)
                    val currentVal = currentPrice.multiply(t.quantity).multiply(rate)
                        .setScale(2, RoundingMode.HALF_UP)
                    val pnl = currentVal.subtract(cost.multiply(rate).setScale(2, RoundingMode.HALF_UP))
                    val pnlPct = if (cost.compareTo(BigDecimal.ZERO) > 0)
                        pnl.divide(cost.multiply(rate), 4, RoundingMode.HALF_UP)
                            .multiply(BigDecimal(100))
                    else BigDecimal.ZERO

                    buyRecords.add(
                        BuyRecordDisplay(
                            transactionId = t.id,
                            holdingId = h.id,
                            date = t.date.toString(),
                            type = t.type,
                            quantity = t.quantity.toPlainString(),
                            price = formatMoney(t.price.multiply(rate)),
                            cost = formatMoney(cost.multiply(rate).setScale(2, RoundingMode.HALF_UP)),
                            currentValue = formatMoney(currentVal),
                            pnl = if (pnl >= BigDecimal.ZERO) "+${formatMoney(pnl)}" else formatMoney(pnl),
                            pnlPct = if (pnlPct >= BigDecimal.ZERO) "+${pnlPct.setScale(2, RoundingMode.HALF_UP)}%"
                            else "${pnlPct.setScale(2, RoundingMode.HALF_UP)}%",
                            isUp = pnl >= BigDecimal.ZERO
                        )
                    )
                }
            }

            // 近30天价格历史（迷你走势图数据）
            val historySnapshots = try {
                priceSnapshotDao.getByHoldingAndDateRange(
                    first.id, today.minus(30, DateTimeUnit.DAY), today, accountId
                ).first()
            } catch (_: Exception) {
                emptyList()
            }
            val priceHistory = historySnapshots.sortedBy { it.date }.map { it.unitPrice }

            HoldingGroupDisplay(
                symbol = symbol,
                name = first.name,
                market = first.market,
                type = first.type,
                totalQuantity = totalQuantity.toPlainString(),
                avgCost = formatMoney(avgCost.multiply(rate)),
                currentPrice = formatMoney(currentPrice.multiply(rate)),
                totalPnL = if (totalPnL >= BigDecimal.ZERO) "+${formatMoney(totalPnL)}" else formatMoney(totalPnL),
                totalPnLPct = if (totalPnLPct >= BigDecimal.ZERO) "+${totalPnLPct.setScale(2, RoundingMode.HALF_UP)}%"
                else "${totalPnLPct.setScale(2, RoundingMode.HALF_UP)}%",
                todayChange = if (todayChange >= BigDecimal.ZERO) "+${formatMoney(todayChange)}" else formatMoney(todayChange),
                todayChangePct = if (todayChangePct >= BigDecimal.ZERO) "+${todayChangePct.setScale(2, RoundingMode.HALF_UP)}%"
                else "${todayChangePct.setScale(2, RoundingMode.HALF_UP)}%",
                isUp = totalPnL >= BigDecimal.ZERO,
                marketValue = formatMoney(marketValue),
                currency = first.currency,
                buyRecords = buyRecords,
                mainHoldingId = first.id,
                priceHistory = priceHistory
            )
        }.sortedByDescending { parseMoneyValue(it.marketValue) }
    }

    private fun computeDepositCategoryValue(displays: List<DepositDisplay>): String {
        val sum = displays.sumOf { parseMoneyValue(it.currentValue) }
        return formatMoney(sum)
    }

    private suspend fun computeDepositInterestSum(
        deposits: List<Deposit>,
        today: kotlinx.datetime.LocalDate
    ): String {
        val sum = deposits.sumOf { d ->
            val rate = if (d.currency == "CNY") BigDecimal.ONE
            else exchangeRateRepository.getRate(d.currency, "CNY", today) ?: BigDecimal.ONE
            d.principal.multiply(d.interestRate)
                .divide(BigDecimal(365), 6, RoundingMode.HALF_UP)
                .multiply(rate)
        }
        return if (sum >= BigDecimal.ZERO) "+${formatMoney(sum)}" else formatMoney(sum)
    }

    private fun computeHoldingCategoryValue(displays: List<HoldingDisplay>): String {
        val sum = displays.sumOf { parseMoneyValue(it.marketValue) }
        return formatMoney(sum)
    }

    private suspend fun computeHoldingTodaySum(
        holdings: List<Holding>,
        accountId: Long
    ): Pair<String, Boolean> {
        val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
        val yesterday = today.minus(1, DateTimeUnit.DAY)
        var sum = BigDecimal.ZERO
        for (h in holdings) {
            val latestSnapshot = priceSnapshotDao.getLatest(h.id, accountId)
            val currentPrice = latestSnapshot?.unitPrice ?: h.costPrice
            val prevPrice = priceSnapshotDao.getByHoldingAndDate(h.id, yesterday, accountId)?.unitPrice
                ?: priceSnapshotDao.getLatestBefore(h.id, today, accountId)?.unitPrice
                ?: currentPrice
            val rate = if (h.currency == "CNY") BigDecimal.ONE
            else exchangeRateRepository.getRate(h.currency, "CNY", today) ?: BigDecimal.ONE
            sum = sum.add(currentPrice.subtract(prevPrice).multiply(h.quantity).multiply(rate))
        }
        val formatted = if (sum >= BigDecimal.ZERO) "+${formatMoney(sum)}" else formatMoney(sum)
        return Pair(formatted, sum >= BigDecimal.ZERO)
    }

    private fun computeInvestmentTotal(state: HoldingsUiState): String {
        val sum = listOf(
            parseMoneyValue(state.stockTotalValue),
            parseMoneyValue(state.fundTotalValue),
            parseMoneyValue(state.goldTotalValue)
        ).fold(BigDecimal.ZERO) { acc, v -> acc.add(v) }
        return formatMoney(sum)
    }

    private fun computeInvestmentTodayChange(state: HoldingsUiState): String {
        val sum = listOf(
            parseMoneyValueOrZero(state.stockTodayChange),
            parseMoneyValueOrZero(state.fundTodayChange),
            parseMoneyValueOrZero(state.goldTodayChange)
        ).fold(BigDecimal.ZERO) { acc, v -> acc.add(v) }
        return if (sum >= BigDecimal.ZERO) "+${formatMoney(sum)}" else formatMoney(sum)
    }

    private suspend fun toDepositDisplay(d: Deposit, today: kotlinx.datetime.LocalDate): DepositDisplay {
        val holdingDays = interestCalculator.holdingDays(d.startDate, d.maturityDate, today)
        val interestRaw = interestCalculator.accruedInterest(d.principal, d.interestRate, d.startDate, d.maturityDate, today)
        val rate = if (d.currency == "CNY") BigDecimal.ONE
        else exchangeRateRepository.getRate(d.currency, "CNY", today) ?: BigDecimal.ONE

        val totalDays = d.startDate.until(d.maturityDate, kotlinx.datetime.DateTimeUnit.DAY).toInt().coerceAtLeast(1)
        val progress = (holdingDays.toFloat() / totalDays).coerceIn(0f, 1f)

        val dailyInterest = d.principal.multiply(d.interestRate)
            .divide(BigDecimal(365), 6, RoundingMode.HALF_UP)
            .multiply(rate).setScale(2, RoundingMode.HALF_UP)

        val interestCNY = interestRaw.multiply(rate).setScale(2, RoundingMode.HALF_UP)

        return DepositDisplay(
            id = d.id,
            name = d.name,
            bank = d.bank,
            principal = "${d.principal} ${d.currency}",
            rate = "${d.interestRate.multiply(BigDecimal(100)).setScale(2, RoundingMode.HALF_UP)}%",
            accruedInterest = formatMoney(interestCNY),
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

        val prevPrice = priceSnapshotDao.getByHoldingAndDate(h.id, yesterday, accountId)?.unitPrice
            ?: priceSnapshotDao.getLatestBefore(h.id, today, accountId)?.unitPrice
            ?: currentPrice
        val todayChange = currentPrice.subtract(prevPrice).multiply(h.quantity).multiply(rate)

        val marketValue = currentPrice.multiply(h.quantity).multiply(rate).setScale(2, RoundingMode.HALF_UP)

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
            isUp = totalPnL >= BigDecimal.ZERO,
            marketValue = formatMoney(marketValue),
            currency = h.currency
        )
    }

    private fun formatMoney(value: BigDecimal): String {
        val multiplier = _uiState.value.displayMultiplier
        return com.financial.freedom.ui.common.FormatUtils.formatMoney(value, multiplier)
    }

    private fun parseMoneyValue(formatted: String): BigDecimal {
        return com.financial.freedom.ui.common.FormatUtils.parseMoneyValue(formatted) ?: BigDecimal.ZERO
    }

    private fun parseMoneyValueOrZero(formatted: String): BigDecimal {
        if (formatted.isEmpty()) return BigDecimal.ZERO
        val cleaned = formatted.replace(",", "").replace("+", "").replace("%", "").trim()
        return try {
            BigDecimal(cleaned)
        } catch (_: Exception) {
            BigDecimal.ZERO
        }
    }
}
