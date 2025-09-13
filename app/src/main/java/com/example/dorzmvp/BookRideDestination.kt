package com.example.dorzmvp

import android.content.Context
import android.location.Geocoder
import android.os.Build
import android.util.Log
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.AutocompletePrediction
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.api.net.FetchPlaceRequest
import com.google.android.libraries.places.api.net.FindAutocompletePredictionsRequest
import com.google.android.libraries.places.api.net.PlacesClient
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.io.IOException
import java.util.Locale

// DEFAULT_LOCATION and DEFAULT_ZOOM can be reused or defined specifically for destination context if needed.
// For now, re-using the same constants from BookRideStartScreen for consistency.
// This is essentially the same as the code in BookRideStart.kt, just changed to be for destination instead of pickup point

/**
 * A Composable screen that allows the user to select a destination for their ride.
 * It mirrors the functionality of [BookRideStartScreen] but is contextually for choosing a destination.
 * Features a map, a search bar (Places API), and a confirmation button.
 *
 * @param navController The [NavController] used for navigating back, typically to [BookARideMainUI],
 *                      after a destination is selected and confirmed.
 */
@OptIn(ExperimentalMaterial3Api::class) // Required for TopAppBar usage.
@Composable
fun BookRideDestinationScreen(navController: NavController) {
    var selectedLatLng by remember { mutableStateOf<LatLng?>(null) }
    var selectedPlaceDisplayName by remember { mutableStateOf<String?>(null) }

    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(DEFAULT_LOCATION, DEFAULT_ZOOM)
    }
    val uiSettings by remember { mutableStateOf(MapUiSettings(zoomControlsEnabled = true)) }
    val mapProperties by remember { mutableStateOf(MapProperties(isMyLocationEnabled = false)) }

    var searchQuery by remember { mutableStateOf("") }
    val predictions = remember { mutableStateListOf<AutocompletePrediction>() }

    val context = LocalContext.current
    val placesClient = remember { Places.createClient(context) }
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(searchQuery) {
        if (searchQuery.length > 2) {
            val request = FindAutocompletePredictionsRequest.builder()
                .setQuery(searchQuery)
                .build()
            try {
                val response = placesClient.findAutocompletePredictions(request).await()
                predictions.clear()
                predictions.addAll(response.autocompletePredictions)
            } catch (e: ApiException) {
                Log.e("BookRideDestination", "Place API Autocomplete error: ${e.statusCode}: ${e.statusMessage}")
                predictions.clear()
            }
        } else {
            predictions.clear()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Select Destination") })
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
        ) {
            TextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                label = { Text("Search for a destination") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp)
            )

            if (predictions.isNotEmpty()) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(0.5f)
                ) {
                    items(predictions) { prediction ->
                        Text(
                            text = prediction.getFullText(null).toString(),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    searchQuery = ""
                                    predictions.clear()
                                    fetchPlaceDetails(placesClient, prediction.placeId, context) { place -> // Re-using fetchPlaceDetails
                                        place.latLng?.let { latLng ->
                                            selectedLatLng = latLng
                                            selectedPlaceDisplayName = place.name ?: place.address
                                            coroutineScope.launch {
                                                cameraPositionState.animate(
                                                    CameraUpdateFactory.newLatLngZoom(latLng, 15f),
                                                    1000
                                                )
                                            }
                                        }
                                    }
                                }
                                .padding(12.dp)
                        )
                    }
                }
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                GoogleMap(
                    modifier = Modifier.fillMaxSize(),
                    cameraPositionState = cameraPositionState,
                    properties = mapProperties,
                    uiSettings = uiSettings,
                    onMapClick = { latLng ->
                        selectedLatLng = latLng
                        selectedPlaceDisplayName = "Fetching address..."
                        coroutineScope.launch {
                            // Re-using getAddressFromMapTap
                            val address = getAddressFromMapTap(context, latLng)
                            selectedPlaceDisplayName = address
                        }
                        Log.d("BookRideDestination", "Map tapped. Selected: $latLng")
                    }
                ) {
                    selectedLatLng?.let { location ->
                        Marker(
                            state = MarkerState(position = location),
                            title = selectedPlaceDisplayName ?: "Destination", // Changed default title
                            snippet = selectedPlaceDisplayName ?: "Lat: ${location.latitude}, Lng: ${location.longitude}"
                        )
                    }
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                val (displayText, isLocationSelected) = when {
                    selectedPlaceDisplayName != null -> selectedPlaceDisplayName to true
                    selectedLatLng != null -> "Lat: %.5f, Lng: %.5f".format(selectedLatLng!!.latitude, selectedLatLng!!.longitude) to true
                    else -> "Tap on the map or search to select a destination." to false
                }
                Text(
                    text = "Selected: $displayText",
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                Button(
                    onClick = {
                        selectedLatLng?.let { destinationLocation ->
                            Log.i("BookRideDestination", "Confirmed destination: $destinationLocation, Name: $selectedPlaceDisplayName")
                            navController.previousBackStackEntry?.savedStateHandle?.set("selectedDestinationLocation", destinationLocation)
                            // Optionally, pass the display name back:
                            // navController.previousBackStackEntry?.savedStateHandle?.set("selectedDestinationLocationName", selectedPlaceDisplayName)
                            navController.popBackStack()
                        }
                    },
                    enabled = isLocationSelected,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Confirm Destination") // Changed button text
                }
            }
        }
    }
}

// Note: fetchPlaceDetails and getAddressFromMapTap are identical to those in BookRideStart.kt.
// They could be moved to a common utility file if desired, but for now, they are duplicated for simplicity.
// Consider them as private utility functions for this specific screen's implementation.

/**
 * Fetches detailed information for a place given its ID using the Google Places API.
 */
private fun fetchPlaceDetails(
    placesClient: PlacesClient,
    placeId: String,
    context: Context,
    onPlaceFetched: (Place) -> Unit
) {
    val placeFields = listOf(Place.Field.ID, Place.Field.NAME, Place.Field.LAT_LNG, Place.Field.ADDRESS)
    val request = FetchPlaceRequest.builder(placeId, placeFields).build()

    placesClient.fetchPlace(request)
        .addOnSuccessListener { response ->
            val place = response.place
            Log.i("BookRideDestination", "Place details fetched: ${place.name ?: "Unknown name"}") // Log context updated
            onPlaceFetched(place)
        }
        .addOnFailureListener { exception ->
            if (exception is ApiException) {
                Log.e("BookRideDestination", "Place details fetch error: ${exception.statusCode}: ${exception.statusMessage}") // Log context updated
            }
        }
}

/**
 * Helper function to get a human-readable address from LatLng coordinates using Android's [Geocoder].
 */
private suspend fun getAddressFromMapTap(context: Context, latLng: LatLng): String {
    return withContext(Dispatchers.IO) {
        val geocoder = Geocoder(context, Locale.getDefault())
        var addressText: String? = null
        try {
            @Suppress("DEPRECATION")
            val addresses = geocoder.getFromLocation(latLng.latitude, latLng.longitude, 1)
            if (addresses?.isNotEmpty() == true) {
                addressText = addresses[0].getAddressLine(0)
            }
        } catch (e: IOException) {
            Log.e("BookRideDestination", "Error getting address from map tap: ${e.message}") // Log context updated
        } catch (e: IllegalArgumentException) {
            Log.e("BookRideDestination", "Invalid LatLng passed to geocoder for map tap: ${e.message}") // Log context updated
        }
        addressText ?: "Unknown location near %.5f, %.5f".format(latLng.latitude, latLng.longitude)
    }
}
