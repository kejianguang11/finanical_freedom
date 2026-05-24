package com.financial.freedom.data.repository

import com.financial.freedom.data.local.dao.DebtDao
import com.financial.freedom.data.local.entity.Debt
import kotlinx.coroutines.flow.Flow
import java.math.BigDecimal
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DebtRepository @Inject constructor(
    private val dao: DebtDao
) {
    fun getAll(accountId: Long): Flow<List<Debt>> = dao.getAll(accountId)

    suspend fun getTotal(accountId: Long): BigDecimal {
        val all = dao.getAllList(accountId)
        return all.filter { it.status == "未还" }.fold(BigDecimal.ZERO) { acc, d -> acc.add(d.amount) }
    }

    suspend fun getById(id: Long, accountId: Long): Debt? = dao.getById(id, accountId)

    suspend fun insert(d: Debt): Long = dao.insert(d)

    suspend fun update(d: Debt) = dao.update(d)

    suspend fun delete(d: Debt) = dao.delete(d)

    suspend fun deleteByAccountId(accountId: Long) = dao.deleteByAccountId(accountId)
}
