package com.example.dorzmvp.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object (DAO) for the saved_addresses table.
 *
 * This interface defines the database interactions for the [SavedAddress] entity,
 * including methods to insert, update, delete, and retrieve addresses.
 */
@Dao
interface SavedAddressDao {

    /**
     * Inserts a new address into the database. If an address with the same primary key
     * already exists, it will be replaced.
     *
     * @param address The [SavedAddress] object to insert.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(address: SavedAddress)

    /**
     * Updates an existing address in the database.
     *
     * @param address The [SavedAddress] object to update, identified by its primary key.
     */
    @Update
    suspend fun update(address: SavedAddress)

    /**
     * Deletes a specific address from the database.
     *
     * @param address The [SavedAddress] object to delete.
     */
    @Delete
    suspend fun delete(address: SavedAddress)

    /**
     * Retrieves all saved addresses from the database, ordered alphabetically by name.
     *
     * @return A [Flow] that emits a list of all [SavedAddress] objects whenever the data changes.
     */
    @Query("SELECT * FROM saved_addresses ORDER BY name ASC")
    fun getAllSavedAddresses(): Flow<List<SavedAddress>>

    /**
     * Retrieves a single saved address from the database by its unique ID.
     *
     * @param id The primary key of the address to retrieve.
     * @return The corresponding [SavedAddress] object, or null if no address is found with that ID.
     */
    @Query("SELECT * FROM saved_addresses WHERE id = :id")
    suspend fun getById(id: Int): SavedAddress?
}
