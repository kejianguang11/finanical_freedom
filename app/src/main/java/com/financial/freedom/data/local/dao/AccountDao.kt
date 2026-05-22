package com.financial.freedom.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.financial.freedom.data.local.entity.Account
import kotlinx.coroutines.flow.Flow

@Dao
interface AccountDao {
    @Query("SELECT * FROM accounts ORDER BY createdAt DESC")
    fun getAll(): Flow<List<Account>>

    @Query("SELECT * FROM accounts ORDER BY createdAt DESC")
    suspend fun getAllList(): List<Account>

    @Query("SELECT * FROM accounts WHERE id = :id")
    suspend fun getById(id: Long): Account?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(account: Account): Long

    @Update
    suspend fun update(account: Account)

    @Delete
    suspend fun delete(account: Account)

    @Query("SELECT COUNT(*) FROM accounts")
    suspend fun count(): Int
}
