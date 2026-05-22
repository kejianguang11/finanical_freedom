package com.financial.freedom.domain.account

import android.content.Context
import com.financial.freedom.data.local.dao.AccountDao
import com.financial.freedom.data.local.dao.DailyBreakdownItemDao
import com.financial.freedom.data.local.dao.DailySummaryDao
import com.financial.freedom.data.local.dao.DepositDao
import com.financial.freedom.data.local.dao.HoldingDao
import com.financial.freedom.data.local.dao.PriceSnapshotDao
import com.financial.freedom.data.local.dao.TransactionDao
import com.financial.freedom.data.local.entity.Account
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import java.security.MessageDigest
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AccountManager @Inject constructor(
    private val accountDao: AccountDao,
    private val depositDao: DepositDao,
    private val holdingDao: HoldingDao,
    private val priceSnapshotDao: PriceSnapshotDao,
    private val transactionDao: TransactionDao,
    private val dailySummaryDao: DailySummaryDao,
    private val dailyBreakdownItemDao: DailyBreakdownItemDao,
    @ApplicationContext private val context: Context
) {
    private val prefs = context.getSharedPreferences("account_prefs", Context.MODE_PRIVATE)

    private val _currentAccountId = MutableStateFlow<Long?>(null)
    val currentAccountId: StateFlow<Long?> = _currentAccountId.asStateFlow()

    private val _currentAccount = MutableStateFlow<Account?>(null)
    val currentAccount: StateFlow<Account?> = _currentAccount.asStateFlow()

    /** 启动时调用：恢复上次账号（如果有） */
    suspend fun restoreLastAccount(): Boolean {
        val lastId = prefs.getLong("last_account_id", -1L)
        if (lastId == -1L) return false
        val account = accountDao.getById(lastId)
        if (account != null) {
            _currentAccountId.value = account.id
            _currentAccount.value = account
            return true
        }
        return false
    }

    /** 是否有任何账号 */
    suspend fun hasAnyAccount(): Boolean = accountDao.count() > 0

    /** 验证 PIN */
    fun verifyPin(account: Account, pin: String): Boolean =
        hashPin(pin) == account.pinHash

    /** 创建新账号 */
    suspend fun createAccount(nickname: String, pin: String): Account {
        val account = Account(nickname = nickname, pinHash = hashPin(pin))
        val id = accountDao.insert(account)
        val created = account.copy(id = id)
        switchTo(created)
        return created
    }

    /** 切换到指定账号 */
    suspend fun switchTo(account: Account) {
        _currentAccountId.value = account.id
        _currentAccount.value = account
        prefs.edit().putLong("last_account_id", account.id).apply()
    }

    /** 登出当前账号 */
    fun logout() {
        _currentAccountId.value = null
        _currentAccount.value = null
    }

    /** 修改 PIN */
    suspend fun changePin(accountId: Long, newPin: String) {
        val account = accountDao.getById(accountId) ?: return
        val updated = account.copy(pinHash = hashPin(newPin))
        accountDao.update(updated)
        if (_currentAccount.value?.id == accountId) {
            _currentAccount.value = updated
        }
    }

    /** 删除账号及其所有数据（级联删除） */
    suspend fun deleteAccount(accountId: Long) {
        val account = accountDao.getById(accountId) ?: return
        depositDao.deleteByAccountId(accountId)
        holdingDao.deleteByAccountId(accountId)
        priceSnapshotDao.deleteByAccountId(accountId)
        transactionDao.deleteByAccountId(accountId)
        dailySummaryDao.deleteByAccountId(accountId)
        dailyBreakdownItemDao.deleteByAccountId(accountId)
        accountDao.delete(account)
        if (_currentAccountId.value == accountId) {
            logout()
        }
    }

    /** 获取所有账号列表 */
    suspend fun getAllAccounts(): List<Account> = accountDao.getAllList()

    companion object {
        fun hashPin(pin: String): String {
            val digest = MessageDigest.getInstance("SHA-256")
            val hashBytes = digest.digest(pin.toByteArray(Charsets.UTF_8))
            return hashBytes.joinToString("") { "%02x".format(it) }
        }
    }
}
