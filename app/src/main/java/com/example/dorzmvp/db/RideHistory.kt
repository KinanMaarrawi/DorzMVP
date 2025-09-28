package com.example.dorzmvp.db

import androidx.room.Entity
import androidx.room.PrimaryKey

// Define constants for the ride status to avoid magic strings
object RideStatus {
    const val ONGOING = "ONGOING"
    const val FINISHED = "FINISHED"
}

@Entity(tableName = "ride_history_table")
data class RideHistory(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val startAddress: String,
    val destinationAddress: String,
    val priceText: String,
    val rideClass: String,
    val timestamp: Long = System.currentTimeMillis(),
    val status: String // New field to track if the ride is ongoing or finished
)
