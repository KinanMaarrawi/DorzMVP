package com.example.dorzmvp.network

import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

/**
 * Retrofit service interface for interacting with the Yandex Taxi API.
 * This defines the HTTP API endpoints and their request/response structures.
 */
interface YandexApiService {

    /**
     * Fetches taxi route information including available ride options, prices, and time estimates.
     * This corresponds to the `taxi_info` endpoint in the Yandex API.
     *
     * @param clid The client ID for API authentication.
     * @param apikey The API key for authentication.
     * @param routeLongLat A string defining the route. Format: "lon1,lat1~lon2,lat2".
     * @param lang The response language (e.g., "en").
     * @param currency The currency for pricing (e.g., "AED").
     * @param taxiClass An optional filter for a specific taxi class (e.g., "econom", "business").
     * @param requirements An optional string for specific ride requirements.
     * @return A Retrofit [Response] wrapping the [YandexTaxiInfoResponse] data class.
     */
    @GET("taxi_info")
    suspend fun getTaxiInfo(
        @Query("clid") clid: String,
        @Query("apikey") apikey: String,
        @Query("rll") routeLongLat: String,
        @Query("lang") lang: String,
        @Query("currency") currency: String,
        @Query("class") taxiClass: String? = null,
        @Query("req") requirements: String? = null
    ): Response<YandexTaxiInfoResponse>
}
