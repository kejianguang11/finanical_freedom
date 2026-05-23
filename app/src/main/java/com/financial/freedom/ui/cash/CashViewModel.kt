package com.financial.freedom.ui.cash

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.financial.freedom.data.local.entity.CashTransaction
import com.financial.freedom.data.repository.CashRepository
import com.financial.freedom.domain.account.AccountManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn
import java.math.BigDecimal
import java.math.RoundingMode
import javax.inject.Inject

data class CashUiState(
    val balance: String = "0",
    val transactions: List<CashTransaction> = emptyList(),
    val showAddDialog: Boolean = false,
    val showWithdrawDialog: Boolean = false
)

@HiltViewModel
class CashViewModel @Inject constructor(
    private val cashRepository: CashRepository,
    private val accountManager: AccountManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(CashUiState())
    val uiState: StateFlow<CashUiState> = _uiState.asStateFlow()

    init {
        val accountId = accountManager.currentAccountId.value
        if (accountId != null) {
            viewModelScope.launch {
                cashRepository.getAll(accountId).collect { txs ->
                    val balance = cashRepository.getBalance(accountId)
                    _uiState.value = _uiState.value.copy(
                        balance = formatMoney(balance),
                        transactions = txs
                    )
                }
            }
        }
    }

    fun showAddDialog() { _uiState.value = _uiState.value.copy(showAddDialog = true) }
    fun hideAddDialog() { _uiState.value = _uiState.value.copy(showAddDialog = false) }
    fun showWithdrawDialog() { _uiState.value = _uiState.value.copy(showWithdrawDialog = true) }
    fun hideWithdrawDialog() { _uiState.value = _uiState.value.copy(showWithdrawDialog = false) }

    fun addCash(amount: BigDecimal, note: String) {
        val accountId = accountManager.currentAccountId.value ?: return
        viewModelScope.launch {
            cashRepository.insert(
                CashTransaction(
                    accountId = accountId,
                    date = Clock.System.todayIn(TimeZone.currentSystemDefault()),
                    amount = amount,
                    type = "MANUAL",
                    note = note
                )
            )
            _uiState.value = _uiState.value.copy(showAddDialog = false)
        }
    }

    fun withdrawCash(amount: BigDecimal, note: String) {
        val accountId = accountManager.currentAccountId.value ?: return
        viewModelScope.launch {
            cashRepository.insert(
                CashTransaction(
                    accountId = accountId,
                    date = Clock.System.todayIn(TimeZone.currentSystemDefault()),
                    amount = amount.negate(),
                    type = "MANUAL",
                    note = note
                )
            )
            _uiState.value = _uiState.value.copy(showWithdrawDialog = false)
        }
    }

    private fun formatMoney(value: BigDecimal): String {
        val rounded = value.setScale(2, RoundingMode.HALF_UP)
        val abs = rounded.abs()
        val intPart = abs.toBigInteger().toString()
        val formatted = intPart.reversed().chunked(3).joinToString(",").reversed()
        val decimal = abs.subtract(BigDecimal(abs.toBigInteger())).toPlainString().removePrefix("0")
        val full = if (decimal.isNotEmpty() && decimal != ".00") "$formatted$decimal" else formatted
        return if (rounded < BigDecimal.ZERO) "-$full" else full
    }
}
