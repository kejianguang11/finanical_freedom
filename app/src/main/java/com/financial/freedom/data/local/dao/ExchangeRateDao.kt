package com.financial.freedom.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.financial.freedom.data.local.entity.ExchangeRate
import kotlinx.datetime.LocalDate

@Dao
interface ExchangeRateDao {
    @Query("SELECT * FROM exchange_rates WHERE fromCurrency = :from AND toCurrency = :to AND date = :date LIMIT 1")
    suspend fun getRate(from: String, to: String, date: LocalDate): ExchangeRate?

    @Query("SELECT * FROM exchange_rates WHERE date = (SELECT MAX(date) FROM exchange_rates)")
    suspend fun getLatestRates(): List<ExchangeRate>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(rate: ExchangeRate)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(rates: List<ExchangeRate>)

    @Query("SELECT * FROM exchange_rates ORDER BY date ASC")
    suspend fun getAllList(): List<ExchangeRate>

    @Query("DELETE FROM exchange_rates")
    suspend fun deleteAll()
}
