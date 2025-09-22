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

class SavedAddressViewModel(private val repository: SavedAddressRepository) : ViewModel() {

    val savedAddresses: StateFlow<List<SavedAddress>> = repository.getAllSavedAddresses()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = emptyList()
        )

    fun addAddress(address: SavedAddress) {
        viewModelScope.launch {
            repository.insert(address)
        }
    }

    fun updateAddress(address: SavedAddress) {
        viewModelScope.launch {
            repository.update(address)
        }
    }

    fun deleteAddress(address: SavedAddress) {
        viewModelScope.launch {
            repository.delete(address)
        }
    }
}

class SavedAddressViewModelFactory(private val application: Application) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SavedAddressViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            val database = AppDatabase.getDatabase(application)
            val repository = SavedAddressRepository(database.savedAddressDao())
            return SavedAddressViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
