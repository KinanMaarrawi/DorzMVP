/**
 * This file defines the primary user interface for the initial stage of booking a ride,
 * centered around the [BookARideMainUI] composable.
 *
 * Core Functionality of `BookARideMainUI`:
 * - **Location Management**: Manages the start and destination locations for a ride.
 *   - `selectedStartLocation` / `selectedDestinationLocation` (LatLng?): Store the precise
 *     geographic coordinates. Updated either by receiving results from map selection screens
 *     ([BookRideStartScreen], [BookRideDestinationScreen] via NavController's SavedStateHandle)
 *     or by parsing pasted Google Maps links.
 *   - `startAddress` / `destinationAddress` (String?): Store the human-readable string
 *     representations of the selected LatLng coordinates. These are fetched asynchronously
 *     using [getAddressFromLatLng] whenever their corresponding LatLng state changes.
 * - **ViewModel Interaction**: Collaborates with [com.example.dorzmvp.ui.viewmodel.BookRideViewModel] to:
 *   - Observe `isLoading`, `taxiOptions`, and `errorMessage` LiveData to reflect
 *     the current state of taxi information fetching (e.g., show loading indicators,
 *     display ride options, or present error messages).
 *   - Trigger `fetchTaxiInformation` in the ViewModel when both start and destination
 *     locations (both LatLng and their addresses) are successfully selected and resolved.
 * - **Navigation**: Uses [NavController] to:
 *   - Navigate to dedicated map screens (`book_ride_start`, `book_ride_destination`) for
 *     interactive location selection.
 *   - Receive the selected `LatLng` data back from these screens.
 * - **Clipboard Integration**: Allows users to paste Google Maps links to set locations.
 *   - `getClipboardText` retrieves text from the system clipboard.
 *   - `parseGoogleMapsLinkForLatLng` attempts to extract `LatLng` coordinates from the
 *     pasted text, handling various Google Maps URL formats and short links.
 * - **Reactive Updates via `LaunchedEffect`**:
 *   - When `selectedStartLocation` or `selectedDestinationLocation` changes, a
 *     `LaunchedEffect` triggers [getAddressFromLatLng] to update the corresponding
 *     address string.
 *   - Another `LaunchedEffect` observes `selectedStartLocation`, `selectedDestinationLocation`,
 *     `startAddress`, and `destinationAddress`. Once all four are non-null, it calls
 *     `rideViewModel.fetchTaxiInformation(...)`.
 * - **UI Composition**: Structures the screen using smaller, focused composables:
 *   - [TopBarBookRide]: Displays the screen title.
 *   - [RideBox]: Contains UI elements for selecting/displaying start and destination
 *     locations, including paste buttons.
 *   - [YangoRideOptionsDisplay]: Shows available taxi options from Yandex, or loading/error states.
 */
package com.example.dorzmvp

import android.content.ClipboardManager
import android.content.Context
import android.location.Geocoder
import android.os.Build
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add // Ensured correct Icon
import androidx.compose.material.icons.filled.KeyboardArrowDown
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
import java.io.UnsupportedEncodingException
import java.net.HttpURLConnection
import java.net.URI
import java.net.URL
import java.net.URLDecoder
import java.util.Locale
import java.util.regex.Pattern
import kotlin.math.roundToInt

// Logcat tag for debugging Google Maps link parsing activities.
private const val TAG_LINK_PARSING = "BookRideOneLinkParse"
// Maximum number of redirects to follow when resolving short URLs (e.g., maps.app.goo.gl).
private const val MAX_REDIRECTS = 5

/**
 * The main UI composable for the initial ride booking screen.
 * This screen allows users to select start and destination locations (either by navigating
 * to a map screen or by pasting a Google Maps link), and then view available taxi options
 * fetched based on these locations.
 *
 * @param navController The [NavController] used for navigating to other screens,
 *                      such as map selection screens ([BookRideStartScreen], [BookRideDestinationScreen]).
 * @param rideViewModel The [BookRideViewModel] instance, responsible for fetching taxi
 *                      information from Yandex and managing related UI state like loading
 *                      indicators, taxi options list, and error messages.
 */
