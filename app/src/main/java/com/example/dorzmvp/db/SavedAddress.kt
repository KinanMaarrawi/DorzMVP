package com.example.dorzmvp.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "saved_addresses")
data class SavedAddress(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val name: String,
    val address: String,
    val latitude: Double,
    val longitude: Double
)
