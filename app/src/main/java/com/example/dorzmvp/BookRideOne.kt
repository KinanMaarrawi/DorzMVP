// This file defines the primary user interface for the initial stage of booking a ride.
// The main entry point is the `BookARideMainUI` composable function, which orchestrates
// the overall screen layout and behavior. It's composed of several smaller, reusable composable
// functions such as `TopBarBookRide` for the header, `RideBox` for selecting start and
// destination locations, and `YangoRideOptionsDisplay` for showing the list of available taxi services.
//
// How it works:
// - It interacts with `BookRideViewModel` to observe and display taxi options, loading states,
//   and error messages.
// - It uses `NavController` to handle navigation to other screens, specifically for selecting
//   pickup (`book_ride_start`) and destination (`book_ride_destination`) locations on a map.
// - Location selections are passed back from these map screens via the NavController's
//   SavedStateHandle and observed using `LaunchedEffect` to update local state and trigger
//   address resolution (using `getAddressFromLatLng`).
// - Once both start and destination locations are selected, another `LaunchedEffect` triggers
//   the `BookRideViewModel` to fetch taxi options from the Yandex API.
// - Helper composables like `RideOptionItem` define the appearance of individual taxi options,
//   and `LocationDisplayBox` provides a standardized UI for showing selectable location fields.

package com.example.dorzmvp

import android.content.Context
import android.location.Geocoder
import android.os.Build
import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.dorzmvp.network.TaxiOptionResponse
import com.example.dorzmvp.ui.viewmodel.BookRideViewModel
import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException
import java.util.Locale
import kotlin.math.roundToInt

/**
 * The main UI composable for the initial ride booking screen.
 *
 * This screen allows users to select their start and destination locations and view available
 * Yango taxi options. It observes data from [BookRideViewModel] and handles navigation
 * for location selection.
 *
 * @param navController The [NavController] used for navigating to location selection screens.
 * @param rideViewModel The [BookRideViewModel] instance used to fetch taxi options and manage state.
 */
@Composable
fun BookARideMainUI(navController: NavController, rideViewModel: BookRideViewModel = viewModel()) {
    // State for selected start location (LatLng and address string)
    var selectedStartLocation by rememberSaveable { mutableStateOf<LatLng?>(null) }
    var startAddress by rememberSaveable { mutableStateOf<String?>(null) }
    // State for selected destination location (LatLng and address string)
    var selectedDestinationLocation by rememberSaveable { mutableStateOf<LatLng?>(null) }
    var destinationAddress by rememberSaveable { mutableStateOf<String?>(null) }

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val navBackStackEntry = navController.currentBackStackEntry

    // Observe LiveData from BookRideViewModel for Yango API call status and results.
    val isLoadingYango by rideViewModel.isLoading.observeAsState(initial = false)
    val yangoTaxiOptions by rideViewModel.taxiOptions.observeAsState()
    val yangoErrorMessage by rideViewModel.errorMessage.observeAsState()

    // Observe selected start location passed back from the map selection screen.
    val startLocationResult = navBackStackEntry?.savedStateHandle?.getLiveData<LatLng>("selectedStartLocation")?.observeAsState()
    LaunchedEffect(startLocationResult?.value) { // Re-triggers if the LatLng value changes.
        startLocationResult?.value?.let { latLng ->
            if (selectedStartLocation != latLng) { // Process only if it's a new selection.
                selectedStartLocation = latLng
                startAddress = null // Clear previous address, will be updated by getAddressFromLatLng.
                coroutineScope.launch {
                    startAddress = getAddressFromLatLng(context, latLng)
                }
            }
            // Clear the result from SavedStateHandle to prevent re-processing on recomposition if not handled.
            navBackStackEntry.savedStateHandle.remove<LatLng>("selectedStartLocation")
        }
    }

    // Observe selected destination location passed back from the map selection screen.
    val destinationLocationResult = navBackStackEntry?.savedStateHandle?.getLiveData<LatLng>("selectedDestinationLocation")?.observeAsState()
    LaunchedEffect(destinationLocationResult?.value) { // Re-triggers if the LatLng value changes.
        destinationLocationResult?.value?.let { latLng ->
            if (selectedDestinationLocation != latLng) { // Process only if it's a new selection.
                selectedDestinationLocation = latLng
                destinationAddress = null // Clear previous address.
                coroutineScope.launch {
                    destinationAddress = getAddressFromLatLng(context, latLng)
                }
            }
            navBackStackEntry.savedStateHandle.remove<LatLng>("selectedDestinationLocation")
        }
    }

    // Trigger Yango API call via ViewModel when both start and destination locations are set.
    LaunchedEffect(selectedStartLocation, selectedDestinationLocation) {
        if (selectedStartLocation != null && selectedDestinationLocation != null) {
            Log.d("BookARideMainUI", "Both locations selected, fetching Yango info...")
            rideViewModel.fetchTaxiInformation(
                startLatLng = selectedStartLocation!!, // !! is safe due to the null check above.
                destinationLatLng = selectedDestinationLocation!!
            )
        }
    }

    // Main layout Column for the screen.
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        TopBarBookRide() // Displays the top app bar/header.
        Spacer(modifier = Modifier.height(24.dp))
        // Composable for selecting start/destination and displaying their addresses.
        RideBox(
            navController = navController,
            selectedStartLocation = selectedStartLocation,
            startAddress = startAddress,
            selectedDestinationLocation = selectedDestinationLocation,
            destinationAddress = destinationAddress
        )
        Spacer(modifier = Modifier.height(16.dp))
        // Composable for displaying the list of Yango ride options or loading/error states.
        YangoRideOptionsDisplay(
            isLoading = isLoadingYango,
            options = yangoTaxiOptions,
            errorMsg = yangoErrorMessage,
            onClearError = { rideViewModel.clearErrorMessage() } // Lambda to clear error message via ViewModel.
        )
    }
}

