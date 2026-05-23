package com.financial.freedom.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.financial.freedom.data.local.entity.Receivable
import kotlinx.coroutines.flow.Flow

@Dao
interface ReceivableDao {
    @Query("SELECT * FROM receivables WHERE accountId = :accountId ORDER BY date DESC")
    fun getAll(accountId: Long): Flow<List<Receivable>>

    @Query("SELECT * FROM receivables WHERE accountId = :accountId ORDER BY date DESC")
    suspend fun getAllList(accountId: Long): List<Receivable>

    @Query("SELECT * FROM receivables WHERE id = :id AND accountId = :accountId")
    suspend fun getById(id: Long, accountId: Long): Receivable?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(r: Receivable): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(rs: List<Receivable>)

    @Update
    suspend fun update(r: Receivable)

    @Delete
    suspend fun delete(r: Receivable)

    @Query("DELETE FROM receivables WHERE accountId = :accountId")
    suspend fun deleteByAccountId(accountId: Long)
}
