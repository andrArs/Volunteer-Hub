package com.example.volunteering.ui.screen

import android.util.Log
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.example.volunteering.data.model.Event
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

private const val TAG = "MyEventsScreen"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyEventsScreen(navController: NavHostController) {
    val tabs = listOf("Interested", "Going", "Created")
    var selectedTab by remember { mutableIntStateOf(0) }

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        TopAppBar(
            title = {
                Text(
                    text = "My Events",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
            },
            navigationIcon = {
                IconButton(onClick = {
                    navController.navigate("home") {
                        popUpTo("home") { inclusive = false }
                    }
                }) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back to Home"
                    )
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        )

        TabRow(selectedTabIndex = selectedTab) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    text = { Text(title) }
                )
            }
        }

        when (selectedTab) {
            0 -> EventList(navController = navController,filter = "interested")
            1 -> EventList(navController = navController,filter = "going")
            2 -> EventList(navController = navController,filter = "created")
        }
    }
}

@Composable
fun EventList(navController: NavHostController,filter: String) {
    val auth = FirebaseAuth.getInstance()
    val userId = auth.currentUser?.uid
    var events by remember { mutableStateOf<List<Event>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    if (userId == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("User not logged in")
        }
        return
    }

    LaunchedEffect(filter, userId) {
        try {
            Log.d(TAG, "Loading events for filter: $filter, userId: $userId")
            isLoading = true
            errorMessage = null

            val firestore = FirebaseFirestore.getInstance()
            val query = when (filter) {
                "interested" -> {
                    Log.d(TAG, "Querying interested events")
                    firestore.collection("events")
                        .whereArrayContains("interestedUsers", userId)
                }
                "going" -> {
                    Log.d(TAG, "Querying going events")
                    firestore.collection("events")
                        .whereArrayContains("goingUsers", userId)
                }
                "created" -> {
                    Log.d(TAG, "Querying created events")
                    firestore.collection("events")
                        .whereEqualTo("creatorUid", userId)
                }
                else -> {
                    Log.w(TAG, "Unknown filter: $filter")
                    firestore.collection("events")
                }
            }

            val result = query.get().await()
            Log.d(TAG, "Query successful, documents count: ${result.size()}")

            events = result.documents.mapNotNull { doc ->
                try {
                    doc.toObject(Event::class.java)?.copy(
                        id = doc.id
                    )?.also {
                        Log.d(TAG, "Loaded event: ${it.title}")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error parsing event document ${doc.id}", e)
                    null
                }
            }

            Log.d(TAG, "Successfully loaded ${events.size} events")
            isLoading = false

        } catch (e: Exception) {
            Log.e(TAG, "Error loading events for filter: $filter", e)
            errorMessage = "Failed to load events: ${e.localizedMessage}"
            isLoading = false
        }
    }

    when {
        isLoading -> {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    CircularProgressIndicator()
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Loading events...")
                }            }
        }
        errorMessage != null -> {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.padding(24.dp)

                ) {
                    Text(
                        text = errorMessage ?: "Unknown error",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = {
                        isLoading = true
                        errorMessage = null
                    }) {
                        Text("Retry")
                    }
                }
            }
        }
        events.isEmpty() -> {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("No events found for this category.")
            }
        }
        else -> {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(vertical = 16.dp)
            ) {
                item {
                    Text(
                        text = "Found ${events.size} event${if (events.size != 1) "s" else ""}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }

                items(
                    items = events,
                    key = { event -> event.id }
                ) { event ->
                    EventCard(
                        event = event,
                        filter = filter,
                        onClick = {
                            navController.navigate("event_details/${event.id}")
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun EventCard(event: Event,filter: String,
                      onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)

    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Text(
                    text = event.title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )

                Surface(
                    color = when (filter) {
                        "created" -> MaterialTheme.colorScheme.primaryContainer
                        "going" -> MaterialTheme.colorScheme.secondaryContainer
                        "interested" -> MaterialTheme.colorScheme.tertiaryContainer
                        else -> MaterialTheme.colorScheme.surfaceVariant
                    },
                    shape = MaterialTheme.shapes.small
                ) {
                    Text(
                        text = when (filter) {
                            "created" -> "Created"
                            "going" -> "Going"
                            "interested" -> "Interested"
                            else -> ""
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = when (filter) {
                            "created" -> MaterialTheme.colorScheme.onPrimaryContainer
                            "going" -> MaterialTheme.colorScheme.onSecondaryContainer
                            "interested" -> MaterialTheme.colorScheme.onTertiaryContainer
                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Default.DateRange,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "${event.date} • ${event.time}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Default.LocationOn,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = event.location,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}