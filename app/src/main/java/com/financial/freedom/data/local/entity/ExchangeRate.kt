package com.financial.freedom.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.datetime.LocalDate
import java.math.BigDecimal

@Entity(
    tableName = "exchange_rates",
    indices = [androidx.room.Index(value = ["fromCurrency", "toCurrency", "date"], unique = true)]
)
data class ExchangeRate(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val fromCurrency: String,
    val toCurrency: String,
    val date: LocalDate,
    val rate: BigDecimal
)
