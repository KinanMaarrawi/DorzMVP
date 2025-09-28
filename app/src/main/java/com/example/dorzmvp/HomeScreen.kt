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
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController

/**
 * Displays the main home screen, which serves as the entry point of the app.
 *
 * @param navController The controller for navigating to other screens.
 */
@Composable
fun HomeScreenUI(navController: NavController) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        TopBar()
        Spacer(modifier = Modifier.height(24.dp))
        TaxiMenu(navController = navController)
    }
}

/**
 * Displays the top bar of the home screen, including a profile icon and a static search bar.
 */
@Composable
fun TopBar() {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Start,
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.Red)
            .padding(16.dp)
    ) {
        Icon(
            imageVector = Icons.Default.Person,
            contentDescription = "User Profile",
            modifier = Modifier.size(32.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = "Search here",
            textAlign = TextAlign.Left,
            fontSize = 32.sp,
            color = Color.Gray,
            modifier = Modifier
                .weight(1f)
                .background(Color.White)
                .padding(12.dp)
        )
    }
}

/**
 * Displays a menu of quick actions for taxi services.
 *
 * @param navController The controller for handling navigation from menu items.
 */
@Composable
fun TaxiMenu(navController: NavController) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .shadow(elevation = 4.dp, shape = RoundedCornerShape(24.dp))
            .background(color = Color.White, shape = RoundedCornerShape(24.dp))
            .padding(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        ) {
            Text(
                text = "Book a Ride",
                color = Color.Red,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .align(Alignment.Start)
                    .padding(bottom = 8.dp)
            )
            // A horizontally scrolling row for action items.
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                item {
                    TaxiMenuElement(
                        painter = painterResource(id = R.drawable.hail),
                        text = "Instant Ride",
                        onClick = { navController.navigate("book_ride_one") }
                    )
                }
                item {
                    TaxiMenuElement(
                        painter = painterResource(id = R.drawable.history),
                        text = "Your Rides",
                        onClick = { navController.navigate("your_rides") }
                    )
                }
                item {
                    TaxiMenuElement(
                        painter = painterResource(id = R.drawable.address),
                        text = "Saved Addresses",
                        onClick = { navController.navigate("saved_addresses") }
                    )
                }
            }
        }
    }
}

/**
 * A single clickable element for the taxi menu, with an icon and text.
 *
 * @param painter The icon to display for the element.
 * @param text The label for the element.
 * @param onClick The action to perform when clicked.
 */
@Composable
fun TaxiMenuElement(
    painter: Painter,
    text: String,
    onClick: () -> Unit = {}
) {
    val elementSize = 100.dp
    // Constrain text width to prevent overflow.
    val textMaxWidth = elementSize - 16.dp

    Column(
        modifier = Modifier
            .size(elementSize)
            .clip(CircleShape)
            .clickable(onClick = onClick)
            .border(width = 1.dp, color = Color.Black, shape = CircleShape)
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            painter = painter,
            contentDescription = text, // Use text for accessibility.
            modifier = Modifier.size(elementSize * 0.3f)
        )
        Spacer(modifier = Modifier.height(elementSize * 0.05f))
        Text(
            text = text,
            fontSize = (elementSize.value * 0.12f).sp, // Font size scales with element size.
            textAlign = TextAlign.Center,
            modifier = Modifier.width(textMaxWidth),
            maxLines = 2
        )
    }
}
