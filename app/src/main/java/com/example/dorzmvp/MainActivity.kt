package com.example.dorzmvp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.dorzmvp.ui.theme.DorzMVPTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val navController = rememberNavController()
            NavHost(navController = navController, startDestination = "home_screen", builder = {
                composable("home_screen") {
                    HomeScreenUI(navController)
                }
                composable("book_ride_one"){
                    BookARideMainUI(navController)
                }

                composable("book_ride_start"){
                    /* TODO */
                }

                composable("book_ride_destination"){
                    /* TODO */
                }

                composable("book_ride_two"){
                    /* TODO */
                }

                composable("book_ride_card"){
                    /* TODO */
                }

                composable("book_ride_confirmed"){
                    /* TODO */
                }

                composable("book_ride_tracking"){
                    /* TODO */
                }

                composable("saved_addresses"){
                    /* TODO */
                }
                composable("last_ride"){
                    /* TODO */
                }
            } )
        }
    }
}
