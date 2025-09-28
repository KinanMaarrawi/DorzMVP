package com.example.dorzmvp

import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
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
import com.example.dorzmvp.ui.viewmodel.RideHistoryViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun YourRidesScreen(navController: NavController, rideHistoryViewModel: RideHistoryViewModel) {
    // Collect both ongoing and finished rides from the ViewModel
    val ongoingRides by rideHistoryViewModel.ongoingRides.collectAsState(initial = emptyList())
    val finishedRides by rideHistoryViewModel.finishedRides.collectAsState(initial = emptyList())

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Your Rides") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFFD32F2F),
                    titleContentColor = Color.White
                )
            )
        }
    ) { paddingValues ->
        if (ongoingRides.isEmpty() && finishedRides.isEmpty()) {
            // Show empty state only if both lists are empty
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
            LazyColumn(
                modifier = Modifier
                    .padding(paddingValues)
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(vertical = 16.dp)
            ) {
                // --- Ongoing Ride Section ---
                if (ongoingRides.isNotEmpty()) {
                    item {
                        Text(
                            text = "Ongoing Ride",
                            style = MaterialTheme.typography.titleLarge,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                    }
                    items(ongoingRides) { ride ->
                        // Using a modified RideItem that includes the debug button
                        OngoingRideItem(
                            ride = ride,
                            onFinishClick = {
                                Log.d("YourRidesScreen", "Finishing ride: ${ride.id}")
                                rideHistoryViewModel.finishRide(ride)
                            }
                        )
                    }
                }

                // --- Previous Rides Section ---
                if (finishedRides.isNotEmpty()) {
                    item {
                        // Add a spacer if there are ongoing rides above
                        if (ongoingRides.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(16.dp))
                        }
                        Text(
                            text = "Previous Rides",
                            style = MaterialTheme.typography.titleLarge,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                    }
                    items(finishedRides) { ride ->
                        PreviousRideItem(
                            ride = ride,
                            onReorderClick = {
                                Log.d("YourRidesScreen", "Reorder clicked for ride: ${ride.id}")
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun OngoingRideItem(
    ride: RideHistory,
    onFinishClick: () -> Unit
) {
    val dateFormatter = remember { SimpleDateFormat("MMM dd, yyyy, hh:mm a", Locale.getDefault()) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp), // More elevation for ongoing
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("From: ${ride.startAddress}", fontWeight = FontWeight.SemiBold)
            Text("To: ${ride.destinationAddress}", fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(4.dp))
            Text(dateFormatter.format(Date(ride.timestamp)), style = MaterialTheme.typography.bodySmall, color = Color.Gray)
            Spacer(modifier = Modifier.height(4.dp))
            Text("Price: ${ride.priceText}", fontWeight = FontWeight.Bold)
            Text("Class: ${ride.rideClass}")
            Spacer(modifier = Modifier.height(12.dp))
            Button( // Debug button to finish the ride
                onClick = onFinishClick,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray)
            ) {
                Text("DEBUG: Finish Ride")
            }
        }
    }
}

@Composable
fun PreviousRideItem(
    ride: RideHistory,
    onReorderClick: () -> Unit
) {
    val dateFormatter = remember { SimpleDateFormat("MMM dd, yyyy, hh:mm a", Locale.getDefault()) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("From: ${ride.startAddress}", fontWeight = FontWeight.SemiBold)
                Text("To: ${ride.destinationAddress}", fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(4.dp))
                Text(dateFormatter.format(Date(ride.timestamp)), style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                Spacer(modifier = Modifier.height(4.dp))
                Text("Price: ${ride.priceText}", fontWeight = FontWeight.Bold)
                Text("Class: ${ride.rideClass}")
            }
            Spacer(modifier = Modifier.width(8.dp))
            Button(
                onClick = onReorderClick,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFD32F2F),
                    contentColor = Color.White
                )
            ) {
                Text("Order Again")
            }
        }
    }
}
