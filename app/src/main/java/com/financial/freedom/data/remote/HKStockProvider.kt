package com.financial.freedom.data.remote

import android.util.Log
import kotlinx.datetime.LocalDate
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.OkHttpClient
import okhttp3.Request
import java.math.BigDecimal
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 港股实时行情（新浪财经）
 * https://hq.sinajs.cn/list=rt_hk00700
 * 格式: "英文名,中文名,今开,昨收,最高,最低,最新价,涨跌额,涨跌幅,买价,卖价,..."
 */
@Singleton
class HKStockProvider @Inject constructor(
    private val okHttpClient: OkHttpClient
) : PriceProvider {

    override val assetType = "STOCK"
    private val json = Json { ignoreUnknownKeys = true }

    companion object {
        private const val TAG = "HKStockProvider"
    }

    override suspend fun fetchPrice(symbol: String, date: LocalDate): PriceResult? {
        return try {
            val url = "https://hq.sinajs.cn/list=rt_hk$symbol"
            Log.d(TAG, "Fetching HK stock: $symbol -> $url")
            val request = Request.Builder()
                .url(url)
                .addHeader("Referer", "https://finance.sina.com.cn")
                .get().build()
            val response = okHttpClient.newCall(request).execute()
            val body = response.body?.string() ?: return null

            val values = body.substringAfter("\"").substringBefore("\"").split(",")
            if (values.size < 7) {
                Log.w(TAG, "HK $symbol: unexpected format, size=${values.size}")
                return null
            }

            val name = values[1]  // 中文名
            val price = BigDecimal(values[6])  // 最新价
            Log.d(TAG, "HK $symbol ($name): HKD $price")

            PriceResult(symbol = symbol, price = price, currency = "HKD", date = date, name = name)
        } catch (e: Exception) {
            Log.w(TAG, "HK $symbol fetch FAILED: ${e.message}")
            null
        }
    }

    /**
     * 港股历史日线（东方财富）
     * https://push2his.eastmoney.com/api/qt/stock/kline/get?secid=116.00700&fields1=f1&fields2=f51,f52&klt=101
     * 港股 secid 格式：116.xxxxx
     */
    override suspend fun fetchHistory(symbol: String, start: LocalDate, end: LocalDate): List<PriceResult> {
        return try {
            val emCode = "116.$symbol"
            val beg = start.toString().replace("-", "")
            val endStr = end.toString().replace("-", "")
            val url = "https://push2his.eastmoney.com/api/qt/stock/kline/get" +
                "?secid=$emCode&fields1=f1&fields2=f51,f52&klt=101&beg=$beg&end=$endStr"

            Log.d(TAG, "Fetching HK history: $symbol -> $url")
            val request = Request.Builder()
                .url(url)
                .addHeader("Referer", "https://quote.eastmoney.com")
                .get().build()
            val response = okHttpClient.newCall(request).execute()
            val body = response.body?.string() ?: return emptyList()

            val root = json.parseToJsonElement(body).jsonObject
            val data = root["data"]?.jsonObject ?: return emptyList()
            val klines = data["klines"]?.jsonArray ?: return emptyList()

            klines.mapNotNull { element ->
                val line = element.jsonPrimitive.content
                val parts = line.split(",")
                if (parts.size < 3) return@mapNotNull null
                val date = LocalDate.parse(parts[0])
                val close = BigDecimal(parts[2])
                PriceResult(symbol = symbol, price = close, currency = "HKD", date = date)
            }
        } catch (e: Exception) {
            Log.w(TAG, "HK $symbol history fetch FAILED: ${e.message}")
            emptyList()
        }
    }

    override suspend fun search(query: String): List<SearchResult> {
        return try {
            val url = "https://searchadapter.eastmoney.com/api/suggest/get?input=$query&type=116&token=d"
            val request = Request.Builder()
                .url(url)
                .addHeader("Referer", "https://quote.eastmoney.com")
                .get().build()
            val response = okHttpClient.newCall(request).execute()
            val body = response.body?.string() ?: return emptyList()

            val root = json.parseToJsonElement(body).jsonObject
            val queries = root["QuotationCodeTable"]?.jsonObject
            val data = queries?.get("Data")?.jsonArray ?: return emptyList()

            data.mapNotNull { item ->
                val obj = item.jsonObject
                val market = obj["Market"]?.jsonPrimitive?.content ?: return@mapNotNull null
                // 港股市场代码：116（东方财富港股标识）
                if (market != "116") return@mapNotNull null
                SearchResult(
                    symbol = obj["Code"]?.jsonPrimitive?.content ?: return@mapNotNull null,
                    name = obj["Name"]?.jsonPrimitive?.content ?: "",
                    market = "HK",
                    type = "STOCK"
                )
            }
        } catch (e: Exception) {
            Log.w(TAG, "HK search FAILED: ${e.message}")
            emptyList()
        }
    }
}
