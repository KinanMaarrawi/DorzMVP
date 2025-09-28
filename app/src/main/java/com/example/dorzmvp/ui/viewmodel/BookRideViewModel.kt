package com.example.dorzmvp.ui.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.dorzmvp.BuildConfig
import com.example.dorzmvp.network.ApiClient
import com.example.dorzmvp.network.TaxiOptionResponse
import com.example.dorzmvp.network.YandexTaxiInfoResponse
import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

/**
 * ViewModel responsible for managing the state and logic for the ride booking process.
 *
 * This class handles fetching taxi options from the Yandex API, holding location data,
 * and managing UI states like loading indicators and error messages.
 *
 * @param application The application context, required for AndroidViewModel.
 */
class BookRideViewModel(application: Application) : AndroidViewModel(application) {

    private val yandexApiService = ApiClient.yandexApiService

    // --- LiveData for UI State ---

    /** Holds the response from the Yandex API containing ride options. Emits null during refetch or on error. */
    private val _taxiOptions = MutableLiveData<YandexTaxiInfoResponse?>()
    val taxiOptions: LiveData<YandexTaxiInfoResponse?> = _taxiOptions

    /** Tracks whether a network request for taxi options is in progress. */
    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    /** Contains an error message string if an API call fails or finds no options. */
    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> = _errorMessage

    // --- LiveData for Ride Details ---

    /** The starting address string for the ride. */
    private val _startAddress = MutableLiveData<String?>()
    val startAddress: LiveData<String?> = _startAddress

    /** The destination address string for the ride. */
    private val _destinationAddress = MutableLiveData<String?>()
    val destinationAddress: LiveData<String?> = _destinationAddress

    /** The geographic coordinates (LatLng) of the starting location. */
    private val _startLocation = MutableLiveData<LatLng?>()
    val startLocation: LiveData<LatLng?> = _startLocation

    /** The geographic coordinates (LatLng) of the destination location. */
    private val _destinationLocation = MutableLiveData<LatLng?>()
    val destinationLocation: LiveData<LatLng?> = _destinationLocation

    /** The specific [TaxiOptionResponse] chosen by the user. */
    private val _selectedRideOption = MutableLiveData<TaxiOptionResponse?>()
    val selectedRideOption: LiveData<TaxiOptionResponse?> = _selectedRideOption

    /**
     * Fetches taxi information by making parallel API calls for different taxi classes.
     * It aggregates the results and posts them to the [_taxiOptions] LiveData.
     *
     * @param startLatLng The geographic coordinates of the starting point.
     * @param destinationLatLng The geographic coordinates of the destination.
     */
    fun fetchTaxiInformation(startLatLng: LatLng, destinationLatLng: LatLng) {
        // Store the coordinates to be used by other screens (e.g., confirmation/tracking).
        _startLocation.value = startLatLng
        _destinationLocation.value = destinationLatLng

        // A list of taxi classes to query from the Yandex API.
        val taxiClasses = listOf("econom", "business", "comfortplus", "minivan", "vip")
        val aggregatedOptions = mutableListOf<TaxiOptionResponse>()
        var currency: String? = null

        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            _taxiOptions.value = null // Clear previous options

            val route = "${startLatLng.longitude},${startLatLng.latitude}~${destinationLatLng.longitude},${destinationLatLng.latitude}"

            try {
                // Launch API calls for all taxi classes concurrently for better performance.
                val responses = coroutineScope {
                    taxiClasses.map { taxiClass ->
                        async(Dispatchers.IO) {
                            try {
                                Log.d("BookRideViewModel", "Fetching info for class: $taxiClass")
                                yandexApiService.getTaxiInfo(
                                    clid = BuildConfig.YANGO_CLID,
                                    apikey = BuildConfig.YANGO_API_KEY,
                                    routeLongLat = route,
                                    lang = "en",
                                    currency = "AED",
                                    taxiClass = taxiClass
                                )
                            } catch (e: Exception) {
                                Log.e("BookRideViewModel", "API call failed for $taxiClass", e)
                                null // Return null on failure for this specific call
                            }
                        }
                    }.awaitAll() // Wait for all concurrent calls to complete.
                }

                // Filter out failed or empty responses and process the successful ones.
                responses.filterNotNull().filter { it.isSuccessful && it.body() != null }.forEach { response ->
                    response.body()?.let { body ->
                        if (currency == null) currency = body.currency
                        body.options?.let { aggregatedOptions.addAll(it) }
                    }
                }

                if (aggregatedOptions.isNotEmpty()) {
                    // Create a final response object with unique, sorted options.
                    val finalResponse = YandexTaxiInfoResponse(
                        currency = currency,
                        options = aggregatedOptions.distinct().sortedBy { it.price }
                    )
                    _taxiOptions.postValue(finalResponse)
                    Log.d("BookRideViewModel", "Final options count: ${finalResponse.options?.size}")
                } else {
                    _errorMessage.postValue("No ride options found for this route.")
                }

            } catch (e: Exception) {
                _errorMessage.postValue("An unexpected error occurred: ${e.message}")
            } finally {
                _isLoading.postValue(false)
            }
        }
    }

    /** Updates the start and destination address strings. */
    fun updateAddresses(start: String?, destination: String?) {
        _startAddress.value = start
        _destinationAddress.value = destination
    }

    /** Sets the user's chosen ride option. */
    fun selectRideOption(option: TaxiOptionResponse) {
        _selectedRideOption.value = option
    }

    /** Clears the selected ride option, typically after a ride is booked or cancelled. */
    fun clearSelectedRideOption() {
        _selectedRideOption.value = null
    }

    /** Clears the current error message. */
    fun clearError() {
        _errorMessage.value = null
    }
}
