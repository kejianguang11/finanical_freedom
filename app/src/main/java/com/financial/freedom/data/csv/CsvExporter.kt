package com.financial.freedom.data.csv

import android.content.Context
import android.net.Uri
import com.financial.freedom.data.local.dao.DepositDao
import com.financial.freedom.data.local.dao.HoldingDao
import com.financial.freedom.data.local.dao.TransactionDao
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import java.io.OutputStream
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CsvExporter @Inject constructor(
    @ApplicationContext private val context: Context,
    private val depositDao: DepositDao,
    private val holdingDao: HoldingDao,
    private val transactionDao: TransactionDao
) {
    suspend fun exportDeposits(uri: Uri, accountId: Long) {
        val csv = buildDepositsCsv(accountId)
        context.contentResolver.openOutputStream(uri)?.use { it.write(csv.toByteArray()) }
    }

    suspend fun exportDepositsToStream(output: OutputStream, accountId: Long) {
        val csv = buildDepositsCsv(accountId)
        output.use { it.write(csv.toByteArray()) }
    }

    suspend fun exportHoldings(uri: Uri, accountId: Long) {
        val csv = buildHoldingsCsv(accountId)
        context.contentResolver.openOutputStream(uri)?.use { it.write(csv.toByteArray()) }
    }

    suspend fun exportHoldingsToStream(output: OutputStream, accountId: Long) {
        val csv = buildHoldingsCsv(accountId)
        output.use { it.write(csv.toByteArray()) }
    }

    suspend fun exportTransactions(uri: Uri, accountId: Long) {
        val csv = buildTransactionsCsv(accountId)
        context.contentResolver.openOutputStream(uri)?.use { it.write(csv.toByteArray()) }
    }

    private suspend fun buildDepositsCsv(accountId: Long): String = buildString {
        val deposits = depositDao.getAll(accountId).first()
        appendLine("name,bank,currency,principal,interest_rate,start_date,maturity_date,status,note")
        deposits.forEach { d ->
            appendLine("${d.name},${d.bank},${d.currency},${d.principal},${d.interestRate},${d.startDate},${d.maturityDate},${d.status},${d.note}")
        }
    }

    private suspend fun buildHoldingsCsv(accountId: Long): String = buildString {
        val holdings = holdingDao.getAll(accountId).first()
        appendLine("type,symbol,name,market,currency,quantity,cost_price,cost_date,note")
        holdings.forEach { h ->
            appendLine("${h.type},${h.symbol},${h.name},${h.market},${h.currency},${h.quantity},${h.costPrice},${h.costDate},${h.note}")
        }
    }

    private suspend fun buildTransactionsCsv(accountId: Long): String = buildString {
        val holdings = holdingDao.getAll(accountId).first()
        appendLine("date,type,symbol,action,price,quantity,fee")
        holdings.forEach { h ->
            val transactions = transactionDao.getByHolding(h.id, accountId).first()
            transactions.forEach { t ->
                appendLine("${t.date},${h.type},${h.symbol},${t.type},${t.price},${t.quantity},${t.fee}")
            }
        }
    }
}
