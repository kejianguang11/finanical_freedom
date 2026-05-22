package com.financial.freedom.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.datetime.LocalDate
import java.math.BigDecimal

@Entity(tableName = "holdings")
data class Holding(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val accountId: Long = 0,
    val type: String,                 // STOCK / FUND / GOLD
    val symbol: String,
    val name: String,
    val market: String = "",          // CN / US / HK
    val currency: String,
    val quantity: BigDecimal,         // 股数 / 份额 / 克
    val costPrice: BigDecimal,        // 成本单价
    val costDate: LocalDate,
    val note: String = ""
)
