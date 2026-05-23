package com.financial.freedom.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.datetime.LocalDate
import kotlinx.serialization.Serializable
import java.math.BigDecimal

@Entity(tableName = "debts")
@Serializable
data class Debt(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val accountId: Long,
    val name: String,
    @Serializable(with = BigDecimalSerializer::class) val amount: BigDecimal,
    @Serializable(with = LocalDateSerializer::class) val date: LocalDate,
    @Serializable(with = BigDecimalSerializer::class) val interestRate: BigDecimal? = null,
    val note: String = ""
)
