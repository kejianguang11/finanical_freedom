package com.financial.freedom.ui.credit

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.financial.freedom.data.local.entity.Debt
import com.financial.freedom.data.local.entity.Receivable
import com.financial.freedom.data.repository.DebtRepository
import com.financial.freedom.data.repository.ReceivableRepository
import com.financial.freedom.domain.account.AccountManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn
import java.math.BigDecimal
import java.math.RoundingMode
import javax.inject.Inject

data class CreditUiState(
    val receivables: List<Receivable> = emptyList(),
    val debts: List<Debt> = emptyList(),
    val receivablesTotal: String = "0",
    val debtsTotal: String = "0",
    val netAmount: String = "0",
    val showAddReceivable: Boolean = false,
    val showEditReceivable: Boolean = false,
    val editingReceivable: Receivable? = null,
    val showAddDebt: Boolean = false,
    val showEditDebt: Boolean = false,
    val editingDebt: Debt? = null
)

@HiltViewModel
class CreditViewModel @Inject constructor(
    private val receivableRepository: ReceivableRepository,
    private val debtRepository: DebtRepository,
    private val accountManager: AccountManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(CreditUiState())
    val uiState: StateFlow<CreditUiState> = _uiState.asStateFlow()

    init {
        val accountId = accountManager.currentAccountId.value
        if (accountId != null) {
            viewModelScope.launch {
                combine(
                    receivableRepository.getAll(accountId),
                    debtRepository.getAll(accountId)
                ) { receivables, debts ->
                    val rTotal = receivables.fold(BigDecimal.ZERO) { acc, r -> acc.add(r.amount) }
                    val dTotal = debts.fold(BigDecimal.ZERO) { acc, d -> acc.add(d.amount) }
                    val net = rTotal.subtract(dTotal)
                    _uiState.value.copy(
                        receivables = receivables,
                        debts = debts,
                        receivablesTotal = formatMoney(rTotal),
                        debtsTotal = formatMoney(dTotal),
                        netAmount = if (net >= BigDecimal.ZERO) "+${formatMoney(net)}" else formatMoney(net)
                    )
                }.collect { _uiState.value = it }
            }
        }
    }

    fun refresh() {
        val accountId = accountManager.currentAccountId.value ?: return
        viewModelScope.launch {
            val rTotal = receivableRepository.getTotal(accountId)
            val dTotal = debtRepository.getTotal(accountId)
            val net = rTotal.subtract(dTotal)
            _uiState.value = _uiState.value.copy(
                receivablesTotal = formatMoney(rTotal),
                debtsTotal = formatMoney(dTotal),
                netAmount = if (net >= BigDecimal.ZERO) "+${formatMoney(net)}" else formatMoney(net)
            )
        }
    }

    // Receivable actions
    fun showAddReceivable() { _uiState.value = _uiState.value.copy(showAddReceivable = true) }
    fun hideAddReceivable() { _uiState.value = _uiState.value.copy(showAddReceivable = false) }
    fun showEditReceivable(r: Receivable) { _uiState.value = _uiState.value.copy(showEditReceivable = true, editingReceivable = r) }
    fun hideEditReceivable() { _uiState.value = _uiState.value.copy(showEditReceivable = false, editingReceivable = null) }

    fun addReceivable(name: String, amount: BigDecimal, expectedDate: kotlinx.datetime.LocalDate?, note: String) {
        val accountId = accountManager.currentAccountId.value ?: return
        viewModelScope.launch {
            receivableRepository.insert(
                Receivable(
                    accountId = accountId,
                    name = name,
                    amount = amount,
                    date = Clock.System.todayIn(TimeZone.currentSystemDefault()),
                    expectedDate = expectedDate,
                    note = note
                )
            )
            _uiState.value = _uiState.value.copy(showAddReceivable = false)
        }
    }

    fun updateReceivable(id: Long, name: String, amount: BigDecimal, expectedDate: kotlinx.datetime.LocalDate?, note: String) {
        val accountId = accountManager.currentAccountId.value ?: return
        viewModelScope.launch {
            val existing = receivableRepository.getById(id, accountId) ?: return@launch
            receivableRepository.update(existing.copy(name = name, amount = amount, expectedDate = expectedDate, note = note))
            _uiState.value = _uiState.value.copy(showEditReceivable = false, editingReceivable = null)
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

    fun addDebt(name: String, amount: BigDecimal, interestRate: BigDecimal?, date: LocalDate, note: String) {
        val accountId = accountManager.currentAccountId.value ?: return
        viewModelScope.launch {
            debtRepository.insert(
                Debt(
                    accountId = accountId,
                    name = name,
                    amount = amount,
                    date = date,
                    interestRate = interestRate,
                    note = note
                )
            )
            _uiState.value = _uiState.value.copy(showAddDebt = false)
        }
    }

    fun updateDebt(id: Long, name: String, amount: BigDecimal, interestRate: BigDecimal?, date: LocalDate, note: String) {
        val accountId = accountManager.currentAccountId.value ?: return
        viewModelScope.launch {
            val existing = debtRepository.getById(id, accountId) ?: return@launch
            debtRepository.update(existing.copy(name = name, amount = amount, interestRate = interestRate, date = date, note = note))
            _uiState.value = _uiState.value.copy(showEditDebt = false, editingDebt = null)
        }
    }

    fun deleteDebt(d: Debt) {
        viewModelScope.launch { debtRepository.delete(d) }
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
