package com.example.dorzmvp

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.dorzmvp.network.TaxiOptionResponse
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaymentScreen(
    navController: NavController,
    rideOption: TaxiOptionResponse
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Confirm and Pay") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFFD32F2F),
                    titleContentColor = Color.White
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Ride Details Section
            Text(
                text = "Your Selected Ride",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(16.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Class: ${rideOption.className}",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Price: ${rideOption.priceText}", fontSize = 18.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    val waitTimeMinutes = rideOption.waitingTime?.div(60)?.roundToInt()
                    Text("Estimated Wait Time: $waitTimeMinutes min", fontSize = 16.sp)
                }
            }

            Spacer(Modifier.weight(1f))

            // Payment Options Section
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = { /* Dummy action */ },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Google Pay")
                }
                Button(
                    onClick = { /* Dummy action */ },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Pay with Card")
                }
                OutlinedButton(
                    onClick = { /* Dummy action */ },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Cash")
                }
            }

            Spacer(Modifier.height(16.dp))

            // Confirmation Button
            Button(
                onClick = { /* Dummy action */ },
                modifier = Modifier
                    .fillMaxWidth()
            ) {
                Text("Confirm Ride")
            }
        }
    }
}
