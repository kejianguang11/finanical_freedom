package com.financial.freedom.data.remote

import com.financial.freedom.data.local.entity.ExchangeRate
import kotlinx.datetime.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 统一价格/汇率服务入口
 */
@Singleton
class PriceService @Inject constructor(
    private val aStockProvider: AStockProvider,
    private val usStockProvider: USStockProvider,
    private val cnFundProvider: CNFundProvider,
    private val goldProvider: GoldProvider,
    private val exchangeRateProvider: ExchangeRateProvider
) {
    private fun providerFor(type: String, market: String): PriceProvider = when (type) {
        "STOCK" -> if (market == "US" || market == "HK") usStockProvider else aStockProvider
        "FUND" -> cnFundProvider
        "GOLD" -> goldProvider
        else -> aStockProvider
    }

    suspend fun fetchPrice(type: String, symbol: String, market: String, date: LocalDate): PriceResult? =
        providerFor(type, market).fetchPrice(symbol, date)

    suspend fun fetchHistory(
        type: String,
        symbol: String,
        market: String,
        start: LocalDate,
        end: LocalDate
    ): List<PriceResult> = providerFor(type, market).fetchHistory(symbol, start, end)

    suspend fun searchAll(query: String): List<SearchResult> =
        aStockProvider.search(query) +
            usStockProvider.search(query) +
            cnFundProvider.search(query) +
            goldProvider.search(query)

    /** 拉取单个汇率 */
    suspend fun fetchExchangeRate(from: String, to: String, date: LocalDate): ExchangeRate? =
        exchangeRateProvider.fetchRate(from, to, date)

    /** 拉取最新汇率 */
    suspend fun fetchLatestRate(from: String, to: String): ExchangeRate? =
        exchangeRateProvider.fetchLatest(from, to)

    /** 批量拉取历史汇率 */
    suspend fun fetchExchangeRateHistory(
        from: String,
        to: String,
        start: LocalDate,
        end: LocalDate
    ): List<ExchangeRate> = exchangeRateProvider.fetchRateHistory(from, to, start, end)
}
