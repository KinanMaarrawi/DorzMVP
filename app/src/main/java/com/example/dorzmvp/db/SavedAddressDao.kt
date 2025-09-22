package com.example.dorzmvp.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface SavedAddressDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(address: SavedAddress)

    @Update
    suspend fun update(address: SavedAddress)

    @Delete
    suspend fun delete(address: SavedAddress)

    @Query("SELECT * FROM saved_addresses ORDER BY name ASC")
    fun getAll(): Flow<List<SavedAddress>>

    @Query("SELECT * FROM saved_addresses WHERE id = :id")
    suspend fun getById(id: Int): SavedAddress?
}
