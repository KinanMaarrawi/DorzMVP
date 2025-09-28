/**
 * This file defines the primary UI for booking a ride, centered on [BookARideMainUI].
 *
 * Core Functionality:
 * - **Location Management**: Manages start and destination locations using `rememberSaveable` to
 *   persist state. It handles both geographic coordinates (`LatLng`) and human-readable addresses (`String`).
 * - **Navigation & Data Passing**: Navigates to dedicated map screens (`book_ride_start`,
 *   `book_ride_destination`) and receives the selected `LatLng` back via the `SavedStateHandle`.
 * - **ViewModel Interaction**: Collaborates with [BookRideViewModel] to trigger API calls for
 *   taxi options and observe the results (`isLoading`, `taxiOptions`, `errorMessage`).
 * - **Clipboard Integration**: Allows users to paste Google Maps links to set locations,
 *   using a robust parser (`parseGoogleMapsLinkForLatLng`) to extract coordinates.
 * - **Reactive Logic**: Uses `LaunchedEffect` to react to state changes:
 *   - Fetches addresses via reverse geocoding when a `LatLng` is selected.
 *   - Triggers the taxi info API call once both start and destination are fully resolved.
 * - **UI Composition**: Builds the screen from smaller, reusable composables like [RideBox]
 *   and [YangoRideOptionsDisplay].
 */
package com.example.dorzmvp

import android.content.ClipboardManager
import android.content.Context
import android.location.Geocoder
import android.net.Uri
import android.util.Log
import android.widget.Toast
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
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.dorzmvp.network.TaxiOptionResponse
import com.example.dorzmvp.network.YandexTaxiInfoResponse
import com.example.dorzmvp.ui.viewmodel.BookRideViewModel
import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLDecoder
import java.util.Locale
import java.util.regex.Pattern
import kotlin.math.roundToInt

// Logcat tag for debugging link parsing activities.
private const val TAG_LINK_PARSING = "BookRideOneLinkParse"
// Max redirects to follow when resolving short URLs.
private const val MAX_REDIRECTS = 5

/**
 * The main UI for the ride booking screen. It allows users to select start/destination
 * locations and view available taxi options.
 *
 * @param navController The NavController for navigation.
 * @param rideViewModel The ViewModel for fetching taxi info and managing state.
 */
