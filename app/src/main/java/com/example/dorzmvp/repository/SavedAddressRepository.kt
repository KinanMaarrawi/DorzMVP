package com.example.dorzmvp.repository

import com.example.dorzmvp.db.SavedAddress
import com.example.dorzmvp.db.SavedAddressDao
import kotlinx.coroutines.flow.Flow

class SavedAddressRepository(private val savedAddressDao: SavedAddressDao) {

    fun getAllSavedAddresses(): Flow<List<SavedAddress>> {
        return savedAddressDao.getAll()
    }

    suspend fun insert(address: SavedAddress) {
        savedAddressDao.insert(address)
    }

    suspend fun update(address: SavedAddress) {
        savedAddressDao.update(address)
    }

    suspend fun delete(address: SavedAddress) {
        savedAddressDao.delete(address)
    }

    suspend fun getById(id: Int): SavedAddress? {
        return savedAddressDao.getById(id)
    }
}
