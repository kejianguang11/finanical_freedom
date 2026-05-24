package com.financial.freedom.ui.credit

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.financial.freedom.data.local.entity.CashTransaction
import com.financial.freedom.data.local.entity.Debt
import com.financial.freedom.data.local.entity.Receivable
import com.financial.freedom.data.repository.CashRepository
import com.financial.freedom.data.repository.DebtRepository
import com.financial.freedom.data.repository.ReceivableRepository
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
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn
import java.math.BigDecimal
import javax.inject.Inject

data class CreditUiState(
    val receivables: List<Receivable> = emptyList(),
    val debts: List<Debt> = emptyList(),
    val receivablesTotal: String = "0",
    val debtsTotal: String = "0",
    val netAmount: String = "0",
    val netSign: Int = 0,
    val showAddReceivable: Boolean = false,
    val showEditReceivable: Boolean = false,
    val editingReceivable: Receivable? = null,
    val showAddDebt: Boolean = false,
    val showEditDebt: Boolean = false,
    val editingDebt: Debt? = null,
    val displayMultiplier: BigDecimal = BigDecimal.ONE
)

@HiltViewModel
class CreditViewModel @Inject constructor(
    private val receivableRepository: ReceivableRepository,
    private val debtRepository: DebtRepository,
    private val cashRepository: CashRepository,
    private val accountManager: AccountManager,
    private val displaySettings: DisplaySettings,
    private val backfillEngine: BackfillEngine
) : ViewModel() {

    private val _uiState = MutableStateFlow(CreditUiState())
    val uiState: StateFlow<CreditUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            displaySettings.multiplierFlow.collect { multiplier ->
                _uiState.value = _uiState.value.copy(displayMultiplier = multiplier)
            }
        }
        val accountId = accountManager.currentAccountId.value
        if (accountId != null) {
            viewModelScope.launch {
                combine(
                    receivableRepository.getAll(accountId),
                    debtRepository.getAll(accountId)
                ) { receivables, debts ->
                    val rTotal = receivables.filter { it.status == "未还" }
                        .fold(BigDecimal.ZERO) { acc, r -> acc.add(r.amount) }
                    val dTotal = debts.filter { it.status == "未还" }
                        .fold(BigDecimal.ZERO) { acc, d -> acc.add(d.amount) }
                    val net = rTotal.subtract(dTotal)
                    val m = _uiState.value.displayMultiplier
                    _uiState.value.copy(
                        receivables = receivables,
                        debts = debts,
                        receivablesTotal = formatMoney(rTotal, m),
                        debtsTotal = formatMoney(dTotal, m),
                        netAmount = formatMoney(net.abs(), m),
                        netSign = net.compareTo(BigDecimal.ZERO)
                    )
                }.collect { _uiState.value = it }
            }
            viewModelScope.launch {
                displaySettings.multiplierFlow.drop(1).collect { m ->
                    refresh()
                }
            }
        }
    }

    fun refresh() {
        val accountId = accountManager.currentAccountId.value ?: return
        viewModelScope.launch {
            val rTotal = receivableRepository.getTotal(accountId)
            val dTotal = debtRepository.getTotal(accountId)
            val net = rTotal.subtract(dTotal)
            val m = _uiState.value.displayMultiplier
            _uiState.value = _uiState.value.copy(
                receivablesTotal = formatMoney(rTotal, m),
                debtsTotal = formatMoney(dTotal, m),
                netAmount = formatMoney(net.abs(), m),
                netSign = net.compareTo(BigDecimal.ZERO)
            )
        }
    }

    // Receivable actions
    fun showAddReceivable() { _uiState.value = _uiState.value.copy(showAddReceivable = true) }
    fun hideAddReceivable() { _uiState.value = _uiState.value.copy(showAddReceivable = false) }
    fun showEditReceivable(r: Receivable) { _uiState.value = _uiState.value.copy(showEditReceivable = true, editingReceivable = r) }
    fun hideEditReceivable() { _uiState.value = _uiState.value.copy(showEditReceivable = false, editingReceivable = null) }

    fun addReceivable(name: String, amount: BigDecimal, date: kotlinx.datetime.LocalDate, note: String, deductFromCash: Boolean) {
        val accountId = accountManager.currentAccountId.value ?: return
        viewModelScope.launch {
            val r = Receivable(
                accountId = accountId,
                name = name,
                amount = amount,
                date = date,
                note = note
            )
            receivableRepository.insert(r)
            if (deductFromCash) {
                cashRepository.insert(
                    CashTransaction(
                        accountId = accountId,
                        date = date,
                        amount = amount.negate(),
                        type = "LEND",
                        note = "借给$name",
                        relatedId = r.id
                    )
                )
                withContext(Dispatchers.IO) {
                    backfillEngine.markDirtyAndBackfill(date, accountId)
                }
            }
            _uiState.value = _uiState.value.copy(showAddReceivable = false)
        }
    }

    fun updateReceivable(id: Long, name: String, amount: BigDecimal, date: kotlinx.datetime.LocalDate, note: String) {
        val accountId = accountManager.currentAccountId.value ?: return
        viewModelScope.launch {
            val existing = receivableRepository.getById(id, accountId) ?: return@launch
            receivableRepository.update(existing.copy(name = name, amount = amount, date = date, note = note))
            _uiState.value = _uiState.value.copy(showEditReceivable = false, editingReceivable = null)
        }
    }

    fun markReceivableRepaid(r: Receivable) {
        val accountId = accountManager.currentAccountId.value ?: return
        viewModelScope.launch {
            receivableRepository.update(r.copy(status = "已还"))
            cashRepository.insert(
                CashTransaction(
                    accountId = accountId,
                    date = Clock.System.todayIn(TimeZone.currentSystemDefault()),
                    amount = r.amount,
                    type = "REPAY",
                    note = "${r.name}还款",
                    relatedId = r.id
                )
            )
            withContext(Dispatchers.IO) {
                backfillEngine.markDirtyAndBackfill(
                    Clock.System.todayIn(TimeZone.currentSystemDefault()), accountId
                )
            }
        }
    }

    fun deleteReceivable(r: Receivable) {
        viewModelScope.launch { receivableRepository.delete(r) }
    }

    // Debt actions
    fun showAddDebt() { _uiState.value = _uiState.value.copy(showAddDebt = true) }
    fun hideAddDebt() { _uiState.value = _uiState.value.copy(showAddDebt = false) }
    fun showEditDebt(d: Debt) { _uiState.value = _uiState.value.copy(showEditDebt = true, editingDebt = d) }
    fun hideEditDebt() { _uiState.value = _uiState.value.copy(showEditDebt = false, editingDebt = null) }

    fun addDebt(name: String, amount: BigDecimal, date: kotlinx.datetime.LocalDate, note: String, addToCash: Boolean) {
        val accountId = accountManager.currentAccountId.value ?: return
        viewModelScope.launch {
            val d = Debt(
                accountId = accountId,
                name = name,
                amount = amount,
                date = date,
                note = note
            )
            debtRepository.insert(d)
            if (addToCash) {
                cashRepository.insert(
                    CashTransaction(
                        accountId = accountId,
                        date = date,
                        amount = amount,
                        type = "REPAY",
                        note = "向${name}借款",
                        relatedId = d.id
                    )
                )
                withContext(Dispatchers.IO) {
                    backfillEngine.markDirtyAndBackfill(date, accountId)
                }
            }
            _uiState.value = _uiState.value.copy(showAddDebt = false)
        }
    }

    fun updateDebt(id: Long, name: String, amount: BigDecimal, date: kotlinx.datetime.LocalDate, note: String) {
        val accountId = accountManager.currentAccountId.value ?: return
        viewModelScope.launch {
            val existing = debtRepository.getById(id, accountId) ?: return@launch
            debtRepository.update(existing.copy(name = name, amount = amount, date = date, note = note))
            _uiState.value = _uiState.value.copy(showEditDebt = false, editingDebt = null)
        }
    }

    fun markDebtPaid(d: Debt) {
        val accountId = accountManager.currentAccountId.value ?: return
        viewModelScope.launch {
            debtRepository.update(d.copy(status = "已还"))
            cashRepository.insert(
                CashTransaction(
                    accountId = accountId,
                    date = Clock.System.todayIn(TimeZone.currentSystemDefault()),
                    amount = d.amount.negate(),
                    type = "REPAY",
                    note = "归还${d.name}",
                    relatedId = d.id
                )
            )
            withContext(Dispatchers.IO) {
                backfillEngine.markDirtyAndBackfill(
                    Clock.System.todayIn(TimeZone.currentSystemDefault()), accountId
                )
            }
        }
    }

    fun deleteDebt(d: Debt) {
        viewModelScope.launch { debtRepository.delete(d) }
    }

    private fun formatMoney(value: BigDecimal, multiplier: BigDecimal): String {
        return com.financial.freedom.ui.common.FormatUtils.formatMoney(value, multiplier)
    }
}
