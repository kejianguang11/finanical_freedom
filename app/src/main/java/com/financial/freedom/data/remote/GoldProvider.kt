package com.financial.freedom.data.remote

import android.util.Log
import com.financial.freedom.data.local.dao.ExchangeRateDao
import kotlinx.datetime.Clock
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.plus
import kotlinx.datetime.todayIn
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.OkHttpClient
import okhttp3.Request
import java.math.BigDecimal
import java.math.RoundingMode
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GoldProvider @Inject constructor(
    private val okHttpClient: OkHttpClient,
    private val exchangeRateDao: ExchangeRateDao
) : PriceProvider {

    override val assetType = "GOLD"

    /** 1 金衡盎司 = 31.1035 克 */
    private val gramsPerOunce = BigDecimal("31.1035")

    private val json = Json { ignoreUnknownKeys = true }

    companion object {
        private const val TAG = "GoldProvider"
    }

    /**
     * 获取国内金价（优先东方财富 AU9999，失败回退到国际金价 XAU）
     * AU9999 价格单位：元/克
     */
    override suspend fun fetchPrice(symbol: String, date: LocalDate): PriceResult? {
        // 优先用东方财富 AU9999 国内金价
        val domestic = fetchAU9999Price(date)
        if (domestic != null) return domestic

        // 降级：国际金价 XAU 换算
        return fetchXAUSinaPrice(date)
    }

    /**
     * 从东方财富获取 AU9999 国内金价（元/克）
     * https://push2.eastmoney.com/api/qt/stock/get?secid=118.AU9999&fields=f43,f57,f58
     */
    private suspend fun fetchAU9999Price(date: LocalDate): PriceResult? {
        return try {
            val url = "https://push2.eastmoney.com/api/qt/stock/get?secid=118.AU9999&fields=f43,f57,f58"
            Log.d(TAG, "Fetching AU9999: $url")
            val request = Request.Builder()
                .url(url)
                .addHeader("Referer", "https://quote.eastmoney.com")
                .get().build()
            val response = okHttpClient.newCall(request).execute()
            val body = response.body?.string() ?: return null

            val root = json.parseToJsonElement(body).jsonObject
            val data = root["data"]?.jsonObject ?: return null
            val priceRaw = data["f43"]?.jsonPrimitive?.content?.toDoubleOrNull() ?: return null
            val name = data["f58"]?.jsonPrimitive?.content ?: "上海金AU9999"

            // f43 返回的价格是实际价格 × 100（整数传输），除以 100 得到元/克
            val cnyPerGram = BigDecimal.valueOf(priceRaw / 100.0).setScale(4, RoundingMode.HALF_UP)

            Log.d(TAG, "AU9999: $name, raw=$priceRaw, CNY/g=$cnyPerGram")
            PriceResult(symbol = "AU9999", price = cnyPerGram, currency = "CNY", date = date, name = name)
        } catch (e: Exception) {
            Log.w(TAG, "AU9999 fetch FAILED: ${e.message}")
            null
        }
    }

    /**
     * 国际金价 XAU（新浪财经），降级方案
     * 返回美元/盎司，换算为 CNY/克
     */
    private suspend fun fetchXAUSinaPrice(date: LocalDate): PriceResult? {
        return try {
            val url = "https://hq.sinajs.cn/list=hf_XAU"
            Log.d(TAG, "Fallback to XAU: $url")
            val request = Request.Builder()
                .url(url)
                .addHeader("Referer", "https://finance.sina.com.cn")
                .get().build()
            val response = okHttpClient.newCall(request).execute()
            val body = response.body?.string() ?: return null

            val values = body.substringAfter("\"").substringBefore("\"").split(",")
            if (values.isEmpty() || values[0].isBlank()) {
                Log.w(TAG, "XAU: empty response")
                return null
            }

            // hf_XAU 返回格式: "最新价,昨收,开盘,最高,最低,..."
            // 最新价可能包含日期前缀，取纯数字部分
            val rawPrice = values[0].trim()
            val usdPerOunce = BigDecimal(rawPrice)

            // 获取 USD→CNY 汇率
            val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
            val rate = exchangeRateDao.getRate("USD", "CNY", date)
                ?: exchangeRateDao.getLatestRates()
                    .firstOrNull { it.fromCurrency == "USD" && it.toCurrency == "CNY" }

            val usdCnyRate = rate?.rate ?: BigDecimal("7.2")

            // USD/oz → USD/g → CNY/g
            val usdPerGram = usdPerOunce.divide(gramsPerOunce, 10, RoundingMode.HALF_UP)
            val cnyPerGram = usdPerGram.multiply(usdCnyRate).setScale(4, RoundingMode.HALF_UP)

            Log.d(TAG, "Gold XAU: USD/oz=$usdPerOunce, USD/CNY=$usdCnyRate, CNY/g=$cnyPerGram")

            PriceResult(symbol = "XAU", price = cnyPerGram, currency = "CNY", date = date, name = "国际黄金")
        } catch (e: Exception) {
            Log.w(TAG, "XAU fetch FAILED: ${e.message}")
            null
        }
    }

    override suspend fun fetchHistory(symbol: String, start: LocalDate, end: LocalDate): List<PriceResult> {
        // 优先用新浪 AU0 期货日K线（与 AU9999 价格高度一致，单位：元/克）
        val sina = fetchSinaAU0History(start, end)
        if (sina.isNotEmpty()) return sina

        // 降级：东方财富 K 线 API（AU9999 可能不被该接口支持）
        return fetchAU9999History(start, end)
    }

    /**
     * 从新浪财经获取 AU0（上期所黄金期货主力）日K线历史数据
     * AU0 与 AU9999 价格高度一致（套利维持），均为元/克
     */
    private suspend fun fetchSinaAU0History(start: LocalDate, end: LocalDate): List<PriceResult> {
        return try {
            val url = "https://stock2.finance.sina.com.cn/futures/api/jsonp.php" +
                "/var%20_AU0=/InnerFuturesNewService.getDailyKLine?symbol=AU0"
            Log.d(TAG, "Fetching Sina AU0 history: $url")

            val request = Request.Builder()
                .url(url)
                .addHeader("Referer", "https://finance.sina.com.cn")
                .addHeader("User-Agent", "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36")
                .get().build()
            val response = okHttpClient.newCall(request).execute()
            val body = response.body?.string() ?: return emptyList()
            Log.d(TAG, "Sina AU0 history response: ${body.take(200)}")

            // JSONP 格式: var _AU0=([{...},{...},...]);
            val jsonArrayStr = body
                .substringAfter("=(")
                .substringBeforeLast(");")
            if (jsonArrayStr.isBlank()) return emptyList()

            val jsonArray = json.parseToJsonElement(jsonArrayStr).jsonArray

            jsonArray.mapNotNull { element ->
                val obj = element.jsonObject
                val dateStr = obj["d"]?.jsonPrimitive?.content ?: return@mapNotNull null
                val date = LocalDate.parse(dateStr)
                if (date < start || date > end) return@mapNotNull null
                val closeStr = obj["c"]?.jsonPrimitive?.content ?: return@mapNotNull null
                val close = BigDecimal(closeStr)
                PriceResult(symbol = "AU9999", price = close, currency = "CNY", date = date, name = "上海金AU9999")
            }
        } catch (e: Exception) {
            Log.w(TAG, "Sina AU0 history fetch FAILED: ${e.message}", e)
            emptyList()
        }
    }

    /**
     * 从东方财富 K 线 API 获取 AU9999 历史价格（降级方案）
     * 注意：该接口可能对 AU9999 返回 data: null（rc: 102）
     */
    private suspend fun fetchAU9999History(start: LocalDate, end: LocalDate): List<PriceResult> {
        return try {
            val beg = start.toString().replace("-", "")
            val endStr = end.toString().replace("-", "")
            val url = "https://push2his.eastmoney.com/api/qt/stock/kline/get" +
                "?secid=118.AU9999&fields1=f1&fields2=f51,f52,f53&klt=101&beg=$beg&end=$endStr"

            Log.d(TAG, "Fetching AU9999 history (fallback): $url")
            val request = Request.Builder()
                .url(url)
                .addHeader("Referer", "https://quote.eastmoney.com")
                .addHeader("User-Agent", "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36")
                .get().build()
            val response = okHttpClient.newCall(request).execute()
            val body = response.body?.string() ?: return emptyList()
            Log.d(TAG, "AU9999 history response: ${body.take(300)}")

            val root = json.parseToJsonElement(body).jsonObject
            val data = root["data"]?.jsonObject ?: return emptyList()
            val klines = data["klines"]?.jsonArray ?: return emptyList()
            Log.d(TAG, "AU9999 klines count: ${klines.size}")

            klines.mapNotNull { element ->
                val line = element.jsonPrimitive.content
                val parts = line.split(",")
                if (parts.size < 3) return@mapNotNull null
                val date = LocalDate.parse(parts[0])
                val close = BigDecimal(parts[2])
                PriceResult(symbol = "AU9999", price = close, currency = "CNY", date = date, name = "上海金AU9999")
            }
        } catch (e: Exception) {
            Log.w(TAG, "AU9999 history fetch FAILED: ${e.message}", e)
            emptyList()
        }
    }

    override suspend fun search(query: String): List<SearchResult> {
        return listOf(
            SearchResult("AU9999", "上海金 AU9999 (元/克)", "CN", "GOLD"),
            SearchResult("XAU", "国际黄金 (美元/盎司)", "", "GOLD"),
            SearchResult("GOLD_ETF", "黄金 ETF", "CN", "GOLD")
        ).filter { it.name.contains(query, ignoreCase = true) || it.symbol.contains(query, ignoreCase = true) }
    }
}
