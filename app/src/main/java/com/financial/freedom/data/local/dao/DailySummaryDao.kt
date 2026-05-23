package com.financial.freedom.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.financial.freedom.data.local.entity.DailySummary
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.LocalDate
import java.math.BigDecimal

@Dao
interface DailySummaryDao {
    @Query("SELECT * FROM daily_summaries WHERE accountId = :accountId AND date BETWEEN :start AND :end ORDER BY date ASC")
    fun getByDateRange(start: LocalDate, end: LocalDate, accountId: Long): Flow<List<DailySummary>>

    @Query("SELECT * FROM daily_summaries WHERE accountId = :accountId AND date BETWEEN :start AND :end ORDER BY date ASC")
    suspend fun getListByDateRange(start: LocalDate, end: LocalDate, accountId: Long): List<DailySummary>

    @Query("SELECT * FROM daily_summaries WHERE date = :date AND accountId = :accountId")
    suspend fun getByDate(date: LocalDate, accountId: Long): DailySummary?

    @Query("SELECT MAX(date) FROM daily_summaries WHERE accountId = :accountId")
    suspend fun getLatestDate(accountId: Long): LocalDate?

    @Query("SELECT MIN(date) FROM daily_summaries WHERE accountId = :accountId")
    suspend fun getEarliestDate(accountId: Long): LocalDate?

    @Query("SELECT COUNT(*) FROM daily_summaries WHERE accountId = :accountId")
    suspend fun countByAccountId(accountId: Long): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(summary: DailySummary)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(summaries: List<DailySummary>)

    @Query("DELETE FROM daily_summaries WHERE accountId = :accountId")
    suspend fun deleteByAccountId(accountId: Long)

    @Query("DELETE FROM daily_summaries WHERE accountId = :accountId AND date >= :fromDate")
    suspend fun deleteFromDate(fromDate: LocalDate, accountId: Long)

    @Query("SELECT COALESCE(SUM(dayChange), 0) FROM daily_summaries WHERE accountId = :accountId AND date > :afterDate AND date <= :asOfDate")
    suspend fun getCumulativeDayChange(accountId: Long, afterDate: LocalDate, asOfDate: LocalDate): BigDecimal
}
