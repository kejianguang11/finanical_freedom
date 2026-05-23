package com.financial.freedom.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.financial.freedom.data.local.entity.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {
    @Query("SELECT * FROM transactions WHERE holdingId = :holdingId AND accountId = :accountId ORDER BY date DESC")
    fun getByHolding(holdingId: Long, accountId: Long): Flow<List<Transaction>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(transaction: Transaction): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(transactions: List<Transaction>)

    @Delete
    suspend fun delete(transaction: Transaction)

    @Query("DELETE FROM transactions WHERE accountId = :accountId")
    suspend fun deleteByAccountId(accountId: Long)

    @Query("SELECT * FROM transactions WHERE accountId = :accountId ORDER BY date ASC")
    suspend fun getAllList(accountId: Long): List<Transaction>
}
