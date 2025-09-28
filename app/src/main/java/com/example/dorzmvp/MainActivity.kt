package com.example.dorzmvp

import android.app.Application
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
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
import java.nio.charset.StandardCharsets

/**
 * The main and only activity for the application.
 *
 * This activity hosts the Jetpack Compose navigation graph and initializes key services
 * and ViewModels that are shared across different screens.
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize the Google Places SDK with the API key from the manifest.
        initializePlacesSdk()

        enableEdgeToEdge()
        setContent {
            // Setup ViewModels that will be shared across composables.
            val context = LocalContext.current
            val bookRideViewModel: BookRideViewModel = viewModel()
            val savedAddressViewModel: SavedAddressViewModel = ViewModelProvider(
                this,
                SavedAddressViewModelFactory(context.applicationContext as Application)
            )[SavedAddressViewModel::class.java]
            val rideHistoryViewModel: RideHistoryViewModel = ViewModelProvider(
                this,
                RideHistoryViewModelFactory(context.applicationContext as Application)
            )[RideHistoryViewModel::class.java]

            // Define the navigation structure for the entire application.
            AppNavigation(
                bookRideViewModel = bookRideViewModel,
                savedAddressViewModel = savedAddressViewModel,
                rideHistoryViewModel = rideHistoryViewModel
            )
        }
    }

    /**
     * Safely initializes the Google Places SDK.
     * It reads the API key from the app's metadata and logs errors if the key
     * is missing or if initialization fails.
     */
    private fun initializePlacesSdk() {
        try {
            val appInfo = packageManager.getApplicationInfo(packageName, PackageManager.GET_META_DATA)
            val apiKey = appInfo.metaData.getString("com.google.android.geo.API_KEY")

            if (apiKey != null && !Places.isInitialized()) {
                Places.initialize(applicationContext, apiKey)
            } else if (apiKey == null) {
                Log.e("MainActivity", "API key for Places SDK not found in manifest.")
            }
        } catch (e: Exception) {
            // Catching a broad exception for multiple failure points (e.g., NameNotFound, NullPointer).
            Log.e("MainActivity", "Failed to load meta-data for Places SDK: ${e.message}", e)
        }
    }
}

/**
 * Sets up the application's navigation graph using Jetpack Compose Navigation.
 *
 * @param navController The controller to manage navigation events.
 * @param bookRideViewModel ViewModel for ride booking flow.
 * @param savedAddressViewModel ViewModel for managing saved addresses.
 * @param rideHistoryViewModel ViewModel for managing ride history.
 */
@Composable
private fun AppNavigation(
    navController: NavHostController = rememberNavController(),
    bookRideViewModel: BookRideViewModel,
    savedAddressViewModel: SavedAddressViewModel,
    rideHistoryViewModel: RideHistoryViewModel
) {
    NavHost(navController = navController, startDestination = "home_screen") {

        // --- Core Screens ---
        composable("home_screen") { HomeScreenUI(navController) }
        composable("saved_addresses") { SavedAddressesScreen(navController, savedAddressViewModel) }
        composable("your_rides") {
            YourRidesScreen(
                navController = navController,
                rideHistoryViewModel = rideHistoryViewModel,
                bookRideViewModel = bookRideViewModel
            )
        }

        // --- Ride Booking Flow ---
        composable("book_ride_one") { BookARideMainUI(navController, bookRideViewModel) }
        composable("book_ride_start") { BookRideStartScreen(navController, savedAddressViewModel) }
        composable("book_ride_destination") { BookRideDestinationScreen(navController, savedAddressViewModel) }

        composable("payment_screen") {
            val rideOption by bookRideViewModel.selectedRideOption.observeAsState()
            val start by bookRideViewModel.startAddress.observeAsState()
            val dest by bookRideViewModel.destinationAddress.observeAsState()

            // Ensure ride option is available before navigating to payment.
            rideOption?.let {
                PaymentScreen(
                    navController = navController,
                    rideOption = it,
                    startAddress = start,
                    destinationAddress = dest,
                    rideHistoryViewModel = rideHistoryViewModel
                )
            } ?: run {
                Log.e("AppNavigation", "Attempted to navigate to payment_screen with a null ride option.")
                navController.popBackStack() // Go back if data is missing.
            }
        }

        composable("book_ride_confirmed") {
            val start by bookRideViewModel.startLocation.observeAsState()
            val dest by bookRideViewModel.destinationLocation.observeAsState()
            RideConfirmedScreen(
                navController = navController,
                startLocation = start,
                destinationLocation = dest
            )
        }

        // Route for ride tracking, accepts start/destination LatLng as string arguments.
        composable(
            route = "book_ride_tracking/{startLocation}/{destinationLocation}",
            arguments = listOf(
                navArgument("startLocation") { type = NavType.StringType },
                navArgument("destinationLocation") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val startArg = backStackEntry.arguments?.getString("startLocation")
            val destArg = backStackEntry.arguments?.getString("destinationLocation")

            // Helper to parse "lat,lng" string arguments back into LatLng objects.
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

        // --- Utility Screens ---
        composable("pick_location_for_saved_address_route") {
            PickLocationScreen(navController, resultKey = "pickedLocationForSavedAddress")
        }
    }
}
