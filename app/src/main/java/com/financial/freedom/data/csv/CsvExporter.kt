package com.financial.freedom.data.csv

import android.content.Context
import android.net.Uri
import com.financial.freedom.data.local.dao.DepositDao
import com.financial.freedom.data.local.dao.HoldingDao
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import java.io.OutputStream
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CsvExporter @Inject constructor(
    @ApplicationContext private val context: Context,
    private val depositDao: DepositDao,
    private val holdingDao: HoldingDao
) {
    suspend fun exportAll(uri: Uri, accountId: Long, pin: String? = null) {
        val csv = buildAllCsv(accountId)
        val bytes = if (pin != null) CsvEncryption.encrypt(csv.toByteArray(), pin) else csv.toByteArray()
        context.contentResolver.openOutputStream(uri)?.use { it.write(bytes) }
    }

    suspend fun exportAllToStream(output: OutputStream, accountId: Long, pin: String? = null) {
        val csv = buildAllCsv(accountId)
        val bytes = if (pin != null) CsvEncryption.encrypt(csv.toByteArray(), pin) else csv.toByteArray()
        output.use { it.write(bytes) }
    }

    private suspend fun buildAllCsv(accountId: Long): String = buildString {
        appendLine("type,name,bank,symbol,market,currency,principal,quantity,cost_price,interest_rate,start_date,end_date,note")

        val deposits = depositDao.getAll(accountId).first()
        deposits.forEach { d ->
            appendLine("DEPOSIT,${csvEscape(d.name)},${csvEscape(d.bank)},,,${d.currency},${d.principal},,,${d.interestRate},${d.startDate},${d.maturityDate},${csvEscape(d.note)}")
        }

        val holdings = holdingDao.getAll(accountId).first()
        holdings.forEach { h ->
            appendLine("${h.type},${csvEscape(h.name)},,,${h.symbol},${h.market},${h.currency},,${h.quantity},${h.costPrice},,${h.costDate},,${csvEscape(h.note)}")
        }
    }

    private fun csvEscape(s: String): String {
        if (s.contains(",") || s.contains("\"") || s.contains("\n")) {
            return "\"${s.replace("\"", "\"\"")}\""
        }
        return s
    }
}
