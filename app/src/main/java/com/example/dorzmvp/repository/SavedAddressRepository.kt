package com.example.dorzmvp.repository

import com.example.dorzmvp.db.SavedAddress
import com.example.dorzmvp.db.SavedAddressDao
import kotlinx.coroutines.flow.Flow

/**
 * A repository that abstracts access to the saved addresses data source.
 *
 * This class provides a clean API for the app to interact with saved address data,
 * acting as a mediator between the [SavedAddressDao] and the ViewModels.
 *
 * @param savedAddressDao The Data Access Object for the saved addresses table.
 */
class SavedAddressRepository(private val savedAddressDao: SavedAddressDao) {

    /**
     * A flow that provides a real-time list of all saved addresses, ordered by name.
     * UI components can collect this flow to observe changes to the data.
     */
    val allAddresses: Flow<List<SavedAddress>> = savedAddressDao.getAllSavedAddresses()

    /**
     * Inserts a new address into the database on a background thread.
     *
     * @param address The [SavedAddress] object to insert.
     */
    suspend fun insert(address: SavedAddress) {
        savedAddressDao.insert(address)
    }

    /**
     * Updates an existing address in the database on a background thread.
     *
     * @param address The [SavedAddress] object to update.
     */
    suspend fun update(address: SavedAddress) {
        savedAddressDao.update(address)
    }

    /**
     * Deletes a specific address from the database on a background thread.
     *
     * @param address The [SavedAddress] object to delete.
     */
    suspend fun delete(address: SavedAddress) {
        savedAddressDao.delete(address)
    }

    /**
     * Retrieves a single address by its unique ID from the database.
     *
     * @param id The primary key of the address to fetch.
     * @return The [SavedAddress] object if found, otherwise null.
     */
    suspend fun getById(id: Int): SavedAddress? {
        return savedAddressDao.getById(id)
    }
}