@Composable
fun BookARideMainUI(navController: NavController, rideViewModel: BookRideViewModel = viewModel()) {
    // State for the selected start location's geographic coordinates (latitude and longitude).
    // Null if no start location has been selected yet. Persisted across configuration changes.
    var selectedStartLocation by rememberSaveable { mutableStateOf<LatLng?>(null) }
    // State for the human-readable address of the selected start location.
    // Null if no address has been fetched or if no start location is selected. Persisted.
    var startAddress by rememberSaveable { mutableStateOf<String?>(null) }
    // State for the selected destination location's geographic coordinates.
    // Null if no destination has been selected yet. Persisted.
    var selectedDestinationLocation by rememberSaveable { mutableStateOf<LatLng?>(null) }
    // State for the human-readable address of the selected destination location.
    // Null if no address has been fetched or if no destination is selected. Persisted.
    var destinationAddress by rememberSaveable { mutableStateOf<String?>(null) }

    // Provides the current Android application context, needed for services like Geocoder and ClipboardManager.
    val context = LocalContext.current
    // Coroutine scope tied to the composable's lifecycle for launching asynchronous tasks like address fetching or link parsing.
    val coroutineScope = rememberCoroutineScope()
    // The current NavBackStackEntry, used to access SavedStateHandle for retrieving results from other screens.
    val navBackStackEntry = navController.currentBackStackEntry

    // Observes the loading state from the ViewModel for Yandex taxi options.
    // True if taxi options are currently being fetched, false otherwise. Displayed as a CircularProgressIndicator.
    val isLoadingYango by rideViewModel.isLoading.observeAsState(initial = false)
    // Observes the list of available Yandex taxi options from the ViewModel. Displayed in YangoRideOptionsDisplay.
    val yangoTaxiOptions by rideViewModel.taxiOptions.observeAsState()
    // Observes any error messages from the ViewModel related to fetching Yandex taxi options. Displayed to the user.
    val yangoErrorMessage by rideViewModel.errorMessage.observeAsState()

    /**
     * Retrieves the primary clip text content from the system clipboard.
     * It specifically looks for plain text content.
     *
     * @param context The current Android [Context] to access system services.
     * @return The trimmed text content from the clipboard if it's plain text and not empty;
     *         otherwise, null. Logs information about clipboard state for debugging.
     */
    fun getClipboardText(context: Context): String? {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        if (!clipboard.hasPrimaryClip()) {
            Log.d(TAG_LINK_PARSING, "Clipboard has no primary clip.")
            return null
        }
        val primaryClip = clipboard.primaryClip
        if (primaryClip == null || primaryClip.itemCount == 0) {
            Log.d(TAG_LINK_PARSING, "Primary clip is null or empty.")
            return null
        }
        val item = primaryClip.getItemAt(0)
        // Coerce the clipboard item to text and trim leading/trailing whitespace.
        val coerced = item.coerceToText(context)?.toString()?.trim()
        Log.d(TAG_LINK_PARSING, "Coerced clipboard text: $coerced")
        return coerced?.takeIf { it.isNotEmpty() } // Return text only if it's not effectively blank.
    }

    // LaunchedEffect to react to changes in the selected start location's LatLng.
    // When `selectedStartLocation` is updated and non-null, it launches a coroutine
    // in the `coroutineScope` to fetch the human-readable address for these coordinates
    // using `getAddressFromLatLng`. The result updates the `startAddress` state.
    LaunchedEffect(selectedStartLocation) { // Keyed: re-runs if selectedStartLocation changes.
        selectedStartLocation?.let { latLng ->
            Log.d(TAG_LINK_PARSING, "Start location LatLng updated: $latLng. Fetching address.")
            coroutineScope.launch {
                startAddress = getAddressFromLatLng(context, latLng)
            }
        }
    }
    // Observes LiveData from the NavController's SavedStateHandle for the key "selectedStartLocation".
    // This mechanism is used to receive LatLng data passed back from the BookRideStartScreen
    // after the user selects a location on the map.
    // If new LatLng data is received and it's different from the current `selectedStartLocation`,
    // it updates the local `selectedStartLocation` state.
    // The data is then removed from the SavedStateHandle to prevent it from being processed again
    // on recomposition or configuration change.
    navBackStackEntry?.savedStateHandle?.getLiveData<LatLng>("selectedStartLocation")?.observeAsState()?.value?.let { latLng ->
        if (selectedStartLocation != latLng) { // Update only if the location is genuinely new.
            Log.d(TAG_LINK_PARSING, "Start location received from NavBackStack: $latLng")
            selectedStartLocation = latLng // Update the state.
        }
        // Crucial: Remove the data from SavedStateHandle to prevent re-processing on next recomposition
        // or if the screen is revisited after a configuration change.
        navBackStackEntry.savedStateHandle.remove<LatLng>("selectedStartLocation")
    }

    LaunchedEffect(selectedDestinationLocation) {
        selectedDestinationLocation?.let { latLng ->
            Log.d(TAG_LINK_PARSING, "Destination location LatLng updated: $latLng. Fetching address.")
            coroutineScope.launch {
                destinationAddress = getAddressFromLatLng(context, latLng)
            }
        }
    }
    navBackStackEntry?.savedStateHandle?.getLiveData<LatLng>("selectedDestinationLocation")?.observeAsState()?.value?.let { latLng ->
        if (selectedDestinationLocation != latLng) {
            Log.d(TAG_LINK_PARSING, "Destination location received from NavBackStack: $latLng")
            selectedDestinationLocation = latLng
        }
        navBackStackEntry.savedStateHandle.remove<LatLng>("selectedDestinationLocation")
    }

    LaunchedEffect(selectedStartLocation, selectedDestinationLocation, startAddress, destinationAddress) {
        if (selectedStartLocation != null && selectedDestinationLocation != null && startAddress != null && destinationAddress != null) {
            Log.d("BookARideMainUI", "Both locations selected and addresses resolved, fetching Yango info...")
            rideViewModel.fetchTaxiInformation(
                startLatLng = selectedStartLocation!!,
                destinationLatLng = selectedDestinationLocation!!
            )
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
            destinationAddress = destinationAddress,
            onPasteStartLocation = {
                val pastedText = getClipboardText(context)
                if (pastedText != null) {
                    Log.d(TAG_LINK_PARSING, "Pasting for Start Location: '$pastedText'")
                    coroutineScope.launch {
                        val parsedLatLng = parseGoogleMapsLinkForLatLng(pastedText)
                        if (parsedLatLng != null) {
                            if (selectedStartLocation != parsedLatLng) {
                                startAddress = null
                                selectedStartLocation = parsedLatLng
                            }
                            Log.d(TAG_LINK_PARSING, "Successfully parsed for start: $parsedLatLng")
                        } else {
                            Log.w(TAG_LINK_PARSING, "Failed to parse Google Maps link for start: $pastedText")
                            Toast.makeText(context, "Pasted text is not a valid Google Maps link", Toast.LENGTH_SHORT).show()
                        }
                    }
                } else {
                    Log.w(TAG_LINK_PARSING, "Clipboard empty or no text for start location")
                    Toast.makeText(context, "Clipboard empty or does not contain text", Toast.LENGTH_SHORT).show()
                }
            },
            onPasteDestinationLocation = {
                val pastedText = getClipboardText(context)
                if (pastedText != null) {
                    Log.d(TAG_LINK_PARSING, "Pasting for Destination Location: '$pastedText'")
                    coroutineScope.launch {
                        val parsedLatLng = parseGoogleMapsLinkForLatLng(pastedText)
                        if (parsedLatLng != null) {
                            if (selectedDestinationLocation != parsedLatLng) {
                                destinationAddress = null
                                selectedDestinationLocation = parsedLatLng
                            }
                            Log.d(TAG_LINK_PARSING, "Successfully parsed for destination: $parsedLatLng")
                        } else {
                            Log.w(TAG_LINK_PARSING, "Failed to parse Google Maps link for destination: $pastedText")
                            Toast.makeText(context, "Pasted text is not a valid Google Maps link or coordinates", Toast.LENGTH_SHORT).show()
                        }
                    }
                } else {
                    Log.w(TAG_LINK_PARSING, "Clipboard empty or no text for destination location")
                    Toast.makeText(context, "Clipboard empty or does not contain text", Toast.LENGTH_SHORT).show()
                }
            }
        )
        Spacer(modifier = Modifier.height(16.dp))
        YangoRideOptionsDisplay(
            isLoading = isLoadingYango,
            options = yangoTaxiOptions,
            errorMsg = yangoErrorMessage,
            onClearError = { rideViewModel.clearErrorMessage() }
        )
    }
}

