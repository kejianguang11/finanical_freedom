package com.financial.freedom.data.repository

import com.financial.freedom.data.local.dao.ExchangeRateDao
import com.financial.freedom.data.local.entity.ExchangeRate
import kotlinx.datetime.LocalDate
import java.math.BigDecimal
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ExchangeRateRepository @Inject constructor(
    private val dao: ExchangeRateDao
) {
    suspend fun getRate(from: String, to: String, date: LocalDate): BigDecimal? =
        dao.getRate(from, to, date)?.rate

    suspend fun getLatestRates(): List<ExchangeRate> = dao.getLatestRates()

    suspend fun saveRates(rates: List<ExchangeRate>) = dao.insertAll(rates)
}
