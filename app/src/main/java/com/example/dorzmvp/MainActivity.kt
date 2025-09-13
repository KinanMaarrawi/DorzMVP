package com.example.dorzmvp

import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.dorzmvp.ui.theme.DorzMVPTheme
import com.google.android.libraries.places.api.Places

/**
 * The main activity for the DorzMVP application.
 * This activity serves as the entry point and hosts the Jetpack Compose navigation graph.
 */
class MainActivity : ComponentActivity() {
    /**
     * Called when the activity is first created.
     * This method sets up the edge-to-edge display and initializes the Compose content
     * with a NavHost for managing navigation between different screens.
     *
     * @param savedInstanceState If the activity is being re-initialized after
     *     previously being shut down then this Bundle contains the data it most
     *     recently supplied in [onSaveInstanceState].  Otherwise it is null.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize Places SDK
        try {
            val applicationInfo = packageManager.getApplicationInfo(packageName, PackageManager.GET_META_DATA)
            val apiKey = applicationInfo.metaData.getString("com.google.android.geo.API_KEY")
            if (apiKey != null) {
                if (!Places.isInitialized()) {
                    Places.initialize(applicationContext, apiKey)
                }
            } else {
                Log.e("MainActivity", "API key for Places SDK not found in manifest")
            }
        } catch (e: PackageManager.NameNotFoundException) {
            Log.e("MainActivity", "Failed to load meta-data, NameNotFound: " + e.message)
        } catch (e: NullPointerException) {
            Log.e("MainActivity", "Failed to load meta-data, NullPointer: " + e.message)
        }

        enableEdgeToEdge() // Enables drawing behind system bars for a more immersive experience.
        setContent {
            // rememberNavController creates and remembers a NavController across recompositions.
            val navController = rememberNavController()
            // NavHost is a container that displays different composable destinations based on the current route.
            NavHost(navController = navController, startDestination = "home_screen", builder = {
                // Defines the "home_screen" destination.
                composable("home_screen") {
                    HomeScreenUI(navController) // Composable function for the home screen.
                }
                // Defines the "book_ride_one" destination.
                composable("book_ride_one"){
                    BookARideMainUI(navController) // Composable function for the first step of booking a ride.
                }

                // Defines the "book_ride_start" destination - for selecting pickup location.
                composable("book_ride_start"){
                    BookRideStartScreen(navController) // Composable for selecting the start point.
                }

                // Defines the "book_ride_destination" destination - for selecting the ride destination.
                composable("book_ride_destination"){
                    BookRideDestinationScreen(navController) // Composable for selecting the destination point.
                }

                // Defines the "book_ride_two" destination - potentially for further ride details or confirmation.
                composable("book_ride_two"){
                    /* TODO: Implement the UI for the second step or details of booking a ride. */
                }

                // Defines the "book_ride_card" destination - likely for payment or card details.
                composable("book_ride_card"){
                    /* TODO: Implement the UI for payment/card details related to the ride. */
                }

                // Defines the "book_ride_confirmed" destination - to show after a ride is successfully booked.
                composable("book_ride_confirmed"){
                    /* TODO: Implement the UI to confirm the ride booking. */
                }

                // Defines the "book_ride_tracking" destination - for tracking an ongoing ride.
                composable("book_ride_tracking"){
                    /* TODO: Implement the UI for tracking the ride in real-time. */
                }

                // Defines the "saved_addresses" destination.
                composable("saved_addresses"){
                    /* TODO: Implement the UI for displaying and managing saved addresses. */
                }
                // Defines the "last_ride" destination - to show details of the user's previous ride.
                composable("last_ride"){
                    /* TODO: Implement the UI to display details of the last ride. */
                }
            } )
        }
    }
}
