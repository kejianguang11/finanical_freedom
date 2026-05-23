package com.financial.freedom.ui.settings

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.financial.freedom.data.DefaultDataSeeder
import com.financial.freedom.data.csv.CsvExporter
import com.financial.freedom.data.csv.CsvImporter
import com.financial.freedom.data.csv.ImportPreview
import com.financial.freedom.data.local.AppDatabase
import com.financial.freedom.data.local.dao.ExchangeRateDao
import com.financial.freedom.data.repository.ExchangeRateRepository
import com.financial.freedom.data.local.entity.Account
import com.financial.freedom.data.TestDataGenerator
import com.financial.freedom.domain.account.AccountManager
import com.financial.freedom.domain.backup.BackupData
import com.financial.freedom.domain.backup.BackupManager
import com.financial.freedom.domain.calculator.BackfillEngine
import com.financial.freedom.domain.settings.DisplaySettings
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn
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
    val testDataDone: Boolean = false,
    val showExportPinDialog: Boolean = false,
    val showImportPinDialog: Boolean = false,
    val pendingExportUri: android.net.Uri? = null,
    val pendingImportUri: android.net.Uri? = null,
    // Backup/Restore
    val backupDone: Boolean = false,
    val restorePreview: BackupData? = null,
    val showRestoreConfirm: Boolean = false,
    val restoreDone: Boolean = false,
    val pendingRestoreUri: android.net.Uri? = null,
    // 一键重算收益
    val showRecalcConfirm: Boolean = false,
    val isRecalculating: Boolean = false,
    val recalcDone: Boolean = false,
    // 显示倍率
    val displayMultiplier: java.math.BigDecimal = java.math.BigDecimal.ONE
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val csvExporter: CsvExporter,
    private val csvImporter: CsvImporter,
    private val exchangeRateDao: ExchangeRateDao,
    private val exchangeRateRepository: ExchangeRateRepository,
    private val database: AppDatabase,
    private val accountManager: AccountManager,
    private val testDataGenerator: TestDataGenerator,
    private val backupManager: BackupManager,
    private val defaultDataSeeder: DefaultDataSeeder,
    private val backfillEngine: BackfillEngine,
    private val displaySettings: DisplaySettings
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
        viewModelScope.launch {
            displaySettings.multiplierFlow.collect { multiplier ->
                _uiState.value = _uiState.value.copy(displayMultiplier = multiplier)
            }
        }
    }

    private fun accountId(): Long? = accountManager.currentAccountId.value

    fun exportAll(uri: Uri, pin: String? = null) {
        viewModelScope.launch {
            val id = accountId() ?: return@launch
            csvExporter.exportAll(uri, id, pin)
            _uiState.value = _uiState.value.copy(exportDone = true, showExportPinDialog = false, pendingExportUri = null)
        }
    }

    fun showExportPinDialog(uri: Uri) {
        _uiState.value = _uiState.value.copy(showExportPinDialog = true, pendingExportUri = uri)
    }

    fun dismissExportPinDialog() {
        _uiState.value = _uiState.value.copy(showExportPinDialog = false, pendingExportUri = null)
    }

    fun previewImport(uri: Uri) {
        val preview = csvImporter.preview(uri)
        _uiState.value = _uiState.value.copy(importPreview = preview)
    }

    fun confirmImportAll(uri: Uri, pin: String? = null) {
        viewModelScope.launch {
            val id = accountId() ?: return@launch
            val count = csvImporter.importAll(uri, id, pin)
            _uiState.value = _uiState.value.copy(
                importPreview = null, importDone = count > 0,
                showImportPinDialog = false, pendingImportUri = null
            )
        }
    }

    fun showImportPinDialog(uri: Uri) {
        _uiState.value = _uiState.value.copy(showImportPinDialog = true, pendingImportUri = uri)
    }

    fun dismissImportPinDialog() {
        _uiState.value = _uiState.value.copy(showImportPinDialog = false, pendingImportUri = null)
    }

    fun dismissImportPreview() {
        _uiState.value = _uiState.value.copy(importPreview = null)
    }

    fun refreshRates() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(rates = "拉取中...")
            exchangeRateRepository.refreshAllRates()
            loadRates()
        }
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

    fun shareAll() {
        viewModelScope.launch {
            val id = accountId() ?: return@launch
            val intent = withContext(Dispatchers.IO) {
                val cacheDir = File(context.cacheDir, "exports")
                cacheDir.mkdirs()
                val file = File(cacheDir, "assets.csv")
                file.outputStream().use { output ->
                    csvExporter.exportAllToStream(output, id)
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
            defaultDataSeeder.seedIfNeeded()
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

    // ---- Backup / Restore ----

    fun backup(uri: Uri) {
        viewModelScope.launch {
            val id = accountId() ?: return@launch
            try {
                val json = withContext(Dispatchers.IO) { backupManager.exportJson(id) }
                withContext(Dispatchers.IO) {
                    context.contentResolver.openOutputStream(uri)?.use { output ->
                        output.write(json.toByteArray(Charsets.UTF_8))
                    }
                }
                _uiState.value = _uiState.value.copy(backupDone = true)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    backupDone = true
                )
            }
        }
    }

    fun previewRestore(uri: Uri) {
        viewModelScope.launch {
            val id = accountId() ?: return@launch
            try {
                val json = withContext(Dispatchers.IO) {
                    context.contentResolver.openInputStream(uri)?.bufferedReader()?.readText() ?: ""
                }
                val data = withContext(Dispatchers.IO) { backupManager.parseBackup(json) }
                require(data.accountId == id) { "备份数据账号(${data.accountId})与当前账号($id)不匹配" }
                _uiState.value = _uiState.value.copy(
                    restorePreview = data, showRestoreConfirm = true, pendingRestoreUri = uri
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    restoreDone = true
                )
            }
        }
    }

    fun dismissRestoreConfirm() {
        _uiState.value = _uiState.value.copy(showRestoreConfirm = false, restorePreview = null, pendingRestoreUri = null)
    }

    fun confirmRestore() {
        viewModelScope.launch {
            val id = accountId() ?: return@launch
            val uri = _uiState.value.pendingRestoreUri ?: return@launch
            try {
                val json = withContext(Dispatchers.IO) {
                    context.contentResolver.openInputStream(uri)?.bufferedReader()?.readText() ?: ""
                }
                withContext(Dispatchers.IO) { backupManager.restoreFromJson(json, id) }
                _uiState.value = _uiState.value.copy(
                    showRestoreConfirm = false, restorePreview = null, pendingRestoreUri = null, restoreDone = true
                )
            } catch (_: Exception) {
                _uiState.value = _uiState.value.copy(showRestoreConfirm = false, restorePreview = null, pendingRestoreUri = null)
            }
        }
    }

    fun dismissBackupDone() {
        _uiState.value = _uiState.value.copy(backupDone = false)
    }

    fun dismissRestoreDone() {
        _uiState.value = _uiState.value.copy(restoreDone = false)
    }

    // ---- 一键重算收益 ----

    fun showRecalcConfirm() {
        _uiState.value = _uiState.value.copy(showRecalcConfirm = true)
    }

    fun dismissRecalcConfirm() {
        _uiState.value = _uiState.value.copy(showRecalcConfirm = false)
    }

    fun recalculateReturns() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isRecalculating = true)
            try {
                val id = accountId() ?: run {
                    _uiState.value = _uiState.value.copy(isRecalculating = false)
                    return@launch
                }
                // 找到最早的资产日期作为回填起点
                val deposits = withContext(Dispatchers.IO) { database.depositDao().getAllList(id) }
                val holdings = withContext(Dispatchers.IO) { database.holdingDao().getAllList(id) }
                val cashTx = withContext(Dispatchers.IO) { database.cashTransactionDao().getAllList(id) }
                val receivables = withContext(Dispatchers.IO) { database.receivableDao().getAllList(id) }
                val debts = withContext(Dispatchers.IO) { database.debtDao().getAllList(id) }

                val dates = mutableListOf<kotlinx.datetime.LocalDate>()
                dates.addAll(deposits.map { it.startDate })
                dates.addAll(holdings.map { it.costDate })
                dates.addAll(cashTx.map { it.date })
                dates.addAll(receivables.map { it.date })
                dates.addAll(debts.map { it.date })
                val earliestDate = dates.minOrNull() ?: Clock.System.todayIn(TimeZone.currentSystemDefault())

                withContext(Dispatchers.IO) {
                    backfillEngine.markDirtyAndBackfill(earliestDate, id)
                }
                _uiState.value = _uiState.value.copy(showRecalcConfirm = false, isRecalculating = false, recalcDone = true)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(showRecalcConfirm = false, isRecalculating = false, recalcDone = true)
            }
        }
    }

    fun dismissRecalcDone() {
        _uiState.value = _uiState.value.copy(recalcDone = false)
    }

    // ---- 显示倍率 ----

    fun setDisplayMultiplier(value: java.math.BigDecimal) {
        displaySettings.setMultiplier(value)
    }

    private suspend fun loadRates() {
        val rates = exchangeRateDao.getLatestRates()
        _uiState.value = _uiState.value.copy(
            rates = if (rates.isEmpty()) "暂无汇率数据，点击刷新" else
                rates.joinToString("\n") { r ->
                    "${r.fromCurrency}→${r.toCurrency}: ${r.rate.setScale(4, java.math.RoundingMode.HALF_UP)} （${r.date}）"
                }
        )
    }
}
