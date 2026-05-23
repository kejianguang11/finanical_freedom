package com.financial.freedom.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.datetime.LocalDate
import kotlinx.serialization.Serializable
import java.math.BigDecimal

@Entity(tableName = "cash_transactions")
@Serializable
data class CashTransaction(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val accountId: Long,
    @Serializable(with = LocalDateSerializer::class) val date: LocalDate,
    @Serializable(with = BigDecimalSerializer::class) val amount: BigDecimal,
    val type: String,
    val note: String = ""
)
