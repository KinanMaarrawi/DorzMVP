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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
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
import com.example.dorzmvp.ui.viewmodel.SavedAddressViewModel
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
 * A screen for selecting a destination location for a ride.
 * Users can search for a place, select from saved addresses, or tap on the map.
 * The selected location is then returned to the previous screen.
 *
 * @param navController The NavController for navigation.
 * @param savedAddressViewModel ViewModel to access the user's saved addresses.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookRideDestinationScreen(
    navController: NavController,
    savedAddressViewModel: SavedAddressViewModel
) {
    // --- State Management ---
    var selectedLatLng by remember { mutableStateOf<LatLng?>(null) }
    var selectedPlaceDisplayName by remember { mutableStateOf<String?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    val predictions = remember { mutableStateListOf<AutocompletePrediction>() }
    var showSavedAddressesDialog by remember { mutableStateOf(false) }

    // --- UI and Services Initialization ---
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val placesClient = remember { Places.createClient(context) }
    val savedAddresses by savedAddressViewModel.savedAddresses.collectAsState()
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(DEFAULT_LOCATION, DEFAULT_ZOOM)
    }

    /**
     * Updates the selected location and animates the map camera to it.
     */
    fun selectAndMoveToLocation(latLng: LatLng, name: String?) {
        selectedLatLng = latLng
        selectedPlaceDisplayName = name
        coroutineScope.launch {
            cameraPositionState.animate(
                update = CameraUpdateFactory.newLatLngZoom(latLng, 15f),
                durationMs = 1000
            )
        }
    }

    // --- Effects ---

    // Fetches autocomplete predictions from the Places API as the user types.
    LaunchedEffect(searchQuery) {
        if (searchQuery.length > 2) {
            val request = FindAutocompletePredictionsRequest.builder().setQuery(searchQuery).build()
            try {
                val response = placesClient.findAutocompletePredictions(request).await()
                predictions.clear()
                predictions.addAll(response.autocompletePredictions)
            } catch (e: ApiException) {
                Log.e("BookRideDestination", "Place Autocomplete error: ${e.statusCode}")
                predictions.clear() // Clear predictions on error
            }
        } else {
            predictions.clear()
        }
    }

    // --- UI Composables ---

    if (showSavedAddressesDialog) {
        SavedAddressesDialog(
            savedAddresses = savedAddresses,
            onDismiss = { showSavedAddressesDialog = false },
            onAddressSelected = { address ->
                selectAndMoveToLocation(LatLng(address.latitude, address.longitude), address.name)
                showSavedAddressesDialog = false
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Select Destination") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFFD32F2F),
                    titleContentColor = Color.White
                )
            )
        }
    ) { paddingValues ->
        Column(modifier = Modifier.padding(paddingValues).fillMaxSize()) {
            // Search field and "Use Saved Address" button
            LocationSearchHeader(
                searchQuery = searchQuery,
                onSearchQueryChange = { searchQuery = it },
                onShowSavedAddresses = { showSavedAddressesDialog = true }
            )

            // Display search predictions if available
            if (predictions.isNotEmpty()) {
                LazyColumn(modifier = Modifier.fillMaxWidth().weight(0.5f)) {
                    items(predictions) { prediction ->
                        Text(
                            text = prediction.getFullText(null).toString(),
                            modifier = Modifier.fillMaxWidth().clickable {
                                searchQuery = "" // Clear search field
                                predictions.clear() // Hide predictions list
                                fetchPlaceDetails(placesClient, prediction.placeId) { place ->
                                    place.latLng?.let { latLng ->
                                        selectAndMoveToLocation(latLng, place.name ?: place.address)
                                    }
                                }
                            }.padding(12.dp)
                        )
                    }
                }
            }

            // Google Map view
            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                GoogleMap(
                    modifier = Modifier.fillMaxSize(),
                    cameraPositionState = cameraPositionState,
                    uiSettings = MapUiSettings(zoomControlsEnabled = true),
                    onMapClick = { latLng ->
                        selectAndMoveToLocation(latLng, "Fetching address...")
                        coroutineScope.launch {
                            selectedPlaceDisplayName = getAddressFromMapTap(context, latLng)
                        }
                    }
                ) {
                    selectedLatLng?.let { location ->
                        Marker(
                            state = MarkerState(position = location),
                            title = selectedPlaceDisplayName ?: "Destination"
                        )
                    }
                }
            }

            // Bottom section with selected location text and confirm button
            ConfirmationFooter(
                selectedLatLng = selectedLatLng,
                selectedPlaceDisplayName = selectedPlaceDisplayName,
                onConfirm = {
                    selectedLatLng?.let { destinationLocation ->
                        // Pass the selected LatLng back to the previous screen
                        navController.previousBackStackEntry
                            ?.savedStateHandle
                            ?.set("selectedDestinationLocation", destinationLocation)
                        navController.popBackStack()
                    }
                }
            )
        }
    }
}

