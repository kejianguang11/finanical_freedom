package com.financial.freedom.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.datetime.LocalDate
import kotlinx.serialization.Serializable
import java.math.BigDecimal

@Entity(
    tableName = "daily_summaries",
    indices = [Index(value = ["date", "accountId"], unique = true)]
)
@Serializable
data class DailySummary(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val accountId: Long = 0,
    @Serializable(with = LocalDateSerializer::class) val date: LocalDate,
    @Serializable(with = BigDecimalSerializer::class) val totalValueCNY: BigDecimal,
    @Serializable(with = BigDecimalSerializer::class) val dayChange: BigDecimal,
    @Serializable(with = BigDecimalSerializer::class) val dayChangePct: BigDecimal,
    @Serializable(with = BigDecimalSerializer::class) val netInflow: BigDecimal = BigDecimal.ZERO,
    @Serializable(with = BigDecimalSerializer::class) val netWorth: BigDecimal = BigDecimal.ZERO,
    @Serializable(with = BigDecimalSerializer::class) val cashBalance: BigDecimal = BigDecimal.ZERO
)
