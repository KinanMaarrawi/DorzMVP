package com.example.dorzmvp

import android.content.Context
import android.location.Geocoder
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
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import androidx.compose.ui.graphics.Color
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

// Default location (Dubai) and zoom level for the map.
private val DEFAULT_LOCATION = LatLng(25.2048, 55.2708)
private const val DEFAULT_ZOOM = 10f

/**
 * A general-purpose screen for picking a location from a map.
 *
 * This composable provides a map interface where a user can either search for a place
 * or tap directly on the map to select coordinates. The selected `LatLng` is then
 * passed back to the calling screen via the `NavController`'s `SavedStateHandle`
 * using the provided [resultKey].
 *
 * @param navController The navigation controller used to pop back.
 * @param resultKey The key used to set the `LatLng` result in the `SavedStateHandle`.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PickLocationScreen(
    navController: NavController,
    resultKey: String
) {
    // --- State Management ---
    var selectedLatLng by remember { mutableStateOf<LatLng?>(null) }
    var selectedPlaceDisplayName by remember { mutableStateOf<String?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    val predictions = remember { mutableStateListOf<AutocompletePrediction>() }

    // --- UI and Services Initialization ---
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val placesClient = remember { Places.createClient(context) }
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(DEFAULT_LOCATION, DEFAULT_ZOOM)
    }

    // Effect to fetch place predictions as the user types in the search bar.
    LaunchedEffect(searchQuery) {
        if (searchQuery.length > 2) {
            val request = FindAutocompletePredictionsRequest.builder().setQuery(searchQuery).build()
            try {
                val response = placesClient.findAutocompletePredictions(request).await()
                predictions.clear()
                predictions.addAll(response.autocompletePredictions)
            } catch (e: ApiException) {
                Log.e("PickLocationScreen", "Place Autocomplete error: ${e.statusCode}")
                predictions.clear()
            }
        } else {
            predictions.clear()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Pick Location") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFFD32F2F),
                    titleContentColor = Color.White
                )
            )
        }
    ) { paddingValues ->
        Column(modifier = Modifier.padding(paddingValues).fillMaxSize()) {
            // --- Search Area ---
            TextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                label = { Text("Search for a location") },
                modifier = Modifier.fillMaxWidth().padding(8.dp)
            )

            if (predictions.isNotEmpty()) {
                LazyColumn(modifier = Modifier.fillMaxWidth().weight(0.5f)) {
                    items(predictions) { prediction ->
                        Text(
                            text = prediction.getFullText(null).toString(),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    searchQuery = ""
                                    predictions.clear()
                                    fetchPlaceDetailsForPicker(placesClient, prediction.placeId) { place ->
                                        place.latLng?.let { latLng ->
                                            selectedLatLng = latLng
                                            selectedPlaceDisplayName = place.name ?: place.address
                                            coroutineScope.launch {
                                                cameraPositionState.animate(
                                                    CameraUpdateFactory.newLatLngZoom(latLng, 15f), 1000
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

            // --- Map Area ---
            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                GoogleMap(
                    modifier = Modifier.fillMaxSize(),
                    cameraPositionState = cameraPositionState,
                    uiSettings = MapUiSettings(zoomControlsEnabled = true),
                    onMapClick = { latLng ->
                        selectedLatLng = latLng
                        selectedPlaceDisplayName = "Fetching address..."
                        coroutineScope.launch {
                            selectedPlaceDisplayName = getAddressFromMapTapForPicker(context, latLng)
                                ?: "Unknown location"
                        }
                    }
                ) {
                    selectedLatLng?.let { location ->
                        Marker(
                            state = MarkerState(position = location),
                            title = selectedPlaceDisplayName ?: "Selected Location"
                        )
                    }
                }
            }

            // --- Confirmation Area ---
            ConfirmationFooter(
                selectedLatLng = selectedLatLng,
                selectedPlaceDisplayName = selectedPlaceDisplayName,
                onConfirm = {
                    selectedLatLng?.let {
                        // Pass the result back to the previous screen and pop the back stack.
                        navController.previousBackStackEntry?.savedStateHandle?.set(resultKey, it)
                        navController.popBackStack()
                    }
                }
            )
        }
    }
}

/**
 * The bottom section of the screen showing the selected location and the confirm button.
 */
@Composable
private fun ConfirmationFooter(
    selectedLatLng: LatLng?,
    selectedPlaceDisplayName: String?,
    onConfirm: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Determine the text to display based on the current selection state.
        val (displayText, isLocationSelected) = when {
            selectedPlaceDisplayName != null -> selectedPlaceDisplayName to true
            selectedLatLng != null -> "Lat: %.5f, Lng: %.5f".format(Locale.US, selectedLatLng.latitude, selectedLatLng.longitude) to true
            else -> "Tap on the map or search to select a location." to false
        }

        Text(
            text = "Selected: $displayText",
            modifier = Modifier.padding(bottom = 8.dp)
        )

        Button(
            onClick = onConfirm,
            enabled = isLocationSelected,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFFD32F2F),
                contentColor = Color.White
            )
        ) {
            Text("Confirm Location")
        }
    }
}


/**
 * Fetches detailed information for a place using its ID from the Google Places API.
 *
 * @param placesClient The API client.
 * @param placeId The unique ID of the place to fetch.
 * @param onPlaceFetched A callback that returns the fetched [Place] object.
 */
private fun fetchPlaceDetailsForPicker(
    placesClient: PlacesClient,
    placeId: String,
    onPlaceFetched: (Place) -> Unit
) {
    val placeFields = listOf(Place.Field.ID, Place.Field.NAME, Place.Field.LAT_LNG, Place.Field.ADDRESS)
    val request = FetchPlaceRequest.builder(placeId, placeFields).build()

    placesClient.fetchPlace(request)
        .addOnSuccessListener { response ->
            onPlaceFetched(response.place)
        }
        .addOnFailureListener { exception ->
            if (exception is ApiException) {
                Log.e("PickLocationScreen", "Place details fetch error: ${exception.statusCode}")
            }
        }
}

/**
 * Performs a reverse geocode lookup to get a human-readable address from coordinates.
 * This is a suspending function that runs on the IO dispatcher for safety.
 *
 * @param context The application context.
 * @param latLng The coordinates to look up.
 * @return The address string if found, otherwise null.
 */
private suspend fun getAddressFromMapTapForPicker(context: Context, latLng: LatLng): String? {
    return withContext(Dispatchers.IO) {
        val geocoder = Geocoder(context, Locale.getDefault())
        try {
            @Suppress("DEPRECATION")
            val addresses = geocoder.getFromLocation(latLng.latitude, latLng.longitude, 1)
            addresses?.firstOrNull()?.getAddressLine(0)
        } catch (e: IOException) {
            Log.e("PickLocationScreen", "Geocoder failed: ${e.message}")
            null
        }
    }
}
