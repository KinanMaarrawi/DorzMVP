package com.example.dorzmvp.repository

import com.example.dorzmvp.db.RideHistory
import com.example.dorzmvp.db.RideHistoryDao
import kotlinx.coroutines.flow.Flow

class RideHistoryRepository(private val rideHistoryDao: RideHistoryDao) {

    val allRides: Flow<List<RideHistory>> = rideHistoryDao.getAllRides()

    suspend fun insert(ride: RideHistory) {
        rideHistoryDao.insertRide(ride)
    }

    suspend fun update(ride: RideHistory) { // <-- Add this function
        rideHistoryDao.updateRide(ride)
    }
}
