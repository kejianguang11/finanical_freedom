package com.financial.freedom.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.datetime.LocalDate
import java.math.BigDecimal

@Entity(tableName = "deposits")
data class Deposit(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val accountId: Long = 0,
    val name: String,
    val bank: String,
    val currency: String,
    val principal: BigDecimal,
    val interestRate: BigDecimal,     // 年化利率，如 0.0275 = 2.75%
    val startDate: LocalDate,
    val maturityDate: LocalDate,
    val status: String = "active",    // active / matured
    val note: String = ""
)
