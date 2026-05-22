package com.financial.freedom.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.financial.freedom.data.local.entity.Holding
import kotlinx.coroutines.flow.Flow

@Dao
interface HoldingDao {
    @Query("SELECT * FROM holdings WHERE accountId = :accountId ORDER BY type, name")
    fun getAll(accountId: Long): Flow<List<Holding>>

    @Query("SELECT * FROM holdings WHERE accountId = :accountId ORDER BY type, name")
    suspend fun getAllList(accountId: Long): List<Holding>

    @Query("SELECT * FROM holdings WHERE type = :type AND accountId = :accountId ORDER BY name")
    fun getByType(type: String, accountId: Long): Flow<List<Holding>>

    @Query("SELECT * FROM holdings WHERE id = :id AND accountId = :accountId")
    suspend fun getById(id: Long, accountId: Long): Holding?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(holding: Holding): Long

    @Update
    suspend fun update(holding: Holding)

    @Delete
    suspend fun delete(holding: Holding)

    @Query("DELETE FROM holdings WHERE accountId = :accountId")
    suspend fun deleteByAccountId(accountId: Long)
}
