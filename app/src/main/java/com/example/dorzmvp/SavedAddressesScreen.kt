package com.example.dorzmvp

import android.content.Context
import android.location.Geocoder
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.dorzmvp.db.SavedAddress
import com.example.dorzmvp.ui.viewmodel.SavedAddressViewModel
import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException
import java.util.Locale

// Logcat tag for debugging this screen.
private const val TAG = "SavedAddressesScreen"
// App-specific brand colors for consistent styling.
private val dorzRed = Color(0xFFD32F2F)
private val dorzWhite = Color.White

/**
 * A screen that displays, creates, edits, and deletes a user's saved addresses.
 *
 * It observes a list of addresses from the [SavedAddressViewModel] and provides
 * UI for list management. It launches a separate map screen (`PickLocationScreen`)
 * to get coordinates and handles the result via the NavController's `SavedStateHandle`.
 *
 * @param navController Controller for navigation, especially to the location picker.
 * @param viewModel ViewModel for interacting with the address data source.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SavedAddressesScreen(
    navController: NavController,
    viewModel: SavedAddressViewModel = viewModel()
) {
    // --- State Management ---
    val addresses by viewModel.savedAddresses.collectAsState()
    var showDialog by rememberSaveable { mutableStateOf(false) }
    var addressToEdit by remember { mutableStateOf<SavedAddress?>(null) }

    // State for the Add/Edit dialog, hoisted here to survive dialog dismissal
    // when navigating to the map picker.
    var nameState by rememberSaveable { mutableStateOf("") }
    var addressStringState by rememberSaveable { mutableStateOf("") }
    var latState by rememberSaveable { mutableStateOf(0.0) }
    var lngState by rememberSaveable { mutableStateOf(0.0) }
    var expectingLocationResult by rememberSaveable { mutableStateOf(false) }

    val context = LocalContext.current

    // --- Navigation Result Handling ---
    // Observe the result from the `PickLocationScreen`.
    val navBackStackEntry = navController.currentBackStackEntry
    val pickedLocationState: State<LatLng?> = navBackStackEntry
        ?.savedStateHandle
        ?.getLiveData<LatLng>("pickedLocationForSavedAddress")
        ?.observeAsState(initial = null) ?: remember { mutableStateOf(null) }
    val pickedLocationValue by pickedLocationState

    // This effect runs when a location is received from the picker screen.
    LaunchedEffect(pickedLocationValue, expectingLocationResult) {
        if (expectingLocationResult && pickedLocationValue != null) {
            val latLng = pickedLocationValue!!
            Log.d(TAG, "Received picked location: $latLng")

            // Update state with the new location data.
            latState = latLng.latitude
            lngState = latLng.longitude
            addressStringState = getAddressFromLatLng(context, latLng)

            // Consume the result and reset flags to prevent re-processing.
            navBackStackEntry?.savedStateHandle?.remove<LatLng>("pickedLocationForSavedAddress")
            expectingLocationResult = false
            showDialog = true // Re-open the dialog with the new data.
        }
    }

    // --- UI Layout ---
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Saved Addresses") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = dorzRed,
                    titleContentColor = dorzWhite
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    // Reset state for adding a new address.
                    addressToEdit = null
                    nameState = ""
                    addressStringState = ""
                    latState = 0.0
                    lngState = 0.0
                    showDialog = true
                },
                containerColor = dorzRed,
                contentColor = dorzWhite
            ) {
                Icon(Icons.Filled.Add, contentDescription = "Add new address")
            }
        }
    ) { paddingValues ->
        Column(modifier = Modifier.padding(paddingValues).fillMaxSize()) {
            if (addresses.isEmpty()) {
                // Display a message if the list is empty.
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No saved addresses. Tap '+' to add one.")
                }
            } else {
                // Display the list of saved addresses.
                LazyColumn(modifier = Modifier.padding(8.dp)) {
                    items(addresses) { address ->
                        SavedAddressItem(
                            address = address,
                            onDelete = { viewModel.deleteAddress(address) },
                            onEdit = {
                                // Populate state for editing an existing address.
                                addressToEdit = address
                                nameState = address.name
                                addressStringState = address.address
                                latState = address.latitude
                                lngState = address.longitude
                                showDialog = true
                            }
                        )
                        Divider()
                    }
                }
            }
        }

        if (showDialog) {
            AddEditAddressDialog(
                editingAddress = addressToEdit,
                currentName = nameState,
                currentAddressString = addressStringState,
                onNameChange = { nameState = it },
                onDismiss = { showDialog = false },
                onPickLocation = {
                    showDialog = false // Close dialog before navigating.
                    expectingLocationResult = true
                    navController.navigate("pick_location_for_saved_address_route")
                },
                onSave = { name, addressStr ->
                    // --- Save Logic with Validation ---
                    if (name.isBlank()) {
                        Toast.makeText(context, "Name cannot be empty.", Toast.LENGTH_SHORT).show()
                        return@AddEditAddressDialog
                    }
                    if (latState == 0.0 && lngState == 0.0) {
                        Toast.makeText(context, "Please pick a location from the map.", Toast.LENGTH_SHORT).show()
                        return@AddEditAddressDialog
                    }

                    val isDuplicate = addresses.any {
                        it.name.equals(name, ignoreCase = true) && it.id != (addressToEdit?.id ?: 0)
                    }
                    if (isDuplicate) {
                        Toast.makeText(context, "An address with this name already exists.", Toast.LENGTH_SHORT).show()
                        return@AddEditAddressDialog
                    }

                    // --- Create or Update Address ---
                    val finalAddress = SavedAddress(
                        id = addressToEdit?.id ?: 0,
                        name = name,
                        address = addressStr.ifBlank {
                            // Provide a fallback address string if geocoding failed.
                            "Lat: %.4f, Lng: %.4f".format(Locale.US, latState, lngState)
                        },
                        latitude = latState,
                        longitude = lngState
                    )

                    if (addressToEdit == null) {
                        viewModel.addAddress(finalAddress)
                        Log.d(TAG, "Added new address: $finalAddress")
                    } else {
                        viewModel.updateAddress(finalAddress)
                        Log.d(TAG, "Updated address: $finalAddress")
                    }
                    showDialog = false
                }
            )
        }
    }
}

/**
 * A single row item in the `LazyColumn` for displaying a saved address.
 *
 * @param address The address data to display.
 * @param onDelete Callback for when the delete button is clicked.
 * @param onEdit Callback for when the edit button is clicked.
 */
