package com.example.dorzmvp

import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController

/**
 * Currently this is all hardcoded
 * For a proper release of the app, we need to dynamically create ride info from the second the user orders the ride
 * This is a whole separate issue due to the fact that you need to open Yango app to order the ride as per their documentation. Have to see Uber and Careem if they respond
 * Need a proper backend database or servers to store ride info of customers
 * */


/**
 * Represents the status of a ride.
 */
enum class RideStatus {
    /** The ride is currently in progress. */
    ONGOING,

    /** The ride has been completed. */
    COMPLETED
}

/**
 * Data class holding information about a specific ride.
 *
 * @property id Unique identifier for the ride.
 * @property origin The starting point of the ride.
 * @property destination The ending point of the ride.
 * @property date The date and time of the ride.
 * @property status The current [RideStatus] of the ride.
 * @property price The price of the ride, typically available only for [RideStatus.COMPLETED] rides.
 * @property vehicleInfo Information about the vehicle used for the ride, e.g., "Toyota Camry - Black - ABC 123".
 */
data class RideInfo(
    val id: String,
    val origin: String,
    val destination: String,
    val date: String,
    val status: RideStatus,
    val price: String? = null, // Only for completed rides
    val vehicleInfo: String? = null // e.g., "Toyota Camry - Black - ABC 123"
)

// Dummy Data for demonstration purposes
/** A sample ongoing ride. */
val dummyOngoingRide = RideInfo(
    id = "ride_ongoing_001",
    origin = "Dubai Mall, Downtown Dubai",
    destination = "Mall of the Emirates, Al Barsha",
    date = "Today, 10:30 AM",
    status = RideStatus.ONGOING,
    vehicleInfo = "Tesla Model 3 - White - DXB 7890"
)

/** A list of sample previous rides. */
val dummyPreviousRides = listOf(
    RideInfo(
        id = "ride_completed_001",
        origin = "Jumeirah Beach Residence",
        destination = "Dubai Marina Mall",
        date = "Yesterday, 05:45 PM",
        status = RideStatus.COMPLETED,
        price = "AED 35.00"
    ),
    RideInfo(
        id = "ride_completed_002",
        origin = "Al Karama",
        destination = "Deira City Centre",
        date = "Aug 22, 2025, 09:15 AM",
        status = RideStatus.COMPLETED,
        price = "AED 22.50"
    ),
    RideInfo(
        id = "ride_completed_003",
        origin = "Business Bay",
        destination = "World Trade Centre",
        date = "Jul 20, 2025, 02:00 PM",
        status = RideStatus.COMPLETED,
        price = "AED 28.00"
    )
)

/**
 * Composable function for the "Your Rides" screen.
 * Displays ongoing and previous rides to the user.
 *
 * @param navController The [NavController] used for navigation actions, such as going back
 *                      or navigating to ride details.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun YourRidesScreen(navController: NavController) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Your Rides") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White // Ensure icon is visible on red background
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFFD32F2F), // Material Red 700
                    titleContentColor = Color.White
                )
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Ongoing Ride Section
            item {
                Text(
                    text = "Ongoing Ride",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                OngoingRideCard(
                    ride = dummyOngoingRide,
                    onTrackRideClick = { // Renamed from onClick for clarity
                        Log.d("YourRidesScreen", "Track Ride button clicked for ongoing ride: ${dummyOngoingRide.id}")
                        // TODO: Navigate to live tracking screen
                        // navController.navigate("ride_tracking_screen/${dummyOngoingRide.id}")
                    }
                )
            }

            // Previous Rides Section
            item {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Previous Rides",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }

            items(dummyPreviousRides) { ride ->
                PreviousRideItem(
                    ride = ride,
                    onReorderClick = {
                        Log.d("YourRidesScreen", "Reorder clicked for ride: ${ride.id}")
                        // TODO: Navigate to booking screen with prefilled details or directly reorder
                        // navController.navigate("book_ride_one?origin=${ride.origin}&destination=${ride.destination}")
                    }
                )
            }
        }
    }
}

/**
 * Composable function to display a card for an ongoing ride.
 * It shows ride details on the left and a "Track Ride" button on the right.
 *
 * @param ride The [RideInfo] object for the ongoing ride.
 * @param onTrackRideClick Lambda function to be invoked when the "Track Ride" button is clicked.
 */
@Composable
fun OngoingRideCard(
    ride: RideInfo,
    onTrackRideClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(), // Card is no longer clickable itself
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically // Aligns items in the row vertically centered
        ) {
            Column(modifier = Modifier.weight(1f)) { // Text details take available space
                Text(
                    text = "Status: ${ride.status}",
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFD32F2F) // Red for ongoing status
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "From: ${ride.origin}",
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = "To: ${ride.destination}",
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = ride.date,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
                ride.vehicleInfo?.let {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Vehicle: $it",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
            Spacer(modifier = Modifier.width(8.dp)) // Space between text column and button
            Button(
                onClick = onTrackRideClick, // Click action is now on the button
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFD32F2F), // Red button
                    contentColor = Color.White
                )
            ) {
                Text("Track Ride")
            }
        }
    }
}

/**
 * Composable function to display an item for a previous ride in a list.
 *
 * @param ride The [RideInfo] object for the previous ride.
 * @param onReorderClick Lambda function to be invoked when the "Order Again" button is clicked.
 */
@Composable
fun PreviousRideItem(
    ride: RideInfo,
    onReorderClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "From: ${ride.origin}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "To: ${ride.destination}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = ride.date,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
                ride.price?.let {
                    Text(
                        text = "Price: $it",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            Spacer(modifier = Modifier.width(8.dp))
            Button(
                onClick = onReorderClick,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFD32F2F), // Red button
                    contentColor = Color.White
                )
            ) {
                Text("Order Again")
            }
        }
    }
}
