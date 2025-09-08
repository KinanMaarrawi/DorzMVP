package com.example.dorzmvp

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController

@Composable
fun HomeScreenUI(navController: NavController){
    Column(
        //make elements on home screen appear one after another rather than on top of each other using a column container
        modifier = Modifier.fillMaxSize(),
        //fill max size on screen
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.CenterHorizontally
        //align items to center
    ) {
        TopBar()
        Spacer(modifier = Modifier.height(24.dp))
        TaxiMenu(NavContoller = navController)
    }
}
@Composable
fun TopBar(){
    //recreating Dorz top bar, not functional just for looks
    Row(
        //row for search bar and user icon, centered vertically, using max width
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Start,
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.Red)  // Row background
            .padding(16.dp)
    ) {
        Icon(
            imageVector = Icons.Default.Person,
            contentDescription = null,
            modifier = Modifier.size(32.dp)
        )

        Spacer(modifier = Modifier.width(12.dp)) // optional spacing

        Text(
            text = "Search here",
            textAlign = TextAlign.Left,
            fontSize = 32.sp,
            color = Color.Gray,
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White) // Text background
                .padding(12.dp)          // padding inside background
                .wrapContentSize()       // lets the background wrap the text
        )
    }
}

@Composable
fun TaxiMenu(NavContoller: NavController) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp) //padding OUTSIDE border
            .border(
                width = 1.dp,
                color = Color.Black,
                shape = RoundedCornerShape(8.dp)
            )
            .padding(8.dp) // Padding INSIDE the border
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
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            item {
                TaxiMenuElement(imageVector = Icons.Default.Done, text = "Instant Ride", onClick = { NavContoller.navigate("book_ride_one") })
                Spacer(Modifier.width(30.dp))
                TaxiMenuElement(imageVector = Icons.Default.Favorite, text = "Last Ride", onClick = { NavContoller.navigate("last_ride") })
                Spacer(Modifier.width(30.dp))
                TaxiMenuElement(imageVector = Icons.Default.Add, text = "Saved Addresses", onClick = { NavContoller.navigate("saved_addresses") })
            }
        }
    }
}

@Composable
fun TaxiMenuElement(
    imageVector: ImageVector,
    text: String,
    onClick: () -> Unit = {},
) {
    // Define a fixed size for the circular element
    val elementSize = 100.dp // Adjust as needed

    Column(
        modifier = Modifier
            .size(elementSize) // Apply a fixed size to the Column
            .border(
                width = 1.dp,
                color = Color.Black,
                shape = CircleShape
            )
            // Padding inside the circle, make it consistent
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = androidx.compose.foundation.layout.Arrangement.Center // Center content vertically
    ) {
        Icon(
            imageVector = imageVector,
            contentDescription = null,
            modifier = Modifier.size(24.dp) // Give Icon a specific size
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = text,
            fontSize = 12.sp,
            textAlign = TextAlign.Center, // Center the text

        )
    }
}