package com.financial.freedom.data

import android.content.Context
import android.util.Log
import com.financial.freedom.data.local.dao.DepositDao
import com.financial.freedom.data.local.dao.ExchangeRateDao
import com.financial.freedom.data.local.dao.HoldingDao
import com.financial.freedom.data.local.dao.TransactionDao
import com.financial.freedom.data.local.entity.Deposit
import com.financial.freedom.data.local.entity.ExchangeRate
import com.financial.freedom.data.local.entity.Holding
import com.financial.freedom.data.local.entity.Transaction
import com.financial.freedom.domain.account.AccountManager
import com.financial.freedom.domain.calculator.BackfillEngine
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn
import java.math.BigDecimal
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DefaultDataSeeder @Inject constructor(
    private val accountManager: AccountManager,
    private val depositDao: DepositDao,
    private val exchangeRateDao: ExchangeRateDao,
    private val holdingDao: HoldingDao,
    private val transactionDao: TransactionDao,
    private val backfillEngine: BackfillEngine,
    @ApplicationContext private val context: Context
) {
    private val prefs = context.getSharedPreferences("account_prefs", Context.MODE_PRIVATE)

    suspend fun seedIfNeeded() {
        val seeded = prefs.getBoolean("default_seeded", false)
        if (seeded && accountManager.hasAnyAccount()) return
        if (accountManager.hasAnyAccount() && !seeded) {
            prefs.edit().putBoolean("default_seeded", true).apply()
            return
        }

        try {
            Log.d("DefaultSeeder", "No accounts found, creating default god account...")

            // 1. Create god account with PIN "0000"
            val account = accountManager.createAccount("god", "0000")
            val accountId = account.id
            Log.d("DefaultSeeder", "Created god account id=$accountId")

            // 2. Seed exchange rates
            val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
            exchangeRateDao.insertAll(
                listOf(
                    ExchangeRate(fromCurrency = "USD", toCurrency = "CNY", date = today, rate = BigDecimal("7.25")),
                    ExchangeRate(fromCurrency = "HKD", toCurrency = "CNY", date = today, rate = BigDecimal("0.93"))
                )
            )
            Log.d("DefaultSeeder", "Seeded exchange rates")

            // 3. Read CSV from assets
            val csvText = context.assets.open("deposits_import.csv")
                .bufferedReader().use { it.readText() }
            val lines = csvText.lines().filter { it.isNotBlank() }
            if (lines.size < 2) {
                Log.w("DefaultSeeder", "CSV empty or missing header")
                prefs.edit().putBoolean("default_seeded", true).apply()
                return
            }

            // 4. Parse header
            val headers = parseCsvLine(lines[0]).map { it.trim().lowercase() }
            Log.d("DefaultSeeder", "CSV headers: $headers")

            // 5. Parse and insert rows
            var depositCount = 0
            var holdingCount = 0
            for (i in 1 until lines.size) {
                val values = parseCsvLine(lines[i])
                if (values.size < headers.size) continue
                val map = headers.zip(values).toMap()

                val type = map["type"]?.uppercase() ?: continue
                when (type) {
                    "DEPOSIT" -> {
                        val deposit = Deposit(
                            accountId = accountId,
                            name = map["name"] ?: (map["bank"] ?: ""),
                            bank = map["bank"] ?: "",
                            currency = map["currency"] ?: "CNY",
                            principal = map["principal"]?.let { BigDecimal(it.trim()) } ?: BigDecimal.ZERO,
                            interestRate = map["interest_rate"]?.takeIf { it.isNotBlank() }
                                ?.let { BigDecimal(it.trim()).divide(BigDecimal(100), 6, java.math.RoundingMode.HALF_UP) }
                                ?: BigDecimal.ZERO,
                            startDate = map["start_date"]?.let { parseDate(it.trim()) } ?: LocalDate.fromEpochDays(0),
                            maturityDate = map["end_date"]?.let { parseDate(it.trim()) } ?: LocalDate.fromEpochDays(0),
                            status = "active",
                            note = map["note"] ?: ""
                        )
                        depositDao.insert(deposit)
                        depositCount++
                    }
                    "HOLDING" -> {
                        val costDate = map["start_date"]?.let { parseDate(it.trim()) } ?: continue
                        val quantity = map["quantity"]?.takeIf { it.isNotBlank() }?.let { BigDecimal(it.trim()) } ?: continue
                        val costPrice = map["cost_price"]?.takeIf { it.isNotBlank() }?.let { BigDecimal(it.trim()) } ?: continue
                        val holding = Holding(
                            accountId = accountId,
                            type = "STOCK",
                            symbol = map["symbol"] ?: "",
                            name = map["name"] ?: "",
                            market = map["market"] ?: "",
                            currency = map["currency"] ?: "USD",
                            quantity = quantity,
                            costPrice = costPrice,
                            costDate = costDate,
                            note = map["note"] ?: "",
                            status = "active"
                        )
                        val holdingId = holdingDao.insert(holding)
                        transactionDao.insert(
                            Transaction(
                                holdingId = holdingId,
                                accountId = accountId,
                                type = "BUY",
                                date = costDate,
                                price = costPrice,
                                quantity = quantity
                            )
                        )
                        holdingCount++
                    }
                }
            }

            Log.d("DefaultSeeder", "Imported $depositCount deposits and $holdingCount holdings for god account")

            // 6. Backfill will be triggered by HomeViewModel on first visit

            prefs.edit().putBoolean("default_seeded", true).apply()
        } catch (e: Exception) {
            Log.e("DefaultSeeder", "Seed failed: ${e.message}", e)
            prefs.edit().putBoolean("default_seeded", true).apply() // Don't retry on failure
        }
    }

    private fun parseCsvLine(line: String): List<String> {
        val result = mutableListOf<String>()
        var current = StringBuilder()
        var inQuotes = false
        for (ch in line) {
            when {
                ch == '"' -> inQuotes = !inQuotes
                ch == ',' && !inQuotes -> {
                    result.add(current.toString())
                    current = StringBuilder()
                }
                else -> current.append(ch)
            }
        }
        result.add(current.toString())
        return result
    }

    private fun parseDate(s: String): LocalDate? {
        return try {
            LocalDate.parse(s.trim())
        } catch (_: Exception) {
            null
        }
    }
}
