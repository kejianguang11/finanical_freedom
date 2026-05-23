package com.financial.freedom.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.financial.freedom.data.local.entity.CashTransaction
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.LocalDate
import java.math.BigDecimal

@Dao
interface CashTransactionDao {
    @Query("SELECT * FROM cash_transactions WHERE accountId = :accountId ORDER BY date DESC")
    fun getAll(accountId: Long): Flow<List<CashTransaction>>

    @Query("SELECT * FROM cash_transactions WHERE accountId = :accountId ORDER BY date DESC")
    suspend fun getAllList(accountId: Long): List<CashTransaction>

    @Query("SELECT * FROM cash_transactions WHERE accountId = :accountId AND date BETWEEN :start AND :end ORDER BY date DESC")
    suspend fun getByDateRange(accountId: Long, start: LocalDate, end: LocalDate): List<CashTransaction>

    @Query("SELECT * FROM cash_transactions WHERE id = :id AND accountId = :accountId")
    suspend fun getById(id: Long, accountId: Long): CashTransaction?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(tx: CashTransaction): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(txs: List<CashTransaction>)

    @Update
    suspend fun update(tx: CashTransaction)

    @Delete
    suspend fun delete(tx: CashTransaction)

    @Query("DELETE FROM cash_transactions WHERE accountId = :accountId")
    suspend fun deleteByAccountId(accountId: Long)

    @Query("SELECT COALESCE(SUM(amount), 0) FROM cash_transactions WHERE accountId = :accountId AND date <= :asOfDate")
    suspend fun getCumulativeBalance(accountId: Long, asOfDate: LocalDate): BigDecimal
}