/**
 * Orchestrates the parsing of a Google Maps link (or any text potentially containing coordinates) to extract a [LatLng].
 * This function employs a multi-step strategy:
 * 1. **Direct Text Parsing**: Attempts to parse coordinates directly from the input string if it's in a simple "lat,lng" format.
 * 2. **URL Normalization & Decoding**: Ensures the input has a scheme (defaults to "https") and URL-decodes it to handle percent-encoded characters.
 * 3. **Short Link Resolution**: If the URL appears to be a known short link (e.g., maps.app.goo.gl, goo.gl/maps), it attempts to resolve it to its full target URL using [resolveShortUrl]. The resolved URL is then parsed.
 * 4. **Structured URL Parsing**: If not a short link or after resolution, it attempts to extract coordinates from the (decoded) URL structure using [extractLatLngFromUrlString], which checks query parameters and common path patterns.
 * 5. **Fallback Text Parsing**: As a last resort, it re-attempts direct coordinate parsing on the original input string.
 *
 * This hierarchical approach aims to maximize the chances of successfully extracting coordinates from various link formats and user inputs.
 *
 * @param initialUrl The raw string input from the user (e.g., pasted from clipboard). This can be a full Google Maps URL, a short link, plain coordinates, or other text.
 * @return A [LatLng] object if coordinates are successfully parsed and validated; `null` otherwise.
 */
