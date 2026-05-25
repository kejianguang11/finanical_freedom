package com.financial.freedom.ui.holdings.deposit

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.financial.freedom.data.local.dao.CashTransactionDao
import com.financial.freedom.data.local.entity.CashTransaction
import com.financial.freedom.data.local.entity.Deposit
import com.financial.freedom.data.repository.DepositRepository
import com.financial.freedom.data.repository.ExchangeRateRepository
import com.financial.freedom.domain.account.AccountManager
import com.financial.freedom.domain.calculator.InterestCalculator
import com.financial.freedom.domain.settings.DisplaySettings
import com.financial.freedom.ui.common.FormatUtils
import com.financial.freedom.ui.holdings.DepositDisplay
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn
import kotlinx.datetime.until
import java.math.BigDecimal
import java.math.RoundingMode
import javax.inject.Inject

data class MaturedDepositsUiState(
    val bankGroups: List<MaturedBankGroup> = emptyList(),
    val totalCount: Int = 0,
    val grandTotal: String = ""
)

@HiltViewModel
class MaturedDepositsViewModel @Inject constructor(
    private val depositRepository: DepositRepository,
    private val exchangeRateRepository: ExchangeRateRepository,
    private val interestCalculator: InterestCalculator,
    private val cashTransactionDao: CashTransactionDao,
    private val accountManager: AccountManager,
    private val displaySettings: DisplaySettings
) : ViewModel() {

    private val _uiState = MutableStateFlow(MaturedDepositsUiState())
    val uiState: StateFlow<MaturedDepositsUiState> = _uiState.asStateFlow()

    private var multiplier = BigDecimal.ONE
    private var loaded = false

    init {
        viewModelScope.launch {
            displaySettings.multiplierFlow.collect { m ->
                multiplier = m
                if (loaded) load()
            }
        }
    }

    fun load() {
        loaded = true
        viewModelScope.launch {
            val accountId = accountManager.currentAccountId.value ?: return@launch
            val today = Clock.System.todayIn(TimeZone.currentSystemDefault())

            val allDeposits = depositRepository.getInactiveList(accountId).first()

            val groups = allDeposits
                .groupBy { it.bank }
                .map { (bank, deposits) ->
                    val displays = deposits.map { d -> toDisplay(d, today) }
                    val totalPrincipal = deposits.sumOf { d ->
                        val rate = if (d.currency == "CNY") BigDecimal.ONE
                        else exchangeRateRepository.getRate(d.currency, "CNY", today) ?: BigDecimal.ONE
                        d.principal.multiply(rate)
                    }
                    val totalAmount = deposits.sumOf { d ->
                        val rate = if (d.currency == "CNY") BigDecimal.ONE
                        else exchangeRateRepository.getRate(d.currency, "CNY", today) ?: BigDecimal.ONE
                        val interest = interestCalculator.accruedInterest(
                            d.principal, d.interestRate, d.startDate, d.maturityDate, d.maturityDate
                        )
                        d.principal.add(interest).multiply(rate)
                    }
                    MaturedBankGroup(
                        bank = bank,
                        deposits = displays,
                        totalPrincipal = FormatUtils.formatMoney(totalPrincipal.setScale(0, RoundingMode.HALF_UP), multiplier),
                        totalAmount = FormatUtils.formatMoney(totalAmount.setScale(0, RoundingMode.HALF_UP), multiplier)
                    )
                }
                .sortedByDescending { it.deposits.size }

            val grandTotal = allDeposits.sumOf { d ->
                val rate = if (d.currency == "CNY") BigDecimal.ONE
                else exchangeRateRepository.getRate(d.currency, "CNY", today) ?: BigDecimal.ONE
                val interest = interestCalculator.accruedInterest(
                    d.principal, d.interestRate, d.startDate, d.maturityDate, d.maturityDate
                )
                d.principal.add(interest).multiply(rate)
            }

            _uiState.value = MaturedDepositsUiState(
                bankGroups = groups,
                totalCount = allDeposits.size,
                grandTotal = FormatUtils.formatMoney(grandTotal.setScale(0, RoundingMode.HALF_UP), multiplier)
            )
        }
    }

    /** 仅标记已入账，不生成现金流水（用于异常数据修复） */
    fun markSettledOnly(depositId: Long, onDone: () -> Unit) {
        viewModelScope.launch {
            val accountId = accountManager.currentAccountId.value ?: return@launch
            val deposit = depositRepository.getById(depositId, accountId)
            if (deposit != null && deposit.status != "settled") {
                depositRepository.update(deposit.copy(status = "settled"))
            }
            onDone()
            load()
        }
    }

    /** 正常入账：生成现金流水 + 标记已入账 */
    fun settleWithCash(depositId: Long, onDone: () -> Unit) {
        viewModelScope.launch {
            val accountId = accountManager.currentAccountId.value ?: return@launch
            val deposit = depositRepository.getById(depositId, accountId)
            if (deposit != null && deposit.status != "settled") {
                val totalInterest = interestCalculator.accruedInterest(
                    deposit.principal, deposit.interestRate,
                    deposit.startDate, deposit.maturityDate, deposit.maturityDate
                )
                val totalAmount = deposit.principal.add(totalInterest)

                cashTransactionDao.insert(
                    CashTransaction(
                        accountId = accountId,
                        date = deposit.maturityDate,
                        amount = totalAmount,
                        type = "DEPOSIT_MATURITY",
                        note = "${deposit.name}到期入账，本金${deposit.principal} ${deposit.currency}，利息${totalInterest} ${deposit.currency}"
                    )
                )
                depositRepository.update(deposit.copy(status = "settled"))
            }
            onDone()
            load()
        }
    }

    fun deleteDeposit(depositId: Long, onDone: () -> Unit) {
        viewModelScope.launch {
            val accountId = accountManager.currentAccountId.value ?: return@launch
            val deposit = depositRepository.getById(depositId, accountId)
            if (deposit != null) {
                depositRepository.delete(deposit)
            }
            onDone()
            load()
        }
    }

    private suspend fun toDisplay(d: Deposit, today: kotlinx.datetime.LocalDate): DepositDisplay {
        val interestRaw = interestCalculator.accruedInterest(d.principal, d.interestRate, d.startDate, d.maturityDate, d.maturityDate)
        val rate = if (d.currency == "CNY") BigDecimal.ONE
        else exchangeRateRepository.getRate(d.currency, "CNY", today) ?: BigDecimal.ONE

        val totalDays = d.startDate.until(d.maturityDate, kotlinx.datetime.DateTimeUnit.DAY).toInt().coerceAtLeast(1)
        val interestCNY = interestRaw.multiply(rate).setScale(2, RoundingMode.HALF_UP)

        return DepositDisplay(
            id = d.id,
            name = d.name,
            bank = d.bank,
            principal = "${d.principal} ${d.currency}",
            rate = "${d.interestRate.multiply(BigDecimal(100)).setScale(2, RoundingMode.HALF_UP)}%",
            accruedInterest = FormatUtils.formatMoney(interestCNY, multiplier),
            holdingDays = totalDays,
            totalDays = totalDays,
            startDate = d.startDate.toString(),
            maturityDate = d.maturityDate.toString(),
            progress = 1f,
            currentValue = FormatUtils.formatMoney(d.principal.add(interestRaw).multiply(rate).setScale(2, RoundingMode.HALF_UP), multiplier),
            currency = d.currency,
            todayInterest = "0",
            isInterestUp = true,
            status = d.status
        )
    }
}