@Composable
fun BookARideMainUI(navController: NavController, rideViewModel: BookRideViewModel = viewModel()) {
    // --- State Management ---
    // Persisted state for start/destination coordinates and their human-readable addresses.
    var selectedStartLocation by rememberSaveable { mutableStateOf<LatLng?>(null) }
    var startAddress by rememberSaveable { mutableStateOf<String?>(null) }
    var selectedDestinationLocation by rememberSaveable { mutableStateOf<LatLng?>(null) }
    var destinationAddress by rememberSaveable { mutableStateOf<String?>(null) }
    // Guard to prevent re-fetching API data for the same locations.
    var hasFetchedForCurrentLocations by rememberSaveable { mutableStateOf(false) }

    // --- Context and Scope ---
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // --- ViewModel State Observation ---
    val isLoadingYango by rideViewModel.isLoading.observeAsState(initial = false)
    val yangoTaxiOptions by rideViewModel.taxiOptions.observeAsState()
    val yangoErrorMessage by rideViewModel.errorMessage.observeAsState()

    /**
     * Retrieves plain text from the system clipboard.
     * @return The trimmed text, or null if the clipboard is empty or has no text.
     */
    fun getClipboardText(context: Context): String? {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val item = clipboard.primaryClip?.getItemAt(0)
        return item?.coerceToText(context)?.toString()?.trim()?.takeIf { it.isNotEmpty() }
    }

    // --- Navigation Result Handling ---
    // This effect listens for results from the map selection screens.
    LaunchedEffect(navController.currentBackStackEntry) {
        val savedStateHandle = navController.currentBackStackEntry?.savedStateHandle ?: return@LaunchedEffect

        // Handle start location result
        savedStateHandle.get<LatLng>("selectedStartLocation")?.let { latLng ->
            if (selectedStartLocation != latLng) {
                Log.d("BookRideOneResult", "Got new START location: $latLng")
                selectedStartLocation = latLng
            }
            // Consume the event to prevent re-processing on config change.
            savedStateHandle.remove<LatLng>("selectedStartLocation")
        }

        // Handle destination location result
        savedStateHandle.get<LatLng>("selectedDestinationLocation")?.let { latLng ->
            if (selectedDestinationLocation != latLng) {
                Log.d("BookRideOneResult", "Got new DESTINATION location: $latLng")
                selectedDestinationLocation = latLng
            }
            // Consume the event.
            savedStateHandle.remove<LatLng>("selectedDestinationLocation")
        }
    }

    // --- Address Fetching Effects ---
    // Fetches the address when the start location LatLng changes.
    LaunchedEffect(selectedStartLocation) {
        selectedStartLocation?.let { latLng ->
            Log.d("BookRideOneLogic", "Start LatLng changed. Fetching address.")
            hasFetchedForCurrentLocations = false // Reset fetch guard
            coroutineScope.launch {
                startAddress = getAddressFromLatLng(context, latLng)
                rideViewModel.updateAddresses(start = startAddress, destination = destinationAddress)
            }
        }
    }

    // Fetches the address when the destination location LatLng changes.
    LaunchedEffect(selectedDestinationLocation) {
        selectedDestinationLocation?.let { latLng ->
            Log.d("BookRideOneLogic", "Destination LatLng changed. Fetching address.")
            hasFetchedForCurrentLocations = false // Reset fetch guard
            coroutineScope.launch {
                destinationAddress = getAddressFromLatLng(context, latLng)
                rideViewModel.updateAddresses(start = startAddress, destination = destinationAddress)
            }
        }
    }

    // --- API Call Trigger Effect ---
    // This effect triggers the API call once all location data is present and valid.
    LaunchedEffect(selectedStartLocation, selectedDestinationLocation, startAddress, destinationAddress) {
        val canFetch = selectedStartLocation != null && selectedDestinationLocation != null &&
                startAddress != null && destinationAddress != null && !hasFetchedForCurrentLocations

        if (canFetch) {
            Log.d("BookRideOneLogic", "All conditions met. Fetching taxi info.")
            hasFetchedForCurrentLocations = true // Prevent re-fetching
            rideViewModel.fetchTaxiInformation(
                startLatLng = selectedStartLocation!!,
                destinationLatLng = selectedDestinationLocation!!
            )
        }
    }

    // --- UI Layout ---
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        TopBarBookRide()
        Spacer(modifier = Modifier.height(24.dp))

        RideBox(
            navController = navController,
            startAddress = startAddress,
            destinationAddress = destinationAddress,
            onPasteStartLocation = {
                getClipboardText(context)?.let { text ->
                    coroutineScope.launch {
                        parseGoogleMapsLinkForLatLng(text)?.let {
                            if (selectedStartLocation != it) selectedStartLocation = it
                        } ?: Toast.makeText(context, "Not a valid map link", Toast.LENGTH_SHORT).show()
                    }
                } ?: Toast.makeText(context, "Clipboard is empty", Toast.LENGTH_SHORT).show()
            },
            onPasteDestinationLocation = {
                getClipboardText(context)?.let { text ->
                    coroutineScope.launch {
                        parseGoogleMapsLinkForLatLng(text)?.let {
                            if (selectedDestinationLocation != it) selectedDestinationLocation = it
                        } ?: Toast.makeText(context, "Not a valid map link", Toast.LENGTH_SHORT).show()
                    }
                } ?: Toast.makeText(context, "Clipboard is empty", Toast.LENGTH_SHORT).show()
            }
        )

        Spacer(modifier = Modifier.height(16.dp))

        YangoRideOptionsDisplay(
            isLoading = isLoadingYango,
            optionsResponse = yangoTaxiOptions,
            errorMsg = yangoErrorMessage,
            onClearError = { rideViewModel.clearError() },
            onOptionSelected = { selectedOption ->
                rideViewModel.selectRideOption(selectedOption)
                navController.navigate("payment_screen")
            }
        )
    }
}

/**
 * A container for the start and destination location selection UI.
 *
 * @param navController For navigating to map selection screens.
 * @param startAddress The address string for the start location.
 * @param destinationAddress The address string for the destination.
 * @param onPasteStartLocation Lambda for handling paste action on the start location.
 * @param onPasteDestinationLocation Lambda for handling paste action on the destination.
 */
@Composable
fun RideBox(
    navController: NavController,
    startAddress: String?,
    destinationAddress: String?,
    onPasteStartLocation: () -> Unit,
    onPasteDestinationLocation: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .shadow(4.dp, RoundedCornerShape(24.dp))
            .background(Color.White, RoundedCornerShape(24.dp))
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Where to?", fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(16.dp))

        // Start Location Row
        LocationRow(
            locationText = startAddress ?: "Select starting point",
            onPasteClick = onPasteStartLocation,
            onClick = { navController.navigate("book_ride_start") }
        )

        Spacer(Modifier.height(12.dp))

        // Destination Location Row
        LocationRow(
            locationText = destinationAddress ?: "Select destination",
            onPasteClick = onPasteDestinationLocation,
            onClick = { navController.navigate("book_ride_destination") }
        )
    }
}

