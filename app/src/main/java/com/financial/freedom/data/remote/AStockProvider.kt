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

@Singleton
class AStockProvider @Inject constructor(
    private val okHttpClient: OkHttpClient
) : PriceProvider {

    override val assetType = "STOCK"
    private val json = Json { ignoreUnknownKeys = true }

    companion object {
        private const val TAG = "AStockProvider"
    }

    /**
     * 获取 A 股实时价格（新浪财经）
     * http://hq.sinajs.cn/list=sh600519
     * 返回格式: var hq_str_sh600519="茅台,1720.00,1715.00,..."
     */
    override suspend fun fetchPrice(symbol: String, date: LocalDate): PriceResult? {
        return try {
            val sinaCode = toSinaCode(symbol)
            val url = "https://hq.sinajs.cn/list=$sinaCode"
            Log.d(TAG, "Fetching A-stock price: $symbol -> $url")
            val request = Request.Builder()
                .url(url)
                .addHeader("Referer", "https://finance.sina.com.cn")
                .get().build()
            val response = okHttpClient.newCall(request).execute()
            val body = response.body?.string() ?: return null

            // 解析 var hq_str_xxx="name,open,close,current,..."
            val values = body.substringAfter("\"").substringBefore("\"").split(",")
            if (values.size < 4) {
                Log.w(TAG, "A-stock $symbol: unexpected response format, values.size=${values.size}")
                return null
            }

            val name = values[0]
            val price = BigDecimal(values[3])
            Log.d(TAG, "A-stock $symbol ($name): price=$price CNY")

            PriceResult(symbol = symbol, price = price, currency = "CNY", date = date, name = name)
        } catch (e: Exception) {
            Log.w(TAG, "A-stock $symbol fetch FAILED: ${e.message}")
            null
        }
    }

    /**
     * 获取 A 股历史日线（东方财富）
     * https://push2his.eastmoney.com/api/qt/stock/kline/get?secid=1.600519&fields1=f1&fields2=f51&klt=101&beg=20260501&end=20260521
     */
    override suspend fun fetchHistory(symbol: String, start: LocalDate, end: LocalDate): List<PriceResult> {
        return try {
            val emCode = toEastMoneyCode(symbol)
            val beg = start.toString().replace("-", "")
            val endStr = end.toString().replace("-", "")
            val url = "https://push2his.eastmoney.com/api/qt/stock/kline/get" +
                "?secid=$emCode&fields1=f1&fields2=f51,f52&klt=101&beg=$beg&end=$endStr"

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
                // 格式: "2026-05-21,1720.00,1725.00,1715.00,1720.00,100000,172000000.00"
                val parts = line.split(",")
                if (parts.size < 3) return@mapNotNull null
                val date = LocalDate.parse(parts[0])
                val close = BigDecimal(parts[2]) // 收盘价第3列
                PriceResult(symbol = symbol, price = close, currency = "CNY", date = date)
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    /**
     * 搜索 A 股（东方财富）
     * https://searchadapter.eastmoney.com/api/suggest/get?input=茅台&type=14
     */
    override suspend fun search(query: String): List<SearchResult> {
        return try {
            val url = "https://searchadapter.eastmoney.com/api/suggest/get?input=$query&type=14&token=d"
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
                SearchResult(
                    symbol = obj["Code"]?.jsonPrimitive?.content ?: return@mapNotNull null,
                    name = obj["Name"]?.jsonPrimitive?.content ?: "",
                    market = if (obj["Market"]?.jsonPrimitive?.content == "1") "CN" else "CN",
                    type = "STOCK"
                )
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    /** 600519 → sh600519 */
    private fun toSinaCode(symbol: String): String {
        val numeric = symbol.filter { it.isDigit() }
        return when {
            numeric.startsWith("6") -> "sh$numeric"
            numeric.startsWith("0") || numeric.startsWith("3") -> "sz$numeric"
            else -> symbol
        }
    }

    /** 600519 → 1.600519 (上交所: 1, 深交所: 0) */
    private fun toEastMoneyCode(symbol: String): String {
        val numeric = symbol.filter { it.isDigit() }
        val market = if (numeric.startsWith("6")) "1" else "0"
        return "$market.$numeric"
    }
}
