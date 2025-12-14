package com.example.volunteering.ui.screen

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import coil.compose.AsyncImage
import com.example.volunteering.data.model.Event
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import com.example.volunteering.R
import kotlinx.coroutines.launch
import com.example.volunteering.data.repository.EventRepository


private const val TAG = "EventDetailsScreen"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventDetailsScreen(navController: NavHostController, eventId: String) {
    val context = LocalContext.current
    val auth = FirebaseAuth.getInstance()
    val userId = auth.currentUser?.uid
    val repository = EventRepository()

    var event by remember { mutableStateOf<Event?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    var isInterested by remember { mutableStateOf(false) }
    var isGoing by remember { mutableStateOf(false) }

    var showDeleteDialog by remember { mutableStateOf(false) }
    var isDeleting by remember { mutableStateOf(false) }

    val locationHelper = remember { com.example.volunteering.utils.LocationHelper(context) }
    var userLocation by remember { mutableStateOf<android.location.Location?>(null) }
    var distanceText by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        if (locationHelper.hasLocationPermission()) {
            userLocation = locationHelper.getCurrentLocation()
        }
    }
    LaunchedEffect(eventId) {
        try {
            Log.d(TAG, "Loading event details for: $eventId")
            isLoading = true

            val firestore = FirebaseFirestore.getInstance()
            val doc = firestore.collection("events").document(eventId).get().await()

            if (doc.exists()) {
                event = doc.toObject(Event::class.java)?.copy(id = doc.id)
                event?.let {
                    isInterested = userId in it.interestedUsers
                    isGoing = userId in it.goingUsers
                }
                Log.d(TAG, "Event loaded: ${event?.title}")
            } else {
                errorMessage = "Event not found"
            }

            isLoading = false
        } catch (e: Exception) {
            Log.e(TAG, "Error loading event", e)
            errorMessage = "Failed to load event: ${e.localizedMessage}"
            isLoading = false
        }
    }

    LaunchedEffect(event, userLocation) {
        if (event != null && userLocation != null && event!!.latitude != null && event!!.longitude != null) {
            val dist = locationHelper.calculateDistance(
                userLocation!!.latitude,
                userLocation!!.longitude,
                event!!.latitude!!,
                event!!.longitude!!
            )
            distanceText = locationHelper.formatDistance(dist)
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("Event Details") },
            navigationIcon = {
                IconButton(onClick = { navController.popBackStack() }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.primary,
                titleContentColor = MaterialTheme.colorScheme.onPrimary,
                navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
            )
        )

        when {
            isLoading -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            errorMessage != null -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(errorMessage ?: "", color = MaterialTheme.colorScheme.error)
                        Button(onClick = { navController.popBackStack() }) {
                            Text("Go Back")
                        }
                    }
                }
            }
            event != null -> {
                EventDetailsContent(
                    event = event!!,
                    navController = navController,
                    userId = userId,
                    isInterested = isInterested,
                    isGoing = isGoing,
                    distanceText = distanceText,
                    onInterestedClick = {
                        if (userId != null) {
                            vibratePhone(context)
                            toggleInterested(eventId, userId, !isInterested) { success ->
                                if (success) isInterested = !isInterested
                            }
                        }
                    },
                    onGoingClick = {
                        if (userId != null) {
                            vibratePhone(context)
                            toggleGoing(eventId, userId, !isGoing) { success ->
                                if (success) isGoing = !isGoing
                            }
                        }
                    },
                    onDeleteClick = {
                        showDeleteDialog = true
                    }
                )
            }
        }
    }
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete Event") },
            text = { Text("Are you sure you want to delete this event? This action cannot be undone.") },
            confirmButton = {
                Button(
                    onClick = {
                        isDeleting = true
                        repository.deleteEvent(eventId) { success ->
                            isDeleting = false
                            if (success) {
                                navController.navigate("my_events") {
                                    popUpTo("my_events") { inclusive = false }
                                }
                            } else {
                                errorMessage = "Failed to delete event"
                                showDeleteDialog = false
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    if (isDeleting) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            color = MaterialTheme.colorScheme.onError,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text("Delete")
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun EventDetailsContent(
    event: Event,
    navController: NavHostController,
    userId: String?,
    isInterested: Boolean,
    isGoing: Boolean,
    distanceText: String?,
    onInterestedClick: () -> Unit,
    onGoingClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        if (event.imageUrl.isNotEmpty()) {
            AsyncImage(
                model = event.imageUrl,
                contentDescription = "Event image",
                modifier = Modifier
                    .fillMaxWidth()
                    .height(250.dp)
                    .clip(RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp))
                    .background(Color.LightGray),
                contentScale = ContentScale.Crop,
                onError = { state ->
                    Log.e("AsyncImageError", "Error loading image: ${state.result.throwable}")
                },
            )
        }
//        else {
//            Box(
//                modifier = Modifier
//                    .fillMaxWidth()
//                    .height(250.dp)
//                    .background(MaterialTheme.colorScheme.surfaceVariant)
//                    .clip(RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp)),
//                contentAlignment = Alignment.Center
//            ) {
//                Icon(
//                    imageVector = Icons.Default.Image,
//                    contentDescription = null,
//                    modifier = Modifier.size(80.dp),
//                    tint = MaterialTheme.colorScheme.onSurfaceVariant
//                )
//            }
//        }

        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = event.title,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(8.dp))
            Surface(
                color = Color(0xFF445E91),
                shape = MaterialTheme.shapes.small
            ) {
                Text(
                    text = "${event.type}",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "Description",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = event.description,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(20.dp))

            InfoRow(
                icon = Icons.Default.DateRange,
                label = "Date & Time",
                value = "${event.date} at ${event.time}"
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                verticalAlignment = Alignment.Top,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Default.LocationOn,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                    tint = Color(0xFF445E91)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "Location",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    val locationText = event.location

                    if (locationText.contains("(") && locationText.endsWith(")")) {
                        val parts = locationText.split(" (", limit = 2)
                        val placeName = parts[0]
                        val address = parts[1].removeSuffix(")")

                        Text(
                            text = placeName,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = address,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        Text(
                            text = locationText,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    if (distanceText != null) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = distanceText!!,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF445E91),
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            event.participants?.let { max ->
                Spacer(modifier = Modifier.height(12.dp))
                InfoRow(
                    icon = Icons.Default.Person,
                    label = "Participants",
                    value = "${event.goingUsers.size}/$max people going"
                )
            }

            Spacer(modifier = Modifier.height(32.dp))
            val isCreator = userId != null && userId == event.creatorUid
            if (!isCreator) {

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onInterestedClick,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = if (isInterested)
                                MaterialTheme.colorScheme.secondaryContainer
                            else
                                MaterialTheme.colorScheme.surface,
                            contentColor = MaterialTheme.colorScheme.onSurface
                        )
                    ) {
                        Icon(
                            imageVector = if (isInterested) Icons.Default.Star else Icons.Default.StarBorder,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(if (isInterested) "Interested ✓" else "Interested")
                    }

                    Button(
                        onClick = onGoingClick,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isGoing)
                                MaterialTheme.colorScheme.primary
                            else
                                MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = if (isGoing)
                                MaterialTheme.colorScheme.onPrimary
                            else
                                MaterialTheme.colorScheme.onSurface
                        )

                    ) {
                        Icon(
                            imageVector = if (isGoing) Icons.Default.Check else Icons.Default.Add,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(if (isGoing) "Going ✓" else "I'm Going")
                    }
                }
            }
            else{
                Divider(thickness = 1.dp, color = MaterialTheme.colorScheme.outlineVariant)
                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Admin Controls",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            navController.navigate("edit_event/${event.id}")
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit")
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Edit Event")
                    }

                    Button(
                        onClick = onDeleteClick,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error,
                            contentColor = MaterialTheme.colorScheme.onError
                        )
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete")
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Delete")
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun InfoRow(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, value: String) {
    Row(
        verticalAlignment = Alignment.Top,
        modifier = Modifier.fillMaxWidth()
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(24.dp),
            tint = Color(0xFF445E91)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

private fun vibratePhone(context: Context) {
    val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
        vibratorManager.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
    }

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        vibrator.vibrate(VibrationEffect.createOneShot(100, VibrationEffect.DEFAULT_AMPLITUDE))
    } else {
        @Suppress("DEPRECATION")
        vibrator.vibrate(100)
    }
}

private fun toggleInterested(eventId: String, userId: String, add: Boolean, onResult: (Boolean) -> Unit) {
    val firestore = FirebaseFirestore.getInstance()
    val eventRef = firestore.collection("events").document(eventId)

    firestore.runTransaction { transaction ->
        val snapshot = transaction.get(eventRef)
        val interestedUsers = snapshot.get("interestedUsers") as? List<String> ?: emptyList()

        val updatedList = if (add) {
            interestedUsers + userId
        } else {
            interestedUsers - userId
        }

        transaction.update(eventRef, "interestedUsers", updatedList)
    }.addOnSuccessListener {
        Log.d(TAG, "Interested status updated")
        onResult(true)
    }.addOnFailureListener { e ->
        Log.e(TAG, "Failed to update interested status", e)
        onResult(false)
    }
}

private fun toggleGoing(eventId: String, userId: String, add: Boolean, onResult: (Boolean) -> Unit) {
    val firestore = FirebaseFirestore.getInstance()
    val eventRef = firestore.collection("events").document(eventId)

    firestore.runTransaction { transaction ->
        val snapshot = transaction.get(eventRef)
        val goingUsers = snapshot.get("goingUsers") as? List<String> ?: emptyList()

        val updatedList = if (add) {
            goingUsers + userId
        } else {
            goingUsers - userId
        }

        transaction.update(eventRef, "goingUsers", updatedList)
    }.addOnSuccessListener {
        Log.d(TAG, "Going status updated")
        onResult(true)
    }.addOnFailureListener { e ->
        Log.e(TAG, "Failed to update going status", e)
        onResult(false)
    }
}