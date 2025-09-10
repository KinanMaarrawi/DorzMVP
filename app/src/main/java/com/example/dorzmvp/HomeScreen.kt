package com.example.dorzmvp

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController

/**
 * Displays the main home screen UI.
 * This Composable acts as the main container for the home screen.
 *
 * @param navController The navigation controller for navigating to other screens.
 */
@Composable
fun HomeScreenUI(navController: NavController){
    Column(
        modifier = Modifier.fillMaxSize(), // Make the Column take up the entire available screen space.
        verticalArrangement = Arrangement.Top, // Arrange children from the top.
        horizontalAlignment = Alignment.CenterHorizontally // Center children horizontally.
    ) {
        TopBar()
        Spacer(modifier = Modifier.height(24.dp)) // Adds a vertical space of 24 dp between TopBar and TaxiMenu.
        TaxiMenu(navController = navController)
    }
}

/**
 * Displays the top bar of the home screen.
 * It includes a user profile icon and a search bar placeholder.
 */
@Composable
fun TopBar(){
    Row(
        verticalAlignment = Alignment.CenterVertically, // Align children vertically to the center of the Row.
        horizontalArrangement = Arrangement.Start, // Align children horizontally from the start of the Row.
        modifier = Modifier
            .fillMaxWidth() // Make the Row take up the entire available width.
            .background(Color.Red) // Set the background color of the Row to red.
            .padding(16.dp) // Add padding of 16 dp around the content of the Row.
    ) {
        Icon(
            imageVector = Icons.Default.Person, // Use the default Person icon.
            contentDescription = "User Profile", // Accessibility description for the icon.
            modifier = Modifier.size(32.dp) // Set the size of the icon to 32 dp.
        )
        Spacer(modifier = Modifier.width(12.dp)) // Adds a horizontal space of 12 dp between the Icon and Text.
        Text(
            text = "Search here",
            textAlign = TextAlign.Left, // Align the text to the left.
            fontSize = 32.sp, // Set the font size of the text to 32 sp.
            color = Color.Gray, // Set the text color to gray.
            modifier = Modifier
                .weight(1f) // Allow the Text to take up all available horizontal space in the Row.
                .background(Color.White) // Set the background color of the Text to white.
                .padding(12.dp) // Add padding of 12 dp around the text.
        )
    }
}

/**
 * Displays the taxi menu options for booking a ride.
 * This section provides quick actions related to taxi services.
 *
 * @param navController The navigation controller for handling ride booking actions.
 */
@Composable
fun TaxiMenu(navController: NavController) {
    val cornerRadius = 24.dp // Corner radius for the main box (consistent with book a ride menu)

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
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth() // Make the Column take up the entire available width.
                .padding(horizontal = 16.dp) // Add horizontal padding of 16 dp.
        ) {
            Text(
                text = "Book a Ride",
                color = Color.Red, // Set the text color to red.
                fontSize = 20.sp, // Set the font size to 20 sp.
                fontWeight = FontWeight.Bold, // Make the text bold.
                modifier = Modifier
                    .align(Alignment.Start) // Align the text to the start of the Column.
                    .padding(bottom = 8.dp) // Add padding of 8 dp to the bottom of the text.
            )
            LazyRow( // A horizontally scrolling list that only composes and lays out the currently visible items.
                horizontalArrangement = Arrangement.spacedBy(8.dp), // Add 8 dp of space between each item in the LazyRow.
                modifier = Modifier.fillMaxWidth() // Make the LazyRow take up the entire available width.
            ) {
                item { // Defines a single item in the LazyRow.
                    TaxiMenuElement(
                        imageVector = Icons.Default.Done, // Icon for "Instant Ride".
                        text = "Instant Ride",
                        onClick = {
                            navController.navigate("book_ride_one") // Navigate to the "book_ride_one" screen on click.
                        }
                    )
                }
                item {
                    TaxiMenuElement(
                        imageVector = Icons.Default.Favorite, // Icon for "Last Ride".
                        text = "Last Ride",
                        onClick = {
                            navController.navigate("last_ride") // Navigate to the "last_ride" screen on click.
                        }
                    )
                }
                item {
                    TaxiMenuElement(
                        imageVector = Icons.Default.Add, // Icon for "Saved Addresses".
                        text = "Saved Addresses",
                        onClick = {
                            navController.navigate("saved_addresses") // Navigate to the "saved_addresses" screen on click.
                        }
                    )
                }
            }
        }
    }
}

/**
 * Displays a single element within the taxi menu.
 * Each element consists of an icon and a text label, and is clickable.
 *
 * @param imageVector The icon to display for this menu element.
 * @param text The text label for this menu element.
 * @param onClick The lambda function to be executed when this element is clicked. Defaults to an empty action.
 */
@Composable
fun TaxiMenuElement(
    imageVector: ImageVector,
    text: String,
    onClick: () -> Unit = {} // Default empty lambda if no onClick is provided.
) {
    val elementSize = 100.dp // Define the size of the entire clickable element.
    val textMaxWidth = elementSize - 16.dp // Calculate max width for text to prevent overflow, considering padding (8.dp on each side).

    Column(
        modifier = Modifier
            .size(elementSize) // Set the size of the Column (the clickable area).
            .clip(CircleShape) // Clip the Column to a circular shape.
            .clickable(onClick = onClick) // Make the Column clickable, executing the onClick lambda.
            .border( // Add a circular border to the element.
                width = 1.dp, // Border width.
                color = Color.Black, // Border color.
                shape = CircleShape // Make the border circular.
            )
            .padding(8.dp), // Add padding inside the border.
        horizontalAlignment = Alignment.CenterHorizontally, // Center children horizontally within the Column.
        verticalArrangement = Arrangement.Center // Center children vertically within the Column.
    ) {
        Icon(
            imageVector = imageVector,
            contentDescription = text, // Use the text as content description for accessibility.
            modifier = Modifier.size(elementSize * 0.3f) // Icon size relative to the element size (30% of 100.dp = 30.dp).
        )
        Spacer(modifier = Modifier.height(elementSize * 0.05f)) // Spacer height relative to element size (5% of 100.dp = 5.dp).
        Text(
            text = text,
            fontSize = (elementSize.value * 0.12f).sp, // Font size relative to element size (12% of 100.dp value, converted to sp).
            textAlign = TextAlign.Center, // Center the text.
            modifier = Modifier.width(textMaxWidth), // Set the calculated max width for the text.
            maxLines = 2 // Allow text to wrap up to 2 lines.
        )
    }
}
