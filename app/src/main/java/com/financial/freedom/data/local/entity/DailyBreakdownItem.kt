package com.financial.freedom.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.datetime.LocalDate
import kotlinx.serialization.Serializable
import java.math.BigDecimal

@Entity(
    tableName = "daily_breakdown_items",
    indices = [Index(value = ["date", "accountId", "type"], unique = true)]
)
@Serializable
data class DailyBreakdownItem(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val accountId: Long = 0,
    @Serializable(with = LocalDateSerializer::class) val date: LocalDate,
    val type: String,
    @Serializable(with = BigDecimalSerializer::class) val valueCNY: BigDecimal,
    @Serializable(with = BigDecimalSerializer::class) val changeCNY: BigDecimal,
    @Serializable(with = BigDecimalSerializer::class) val contribution: BigDecimal = BigDecimal.ZERO
)