/**
 * Displays the list of Yango taxi options, or a loading indicator / error message.
 *
 * @param isLoading True if data is currently being fetched, false otherwise.
 * @param options The list of [TaxiOptionResponse] to display. Can be null if not yet loaded or if an error occurred.
 * @param errorMsg An error message string to display. Null if no error.
 * @param onClearError A lambda function to be invoked when the user dismisses an error message.
 */
@Composable
private fun YangoRideOptionsDisplay(
    isLoading: Boolean,
    options: List<TaxiOptionResponse>?,
    errorMsg: String?,
    onClearError: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        if (isLoading) {
            CircularProgressIndicator(modifier = Modifier.padding(16.dp))
        }

        errorMsg?.let {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = it,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(8.dp)
                )
                Button(onClick = onClearError) { // Button to dismiss the error message.
                    Text("Dismiss")
                }
            }
        }

        // Display ride options only if not loading and no error message is present.
        if (!isLoading && errorMsg == null) {
            options?.let { rideOptions ->
                if (rideOptions.isEmpty()) { // Check if the list of options is empty.
                    Text("No ride options available for this route currently.", modifier = Modifier.padding(16.dp))
                }
                LazyColumn(modifier = Modifier.fillMaxWidth()) { // Efficiently displays a scrollable list.
                    items(rideOptions) { option ->
                        RideOptionItem(option = option)
                    }
                }
            }
        }
    }
}

/**
 * Displays a single taxi ride option item.
 *
 * Shows the Yango logo, taxi class name, estimated waiting time, and price.
 * @param option The [TaxiOptionResponse] data for the ride option to display.
 */
@Composable
private fun RideOptionItem(option: TaxiOptionResponse) {
    val waitingTimeSeconds = option.waitingTime ?: 0.0
    val waitingTimeMinutes = (waitingTimeSeconds / 60).roundToInt() // Convert waiting time to minutes.

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp, horizontal = 16.dp)
            .border(1.dp, Color.LightGray, RoundedCornerShape(8.dp))
            .padding(16.dp) // Inner padding for content.
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Yango Logo image.
            Image(
                painter = painterResource(id = R.drawable.yango_logo),
                contentDescription = "Yango Logo",
                modifier = Modifier.size(40.dp)
            )

            Spacer(modifier = Modifier.width(12.dp)) // Spacing between logo and text details.

            // Column for taxi class and waiting time.
            Column(modifier = Modifier.weight(1f)) { // `weight(1f)` allows this column to take available space.
                Text(
                    text = option.classText ?: option.className ?: "Unknown Class",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Wait: ~$waitingTimeMinutes min",
                    fontSize = 14.sp,
                    color = Color.Gray
                )
            }
            Spacer(modifier = Modifier.width(16.dp)) // Spacing before the price.
            // Price text.
            Text(
                text = option.priceText ?: "N/A", // Fallback if price text is not available.
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = MaterialTheme.colorScheme.primary // Use theme color for emphasis.
            )
        }
    }
}

/**
 * A suspend function to convert Latitude/Longitude coordinates into a human-readable address string.
 *
 * Uses Android's Geocoder API. Runs on the IO dispatcher for network/disk operations.
 * @param context The application context.
 * @param latLng The [LatLng] coordinates to geocode.
 * @return A formatted address string if successful, or a fallback string with Lat/Lng if not.
 */
