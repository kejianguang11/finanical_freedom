package com.financial.freedom.data.repository

import android.util.Log
import com.financial.freedom.data.local.dao.ExchangeRateDao
import com.financial.freedom.data.local.entity.ExchangeRate
import com.financial.freedom.data.remote.ExchangeRateProvider
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn
import java.math.BigDecimal
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ExchangeRateRepository @Inject constructor(
    private val dao: ExchangeRateDao,
    private val provider: ExchangeRateProvider
) {
    companion object {
        private const val TAG = "ExchangeRateRepo"
    }

    suspend fun getRate(from: String, to: String, date: LocalDate): BigDecimal? {
        if (from == to) return BigDecimal.ONE

        // 1. Exact date match
        val exact = dao.getRate(from, to, date)?.rate
        if (exact != null) return exact

        // 2. Try to fetch from API and cache
        val fetched = fetchAndCache(from, to, date)
        if (fetched != null) return fetched

        // 3. Fallback to latest cached rate
        val latest = dao.getLatestRates().firstOrNull { it.fromCurrency == from && it.toCurrency == to }
        return latest?.rate
    }

    suspend fun getLatestRates(): List<ExchangeRate> = dao.getLatestRates()

    suspend fun saveRates(rates: List<ExchangeRate>) = dao.insertAll(rates)

    /**
     * Fetch latest rates for common currency pairs and cache them.
     * Called from Settings to refresh rate data.
     */
    suspend fun refreshAllRates() {
        val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
        val pairs = listOf("USD" to "CNY", "HKD" to "CNY")
        for ((from, to) in pairs) {
            fetchAndCache(from, to, today)
        }
    }

    private suspend fun fetchAndCache(from: String, to: String, date: LocalDate): BigDecimal? {
        return try {
            val rate = provider.fetchLatest(from, to)
            if (rate != null) {
                dao.insert(rate)
                Log.d(TAG, "Fetched and cached $from→$to: ${rate.rate} (date=${rate.date})")
                rate.rate
            } else {
                Log.w(TAG, "Failed to fetch $from→$to from API")
                null
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error fetching $from→$to: ${e.message}")
            null
        }
    }
}
