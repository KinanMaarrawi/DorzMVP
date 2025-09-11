package com.example.dorzmvp

import android.content.Context
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
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

/** Default latitude and longitude, initially set to Dubai. Used to center the map when it first loads. */
val DEFAULT_LOCATION = LatLng(25.2048, 55.2708) // Dubai
/** Default zoom level for the map. */
const val DEFAULT_ZOOM = 10f

/**
 * A screen that allows the user to select a starting point for their ride.
 * It features a map for visual selection, a search bar for finding locations via text input
 * using the Google Places API, and a confirmation button.
 *
 * @param navController The [NavController] used for navigating back to the previous screen
 *                      after a location is selected and confirmed.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookRideStartScreen(navController: NavController) {
    // State to hold the latitude and longitude of the location currently selected by the user.
    var selectedLatLng by remember { mutableStateOf<LatLng?>(null) }
    // State for managing the Google Map's camera position (location, zoom, tilt, bearing).
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(DEFAULT_LOCATION, DEFAULT_ZOOM)
    }
    // Configuration for the Google Map's UI elements (e.g., zoom controls).
    val uiSettings by remember { mutableStateOf(MapUiSettings(zoomControlsEnabled = true)) }
    // Configuration for the Google Map's properties (e.g., enabling/disabling "My Location" layer).
    val mapProperties by remember { mutableStateOf(MapProperties(isMyLocationEnabled = false)) } // MyLocation is off as search/tap is the primary interaction.

    // --- Places API Search States ---
    /** State for the user's current text input in the search field. */
    var searchQuery by remember { mutableStateOf("") }
    /** A mutable list to hold [AutocompletePrediction] objects received from the Places API. */
    val predictions = remember { mutableStateListOf<AutocompletePrediction>() }
    /** The current Android [Context], required by the Places API. */
    val context = LocalContext.current
    /** Client for interacting with the Google Places API. Initialized once and remembered. */
    val placesClient = remember { Places.createClient(context) }
    /** Coroutine scope for launching asynchronous operations like API calls. */
    val coroutineScope = rememberCoroutineScope()

    /**
     * A [LaunchedEffect] that triggers whenever the [searchQuery] changes.
     * If the query is longer than 2 characters, it initiates an autocomplete request to the Places API.
     * The results (predictions) are then used to update the [predictions] list.
     */
    LaunchedEffect(searchQuery) {
        if (searchQuery.length > 2) { // Start searching only after a few characters are typed.
            val request = FindAutocompletePredictionsRequest.builder()
                .setQuery(searchQuery)
                .build()
            try {
                val response = placesClient.findAutocompletePredictions(request).await() // Suspending call
                predictions.clear()
                predictions.addAll(response.autocompletePredictions)
            } catch (e: ApiException) {
                Log.e("BookRideStart", "Place API Autocomplete error: " + e.statusCode + ": " + e.statusMessage)
            }
        } else {
            predictions.clear() // Clear predictions if the query is too short.
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Select Starting Point") })
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
        ) {
            // Text field for user to input their desired location.
            TextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                label = { Text("Search for a location") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp)
            )

            // Displays the list of autocomplete predictions if any are available.
            if (predictions.isNotEmpty()) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(0.5f) // Adjust weight to control how much space the list takes vs the map.
                ) {
                    items(predictions) { prediction ->
                        Text(
                            text = prediction.getFullText(null).toString(), // Display the full address of the prediction.
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { // When a prediction is clicked:
                                    searchQuery = "" // Clear the search query.
                                    predictions.clear() // Clear the list of predictions.
                                    // Fetch detailed information for the selected place.
                                    fetchPlaceDetails(placesClient, prediction.placeId, context) { place ->
                                        place.latLng?.let { latLng ->
                                            selectedLatLng = latLng // Update the selected location.
                                            // Animate the map camera to the newly selected location.
                                            coroutineScope.launch {
                                                cameraPositionState.animate(
                                                    CameraUpdateFactory.newLatLngZoom(latLng, 15f), // Zoom closer to the selected place.
                                                    1000 // Animation duration in milliseconds.
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

            // Box container for the map.
            Box(
                modifier = Modifier
                    .weight(1f) // The map takes up the remaining vertical space.
                    .fillMaxWidth()
            ) {
                GoogleMap(
                    modifier = Modifier.fillMaxSize(),
                    cameraPositionState = cameraPositionState,
                    properties = mapProperties,
                    uiSettings = uiSettings,
                    // Allows the user to select a location by tapping directly on the map.
                    onMapClick = { latLng ->
                        selectedLatLng = latLng
                        Log.d("MapClick", "Map tapped. Selected: $latLng")
                    }
                ) {
                    // A marker is displayed on the map at the currently selected location.
                    selectedLatLng?.let { location ->
                        Marker(
                            state = MarkerState(position = location),
                            title = "Starting Point",
                            snippet = "Lat: ${location.latitude}, Lng: ${location.longitude}"
                        )
                    }
                }
            }

            // Section at the bottom to display selected coordinates and the confirmation button.
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (selectedLatLng != null) {
                    Text(
                        text = "Selected: %.5f, %.5f".format(
                            selectedLatLng!!.latitude,
                            selectedLatLng!!.longitude
                        ),
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                } else {
                    Text(
                        text = "Tap on the map or search to select a starting point.",
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }

                // Button to confirm the selected location and navigate back.
                Button(
                    onClick = {
                        selectedLatLng?.let { startLocation ->
                            Log.i("BookRideStart", "Confirmed starting point: $startLocation")
                            // Pass the selected LatLng back to the previous screen using SavedStateHandle.
                            navController.previousBackStackEntry?.savedStateHandle?.set("selectedStartLocation", startLocation)
                            navController.popBackStack() // Navigate back.
                        }
                    },
                    enabled = selectedLatLng != null, // Button is enabled only if a location has been selected.
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Confirm Starting Point")
                }
            }
        }
    }
}

/**
 * Fetches detailed information for a place given its ID using the Places API.
 *
 * @param placesClient The [PlacesClient] instance to use for the API call.
 * @param placeId The unique identifier of the place to fetch details for.
 * @param context The current Android [Context].
 * @param onPlaceFetched A callback function that is invoked with the fetched [Place] object upon success.
 */
private fun fetchPlaceDetails(placesClient: PlacesClient, placeId: String, context: Context, onPlaceFetched: (Place) -> Unit) {
    // Define which place fields to request (e.g., ID, name, LatLng, address).
    val placeFields = listOf(Place.Field.ID, Place.Field.NAME, Place.Field.LAT_LNG, Place.Field.ADDRESS)
    val request = FetchPlaceRequest.builder(placeId, placeFields).build()

    placesClient.fetchPlace(request).addOnSuccessListener { response ->
        val place = response.place
        Log.i("BookRideStart", "Place details fetched: " + place.name)
        onPlaceFetched(place) // Pass the fetched Place object to the callback.
    }.addOnFailureListener { exception ->
        if (exception is ApiException) {
            Log.e("BookRideStart", "Place details fetch error: " + exception.statusCode + ": " + exception.statusMessage)
        }
    }
}
