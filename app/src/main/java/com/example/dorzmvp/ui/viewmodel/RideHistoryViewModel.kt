package com.example.dorzmvp.ui.viewmodel

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.dorzmvp.db.AppDatabase
import com.example.dorzmvp.db.RideHistory
import com.example.dorzmvp.db.RideStatus
import com.example.dorzmvp.repository.RideHistoryRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class RideHistoryViewModel(private val repository: RideHistoryRepository) : ViewModel() {

    private val allRides: StateFlow<List<RideHistory>> = repository.allRides
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = emptyList()
        )

    // Expose a flow of only ongoing rides
    val ongoingRides: Flow<List<RideHistory>> = allRides.map { rides ->
        rides.filter { it.status == RideStatus.ONGOING }
    }

    // Expose a flow of only finished rides
    val finishedRides: Flow<List<RideHistory>> = allRides.map { rides ->
        rides.filter { it.status == RideStatus.FINISHED }
    }

    fun saveRideToHistory(ride: RideHistory) {
        viewModelScope.launch {
            repository.insert(ride)
        }
    }

    // Function to change a ride's status to FINISHED
    fun finishRide(ride: RideHistory) {
        viewModelScope.launch {
            val finishedRide = ride.copy(status = RideStatus.FINISHED)
            repository.update(finishedRide)
        }
    }
}

class RideHistoryViewModelFactory(private val application: Application) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(RideHistoryViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            val database = AppDatabase.getDatabase(application)
            val repository = RideHistoryRepository(database.rideHistoryDao())
            return RideHistoryViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
