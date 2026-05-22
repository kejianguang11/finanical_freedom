package com.financial.freedom.ui.settings

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.financial.freedom.data.csv.CsvExporter
import com.financial.freedom.data.csv.CsvImporter
import com.financial.freedom.data.csv.ImportPreview
import com.financial.freedom.data.local.AppDatabase
import com.financial.freedom.data.local.dao.ExchangeRateDao
import com.financial.freedom.data.local.entity.Account
import com.financial.freedom.data.TestDataGenerator
import com.financial.freedom.domain.account.AccountManager
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject

data class SettingsUiState(
    val rates: String = "加载中...",
    val importPreview: ImportPreview? = null,
    val importDone: Boolean = false,
    val exportDone: Boolean = false,
    val showClearConfirm: Boolean = false,
    val shareIntent: Intent? = null,
    val accounts: List<Account> = emptyList(),
    val currentAccountNickname: String = "",
    val showAccountDialog: Boolean = false,
    val showPinChangeDialog: Boolean = false,
    val showDeleteConfirm: Boolean = false,
    val showCreateAccountDialog: Boolean = false,
    val testDataDone: Boolean = false
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val csvExporter: CsvExporter,
    private val csvImporter: CsvImporter,
    private val exchangeRateDao: ExchangeRateDao,
    private val database: AppDatabase,
    private val accountManager: AccountManager,
    private val testDataGenerator: TestDataGenerator
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            loadRates()
            loadAccounts()
            accountManager.currentAccount.collect { account ->
                _uiState.value = _uiState.value.copy(
                    currentAccountNickname = account?.nickname ?: ""
                )
            }
        }
    }

    private fun accountId(): Long? = accountManager.currentAccountId.value

    fun exportDeposits(uri: Uri) {
        viewModelScope.launch {
            val id = accountId() ?: return@launch
            csvExporter.exportDeposits(uri, id)
            _uiState.value = _uiState.value.copy(exportDone = true)
        }
    }

    fun exportHoldings(uri: Uri) {
        viewModelScope.launch {
            val id = accountId() ?: return@launch
            csvExporter.exportHoldings(uri, id)
            _uiState.value = _uiState.value.copy(exportDone = true)
        }
    }

    fun previewImport(uri: Uri) {
        val preview = csvImporter.preview(uri)
        _uiState.value = _uiState.value.copy(importPreview = preview)
    }

    fun confirmImportDeposits(uri: Uri) {
        viewModelScope.launch {
            val id = accountId() ?: return@launch
            val count = csvImporter.importDeposits(uri, id)
            _uiState.value = _uiState.value.copy(importPreview = null, importDone = count > 0)
        }
    }

    fun confirmImportHoldings(uri: Uri) {
        viewModelScope.launch {
            val id = accountId() ?: return@launch
            val count = csvImporter.importHoldings(uri, id)
            _uiState.value = _uiState.value.copy(importPreview = null, importDone = count > 0)
        }
    }

    fun dismissImportPreview() {
        _uiState.value = _uiState.value.copy(importPreview = null)
    }

    fun refreshRates() {
        viewModelScope.launch { loadRates() }
    }

    fun showClearConfirm() {
        _uiState.value = _uiState.value.copy(showClearConfirm = true)
    }

    fun dismissClearConfirm() {
        _uiState.value = _uiState.value.copy(showClearConfirm = false)
    }

    fun clearAllData() {
        viewModelScope.launch {
            database.clearAllTables()
            _uiState.value = _uiState.value.copy(showClearConfirm = false)
        }
    }

    fun shareDeposits() {
        viewModelScope.launch {
            val id = accountId() ?: return@launch
            val intent = withContext(Dispatchers.IO) {
                val cacheDir = File(context.cacheDir, "exports")
                cacheDir.mkdirs()
                val file = File(cacheDir, "deposits.csv")
                file.outputStream().use { output ->
                    csvExporter.exportDepositsToStream(output, id)
                }
                val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                Intent(Intent.ACTION_SEND).apply {
                    type = "text/csv"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
            }
            _uiState.value = _uiState.value.copy(shareIntent = intent)
        }
    }

    fun shareHoldings() {
        viewModelScope.launch {
            val id = accountId() ?: return@launch
            val intent = withContext(Dispatchers.IO) {
                val cacheDir = File(context.cacheDir, "exports")
                cacheDir.mkdirs()
                val file = File(cacheDir, "holdings.csv")
                file.outputStream().use { output ->
                    csvExporter.exportHoldingsToStream(output, id)
                }
                val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                Intent(Intent.ACTION_SEND).apply {
                    type = "text/csv"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
            }
            _uiState.value = _uiState.value.copy(shareIntent = intent)
        }
    }

    fun clearShareIntent() {
        _uiState.value = _uiState.value.copy(shareIntent = null)
    }

    // ---- Account management ----

    fun loadAccounts() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(accounts = accountManager.getAllAccounts())
        }
    }

    fun showAccountDialog() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                accounts = accountManager.getAllAccounts(),
                showAccountDialog = true
            )
        }
    }

    fun dismissAccountDialog() {
        _uiState.value = _uiState.value.copy(showAccountDialog = false)
    }

    fun switchAccount(account: Account) {
        viewModelScope.launch {
            accountManager.switchTo(account)
            _uiState.value = _uiState.value.copy(showAccountDialog = false)
        }
    }

    fun showPinChangeDialog() {
        _uiState.value = _uiState.value.copy(showPinChangeDialog = true)
    }

    fun dismissPinChangeDialog() {
        _uiState.value = _uiState.value.copy(showPinChangeDialog = false)
    }

    fun changePin(oldPin: String, newPin: String, onError: (String) -> Unit) {
        viewModelScope.launch {
            val account = accountManager.currentAccount.value ?: run {
                onError("未登录账号")
                return@launch
            }
            if (!accountManager.verifyPin(account, oldPin)) {
                onError("旧 PIN 错误")
                return@launch
            }
            accountManager.changePin(account.id, newPin)
            _uiState.value = _uiState.value.copy(showPinChangeDialog = false)
        }
    }

    fun showDeleteConfirm() {
        _uiState.value = _uiState.value.copy(showDeleteConfirm = true)
    }

    fun dismissDeleteConfirm() {
        _uiState.value = _uiState.value.copy(showDeleteConfirm = false)
    }

    fun deleteCurrentAccount() {
        viewModelScope.launch {
            val id = accountId() ?: return@launch
            accountManager.deleteAccount(id)
            _uiState.value = _uiState.value.copy(showDeleteConfirm = false)
        }
    }

    fun showCreateAccountDialog() {
        _uiState.value = _uiState.value.copy(showCreateAccountDialog = true)
    }

    fun dismissCreateAccountDialog() {
        _uiState.value = _uiState.value.copy(showCreateAccountDialog = false)
    }

    fun createAccount(nickname: String, pin: String) {
        viewModelScope.launch {
            accountManager.createAccount(nickname, pin)
            _uiState.value = _uiState.value.copy(showCreateAccountDialog = false)
        }
    }

    fun generateTestData() {
        viewModelScope.launch {
            testDataGenerator.generate()
            _uiState.value = _uiState.value.copy(testDataDone = true)
        }
    }

    fun dismissTestDataDone() {
        _uiState.value = _uiState.value.copy(testDataDone = false)
    }

    private suspend fun loadRates() {
        val rates = exchangeRateDao.getLatestRates()
        _uiState.value = _uiState.value.copy(
            rates = if (rates.isEmpty()) "暂无汇率数据" else
                rates.joinToString("\n") { "${it.fromCurrency}→${it.toCurrency}: ${it.rate}" }
        )
    }
}
