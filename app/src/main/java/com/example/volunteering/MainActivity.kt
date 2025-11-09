package com.example.volunteering

import HomeScreen
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.example.volunteering.ui.theme.VolunteeringTheme
import com.google.firebase.FirebaseApp
import androidx.navigation.compose.*
import com.example.volunteering.ui.screen.CreateEventScreen
import com.example.volunteering.ui.screen.EventDetailsScreen
import com.example.volunteering.ui.screen.LoginScreen
import com.example.volunteering.ui.screen.MyEventsScreen
import com.example.volunteering.ui.screen.RegisterScreen
import com.example.volunteering.ui.screen.ViewEventsScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        FirebaseApp.initializeApp(this)

        enableEdgeToEdge()
        setContent {
            VolunteeringTheme {
                VolunteeringApp()
            }
        }
    }

}

@Composable
fun VolunteeringApp() {
    val navController = rememberNavController()
    NavHost(navController, startDestination = "login") {
        composable("login") { LoginScreen(navController) }
        composable("register") { RegisterScreen(navController) }
        composable("home") { HomeScreen(navController) }
        composable("my_events") { MyEventsScreen(navController)}
        composable("view_events") { ViewEventsScreen(navController) }
        composable("create_event") { CreateEventScreen(navController) }
        composable("event_details/{eventId}") { backStackEntry ->
            val eventId = backStackEntry.arguments?.getString("eventId") ?: ""
            EventDetailsScreen(navController, eventId)
        }
    }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello $name!",
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    VolunteeringTheme {
        Greeting("Android")
    }
}