/**
 * A reusable row for displaying a location (start or destination).
 *
 * @param locationText The text to display for the location.
 * @param onPasteClick Lambda triggered when the paste button is clicked.
 * @param onClick Lambda triggered when the row itself is clicked.
 */
@Composable
fun LocationRow(
    locationText: String,
    onPasteClick: () -> Unit,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Color.LightGray, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = locationText,
            modifier = Modifier.weight(1f),
            maxLines = 1,
            color = if (locationText.startsWith("Select")) Color.Gray else Color.Black
        )
        IconButton(onClick = onPasteClick) {
            Icon(
                painter = painterResource(id = R.drawable.paste),
                contentDescription = "Paste Address",
                tint = Color.Gray
            )
        }
    }
}

/**
 * Displays the top app bar for the ride booking screen.
 */
@Composable
fun TopBarBookRide() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFFD32F2F))
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            "Book a Ride",
            color = Color.White,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

/**
 * Displays available Yandex taxi options, a loading indicator, or an error message.
 *
 * @param isLoading True if options are being fetched.
 * @param optionsResponse The full response from the Yandex API.
 * @param errorMsg An optional error message to display.
 * @param onClearError Lambda to clear the current error message.
 * @param onOptionSelected Lambda triggered when a user selects a ride option.
 */
@Composable
fun YangoRideOptionsDisplay(
    isLoading: Boolean,
    optionsResponse: YandexTaxiInfoResponse?,
    errorMsg: String?,
    onClearError: () -> Unit,
    onOptionSelected: (TaxiOptionResponse) -> Unit
) {
    val rideOptions = optionsResponse?.options

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        when {
            isLoading -> CircularProgressIndicator()
            errorMsg != null -> {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(errorMsg, color = MaterialTheme.colorScheme.error)
                    Button(onClick = onClearError) {
                        Text("Dismiss")
                    }
                }
            }
            !rideOptions.isNullOrEmpty() -> {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    items(rideOptions) { option ->
                        YangoOptionItem(option = option, onOptionSelected = onOptionSelected)
                    }
                }
            }
        }
    }
}

/**
 * Displays a single item for a Yandex taxi option.
 *
 * @param option The taxi option to display.
 * @param onOptionSelected Lambda to execute when this item is clicked.
 */
@Composable
fun YangoOptionItem(option: TaxiOptionResponse, onOptionSelected: (TaxiOptionResponse) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(4.dp, RoundedCornerShape(24.dp))
            .clip(RoundedCornerShape(24.dp))
            .background(Color.White)
            .clickable { onOptionSelected(option) }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Yango Logo
        Image(
            painter = painterResource(id = R.drawable.yango_logo),
            contentDescription = "Yango Logo",
            modifier = Modifier.size(40.dp)
        )
        Spacer(Modifier.width(16.dp))

        // Ride Class and Waiting Time
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = option.classText ?: "Unknown Class",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
            option.waitingTime?.let {
                val waitMinutes = (it / 60).roundToInt()
                Text("~ $waitMinutes min wait", fontSize = 14.sp, color = Color.Gray)
            }
        }

        // Price
        Text(
            text = option.priceText?.replace("dirham", "AED") ?: "N/A",
            fontWeight = FontWeight.Bold,
            fontSize = 20.sp,
            color = Color(0xFFD32F2F)
        )
    }
}


/**
 * Converts latitude and longitude coordinates into a human-readable address.
 *
 * @param context The application context.
 * @param latLng The coordinates to reverse geocode.
 * @return The address string, or a fallback message if an error occurs.
 */
private suspend fun getAddressFromLatLng(context: Context, latLng: LatLng): String {
    return withContext(Dispatchers.IO) {
        val geocoder = Geocoder(context, Locale.getDefault())
        try {
            // getFromLocation is deprecated on API 33+, but this handles both cases.
            @Suppress("DEPRECATION")
            val addresses = geocoder.getFromLocation(latLng.latitude, latLng.longitude, 1)
            addresses?.firstOrNull()?.getAddressLine(0)
        } catch (e: IOException) {
            Log.e("GeoCoder", "Failed to get address: ${e.message}")
            null
        } ?: "Lat: ${"%.5f".format(latLng.latitude)}, Lng: ${"%.5f".format(latLng.longitude)}"
    }
}


