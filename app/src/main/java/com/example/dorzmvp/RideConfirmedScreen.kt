package com.example.dorzmvp

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.google.android.gms.maps.model.LatLng
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

// Predefined colors for consistent styling.
private val dorzRed = Color(0xFFD32F2F)
private val dorzWhite = Color.White
private val successGreen = Color(0xFF34A853)

/**
 * A confirmation screen displayed after a user successfully books a ride.
 *
 * This screen provides feedback that the ride is confirmed and offers two main actions:
 * 1. Navigate to a real-time tracking screen for the ride.
 * 2. Navigate back to the home screen.
 *
 * @param navController The navigation controller for handling screen transitions.
 * @param startLocation The starting coordinates of the booked ride.
 * @param destinationLocation The destination coordinates of the booked ride.
 */
@Composable
fun RideConfirmedScreen(
    navController: NavController,
    startLocation: LatLng?,
    destinationLocation: LatLng?
) {
    Scaffold { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
                .padding(32.dp), // Extra padding for centered content
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = "Ride Confirmed",
                modifier = Modifier.size(120.dp),
                tint = successGreen
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Confirmation text
            Text(
                text = "Ride Confirmed!",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Text(
                text = "Your driver is on the way.",
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                color = Color.Gray
            )

            Spacer(modifier = Modifier.height(48.dp))

            // Button to navigate to the ride tracking screen.
            Button(
                onClick = {
                    // Encodes LatLng objects into URL-safe strings for navigation arguments.
                    fun latLngToString(latLng: LatLng?): String {
                        val coords = latLng ?: LatLng(0.0, 0.0) // Fallback for safety
                        return URLEncoder.encode(
                            "${coords.latitude},${coords.longitude}",
                            StandardCharsets.UTF_8.name()
                        )
                    }

                    val startArg = latLngToString(startLocation)
                    val destArg = latLngToString(destinationLocation)

                    navController.navigate("book_ride_tracking/$startArg/$destArg")
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = dorzRed,
                    contentColor = dorzWhite
                )
            ) {
                Text("Track Your Ride", fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Button to go back to the home screen, clearing the booking flow from the back stack.
            Button(
                onClick = {
                    navController.navigate("home_screen") {
                        // Clear the back stack to prevent the user from navigating back
                        // to the payment or confirmation screens.
                        popUpTo(navController.graph.startDestinationId) {
                            inclusive = true
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = dorzRed),
                border = BorderStroke(1.dp, dorzRed)
            ) {
                Text("Back to Home", fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}
