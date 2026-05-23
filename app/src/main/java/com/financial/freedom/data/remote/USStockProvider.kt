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

        // 本地美股英文名索引（东方财富 API 不支持英文名模糊搜索，客户端补齐）
        private val localIndex = listOf(
            "NTES" to "NetEase 网易",
            "BABA" to "Alibaba 阿里巴巴",
            "JD" to "JD.com 京东",
            "PDD" to "Pinduoduo 拼多多",
            "BIDU" to "Baidu 百度",
            "NIO" to "NIO 蔚来",
            "XPEV" to "XPeng 小鹏汽车",
            "LI" to "Li Auto 理想汽车",
            "TME" to "Tencent Music 腾讯音乐",
            "BILI" to "Bilibili 哔哩哔哩",
            "ZTO" to "ZTO Express 中通快递",
            "TAL" to "TAL Education 好未来",
            "EDU" to "New Oriental 新东方",
            "YUMC" to "Yum China 百胜中国",
            "BEKE" to "KE Holdings 贝壳找房",
            "DADA" to "Dada Nexus 达达",
            "VIPS" to "Vipshop 唯品会",
            "IQ" to "iQIYI 爱奇艺",
            "WB" to "Weibo 微博",
            "ATHM" to "Autohome 汽车之家",
            "AAPL" to "Apple 苹果",
            "GOOGL" to "Alphabet Google",
            "MSFT" to "Microsoft 微软",
            "AMZN" to "Amazon 亚马逊",
            "TSLA" to "Tesla 特斯拉",
            "META" to "Meta 脸书",
            "NVDA" to "NVIDIA 英伟达",
            "AMD" to "AMD 超威半导体",
            "INTC" to "Intel 英特尔",
            "NFLX" to "Netflix 奈飞",
        ).map { (symbol, fullName) ->
            LocalStock(symbol = symbol, fullName = fullName, nameLower = fullName.lowercase())
        }
    }

    private data class LocalStock(val symbol: String, val fullName: String, val nameLower: String)

    /**
     * 获取美股实时价格（新浪财经）
     * https://hq.sinajs.cn/list=gb_ntes
     * 格式: "英文名,最新价,涨跌幅,日期时间,涨跌额,今开,最高,最低,..."
     */
    override suspend fun fetchPrice(symbol: String, date: LocalDate): PriceResult? {
        return try {
            val url = "https://hq.sinajs.cn/list=gb_${symbol.lowercase()}"
            Log.d(TAG, "Fetching US stock: $symbol -> $url")
            val request = Request.Builder()
                .url(url)
                .addHeader("Referer", "https://finance.sina.com.cn")
                .get().build()
            val response = okHttpClient.newCall(request).execute()
            val body = response.body?.string() ?: return null

            val values = body.substringAfter("\"").substringBefore("\"").split(",")
            if (values.size < 2) {
                Log.w(TAG, "US $symbol: unexpected format, size=${values.size}")
                return null
            }

            val name = values[0]
            val price = BigDecimal(values[1])
            Log.d(TAG, "US $symbol ($name): USD $price")

            PriceResult(symbol = symbol, price = price.setScale(4, RoundingMode.HALF_UP), currency = "USD", date = date, name = name)
        } catch (e: Exception) {
            Log.w(TAG, "US $symbol fetch FAILED: ${e.message}")
            null
        }
    }

    /**
     * 获取美股历史日线（新浪财经）
     * https://stock.finance.sina.com.cn/usstock/api/json_v2.php/US_MinKService.getDailyK?symbol=NTES&scale=240&datalen=100
     */
    override suspend fun fetchHistory(symbol: String, start: LocalDate, end: LocalDate): List<PriceResult> {
        return try {
            val url = "https://stock.finance.sina.com.cn/usstock/api/json_v2.php/US_MinKService.getDailyK" +
                "?symbol=${symbol.uppercase()}&scale=240&datalen=200"
            Log.d(TAG, "Fetching US history: $symbol -> $url")
            val request = Request.Builder()
                .url(url)
                .addHeader("Referer", "https://finance.sina.com.cn")
                .get().build()
            val response = okHttpClient.newCall(request).execute()
            val body = response.body?.string() ?: return emptyList()

            val arr = json.parseToJsonElement(body).jsonArray
            arr.mapNotNull { element ->
                val obj = element.jsonObject
                val d = obj["d"]?.jsonPrimitive?.content ?: return@mapNotNull null
                val date = LocalDate.parse(d)
                if (date < start || date > end) return@mapNotNull null
                val close = obj["c"]?.jsonPrimitive?.content?.toBigDecimalOrNull() ?: return@mapNotNull null
                PriceResult(
                    symbol = symbol,
                    price = close.setScale(4, RoundingMode.HALF_UP),
                    currency = "USD",
                    date = date
                )
            }
        } catch (e: Exception) {
            Log.w(TAG, "US $symbol history fetch FAILED: ${e.message}")
            emptyList()
        }
    }

    /**
     * 搜索美股（东方财富）
     * https://searchadapter.eastmoney.com/api/suggest/get?input=NTES&type=14&token=d
     * 美股: SecurityType="7" / MarketType="7"
     */
    override suspend fun search(query: String): List<SearchResult> {
        val q = query.trim().lowercase()
        val results = mutableListOf<SearchResult>()

        // 1. 本地英文名索引匹配（前缀/包含）
        for (stock in localIndex) {
            if (q.length >= 2 && (stock.nameLower.contains(q) || stock.symbol.lowercase().startsWith(q))) {
                results.add(
                    SearchResult(
                        symbol = stock.symbol,
                        name = stock.fullName,
                        market = "US",
                        type = "STOCK"
                    )
                )
            }
        }

        // 2. 东方财富 API 搜索（补充中文名/代码结果）
        try {
            val url = "https://searchadapter.eastmoney.com/api/suggest/get?input=$query&type=14&token=d"
            val request = Request.Builder()
                .url(url)
                .addHeader("Referer", "https://quote.eastmoney.com")
                .get().build()
            val response = okHttpClient.newCall(request).execute()
            val body = response.body?.string() ?: ""

            val root = json.parseToJsonElement(body).jsonObject
            val queries = root["QuotationCodeTable"]?.jsonObject
            val data = queries?.get("Data")?.jsonArray

            if (data != null) {
                for (item in data) {
                    val obj = item.jsonObject
                    val securityType = obj["SecurityType"]?.jsonPrimitive?.content ?: continue
                    if (securityType != "7") continue
                    val symbol = obj["Code"]?.jsonPrimitive?.content ?: continue
                    val name = obj["Name"]?.jsonPrimitive?.content ?: ""
                    // 去重：避免本地索引已有同代码结果
                    if (results.none { it.symbol == symbol }) {
                        results.add(
                            SearchResult(
                                symbol = symbol,
                                name = name,
                                market = "US",
                                type = "STOCK"
                            )
                        )
                    }
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "US search FAILED: ${e.message}")
        }

        return results
    }
}
