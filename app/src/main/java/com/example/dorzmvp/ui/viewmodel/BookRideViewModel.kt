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
import com.example.dorzmvp.network.TaxiOptionResponse
import com.example.dorzmvp.network.YandexApiService
import com.google.android.gms.maps.model.LatLng
import com.google.gson.JsonSyntaxException
import kotlinx.coroutines.launch
import java.io.EOFException
import java.io.IOException

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
    private val _taxiOptions = MutableLiveData<List<TaxiOptionResponse>?>()
    val taxiOptions: LiveData<List<TaxiOptionResponse>?> = _taxiOptions

    // LiveData to indicate whether data is currently being loaded from the API.
    // Observed by the UI to show/hide loading indicators.
    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    // LiveData to hold error messages that can be displayed to the user.
    // Observed by the UI to show error notifications.
    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> = _errorMessage

    // Defines the different taxi service classes to query the API for.
    // The API is called individually for each of these classes.
    private val taxiClassesToFetch = listOf("econom", "business", "comfortplus", "minivan", "vip")

    /**
     * Fetches taxi information for multiple service classes from the Yandex Taxi API.
     *
     * Iterates through a predefined list of taxi classes (`taxiClassesToFetch`),
     * making an individual API call for each. It aggregates valid responses and updates
     * the `taxiOptions` LiveData. It also manages loading states and error messages.
     *
     * @param startLatLng The starting coordinates (latitude and longitude).
     * @param destinationLatLng The destination coordinates (latitude and longitude).
     * @param taxiClass This parameter is present from a previous structure but is effectively
     *                  overridden by the internal loop that iterates through `taxiClassesToFetch`.
     *                  It's kept for potential future use or direct class queries if needed.
     * @param requirements Optional requirements for the taxi (e.g., child seat). Currently passed through.
     */
    fun fetchTaxiInformation(
        startLatLng: LatLng,
        destinationLatLng: LatLng,
        taxiClass: String? = null, // Note: This parameter is currently unused due to the loop below.
        requirements: String? = null
    ) {
        // Format the route coordinates string as required by the Yandex API.
        val rll = "${startLatLng.longitude},${startLatLng.latitude}~${destinationLatLng.longitude},${destinationLatLng.latitude}"
        // Retrieve API credentials securely from BuildConfig (which gets them from secrets or gradle properties).
        val clid = BuildConfig.YANGO_CLID
        val apikey = BuildConfig.YANGO_API_KEY

        _isLoading.value = true // Signal that loading has started.
        _errorMessage.value = null // Clear any previous error messages.

        val allFetchedOptions = mutableListOf<TaxiOptionResponse>() // Accumulator for all valid options.

        // Launch a coroutine in the viewModelScope. This scope is tied to the ViewModel's lifecycle.
        viewModelScope.launch {
            try {
                // Loop through each defined taxi class and make a separate API call.
                for (currentClass in taxiClassesToFetch) {
                    Log.d("BookRideViewModel", "Fetching info for class: $currentClass")
                    try {
                        // Perform the actual network request using the YandexApiService.
                        val response = yandexApiService.getTaxiInfo(
                            clid = clid,
                            apikey = apikey,
                            routeLongLat = rll,
                            lang = "en",        // Request language for response data.
                            currency = "AED",   // Request currency for pricing.
                            taxiClass = currentClass, // The specific class for this request.
                            requirements = requirements
                        )

                        if (response.isSuccessful) {
                            // HTTP request was successful (e.g., 200 OK).
                            try {
                                // Attempt to parse the response body.
                                // This is where an EOFException can occur if the body is empty,
                                // or JsonSyntaxException if the body is not valid JSON.
                                val responseBody = response.body()

                                if (responseBody?.options?.isNotEmpty() == true) {
                                    // Successfully parsed and options are available.
                                    val processedOptions = responseBody.options.map {
                                        // Ensure classText and className are populated, falling back to currentClass if needed.
                                        it.copy(
                                            classText = it.classText ?: it.className ?: currentClass.replaceFirstChar { char -> if (char.isLowerCase()) char.titlecase() else char.toString() },
                                            className = it.className ?: currentClass
                                        )
                                    }
                                    allFetchedOptions.addAll(processedOptions)
                                    Log.d("BookRideViewModel", "Added ${processedOptions.size} options for class: $currentClass. Details: $processedOptions")
                                } else {
                                    // Response was successful and parsable, but contained no options or an empty body leading to a null responseBody after parsing.
                                    Log.d("BookRideViewModel", "No options in API response for class: $currentClass (body: $responseBody)")
                                }
                            } catch (e: EOFException) {
                                // Handle cases where the API returns a 200 OK but with an empty body (e.g., Content-Length: 0).
                                // This indicates no availability for the class, rather than a server error.
                                Log.w("BookRideViewModel", "Empty response body (EOFException) for class: $currentClass. Treating as no options.")
                            } catch (e: JsonSyntaxException) {
                                // Handle cases where the API returns 200 OK, but the body is not valid JSON or doesn't match YandexTaxiInfoResponse structure.
                                Log.e("BookRideViewModel", "JSON parsing error for $currentClass: ${e.message}. Body might be non-JSON or malformed.")
                            }
                        } else {
                            // HTTP request failed (e.g., 4xx client error, 5xx server error).
                            val errorBodyString = response.errorBody()?.string() // Attempt to read the error body.
                            Log.e("BookRideViewModel", "API Error for $currentClass (${response.code()}): $errorBodyString")
                        }
                    } catch (e: IOException) {
                        // Handle network-level errors for an individual API call (e.g., no internet connection during this specific call).
                        Log.e("BookRideViewModel", "Network error during API call for $currentClass: ${e.message}", e)
                    } catch (e: Exception) {
                        // Catch any other unexpected errors during an individual API call.
                        Log.e("BookRideViewModel", "Unexpected error during API call for $currentClass: ${e.message}", e)
                    }
                } // End of loop for taxiClassesToFetch.

                // After attempting to fetch all classes, update LiveData based on aggregated results.
                if (allFetchedOptions.isNotEmpty()) {
                    // Use distinctBy to avoid showing duplicate entries if the API somehow returns them for different class queries but identical details.
                    _taxiOptions.value = allFetchedOptions.distinctBy {listOf(it.className, it.priceText) }
                    Log.d("BookRideViewModel", "Aggregated Taxi Options (count: ${allFetchedOptions.size}, distinct count: ${_taxiOptions.value?.size}): ${_taxiOptions.value}")
                } else {
                    // No options found for any class, or all calls resulted in errors/empty responses.
                    _taxiOptions.value = emptyList()
                    _errorMessage.value = "No ride options found for the selected route."
                    Log.w("BookRideViewModel", "No ride options found after checking all classes.")
                }

            } catch (e: Exception) {
                // Catch-all for any unforeseen errors within the main try block of the coroutine (e.g., issues outside the loop).
                Log.e("BookRideViewModel", "Outer scope error in fetchTaxiInformation: ${e.message}", e)
                _errorMessage.value = "A critical error occurred while fetching ride options."
                 _taxiOptions.value = emptyList() // Ensure options are cleared on critical error.
            } finally {
                _isLoading.value = false // Signal that loading has finished, regardless of outcome.
            }
        }
    }

    /**
     * Clears the current error message.
     * Called by the UI when an error message has been displayed and should be dismissed.
     */
    fun clearErrorMessage() {
        _errorMessage.value = null
    }
}
