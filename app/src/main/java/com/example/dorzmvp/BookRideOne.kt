package com.example.dorzmvp

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.Icon
import androidx.compose.ui.draw.clip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.compose.ui.unit.sp

/**
 * Main Composable for the first screen of the "Book a Ride" flow.
 * It sets up the overall layout, including a top bar and the ride planning section.
 *
 * @param navController The NavController used for navigating to other screens in the booking process.
 */
@Composable
fun BookARideMainUI(navController : NavController) {
    Column(
        modifier = Modifier.fillMaxSize(), // Occupy the entire screen.
        verticalArrangement = Arrangement.Top, // Arrange elements from the top.
        horizontalAlignment = Alignment.CenterHorizontally // Center elements horizontally.
    ) {
        TopBarBookRide()
        Spacer(modifier = Modifier.height(24.dp)) // Vertical spacing between the top bar and the ride box.
        RideBox(navController = navController)
    }
}

/**
 * Displays the themed top bar for the "Book a Ride" screen.
 * It features a title and a distinctive background shape.
 */
@Composable
fun TopBarBookRide() {
    Box(
        modifier = Modifier
            .fillMaxWidth() // Span the entire width of the screen.
            .height(128.dp) // Set a fixed height for the top bar.
            .background(
                color = Color.Red, // Background color of the top bar.
                shape = RoundedCornerShape( // Apply rounded corners only to the bottom.
                    topStart = 0.dp,
                    topEnd = 0.dp,
                    bottomEnd = 24.dp,
                    bottomStart = 24.dp
                )
            ),
        contentAlignment = Alignment.Center // Center the content (Text) within the Box.
    ) {
        Text(
            text = "Book a Ride",
            color = Color.White, // Text color.
            fontSize = 30.sp, // Font size for the title.
            fontWeight = FontWeight.Bold // Make the title bold.
        )
    }
}

/**
 * Composable for the main content area where users plan their ride.
 * It includes input fields for pickup and destination, visually grouped in a shadowed box.
 *
 * @param navController The NavController for navigating when input fields are tapped.
 */
@Composable
fun RideBox(navController: NavController){
    val cornerRadius = 24.dp // Corner radius for the main ride box.
    val nestedBoxCornerRadius = 8.dp // Corner radius for the inner clickable boxes (pickup/destination).

    // Outer container for the ride planning section with shadow and rounded corners.
    Box(
        modifier = Modifier
            .fillMaxWidth() // Span the entire width.
            .padding(horizontal = 16.dp) // Horizontal padding to create space from screen edges.
            .shadow(
                elevation = 4.dp, // Shadow elevation for a floating effect.
                shape = RoundedCornerShape(cornerRadius), // Shadow shape matches the box's shape.
                clip = false // Allow shadow to be drawn outside the bounds of the composable if needed.
            )
            .background(
                color = Color.White, // Background color of the box.
                shape = RoundedCornerShape(cornerRadius) // Apply rounded corners to the background.
            )
            .padding(16.dp) // Inner padding for the content within this box.
    ){
        Column(modifier = Modifier.fillMaxWidth()) { // Column to arrange elements vertically.
            Text(
                text = "Plan Your Ride",
                color = Color.Black,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
            )

            Spacer(modifier = Modifier.height(16.dp)) // Vertical spacing.

            // Clickable Box for selecting the pickup point.
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    // 1. Clip to the desired shape FIRST to ensure the ripple effect is contained.
                    .clip(RoundedCornerShape(nestedBoxCornerRadius))
                    // 2. Then make it clickable.
                    .clickable {
                        navController.navigate("book_ride_start") // Navigate to pickup selection screen.
                    }
                    .border(
                        width = 1.dp, // Border width.
                        color = Color.Gray, // Border color.
                        shape = RoundedCornerShape(nestedBoxCornerRadius) // Border shape.
                    )
                    .padding(horizontal = 12.dp, vertical = 12.dp), // Inner padding for the text.
                contentAlignment = Alignment.CenterStart // Align text to the start (left) and center vertically.
            ) {
                Text(
                    text = "Tap to select pickup point",
                    fontSize = 16.sp,
                    color = Color.Gray, // Placeholder text color.
                )
            }

            Spacer(modifier = Modifier.height(4.dp)) // Small vertical spacing.

            // Decorative icon between the two input fields.
            Icon(
                imageVector = Icons.Default.KeyboardArrowDown,
                contentDescription = null, // Decorative, so no content description needed.
                modifier = Modifier.align(Alignment.CenterHorizontally) // Center the icon within the Column.
            )

            Spacer(modifier = Modifier.height(4.dp)) // Small vertical spacing.

            // Clickable Box for selecting the destination.
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    // 1. Clip to the desired shape FIRST.
                    .clip(RoundedCornerShape(nestedBoxCornerRadius))
                    // 2. Then make it clickable.
                    .clickable {
                        navController.navigate("book_ride_destination") // Navigate to destination selection screen.
                    }
                    .border(
                        width = 1.dp,
                        color = Color.Gray,
                        shape = RoundedCornerShape(nestedBoxCornerRadius)
                    )
                    .padding(horizontal = 12.dp, vertical = 12.dp), // Inner padding for the text.
                contentAlignment = Alignment.CenterStart // Align text to the start (left) and center vertically.
            ) {
                Text(
                    text = "Tap to select destination",
                    fontSize = 16.sp,
                    color = Color.Gray, // Placeholder text color.
                )
            }
        }
    }
}
