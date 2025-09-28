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
 * Fetches a route from the Google Directions API and returns a list of LatLng points.
 *
 * @param origin The starting point of the route.
 * @param destination The ending point of the route.
 * @return A list of LatLng points representing the decoded polyline of the route, or an empty list if it fails.
 */
suspend fun getDirectionsRoute(origin: LatLng, destination: LatLng): List<LatLng> {
    // This function must be called from a coroutine scope with IO dispatcher
    return withContext(Dispatchers.IO) {
        try {
            // Set up the context for the Directions API call using the API key from BuildConfig
            val geoApiContext = GeoApiContext.Builder()
                .apiKey(BuildConfig.MAPS_API_KEY) // Use the same key as your maps
                .build()

            // Request directions
            val directionsResult = DirectionsApi.newRequest(geoApiContext)
                .origin(com.google.maps.model.LatLng(origin.latitude, origin.longitude))
                .destination(com.google.maps.model.LatLng(destination.latitude, destination.longitude))
                .mode(TravelMode.DRIVING)
                .await()

            // Check if we have any routes
            if (directionsResult.routes.isNotEmpty()) {
                // Get the first route and decode its overview polyline
                val route = directionsResult.routes[0]
                val decodedPath = route.overviewPolyline.decodePath()

                // Convert the decoded path to a list of Jetpack Compose LatLng objects
                return@withContext decodedPath.map { latLng ->
                    LatLng(latLng.lat, latLng.lng)
                }
            } else {
                Log.w("DirectionsApi", "No routes found between origin and destination.")
                return@withContext emptyList()
            }
        } catch (e: Exception) {
            Log.e("DirectionsApi", "Error fetching directions: ${e.message}", e)
            return@withContext emptyList() // Return an empty list on failure
        }
    }
}
