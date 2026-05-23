package com.financial.freedom.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.datetime.LocalDate
import kotlinx.serialization.Serializable
import java.math.BigDecimal

@Entity(
    tableName = "exchange_rates",
    indices = [androidx.room.Index(value = ["fromCurrency", "toCurrency", "date"], unique = true)]
)
@Serializable
data class ExchangeRate(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val fromCurrency: String,
    val toCurrency: String,
    @Serializable(with = LocalDateSerializer::class) val date: LocalDate,
    @Serializable(with = BigDecimalSerializer::class) val rate: BigDecimal
)
