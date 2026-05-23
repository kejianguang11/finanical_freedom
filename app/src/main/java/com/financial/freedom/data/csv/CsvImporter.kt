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
    val sampleRows: List<String>,
    val isEncrypted: Boolean = false
)

@Singleton
class CsvImporter @Inject constructor(
    @ApplicationContext private val context: Context,
    private val depositDao: DepositDao,
    private val holdingDao: HoldingDao
) {
    fun preview(uri: Uri): ImportPreview? {
        val rawBytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() } ?: return null
        val isEnc = CsvEncryption.isEncrypted(rawBytes)
        val content = if (isEnc) {
            "（加密文件，需要 PIN 解密）"
        } else {
            String(rawBytes)
        }
        val lines = content.split("\n").filter { it.isNotBlank() }
        if (lines.isEmpty() && !isEnc) return null

        val fileName = uri.lastPathSegment ?: "unknown.csv"
        return if (isEnc) {
            ImportPreview(fileName = fileName, totalRows = -1, sampleRows = emptyList(), isEncrypted = true)
        } else {
            val dataRows = lines.drop(1).filter { it.isNotBlank() }
            ImportPreview(
                fileName = fileName,
                totalRows = dataRows.size,
                sampleRows = dataRows.take(3).map { it.take(120) },
                isEncrypted = false
            )
        }
    }

    suspend fun importAll(uri: Uri, accountId: Long, pin: String? = null): Int {
        val rawBytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() } ?: return 0
        val isEnc = CsvEncryption.isEncrypted(rawBytes)
        val content = if (isEnc) {
            if (pin == null) return 0
            String(CsvEncryption.decrypt(rawBytes, pin))
        } else {
            String(rawBytes)
        }

        val lines = content.split("\n").filter { it.isNotBlank() }
        if (lines.isEmpty()) return 0
        val header = lines.first().lowercase().split(",").map { it.trim() }
        var count = 0

        for (line in lines.drop(1)) {
            if (line.isBlank()) continue
            val values = parseCsvLine(line)
            val map = header.zip(values).toMap()
            val type = map["type"]?.uppercase() ?: continue

            when (type) {
                "DEPOSIT" -> {
                    depositDao.insert(
                        Deposit(
                            accountId = accountId,
                            name = map["name"] ?: "",
                            bank = map["bank"] ?: "",
                            currency = map["currency"] ?: "CNY",
                            principal = map["principal"]?.let { BigDecimal(it) } ?: BigDecimal.ZERO,
                            interestRate = map["interest_rate"]?.let { BigDecimal(it) } ?: BigDecimal.ZERO,
                            startDate = map["start_date"]?.let { LocalDate.parse(it) } ?: LocalDate.fromEpochDays(0),
                            maturityDate = map["end_date"]?.let { LocalDate.parse(it) } ?: LocalDate.fromEpochDays(0),
                            status = "active",
                            note = map["note"] ?: ""
                        )
                    )
                }
                "GOLD" -> {
                    holdingDao.insert(
                        Holding(
                            accountId = accountId,
                            type = "GOLD",
                            symbol = "XAU",
                            name = "黄金",
                            market = map["market"] ?: "",
                            currency = map["currency"] ?: "CNY",
                            quantity = map["quantity"]?.let { BigDecimal(it) } ?: BigDecimal.ZERO,
                            costPrice = map["cost_price"]?.let { BigDecimal(it) } ?: BigDecimal.ZERO,
                            costDate = map["start_date"]?.let { LocalDate.parse(it) } ?: LocalDate.fromEpochDays(0),
                            note = map["note"] ?: ""
                        )
                    )
                }
                else -> {
                    holdingDao.insert(
                        Holding(
                            accountId = accountId,
                            type = type,
                            symbol = map["symbol"] ?: "",
                            name = map["name"] ?: "",
                            market = map["market"] ?: "",
                            currency = map["currency"] ?: "CNY",
                            quantity = map["quantity"]?.let { BigDecimal(it) } ?: BigDecimal.ZERO,
                            costPrice = map["cost_price"]?.let { BigDecimal(it) } ?: BigDecimal.ZERO,
                            costDate = map["start_date"]?.let { LocalDate.parse(it) } ?: LocalDate.fromEpochDays(0),
                            note = map["note"] ?: ""
                        )
                    )
                }
            }
            count++
        }
        return count
    }

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
