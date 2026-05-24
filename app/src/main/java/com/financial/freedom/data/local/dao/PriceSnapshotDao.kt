package com.financial.freedom.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.financial.freedom.data.local.entity.PriceSnapshot
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.LocalDate

@Dao
interface PriceSnapshotDao {
    @Query("SELECT * FROM price_snapshots WHERE holdingId = :holdingId AND accountId = :accountId AND date BETWEEN :start AND :end ORDER BY date ASC")
    fun getByHoldingAndDateRange(holdingId: Long, start: LocalDate, end: LocalDate, accountId: Long): Flow<List<PriceSnapshot>>

    @Query("SELECT * FROM price_snapshots WHERE holdingId = :holdingId AND accountId = :accountId AND date BETWEEN :start AND :end ORDER BY date ASC")
    suspend fun getByDateRange(holdingId: Long, start: LocalDate, end: LocalDate, accountId: Long): List<PriceSnapshot>

    @Query("SELECT * FROM price_snapshots WHERE holdingId = :holdingId AND date = :date AND accountId = :accountId LIMIT 1")
    suspend fun getByHoldingAndDate(holdingId: Long, date: LocalDate, accountId: Long): PriceSnapshot?

    @Query("SELECT * FROM price_snapshots WHERE holdingId = :holdingId AND accountId = :accountId ORDER BY date DESC LIMIT 1")
    suspend fun getLatest(holdingId: Long, accountId: Long): PriceSnapshot?

    @Query("SELECT * FROM price_snapshots WHERE holdingId = :holdingId AND accountId = :accountId ORDER BY date DESC LIMIT 1")
    fun observeLatest(holdingId: Long, accountId: Long): Flow<PriceSnapshot?>

    @Query("SELECT * FROM price_snapshots WHERE holdingId = :holdingId AND accountId = :accountId ORDER BY date DESC LIMIT 1 OFFSET 1")
    suspend fun getPrevious(holdingId: Long, accountId: Long): PriceSnapshot?

    @Query("SELECT * FROM price_snapshots WHERE holdingId = :holdingId AND accountId = :accountId AND date < :beforeDate ORDER BY date DESC LIMIT 1")
    suspend fun getLatestBefore(holdingId: Long, beforeDate: LocalDate, accountId: Long): PriceSnapshot?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(snapshot: PriceSnapshot)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(snapshots: List<PriceSnapshot>)

    @Query("DELETE FROM price_snapshots WHERE accountId = :accountId")
    suspend fun deleteByAccountId(accountId: Long)

    @Query("DELETE FROM price_snapshots WHERE holdingId = :holdingId AND accountId = :accountId")
    suspend fun deleteByHoldingId(holdingId: Long, accountId: Long)

    @Query("SELECT * FROM price_snapshots WHERE accountId = :accountId ORDER BY date ASC")
    suspend fun getAllList(accountId: Long): List<PriceSnapshot>

    @Query("SELECT COUNT(*) FROM price_snapshots WHERE accountId = :accountId")
    fun observeCount(accountId: Long): Flow<Int>
}
