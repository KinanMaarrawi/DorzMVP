package com.example.dorzmvp

import android.content.Context
import android.location.Geocoder
import android.util.Log
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.dorzmvp.db.SavedAddress
import com.example.dorzmvp.ui.viewmodel.SavedAddressViewModel
import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException
import java.util.Locale

/**
 * This file defines the user interface for managing saved addresses within the application.
 * The primary composable, [SavedAddressesScreen], allows users to view a list of their
 * previously saved addresses, add new addresses, edit existing ones, or delete them.
 * Address data is persisted locally using Room and managed via the [SavedAddressViewModel].
 *
 * The screen integrates with a map picking functionality: users can navigate to a map screen
 * to select a location, and upon returning, the selected coordinates are used to pre-fill
 * the address details in an "Add/Edit" dialog. Reverse geocoding is performed to attempt
 * to convert latitude/longitude coordinates into a human-readable address string.
 * The UI consists of a list display for addresses, a Floating Action Button (FAB) for adding
 * new addresses, and an AlertDialog for the add/edit operations.
 */

private const val TAG = "SavedAddressesScreen" // Log tag for this screen

/**
 * Composable function for the Saved Addresses screen.
 * Displays a list of saved addresses and provides options to add, edit, or delete them.
 * It also handles receiving picked locations from a map screen.
 *
 * @param navController The [NavController] used for navigation, particularly to and from the map picking screen.
 * @param viewModel The [SavedAddressViewModel] instance used to interact with the address data.
 *                  Defaults to a ViewModel provided by `viewModel()`.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SavedAddressesScreen(
    navController: NavController,
    viewModel: SavedAddressViewModel = viewModel()
) {
    // Collect the list of saved addresses from the ViewModel as State.
    // The UI will recompose whenever this list changes.
    val addresses by viewModel.savedAddresses.collectAsState()

    // State to control the visibility of the Add/Edit address dialog.
    var showDialog by rememberSaveable { mutableStateOf(false) }
    // State to hold the address being edited. Null if adding a new address.
    var addressToEdit by rememberSaveable { mutableStateOf<SavedAddress?>(null) }

    // State variables for the fields in the Add/Edit dialog.
    // These are remembered across recompositions and saved across configuration changes.
    var nameState by rememberSaveable { mutableStateOf("") }
    var addressStringState by rememberSaveable { mutableStateOf("") } // Stores the human-readable address string
    var latState by rememberSaveable { mutableStateOf(0.0) }
    var lngState by rememberSaveable { mutableStateOf(0.0) }
    // Flag to indicate if we are currently waiting for a location result from the map picker.
    var expectingLocationResult by rememberSaveable { mutableStateOf(false) }


    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // --- Location Picker Result Handling ---
    val navBackStackEntry = navController.currentBackStackEntry
    // Observe LiveData from SavedStateHandle for a picked location from another screen.
    // Provide a default State if the LiveData source is null.
    val pickedLocationState: State<LatLng?> = navBackStackEntry
        ?.savedStateHandle
        ?.getLiveData<LatLng>("pickedLocationForSavedAddress")
        ?.observeAsState(initial = null) ?: remember { mutableStateOf(null) }
    val pickedLocationValue by pickedLocationState // Delegate to get the LatLng? value

    // Effect to handle the picked location when it changes.
    LaunchedEffect(pickedLocationValue, expectingLocationResult) {
        pickedLocationValue?.let { latLng ->
            if (expectingLocationResult) { // Only process if we were expecting this result
                Log.d(TAG, "Received picked location: $latLng")
                latState = latLng.latitude
                lngState = latLng.longitude
                // Attempt to get a human-readable address from the picked LatLng.
                addressStringState = getAddressFromLatLng(context, latLng)

                // Clear the picked location from SavedStateHandle to prevent re-processing.
                navBackStackEntry?.savedStateHandle?.remove<LatLng>("pickedLocationForSavedAddress")
                showDialog = true // Re-open or ensure dialog is shown with new location data.
                expectingLocationResult = false // Reset the flag
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Saved Addresses") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.primary
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = {
                addressToEdit = null // Clear any address being edited (implies adding new)
                // Reset states for a new address entry
                nameState = ""
                addressStringState = "Tap 'Pick Location' or fill manually"
                latState = 0.0
                lngState = 0.0
                showDialog = true
            }) {
                Icon(Icons.Filled.Add, contentDescription = "Add new address")
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
        ) {
            if (addresses.isEmpty()) {
                // Display a message if there are no saved addresses.
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No saved addresses yet. Click the '+' button to add one.")
                }
            } else {
                // Display the list of saved addresses.
                LazyColumn(modifier = Modifier.padding(16.dp)) {
                    items(addresses) { address ->
                        SavedAddressItem(
                            address = address,
                            onDelete = { viewModel.deleteAddress(address) },
                            onEdit = {
                                addressToEdit = address // Set the address to be edited
                                // Populate dialog states with the selected address details.
                                nameState = address.name
                                addressStringState = address.address
                                latState = address.latitude
                                lngState = address.longitude
                                showDialog = true
                            }
                        )
                        Divider() // Visual separator between items.
                    }
                }
            }
        }

        // Show the Add/Edit dialog if showDialog is true.
        if (showDialog) {
            AddEditAddressDialog(
                editingAddress = addressToEdit, // Pass the address being edited (or null if new)
                currentName = nameState,
                currentAddressString = addressStringState,
                currentLat = latState,
                currentLng = lngState,
                onNameChange = { nameState = it },
                onAddressChange = { addressStringState = it }, // Allow manual address editing
                onPickLocation = {
                    showDialog = false // Close dialog temporarily
                    expectingLocationResult = true // Set flag that we are waiting for map result
                    navController.navigate("pick_location_for_saved_address_route")
                },
                onDismiss = { showDialog = false },
                onSave = { name, addressStr, lat, lng ->
                    val finalAddress = SavedAddress(
                        id = addressToEdit?.id ?: 0, // Use existing ID if editing, 0 for Room to autoGenerate
                        name = name,
                        address = if (addressStr.isBlank() || addressStr == "Tap 'Pick Location' or fill manually") {
                            // If address string is still blank or default, generate one from LatLng
                            "Lat: %.4f, Lng: %.4f".format(Locale.US, lat, lng)
                        } else {
                            addressStr // Use the manually entered or geocoded address
                        },
                        latitude = lat,
                        longitude = lng
                    )
                    if (addressToEdit == null) {
                        viewModel.addAddress(finalAddress)
                        Log.d(TAG, "Added new address: $finalAddress")
                    } else {
                        viewModel.updateAddress(finalAddress)
                        Log.d(TAG, "Updated address: $finalAddress")
                    }
                    showDialog = false // Close the dialog after saving.
                },
                coroutineScope = coroutineScope,
                context = context
            )
        }
    }
}

/**
 * Composable function for displaying a single saved address item in the list.
 *
 * @param address The [SavedAddress] object to display.
 * @param onDelete Lambda function to be invoked when the delete button is clicked.
 * @param onEdit Lambda function to be invoked when the edit button is clicked.
 */
