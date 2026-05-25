package com.financial.freedom.ui.holdings.deposit

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.financial.freedom.data.local.entity.Deposit
import com.financial.freedom.data.repository.DepositRepository
import com.financial.freedom.data.repository.ExchangeRateRepository
import com.financial.freedom.domain.account.AccountManager
import com.financial.freedom.domain.calculator.InterestCalculator
import com.financial.freedom.domain.calculator.ValuationCalculator
import com.financial.freedom.domain.settings.DisplaySettings
import com.financial.freedom.ui.holdings.DepositDisplay
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn
import kotlinx.datetime.until
import java.math.BigDecimal
import java.math.RoundingMode
import javax.inject.Inject

data class BankDepositsUiState(
    val bankName: String = "",
    val status: String = "",
    val depositCount: Int = 0,
    val totalPrincipal: String = "",
    val totalInterest: String = "",
    val totalCurrentValue: String = "",
    val todayTotalInterest: String = "",
    val deposits: List<DepositDisplay> = emptyList(),
    val displayMultiplier: BigDecimal = BigDecimal.ONE
)

@HiltViewModel
class BankDepositsViewModel @Inject constructor(
    private val depositRepository: DepositRepository,
    private val exchangeRateRepository: ExchangeRateRepository,
    private val valuationCalculator: ValuationCalculator,
    private val interestCalculator: InterestCalculator,
    private val accountManager: AccountManager,
    private val displaySettings: DisplaySettings
) : ViewModel() {

    private val _uiState = MutableStateFlow(BankDepositsUiState())
    val uiState: StateFlow<BankDepositsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            displaySettings.multiplierFlow.collect { multiplier ->
                _uiState.value = _uiState.value.copy(displayMultiplier = multiplier)
            }
        }
        viewModelScope.launch {
            displaySettings.multiplierFlow.drop(1).collect {
                val state = _uiState.value
                if (state.bankName.isNotBlank()) {
                    load(state.bankName, state.status)
                }
            }
        }
    }

    fun load(bankName: String, status: String) {
        if (_uiState.value.bankName == bankName && _uiState.value.status == status) return
        _uiState.value = _uiState.value.copy(bankName = bankName, status = status)

        viewModelScope.launch {
            val accountId = accountManager.currentAccountId.value ?: return@launch
            val today = Clock.System.todayIn(TimeZone.currentSystemDefault())

            val activeDeposits = depositRepository.getActiveList(accountId)
            val inactiveDeposits = depositRepository.getInactiveList(accountId).first()

            val activeBankDeposits = activeDeposits.filter { it.bank == bankName }
            val inactiveBankDeposits = inactiveDeposits.filter { it.bank == bankName }
            val allBankDeposits = activeBankDeposits + inactiveBankDeposits

            val activeDisplays = activeBankDeposits.map { d -> toDepositDisplay(d, today) }

            val totalPrincipalRaw = activeBankDeposits.sumOf { d ->
                val rate = if (d.currency == "CNY") BigDecimal.ONE
                else exchangeRateRepository.getRate(d.currency, "CNY", today) ?: BigDecimal.ONE
                d.principal.multiply(rate)
            }

            val totalInterestRaw = allBankDeposits.sumOf { d ->
                val rate = if (d.currency == "CNY") BigDecimal.ONE
                else exchangeRateRepository.getRate(d.currency, "CNY", today) ?: BigDecimal.ONE
                interestCalculator.accruedInterest(d.principal, d.interestRate, d.startDate, d.maturityDate, today)
                    .multiply(rate).setScale(2, RoundingMode.HALF_UP)
            }

            val totalValueRaw = activeBankDeposits.sumOf { d ->
                val rate = if (d.currency == "CNY") BigDecimal.ONE
                else exchangeRateRepository.getRate(d.currency, "CNY", today) ?: BigDecimal.ONE
                valuationCalculator.calcDepositValueCNY(d, rate, today)
            }

            val todayInterestRaw = activeBankDeposits.sumOf { d ->
                val rate = if (d.currency == "CNY") BigDecimal.ONE
                else exchangeRateRepository.getRate(d.currency, "CNY", today) ?: BigDecimal.ONE
                d.principal.multiply(d.interestRate)
                    .divide(BigDecimal(365), 6, RoundingMode.HALF_UP)
                    .multiply(rate).setScale(2, RoundingMode.HALF_UP)
            }

            _uiState.value = _uiState.value.copy(
                depositCount = activeBankDeposits.size,
                totalPrincipal = formatMoney(totalPrincipalRaw.setScale(0, RoundingMode.HALF_UP)),
                totalInterest = formatMoney(totalInterestRaw),
                totalCurrentValue = formatMoney(totalValueRaw),
                todayTotalInterest = formatMoney(todayInterestRaw.abs()),
                deposits = activeDisplays
            )
        }
    }

    fun deleteDeposit(depositId: Long, onDeleted: () -> Unit) {
        viewModelScope.launch {
            val accountId = accountManager.currentAccountId.value ?: return@launch
            val deposit = depositRepository.getById(depositId, accountId)
            if (deposit != null) {
                depositRepository.delete(deposit)
            }
            onDeleted()
            load(_uiState.value.bankName, _uiState.value.status)
        }
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
            todayInterest = formatMoney(dailyInterest.abs()),
            isInterestUp = dailyInterest >= BigDecimal.ZERO,
            status = d.status
        )
    }

    private fun formatMoney(value: BigDecimal): String {
        val multiplier = _uiState.value.displayMultiplier
        return com.financial.freedom.ui.common.FormatUtils.formatMoney(value, multiplier)
    }
}
