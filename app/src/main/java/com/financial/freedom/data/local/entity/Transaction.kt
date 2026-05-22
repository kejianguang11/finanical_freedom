package com.financial.freedom.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.datetime.LocalDate
import java.math.BigDecimal

@Entity(tableName = "transactions")
data class Transaction(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val holdingId: Long,
    val accountId: Long = 0,
    val type: String,                 // BUY / SELL / DIVIDEND
    val date: LocalDate,
    val price: BigDecimal,
    val quantity: BigDecimal,
    val fee: BigDecimal = BigDecimal.ZERO
)