@Composable
fun SavedAddressItem(
    address: SavedAddress,
    onDelete: () -> Unit,
    onEdit: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp, horizontal = 8.dp), // Added horizontal padding for better spacing
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween // Distribute space
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) { // Add padding to prevent text touching icons
            Text(text = address.name, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            Text(text = address.address, fontSize = 14.sp, style = MaterialTheme.typography.bodyMedium)
            Text(
                text = "Lat: %.4f, Lng: %.4f".format(Locale.US, address.latitude, address.longitude),
                fontSize = 12.sp,
                style = MaterialTheme.typography.bodySmall
            )
        }
        Row { // Group icons together
            IconButton(onClick = onEdit) {
                Icon(Icons.Filled.Edit, contentDescription = "Edit ${address.name}")
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Filled.Delete, contentDescription = "Delete ${address.name}")
            }
        }
    }
}

/**
 * Composable function for the Add/Edit Address dialog.
 * Allows users to input or modify address details including name, address string, and location.
 * Provides an option to pick a location from a map.
 *
 * @param editingAddress The [SavedAddress] being edited, or null if adding a new address.
 * @param currentName The current name value for the input field.
 * @param currentAddressString The current address string for display/edit.
 * @param currentLat The current latitude value.
 * @param currentLng The current longitude value.
 * @param onNameChange Lambda invoked when the name input changes.
 * @param onAddressChange Lambda invoked when the address string input changes.
 * @param onPickLocation Lambda invoked when the "Pick Location on Map" button is clicked.
 * @param onDismiss Lambda invoked when the dialog is dismissed.
 * @param onSave Lambda invoked when the save button is clicked, providing the final address details.
 * @param coroutineScope The [CoroutineScope] for launching asynchronous tasks like geocoding.
 * @param context The current [Context].
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditAddressDialog(
    editingAddress: SavedAddress?,
    currentName: String,
    currentAddressString: String,
    currentLat: Double,
    currentLng: Double,
    onNameChange: (String) -> Unit,
    onAddressChange: (String) -> Unit,
    onPickLocation: () -> Unit,
    onDismiss: () -> Unit,
    onSave: (name: String, addressStr: String, lat: Double, lng: Double) -> Unit,
    coroutineScope: CoroutineScope,
    context: Context
) {
    var nameInput by rememberSaveable(currentName) { mutableStateOf(currentName) }
    var addressInput by rememberSaveable(currentAddressString) { mutableStateOf(currentAddressString) }
    var latInput by rememberSaveable(currentLat) { mutableStateOf(currentLat) }
    var lngInput by rememberSaveable(currentLng) { mutableStateOf(currentLng) }
    var isGeocoding by remember { mutableStateOf(false) }

    // Update local state if the props for currentLat/Lng change (e.g., from map picker)
    LaunchedEffect(currentLat, currentLng, currentAddressString) {
        latInput = currentLat
        lngInput = currentLng
        addressInput = currentAddressString
    }


    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (editingAddress == null) "Add New Address" else "Edit Address") },
        text = {
            Column {
                TextField(
                    value = nameInput,
                    onValueChange = { nameInput = it; onNameChange(it) }, // Update parent state immediately if needed for validation
                    label = { Text("Name (e.g., Home, Work)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(16.dp)) // Increased spacing

                Text("Location Details:", style = MaterialTheme.typography.titleSmall)
                Spacer(modifier = Modifier.height(8.dp))

                // Display current Lat/Lng
                Text("Coordinates: Lat: %.4f, Lng: %.4f".format(Locale.US, latInput, lngInput))
                Spacer(modifier = Modifier.height(4.dp))

                // Display current address string (can be from geocoding or manual input)
                if (isGeocoding) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Fetching address...")
                    }
                } else {
                    TextField(
                        value = addressInput,
                        onValueChange = { addressInput = it; onAddressChange(it) },
                        label = { Text("Address (Street, City)")},
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 2
                    )
                }
                Spacer(modifier = Modifier.height(16.dp)) // Increased spacing

                Button(
                    onClick = onPickLocation,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Pick Location on Map")
                }
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = {
                        coroutineScope.launch {
                            isGeocoding = true
                            // Re-fetch address based on current lat/lng in dialog
                            val geocodedAddress = getAddressFromLatLng(context, LatLng(latInput, lngInput))
                            addressInput = geocodedAddress
                            onAddressChange(geocodedAddress) // Update parent state
                            isGeocoding = false
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = latInput != 0.0 || lngInput != 0.0 // Enable if coordinates are set
                ) {
                    Text("Get Address from Coordinates")
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                if (nameInput.isNotBlank()) {
                    onSave(nameInput, addressInput, latInput, lngInput)
                }
                // Optionally add validation/toast if name is blank
            }) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

/**
 * Suspended function to get a human-readable address from [LatLng] coordinates using [Geocoder].
 * This function should be called from a coroutine using a [Dispatchers.IO] context
 * as geocoding can be a long-running network operation.
 *
 * @param context The application [Context].
 * @param latLng The [LatLng] coordinates to convert.
 * @return A [String] representing the address, or a fallback string with Lat/Lng if geocoding fails.
 */
private suspend fun getAddressFromLatLng(context: Context, latLng: LatLng): String {
    // Ensure this runs on a background thread.
    return withContext(Dispatchers.IO) {
        val geocoder = Geocoder(context, Locale.getDefault())
        var addressText: String? = null
        try {
            // The getFromLocation method is deprecated in API 33+.
            // Consider using the new Geocoder.GeocodeListener for API 33+.
            @Suppress("DEPRECATION")
            val addresses = geocoder.getFromLocation(latLng.latitude, latLng.longitude, 1)

            if (!addresses.isNullOrEmpty()) {
                addressText = addresses[0].getAddressLine(0) // Get the first address line.
            }
        } catch (e: IOException) {
            // Handles network errors or other I/O issues.
            Log.e(TAG, "Geocoder IOException for $latLng: ${e.message}", e)
        } catch (e: IllegalArgumentException) {
            // Handles invalid latitude or longitude values.
            Log.e(TAG, "Invalid LatLng for Geocoder: $latLng", e)
        }

        // Fallback to Lat/Lng string if addressText is null or geocoding fails.
        addressText ?: "Lat: %.5f, Lng: %.5f".format(Locale.US, latLng.latitude, latLng.longitude)
    }
}