private suspend fun parseGoogleMapsLinkForLatLng(initialUrl: String): LatLng? {
    Log.d(TAG_LINK_PARSING, "parseGoogleMapsLinkForLatLng called with: '$initialUrl'")
    val trimmed = initialUrl.trim() // Remove leading/trailing whitespace from input.

    // Step 1: Quick direct text coordinate parse (handles simple "lat,lng" pasted).
    // This is a fast check for the most straightforward cases.
    parseCoordinatesFromText(trimmed)?.let {
        Log.d(TAG_LINK_PARSING, "Direct text coordinate parse succeeded: $it")
        return it
    }

    // Step 2: Normalize to a URL-like string for parsing and ensure it has a scheme.
    // Many users paste URLs without "http://" or "https://".
    val withScheme = if (trimmed.startsWith("http://", true) || trimmed.startsWith("https://", true)) {
        trimmed
    } else {
        "https://$trimmed" // Default to HTTPS for scheme-less inputs.
    }

    // Attempt to URL-decode the string to handle percent-encoded characters (e.g., %2C for comma).
    val decoded = try {
        URLDecoder.decode(withScheme, "UTF-8")
    } catch (e: UnsupportedEncodingException) {
        // Should not happen with UTF-8, but fall back to the un-decoded string if it does.
        Log.w(TAG_LINK_PARSING, "UTF-8 decode error; using raw withScheme", e)
        withScheme
    } catch (e: IllegalArgumentException) {
        // Handles cases where the percent-encoding is malformed.
        Log.w(TAG_LINK_PARSING, "Malformed percent-encoding, using raw withScheme", e)
        withScheme
    }

    // Step 3: Check for and resolve known short link patterns (e.g., maps.app.goo.gl).
    val host = try {
        URL(withScheme).host.lowercase(Locale.US) // Extract host for checking.
    } catch (e: Exception) {
        "" // If URL parsing fails here, host remains empty, won't match short link patterns.
    }

    val needsResolve = host.contains("maps.app.goo.gl") || // Google Maps specific shortener
                       trimmed.contains("goo.gl/maps") ||  // Older Google shortener for maps
                       host.contains("goo.gl")             // General Google shortener that might be used for maps

    if (needsResolve) {
        Log.d(TAG_LINK_PARSING, "Detected possible short link. Attempting to resolve: $withScheme")
        val resolved = resolveShortUrl(withScheme) // Network call to follow redirects.
        if (!resolved.isNullOrBlank()) {
            Log.d(TAG_LINK_PARSING, "Short link resolved to: $resolved")
            // Attempt to parse the fully resolved URL (which also needs decoding).
            val resolvedDecoded = try {
                URLDecoder.decode(resolved, "UTF-8")
            } catch (e: Exception) { resolved } // Fallback if decoding resolved URL fails.
            extractLatLngFromUrlString(resolvedDecoded)?.let {
                // If coordinates found in the resolved URL, return them.
                Log.d(TAG_LINK_PARSING, "Coordinates extracted from resolved short link: $it")
                return it
            }
        } else {
            Log.w(TAG_LINK_PARSING, "Failed to resolve short link: $withScheme")
            // If short link resolution fails, we might still try to parse `decoded` (original URL, decoded) below.
        }
    }

    // Step 4: Try parsing the (potentially original, now decoded) URL string using structured heuristics.
    // This is the primary method for full, non-shortened URLs.
    extractLatLngFromUrlString(decoded)?.let {
        Log.d(TAG_LINK_PARSING, "Coordinates extracted from decoded URL string: $it")
        return it
    }

    // Step 5: Last-ditch effort - try the generic coordinate regex across the raw initial input again.
    // This is a fallback if structured parsing of the decoded URL didn't work, but the original raw string might contain plain coordinates.
    if (decoded != trimmed) { // Only re-run if `decoded` was different from `trimmed` (i.e. decoding happened or scheme was added)
        parseCoordinatesFromText(initialUrl)?.let {
            Log.d(TAG_LINK_PARSING, "Fallback regex parse on initialUrl succeeded: $it")
            return it
        }
    }

    Log.d(TAG_LINK_PARSING, "No coordinates extracted after all attempts for: $initialUrl")
    return null // Return null if no coordinates could be extracted through any method.
}

