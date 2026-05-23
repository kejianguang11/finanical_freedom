package com.financial.freedom.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.financial.freedom.data.local.entity.Deposit
import kotlinx.coroutines.flow.Flow

@Dao
interface DepositDao {
    @Query("SELECT * FROM deposits WHERE accountId = :accountId ORDER BY startDate DESC")
    fun getAll(accountId: Long): Flow<List<Deposit>>

    @Query("SELECT * FROM deposits WHERE accountId = :accountId ORDER BY startDate DESC")
    suspend fun getAllList(accountId: Long): List<Deposit>

    @Query("SELECT * FROM deposits WHERE accountId = :accountId AND status = 'active' ORDER BY startDate DESC")
    suspend fun getActiveList(accountId: Long): List<Deposit>

    @Query("SELECT * FROM deposits WHERE id = :id AND accountId = :accountId")
    suspend fun getById(id: Long, accountId: Long): Deposit?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(deposit: Deposit): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(deposits: List<Deposit>)

    @Update
    suspend fun update(deposit: Deposit)

    @Delete
    suspend fun delete(deposit: Deposit)

    @Query("DELETE FROM deposits WHERE accountId = :accountId")
    suspend fun deleteByAccountId(accountId: Long)

    @Query("SELECT * FROM deposits WHERE accountId = :accountId AND status = 'matured' ORDER BY maturityDate DESC")
    suspend fun getMaturedList(accountId: Long): List<Deposit>

    @Query("SELECT * FROM deposits WHERE accountId = :accountId AND status = 'settled' ORDER BY maturityDate DESC")
    suspend fun getSettledList(accountId: Long): List<Deposit>

    @Query("SELECT * FROM deposits WHERE accountId = :accountId AND status IN ('matured', 'settled') ORDER BY maturityDate DESC")
    suspend fun getInactiveList(accountId: Long): List<Deposit>

    @Query("SELECT * FROM deposits WHERE accountId = :accountId AND status IN ('matured', 'settled') ORDER BY maturityDate DESC")
    fun getInactiveFlow(accountId: Long): Flow<List<Deposit>>
}
