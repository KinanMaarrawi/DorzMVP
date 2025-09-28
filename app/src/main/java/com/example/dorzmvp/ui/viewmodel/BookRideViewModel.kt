// This file contains the BookRideViewModel, which is a crucial part of the MVVM architecture.
// Its primary purpose is to manage UI-related data for the ride booking feature in a lifecycle-conscious way.
// It handles the business logic for fetching taxi options from the Yandex Taxi API,
// processes the responses, and exposes the data (taxi options, loading states, error messages)
// to the UI (Composable functions) via LiveData.
// Key responsibilities:
// - Interacting with YandexApiService (via ApiClient) to make network requests.
// - Managing state like loading indicators (_isLoading) and error messages (_errorMessage).
// - Storing and providing the list of available taxi options (_taxiOptions).
// - Using viewModelScope for launching coroutines, ensuring that asynchronous operations
//   are automatically cancelled if the ViewModel is cleared (e.g., when the associated UI component is destroyed).
// - Accessing API keys securely via BuildConfig.

package com.example.dorzmvp.ui.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.dorzmvp.BuildConfig
import com.example.dorzmvp.network.ApiClient
import com.example.dorzmvp.network.YandexTaxiInfoResponse
import com.example.dorzmvp.network.YandexApiService
import com.example.dorzmvp.network.TaxiOptionResponse
import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

/**
 * ViewModel for the ride booking feature.
 *
 * This ViewModel is responsible for fetching taxi ride options from the Yandex Taxi API
 * based on provided start and destination coordinates. It handles multiple API calls for different
 * taxi classes, aggregates the results, and updates LiveData objects that the UI can observe.
 *
 * @param application The application context, required for AndroidViewModel.
 */
class BookRideViewModel(application: Application) : AndroidViewModel(application) {

    // Instance of the YandexApiService obtained from the ApiClient singleton.
    private val yandexApiService: YandexApiService = ApiClient.yandexApiService

    // LiveData to hold the list of successfully fetched and processed taxi options.
    // Observed by the UI to display ride options.
    private val _taxiOptions = MutableLiveData<YandexTaxiInfoResponse?>()
    val taxiOptions: LiveData<YandexTaxiInfoResponse?> = _taxiOptions

    // LiveData to indicate whether data is currently being loaded from the API.
    // Observed by the UI to show/hide loading indicators.
    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    // LiveData to hold error messages that can be displayed to the user.
    // Observed by the UI to show error notifications.
    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> = _errorMessage

    /**
     * Fetches taxi information for multiple service classes from the Yandex Taxi API.
     *
     * Iterates through a predefined list of taxi classes, making an individual API call
     * for each in parallel. It aggregates valid responses and updates the `taxiOptions`
     * LiveData. It also manages loading states and error messages.
     *
     * @param startLatLng The starting coordinates (latitude and longitude).
     * @param destinationLatLng The destination coordinates (latitude and longitude).
     */
    fun fetchTaxiInformation(startLatLng: LatLng, destinationLatLng: LatLng) {
        // A list of taxi classes to fetch information for.
        val taxiClasses = listOf("econom", "business", "comfortplus", "minivan", "vip")
        // This list will temporarily hold the options from all successful API calls.
        val aggregatedOptions = mutableListOf<TaxiOptionResponse>()
        // Holds the currency from the first successful response.
        var currency: String? = null

        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            _taxiOptions.value = null // Clear old results immediately

            val route = "${startLatLng.longitude},${startLatLng.latitude}~${destinationLatLng.longitude},${destinationLatLng.latitude}"

            try {
                // Use coroutineScope to create a structured concurrency block.
                // This ensures we wait for all child jobs (the API calls) to complete.
                coroutineScope {
                    taxiClasses.map { taxiClass ->
                        // CRITICAL: Launch each API call in its OWN async job.
                        // This isolates them. Failure or cancellation of one won't affect others.
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
                                // If a single network call fails (e.g., timeout, cancellation),
                                // log it but return null so it doesn't crash the whole process.
                                Log.e("BookRideViewModel", "API call failed for $taxiClass", e)
                                null
                            }
                        }
                    }.awaitAll() // Wait for all the async jobs to finish.
                        .filterNotNull() // Filter out any calls that failed (returned null).
                        .filter { it.isSuccessful && it.body() != null } // Filter for successful responses with a body.
                        .forEach { response ->
                            response.body()?.let { body ->
                                // If we don't have a currency yet, take it from the first valid response.
                                if (currency == null) {
                                    currency = body.currency
                                }
                                // Add the options from this successful call to our aggregate list.
                                body.options?.let { aggregatedOptions.addAll(it) }
                                Log.d("BookRideViewModel", "Added ${body.options?.size ?: 0} options for class: ${response.raw().request.url.queryParameter("class")}")
                            }
                        }
                }

                // --- Post the FINAL result, only once ---
                if (aggregatedOptions.isNotEmpty()) {
                    val finalResponse = YandexTaxiInfoResponse(
                        currency = currency,
                        options = aggregatedOptions.distinct().sortedBy { it.price } // Remove duplicates and sort by price
                    )
                    _taxiOptions.postValue(finalResponse)
                    Log.d("BookRideViewModel", "Final aggregated options posted. Count: ${finalResponse.options?.size}")
                } else {
                    // If after all calls, we have no options, post an error.
                    _errorMessage.postValue("No ride options found for this route.")
                }

            } catch (e: Exception) {
                // Catch any unexpected errors during the aggregation process.
                _errorMessage.postValue("An unexpected error occurred: ${e.message}")
            } finally {
                // This now runs only after ALL API calls have completed or failed.
                _isLoading.postValue(false)
            }
        }
    }

    private val _selectedRideOption = MutableLiveData<TaxiOptionResponse?>()
    val selectedRideOption: LiveData<TaxiOptionResponse?> = _selectedRideOption

    fun selectRideOption(option: TaxiOptionResponse) {
        _selectedRideOption.value = option
    }

    fun clearSelectedRideOption() {
        _selectedRideOption.value = null
    }


    /**
     * Clears the current error message.
     * Called by the UI when an error message has been displayed and should be dismissed.
     */
    fun clearError() {
        _errorMessage.value = null
    }
}
