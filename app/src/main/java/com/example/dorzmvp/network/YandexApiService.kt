package com.example.dorzmvp.network

import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

/**
 * Retrofit service interface for interacting with the Yandex Taxi API.
 */
interface YandexApiService {

    /**
     * Fetches taxi route information including price, distance, and time estimates.
     *
     * @param clid Client ID.
     * @param apikey API Key.
     * @param routeLongLat A string representing the route with longitude and latitude pairs.
     *                     Format: "lon1,lat1~lon2,lat2"
     * @param lang The language for the response (e.g., "en", "ru").
     * @param currency The currency for price display (e.g., "AED", "RUB").
     * @param taxiClass An optional desired class of the taxi (e.g., "econom", "business").
     *                  If null, the API might return all available classes or a default.
     *                  This maps to the `class_str` from the Yandex documentation.
     * @param requirements An optional string for specific requirements (e.g., child_seat).
     *                     This is the `req_str` from the Yandex documentation.
     * @return A [Response] wrapping the [YandexTaxiInfoResponse].
     */
    @GET("taxi_info") // Endpoint path
    suspend fun getTaxiInfo(
        @Query("clid") clid: String,
        @Query("apikey") apikey: String,
        @Query("rll") routeLongLat: String,
        @Query("lang") lang: String, // Added language parameter
        @Query("currency") currency: String, // Added currency parameter
        @Query("class") taxiClass: String? = null, // Ensured this is nullable
        @Query("req") requirements: String? = null
    ): Response<YandexTaxiInfoResponse>
}
