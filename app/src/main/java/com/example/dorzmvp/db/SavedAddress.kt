package com.example.dorzmvp.db

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Represents a user's saved address in the database.
 * This data class defines the structure of the `saved_addresses` table.
 */
@Entity(tableName = "saved_addresses")
data class SavedAddress(
    /** The unique identifier for the address, generated automatically by the database. */
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,

    /** A custom name or label for the address (e.g., "Home", "Work"). */
    val name: String,

    /** The full, human-readable address string. */
    val address: String,

    /** The geographic latitude of the address. */
    val latitude: Double,

    /** The geographic longitude of the address. */
    val longitude: Double
)
