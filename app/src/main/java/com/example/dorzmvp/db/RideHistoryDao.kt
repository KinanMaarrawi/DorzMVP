package com.example.dorzmvp.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object (DAO) for the ride_history_table.
 *
 * This interface defines the database interactions for the [RideHistory] entity,
 * including inserting, updating, and retrieving ride records.
 */
@Dao
interface RideHistoryDao {

    /**
     * Inserts a new ride into the database. If a ride with the same primary key
     * already exists, it will be replaced.
     *
     * @param ride The [RideHistory] object to insert.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRide(ride: RideHistory)

    /**
     * Updates an existing ride in the database.
     *
     * @param ride The [RideHistory] object to update, identified by its primary key.
     */
    @Update
    suspend fun updateRide(ride: RideHistory)

    /**
     * Retrieves all rides from the database, ordered by timestamp in descending order
     * (most recent rides first).
     *
     * @return A [Flow] that emits a list of all [RideHistory] objects whenever the data changes.
     */
    @Query("SELECT * FROM ride_history_table ORDER BY timestamp DESC")
    fun getAllRides(): Flow<List<RideHistory>>
}
