package com.financial.freedom.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.financial.freedom.data.local.entity.Debt
import kotlinx.coroutines.flow.Flow

@Dao
interface DebtDao {
    @Query("SELECT * FROM debts WHERE accountId = :accountId ORDER BY date DESC")
    fun getAll(accountId: Long): Flow<List<Debt>>

    @Query("SELECT * FROM debts WHERE accountId = :accountId ORDER BY date DESC")
    suspend fun getAllList(accountId: Long): List<Debt>

    @Query("SELECT * FROM debts WHERE id = :id AND accountId = :accountId")
    suspend fun getById(id: Long, accountId: Long): Debt?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(d: Debt): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(ds: List<Debt>)

    @Update
    suspend fun update(d: Debt)

    @Delete
    suspend fun delete(d: Debt)

    @Query("DELETE FROM debts WHERE accountId = :accountId")
    suspend fun deleteByAccountId(accountId: Long)
}