/**
 * A dialog to display and select from a list of saved addresses.
 */
@Composable
private fun SavedAddressesDialog(
    savedAddresses: List<com.example.dorzmvp.db.SavedAddress>,
    onDismiss: () -> Unit,
    onAddressSelected: (com.example.dorzmvp.db.SavedAddress) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Select a Saved Address", color = Color(0xFFD32F2F)) },
        text = {
            if (savedAddresses.isEmpty()) {
                Text("You have no saved addresses yet.")
            } else {
                LazyColumn {
                    items(savedAddresses) { address ->
                        Card(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                                .clickable { onAddressSelected(address) },
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Text(
                                text = "${address.name} - ${address.address}",
                                modifier = Modifier.fillMaxWidth().padding(16.dp)
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = Color(0xFFD32F2F))
            }
        }
    )
}

/**
 * The top section of the screen containing the search field and saved address button.
 */
@Composable
private fun LocationSearchHeader(
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onShowSavedAddresses: () -> Unit
) {
    TextField(
        value = searchQuery,
        onValueChange = { if (!it.contains("\n")) onSearchQueryChange(it) },
        label = { Text("Search for a destination") },
        singleLine = true,
        modifier = Modifier.fillMaxWidth().padding(8.dp)
    )
    Button(
        onClick = onShowSavedAddresses,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F), contentColor = Color.White)
    ) {
        Text("Use Saved Address")
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
        val (displayText, isLocationSelected) = when {
            selectedPlaceDisplayName != null -> selectedPlaceDisplayName to true
            selectedLatLng != null -> "Lat: %.5f, Lng: %.5f".format(
                Locale.US, selectedLatLng.latitude, selectedLatLng.longitude
            ) to true
            else -> "Tap on the map or search to select a destination." to false
        }

        Text(text = "Selected: $displayText", modifier = Modifier.padding(bottom = 8.dp))

        Button(
            onClick = onConfirm,
            enabled = isLocationSelected,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFFD32F2F),
                contentColor = Color.White
            )
        ) {
            Text("Confirm Destination")
        }
    }
}


// --- Helper Functions ---

/**
 * Fetches detailed information for a place using its ID from the Google Places API.
 */
private fun fetchPlaceDetails(
    placesClient: PlacesClient,
    placeId: String,
    onPlaceFetched: (Place) -> Unit
) {
    val placeFields = listOf(Place.Field.ID, Place.Field.NAME, Place.Field.LAT_LNG, Place.Field.ADDRESS)
    val request = FetchPlaceRequest.builder(placeId, placeFields).build()

    placesClient.fetchPlace(request)
        .addOnSuccessListener { response -> onPlaceFetched(response.place) }
        .addOnFailureListener { exception ->
            if (exception is ApiException) {
                Log.e("BookRideDestination", "Place details fetch error: ${exception.statusCode}")
            }
        }
}

/**
 * Performs a reverse geocode lookup to get a human-readable address from LatLng coordinates.
 * This is a suspending function that runs on the IO dispatcher.
 */
private suspend fun getAddressFromMapTap(context: Context, latLng: LatLng): String {
    return withContext(Dispatchers.IO) {
        val geocoder = Geocoder(context, Locale.getDefault())
        try {
            // The getFromLocation method is deprecated on API 33+, but this is a safe fallback.
            @Suppress("DEPRECATION")
            val addresses = geocoder.getFromLocation(latLng.latitude, latLng.longitude, 1)
            addresses?.firstOrNull()?.getAddressLine(0)
        } catch (e: IOException) {
            Log.e("BookRideDestination", "Error getting address from map tap: ${e.message}")
            null
        } ?: "Unknown location near %.5f, %.5f".format(Locale.US, latLng.latitude, latLng.longitude)
    }
}
