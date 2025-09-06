package com.example.dorzmvp

import android.graphics.fonts.FontStyle
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.dorzmvp.ui.theme.DorzMVPTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            DorzMVPTheme {
                HomeScreenUI()
            }
        }
    }
}
@Composable
fun HomeScreenUI(){
    TopBar()
    TaxiMenu()
}
@Composable
fun TopBar(){
    //recreating Dorz top bar, not functional just for looks
    Row(
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
fun TaxiMenu(){
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .border(
                width = 1.dp,
                color = Color.Black,
                shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp)
            )
    ) {
        item {
            Text(text = "ed")
            Text(text = "edfd")
            Text(text = "fdjslkf")

        }
    }
}