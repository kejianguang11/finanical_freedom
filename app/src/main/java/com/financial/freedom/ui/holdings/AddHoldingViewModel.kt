package com.financial.freedom.ui.holdings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.financial.freedom.data.local.dao.CashTransactionDao
import com.financial.freedom.data.local.dao.HoldingDao
import com.financial.freedom.data.local.dao.TransactionDao
import com.financial.freedom.data.local.entity.CashTransaction
import com.financial.freedom.data.local.entity.Holding
import com.financial.freedom.data.local.entity.Transaction
import com.financial.freedom.data.remote.PriceService
import com.financial.freedom.data.remote.SearchResult
import com.financial.freedom.domain.account.AccountManager
import com.financial.freedom.domain.calculator.BackfillEngine
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.math.BigDecimal
import java.math.RoundingMode
import javax.inject.Inject

@HiltViewModel
class AddHoldingViewModel @Inject constructor(
    private val holdingDao: HoldingDao,
    private val transactionDao: TransactionDao,
    private val cashTransactionDao: CashTransactionDao,
    private val priceService: PriceService,
    private val backfillEngine: BackfillEngine,
    private val accountManager: AccountManager
) : ViewModel() {
    private val _searchResults = MutableStateFlow<List<SearchResult>>(emptyList())
    val searchResults: StateFlow<List<SearchResult>> = _searchResults.asStateFlow()

    private val _isSearching = MutableStateFlow(false)
    val isSearching: StateFlow<Boolean> = _isSearching.asStateFlow()

    private var searchJob: Job? = null

    fun onSymbolChanged(query: String) {
        searchJob?.cancel()
        if (query.length < 2) {
            _searchResults.value = emptyList()
            _isSearching.value = false
            return
        }
        searchJob = viewModelScope.launch {
            _isSearching.value = true
            delay(300)
            try {
                _searchResults.value = withContext(Dispatchers.IO) {
                    priceService.searchAll(query)
                }
            } catch (_: Exception) {
                _searchResults.value = emptyList()
            } finally {
                _isSearching.value = false
            }
        }
    }

    fun clearSearchResults() {
        _searchResults.value = emptyList()
    }
    fun save(holding: Holding, deductFromCash: Boolean = false, onSuccess: () -> Unit, onError: (String) -> Unit) {
        if (_isSaving.value) return
        val accountId = accountManager.currentAccountId.value
        if (accountId == null) {
            onError("未登录账号")
            return
        }
        _isSaving.value = true
        viewModelScope.launch {
            try {
                val holdingId = holdingDao.insert(holding.copy(accountId = accountId))
                transactionDao.insert(
                    Transaction(
                        holdingId = holdingId,
                        accountId = accountId,
                        type = "BUY",
                        date = holding.costDate,
                        price = holding.costPrice,
                        quantity = holding.quantity
                    )
                )
                if (deductFromCash) {
                    val totalCost = holding.quantity.multiply(holding.costPrice).setScale(2, RoundingMode.HALF_UP)
                    cashTransactionDao.insert(
                        CashTransaction(
                            accountId = accountId,
                            date = holding.costDate,
                            amount = -totalCost,
                            type = "ASSET_PURCHASE",
                            note = "购买${typeLabel(holding.type)}: ${holding.name.ifBlank { holding.symbol }}"
                        )
                    )
                }
                onSuccess()
                // 回填在后台跑，不阻塞返回
                viewModelScope.launch(Dispatchers.IO) {
                    backfillEngine.markDirtyAndBackfill(holding.costDate, accountId)
                }
            } catch (e: Exception) {
                onError(e.message ?: "未知错误")
            } finally {
                _isSaving.value = false
            }
        }
    }

    suspend fun search(query: String): List<SearchResult> =
        withContext(Dispatchers.IO) {
            priceService.searchAll(query)
        }

    private val _isSaving = MutableStateFlow(false)
    val isSaving: StateFlow<Boolean> = _isSaving.asStateFlow()

    private fun typeLabel(type: String): String = when (type) {
        "STOCK" -> "股票"
        "FUND" -> "基金"
        "GOLD" -> "黄金"
        else -> "资产"
    }
}
