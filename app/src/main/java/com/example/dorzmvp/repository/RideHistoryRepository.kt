package com.example.dorzmvp.repository

import com.example.dorzmvp.db.RideHistory
import com.example.dorzmvp.db.RideHistoryDao
import kotlinx.coroutines.flow.Flow

/**
 * A repository that abstracts access to the ride history data source.
 *
 * This class provides a clean API for the rest of the app to interact with
 * ride history data, acting as a mediator between the DAO and the ViewModels.
 *
 * @param rideHistoryDao The Data Access Object for the ride history table.
 */
class RideHistoryRepository(private val rideHistoryDao: RideHistoryDao) {

    /**
     * A flow that emits a list of all ride history records, ordered from newest to oldest.
     * UI components can collect this flow to observe real-time changes in the ride data.
     */
    val allRides: Flow<List<RideHistory>> = rideHistoryDao.getAllRides()

    /**
     * Inserts a new ride record into the database. This is a suspend function
     * and must be called from a coroutine.
     *
     * @param ride The [RideHistory] object to be inserted.
     */
    suspend fun insert(ride: RideHistory) {
        rideHistoryDao.insertRide(ride)
    }

    /**
     * Updates an existing ride record in the database.
     * This is useful for changing a ride's status from "ONGOING" to "FINISHED".
     *
     * @param ride The [RideHistory] object to be updated.
     */
    suspend fun update(ride: RideHistory) {
        rideHistoryDao.updateRide(ride)
    }
}
