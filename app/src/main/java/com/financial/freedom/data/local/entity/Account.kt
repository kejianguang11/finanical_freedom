package com.financial.freedom.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "accounts")
data class Account(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val nickname: String,
    val pinHash: String,
    val createdAt: Long = System.currentTimeMillis()
)
