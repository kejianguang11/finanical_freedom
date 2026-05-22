package com.financial.freedom.data.repository

import com.financial.freedom.data.local.dao.DepositDao
import com.financial.freedom.data.local.entity.Deposit
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DepositRepository @Inject constructor(
    private val dao: DepositDao
) {
    fun getAll(accountId: Long): Flow<List<Deposit>> = dao.getAll(accountId)

    suspend fun getAllList(accountId: Long): List<Deposit> = dao.getAllList(accountId)

    suspend fun getActiveList(accountId: Long): List<Deposit> = dao.getActiveList(accountId)

    suspend fun getById(id: Long, accountId: Long): Deposit? = dao.getById(id, accountId)

    suspend fun insert(deposit: Deposit): Long = dao.insert(deposit)

    suspend fun update(deposit: Deposit) = dao.update(deposit)

    suspend fun delete(deposit: Deposit) = dao.delete(deposit)
}
