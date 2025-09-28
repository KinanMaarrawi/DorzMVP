package com.example.dorzmvp

import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.dorzmvp.db.RideHistory
import com.example.dorzmvp.ui.viewmodel.BookRideViewModel
import com.example.dorzmvp.ui.viewmodel.RideHistoryViewModel
import com.google.android.gms.maps.model.LatLng
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// Predefined colors for consistent styling.
private val dorzRed = Color(0xFFD32F2F)
private val dorzWhite = Color.White

/**
 * Displays a list of the user's ongoing and finished rides.
 *
 * This screen fetches ride history from the [RideHistoryViewModel] and separates it
 * into two sections. It provides actions to track an ongoing ride or "finish" it
 * for debugging purposes.
 *
 * @param navController Controller for navigating to other screens, like ride tracking.
 * @param rideHistoryViewModel ViewModel for accessing and managing ride history data.
 * @param bookRideViewModel ViewModel to get location data for tracking an ongoing ride.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun YourRidesScreen(
    navController: NavController,
    rideHistoryViewModel: RideHistoryViewModel,
    bookRideViewModel: BookRideViewModel
) {
    // Collect ride lists from the ViewModel as state.
    val ongoingRides by rideHistoryViewModel.ongoingRides.collectAsState(initial = emptyList())
    val finishedRides by rideHistoryViewModel.finishedRides.collectAsState(initial = emptyList())

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Your Rides") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = dorzWhite)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = dorzRed,
                    titleContentColor = dorzWhite
                )
            )
        }
    ) { paddingValues ->
        // Display a message if there is no ride history.
        if (ongoingRides.isEmpty() && finishedRides.isEmpty()) {
            Box(
                modifier = Modifier.padding(paddingValues).fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "You have no ride history yet.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color.Gray,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(16.dp)
                )
            }
        } else {
            // Display the lists of ongoing and finished rides.
            LazyColumn(
                modifier = Modifier
                    .padding(paddingValues)
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(vertical = 16.dp)
            ) {
                // --- Ongoing Rides Section ---
                if (ongoingRides.isNotEmpty()) {
                    item {
                        Text(
                            "Ongoing Ride",
                            style = MaterialTheme.typography.titleLarge,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                    }
                    items(ongoingRides) { ride ->
                        OngoingRideItem(
                            ride = ride,
                            onFinishClick = {
                                Log.d("YourRidesScreen", "Finishing ride: ${ride.id}")
                                rideHistoryViewModel.finishRide(ride)
                            },
                            onTrackClick = {
                                // Retrieve the start/destination from the booking flow's ViewModel.
                                val start = bookRideViewModel.startLocation.value
                                val dest = bookRideViewModel.destinationLocation.value

                                // Helper to safely encode LatLng for navigation.
                                fun latLngToString(latLng: LatLng?): String {
                                    val coords = latLng ?: return "0.0,0.0"
                                    return URLEncoder.encode(
                                        "${coords.latitude},${coords.longitude}",
                                        StandardCharsets.UTF_8.name()
                                    )
                                }

                                val startArg = latLngToString(start)
                                val destArg = latLngToString(dest)

                                navController.navigate("book_ride_tracking/$startArg/$destArg")
                            }
                        )
                    }
                }

                // --- Previous Rides Section ---
                if (finishedRides.isNotEmpty()) {
                    item {
                        if (ongoingRides.isNotEmpty()) Spacer(Modifier.height(16.dp))
                        Text(
                            "Previous Rides",
                            style = MaterialTheme.typography.titleLarge,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                    }
                    items(finishedRides) { ride ->
                        PreviousRideItem(ride = ride) {
                            Log.d("YourRidesScreen", "Reorder clicked for ride: ${ride.id}")
                            // TODO: Implement reorder functionality.
                        }
                    }
                }
            }
        }
    }
}

/**
 * A card representing a currently ongoing ride.
 *
 * @param ride The ongoing ride data.
 * @param onFinishClick A debug action to manually mark the ride as finished.
 * @param onTrackClick Action to navigate to the live tracking screen.
 */
@Composable
private fun OngoingRideItem(
    ride: RideHistory,
    onFinishClick: () -> Unit,
    onTrackClick: () -> Unit
) {
    val dateFormatter = remember { SimpleDateFormat("MMM dd, yyyy, hh:mm a", Locale.getDefault()) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("From: ${ride.startAddress}", fontWeight = FontWeight.SemiBold)
            Text("To: ${ride.destinationAddress}", fontWeight = FontWeight.SemiBold)
            Text(dateFormatter.format(Date(ride.timestamp)), style = MaterialTheme.typography.bodySmall, color = Color.Gray)
            Text("Price: ${ride.priceText}", fontWeight = FontWeight.Bold)
            Text("Class: ${ride.rideClass}")

            Spacer(modifier = Modifier.height(8.dp))

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = onFinishClick,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray)
                ) {
                    Text("DEBUG: Finish")
                }
                Button(
                    onClick = onTrackClick,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = dorzRed)
                ) {
                    Text("Track Ride")
                }
            }
        }
    }
}

/**
 * A card representing a previously completed ride.
 *
 * @param ride The finished ride data.
 * @param onReorderClick Action to start a new booking with the same route.
 */
@Composable
private fun PreviousRideItem(
    ride: RideHistory,
    onReorderClick: () -> Unit
) {
    val dateFormatter = remember { SimpleDateFormat("MMM dd, yyyy, hh:mm a", Locale.getDefault()) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("From: ${ride.startAddress}", fontWeight = FontWeight.SemiBold)
                Text("To: ${ride.destinationAddress}", fontWeight = FontWeight.SemiBold)
                Text(dateFormatter.format(Date(ride.timestamp)), style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                Text("Price: ${ride.priceText}", fontWeight = FontWeight.Bold)
                Text("Class: ${ride.rideClass}")
            }
            Spacer(modifier = Modifier.width(8.dp))
            Button(
                onClick = onReorderClick,
                colors = ButtonDefaults.buttonColors(
                    containerColor = dorzRed,
                    contentColor = dorzWhite
                )
            ) {
                Text("Order Again")
            }
        }
    }
}