/**
 * Parses a URL string (expected to be already URL-decoded) to extract [LatLng] coordinates
 * using multiple common Google Maps URL patterns and heuristics.
 *
 * The methods are tried in order of specificity and commonness:
 * 1.  **Query Parameters**: Checks for `q=lat,lng`, `query=lat,lng`, or `ll=lat,lng` in the URL's query string.
 *     The values of these parameters are then parsed using [parseCoordinatesFromText].
 * 2.  **Path `@` Pattern**: Looks for patterns like `/...@lat,lng,.../` in the URL path.
 *     This is a common pattern in Google Maps URLs for specific locations (e.g., `/maps/place/NamedPlace/@25.123,55.456,15z`).
 * 3.  **Path `!3d !4d` Pattern**: Searches for Google's internal `!3d<latitude>!4d<longitude>` tokens, often found in shared or embedded map URLs.
 * 4.  **Generic Text Parse**: As a final attempt within this function, it uses [parseCoordinatesFromText] on the entire `decodedUrl` string
 *     to catch any plain `lat,lng` occurrences that weren't caught by more specific patterns.
 *
 * @param decodedUrl The URL string to parse. It is assumed that this URL has already been
 *                   URL-decoded (e.g., using `URLDecoder.decode(url, "UTF-8")`) to ensure
 *                   characters like `%2C` (comma) are converted to their literal representation
 *                   before regex matching.
 * @return A [LatLng] object if coordinates are successfully found and validated within the ranges
 *         (latitude: -90 to +90, longitude: -180 to +180). Returns `null` if no valid
 *         coordinates are found through any of the heuristics.
 */
