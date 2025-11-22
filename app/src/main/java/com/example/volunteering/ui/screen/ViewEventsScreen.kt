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
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.example.volunteering.data.model.Event
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await

private const val TAG = "ViewEventsScreen"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ViewEventsScreen(navController: NavHostController) {
    var events by remember { mutableStateOf<List<Event>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var filterType by remember { mutableStateOf("All") }
    var showFilterMenu by remember { mutableStateOf(false) }

    val filterOptions = listOf("All", "Community Service", "Education", "Environmental", "Health", "Animal Welfare")

    LaunchedEffect(filterType) {
        try {
            Log.d(TAG, "Loading all events with filter: $filterType")
            isLoading = true
            errorMessage = null

            val firestore = FirebaseFirestore.getInstance()
            var query: Query = firestore.collection("events")
                .orderBy("date", Query.Direction.ASCENDING)

            if (filterType != "All") {
                query = query.whereEqualTo("type", filterType)
            }

            val result = query.get().await()
            Log.d(TAG, "Query successful, documents count: ${result.size()}")

            events = result.documents.mapNotNull { doc ->
                try {
                    doc.toObject(Event::class.java)?.copy(
                        id = doc.id
                    )?.also {
                        Log.d(TAG, "Loaded event: ${it.title} by ${it.creatorUid}")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error parsing event document ${doc.id}", e)
                    null
                }
            }

            Log.d(TAG, "Successfully loaded ${events.size} events")
            isLoading = false

        } catch (e: Exception) {
            Log.e(TAG, "Error loading events", e)
            errorMessage = "Failed to load events: ${e.localizedMessage}"
            isLoading = false
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = {
                Text(
                    text = "All Events",
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
            actions = {
                Box {
                    TextButton(onClick = { showFilterMenu = true }) {
                        Text("Category: $filterType")
                    }
                    DropdownMenu(
                        expanded = showFilterMenu,
                        onDismissRequest = { showFilterMenu = false }
                    ) {
                        filterOptions.forEach { option ->
                            DropdownMenuItem(
                                text = { Text(option) },
                                onClick = {
                                    filterType = option
                                    showFilterMenu = false
                                }
                            )
                        }
                    }
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        )

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
                    }
                }
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
                            text = "$errorMessage",
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
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "*",
                            style = MaterialTheme.typography.displayLarge
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "No events found",
                            style = MaterialTheme.typography.titleLarge
                        )
                        if (filterType != "All") {
                            Text(
                                text = "Try changing the filter",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
            else -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
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
                            onClick = {
                                navController.navigate("event_details/${event.id}")
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EventCard(
    event: Event,
    onClick: () -> Unit
) {
    val auth = FirebaseAuth.getInstance()
    val isMyEvent = event.creatorUid == auth.currentUser?.uid
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
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
                    overflow = TextOverflow.Ellipsis
                )
                if (isMyEvent) {
                    Surface(
                        color = Color(0xFF445E91),
                        shape = MaterialTheme.shapes.small
                    ) {
                        Text(
                            text = "My Event",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
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
                    tint = Color(0xFF445E91)
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
                    tint = Color(0xFF445E91)
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