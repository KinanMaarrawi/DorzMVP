package com.example.dorzmvp.db

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Defines constants for the ride status to avoid using raw strings in the code.
 */
object RideStatus {
    const val ONGOING = "ONGOING"
    const val FINISHED = "FINISHED"
}

/**
 * Represents a single ride record in the database.
 * This data class is used by Room to create the `ride_history_table`.
 */
@Entity(tableName = "ride_history_table")
data class RideHistory(
    /** The unique identifier for the ride, generated automatically by the database. */
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,

    /** The human-readable starting address of the ride. */
    val startAddress: String,

    /** The human-readable destination address of the ride. */
    val destinationAddress: String,

    /** The estimated price of the ride as a formatted string (e.g., "30 AED"). */
    val priceText: String,

    /** The class or category of the ride (e.g., "Economy", "Business"). */
    val rideClass: String,

    /** The timestamp when the ride was created, in milliseconds. Defaults to the current time. */
    val timestamp: Long = System.currentTimeMillis(),

    /** The current status of the ride, using values from [RideStatus] (e.g., "ONGOING", "FINISHED"). */
    val status: String
)
