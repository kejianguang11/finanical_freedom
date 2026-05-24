package com.financial.freedom.ui.holdings.deposit

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.financial.freedom.data.local.dao.CashTransactionDao
import com.financial.freedom.data.local.dao.DailySummaryDao
import com.financial.freedom.data.local.dao.DepositDao
import com.financial.freedom.data.local.entity.CashTransaction
import com.financial.freedom.data.local.entity.Deposit
import com.financial.freedom.domain.account.AccountManager
import com.financial.freedom.domain.calculator.BackfillEngine
import com.financial.freedom.ui.common.FormatUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.math.BigDecimal
import javax.inject.Inject

@HiltViewModel
class AddDepositViewModel @Inject constructor(
    private val depositDao: DepositDao,
    private val cashTransactionDao: CashTransactionDao,
    private val dailySummaryDao: DailySummaryDao,
    private val backfillEngine: BackfillEngine,
    private val accountManager: AccountManager
) : ViewModel() {
    private val _isSaving = MutableStateFlow(false)
    val isSaving: StateFlow<Boolean> = _isSaving.asStateFlow()

    fun save(deposit: Deposit, deductFromCash: Boolean = false, onSuccess: (savedAmount: String, totalAssets: String) -> Unit, onError: (String) -> Unit) {
        if (_isSaving.value) return
        val accountId = accountManager.currentAccountId.value
        if (accountId == null) {
            onError("未登录账号")
            return
        }
        _isSaving.value = true
        viewModelScope.launch {
            try {
                depositDao.insert(deposit.copy(accountId = accountId))
                if (deductFromCash) {
                    cashTransactionDao.insert(
                        CashTransaction(
                            accountId = accountId,
                            date = deposit.startDate,
                            amount = -deposit.principal,
                            type = "ASSET_PURCHASE",
                            note = "购买存款: ${deposit.name}"
                        )
                    )
                }
                // Compute approximate total assets after save
                val latestDate = dailySummaryDao.getLatestDate(accountId)
                val latestNw = if (latestDate != null) {
                    dailySummaryDao.getByDate(latestDate, accountId)?.netWorth ?: BigDecimal.ZERO
                } else BigDecimal.ZERO
                val roughTotal = latestNw.add(deposit.principal)
                val savedAmount = FormatUtils.formatMoney(deposit.principal)
                val totalAssets = FormatUtils.formatMoneyShort(roughTotal)
                onSuccess(savedAmount, totalAssets)
                viewModelScope.launch(Dispatchers.IO) {
                    backfillEngine.markDirtyAndBackfill(deposit.startDate, accountId)
                }
            } catch (e: Exception) {
                onError(e.message ?: "未知错误")
            } finally {
                _isSaving.value = false
            }
        }
    }
}
