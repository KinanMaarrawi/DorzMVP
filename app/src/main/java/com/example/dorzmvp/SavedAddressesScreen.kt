package com.example.dorzmvp

import android.content.Context
import android.location.Geocoder
import android.os.Build
import android.util.Log
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState // Added this import
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.dorzmvp.db.SavedAddress
import com.example.dorzmvp.ui.viewmodel.SavedAddressViewModel
import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SavedAddressesScreen(
    navController: NavController,
    viewModel: SavedAddressViewModel
) {
    val addresses by viewModel.savedAddresses.collectAsState()
    var showDialog by remember { mutableStateOf(false) }
    var addressToEdit by remember { mutableStateOf<SavedAddress?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Saved Addresses") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.primary,
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = {
                addressToEdit = null // Ensure we are adding a new address
                showDialog = true
            }) {
                Icon(Icons.Filled.Add, "Add new address")
            }
        }
    ) { paddingValues ->
        Column(modifier = Modifier
            .padding(paddingValues)
            .fillMaxSize()) {
            if (addresses.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No saved addresses yet. Click the '+' button to add one.")
                }
            } else {
                LazyColumn(modifier = Modifier.padding(16.dp)) {
                    items(addresses) { address ->
                        SavedAddressItem(address = address, onDelete = {
                            viewModel.deleteAddress(address)
                        }, onEdit = {
                            addressToEdit = address
                            showDialog = true
                        })
                        Divider()
                    }
                }
            }
        }

        if (showDialog) {
            AddEditAddressDialog(
                address = addressToEdit,
                onDismiss = { showDialog = false },
                onSave = {
                    if (addressToEdit == null) {
                        viewModel.addAddress(it)
                    } else {
                        viewModel.updateAddress(it)
                    }
                    showDialog = false
                },
                navController = navController
            )
        }
    }
}

@Composable
fun SavedAddressItem(
    address: SavedAddress,
    onDelete: () -> Unit,
    onEdit: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = address.name, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            Text(text = address.address, fontSize = 14.sp)
            Text(text = "Lat: ${String.format("%.4f", address.latitude)}, Lng: ${String.format("%.4f", address.longitude)}", fontSize = 12.sp)
        }
        IconButton(onClick = onEdit) {
            Icon(Icons.Filled.Edit, contentDescription = "Edit Address")
        }
        IconButton(onClick = onDelete) {
            Icon(Icons.Filled.Delete, contentDescription = "Delete Address")
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditAddressDialog(
    address: SavedAddress?,
    onDismiss: () -> Unit,
    onSave: (SavedAddress) -> Unit,
    navController: NavController // To navigate to map for location picking
) {
    var name by remember { mutableStateOf(address?.name ?: "") }
    var addressString by remember { mutableStateOf(address?.address ?: "") }
    var latitude by remember { mutableStateOf(address?.latitude ?: 0.0) }
    var longitude by remember { mutableStateOf(address?.longitude ?: 0.0) }
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // State to hold LatLng picked from map
    val pickedLocation = navController.currentBackStackEntry
        ?.savedStateHandle
        ?.getLiveData<LatLng>("pickedLocationForSavedAddress")
        ?.observeAsState()

    LaunchedEffect(pickedLocation?.value) {
        pickedLocation?.value?.let { latLng -> // latLng here should be correctly inferred as LatLng
            latitude = latLng.latitude
            longitude = latLng.longitude
            // Attempt to get address string from LatLng
            coroutineScope.launch {
                addressString = getAddressFromLatLng(context, latLng)
            }
            navController.currentBackStackEntry?.savedStateHandle?.remove<LatLng>("pickedLocationForSavedAddress")
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (address == null) "Add New Address" else "Edit Address") },
        text = {
            Column {
                TextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Name (e.g., Home, Work)") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text("Location:")
                Text("Lat: ${String.format("%.4f", latitude)}, Lng: ${String.format("%.4f", longitude)}")
                Text(addressString)
                Spacer(modifier = Modifier.height(8.dp))
                Button(onClick = {
                    navController.navigate("pick_location_for_saved_address_route")
                }) {
                    Text("Pick Location on Map")
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                if (name.isNotBlank() && (latitude != 0.0 || longitude != 0.0)) {
                    val newOrUpdatedAddress = SavedAddress(
                        id = address?.id ?: 0,
                        name = name,
                        address = addressString.ifBlank { "Lat: ${String.format("%.4f", latitude)}, Lng: ${String.format("%.4f", longitude)}" },
                        latitude = latitude,
                        longitude = longitude
                    )
                    onSave(newOrUpdatedAddress)
                }
            }) {
                Text("Save")
            }
        },
        dismissButton = {
            Button(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

// Placeholder for the map picking screen navigation - to be implemented
// composable("pick_location_for_saved_address_route") { PickLocationScreen(navController, "pickedLocationForSavedAddress") }

private suspend fun getAddressFromLatLng(context: Context, latLng: LatLng): String {
    return withContext(Dispatchers.IO) {
        val geocoder = Geocoder(context, Locale.getDefault())
        var addressText: String? = null
        try {
            // The Geocoder.getFromLocation method is deprecated in API 33 (TIRAMISU).
            // However, the alternative getFromLocation(latitude, longitude, maxResults, Geocoder.GeocodeListener)
            // requires a callback and is asynchronous, which is harder to integrate directly here
            // without significantly refactoring this suspend function to use callbacks or another Flow/Channel.
            // For simplicity, and given this is a helper, we'll stick to the deprecated version for now,
            // understanding it might need an update for very high API levels if issues arise or for best practice.
            @Suppress("DEPRECATION")
            val addresses = geocoder.getFromLocation(latLng.latitude, latLng.longitude, 1)

            if (addresses?.isNotEmpty() == true) {
                addressText = addresses[0].getAddressLine(0)
            }
        } catch (e: IOException) {
            Log.e("SavedAddressesScreen", "Error getting address from LatLng (Geocoder IOException): ${e.message}")
        } catch (e: IllegalArgumentException) {
            // This can happen if latitude or longitude are out of range
            Log.e("SavedAddressesScreen", "Invalid LatLng passed to geocoder: ${e.message}")
        }
        // Fallback to coordinates if addressText is null or empty
        addressText ?: "Lat: %.5f, Lng: %.5f".format(Locale.US, latLng.latitude, latLng.longitude)
    }
}
