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

/**
 * ViewModel for managing and providing access to ride history data.
 *
 * This class interacts with the [RideHistoryRepository] to perform database operations
 * and exposes the ride data to the UI in a lifecycle-aware manner.
 *
 * @param repository The repository for accessing ride history data.
 */
class RideHistoryViewModel(private val repository: RideHistoryRepository) : ViewModel() {

    /**
     * A hot flow ([StateFlow]) that holds the complete list of all ride history records.
     * It keeps the most recent list of rides available and shares it among collectors.
     */
    private val allRides: StateFlow<List<RideHistory>> = repository.allRides
        .stateIn(
            scope = viewModelScope,
            // Keep the flow active for 5 seconds after the last collector unsubscribes.
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = emptyList()
        )

    /** A flow that filters [allRides] to emit only rides with an "ONGOING" status. */
    val ongoingRides: Flow<List<RideHistory>> = allRides.map { rides ->
        rides.filter { it.status == RideStatus.ONGOING }
    }

    /** A flow that filters [allRides] to emit only rides with a "FINISHED" status. */
    val finishedRides: Flow<List<RideHistory>> = allRides.map { rides ->
        rides.filter { it.status == RideStatus.FINISHED }
    }

    /**
     * Inserts a new ride record into the database.
     *
     * @param ride The [RideHistory] object to save.
     */
    fun saveRideToHistory(ride: RideHistory) {
        viewModelScope.launch {
            repository.insert(ride)
        }
    }

    /**
     * Updates an existing ride's status to "FINISHED".
     *
     * @param ride The [RideHistory] object to update.
     */
    fun finishRide(ride: RideHistory) {
        viewModelScope.launch {
            // Create a copy with the updated status before sending it to the repository.
            val finishedRide = ride.copy(status = RideStatus.FINISHED)
            repository.update(finishedRide)
        }
    }
}

/**
 * Factory for creating [RideHistoryViewModel] instances.
 *
 * This factory is necessary because the ViewModel has a non-empty constructor (it requires a
 * [RideHistoryRepository]), and it handles the dependency injection of the repository.
 */
class RideHistoryViewModelFactory(private val application: Application) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(RideHistoryViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            // Get database instance and create the repository to be passed to the ViewModel.
            val database = AppDatabase.getDatabase(application)
            val repository = RideHistoryRepository(database.rideHistoryDao())
            return RideHistoryViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
