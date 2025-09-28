package com.example.dorzmvp.network

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

/**
 * A singleton object that provides a configured Retrofit service instance.
 *
 * This client is responsible for creating and managing the network layer for the Yandex API.
 * It uses a lazy-initialized singleton pattern to ensure only one instance of Retrofit
 * and its underlying OkHttpClient is created, which is efficient for network operations.
 */
object ApiClient {

    // The base URL for the Yandex Taxi Info API. All API calls will be relative to this.
    private const val YANDEX_TAXI_BASE_URL = "https://taxi-routeinfo.taxi.yandex.net/"

    /**
     * Lazily provides a singleton instance of [YandexApiService].
     *
     * The `lazy` delegate ensures the Retrofit instance is created only upon first access
     * and is then reused, improving performance. The client is configured with an
     * HTTP logger for debugging and a Gson converter for JSON serialization.
     */
    val yandexApiService: YandexApiService by lazy {

        // The logging interceptor captures and logs network request and response details.
        // This is invaluable for debugging network calls during development.
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        // The OkHttpClient handles the actual HTTP requests. We add our logger here.
        val httpClient = OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            .build()

        // The Retrofit builder configures the API service.
        Retrofit.Builder()
            .baseUrl(YANDEX_TAXI_BASE_URL)
            .client(httpClient) // Use the custom client with the logger.
            .addConverterFactory(GsonConverterFactory.create()) // Use Gson to handle JSON.
            .build()
            .create(YandexApiService::class.java) // Create an implementation of our API interface.
    }
}
