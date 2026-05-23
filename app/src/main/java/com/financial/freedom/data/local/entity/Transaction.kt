package com.financial.freedom.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.datetime.LocalDate
import kotlinx.serialization.Serializable
import java.math.BigDecimal

@Entity(tableName = "transactions")
@Serializable
data class Transaction(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val holdingId: Long,
    val accountId: Long = 0,
    val type: String,
    @Serializable(with = LocalDateSerializer::class) val date: LocalDate,
    @Serializable(with = BigDecimalSerializer::class) val price: BigDecimal,
    @Serializable(with = BigDecimalSerializer::class) val quantity: BigDecimal,
    @Serializable(with = BigDecimalSerializer::class) val fee: BigDecimal = BigDecimal.ZERO
)