private suspend fun getAddressFromLatLng(context: Context, latLng: LatLng): String {
    return withContext(Dispatchers.IO) { // Perform geocoding off the main thread.
        val geocoder = Geocoder(context, Locale.getDefault())
        var addressText: String? = null
        try {
            @Suppress("DEPRECATION") // getFromLocation is deprecated in API 33+, but handles older versions.
            val addresses = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                // For Android 13 (API 33) and above, use the new Geocoder.getFromLocation overload with a listener.
                // However, for simplicity and to maintain compatibility with the existing synchronous style here,
                // the deprecated version is still used with a check. A more robust solution for TIRAMISU+
                // would involve using the callback-based version if this were a more critical path.
                geocoder.getFromLocation(latLng.latitude, latLng.longitude, 1) // Max 1 result.
            } else {
                geocoder.getFromLocation(latLng.latitude, latLng.longitude, 1)
            }

            if (addresses?.isNotEmpty() == true) {
                addressText = addresses[0].getAddressLine(0) // Get the first address line.
            }
        } catch (e: IOException) {
            Log.e("BookRideOne", "Error getting address from LatLng (Geocoder IOException): ${e.message}")
        } catch (e: IllegalArgumentException) {
            Log.e("BookRideOne", "Invalid LatLng passed to geocoder: ${e.message}")
        }
        // Fallback to Lat/Lng string if address resolution fails.
        addressText ?: "Lat: %.5f, Lng: %.5f".format(Locale.US, latLng.latitude, latLng.longitude)
    }
}

/**
 * Displays the top bar for the ride booking screen.
 * It shows a title "Book a Ride" with a distinctive background.
 */
@Composable
fun TopBarBookRide() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(128.dp)
            .background(
                color = Color.Red,
                shape = RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp) // Rounded bottom corners.
            ),
        contentAlignment = Alignment.Center // Center the content (Text).
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
 * A composable that groups the start and destination location selection boxes.
 *
 * @param navController The [NavController] used for navigating to map screens for location selection.
 * @param selectedStartLocation The currently selected start [LatLng], or null if not selected.
 * @param startAddress The address string for the start location, or null.
 * @param selectedDestinationLocation The currently selected destination [LatLng], or null.
 * @param destinationAddress The address string for the destination location, or null.
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
                clip = false // Allow shadow to extend beyond the bounds of the composable.
            )
            .background(
                color = Color.White,
                shape = RoundedCornerShape(cornerRadius)
            )
            .padding(16.dp) // Inner padding for the content of the RideBox.
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = "Plan Your Ride",
                color = Color.Black,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            // Display for selecting/showing the pickup point.
            LocationDisplayBox(
                text = startAddress ?: selectedStartLocation?.let { "Lat: %.5f, Lng: %.5f".format(Locale.US, it.latitude, it.longitude) } ?: "Tap to select pickup point",
                isPlaceholder = startAddress.isNullOrEmpty() && selectedStartLocation == null,
                onClick = { navController.navigate("book_ride_start") },
                cornerRadius = nestedBoxCornerRadius
            )

            Spacer(modifier = Modifier.height(4.dp))
            Icon(
                imageVector = Icons.Default.KeyboardArrowDown, // Visual separator/indicator.
                contentDescription = null, // Decorative icon.
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
            Spacer(modifier = Modifier.height(4.dp))

            // Display for selecting/showing the destination point.
            LocationDisplayBox(
                text = destinationAddress ?: selectedDestinationLocation?.let { "Lat: %.5f, Lng: %.5f".format(Locale.US, it.latitude, it.longitude) } ?: "Tap to select destination",
                isPlaceholder = destinationAddress.isNullOrEmpty() && selectedDestinationLocation == null,
                onClick = { navController.navigate("book_ride_destination") },
                cornerRadius = nestedBoxCornerRadius
            )
        }
    }
}

/**
 * A reusable composable for displaying a tappable location field.
 *
 * Used for both start and destination points in the [RideBox].
 * @param text The text to display (address, Lat/Lng, or placeholder prompt).
 * @param isPlaceholder True if the current text is a placeholder, affecting text color.
 * @param onClick Lambda to be executed when the box is clicked (typically navigates to a map screen).
 * @param cornerRadius The corner radius for the display box.
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
            .clickable(onClick = onClick) // Makes the box tappable.
            .border(
                width = 1.dp,
                color = Color.Gray,
                shape = RoundedCornerShape(cornerRadius)
            )
            .padding(horizontal = 12.dp, vertical = 12.dp), // Padding for the text inside.
        contentAlignment = Alignment.CenterStart
    ) {
        Text(
            text = text,
            fontSize = 16.sp,
            color = if (isPlaceholder) Color.Gray else Color.Black // Different color for placeholder text.
        )
    }
}
