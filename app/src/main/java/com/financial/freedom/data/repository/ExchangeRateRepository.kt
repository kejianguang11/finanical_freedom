package com.financial.freedom.data.repository

import android.util.Log
import com.financial.freedom.data.local.dao.ExchangeRateDao
import com.financial.freedom.data.local.entity.ExchangeRate
import com.financial.freedom.data.remote.ExchangeRateProvider
import com.financial.freedom.data.remote.ForexProvider
import kotlinx.datetime.Clock
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn
import java.math.BigDecimal
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ExchangeRateRepository @Inject constructor(
    private val dao: ExchangeRateDao,
    private val provider: ExchangeRateProvider,
    private val forexProvider: ForexProvider
) {
    companion object {
        private const val TAG = "ExchangeRateRepo"
        private const val MAX_STALENESS_DAYS = 2
    }

    /** Rate freshness level */
    enum class Freshness { FRESH, STALE, UNKNOWN }

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

    /**
     * Check if the latest cached rate for a pair is fresh enough.
     * @return Freshness level and the date of the cached rate (if any).
     */
    suspend fun checkFreshness(from: String, to: String): Pair<Freshness, LocalDate?> {
        val latest = dao.getLatestRates().firstOrNull { it.fromCurrency == from && it.toCurrency == to }
            ?: return Freshness.UNKNOWN to null
        val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
        val daysDiff = today.toEpochDays() - latest.date.toEpochDays()
        return if (daysDiff <= MAX_STALENESS_DAYS) Freshness.FRESH to latest.date
        else Freshness.STALE to latest.date
    }

    suspend fun getLatestRates(): List<ExchangeRate> = dao.getLatestRates()

    suspend fun saveRates(rates: List<ExchangeRate>) = dao.insertAll(rates)

    suspend fun refreshAllRates() {
        val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
        val pairs = listOf("USD" to "CNY", "HKD" to "CNY")
        var anySuccess = false
        for ((from, to) in pairs) {
            val ok = fetchAndCache(from, to, today)
            if (ok != null) anySuccess = true
        }
        if (!anySuccess) {
            Log.w(TAG, "All rate fetches failed — cached data may be stale")
        }
    }

    private suspend fun fetchAndCache(from: String, to: String, date: LocalDate): BigDecimal? {
        // Try ForexProvider (East Money, China-accessible) first
        try {
            val forexRate = forexProvider.fetchLatest(from, to, date)
            if (forexRate != null) {
                dao.insert(forexRate)
                Log.d(TAG, "ForexProvider OK: $from→$to = ${forexRate.rate} (date=${forexRate.date})")
                return forexRate.rate
            }
        } catch (e: Exception) {
            Log.w(TAG, "ForexProvider $from→$to FAILED: ${e.message}")
        }

        // Fallback to frankfurter.app
        try {
            val rate = provider.fetchLatest(from, to)
            if (rate != null) {
                dao.insert(rate)
                Log.d(TAG, "frankfurter OK: $from→$to = ${rate.rate} (date=${rate.date})")
                return rate.rate
            }
        } catch (e: Exception) {
            Log.w(TAG, "frankfurter $from→$to FAILED: ${e.message}")
        }

        Log.w(TAG, "All providers failed for $from→$to")
        return null
    }
}
