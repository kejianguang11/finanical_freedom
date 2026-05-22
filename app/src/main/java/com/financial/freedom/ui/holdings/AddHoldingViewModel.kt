package com.financial.freedom.ui.holdings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.financial.freedom.data.local.dao.HoldingDao
import com.financial.freedom.data.local.entity.Holding
import com.financial.freedom.data.remote.PriceService
import com.financial.freedom.data.remote.SearchResult
import com.financial.freedom.domain.account.AccountManager
import com.financial.freedom.domain.calculator.BackfillEngine
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class AddHoldingViewModel @Inject constructor(
    private val holdingDao: HoldingDao,
    private val priceService: PriceService,
    private val backfillEngine: BackfillEngine,
    private val accountManager: AccountManager
) : ViewModel() {
    fun save(holding: Holding, onSuccess: () -> Unit, onError: (String) -> Unit) {
        val accountId = accountManager.currentAccountId.value
        if (accountId == null) {
            onError("未登录账号")
            return
        }
        viewModelScope.launch {
            try {
                holdingDao.insert(holding.copy(accountId = accountId))
                // 从持仓成本日期重新回填历史收益
                backfillEngine.markDirtyAndBackfill(holding.costDate, accountId)
                onSuccess()
            } catch (e: Exception) {
                onError(e.message ?: "未知错误")
            }
        }
    }

    suspend fun search(query: String): List<SearchResult> =
        withContext(Dispatchers.IO) {
            priceService.searchAll(query)
        }
}
