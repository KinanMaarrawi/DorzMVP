package com.example.dorzmvp

import android.util.Log
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.dorzmvp.network.getDirectionsRoute
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.*
import com.google.maps.android.compose.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// Default locations in case the passed ones are null, to prevent crashes.
private val DUBAI_DEFAULT = LatLng(25.276987, 55.296249)
private val dorzRed = Color(0xFFD32F2F)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RideTrackingScreen(
    navController: NavController,
    startLocation: LatLng?,
    destinationLocation: LatLng?
) {
    val pickup = startLocation ?: DUBAI_DEFAULT
    val dropoff = destinationLocation ?: pickup

    // State to hold the fetched route points from the Directions API.
    var routePoints by remember { mutableStateOf<List<LatLng>>(emptyList()) }
    var isLoadingRoute by remember { mutableStateOf(true) }

    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(pickup, 13f)
    }

    // Fetch the real route when the screen is first composed.
    LaunchedEffect(pickup, dropoff) {
        if (pickup != dropoff) {
            isLoadingRoute = true
            val route = getDirectionsRoute(pickup, dropoff)
            if (route.isNotEmpty()) {
                routePoints = route
                // Adjust camera to show the whole route
                val bounds = LatLngBounds.builder().apply {
                    route.forEach { include(it) }
                }.build()
                cameraPositionState.animate(CameraUpdateFactory.newLatLngBounds(bounds, 100), 1000)
            } else {
                Log.w("RideTrackingScreen", "Failed to get route, showing straight line as fallback.")
                routePoints = listOf(pickup, dropoff) // Fallback to a straight line
            }
            isLoadingRoute = false
        }
    }

    var carPosition by remember { mutableStateOf(pickup) }
    val scope = rememberCoroutineScope()

    // Animate the car's movement along the fetched route points.
    LaunchedEffect(routePoints) {
        if (routePoints.size > 1) {
            scope.launch {
                // Simulate movement by iterating through segments of the route
                for (i in 0 until routePoints.size - 1) {
                    carPosition = routePoints[i]
                    delay(500) // Adjust delay for smoother/faster animation
                }
                carPosition = dropoff // Ensure it ends precisely at the dropoff.
            }
        }
    }

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
            GoogleMap(
                modifier = Modifier.fillMaxSize(),
                cameraPositionState = cameraPositionState
            ) {
                // Draw the route polyline in red.
                if (routePoints.isNotEmpty()) {
                    Polyline(points = routePoints, color = dorzRed, width = 12f)
                }

                // Custom red circle for the pickup location.
                Circle(
                    center = pickup,
                    radius = 50.0, // Radius in meters
                    strokeColor = dorzRed,
                    fillColor = Color.White,
                    strokeWidth = 5f
                )

                // Custom red circle for the drop-off location.
                Circle(
                    center = dropoff,
                    radius = 50.0,
                    strokeColor = dorzRed,
                    fillColor = Color.White,
                    strokeWidth = 5f
                )

                // Marker for the driver's car.
                Marker(
                    state = MarkerState(position = carPosition),
                    title = "Driver",
                    icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE),
                    zIndex = 1f // Ensure car is drawn on top of the line
                )
            }

            if (isLoadingRoute) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            }

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
                        // Themed buttons
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
    }
}
