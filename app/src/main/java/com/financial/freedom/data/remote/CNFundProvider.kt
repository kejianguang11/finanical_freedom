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
class CNFundProvider @Inject constructor(
    private val okHttpClient: OkHttpClient
) : PriceProvider {

    override val assetType = "FUND"
    private val json = Json { ignoreUnknownKeys = true }

    companion object {
        private const val TAG = "CNFundProvider"
    }

    /**
     * 获取基金实时估值（天天基金）
     * https://fundgz.1234567.com.cn/js/005827.js
     * 返回: jsonpgz({"fundcode":"005827","name":"易方达蓝筹","jzrq":"2026-05-21","dwjz":"1.6800","gsz":"1.6850",...})
     */
    override suspend fun fetchPrice(symbol: String, date: LocalDate): PriceResult? {
        return try {
            val url = "https://fundgz.1234567.com.cn/js/$symbol.js"
            Log.d(TAG, "Fetching fund price: $symbol -> $url")
            val request = Request.Builder()
                .url(url)
                .addHeader("Referer", "https://fund.eastmoney.com")
                .get().build()
            val response = okHttpClient.newCall(request).execute()
            val body = response.body?.string() ?: return null

            // 去掉 jsonpgz(...); 包裹
            val jsonStr = body.removePrefix("jsonpgz(").removeSuffix(");")
            val root = json.parseToJsonElement(jsonStr).jsonObject

            val name = root["name"]?.jsonPrimitive?.content ?: ""
            // 优先用实时估值 gsz，否则用昨日净值 dwjz
            val priceStr = root["gsz"]?.jsonPrimitive?.content
                ?: root["dwjz"]?.jsonPrimitive?.content ?: return null
            val price = BigDecimal(priceStr)
            Log.d(TAG, "Fund $symbol ($name): gsz/dwjz=$priceStr CNY")

            PriceResult(symbol = symbol, price = price, currency = "CNY", date = date, name = name)
        } catch (e: Exception) {
            Log.w(TAG, "Fund $symbol fetch FAILED: ${e.message}")
            null
        }
    }

    /**
     * 获取基金历史净值（天天基金）
     * https://api.fund.eastmoney.com/f10/lsjz?fundCode=005827&pageIndex=1&pageSize=365
     */
    override suspend fun fetchHistory(symbol: String, start: LocalDate, end: LocalDate): List<PriceResult> {
        return try {
            val url = "https://api.fund.eastmoney.com/f10/lsjz?fundCode=$symbol&pageIndex=1&pageSize=365"
            val request = Request.Builder()
                .url(url)
                .addHeader("Referer", "https://fundf10.eastmoney.com")
                .get().build()
            val response = okHttpClient.newCall(request).execute()
            val body = response.body?.string() ?: return emptyList()

            val root = json.parseToJsonElement(body).jsonObject
            val data = root["Data"]?.jsonObject ?: return emptyList()
            val list = data["LSJZList"]?.jsonArray ?: return emptyList()

            list.mapNotNull { item ->
                val obj = item.jsonObject
                val dateStr = obj["FSRQ"]?.jsonPrimitive?.content ?: return@mapNotNull null
                val date = LocalDate.parse(dateStr)
                if (date < start || date > end) return@mapNotNull null
                val nav = obj["DWJZ"]?.jsonPrimitive?.content?.let { BigDecimal(it) } ?: return@mapNotNull null
                PriceResult(symbol = symbol, price = nav, currency = "CNY", date = date)
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    /**
     * 搜索基金（东方财富）
     * https://fundsuggest.eastmoney.com/fundsearch/search?key=易方达蓝筹
     */
    override suspend fun search(query: String): List<SearchResult> {
        return try {
            val url = "https://fundsuggest.eastmoney.com/fundsearch/search?key=$query"
            val request = Request.Builder()
                .url(url)
                .addHeader("Referer", "https://fund.eastmoney.com")
                .get().build()
            val response = okHttpClient.newCall(request).execute()
            val body = response.body?.string() ?: return emptyList()

            // 返回 JSONP: var result = [...]
            val jsonStr = body.removePrefix("var result = ")
            val root = json.parseToJsonElement(jsonStr).jsonObject
            val datas = root["Datas"]?.jsonArray ?: return emptyList()

            datas.take(10).mapNotNull { item ->
                val obj = item.jsonObject
                SearchResult(
                    symbol = obj["CODE"]?.jsonPrimitive?.content ?: return@mapNotNull null,
                    name = obj["NAME"]?.jsonPrimitive?.content ?: "",
                    market = "CN",
                    type = "FUND"
                )
            }
        } catch (e: Exception) {
            emptyList()
        }
    }
}
