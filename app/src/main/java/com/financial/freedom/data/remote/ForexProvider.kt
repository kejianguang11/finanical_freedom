package com.financial.freedom.data.remote

import android.util.Log
import com.financial.freedom.data.local.entity.ExchangeRate
import kotlinx.datetime.LocalDate
import okhttp3.OkHttpClient
import okhttp3.Request
import java.math.BigDecimal
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 新浪财经外汇汇率 Provider，国内可访问。
 *
 * API: hq.sinajs.cn/list=fx_susdcny
 * 返回格式: var hq_str_fx_susdcny="time,open,prev_close,current,..."
 * field[3] = 最新价
 */
@Singleton
class ForexProvider @Inject constructor(
    private val okHttpClient: OkHttpClient
) {
    companion object {
        private const val TAG = "ForexProvider"
        private const val BASE_URL = "https://hq.sinajs.cn/list="

        // 货币 → 新浪 forex 代码
        val CURRENCY_TO_SINA = mapOf(
            "USD" to "fx_susdcny",
            "HKD" to "fx_shkdcny",
            "JPY" to "fx_sjpycny",
            "EUR" to "fx_seurcny",
            "GBP" to "fx_sgbpcny"
        )
    }

    suspend fun fetchLatest(from: String, to: String, date: LocalDate): ExchangeRate? {
        if (to != "CNY") return null
        val sinaCode = CURRENCY_TO_SINA[from] ?: return null

        return try {
            val url = "$BASE_URL$sinaCode"
            Log.d(TAG, "Fetching forex: $from/$to -> $url")
            val request = Request.Builder()
                .url(url)
                .addHeader("Referer", "https://finance.sina.com.cn")
                .get().build()
            val response = okHttpClient.newCall(request).execute()
            val body = response.body?.string() ?: return null

            // 解析: var hq_str_fx_susdcny="...,current,..."
            val values = body.substringAfter("\"").substringBefore("\"").split(",")
            if (values.size < 4) {
                Log.w(TAG, "Forex $from/$to: unexpected format, size=${values.size}")
                return null
            }
            val rate = BigDecimal(values[3])

            Log.d(TAG, "Forex $from/$to: rate=$rate (date=$date)")

            ExchangeRate(
                fromCurrency = from,
                toCurrency = to,
                date = date,
                rate = rate
            )
        } catch (e: Exception) {
            Log.w(TAG, "Forex $from/$to fetch FAILED: ${e.message}")
            null
        }
    }
}
