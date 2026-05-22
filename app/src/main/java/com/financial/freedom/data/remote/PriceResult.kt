package com.financial.freedom.data.remote

import kotlinx.datetime.LocalDate
import java.math.BigDecimal

data class PriceResult(
    val symbol: String,
    val price: BigDecimal,
    val currency: String,
    val date: LocalDate,
    val name: String = ""
)

data class SearchResult(
    val symbol: String,
    val name: String,
    val market: String,
    val type: String      // STOCK / FUND
)
