package com.financial.freedom.ui.cash

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.financial.freedom.data.local.entity.CashTransaction
import com.financial.freedom.data.repository.CashRepository
import com.financial.freedom.domain.account.AccountManager
import com.financial.freedom.domain.calculator.BackfillEngine
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
import kotlinx.datetime.LocalDate
import java.math.BigDecimal
import javax.inject.Inject

data class CashUiState(
    val balance: String = "0",
    val transactions: List<CashTransaction> = emptyList(),
    val showAddDialog: Boolean = false,
    val showWithdrawDialog: Boolean = false,
    val displayMultiplier: BigDecimal = BigDecimal.ONE
)

@HiltViewModel
class CashViewModel @Inject constructor(
    private val cashRepository: CashRepository,
    private val accountManager: AccountManager,
    private val displaySettings: DisplaySettings,
    private val backfillEngine: BackfillEngine
) : ViewModel() {

    private val _uiState = MutableStateFlow(CashUiState())
    val uiState: StateFlow<CashUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            displaySettings.multiplierFlow.collect { multiplier ->
                _uiState.value = _uiState.value.copy(displayMultiplier = multiplier)
            }
        }
        val accountId = accountManager.currentAccountId.value
        if (accountId != null) {
            viewModelScope.launch {
                cashRepository.getAll(accountId).collect { txs ->
                    val balance = cashRepository.getBalance(accountId)
                    val m = _uiState.value.displayMultiplier
                    _uiState.value = _uiState.value.copy(
                        balance = formatMoney(balance, m),
                        transactions = txs
                    )
                }
            }
            // 倍率变化时重新格式化余额
            viewModelScope.launch {
                displaySettings.multiplierFlow.drop(1).collect { m ->
                    val balance = cashRepository.getBalance(accountId)
                    _uiState.value = _uiState.value.copy(
                        balance = formatMoney(balance, m)
                    )
                }
            }
        }
    }

    fun showAddDialog() { _uiState.value = _uiState.value.copy(showAddDialog = true) }
    fun hideAddDialog() { _uiState.value = _uiState.value.copy(showAddDialog = false) }
    fun showWithdrawDialog() { _uiState.value = _uiState.value.copy(showWithdrawDialog = true) }
    fun hideWithdrawDialog() { _uiState.value = _uiState.value.copy(showWithdrawDialog = false) }

    fun addCash(amount: BigDecimal, note: String, date: LocalDate) {
        val accountId = accountManager.currentAccountId.value ?: return
        viewModelScope.launch {
            cashRepository.insert(
                CashTransaction(
                    accountId = accountId,
                    date = date,
                    amount = amount,
                    type = "MANUAL",
                    note = note
                )
            )
            _uiState.value = _uiState.value.copy(showAddDialog = false)
            withContext(Dispatchers.IO) {
                backfillEngine.markDirtyAndBackfill(date, accountId)
            }
        }
    }

    fun withdrawCash(amount: BigDecimal, note: String, date: LocalDate) {
        val accountId = accountManager.currentAccountId.value ?: return
        viewModelScope.launch {
            cashRepository.insert(
                CashTransaction(
                    accountId = accountId,
                    date = date,
                    amount = amount.negate(),
                    type = "MANUAL",
                    note = note
                )
            )
            _uiState.value = _uiState.value.copy(showWithdrawDialog = false)
            withContext(Dispatchers.IO) {
                backfillEngine.markDirtyAndBackfill(date, accountId)
            }
        }
    }

    fun deleteCash(tx: CashTransaction) {
        val accountId = accountManager.currentAccountId.value ?: return
        viewModelScope.launch {
            cashRepository.delete(tx)
            withContext(Dispatchers.IO) {
                backfillEngine.markDirtyAndBackfill(tx.date, accountId)
            }
        }
    }

    private fun formatMoney(value: BigDecimal, multiplier: BigDecimal): String {
        return com.financial.freedom.ui.common.FormatUtils.formatMoney(value, multiplier)
    }
}
