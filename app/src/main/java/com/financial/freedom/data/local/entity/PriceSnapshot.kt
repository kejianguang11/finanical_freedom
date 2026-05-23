package com.financial.freedom.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.datetime.LocalDate
import kotlinx.serialization.Serializable
import java.math.BigDecimal

@Entity(
    tableName = "price_snapshots",
    indices = [androidx.room.Index(value = ["holdingId", "date"], unique = true)]
)
@Serializable
data class PriceSnapshot(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val holdingId: Long,
    val accountId: Long = 0,
    @Serializable(with = LocalDateSerializer::class) val date: LocalDate,
    @Serializable(with = BigDecimalSerializer::class) val unitPrice: BigDecimal,
    val currency: String
)
