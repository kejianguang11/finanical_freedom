package com.financial.freedom.data.remote

import android.util.Log
import kotlinx.datetime.LocalDate
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

    override suspend fun fetchHistory(symbol: String, start: LocalDate, end: LocalDate): List<PriceResult> {
        // 港股历史数据暂不支持，使用当前价格作为近似
        return emptyList()
    }

    override suspend fun search(query: String): List<SearchResult> {
        // 港股搜索暂不支持，返回空列表
        return emptyList()
    }
}
