package com.financial.freedom.data.remote

import android.util.Log
import com.financial.freedom.data.local.entity.ExchangeRate
import kotlinx.datetime.LocalDate
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.double
import okhttp3.OkHttpClient
import okhttp3.Request
import java.math.BigDecimal
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ExchangeRateProvider @Inject constructor(
    private val okHttpClient: OkHttpClient
) {
    private val json = Json { ignoreUnknownKeys = true }
    private val baseUrl = "https://api.frankfurter.app"

    companion object {
        private const val TAG = "ExchangeRateProvider"
    }

    /**
     * 拉取某日汇率
     * GET https://api.frankfurter.app/2026-05-21?from=USD&to=CNY
     */
    suspend fun fetchRate(from: String, to: String, date: LocalDate): ExchangeRate? {
        return try {
            val url = "$baseUrl/$date?from=$from&to=$to"
            val request = Request.Builder().url(url).get().build()
            val response = okHttpClient.newCall(request).execute()
            val body = response.body?.string() ?: return null
            val root = json.parseToJsonElement(body).jsonObject
            val rates = root["rates"]?.jsonObject ?: return null
            val rate = rates[to]?.jsonPrimitive?.double ?: return null
            ExchangeRate(fromCurrency = from, toCurrency = to, date = date, rate = BigDecimal.valueOf(rate))
        } catch (e: Exception) {
            null
        }
    }

    /**
     * 拉取最新汇率
     * GET https://api.frankfurter.app/latest?from=USD&to=CNY
     */
    suspend fun fetchLatest(from: String, to: String): ExchangeRate? {
        return try {
            val url = "$baseUrl/latest?from=$from&to=$to"
            Log.d(TAG, "Fetching latest rate: $from -> $to")
            val request = Request.Builder().url(url).get().build()
            val response = okHttpClient.newCall(request).execute()
            val body = response.body?.string() ?: return null
            val root = json.parseToJsonElement(body).jsonObject
            val date = root["date"]?.jsonPrimitive?.content?.let { LocalDate.parse(it) } ?: return null
            val rates = root["rates"]?.jsonObject ?: return null
            val rate = rates[to]?.jsonPrimitive?.double ?: return null
            Log.d(TAG, "Latest rate $from->$to: $rate (date=$date)")
            ExchangeRate(fromCurrency = from, toCurrency = to, date = date, rate = BigDecimal.valueOf(rate))
        } catch (e: Exception) {
            Log.w(TAG, "Latest rate $from->$to fetch FAILED: ${e.message}")
            null
        }
    }

    /**
     * 批量拉取一段日期的汇率
     * GET https://api.frankfurter.app/2026-05-01..2026-05-21?from=USD&to=CNY
     */
    suspend fun fetchRateHistory(
        from: String,
        to: String,
        start: LocalDate,
        end: LocalDate
    ): List<ExchangeRate> {
        return try {
            val url = "$baseUrl/$start..$end?from=$from&to=$to"
            val request = Request.Builder().url(url).get().build()
            val response = okHttpClient.newCall(request).execute()
            val body = response.body?.string() ?: return emptyList()
            val root = json.parseToJsonElement(body).jsonObject
            val ratesObj = root["rates"]?.jsonObject ?: return emptyList()

            ratesObj.entries.mapNotNull { (dateStr, rateObj) ->
                val rate = rateObj.jsonObject[to]?.jsonPrimitive?.double ?: return@mapNotNull null
                val date = LocalDate.parse(dateStr)
                ExchangeRate(fromCurrency = from, toCurrency = to, date = date, rate = BigDecimal.valueOf(rate))
            }
        } catch (e: Exception) {
            emptyList()
        }
    }
}
