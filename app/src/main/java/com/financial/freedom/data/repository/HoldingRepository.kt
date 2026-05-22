package com.financial.freedom.data.repository

import com.financial.freedom.data.local.dao.HoldingDao
import com.financial.freedom.data.local.entity.Holding
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HoldingRepository @Inject constructor(
    private val dao: HoldingDao
) {
    fun getAll(accountId: Long): Flow<List<Holding>> = dao.getAll(accountId)

    suspend fun getAllList(accountId: Long): List<Holding> = dao.getAllList(accountId)

    fun getByType(type: String, accountId: Long): Flow<List<Holding>> = dao.getByType(type, accountId)

    suspend fun getById(id: Long, accountId: Long): Holding? = dao.getById(id, accountId)

    suspend fun insert(holding: Holding): Long = dao.insert(holding)

    suspend fun update(holding: Holding) = dao.update(holding)

    suspend fun delete(holding: Holding) = dao.delete(holding)
}