@Composable
fun SavedAddressItem(address: SavedAddress, onDelete: () -> Unit, onEdit: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp, horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
            Text(text = address.name, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            Text(text = address.address, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Row {
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
 * A dialog for adding a new address or editing an existing one.
 *
 * @param editingAddress The address being edited, or null if adding a new one.
 * @param currentName The current value for the name field.
 * @param currentAddressString The current value for the address string field.
 * @param onNameChange Callback when the name field changes.
 * @param onPickLocation Callback to trigger navigation to the map picker.
 * @param onDismiss Callback when the dialog is dismissed.
 * @param onSave Callback to save the address with the provided data.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditAddressDialog(
    editingAddress: SavedAddress?,
    currentName: String,
    currentAddressString: String,
    onNameChange: (String) -> Unit,
    onPickLocation: () -> Unit,
    onDismiss: () -> Unit,
    onSave: (name: String, addressStr: String) -> Unit
) {
    // Local state for the dialog's input fields, initialized from the parent's state.
    var nameInput by rememberSaveable(currentName) { mutableStateOf(currentName) }
    var addressInput by rememberSaveable(currentAddressString) { mutableStateOf(currentAddressString) }

    // Update local state if the external state changes (e.g., after picking a location).
    LaunchedEffect(currentName, currentAddressString) {
        nameInput = currentName
        addressInput = currentAddressString
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (editingAddress == null) "Add New Address" else "Edit Address", color = dorzRed) },
        text = {
            Column {
                TextField(
                    value = nameInput,
                    onValueChange = {
                        nameInput = it
                        onNameChange(it) // Also update parent state.
                    },
                    label = { Text("Name (e.g., Home, Work)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Spacer(Modifier.height(16.dp))

                // The address field is read-only as it's populated by the map picker.
                TextField(
                    value = addressInput,
                    onValueChange = { /* Read-only */ },
                    label = { Text("Address (from map)") },
                    modifier = Modifier.fillMaxWidth(),
                    readOnly = true,
                    minLines = 2
                )
                Spacer(Modifier.height(16.dp))

                Button(
                    onClick = onPickLocation,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = dorzRed, contentColor = dorzWhite)
                ) {
                    Text("Pick Location on Map")
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(nameInput, addressInput) },
                colors = ButtonDefaults.buttonColors(containerColor = dorzRed, contentColor = dorzWhite)
            ) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", color = dorzRed) }
        }
    )
}

/**
 * Converts geographic coordinates (`LatLng`) into a human-readable address string.
 * This is a suspending function that safely runs on a background thread.
 *
 * @param context The application context.
 * @param latLng The coordinates to reverse-geocode.
 * @return The address string, or a fallback coordinate string on failure.
 */
private suspend fun getAddressFromLatLng(context: Context, latLng: LatLng): String {
    return withContext(Dispatchers.IO) {
        val geocoder = Geocoder(context, Locale.getDefault())
        try {
            @Suppress("DEPRECATION")
            val addresses = geocoder.getFromLocation(latLng.latitude, latLng.longitude, 1)
            addresses?.firstOrNull()?.getAddressLine(0)
        } catch (e: IOException) {
            Log.e(TAG, "Geocoder failed for $latLng: ${e.message}")
            null
        } ?: "Lat: %.5f, Lng: %.5f".format(Locale.US, latLng.latitude, latLng.longitude)
    }
}
