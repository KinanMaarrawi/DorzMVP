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
import androidx.compose.ui.draw.clip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable // Added import
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.compose.ui.unit.sp
import com.google.android.gms.maps.model.LatLng // For LatLng type
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException
import java.util.Locale

/**
 * Main Composable for the first screen of the "Book a Ride" flow.
 */
@Composable
fun BookARideMainUI(navController : NavController) {
    var selectedStartLocation by rememberSaveable { mutableStateOf<LatLng?>(null) } // Changed to rememberSaveable
    var startAddress by rememberSaveable { mutableStateOf<String?>(null) } // Changed to rememberSaveable
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val navBackStackEntry = navController.currentBackStackEntry
    val selectedLocationResult = navBackStackEntry?.savedStateHandle?.getLiveData<LatLng>("selectedStartLocation")?.observeAsState()

    LaunchedEffect(selectedLocationResult) {
        selectedLocationResult?.value?.let { latLng ->
            if (selectedStartLocation != latLng) { // Only update if it's a new location
                selectedStartLocation = latLng
                startAddress = null // Clear previous address immediately
                coroutineScope.launch {
                    startAddress = getAddressFromLatLng(context, latLng)
                }
            }
            // Clear the value from SavedStateHandle after processing to avoid re-processing on config change if not desired
            // Or only if the value has genuinely changed and been processed.
            navBackStackEntry?.savedStateHandle?.remove<LatLng>("selectedStartLocation")
        }
    }

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        TopBarBookRide()
        Spacer(modifier = Modifier.height(24.dp))
        RideBox(navController = navController, selectedStartLocation = selectedStartLocation, startAddress = startAddress)
    }
}

// Helper function to get address from LatLng
private suspend fun getAddressFromLatLng(context: Context, latLng: LatLng): String? {
    return withContext(Dispatchers.IO) { // Perform geocoding on a background thread
        val geocoder = Geocoder(context, Locale.getDefault())
        var addressText: String? = null
        try {
            // For Android Tiramisu (API 33) and above, use the new asynchronous API
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                val addresses = geocoder.getFromLocation(latLng.latitude, latLng.longitude, 1)
                if (addresses?.isNotEmpty() == true) {
                    addressText = addresses[0].getAddressLine(0)
                }
            } else {
                // For older versions, use the deprecated synchronous API
                @Suppress("DEPRECATION")
                val addresses = geocoder.getFromLocation(latLng.latitude, latLng.longitude, 1)
                if (addresses?.isNotEmpty() == true) {
                    addressText = addresses[0].getAddressLine(0)
                }
            }
        } catch (e: IOException) {
            Log.e("BookRideOne", "Error getting address from LatLng", e)
            addressText = "Could not find address" // Or null to show coordinates
        } catch (e: IllegalArgumentException) {
            Log.e("BookRideOne", "Invalid LatLng passed to geocoder", e)
            addressText = "Invalid location" // Or null
        }
        addressText
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
                color = Color.Red,
                shape = RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp)
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "Book a Ride",
            color = Color.White,
            fontSize = 30.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

/**
 * Composable for the main content area where users plan their ride.
 */
@Composable
fun RideBox(navController: NavController, selectedStartLocation: LatLng?, startAddress: String?){
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
    ){
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = "Plan Your Ride",
                color = Color.Black,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
            )

            Spacer(modifier = Modifier.height(16.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(nestedBoxCornerRadius))
                    .clickable { navController.navigate("book_ride_start") }
                    .border(
                        width = 1.dp,
                        color = Color.Gray,
                        shape = RoundedCornerShape(nestedBoxCornerRadius)
                    )
                    .padding(horizontal = 12.dp, vertical = 12.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                val pickupText = when {
                    !startAddress.isNullOrEmpty() -> startAddress
                    selectedStartLocation != null -> "Lat: %.5f, Lng: %.5f".format(
                        selectedStartLocation.latitude,
                        selectedStartLocation.longitude
                    )
                    else -> "Tap to select pickup point"
                }
                Text(
                    text = pickupText,
                    fontSize = 16.sp,
                    color = if (startAddress != null || selectedStartLocation != null) Color.Black else Color.Gray,
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Icon(
                imageVector = Icons.Default.KeyboardArrowDown,
                contentDescription = null,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )

            Spacer(modifier = Modifier.height(4.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(nestedBoxCornerRadius))
                    .clickable { navController.navigate("book_ride_destination") }
                    .border(
                        width = 1.dp,
                        color = Color.Gray,
                        shape = RoundedCornerShape(nestedBoxCornerRadius)
                    )
                    .padding(horizontal = 12.dp, vertical = 12.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Text(
                    text = "Tap to select destination",
                    fontSize = 16.sp,
                    color = Color.Gray,
                )
            }
        }
    }
}
