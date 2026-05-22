package com.financial.freedom.ui.holdings.deposit

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.financial.freedom.data.local.dao.DepositDao
import com.financial.freedom.data.local.entity.Deposit
import com.financial.freedom.domain.account.AccountManager
import com.financial.freedom.domain.calculator.BackfillEngine
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AddDepositViewModel @Inject constructor(
    private val depositDao: DepositDao,
    private val backfillEngine: BackfillEngine,
    private val accountManager: AccountManager
) : ViewModel() {
    fun save(deposit: Deposit, onSuccess: () -> Unit, onError: (String) -> Unit) {
        val accountId = accountManager.currentAccountId.value
        if (accountId == null) {
            onError("未登录账号")
            return
        }
        viewModelScope.launch {
            try {
                depositDao.insert(deposit.copy(accountId = accountId))
                // 从存款开始日期重新回填历史收益
                backfillEngine.markDirtyAndBackfill(deposit.startDate, accountId)
                onSuccess()
            } catch (e: Exception) {
                onError(e.message ?: "未知错误")
            }
        }
    }
}
