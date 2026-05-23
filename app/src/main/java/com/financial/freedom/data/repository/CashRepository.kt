package com.financial.freedom.data.repository

import com.financial.freedom.data.local.dao.CashTransactionDao
import com.financial.freedom.data.local.entity.CashTransaction
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.LocalDate
import java.math.BigDecimal
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CashRepository @Inject constructor(
    private val dao: CashTransactionDao
) {
    fun getAll(accountId: Long): Flow<List<CashTransaction>> = dao.getAll(accountId)

    suspend fun getBalance(accountId: Long): BigDecimal {
        val all = dao.getAllList(accountId)
        return all.fold(BigDecimal.ZERO) { acc, tx -> acc.add(tx.amount) }
    }

    suspend fun getById(id: Long, accountId: Long): CashTransaction? = dao.getById(id, accountId)

    suspend fun insert(tx: CashTransaction): Long = dao.insert(tx)

    suspend fun update(tx: CashTransaction) = dao.update(tx)

    suspend fun delete(tx: CashTransaction) = dao.delete(tx)

    suspend fun deleteByAccountId(accountId: Long) = dao.deleteByAccountId(accountId)

    suspend fun getCumulativeBalance(accountId: Long, asOfDate: LocalDate): BigDecimal =
        dao.getCumulativeBalance(accountId, asOfDate)
}
