package com.example.volunteering.ui.screen

import android.Manifest
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.example.volunteering.data.model.Event
import com.example.volunteering.data.model.EventTypes
import com.example.volunteering.utils.LocationHelper
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.time.LocalDate
import java.time.format.DateTimeFormatter

private const val TAG = "ViewEventsScreen"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ViewEventsScreen(navController: NavHostController) {
    var events by remember { mutableStateOf<List<Event>>(emptyList()) }
    var allEvents by remember { mutableStateOf<List<Event>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var filterType by remember { mutableStateOf("All") }
    var showFilterMenu by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }

    val filterOptions = listOf("All") + EventTypes.ALL_TYPES

    val context = LocalContext.current
    val locationHelper = remember { LocationHelper(context) }
    val scope = rememberCoroutineScope()
    var userLocation by remember { mutableStateOf<android.location.Location?>(null) }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val isGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true

        if (isGranted) {
            Log.d(TAG, "Permission granted by user")
            scope.launch {
                userLocation = locationHelper.getCurrentLocation()
            }
        } else {
            Log.d(TAG, "Permission denied by user")
        }
    }

    LaunchedEffect(Unit) {
        if (!locationHelper.hasLocationPermission()) {
            Log.d(TAG, "Requesting permission...")
            locationPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        } else {
            Log.d(TAG, "Permission already granted, getting location...")
            userLocation = locationHelper.getCurrentLocation()
        }
    }

    LaunchedEffect(filterType) {
        try {
            Log.d(TAG, "Loading all events with filter: $filterType")
            isLoading = true
            errorMessage = null

            val firestore = FirebaseFirestore.getInstance()
            val query: Query = if (filterType != "All") {
                firestore.collection("events")
                    .whereEqualTo("type", filterType)
            } else {
                firestore.collection("events")
                    .orderBy("date", Query.Direction.ASCENDING)
            }

            val result = query.get().await()

            val mappedEvents = result.documents.mapNotNull { doc ->
                try {
                    doc.toObject(Event::class.java)?.copy(id = doc.id)
                } catch (e: Exception) {
                    null
                }
            }

            val sortedEvents = if (filterType != "All") {
                val dateFormatter = DateTimeFormatter.ofPattern("dd-MM-yyyy")
                mappedEvents.sortedWith { event1, event2 ->
                    try {
                        val date1 = LocalDate.parse(event1.date, dateFormatter)
                        val date2 = LocalDate.parse(event2.date, dateFormatter)
                        date1.compareTo(date2)
                    } catch (e: Exception) { 0 }
                }
            } else {
                mappedEvents
            }

            allEvents = sortedEvents
            events = sortedEvents
            isLoading = false
            Log.d(TAG, "Loaded ${events.size} events")

        } catch (e: Exception) {
            Log.e(TAG, "Error loading events", e)
            errorMessage = "Failed to load events: ${e.localizedMessage}"
            isLoading = false
        }
    }

    LaunchedEffect(userLocation, allEvents) {
        if (userLocation != null && allEvents.isNotEmpty()) {
            Log.d(TAG, "My Location: ${userLocation!!.latitude}, ${userLocation!!.longitude}")
            Log.d(TAG, "Calculating distances for user at: ${userLocation?.latitude}, ${userLocation?.longitude}")

            val updatedEvents = allEvents.map { event ->
                if (event.latitude != null && event.longitude != null) {
                    Log.d(TAG, "🏢 Event '${event.title}' coords: ${event.latitude}, ${event.longitude}")
                    val dist = locationHelper.calculateDistance(
                        userLocation!!.latitude,
                        userLocation!!.longitude,
                        event.latitude,
                        event.longitude
                    )
                    event.copy(distance = dist)
                } else {
                    event
                }
            }

            allEvents = updatedEvents

            events = if (searchQuery.isBlank()) {
                updatedEvents
            } else {
                updatedEvents.filter { event ->
                    event.title.contains(searchQuery, ignoreCase = true) ||
                            event.description.contains(searchQuery, ignoreCase = true)
                }
            }
        }
    }

    LaunchedEffect(searchQuery) {
        events = if (searchQuery.isBlank()) {
            allEvents
        } else {
            allEvents.filter { event ->
                event.title.contains(searchQuery, ignoreCase = true) ||
                        event.description.contains(searchQuery, ignoreCase = true)
            }
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

        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            placeholder = { Text("Search event...") },
            leadingIcon = {
                Icon(imageVector = Icons.Default.Search, contentDescription = "Search")
            },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { searchQuery = "" }) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Clear")
                    }
                }
            },
            singleLine = true,
            shape = MaterialTheme.shapes.medium
        )

        when {
            isLoading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
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
                        modifier = Modifier.padding(24.dp)
                    ) {
                        Text(
                            text = "$errorMessage",
                            color = MaterialTheme.colorScheme.error
                        )
                        Button(onClick = { isLoading = true; errorMessage = null }) {
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
                    Text("No events found")
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
    val locationHelper = LocationHelper(LocalContext.current)

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
                        color = Color(0xFF8093B7),
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

                Column(modifier = Modifier.weight(1f)) {
                    val locationText = event.location

                    if (locationText.contains("(") && locationText.endsWith(")")) {
                        val parts = locationText.split(" (", limit = 2)
                        val placeName = parts[0]
                        val address = parts[1].removeSuffix(")")

                        Text(
                            text = placeName,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = address,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    } else {
                        Text(
                            text = locationText,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }


                    event.distance?.let { distance ->
                        Text(
                            text = locationHelper.formatDistance(distance),
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF445E91),
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}