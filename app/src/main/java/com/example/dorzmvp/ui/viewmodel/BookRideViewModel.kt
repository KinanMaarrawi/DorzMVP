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
import com.example.dorzmvp.network.YandexTaxiInfoResponse
import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

class BookRideViewModel(application: Application) : AndroidViewModel(application) {

    private val yandexApiService: YandexApiService = ApiClient.yandexApiService

    private val _taxiOptions = MutableLiveData<YandexTaxiInfoResponse?>()
    val taxiOptions: LiveData<YandexTaxiInfoResponse?> = _taxiOptions

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> = _errorMessage

    private val _startAddress = MutableLiveData<String?>()
    val startAddress: LiveData<String?> = _startAddress

    private val _destinationAddress = MutableLiveData<String?>()
    val destinationAddress: LiveData<String?> = _destinationAddress

    private val _selectedRideOption = MutableLiveData<TaxiOptionResponse?>()
    val selectedRideOption: LiveData<TaxiOptionResponse?> = _selectedRideOption

    // --- ADDED/FIXED: LiveData for LatLng coordinates ---
    private val _startLocation = MutableLiveData<LatLng?>()
    val startLocation: LiveData<LatLng?> = _startLocation

    private val _destinationLocation = MutableLiveData<LatLng?>()
    val destinationLocation: LiveData<LatLng?> = _destinationLocation
    // --- END ---

    fun fetchTaxiInformation(startLatLng: LatLng, destinationLatLng: LatLng) {
        // --- ADDED/FIXED: Set the LatLng values when fetching ---
        _startLocation.value = startLatLng
        _destinationLocation.value = destinationLatLng
        // --- END ---

        val taxiClasses = listOf("econom", "business", "comfortplus", "minivan", "vip")
        val aggregatedOptions = mutableListOf<TaxiOptionResponse>()
        var currency: String? = null

        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            _taxiOptions.value = null

            val route = "${startLatLng.longitude},${startLatLng.latitude}~${destinationLatLng.longitude},${destinationLatLng.latitude}"

            try {
                coroutineScope {
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
                                null
                            }
                        }
                    }.awaitAll()
                        .filterNotNull()
                        .filter { it.isSuccessful && it.body() != null }
                        .forEach { response ->
                            response.body()?.let { body ->
                                if (currency == null) {
                                    currency = body.currency
                                }
                                body.options?.let { aggregatedOptions.addAll(it) }
                                Log.d("BookRideViewModel", "Added ${body.options?.size ?: 0} options for class: ${response.raw().request.url.queryParameter("class")}")
                            }
                        }
                }

                if (aggregatedOptions.isNotEmpty()) {
                    val finalResponse = YandexTaxiInfoResponse(
                        currency = currency,
                        options = aggregatedOptions.distinct().sortedBy { it.price }
                    )
                    _taxiOptions.postValue(finalResponse)
                    Log.d("BookRideViewModel", "Final aggregated options posted. Count: ${finalResponse.options?.size}")
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

    fun updateAddresses(start: String?, destination: String?) {
        _startAddress.value = start
        _destinationAddress.value = destination
    }

    fun selectRideOption(option: TaxiOptionResponse) {
        _selectedRideOption.value = option
    }

    fun clearSelectedRideOption() {
        _selectedRideOption.value = null
    }

    fun clearError() {
        _errorMessage.value = null
    }
}
