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
import com.example.dorzmvp.ui.viewmodel.BookRideViewModel
import com.example.dorzmvp.ui.viewmodel.RideHistoryViewModel
import com.example.dorzmvp.ui.viewmodel.RideHistoryViewModelFactory
import com.example.dorzmvp.ui.viewmodel.SavedAddressViewModel
import com.example.dorzmvp.ui.viewmodel.SavedAddressViewModelFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.libraries.places.api.Places
import java.net.URLDecoder
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

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

            val rideHistoryViewModel: RideHistoryViewModel = ViewModelProvider(
                this,
                RideHistoryViewModelFactory(context.applicationContext as Application)
            )[RideHistoryViewModel::class.java]

            NavHost(navController = navController, startDestination = "home_screen") {
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
                composable("payment_screen") {
                    val rideOption by bookRideViewModel.selectedRideOption.observeAsState()
                    val startAddress by bookRideViewModel.startAddress.observeAsState()
                    val destinationAddress by bookRideViewModel.destinationAddress.observeAsState()

                    if (rideOption != null) {
                        PaymentScreen(
                            navController = navController,
                            rideOption = rideOption!!,
                            startAddress = startAddress,
                            destinationAddress = destinationAddress,
                            rideHistoryViewModel = rideHistoryViewModel
                        )
                    } else {
                        Log.e("MainActivity", "Ride option in ViewModel was null.")
                        navController.popBackStack()
                    }
                }
                composable("book_ride_confirmed"){
                    // Observe the latest ride details from the ViewModel
                    val start by bookRideViewModel.startLocation.observeAsState()
                    val dest by bookRideViewModel.destinationLocation.observeAsState()
                    RideConfirmedScreen(
                        navController = navController,
                        startLocation = start,
                        destinationLocation = dest
                    )
                }
                // REMOVED old dummy composable
                // composable("book_ride_tracking"){
                //     /* TODO: Implement UI */
                // }

                // ADDED new route for tracking with arguments
                composable(
                    "book_ride_tracking/{startLocation}/{destinationLocation}",
                    arguments = listOf(
                        navArgument("startLocation") { type = NavType.StringType },
                        navArgument("destinationLocation") { type = NavType.StringType }
                    )
                ) { backStackEntry ->
                    val startArg = backStackEntry.arguments?.getString("startLocation")
                    val destArg = backStackEntry.arguments?.getString("destinationLocation")

                    fun stringToLatLng(str: String?): LatLng? {
                        if (str == null) return null
                        val parts = URLDecoder.decode(str, StandardCharsets.UTF_8.name()).split(",")
                        return if (parts.size == 2) LatLng(parts[0].toDouble(), parts[1].toDouble()) else null
                    }

                    RideTrackingScreen(
                        navController = navController,
                        startLocation = stringToLatLng(startArg),
                        destinationLocation = stringToLatLng(destArg)
                    )
                }

                composable("saved_addresses"){
                    SavedAddressesScreen(navController, savedAddressViewModel)
                }
                composable("your_rides"){
                    YourRidesScreen(navController = navController, rideHistoryViewModel = rideHistoryViewModel, bookRideViewModel = bookRideViewModel)
                }
                composable("pick_location_for_saved_address_route") {
                    PickLocationScreen(navController, resultKey = "pickedLocationForSavedAddress")
                }
            }
        }
    }
}
