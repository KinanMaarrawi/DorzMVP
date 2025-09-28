package com.example.dorzmvp.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update // <-- Import Update
import kotlinx.coroutines.flow.Flow

@Dao
interface RideHistoryDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRide(ride: RideHistory)

    @Update
    suspend fun updateRide(ride: RideHistory) // <-- Add this function

    @Query("SELECT * FROM ride_history_table ORDER BY timestamp DESC")
    fun getAllRides(): Flow<List<RideHistory>>
}
