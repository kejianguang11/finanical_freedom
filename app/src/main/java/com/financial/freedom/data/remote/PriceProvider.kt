package com.financial.freedom.data.remote

import kotlinx.datetime.LocalDate

interface PriceProvider {
    val assetType: String
    suspend fun fetchPrice(symbol: String, date: LocalDate): PriceResult?
    suspend fun fetchHistory(symbol: String, start: LocalDate, end: LocalDate): List<PriceResult>
    suspend fun search(query: String): List<SearchResult>
}