private fun extractLatLngFromUrlString(decodedUrl: String): LatLng? {
    Log.d(TAG_LINK_PARSING, "extractLatLngFromUrlString input: $decodedUrl")

    // Attempt to parse the string as a URI to safely access components like query and path.
    val uri: URI? = try {
        URI(decodedUrl)
    } catch (e: Exception) {
        // If direct URI creation fails (e.g., due to unescaped characters still present),
        // try creating via URL first, then converting to URI, which can be more lenient.
        try {
            URL(decodedUrl).toURI()
        } catch (e2: Exception) {
            Log.w(TAG_LINK_PARSING, "Could not create URI from decodedUrl: ${'$'}{e.message}; continuing with raw string methods for some patterns.")
            null // Proceed with null URI, some regex will run on raw decodedUrl.
        }
    }

    // Heuristic 1: Look for coordinates in query parameters (e.g., ?q=lat,lng or ?ll=lat,lng).
    try {
        val query = uri?.rawQuery // Use rawQuery to avoid issues with already decoded characters.
        if (!query.isNullOrBlank()) {
            val params = query.split("&")
            for (paramPair in params) {
                val keyValue = paramPair.split("=", limit = 2)
                if (keyValue.size == 2) {
                    // Decode key and value separately, in case they weren't fully decoded by the initial pass on `decodedUrl`
                    // or if the original query string had mixed encoding.
                    val key = try { URLDecoder.decode(keyValue[0], "UTF-8").lowercase(Locale.US) } catch (e: Exception) { keyValue[0].lowercase(Locale.US) }
                    val value = try { URLDecoder.decode(keyValue[1], "UTF-8") } catch (e: Exception) { keyValue[1] }
                    
                    if (key == "q" || key == "query" || key == "ll") {
                        parseCoordinatesFromText(value)?.let {
                            Log.d(TAG_LINK_PARSING, "Coordinates parsed from query param '$key' => $it")
                            return it // Return first valid coordinates found in relevant query params.
                        }
                    }
                }
            }
        }
    } catch (e: Exception) {
        // Log error but continue to other heuristics, as query parsing might fail for various reasons.
        Log.w(TAG_LINK_PARSING, "Error parsing query params: ${'$'}{e.message}")
    }

    // Heuristic 2: Look for @lat,lng in the path (e.g., /maps/place/Name/@25.2048,55.2708,17z).
    // This pattern is common for specific point-of-interest URLs.
    try {
        // Use rawPath if URI is available, otherwise fall back to the whole decodedUrl for matching.
        val pathToSearch = uri?.rawPath ?: decodedUrl 
        // Regex: @<latitude>,<longitude> followed by a comma, slash, or end of string.
        // Latitude/Longitude: optional sign, digits, optional decimal part.
        val atPattern = Pattern.compile("@(-?\d+(?:\.\d+)?),(-?\d+(?:\.\d+)?)(?:,|/|${'$'})")
        val atMatcher = atPattern.matcher(pathToSearch)
        if (atMatcher.find()) {
            val lat = atMatcher.group(1)?.toDoubleOrNull() // Safe toDouble: returns null on parse error.
            val lng = atMatcher.group(2)?.toDoubleOrNull()
            if (lat != null && lng != null && lat in -90.0..90.0 && lng in -180.0..180.0) {
                Log.d(TAG_LINK_PARSING, "Parsed @lat,lng from path: ($lat, $lng)")
                return LatLng(lat, lng)
            }
        }
    } catch (e: Exception) {
        Log.w(TAG_LINK_PARSING, "Error parsing @path pattern: ${'$'}{e.message}")
    }

    // Heuristic 3: Look for Google's internal !3d<latitude>!4d<longitude> tokens.
    // These are often found in data blobs or some types of shared/embedded map URLs.
    try {
        // Regex: !3d<latitude>!4d<longitude>
        // Latitude/Longitude: optional sign, digits, optional decimal part.
        val threeFourDPattern = Pattern.compile("!3d(-?\d+(?:\.\d+)?)!4d(-?\d+(?:\.\d+)?)")
        val matcher = threeFourDPattern.matcher(decodedUrl) // Search the entire decoded URL string.
        if (matcher.find()) {
            val lat = matcher.group(1)?.toDoubleOrNull()
            val lng = matcher.group(2)?.toDoubleOrNull()
            if (lat != null && lng != null && lat in -90.0..90.0 && lng in -180.0..180.0) {
                Log.d(TAG_LINK_PARSING, "Parsed !3d..!4d.. tokens: ($lat, $lng)")
                return LatLng(lat, lng)
            }
        }
    } catch (e: Exception) {
        Log.w(TAG_LINK_PARSING, "Error parsing !3d!4d pattern: ${'$'}{e.message}")
    }

    // Heuristic 4: Fallback to a general lat,lng text search within the entire decoded URL.
    // This can catch coordinates that are plainly in the URL but not in specific structures above.
    parseCoordinatesFromText(decodedUrl)?.let {
        Log.d(TAG_LINK_PARSING, "Parsed generic lat,lng in decodedUrl string via parseCoordinatesFromText: $it")
        return it
    }

    Log.d(TAG_LINK_PARSING, "No coordinates extracted from URL string via extractLatLngFromUrlString: $decodedUrl")
    return null // Return null if none of the heuristics found valid coordinates.
}

/**
 * Finds the first valid latitude,longitude pair within an arbitrary text string.
 * This function is used as a general-purpose coordinate extractor, often as a fallback or for parsing
 * simple user inputs like "25.123, 55.456".
 *
 * The regex pattern `([-+]?\d{1,3}(?:\.\d+)?)[,\s]+([-+]?\d{1,3}(?:\.\d+)?)` is designed to capture:
 * - An optional sign (`+` or `-`).
 * - 1 to 3 digits before the decimal point (covers valid latitude/longitude integer parts, e.g., -90 to 90, -180 to 180).
 * - An optional decimal part (`(?:\.\d+)`).
 * - These two numbers are separated by one or more commas or whitespace characters (`[,\s]+`).
 *
 * After matching, it converts the captured groups to doubles and validates them against
 * standard geographic coordinate ranges (latitude: -90 to +90, longitude: -180 to +180).
 *
 * @param text The input string to search for coordinates.
 * @return A [LatLng] object containing the first valid coordinate pair found, or `null` if no valid pair is found
 *         or if an error occurs during parsing (e.g., [NumberFormatException]).
 */
