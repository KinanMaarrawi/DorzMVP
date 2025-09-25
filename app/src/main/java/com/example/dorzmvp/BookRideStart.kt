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

val DEFAULT_LOCATION = LatLng(25.2048, 55.2708) // Dubai
const val DEFAULT_ZOOM = 10f

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookRideStartScreen(
    navController: NavController,
    savedAddressViewModel: SavedAddressViewModel
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

    var showSavedAddressesDialog by remember { mutableStateOf(false) }
    val savedAddresses by savedAddressViewModel.savedAddresses.collectAsState()

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
                Log.e("BookRideStart", "Place API Autocomplete error: ${e.statusCode}: ${e.statusMessage}")
                predictions.clear()
            }
        } else {
            predictions.clear()
        }
    }

    if (showSavedAddressesDialog) {
        AlertDialog(
            onDismissRequest = { showSavedAddressesDialog = false },
            title = { Text("Select a Saved Address", color = Color(0xFFD32F2F)) },
            text = {
                if (savedAddresses.isEmpty()) {
                    Text("You have no saved addresses yet.")
                } else {
                    LazyColumn {
                        items(savedAddresses) { address ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                                    .clickable {
                                        selectedLatLng = LatLng(address.latitude, address.longitude)
                                        selectedPlaceDisplayName = address.name
                                        coroutineScope.launch {
                                            cameraPositionState.animate(
                                                CameraUpdateFactory.newLatLngZoom(selectedLatLng!!, 15f),
                                                1000
                                            )
                                        }
                                        showSavedAddressesDialog = false
                                    },
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                            ) {
                                Text(
                                    text = "${address.name} - ${address.address}",
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp)
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showSavedAddressesDialog = false }) {
                    Text("Cancel", color = Color(0xFFD32F2F))
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Select Starting Point") },
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
                onValueChange = { query ->
                    if (!query.contains("\n")) {
                        searchQuery = query
                    }
                },
                label = { Text("Search for a location") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp)
            )
            Button(
                onClick = { showSavedAddressesDialog = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFD32F2F),
                    contentColor = Color.White
                )
            ) {
                Text("Use Saved Address")
            }

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
                                    fetchPlaceDetails(placesClient, prediction.placeId) { place ->
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
                            val address = getAddressFromMapTap(context, latLng)
                            selectedPlaceDisplayName = address ?: "Unknown location"
                        }
                    }
                ) {
                    selectedLatLng?.let { location ->
                        Marker(
                            state = MarkerState(position = location),
                            title = selectedPlaceDisplayName ?: "Starting Point",
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
                    else -> "Tap on the map or search to select a starting point." to false
                }
                Text(
                    text = "Selected: $displayText",
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                Button(
                    onClick = {
                        selectedLatLng?.let { startLocation ->
                            navController.previousBackStackEntry?.savedStateHandle?.set("selectedStartLocation", startLocation)
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
                    Text("Confirm Starting Point")
                }
            }
        }
    }
}

private fun fetchPlaceDetails(
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
                Log.e("BookRideStart", "Place details fetch error: ${exception.statusCode}: ${exception.statusMessage}")
            }
        }
}

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
            Log.e("BookRideStart", "Error getting address from map tap: ${e.message}")
        }
        addressText ?: "Unknown location near %.5f, %.5f".format(Locale.US, latLng.latitude, latLng.longitude)
    }
}
