package com.example.volunteering.ui.screen

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.EventAvailable
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.volunteering.data.model.Event
import com.example.volunteering.data.model.User
import com.example.volunteering.data.repository.UserRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyProfileScreen(navController: NavController) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val lightGrayBackground = Color(0xFFF8F9FA)
    val darkBlueLogout = Color(0xFF0D47A1)

    val scope = rememberCoroutineScope()
    val repository = remember { UserRepository() }

    var user by remember { mutableStateOf<User?>(null) }
    var eventsAttended by remember { mutableStateOf(0) }
    var eventsOrganized by remember { mutableStateOf(0) }
    var upcomingEvents by remember { mutableStateOf<List<Event>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    val currentUser = FirebaseAuth.getInstance().currentUser

    LaunchedEffect(Unit) {
        scope.launch {
            try {
                user = repository.getCurrentUser()

                val userId = currentUser?.uid
                if (userId != null) {
                    val firestore = FirebaseFirestore.getInstance()

                    val attendedSnapshot = firestore.collection("events")
                        .get()
                        .await()

                    val today = LocalDate.now()
                    val dateFormatter = DateTimeFormatter.ofPattern("dd-MM-yyyy")

                    eventsAttended = attendedSnapshot.documents.count { doc ->
                        val goingUsers = doc.get("goingUsers") as? List<String> ?: emptyList()
                        val eventDateString = doc.getString("date") ?: ""

                        val eventDate = try {
                            LocalDate.parse(eventDateString, dateFormatter)
                        } catch (e: Exception) {
                            null
                        }

                        userId in goingUsers && eventDate != null && eventDate.isBefore(today)
                    }

                    eventsOrganized = firestore.collection("events")
                        .whereEqualTo("creatorUid", userId)
                        .get()
                        .await()
                        .size()

                    upcomingEvents = attendedSnapshot.documents.mapNotNull { doc ->
                        try {
                            val event = doc.toObject(Event::class.java)?.copy(id = doc.id)
                            event?.let {
                                val goingUsers = it.goingUsers
                                val eventDate = try {
                                    LocalDate.parse(it.date, dateFormatter)
                                } catch (e: Exception) {
                                    null
                                }

                                if (userId in goingUsers &&
                                    eventDate != null &&
                                    (eventDate.isEqual(today) || eventDate.isAfter(today))) {
                                    it
                                } else null
                            }
                        } catch (e: Exception) {
                            null
                        }
                    }.sortedBy { event ->
                        try {
                            LocalDate.parse(event.date, dateFormatter)
                        } catch (e: Exception) {
                            LocalDate.MAX
                        }
                    }.take(5)
                }

                isLoading = false
            } catch (e: Exception) {
                isLoading = false
            }
        }
    }

    Scaffold(
        containerColor = lightGrayBackground,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "My Profile",
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            imageVector = Icons.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    titleContentColor = primaryColor,
                    navigationIconContentColor = primaryColor
                )
            )
        }
    ) { paddingValues ->
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(18.dp))

                ProfileSectionCard {
                    InfoRow(
                        icon = Icons.Filled.Person,
                        text = user?.name ?: currentUser?.displayName ?: "User Name",
                        isPrimary = true
                    )
                    Divider(modifier = Modifier.padding(vertical = 8.dp, horizontal = 16.dp))
                    InfoRow(
                        icon = Icons.Filled.Email,
                        text = user?.email ?: currentUser?.email ?: "email@example.com",
                        isPrimary = false
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                ProfileSectionCard {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        StatItem(
                            count = eventsAttended,
                            label = "Events\nAttended",
                            color = primaryColor,
                            modifier = Modifier.weight(1f)
                        )
                        VerticalDivider(
                            modifier = Modifier
                                .height(50.dp)
                                .padding(vertical = 8.dp),
                            thickness = 1.dp,
                            color = Color.LightGray
                        )
                        StatItem(
                            count = eventsOrganized,
                            label = "Events\nOrganized",
                            color = primaryColor,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                ProfileSectionCard {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "Upcoming Event Reminders",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.Black,
                            modifier = Modifier.padding(start = 16.dp, top = 16.dp, end = 16.dp)
                        )
                        Divider(modifier = Modifier.padding(vertical = 12.dp, horizontal = 16.dp))

                        if (upcomingEvents.isEmpty()) {
                            Text(
                                text = "No upcoming events scheduled.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.Gray,
                                modifier = Modifier
                                    .padding(16.dp)
                                    .align(Alignment.CenterHorizontally)
                            )
                        } else {
                            upcomingEvents.forEachIndexed { index, event ->
                                EventReminderRow(
                                    eventName = event.title,
                                    eventDate = "${event.date} at ${event.time}",
                                    color = primaryColor,
                                    event = event
                                )
                                if (index < upcomingEvents.lastIndex) {
                                    Divider(
                                        modifier = Modifier.padding(horizontal = 16.dp),
                                        color = Color.LightGray.copy(alpha = 0.5f)
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                OutlinedButton(
                    onClick = {
                        FirebaseAuth.getInstance().signOut()
                        navController.navigate("login") {
                            popUpTo(navController.graph.startDestinationId) { inclusive = true }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth(0.8f)
                        .height(50.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = darkBlueLogout
                    ),
                    border = BorderStroke(1.5.dp, darkBlueLogout)
                ) {
                    Text("Logout", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
fun ProfileSectionCard(content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        content = content
    )
}

@Composable
fun InfoRow(icon: ImageVector, text: String, isPrimary: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = text,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = text,
            style = if (isPrimary) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyMedium,
            fontWeight = if (isPrimary) FontWeight.Bold else FontWeight.Normal,
            color = if (isPrimary) Color.Black else Color.Gray,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun StatItem(count: Int, label: String, color: Color, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = count.toString(),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = color
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = Color.Gray,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun EventReminderRow(eventName: String, eventDate: String, color: Color, event: Event? = null) {
    val daysUntil = event?.let { getDaysUntilEvent(it.date) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Filled.EventAvailable,
            contentDescription = "Event Reminder",
            tint = color,
            modifier = Modifier.size(32.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = eventName,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = Color.Black,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = eventDate,
                style = MaterialTheme.typography.bodyMedium,
                color = Color.Gray
            )
        }

        if (daysUntil != null) {
            Surface(
                color = when {
                    daysUntil == 0 -> Color(0xFFFF5722)
                    daysUntil <= 3 -> Color(0xFFFF9800)
                    else -> Color(0xFF445E91)
                },
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = when (daysUntil) {
                        0 -> "Today"
                        1 -> "Tomorrow"
                        else -> "$daysUntil days"
                    },
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                )
            }
        }
    }
}

private fun getDaysUntilEvent(eventDate: String): Int? {
    return try {
        val formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy")
        val today = LocalDate.now()
        val date = LocalDate.parse(eventDate, formatter)
        ChronoUnit.DAYS.between(today, date).toInt()
    } catch (e: Exception) {
        null
    }
}