import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircleOutline
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.google.firebase.auth.FirebaseAuth

@Composable
fun HomeScreen(navController: NavHostController) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val lightGrayBackground = Color(0xFFF8F9FA)
    val darkBlueLogout = Color(0xFF0D47A1)
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(lightGrayBackground)
    ) {

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(310.dp)
                .clip(RoundedCornerShape(bottomStartPercent = 20, bottomEndPercent = 20))
                .background(
                    Brush.verticalGradient(
                        colors = listOf(primaryColor, primaryColor.copy(alpha = 0.8f))
                    )
                )
        )
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 32.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Welcome to \nVolunteer Hub",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = 32.dp),
            )

            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier
                    .height(276.dp)
                    .fillMaxWidth()
            ) {
                item {
                    DashboardCardElevated(
                        icon = Icons.Filled.Event,
                        title = "My Events",
                        color = primaryColor,
                        onClick = { navController.navigate("my_events") }
                    )
                }
                item {
                    DashboardCardElevated(
                        icon = Icons.Filled.Search,
                        title = "View Events",
                        color = primaryColor,
                        onClick = { navController.navigate("view_events") }
                    )
                }
                item {
                    DashboardCardElevated(
                        icon = Icons.Filled.AddCircleOutline,
                        title = "Create Event",
                        color = primaryColor,
                        onClick = { navController.navigate("create_event") }
                    )
                }
                item {
                    DashboardCardElevated(
                        icon = Icons.Filled.Person,
                        title = "My Profile",
                        color = primaryColor,
                        onClick = { navController.navigate("my_profile") }
                    )
                }
            }

            Spacer(modifier = Modifier.height(48.dp))

            OutlinedButton(
                onClick = {
                    FirebaseAuth.getInstance().signOut()
                    navController.navigate("login") {
                        popUpTo("home") { inclusive = true }
                    }
                },
                modifier = Modifier
                    .width(250.dp)
                    .height(50.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = darkBlueLogout),
                border = BorderStroke(1.5.dp, darkBlueLogout)
            ) {
                Text("Logout", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
fun DashboardCardElevated(
    icon: ImageVector,
    title: String,
    color: Color,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .height(130.dp)
            .widthIn(min = 120.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = color,
                modifier = Modifier.size(44.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = color,
                textAlign = TextAlign.Center
            )
        }
    }
}
