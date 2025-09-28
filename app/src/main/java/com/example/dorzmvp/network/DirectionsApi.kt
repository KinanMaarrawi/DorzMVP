package com.example.dorzmvp.network

import android.util.Log
import com.example.dorzmvp.BuildConfig
import com.google.android.gms.maps.model.LatLng
import com.google.maps.DirectionsApi
import com.google.maps.GeoApiContext
import com.google.maps.model.TravelMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Fetches a driving route from the Google Directions API.
 *
 * This function makes a network request on the IO dispatcher to get directions
 * between an origin and a destination, then decodes the resulting polyline
 * into a list of `LatLng` points for drawing on a map.
 *
 * @param origin The starting geographic point of the route.
 * @param destination The ending geographic point of the route.
 * @return A list of `LatLng` points representing the route, or an empty list if the request fails.
 */
suspend fun getDirectionsRoute(origin: LatLng, destination: LatLng): List<LatLng> {
    // Perform the network operation on a background thread to avoid blocking the UI.
    return withContext(Dispatchers.IO) {
        try {
            // Set up the API context using the key from BuildConfig.
            val geoApiContext = GeoApiContext.Builder()
                .apiKey(BuildConfig.MAPS_API_KEY)
                .build()

            // Build and execute the directions request.
            val directionsResult = DirectionsApi.newRequest(geoApiContext)
                .origin(com.google.maps.model.LatLng(origin.latitude, origin.longitude))
                .destination(com.google.maps.model.LatLng(destination.latitude, destination.longitude))
                .mode(TravelMode.DRIVING)
                .await() // Suspends the coroutine until the result is available.

            // Check if any routes were returned.
            if (directionsResult.routes.isNotEmpty()) {
                // Get the first route from the results.
                val route = directionsResult.routes[0]
                // Decode the compact polyline string into a list of coordinates.
                val decodedPath = route.overviewPolyline.decodePath()

                // Convert the Google-native LatLng objects to the map-compatible LatLng objects.
                return@withContext decodedPath.map { latLng ->
                    LatLng(latLng.lat, latLng.lng)
                }
            } else {
                Log.w("DirectionsApi", "No routes found between origin and destination.")
                return@withContext emptyList()
            }
        } catch (e: Exception) {
            Log.e("DirectionsApi", "Error fetching directions: ${e.message}", e)
            return@withContext emptyList() // Return an empty list on any exception.
        }
    }
}
