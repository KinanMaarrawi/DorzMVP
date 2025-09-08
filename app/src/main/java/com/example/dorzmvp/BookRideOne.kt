package com.example.dorzmvp

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController

@Composable
fun BookARideMainUI(navController : NavController) {
    Column(
        //make elements on home screen appear one after another rather than on top of each other using a column container
        modifier = Modifier.fillMaxSize(),
        //fill max size on screen
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.CenterHorizontally
        //align items to center
    ) {
        TopBarBookRide()
    }
}

@Composable
fun TopBarBookRide() {
    Text(
        text = "Book a Ride",
        modifier = Modifier.height(50.dp)
    )
}