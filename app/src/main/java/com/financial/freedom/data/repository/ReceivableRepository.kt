package com.financial.freedom.data.repository

import com.financial.freedom.data.local.dao.ReceivableDao
import com.financial.freedom.data.local.entity.Receivable
import kotlinx.coroutines.flow.Flow
import java.math.BigDecimal
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ReceivableRepository @Inject constructor(
    private val dao: ReceivableDao
) {
    fun getAll(accountId: Long): Flow<List<Receivable>> = dao.getAll(accountId)

    suspend fun getTotal(accountId: Long): BigDecimal {
        val all = dao.getAllList(accountId)
        return all.fold(BigDecimal.ZERO) { acc, r -> acc.add(r.amount) }
    }

    suspend fun getById(id: Long, accountId: Long): Receivable? = dao.getById(id, accountId)

    suspend fun insert(r: Receivable): Long = dao.insert(r)

    suspend fun update(r: Receivable) = dao.update(r)

    suspend fun delete(r: Receivable) = dao.delete(r)

    suspend fun deleteByAccountId(accountId: Long) = dao.deleteByAccountId(accountId)
}
