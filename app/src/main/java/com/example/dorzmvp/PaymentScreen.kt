package com.example.dorzmvp

import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.dorzmvp.db.RideHistory
import com.example.dorzmvp.db.RideStatus
import com.example.dorzmvp.network.TaxiOptionResponse
import com.example.dorzmvp.ui.viewmodel.RideHistoryViewModel

private val dorzRed = Color(0xFFD32F2F)
private val dorzWhite = Color.White

private enum class PaymentMethod {
    NONE, GOOGLE_PAY, CARD, CASH
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaymentScreen(
    navController: NavController,
    rideOption: TaxiOptionResponse,
    startAddress: String?,
    destinationAddress: String?,
    rideHistoryViewModel: RideHistoryViewModel // Use the correct, dedicated ViewModel
) {
    var selectedPaymentMethod by rememberSaveable { mutableStateOf(PaymentMethod.NONE) }
    var isCardFormVisible by rememberSaveable { mutableStateOf(false) }

    var cardNumber by rememberSaveable { mutableStateOf("") }
    var expiryDate by rememberSaveable { mutableStateOf("") }
    var cvv by rememberSaveable { mutableStateOf("") }

    val isCardNumberValid = cardNumber.replace(" ", "").length == 16
    val isExpiryValid = Regex("^(0[1-9]|1[0-2])/\\d{2}$").matches(expiryDate)
    val isCvvValid = cvv.length == 3
    val isCardInfoValid = isCardNumberValid && isExpiryValid && isCvvValid

    val isConfirmEnabled =
        selectedPaymentMethod == PaymentMethod.CASH ||
                (selectedPaymentMethod == PaymentMethod.CARD && isCardInfoValid)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Confirm and Pay") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = dorzRed,
                    titleContentColor = dorzWhite
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
            // --- Ride Info Card ---
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                elevation = CardDefaults.cardElevation(4.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Your Journey for Today!",
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        color = dorzRed
                    )
                    Spacer(Modifier.height(12.dp))
                    Text("From: ${startAddress ?: "Unknown Start"}")
                    Text("To: ${destinationAddress ?: "Unknown Destination"}")
                    Spacer(Modifier.height(12.dp))
                    Text("Ride: ${rideOption.classText ?: "Unknown"}")
                    Text("Estimated cost: ${rideOption.priceText?.replace("dirham", "AED") ?: "Not available"}")
                }
            }

            Spacer(Modifier.height(24.dp))

            // --- Payment Buttons ---
            Column(
                Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                PaymentButton(
                    text = "Google Pay",
                    isSelected = selectedPaymentMethod == PaymentMethod.GOOGLE_PAY,
                    onClick = {
                        selectedPaymentMethod = PaymentMethod.GOOGLE_PAY
                        isCardFormVisible = false
                    },
                    isPrimary = true
                )
                PaymentButton(
                    text = "Pay with Card",
                    isSelected = selectedPaymentMethod == PaymentMethod.CARD,
                    onClick = {
                        selectedPaymentMethod = PaymentMethod.CARD
                        isCardFormVisible = true
                    }
                )
                PaymentButton(
                    text = "Cash",
                    isSelected = selectedPaymentMethod == PaymentMethod.CASH,
                    onClick = {
                        selectedPaymentMethod = PaymentMethod.CASH
                        isCardFormVisible = false
                    }
                )
            }

            // --- Card Form ---
            AnimatedVisibility(
                visible = isCardFormVisible,
                enter = fadeIn(tween(300)),
                exit = fadeOut(tween(300))
            ) {
                CardDetailsForm(
                    cardNumber = cardNumber,
                    onCardNumberChange = {
                        val digits = it.filter { c -> c.isDigit() }.take(16)
                        cardNumber = digits.chunked(4).joinToString(" ")
                    },
                    cardNumberError = !isCardNumberValid && cardNumber.isNotEmpty(),
                    expiryDate = expiryDate,
                    onExpiryDateChange = {
                        val clean = it.filter { c -> c.isDigit() }.take(4)
                        expiryDate = when {
                            clean.length >= 3 -> clean.substring(0, 2) + "/" + clean.substring(2)
                            else -> clean
                        }
                    },
                    expiryError = !isExpiryValid && expiryDate.isNotEmpty(),
                    cvv = cvv,
                    onCvvChange = {
                        cvv = it.filter { c -> c.isDigit() }.take(3)
                    },
                    cvvError = !isCvvValid && cvv.isNotEmpty()
                )
            }

            Spacer(Modifier.weight(1f))

            // --- Confirm Button ---
            Button(
                onClick = {
                    if (isConfirmEnabled) {
                        val rideToSave = RideHistory(
                            startAddress = startAddress ?: "Unknown",
                            destinationAddress = destinationAddress ?: "Unknown",
                            priceText = rideOption.priceText ?: "N/A",
                            rideClass = rideOption.classText ?: "Unknown",
                            status = RideStatus.ONGOING // <-- Set the status here
                        )
                        rideHistoryViewModel.saveRideToHistory(rideToSave)
                        navController.navigate("book_ride_confirmed") {
                            popUpTo("home_screen")
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth().height(50.dp),
                shape = RoundedCornerShape(12.dp),
                enabled = isConfirmEnabled,
                colors = ButtonDefaults.buttonColors(
                    containerColor = dorzRed,
                    contentColor = dorzWhite,
                    disabledContainerColor = Color.LightGray,
                    disabledContentColor = Color.DarkGray
                )
            ) {
                Text("Confirm Ride", fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun PaymentButton(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    isPrimary: Boolean = false
) {
    val bg = if (isSelected) dorzRed else if (isPrimary) Color.Black else dorzWhite
    val fg = if (isSelected || isPrimary) dorzWhite else dorzRed
    Button(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().height(50.dp),
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(containerColor = bg, contentColor = fg)
    ) {
        Text(text, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun CardDetailsForm(
    cardNumber: String,
    onCardNumberChange: (String) -> Unit,
    cardNumberError: Boolean,
    expiryDate: String,
    onExpiryDateChange: (String) -> Unit,
    expiryError: Boolean,
    cvv: String,
    onCvvChange: (String) -> Unit,
    cvvError: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(top = 20.dp),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Enter Card Details", fontWeight = FontWeight.Bold, fontSize = 18.sp)

            // Card Number
            OutlinedTextField(
                value = cardNumber,
                onValueChange = onCardNumberChange,
                label = { Text("Card Number") },
                isError = cardNumberError,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            if (cardNumberError) Text("Invalid card number", color = dorzRed, fontSize = 12.sp)

            // Expiry + CVV
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Column(Modifier.weight(1f)) {
                    OutlinedTextField(
                        value = expiryDate,
                        onValueChange = onExpiryDateChange,
                        label = { Text("MM/YY") },
                        isError = expiryError,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    if (expiryError) Text("Invalid date", color = dorzRed, fontSize = 12.sp)
                }
                Column(Modifier.weight(1f)) {
                    OutlinedTextField(
                        value = cvv,
                        onValueChange = onCvvChange,
                        label = { Text("CVV") },
                        isError = cvvError,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    if (cvvError) Text("Invalid CVV", color = dorzRed, fontSize = 12.sp)
                }
            }
        }
    }
}