/**
 * Parses various Google Maps link formats to extract LatLng coordinates.
 * Handles short links (e.g., maps.app.goo.gl), full URLs with query params,
 * and plain "lat,lng" text.
 *
 * @param text The input string (URL or coordinates).
 * @return A [LatLng] object if parsing is successful, otherwise null.
 */
private suspend fun parseGoogleMapsLinkForLatLng(text: String): LatLng? {
    return withContext(Dispatchers.IO) {
        Log.d(TAG_LINK_PARSING, "Attempting to parse: $text")

        // Strategy 1: Direct "lat,lng" text parsing
        parseLatLngString(text)?.let { return@withContext it }

        // Strategy 2: URL parsing
        try {
            // Normalize URL-like string
            val urlString = if (!text.startsWith("http")) "https://$text" else text
            val decodedUrl = URLDecoder.decode(urlString, "UTF-8")
            val uri = Uri.parse(decodedUrl)

            // Handle short links
            if (uri.host?.contains("goo.gl") == true) {
                val resolvedUrl = resolveShortUrl(URL(urlString))
                if (resolvedUrl != null) {
                    return@withContext extractLatLngFromUrlString(resolvedUrl)
                }
            }

            // Handle standard Google Maps links
            extractLatLngFromUrlString(decodedUrl)?.let { return@withContext it }

        } catch (e: Exception) {
            Log.w(TAG_LINK_PARSING, "URL parsing failed: ${e.message}")
        }
        null
    }
}

/**
 * Extracts LatLng from a standard Google Maps URL string.
 * Checks common query parameters and path formats.
 *
 * @param urlString The decoded URL string.
 * @return A [LatLng] if found, otherwise null.
 */
private fun extractLatLngFromUrlString(urlString: String): LatLng? {
    val uri = Uri.parse(urlString)

    // Check 'q' parameter first (common for shared locations)
    uri.getQueryParameter("q")?.let { qParam ->
        parseLatLngString(qParam)?.let { return it }
    }

    // Check 'll' parameter
    uri.getQueryParameter("ll")?.let { llParam ->
        parseLatLngString(llParam)?.let { return it }
    }

    // Check path for @lat,lng pattern
    val atPattern = Pattern.compile("@(-?\\d+\\.\\d+),(-?\\d+\\.\\d+)")
    val atMatcher = atPattern.matcher(uri.path ?: "")
    if (atMatcher.find()) {
        try {
            val lat = atMatcher.group(1)?.toDouble()
            val lon = atMatcher.group(2)?.toDouble()
            if (lat != null && lon != null) return LatLng(lat, lon)
        } catch (e: NumberFormatException) {
            Log.w(TAG_LINK_PARSING, "Number format error in @ pattern")
        }
    }

    return null
}

/**
 * Parses a simple "latitude,longitude" string.
 *
 * @param text The string to parse.
 * @return A [LatLng] if the format is correct, otherwise null.
 */
private fun parseLatLngString(text: String): LatLng? {
    val parts = text.split(",").map { it.trim() }
    if (parts.size == 2) {
        try {
            val lat = parts[0].toDouble()
            val lon = parts[1].toDouble()
            // Basic validation for coordinate range
            if (lat in -90.0..90.0 && lon in -180.0..180.0) {
                return LatLng(lat, lon)
            }
        } catch (e: NumberFormatException) {
            // Not a valid coordinate pair, ignore.
        }
    }
    return null
}

/**
 * Resolves a short URL (like goo.gl) by following HTTP redirects.
 *
 * @param url The short URL to resolve.
 * @return The final, resolved URL string, or null on failure.
 */
private fun resolveShortUrl(url: URL): String? {
    var connection: HttpURLConnection? = null
    try {
        var redirectCount = 0
        var currentUrl = url
        while (redirectCount < MAX_REDIRECTS) {
            connection = currentUrl.openConnection() as HttpURLConnection
            connection.instanceFollowRedirects = false // We handle redirects manually
            connection.connectTimeout = 3000
            connection.readTimeout = 3000

            val status = connection.responseCode
            if (status != HttpURLConnection.HTTP_MOVED_PERM && status != HttpURLConnection.HTTP_MOVED_TEMP) {
                return currentUrl.toString() // Not a redirect, return current URL
            }

            // It's a redirect, get the new location
            val newLocation = connection.getHeaderField("Location")
            currentUrl = URL(newLocation)
            redirectCount++
            connection.disconnect()
        }
    } catch (e: Exception) {
        Log.e(TAG_LINK_PARSING, "Failed to resolve short URL: ${e.message}")
        return null
    } finally {
        connection?.disconnect()
    }
    return null
}
