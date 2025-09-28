package com.example.dorzmvp

import android.app.Application
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.dorzmvp.network.TaxiOptionResponse
import com.example.dorzmvp.ui.theme.DorzMVPTheme
import com.example.dorzmvp.ui.viewmodel.BookRideViewModel
import com.example.dorzmvp.ui.viewmodel.SavedAddressViewModel
import com.example.dorzmvp.ui.viewmodel.SavedAddressViewModelFactory
import com.google.android.libraries.places.api.Places

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        try {
            val applicationInfo = packageManager.getApplicationInfo(packageName, PackageManager.GET_META_DATA)
            val apiKey = applicationInfo.metaData.getString("com.google.android.geo.API_KEY")
            if (apiKey != null) {
                if (!Places.isInitialized()) {
                    Places.initialize(applicationContext, apiKey)
                }
            } else {
                Log.e("MainActivity", "API key for Places SDK not found in manifest.")
            }
        } catch (e: PackageManager.NameNotFoundException) {
            Log.e("MainActivity", "Failed to load meta-data, NameNotFound: " + e.message)
        } catch (e: NullPointerException) {
            Log.e("MainActivity", "Failed to load meta-data, NullPointer: " + e.message)
        }

        enableEdgeToEdge()
        setContent {
            val navController = rememberNavController()
            val bookRideViewModel: BookRideViewModel = viewModel()
            val context = LocalContext.current
            val savedAddressViewModel: SavedAddressViewModel = ViewModelProvider(
                this,
                SavedAddressViewModelFactory(context.applicationContext as Application)
            )[SavedAddressViewModel::class.java]

            NavHost(navController = navController, startDestination = "home_screen", builder = {
                composable("home_screen") {
                    HomeScreenUI(navController)
                }
                composable("book_ride_one"){
                    BookARideMainUI(navController, bookRideViewModel)
                }
                composable("book_ride_start"){
                    BookRideStartScreen(navController, savedAddressViewModel)
                }
                composable("book_ride_destination"){
                    BookRideDestinationScreen(navController, savedAddressViewModel)
                }
                // In MainActivity.kt, inside your NavHost

                composable("payment_screen") {
                    // Observe all the necessary data from the shared ViewModel
                    val rideOption by bookRideViewModel.selectedRideOption.observeAsState()
                    val startAddress by bookRideViewModel.startAddress.observeAsState()
                    val destinationAddress by bookRideViewModel.destinationAddress.observeAsState()

                    if (rideOption != null) {
                        // Pass the new address parameters to the PaymentScreen
                        PaymentScreen(
                            navController = navController,
                            rideOption = rideOption!!,
                            startAddress = startAddress,
                            destinationAddress = destinationAddress
                        )
                    } else {
                        Log.e("MainActivity", "Ride option in ViewModel was null.")
                        navController.popBackStack()
                    }
                }
                composable("book_ride_confirmed"){
                    /* TODO: Implement UI */
                }
                composable("book_ride_tracking"){
                    /* TODO: Implement UI */
                }
                composable("saved_addresses"){
                    SavedAddressesScreen(navController, savedAddressViewModel)
                }
                composable("your_rides"){
                    YourRidesScreen(navController)
                }
                composable("pick_location_for_saved_address_route") {
                    PickLocationScreen(navController, resultKey = "pickedLocationForSavedAddress")
                }
            } )
        }
    }
}
