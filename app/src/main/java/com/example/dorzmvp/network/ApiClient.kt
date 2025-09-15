// This file defines the ApiClient object, which is responsible for setting up and providing
// a centralized instance of the Retrofit service used for making network requests.
// It follows the Singleton pattern to ensure that only one instance of the Retrofit service
// (and its underlying OkHttpClient) is created and used throughout the application.
// This approach is efficient and helps in managing network configurations, like base URLs
// and interceptors, in a single place.

package com.example.dorzmvp.network

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

/**
 * Singleton object for providing an instance of the Retrofit API service ([YandexApiService]).
 * It configures Retrofit with a base URL, a JSON converter (Gson), and an OkHttpClient
 * that includes an HTTP logging interceptor for debugging network traffic.
 */
object ApiClient {

    // Base URL for all Yandex Taxi Info API endpoints.
    private const val YANDEX_TAXI_BASE_URL = "https://taxi-routeinfo.taxi.yandex.net/"

    /**
     * Lazily creates and provides a singleton instance of [YandexApiService].
     * The `lazy` delegate ensures that the Retrofit setup (which can be somewhat expensive)
     * is performed only when `yandexApiService` is first accessed, and the same instance
     * is reused for all subsequent accesses.
     *
     * The Retrofit instance is configured with:
     * - The base URL for the Yandex API.
     * - An OkHttpClient that includes an `HttpLoggingInterceptor`.
     * - A `GsonConverterFactory` for automatic serialization/deserialization of JSON data to/from Kotlin data classes.
     */
    val yandexApiService: YandexApiService by lazy {

        // Create an HttpLoggingInterceptor to log network request and response data.
        // This is extremely useful for debugging network issues as it shows:
        // - Request URLs, methods, headers, and bodies.
        // - Response status codes, headers, and bodies.
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            // Set the logging level to BODY to see the most detail.
            // Other levels include NONE, BASIC, HEADERS.
            // For production builds, this might be set to a less verbose level or removed.
            level = HttpLoggingInterceptor.Level.BODY
        }

        // Create an OkHttpClient and add the logging interceptor.
        // OkHttpClient is the underlying HTTP client that Retrofit uses to make network calls.
        // Custom configurations, like adding interceptors, timeouts, etc., are done here.
        val httpClient = OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor) // Adds the logger to the chain of interceptors.
            .build()

        // Build the Retrofit instance.
        Retrofit.Builder()
            .baseUrl(YANDEX_TAXI_BASE_URL) // Sets the base URL for all API requests.
            .client(httpClient) // Sets the custom OkHttpClient with the logging interceptor.
            .addConverterFactory(GsonConverterFactory.create()) // Adds Gson for JSON processing.
            .build()
            .create(YandexApiService::class.java) // Creates an implementation of the YandexApiService interface.
    }
}
