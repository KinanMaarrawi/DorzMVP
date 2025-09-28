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

private val dorzRed = Color(0xFFD32F2F)
private val dorzWhite = Color.White
private val successGreen = Color(0xFF34A853)

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
                .padding(32.dp),
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

            // Button to go to Ride Tracking
            Button(
                onClick = {
                    // Encode the LatLng strings to make them URL-safe
                    fun latLngToString(latLng: LatLng?): String {
                        if (latLng == null) return "0.0,0.0"
                        return URLEncoder.encode("${latLng.latitude},${latLng.longitude}", StandardCharsets.UTF_8.name())
                    }

                    val start = latLngToString(startLocation)
                    val dest = latLngToString(destinationLocation)

                    // Navigate with the new route and arguments
                    navController.navigate("book_ride_tracking/$start/$dest")
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

            // Button to go back to Home Menu
            Button(
                onClick = {
                    navController.navigate("home_screen") {
                        // Clear the entire back stack so the user can't go back to the payment/confirmation screens
                        popUpTo(navController.graph.startDestinationId) {
                            inclusive = true
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(12.dp),
                // FIXED: Use ButtonDefaults.outlinedButtonColors and create a BorderStroke manually
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = dorzRed
                ),
                border = BorderStroke(1.dp, dorzRed)
            ) {
                Text("Back to Home", fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}
