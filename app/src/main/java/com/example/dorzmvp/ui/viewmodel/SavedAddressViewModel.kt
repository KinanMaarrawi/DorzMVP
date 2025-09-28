package com.example.dorzmvp.ui.viewmodel

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.dorzmvp.db.AppDatabase
import com.example.dorzmvp.db.SavedAddress
import com.example.dorzmvp.repository.SavedAddressRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * ViewModel for managing and providing access to saved address data.
 *
 * This class interacts with the [SavedAddressRepository] to perform database operations
 * (add, update, delete) and exposes a flow of saved addresses for the UI to observe.
 *
 * @param repository The repository for accessing saved address data.
 */
class SavedAddressViewModel(private val repository: SavedAddressRepository) : ViewModel() {

    /**
     * A hot flow ([StateFlow]) that holds the current list of all saved addresses.
     * The UI can collect this flow to automatically update when the address list changes.
     * The data is shared and kept active for 5 seconds after the last observer is gone.
     */
    val savedAddresses: StateFlow<List<SavedAddress>> = repository.allAddresses
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = emptyList()
        )

    /**
     * Inserts a new address into the database on a background thread.
     *
     * @param address The [SavedAddress] object to add.
     */
    fun addAddress(address: SavedAddress) {
        viewModelScope.launch {
            repository.insert(address)
        }
    }

    /**
     * Updates an existing address in the database on a background thread.
     *
     * @param address The [SavedAddress] object to update.
     */
    fun updateAddress(address: SavedAddress) {
        viewModelScope.launch {
            repository.update(address)
        }
    }

    /**
     * Deletes a specific address from the database on a background thread.
     *
     * @param address The [SavedAddress] object to delete.
     */
    fun deleteAddress(address: SavedAddress) {
        viewModelScope.launch {
            repository.delete(address)
        }
    }
}

/**
 * Factory for creating [SavedAddressViewModel] instances with dependencies.
 *
 * This is necessary because the ViewModel has a constructor that requires a [SavedAddressRepository].
 * This factory handles the creation of the repository and passing it to the ViewModel.
 */
class SavedAddressViewModelFactory(private val application: Application) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        // Check if the requested ViewModel is of the correct type.
        if (modelClass.isAssignableFrom(SavedAddressViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            // Get database instance and create the repository.
            val database = AppDatabase.getDatabase(application)
            val repository = SavedAddressRepository(database.savedAddressDao())
            // Create and return the ViewModel instance.
            return SavedAddressViewModel(repository) as T
        }
        // Throw an exception if the modelClass is unknown.
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
