package com.financial.freedom.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.datetime.LocalDate
import java.math.BigDecimal

@Entity(
    tableName = "daily_summaries",
    indices = [Index(value = ["date", "accountId"], unique = true)]
)
data class DailySummary(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val accountId: Long = 0,
    val date: LocalDate,
    val totalValueCNY: BigDecimal,
    val dayChange: BigDecimal,
    val dayChangePct: BigDecimal
)
