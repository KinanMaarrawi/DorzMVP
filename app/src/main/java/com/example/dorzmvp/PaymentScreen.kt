package com.example.dorzmvp

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
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

// App-specific colors for consistent styling.
private val dorzRed = Color(0xFFD32F2F)
private val dorzWhite = Color.White

/**
 * Represents the available payment methods for a ride.
 */
private enum class PaymentMethod {
    NONE, GOOGLE_PAY, CARD, CASH
}

/**
 * A screen for confirming ride details and selecting a payment method.
 *
 * This screen displays a summary of the ride, provides payment options (Card, Cash),
 * handles card input validation, and upon confirmation, saves the ride to history
 * and navigates to the confirmation screen.
 *
 * @param navController Controller for navigating to the next screen.
 * @param rideOption The specific taxi ride option chosen by the user.
 * @param startAddress The starting address of the ride.
 * @param destinationAddress The destination address of the ride.
 * @param rideHistoryViewModel ViewModel for saving the ride to the database.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaymentScreen(
    navController: NavController,
    rideOption: TaxiOptionResponse,
    startAddress: String?,
    destinationAddress: String?,
    rideHistoryViewModel: RideHistoryViewModel
) {
    // --- State Management ---
    var selectedPaymentMethod by rememberSaveable { mutableStateOf(PaymentMethod.NONE) }
    var isCardFormVisible by rememberSaveable { mutableStateOf(false) }

    // State for card input fields.
    var cardNumber by rememberSaveable { mutableStateOf("") }
    var expiryDate by rememberSaveable { mutableStateOf("") }
    var cvv by rememberSaveable { mutableStateOf("") }

    // Real-time validation for card details.
    val isCardNumberValid = cardNumber.replace(" ", "").length == 16
    val isExpiryValid = Regex("^(0[1-9]|1[0-2])/\\d{2}$").matches(expiryDate)
    val isCvvValid = cvv.length == 3
    val isCardInfoValid = isCardNumberValid && isExpiryValid && isCvvValid

    // Determines if the confirm button should be enabled.
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
            RideInfoCard(startAddress, destinationAddress, rideOption)
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

            // --- Card Form (Animated) ---
            AnimatedVisibility(
                visible = isCardFormVisible,
                enter = fadeIn(tween(300)),
                exit = fadeOut(tween(300))
            ) {
                CardDetailsForm(
                    cardNumber = cardNumber,
                    onCardNumberChange = {
                        // Format card number with spaces every 4 digits.
                        val digits = it.filter(Char::isDigit).take(16)
                        cardNumber = digits.chunked(4).joinToString(" ")
                    },
                    cardNumberError = !isCardNumberValid && cardNumber.isNotEmpty(),
                    expiryDate = expiryDate,
                    onExpiryDateChange = {
                        // Format expiry date with a slash (MM/YY).
                        val clean = it.filter(Char::isDigit).take(4)
                        expiryDate = when {
                            clean.length >= 3 -> clean.substring(0, 2) + "/" + clean.substring(2)
                            else -> clean
                        }
                    },
                    expiryError = !isExpiryValid && expiryDate.isNotEmpty(),
                    cvv = cvv,
                    onCvvChange = { cvv = it.filter(Char::isDigit).take(3) },
                    cvvError = !isCvvValid && cvv.isNotEmpty()
                )
            }

            Spacer(Modifier.weight(1f)) // Pushes the confirm button to the bottom.

            // --- Confirm Ride Button ---
            Button(
                onClick = {
                    val rideToSave = RideHistory(
                        startAddress = startAddress ?: "Unknown",
                        destinationAddress = destinationAddress ?: "Unknown",
                        priceText = rideOption.priceText ?: "N/A",
                        rideClass = rideOption.classText ?: "Unknown",
                        status = RideStatus.ONGOING
                    )
                    rideHistoryViewModel.saveRideToHistory(rideToSave)

                    navController.navigate("book_ride_confirmed") {
                        // Clear the back stack up to home to prevent going back to booking flow.
                        popUpTo("home_screen")
                    }
                },
                modifier = Modifier.fillMaxWidth().height(50.dp),
                enabled = isConfirmEnabled,
                colors = ButtonDefaults.buttonColors(
                    containerColor = dorzRed,
                    contentColor = dorzWhite,
                    disabledContainerColor = Color.LightGray
                )
            ) {
                Text("Confirm Ride", fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

/**
 * A summary card displaying the key details of the ride.
 */
@Composable
private fun RideInfoCard(
    startAddress: String?,
    destinationAddress: String?,
    rideOption: TaxiOptionResponse
) {
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
}

/**
 * A styled button for selecting a payment method.
 *
 * @param isPrimary Marks the button with a distinct style (e.g., for Google Pay).
 */
@Composable
private fun PaymentButton(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    isPrimary: Boolean = false
) {
    val bgColor = when {
        isSelected -> dorzRed
        isPrimary -> Color.Black
        else -> dorzWhite
    }
    val contentColor = if (isSelected || isPrimary) dorzWhite else dorzRed

    Button(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().height(50.dp),
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(containerColor = bgColor, contentColor = contentColor)
    ) {
        Text(text, fontWeight = FontWeight.SemiBold)
    }
}

/**
 * A form for entering credit/debit card details with validation feedback.
 */
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
