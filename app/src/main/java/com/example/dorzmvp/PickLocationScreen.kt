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
import androidx.compose.material3.ButtonDefaults // Added import
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
// import androidx.compose.material3.MaterialTheme // Commented out if not directly used after changes
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
import androidx.compose.ui.graphics.Color // Added import
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

// DEFAULT_LOCATION and DEFAULT_ZOOM can be reused or defined specifically if needed.
// For now, re-using the same constants from BookRideStartScreen for consistency.
// val DEFAULT_LOCATION = LatLng(25.2048, 55.2708) // Dubai
// const val DEFAULT_ZOOM = 10f

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PickLocationScreen(
    navController: NavController,
    resultKey: String // Key to pass the LatLng result back
) {
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
                Log.e("PickLocationScreen", "Place API Autocomplete error: ${e.statusCode}: ${e.statusMessage}")
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
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
        ) {
            TextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                label = { Text("Search for a location") },
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
                                    fetchPlaceDetailsForPicker(placesClient, prediction.placeId) { place ->
                                        place.latLng?.let {
                                            selectedLatLng = it
                                            selectedPlaceDisplayName = place.name ?: place.address
                                            coroutineScope.launch {
                                                cameraPositionState.animate(
                                                    CameraUpdateFactory.newLatLngZoom(it, 15f),
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
                            val address = getAddressFromMapTapForPicker(context, latLng)
                            selectedPlaceDisplayName = address ?: "Unknown location"
                        }
                        Log.d("PickLocationScreen", "Map tapped. Selected: $latLng")
                    }
                ) {
                    selectedLatLng?.let { location ->
                        Marker(
                            state = MarkerState(position = location),
                            title = selectedPlaceDisplayName ?: "Selected Location",
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
                    selectedLatLng != null -> "Lat: %.5f, Lng: %.5f".format(Locale.US, selectedLatLng!!.latitude, selectedLatLng!!.longitude) to true
                    else -> "Tap on the map or search to select a location." to false
                }
                Text(
                    text = "Selected: $displayText",
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                Button(
                    onClick = {
                        selectedLatLng?.let {
                            Log.i("PickLocationScreen", "Confirmed location: $it, Name: $selectedPlaceDisplayName for key $resultKey")
                            navController.previousBackStackEntry?.savedStateHandle?.set(resultKey, it)
                            navController.popBackStack()
                        }
                    },
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
    }
}

private fun fetchPlaceDetailsForPicker(
    placesClient: PlacesClient,
    placeId: String,
    onPlaceFetched: (Place) -> Unit
) {
    val placeFields = listOf(Place.Field.ID, Place.Field.NAME, Place.Field.LAT_LNG, Place.Field.ADDRESS)
    val request = FetchPlaceRequest.builder(placeId, placeFields).build()

    placesClient.fetchPlace(request)
        .addOnSuccessListener { response ->
            val place = response.place
            Log.i("PickLocationScreen", "Place details fetched: ${place.name ?: "Unknown name"}")
            onPlaceFetched(place)
        }
        .addOnFailureListener { exception ->
            if (exception is ApiException) {
                Log.e("PickLocationScreen", "Place details fetch error: ${exception.statusCode}: ${exception.statusMessage}")
            }
        }
}

private suspend fun getAddressFromMapTapForPicker(context: Context, latLng: LatLng): String? {
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
            Log.e("PickLocationScreen", "Error in getAddressFromMapTapForPicker (Geocoder): ${e.message}")
        } catch (e: IllegalArgumentException) {
             Log.e("PickLocationScreen", "Invalid LatLng in getAddressFromMapTapForPicker: ${e.message}")
        }
        addressText
    }
}