private fun parseCoordinatesFromText(text: String): LatLng? {
    // Regex to find two numbers separated by comma and/or space.
    // It expects up to 3 digits for the integer part of lat/lng (e.g., 180.000, -90.000).
    val coordPattern = Pattern.compile("([-+]?\d{1,3}(?:\.\d+)?)[,\s]+([-+]?\d{1,3}(?:\.\d+)?)")
    val matcher = coordPattern.matcher(text)
    
    // Iterate through all matches found by the regex in the text.
    while (matcher.find()) {
        try {
            // Safely attempt to convert captured groups to Double. `toDoubleOrNull` returns null on failure.
            val latString = matcher.group(1)
            val lngString = matcher.group(2)
            val lat = latString?.toDoubleOrNull()
            val lng = lngString?.toDoubleOrNull()

            if (lat != null && lng != null) {
                // Validate if the parsed numbers are within the acceptable geographic ranges.
                if (lat in -90.0..90.0 && lng in -180.0..180.0) {
                    Log.d(TAG_LINK_PARSING, "parseCoordinatesFromText found valid pair: ($lat, $lng) from strings ('$latString', '$lngString')")
                    return LatLng(lat, lng) // Return the first valid pair found.
                } else {
                    // Log if numbers are found but are outside the valid lat/lng range.
                    Log.d(TAG_LINK_PARSING, "Found numbers but out of range: ($lat, $lng) from strings ('$latString', '$lngString')")
                }
            }
        } catch (e: NumberFormatException) {
            // This catch block might be less likely to be hit if toDoubleOrNull is used extensively,
            // but kept for robustness or if direct toDouble was used.
            Log.w(TAG_LINK_PARSING, "NumberFormatException while parsing coordinates from text: ${'$'}{e.message}")
        } catch (e: IllegalStateException) {
            // This could happen if group numbers are incorrect, indicating a regex logic error.
            Log.e(TAG_LINK_PARSING, "IllegalStateException while accessing regex groups in parseCoordinatesFromText: ${'$'}{e.message}")
        }
    }
    // If the loop completes without returning, no valid coordinates were found.
    Log.d(TAG_LINK_PARSING, "parseCoordinatesFromText found no valid coordinate pairs in: '$text'")
    return null
}

private suspend fun resolveShortUrl(shortUrl: String): String? {
    Log.d(TAG_LINK_PARSING, "Attempting to resolve short URL: $shortUrl")
    return withContext(Dispatchers.IO) {
        var currentUrl = shortUrl
        for (i in 0 until MAX_REDIRECTS) {
            var connection: HttpURLConnection? = null
            try {
                Log.d(TAG_LINK_PARSING, "Redirect attempt #${'$'}{i + 1}: Connecting to $currentUrl")
                val url = URL(currentUrl)
                connection = url.openConnection() as HttpURLConnection
                connection.instanceFollowRedirects = false // Handle redirects manually
                connection.connectTimeout = 5000 // 5 seconds
                connection.readTimeout = 5000    // 5 seconds
                connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Android; Mobile; rv:100.0) Gecko/100.0 Firefox/100.0")

                val responseCode = connection.responseCode
                Log.d(TAG_LINK_PARSING, "Response code for $currentUrl: $responseCode")

                when (responseCode) {
                    HttpURLConnection.HTTP_OK -> {
                        Log.d(TAG_LINK_PARSING, "HTTP_OK for $currentUrl. This is the final URL.")
                        return@withContext currentUrl // Successfully resolved
                    }
                    HttpURLConnection.HTTP_MOVED_PERM,
                    HttpURLConnection.HTTP_MOVED_TEMP,
                    HttpURLConnection.HTTP_SEE_OTHER,
                    307, 308 -> {
                        val locationHeader = connection.getHeaderField("Location")
                        Log.d(TAG_LINK_PARSING, "Redirect code $responseCode. Location header: $locationHeader")
                        if (locationHeader.isNullOrBlank()) {
                            Log.e(TAG_LINK_PARSING, "Redirect response but Location header is null or blank.")
                            return@withContext null // Cannot follow redirect
                        }
                        val newUrl = URL(url, locationHeader) // Constructor handles resolving relative paths
                        currentUrl = newUrl.toExternalForm()
                        Log.d(TAG_LINK_PARSING, "Following redirect to: $currentUrl")
                    }
                    else -> {
                        Log.e(TAG_LINK_PARSING, "Unhandled response code: $responseCode for $currentUrl")
                        return@withContext null // Unhandled response
                    }
                }
            } catch (e: IOException) {
                Log.e(TAG_LINK_PARSING, "IOException while resolving $currentUrl: ${'$'}{e.message}", e)
                return@withContext null
            } catch (e: SecurityException) {
                Log.e(TAG_LINK_PARSING, "SecurityException while resolving $currentUrl: ${'$'}{e.message}", e)
                return@withContext null
            } catch (e: Exception) {
                Log.e(TAG_LINK_PARSING, "Generic exception while resolving $currentUrl: ${'$'}{e.message}", e)
                return@withContext null
            } finally {
                connection?.disconnect()
                Log.d(TAG_LINK_PARSING, "Disconnected from connection to $currentUrl (attempt #${'$'}{i + 1})")
            }
        }
        Log.w(TAG_LINK_PARSING, "Exceeded MAX_REDIRECTS ($MAX_REDIRECTS) for $shortUrl")
        null // Exceeded max redirects
    }
}

