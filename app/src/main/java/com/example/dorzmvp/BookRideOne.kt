package com.example.dorzmvp

import android.content.Context
import android.location.Geocoder
import android.os.Build
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException
import java.util.Locale

/**
 * Main Composable for the first screen of the "Book a Ride" flow.
 * It handles the selection of both start and destination locations and displays a summary.
 */
@Composable
fun BookARideMainUI(navController: NavController) {
    var selectedStartLocation by rememberSaveable { mutableStateOf<LatLng?>(null) }
    var startAddress by rememberSaveable { mutableStateOf<String?>(null) }
    var selectedDestinationLocation by rememberSaveable { mutableStateOf<LatLng?>(null) }
    var destinationAddress by rememberSaveable { mutableStateOf<String?>(null) }

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val navBackStackEntry = navController.currentBackStackEntry

    // Observe selected start location
    val startLocationResult = navBackStackEntry?.savedStateHandle?.getLiveData<LatLng>("selectedStartLocation")?.observeAsState()
    LaunchedEffect(startLocationResult) {
        startLocationResult?.value?.let { latLng ->
            if (selectedStartLocation != latLng) {
                selectedStartLocation = latLng
                startAddress = null // Clear previous address immediately, will be updated below
                coroutineScope.launch {
                    startAddress = getAddressFromLatLng(context, latLng)
                }
            }
            navBackStackEntry.savedStateHandle.remove<LatLng>("selectedStartLocation")
        }
    }

    // Observe selected destination location
    val destinationLocationResult = navBackStackEntry?.savedStateHandle?.getLiveData<LatLng>("selectedDestinationLocation")?.observeAsState()
    LaunchedEffect(destinationLocationResult) {
        destinationLocationResult?.value?.let { latLng ->
            if (selectedDestinationLocation != latLng) {
                selectedDestinationLocation = latLng
                destinationAddress = null // Clear previous address immediately, will be updated below
                coroutineScope.launch {
                    destinationAddress = getAddressFromLatLng(context, latLng)
                }
            }
            navBackStackEntry.savedStateHandle.remove<LatLng>("selectedDestinationLocation")
        }
    }

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        TopBarBookRide()
        Spacer(modifier = Modifier.height(24.dp))
        RideBox(
            navController = navController,
            selectedStartLocation = selectedStartLocation,
            startAddress = startAddress,
            selectedDestinationLocation = selectedDestinationLocation,
            destinationAddress = destinationAddress
        )
    }
}

/**
 * Helper function to get a human-readable address from LatLng coordinates using Android's Geocoder.
 * This function is suspending and performs network operations on [Dispatchers.IO].
 *
 * @param context The current Android [Context], required for [Geocoder].
 * @param latLng The [LatLng] coordinates for which to find the address.
 * @return A [String] representing the address, or a fallback string if not found or an error occurs.
 */
private suspend fun getAddressFromLatLng(context: Context, latLng: LatLng): String {
    return withContext(Dispatchers.IO) {
        val geocoder = Geocoder(context, Locale.getDefault())
        var addressText: String? = null
        try {
            @Suppress("DEPRECATION")
            val addresses = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                geocoder.getFromLocation(latLng.latitude, latLng.longitude, 1)
            } else {
                geocoder.getFromLocation(latLng.latitude, latLng.longitude, 1)
            }
            if (addresses?.isNotEmpty() == true) {
                addressText = addresses[0].getAddressLine(0)
            }
        } catch (e: IOException) {
            Log.e("BookRideOne", "Error getting address from LatLng: ${e.message}")
        } catch (e: IllegalArgumentException) {
            Log.e("BookRideOne", "Invalid LatLng passed to geocoder: ${e.message}")
        }
        addressText ?: "Lat: %.5f, Lng: %.5f".format(latLng.latitude, latLng.longitude) // Fallback to coordinates
    }
}

/**
 * Displays the themed top bar for the "Book a Ride" screen.
 */
@Composable
fun TopBarBookRide() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(128.dp)
            .background(
                color = Color.Red, // Consider using Theme colors
                shape = RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp)
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "Book a Ride",
            color = Color.White, // Consider using Theme colors
            fontSize = 30.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

/**
 * Composable for the main content area where users plan their ride, including pickup and destination.
 *
 * @param navController For navigating to location selection screens.
 * @param selectedStartLocation The selected pickup [LatLng], or null.
 * @param startAddress The address string for the pickup location, or null.
 * @param selectedDestinationLocation The selected destination [LatLng], or null.
 * @param destinationAddress The address string for the destination, or null.
 */
@Composable
fun RideBox(
    navController: NavController,
    selectedStartLocation: LatLng?,
    startAddress: String?,
    selectedDestinationLocation: LatLng?,
    destinationAddress: String?
) {
    val cornerRadius = 24.dp
    val nestedBoxCornerRadius = 8.dp

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .shadow(
                elevation = 4.dp,
                shape = RoundedCornerShape(cornerRadius),
                clip = false
            )
            .background(
                color = Color.White,
                shape = RoundedCornerShape(cornerRadius)
            )
            .padding(16.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = "Plan Your Ride",
                color = Color.Black,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 16.dp) // Added padding
            )

            // Pickup Location Box
            LocationDisplayBox(
                text = startAddress ?: selectedStartLocation?.let { "Lat: %.5f, Lng: %.5f".format(it.latitude, it.longitude) } ?: "Tap to select pickup point",
                isPlaceholder = startAddress.isNullOrEmpty() && selectedStartLocation == null,
                onClick = { navController.navigate("book_ride_start") },
                cornerRadius = nestedBoxCornerRadius
            )

            Spacer(modifier = Modifier.height(4.dp))
            Icon(
                imageVector = Icons.Default.KeyboardArrowDown,
                contentDescription = null,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
            Spacer(modifier = Modifier.height(4.dp))

            // Destination Location Box
            LocationDisplayBox(
                text = destinationAddress ?: selectedDestinationLocation?.let { "Lat: %.5f, Lng: %.5f".format(it.latitude, it.longitude) } ?: "Tap to select destination",
                isPlaceholder = destinationAddress.isNullOrEmpty() && selectedDestinationLocation == null,
                onClick = { navController.navigate("book_ride_destination") },
                cornerRadius = nestedBoxCornerRadius
            )

            // Ride Summary Text for debugging/testing
            /*
            if (startAddress != null && destinationAddress != null) {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Ride from: $startAddress\nTo: $destinationAddress",
                    fontSize = 14.sp,
                    color = Color.DarkGray, // Consider using Theme colors
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
            */
        }
    }
}

/**
 * A reusable composable for displaying a location (pickup or destination).
 *
 * @param text The text to display (address, coordinates, or placeholder).
 * @param isPlaceholder True if the current text is a placeholder, to adjust text color.
 * @param onClick Lambda to execute when the box is clicked.
 * @param cornerRadius The corner radius for the box.
 */
@Composable
private fun LocationDisplayBox(
    text: String,
    isPlaceholder: Boolean,
    onClick: () -> Unit,
    cornerRadius: Dp
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(cornerRadius))
            .clickable(onClick = onClick)
            .border(
                width = 1.dp,
                color = Color.Gray,
                shape = RoundedCornerShape(cornerRadius)
            )
            .padding(horizontal = 12.dp, vertical = 12.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Text(
            text = text,
            fontSize = 16.sp,
            color = if (isPlaceholder) Color.Gray else Color.Black
        )
    }
}
