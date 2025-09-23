package com.example.dorzmvp

import android.content.Context
import android.location.Geocoder
import android.util.Log
import android.widget.Toast // Added for validation messages
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
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.graphics.Color // Required for custom colors
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

private const val TAG = "SavedAddressesScreen" 

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SavedAddressesScreen(
    navController: NavController,
    viewModel: SavedAddressViewModel = viewModel()
) {
    val addresses by viewModel.savedAddresses.collectAsState()
    var showDialog by rememberSaveable { mutableStateOf(false) }
    var addressToEdit by rememberSaveable { mutableStateOf<SavedAddress?>(null) }

    var nameState by rememberSaveable { mutableStateOf("") }
    var addressStringState by rememberSaveable { mutableStateOf("") } 
    var latState by rememberSaveable { mutableStateOf(0.0) }
    var lngState by rememberSaveable { mutableStateOf(0.0) }
    var expectingLocationResult by rememberSaveable { mutableStateOf(false) }

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val navBackStackEntry = navController.currentBackStackEntry
    val pickedLocationState: State<LatLng?> = navBackStackEntry
        ?.savedStateHandle
        ?.getLiveData<LatLng>("pickedLocationForSavedAddress")
        ?.observeAsState(initial = null) ?: remember { mutableStateOf(null) }
    val pickedLocationValue by pickedLocationState

    LaunchedEffect(pickedLocationValue, expectingLocationResult) {
        pickedLocationValue?.let { latLng ->
            if (expectingLocationResult) {
                Log.d(TAG, "Received picked location: $latLng")
                latState = latLng.latitude
                lngState = latLng.longitude
                addressStringState = getAddressFromLatLng(context, latLng)
                navBackStackEntry?.savedStateHandle?.remove<LatLng>("pickedLocationForSavedAddress")
                showDialog = true
                expectingLocationResult = false
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Saved Addresses") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFFD32F2F), 
                    titleContentColor = Color.White    
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    addressToEdit = null
                    nameState = ""
                    addressStringState = "" 
                    latState = 0.0
                    lngState = 0.0
                    showDialog = true
                },
                containerColor = Color(0xFFD32F2F), 
                contentColor = Color.White         
            ) {
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
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No saved addresses yet. Click the '+' button to add one.")
                }
            } else {
                LazyColumn(modifier = Modifier.padding(16.dp)) {
                    items(addresses) { address ->
                        SavedAddressItem(
                            address = address,
                            onDelete = { viewModel.deleteAddress(address) },
                            onEdit = {
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
                currentLat = latState, 
                currentLng = lngState, 
                onNameChange = { nameState = it },
                onAddressChange = { addressStringState = it },
                onPickLocation = {
                    showDialog = false
                    expectingLocationResult = true
                    navController.navigate("pick_location_for_saved_address_route")
                },
                onDismiss = { showDialog = false },
                onSave = { name, addressStr, lat, lng ->
                    val currentAddresses = viewModel.savedAddresses.value
                    var validationPassed = true
                    var toastMessage = ""

                    if (name.isBlank() && (lat == 0.0 && lng == 0.0)) {
                        toastMessage = "Name and location are required."
                        validationPassed = false
                    } else if (name.isBlank()) {
                        toastMessage = "Name cannot be empty."
                        validationPassed = false
                    } else if (lat == 0.0 && lng == 0.0) {
                        toastMessage = "Location is required. Please pick a location."
                        validationPassed = false
                    } else {
                        val isDuplicateName = if (addressToEdit == null) {
                            currentAddresses.any { it.name.equals(name, ignoreCase = true) }
                        } else {
                            currentAddresses.any { it.name.equals(name, ignoreCase = true) && it.id != addressToEdit!!.id }
                        }
                        if (isDuplicateName) {
                            toastMessage = "An address with this name already exists."
                            validationPassed = false
                        }
                    }

                    if (validationPassed) {
                        val finalAddress = SavedAddress(
                            id = addressToEdit?.id ?: 0,
                            name = name,
                            address = if (addressStr.isBlank()) {
                                "Lat: %.4f, Lng: %.4f".format(Locale.US, lat, lng)
                            } else {
                                addressStr
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
                        showDialog = false
                    } else {
                        Toast.makeText(context, toastMessage, Toast.LENGTH_LONG).show()
                    }
                },
                coroutineScope = coroutineScope,
                context = context
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
            .padding(vertical = 12.dp, horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
            Text(text = address.name, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            Text(text = address.address, fontSize = 14.sp, style = MaterialTheme.typography.bodyMedium)
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
    val latForSave = currentLat
    val lngForSave = currentLng

    LaunchedEffect(currentName, currentAddressString) {
        nameInput = currentName
        addressInput = currentAddressString
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (editingAddress == null) "Add New Address" else "Edit Address", color = Color(0xFFD32F2F)) },
        text = {
            Column {
                TextField(
                    value = nameInput,
                    onValueChange = { nameInput = it; onNameChange(it) },
                    label = { Text("Name (e.g., Home, Work)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(16.dp))

                TextField(
                    value = addressInput,
                    onValueChange = { addressInput = it; onAddressChange(it) },
                    label = { Text("Address (auto-filled from map)") }, 
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2
                )
                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = onPickLocation,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFD32F2F),
                        contentColor = Color.White
                    )
                ) {
                    Text("Pick Location on Map")
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    // Name and address are taken directly from the dialog's local state (nameInput, addressInput)
                    // Lat and Lng are taken from the values passed into the dialog (latForSave, lngForSave)
                    // which are updated by the map picker via the parent screen's state.
                    onSave(nameInput, addressInput, latForSave, lngForSave)
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFD32F2F),
                    contentColor = Color.White
                )
            ) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", color = Color(0xFFD32F2F)) }
        }
    )
}

private suspend fun getAddressFromLatLng(context: Context, latLng: LatLng): String {
    return withContext(Dispatchers.IO) {
        val geocoder = Geocoder(context, Locale.getDefault())
        var addressText: String? = null
        try {
            @Suppress("DEPRECATION")
            val addresses = geocoder.getFromLocation(latLng.latitude, latLng.longitude, 1)

            if (!addresses.isNullOrEmpty()) {
                addressText = addresses[0].getAddressLine(0)
            }
        } catch (e: IOException) {
            Log.e(TAG, "Geocoder IOException for $latLng: ${e.message}", e)
        } catch (e: IllegalArgumentException) {
            Log.e(TAG, "Invalid LatLng for Geocoder: $latLng", e)
        }
        addressText ?: "Lat: %.5f, Lng: %.5f".format(Locale.US, latLng.latitude, latLng.longitude)
    }
}
