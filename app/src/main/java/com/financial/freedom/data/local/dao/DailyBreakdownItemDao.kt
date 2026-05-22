package com.financial.freedom.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.financial.freedom.data.local.entity.DailyBreakdownItem
import kotlinx.datetime.LocalDate

@Dao
interface DailyBreakdownItemDao {
    @Query("SELECT * FROM daily_breakdown_items WHERE date = :date AND accountId = :accountId")
    suspend fun getByDate(date: LocalDate, accountId: Long): List<DailyBreakdownItem>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<DailyBreakdownItem>)

    @Query("DELETE FROM daily_breakdown_items WHERE accountId = :accountId")
    suspend fun deleteByAccountId(accountId: Long)

    @Query("DELETE FROM daily_breakdown_items WHERE date = :date AND accountId = :accountId")
    suspend fun deleteByDate(date: LocalDate, accountId: Long)

    @Query("DELETE FROM daily_breakdown_items WHERE accountId = :accountId AND date >= :fromDate")
    suspend fun deleteFromDate(fromDate: LocalDate, accountId: Long)
}