/**
 * Displays the list of Yango taxi options, or a loading indicator / error message.
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
                Button(onClick = onClearError) {
                    Text("Dismiss")
                }
            }
        }

        if (!isLoading && errorMsg == null) {
            options?.let { rideOptions ->
                if (rideOptions.isEmpty()) {
                    Text("No ride options available for this route currently.", modifier = Modifier.padding(16.dp))
                }
                LazyColumn(modifier = Modifier.fillMaxWidth()) {
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
 */
@Composable
private fun RideOptionItem(option: TaxiOptionResponse) {
    val waitingTimeSeconds = option.waitingTime ?: 0.0
    val waitingTimeMinutes = (waitingTimeSeconds / 60).roundToInt()

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp, horizontal = 16.dp)
            .border(1.dp, Color.LightGray, RoundedCornerShape(8.dp))
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(
                painter = painterResource(id = R.drawable.yango_logo),
                contentDescription = "Yango Logo",
                modifier = Modifier.size(40.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
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
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = option.priceText ?: "N/A",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

/**
 * A suspend function to convert Latitude/Longitude coordinates into a human-readable address string.
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
            Log.e("BookRideOne", "Error getting address from LatLng (Geocoder IOException): ${'$'}{e.message}")
        } catch (e: IllegalArgumentException) {
            Log.e("BookRideOne", "Invalid LatLng passed to geocoder: ${'$'}{e.message}")
        }
        addressText ?: "Lat: %.5f, Lng: %.5f".format(Locale.US, latLng.latitude, latLng.longitude)
    }
}

/**
 * Displays the top bar for the ride booking screen.
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
 * A composable that groups the start and destination location selection boxes, each with a paste icon.
 */
@Composable
fun RideBox(
    navController: NavController,
    selectedStartLocation: LatLng?,
    startAddress: String?,
    selectedDestinationLocation: LatLng?,
    destinationAddress: String?,
    onPasteStartLocation: () -> Unit,
    onPasteDestinationLocation: () -> Unit
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
                modifier = Modifier.padding(bottom = 16.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                LocationDisplayBox(
                    modifier = Modifier.weight(1f),
                    text = startAddress ?: selectedStartLocation?.let {
                        "Lat: %.5f, Lng: %.5f".format(Locale.US, it.latitude, it.longitude)
                    } ?: "Tap to select pickup point",
                    isPlaceholder = startAddress.isNullOrEmpty() && selectedStartLocation == null,
                    onClick = { navController.navigate("book_ride_start") },
                    cornerRadius = nestedBoxCornerRadius
                )
                Spacer(modifier = Modifier.width(8.dp))
                IconButton(onClick = onPasteStartLocation) {
                    Icon(
                        imageVector = Icons.Filled.Add, // Ensured correct Icon
                        contentDescription = "Paste Start Location"
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))
            Icon(
                imageVector = Icons.Default.KeyboardArrowDown,
                contentDescription = null,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
            Spacer(modifier = Modifier.height(4.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                LocationDisplayBox(
                    modifier = Modifier.weight(1f),
                    text = destinationAddress ?: selectedDestinationLocation?.let {
                        "Lat: %.5f, Lng: %.5f".format(Locale.US, it.latitude, it.longitude)
                    } ?: "Tap to select destination",
                    isPlaceholder = destinationAddress.isNullOrEmpty() && selectedDestinationLocation == null,
                    onClick = { navController.navigate("book_ride_destination") },
                    cornerRadius = nestedBoxCornerRadius
                )
                Spacer(modifier = Modifier.width(8.dp))
                IconButton(onClick = onPasteDestinationLocation) {
                    Icon(
                        imageVector = Icons.Filled.Add, // Ensured correct Icon
                        contentDescription = "Paste Destination Location"
                    )
                }
            }
        }
    }
}

/**
 * A reusable composable for displaying a tappable location field.
 */
@Composable
private fun LocationDisplayBox(
    modifier: Modifier = Modifier,
    text: String,
    isPlaceholder: Boolean,
    onClick: () -> Unit,
    cornerRadius: Dp
) {
    Box(
        modifier = modifier
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
