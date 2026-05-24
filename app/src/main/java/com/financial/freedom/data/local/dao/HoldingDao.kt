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
    @Query("SELECT * FROM holdings WHERE accountId = :accountId AND status = 'active' ORDER BY type, name")
    fun getAll(accountId: Long): Flow<List<Holding>>

    @Query("SELECT * FROM holdings WHERE accountId = :accountId AND status = 'active' ORDER BY type, name")
    suspend fun getAllList(accountId: Long): List<Holding>

    @Query("SELECT * FROM holdings WHERE type = :type AND accountId = :accountId AND status = 'active' ORDER BY name")
    fun getByType(type: String, accountId: Long): Flow<List<Holding>>

    @Query("SELECT * FROM holdings WHERE type = :type AND accountId = :accountId AND status = 'active' ORDER BY name")
    suspend fun getByTypeList(type: String, accountId: Long): List<Holding>

    @Query("SELECT * FROM holdings WHERE id = :id AND accountId = :accountId")
    suspend fun getById(id: Long, accountId: Long): Holding?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(holding: Holding): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(holdings: List<Holding>)

    @Update
    suspend fun update(holding: Holding)

    @Delete
    suspend fun delete(holding: Holding)

    @Query("UPDATE holdings SET name = :name WHERE id = :id")
    suspend fun updateName(id: Long, name: String)

    @Query("UPDATE holdings SET symbol = :symbol WHERE id = :id")
    suspend fun updateSymbol(id: Long, symbol: String)

    @Query("DELETE FROM holdings WHERE accountId = :accountId")
    suspend fun deleteByAccountId(accountId: Long)
}
