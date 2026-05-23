package com.financial.freedom.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.datetime.LocalDate
import kotlinx.serialization.Serializable
import java.math.BigDecimal

@Entity(tableName = "holdings")
@Serializable
data class Holding(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val accountId: Long = 0,
    val type: String,
    val symbol: String,
    val name: String,
    val market: String = "",
    val currency: String,
    @Serializable(with = BigDecimalSerializer::class) val quantity: BigDecimal,
    @Serializable(with = BigDecimalSerializer::class) val costPrice: BigDecimal,
    @Serializable(with = LocalDateSerializer::class) val costDate: LocalDate,
    val note: String = "",
    val status: String = "active"
)
