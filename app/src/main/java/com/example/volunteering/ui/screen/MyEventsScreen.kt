package com.example.volunteering.ui.screen

import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.example.volunteering.data.model.Event
import com.example.volunteering.utils.LocationHelper
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import android.Manifest
import com.example.volunteering.data.model.EventTypes
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search

private const val TAG = "MyEventsScreen"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyEventsScreen(navController: NavHostController) {
    val tabs = listOf("Interested", "Going", "Created", "History")
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
                containerColor = MaterialTheme.colorScheme.primary,
                titleContentColor = MaterialTheme.colorScheme.onPrimary,
                navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
            )
        )

        TabRow(selectedTabIndex = selectedTab,
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.primary) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    text = { Text(
                        title,
                        color = if (selectedTab == index)
                            MaterialTheme.colorScheme.primary
                        else
                            MaterialTheme.colorScheme.onSurfaceVariant
                    ) }
                )
            }
        }

        when (selectedTab) {
            0 -> EventList(navController = navController,filter = "interested")
            1 -> EventList(navController = navController,filter = "going")
            2 -> EventList(navController = navController,filter = "created")
            3 -> EventList(navController = navController,filter = "history")
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

    var searchQuery by remember { mutableStateOf("") }

    var filterType by remember { mutableStateOf("All") }
    var showFilterMenu by remember { mutableStateOf(false) }
    val filterOptions = listOf("All") + EventTypes.ALL_TYPES

    var distanceFilter by remember { mutableStateOf("Any") }
    var showDistanceMenu by remember { mutableStateOf(false) }
    val distanceOptions = listOf("Any", "< 1 km", "< 3 km", "< 5 km", "< 10 km", "< 20 km", "20+ km")

    var filteredEvents by remember { mutableStateOf<List<Event>>(emptyList()) }

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
            scope.launch {
                userLocation = locationHelper.getCurrentLocation()
            }
        }
    }

    LaunchedEffect(Unit) {
        if (!locationHelper.hasLocationPermission()) {
            locationPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        } else {
            userLocation = locationHelper.getCurrentLocation()
        }
    }

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
                "going", "history" -> {
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

            val loadedEvents = result.documents.mapNotNull { doc ->
                doc.toObject(Event::class.java)?.copy(id = doc.id)
            }

            val dateFormatter = java.time.format.DateTimeFormatter.ofPattern("dd-MM-yyyy")
            val today = java.time.LocalDate.now()

            val finalEvents = when (filter) {
                "interested", "going" -> {
                    loadedEvents.filter { event ->
                        try {
                            val eventDate = java.time.LocalDate.parse(event.date, dateFormatter)
                            !eventDate.isBefore(today)
                        } catch (e: Exception) { true }
                    }
                }
                "history" -> {
                    loadedEvents.filter { event ->
                        try {
                            val eventDate = java.time.LocalDate.parse(event.date, dateFormatter)
                            eventDate.isBefore(today)
                        } catch (e: Exception) { false }
                    }
                }
                else -> {
                    loadedEvents
                }
            }

            val sortedEvents = finalEvents.sortedWith { e1, e2 ->
                try {
                    val d1 = java.time.LocalDate.parse(e1.date, dateFormatter)
                    val d2 = java.time.LocalDate.parse(e2.date, dateFormatter)
                    d1.compareTo(d2)
                } catch (e: Exception) {
                    0
                }
            }

            events = sortedEvents

            Log.d(TAG, "Successfully loaded ${events.size} events")
            isLoading = false

        } catch (e: Exception) {
            Log.e(TAG, "Error loading events for filter: $filter", e)
            errorMessage = "Failed to load events: ${e.localizedMessage}"
            isLoading = false
        }
    }

    LaunchedEffect(userLocation, events) {
        if (userLocation != null && events.isNotEmpty()) {
            val updatedEvents = events.map { event ->
                if (event.latitude != null && event.longitude != null) {
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
            if (updatedEvents != events) {
                events = updatedEvents
            }
        }
    }

    LaunchedEffect(events, searchQuery, filterType, distanceFilter) {
        var result = events

        if (searchQuery.isNotBlank()) {
            result = result.filter {
                it.title.contains(searchQuery, ignoreCase = true) ||
                        it.description.contains(searchQuery, ignoreCase = true)
            }
        }
        if (filterType != "All") {
            result = result.filter { it.type == filterType }
        }
        if (distanceFilter != "Any") {
            result = result.filter {
                val dist = it.distance
                if (dist == null) false
                else when (distanceFilter) {
                    "< 1 km" -> dist < 1.0
                    "< 3 km" -> dist < 3.0
                    "< 5 km" -> dist < 5.0
                    "< 10 km" -> dist < 10.0
                    "< 20 km" -> dist < 20.0
                    "20+ km" -> dist >= 20.0
                    else -> true
                }
            }
        }
        filteredEvents = result
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
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Loading events...",
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
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
                Text("No events found for this category.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        else -> {
            Column(modifier = Modifier.fillMaxSize()) {

                Column(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(modifier = Modifier.weight(1f)) {
                            OutlinedButton(
                                onClick = { showFilterMenu = true },
                                modifier = Modifier.fillMaxWidth(),
                                contentPadding = PaddingValues(horizontal = 8.dp)
                            ) {
                                Text(if(filterType == "All") "Cat: All" else filterType, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                                Icon(Icons.Default.ArrowDropDown, null)
                            }
                            DropdownMenu(expanded = showFilterMenu, onDismissRequest = { showFilterMenu = false }, modifier = Modifier.fillMaxWidth(0.5f)) {
                                filterOptions.forEach { option ->
                                    DropdownMenuItem(text = { Text(option) }, onClick = { filterType = option; showFilterMenu = false })
                                }
                            }
                        }
                        Box(modifier = Modifier.weight(1f)) {
                            OutlinedButton(
                                onClick = { showDistanceMenu = true },
                                modifier = Modifier.fillMaxWidth(),
                                contentPadding = PaddingValues(horizontal = 8.dp)
                            ) {
                                Text(if(distanceFilter == "Any") "Dist: Any" else distanceFilter, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                                Icon(Icons.Default.ArrowDropDown, null)
                            }
                            DropdownMenu(expanded = showDistanceMenu, onDismissRequest = { showDistanceMenu = false }, modifier = Modifier.fillMaxWidth(0.5f)) {
                                distanceOptions.forEach { option ->
                                    DropdownMenuItem(text = { Text(option) }, onClick = { distanceFilter = option; showDistanceMenu = false })
                                }
                            }
                        }
                    }
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Search event...") },
                        leadingIcon = { Icon(Icons.Default.Search, "Search") },
                        trailingIcon = { if (searchQuery.isNotEmpty()) IconButton({ searchQuery = "" }) { Icon(Icons.Default.Close, "Clear") } },
                        singleLine = true
                    )
                }

                if (filteredEvents.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("No events match your search.")
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(bottom = 16.dp)
                    ) {
                        item {
                            Text(
                                text = "Found ${filteredEvents.size} event(s)",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                        }
                        items(items = filteredEvents, key = { it.id }) { event ->
                            EventCard(
                                event = event,
                                filter = filter,
                                onClick = { navController.navigate("event_details/${event.id}") }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EventCard(event: Event,filter: String,
                      onClick: () -> Unit) {
    val locationHelper = LocationHelper(LocalContext.current)
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