package com.financial.freedom.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.datetime.LocalDate
import java.math.BigDecimal

@Entity(
    tableName = "daily_breakdown_items",
    indices = [Index(value = ["date", "accountId", "type"], unique = true)]
)
data class DailyBreakdownItem(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val accountId: Long = 0,
    val date: LocalDate,
    val type: String,                 // DEPOSIT / STOCK / FUND / GOLD
    val valueCNY: BigDecimal,
    val changeCNY: BigDecimal
)
