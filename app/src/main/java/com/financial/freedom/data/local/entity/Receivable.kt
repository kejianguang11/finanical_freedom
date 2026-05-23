package com.financial.freedom.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.datetime.LocalDate
import kotlinx.serialization.Serializable
import java.math.BigDecimal

@Entity(tableName = "receivables")
@Serializable
data class Receivable(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val accountId: Long,
    val name: String,
    @Serializable(with = BigDecimalSerializer::class) val amount: BigDecimal,
    @Serializable(with = LocalDateSerializer::class) val date: LocalDate,
    @Serializable(with = LocalDateSerializer::class) val expectedDate: LocalDate? = null,
    val note: String = ""
)
