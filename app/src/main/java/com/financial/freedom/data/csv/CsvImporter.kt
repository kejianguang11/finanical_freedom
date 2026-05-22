package com.financial.freedom.data.csv

import android.content.Context
import android.net.Uri
import com.financial.freedom.data.local.dao.DepositDao
import com.financial.freedom.data.local.dao.HoldingDao
import com.financial.freedom.data.local.entity.Deposit
import com.financial.freedom.data.local.entity.Holding
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.datetime.LocalDate
import java.io.BufferedReader
import java.io.InputStreamReader
import java.math.BigDecimal
import javax.inject.Inject
import javax.inject.Singleton

data class ImportPreview(
    val fileName: String,
    val totalRows: Int,
    val sampleRows: List<String>
)

@Singleton
class CsvImporter @Inject constructor(
    @ApplicationContext private val context: Context,
    private val depositDao: DepositDao,
    private val holdingDao: HoldingDao
) {
    fun preview(uri: Uri): ImportPreview? {
        val reader = context.contentResolver.openInputStream(uri)?.let { BufferedReader(InputStreamReader(it)) } ?: return null
        val lines = reader.use { it.readLines() }.filter { it.isNotBlank() }
        if (lines.isEmpty()) return null

        val fileName = uri.lastPathSegment ?: "unknown.csv"
        val dataRows = lines.drop(1).filter { it.isNotBlank() }
        return ImportPreview(
            fileName = fileName,
            totalRows = dataRows.size,
            sampleRows = dataRows.take(3).map { it.take(120) }
        )
    }

    suspend fun importDeposits(uri: Uri, accountId: Long): Int {
        val lines = readLines(uri) ?: return 0
        val header = lines.first().lowercase().split(",").map { it.trim() }
        var count = 0

        for (line in lines.drop(1)) {
            if (line.isBlank()) continue
            val values = parseCsvLine(line)
            val map = header.zip(values).toMap()

            depositDao.insert(
                Deposit(
                    accountId = accountId,
                    name = map["name"] ?: "",
                    bank = map["bank"] ?: "",
                    currency = map["currency"] ?: "CNY",
                    principal = map["principal"]?.let { BigDecimal(it) } ?: BigDecimal.ZERO,
                    interestRate = map["interest_rate"]?.let { BigDecimal(it) } ?: BigDecimal.ZERO,
                    startDate = map["start_date"]?.let { LocalDate.parse(it) } ?: LocalDate.fromEpochDays(0),
                    maturityDate = map["maturity_date"]?.let { LocalDate.parse(it) } ?: LocalDate.fromEpochDays(0),
                    status = map["status"] ?: "active",
                    note = map["note"] ?: ""
                )
            )
            count++
        }
        return count
    }

    suspend fun importHoldings(uri: Uri, accountId: Long): Int {
        val lines = readLines(uri) ?: return 0
        val header = lines.first().lowercase().split(",").map { it.trim() }
        var count = 0

        for (line in lines.drop(1)) {
            if (line.isBlank()) continue
            val values = parseCsvLine(line)
            val map = header.zip(values).toMap()

            holdingDao.insert(
                Holding(
                    accountId = accountId,
                    type = map["type"] ?: "STOCK",
                    symbol = map["symbol"] ?: "",
                    name = map["name"] ?: "",
                    market = map["market"] ?: "",
                    currency = map["currency"] ?: "CNY",
                    quantity = map["quantity"]?.let { BigDecimal(it) } ?: BigDecimal.ZERO,
                    costPrice = map["cost_price"]?.let { BigDecimal(it) } ?: BigDecimal.ZERO,
                    costDate = map["cost_date"]?.let { LocalDate.parse(it) } ?: LocalDate.fromEpochDays(0),
                    note = map["note"] ?: ""
                )
            )
            count++
        }
        return count
    }

    private fun readLines(uri: Uri): List<String>? =
        context.contentResolver.openInputStream(uri)?.let { BufferedReader(InputStreamReader(it)) }
            .use { it?.readLines() }

    private fun parseCsvLine(line: String): List<String> {
        val result = mutableListOf<String>()
        val current = StringBuilder()
        var inQuotes = false

        for (ch in line) {
            when {
                ch == '"' -> inQuotes = !inQuotes
                ch == ',' && !inQuotes -> {
                    result.add(current.toString().trim())
                    current.clear()
                }
                else -> current.append(ch)
            }
        }
        result.add(current.toString().trim())
        return result
    }
}
