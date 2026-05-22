package com.financial.freedom.data.remote

import android.util.Log
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.double
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.long
import okhttp3.OkHttpClient
import okhttp3.Request
import java.math.BigDecimal
import java.math.RoundingMode
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class USStockProvider @Inject constructor(
    private val okHttpClient: OkHttpClient
) : PriceProvider {

    override val assetType = "STOCK"
    private val json = Json { ignoreUnknownKeys = true }

    companion object {
        private const val TAG = "USStockProvider"
        // Finnhub 免费 API Key（60次/分钟），可在 https://finnhub.io/register 获取
        private const val API_KEY = "cnl5e2hr01qvfkpp31lgcnl5e2hr01qvfkpp31m0"
        private const val BASE_URL = "https://finnhub.io/api/v1"
    }

    override suspend fun fetchPrice(symbol: String, date: LocalDate): PriceResult? {
        return try {
            val url = "$BASE_URL/quote?symbol=$symbol&token=$API_KEY"
            Log.d(TAG, "Fetching US stock price: $symbol -> $BASE_URL/quote?symbol=$symbol")
            val request = Request.Builder().url(url).get().build()
            val response = okHttpClient.newCall(request).execute()
            val body = response.body?.string() ?: return null
            Log.d(TAG, "US stock $symbol response: ${body.take(200)}")

            val root = json.parseToJsonElement(body).jsonObject
            val currentPrice = root["c"]?.jsonPrimitive?.double ?: return null
            Log.d(TAG, "US stock $symbol: price=$currentPrice USD")

            PriceResult(
                symbol = symbol,
                price = BigDecimal.valueOf(currentPrice).setScale(4, RoundingMode.HALF_UP),
                currency = "USD",
                date = date
            )
        } catch (e: Exception) {
            Log.w(TAG, "US stock $symbol fetch FAILED: ${e.message}")
            null
        }
    }

    override suspend fun fetchHistory(symbol: String, start: LocalDate, end: LocalDate): List<PriceResult> {
        return try {
            val startInstant = Instant.parse("${start}T00:00:00Z")
            val endInstant = Instant.parse("${end}T00:00:00Z")
            val fromEpoch = startInstant.toEpochMilliseconds() / 1000
            val toEpoch = endInstant.toEpochMilliseconds() / 1000

            val url = "$BASE_URL/stock/candle?symbol=$symbol&resolution=D&from=$fromEpoch&to=$toEpoch&token=$API_KEY"
            val request = Request.Builder().url(url).get().build()
            val response = okHttpClient.newCall(request).execute()
            val body = response.body?.string() ?: return emptyList()

            val root = json.parseToJsonElement(body).jsonObject
            val status = root["s"]?.jsonPrimitive?.content ?: return emptyList()
            if (status != "ok") return emptyList()

            val timestamps = root["t"]?.jsonArray ?: return emptyList()
            val closes = root["c"]?.jsonArray ?: return emptyList()

            timestamps.indices.mapNotNull { i ->
                val epochSeconds = timestamps[i].jsonPrimitive.long
                val close = closes[i].jsonPrimitive.double
                val date = Instant.fromEpochSeconds(epochSeconds)
                    .toLocalDateTime(TimeZone.UTC).date
                if (date < start || date > end) return@mapNotNull null
                PriceResult(
                    symbol = symbol,
                    price = BigDecimal.valueOf(close).setScale(4, RoundingMode.HALF_UP),
                    currency = "USD",
                    date = date
                )
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    override suspend fun search(query: String): List<SearchResult> {
        return try {
            val url = "$BASE_URL/search?q=$query&token=$API_KEY"
            val request = Request.Builder().url(url).get().build()
            val response = okHttpClient.newCall(request).execute()
            val body = response.body?.string() ?: return emptyList()

            val root = json.parseToJsonElement(body).jsonObject
            val results = root["result"]?.jsonArray ?: return emptyList()

            results.take(10).mapNotNull { item ->
                val obj = item.jsonObject
                val symbol = obj["symbol"]?.jsonPrimitive?.content ?: return@mapNotNull null
                val name = obj["description"]?.jsonPrimitive?.content ?: ""
                SearchResult(
                    symbol = symbol,
                    name = name,
                    market = "US",
                    type = "STOCK"
                )
            }
        } catch (e: Exception) {
            emptyList()
        }
    }
}
