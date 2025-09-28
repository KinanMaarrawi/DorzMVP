package com.example.dorzmvp

import android.util.Log
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.dorzmvp.network.getDirectionsRoute
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.maps.android.compose.Circle
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.Polyline
import com.google.maps.android.compose.rememberCameraPositionState
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// A default location in Dubai to prevent crashes if navigation arguments are null.
private val DUBAI_DEFAULT = LatLng(25.276987, 55.296249)
// App-specific brand color for consistent styling.
private val dorzRed = Color(0xFFD32F2F)

/**
 * A screen for real-time tracking of a ride's progress on a map.
 *
 * This composable fetches the driving route between a pickup and drop-off point,
 * displays it on a Google Map, and simulates the driver's movement along the path.
 * It also provides a bottom card with driver info and action buttons.
 *
 * @param navController Controller for handling navigation actions, like going back.
 * @param startLocation The geographic coordinates for the ride's starting point.
 * @param destinationLocation The geographic coordinates for the ride's destination.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RideTrackingScreen(
    navController: NavController,
    startLocation: LatLng?,
    destinationLocation: LatLng?
) {
    // Ensure start/end locations are not null to avoid runtime errors.
    val pickup = startLocation ?: DUBAI_DEFAULT
    val dropoff = destinationLocation ?: pickup

    // --- State Management ---
    var routePoints by remember { mutableStateOf<List<LatLng>>(emptyList()) }
    var isLoadingRoute by remember { mutableStateOf(true) }
    var carPosition by remember { mutableStateOf(pickup) }

    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(pickup, 13f)
    }
    val scope = rememberCoroutineScope()

    // --- Effects ---

    // Fetches the route from the Google Directions API when the screen first launches.
    LaunchedEffect(pickup, dropoff) {
        if (pickup != dropoff) {
            isLoadingRoute = true
            val route = getDirectionsRoute(pickup, dropoff)
            if (route.isNotEmpty()) {
                routePoints = route
                // Automatically zoom the camera to fit the entire route on screen.
                val bounds = LatLngBounds.builder().apply { route.forEach(::include) }.build()
                cameraPositionState.animate(CameraUpdateFactory.newLatLngBounds(bounds, 100), 1000)
            } else {
                Log.w("RideTrackingScreen", "Failed to get a valid route. Drawing a straight line as a fallback.")
                routePoints = listOf(pickup, dropoff) // Fallback to a straight line.
            }
            isLoadingRoute = false
        }
    }

    // Animates the car's simulated movement along the fetched route.
    LaunchedEffect(routePoints) {
        if (routePoints.size > 1) {
            scope.launch {
                // Iterate through the points of the route to simulate movement.
                for (i in 0 until routePoints.size - 1) {
                    carPosition = routePoints[i]
                    delay(500) // Delay can be adjusted for faster or slower animation.
                }
                carPosition = dropoff // Ensure the car ends precisely at the destination.
            }
        }
    }

    // --- UI Layout ---
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Tracking Your Ride") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = dorzRed,
                    titleContentColor = Color.White
                )
            )
        }
    ) { paddingValues ->
        Box(Modifier
            .padding(paddingValues)
            .fillMaxSize()) {
            // Main map view
            GoogleMap(
                modifier = Modifier.fillMaxSize(),
                cameraPositionState = cameraPositionState
            ) {
                // Draw the route polyline if available.
                if (routePoints.isNotEmpty()) {
                    Polyline(points = routePoints, color = dorzRed, width = 12f)
                }

                // Decorative circles for pickup and drop-off points.
                Circle(center = pickup, radius = 50.0, strokeColor = dorzRed, fillColor = Color.White, strokeWidth = 5f)
                Circle(center = dropoff, radius = 50.0, strokeColor = dorzRed, fillColor = Color.White, strokeWidth = 5f)

                // Marker representing the driver's car.
                Marker(
                    state = MarkerState(position = carPosition),
                    title = "Driver",
                    icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE),
                    zIndex = 1f // Ensure car is drawn on top of the route line.
                )
            }

            // Show a loading indicator while fetching the route.
            if (isLoadingRoute) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            }

            // Bottom card with driver information and action buttons.
            DriverInfoCard()
        }
    }
}

/**
 * A Card composable displayed at the bottom of the screen with driver details and actions.
 */
@Composable
private fun BoxScope.DriverInfoCard() {
    Card(
        modifier = Modifier
            .align(Alignment.BottomCenter)
            .fillMaxWidth()
            .padding(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Driver: Ahmed | Toyota Camry", style = MaterialTheme.typography.titleMedium)
            Text("ETA: ~5 mins", style = MaterialTheme.typography.bodyMedium)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OutlinedButton(
                    onClick = { /* TODO: Implement ride cancellation logic */ },
                    modifier = Modifier.weight(1f),
                    border = BorderStroke(1.dp, dorzRed)
                ) {
                    Text("Cancel Ride", color = dorzRed)
                }
                Button(
                    onClick = { /* TODO: Implement calling/messaging logic */ },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = dorzRed)
                ) {
                    Text("Contact Driver")
                }
            }
        }
    }
}
