package com.financial.freedom.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.datetime.LocalDate
import kotlinx.serialization.Serializable
import java.math.BigDecimal

@Entity(tableName = "deposits")
@Serializable
data class Deposit(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val accountId: Long = 0,
    val name: String,
    val bank: String,
    val currency: String,
    @Serializable(with = BigDecimalSerializer::class) val principal: BigDecimal,
    @Serializable(with = BigDecimalSerializer::class) val interestRate: BigDecimal,
    @Serializable(with = LocalDateSerializer::class) val startDate: LocalDate,
    @Serializable(with = LocalDateSerializer::class) val maturityDate: LocalDate,
    val status: String = "active",
    val note: String = ""
)
