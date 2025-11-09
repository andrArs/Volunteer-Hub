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


private const val TAG = "EventDetailsScreen"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventDetailsScreen(navController: NavHostController, eventId: String) {
    val context = LocalContext.current
    val auth = FirebaseAuth.getInstance()
    val userId = auth.currentUser?.uid

    var event by remember { mutableStateOf<Event?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    var isInterested by remember { mutableStateOf(false) }
    var isGoing by remember { mutableStateOf(false) }

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

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("Event Details") },
            navigationIcon = {
                IconButton(onClick = { navController.popBackStack() }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                }
            }
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
                    isInterested = isInterested,
                    isGoing = isGoing,
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
                    }
                )
            }
        }
    }
}

@Composable
private fun EventDetailsContent(
    event: Event,
    isInterested: Boolean,
    isGoing: Boolean,
    onInterestedClick: () -> Unit,
    onGoingClick: () -> Unit
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
                    .clip(RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp)),
                contentScale = ContentScale.Crop
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
                color = MaterialTheme.colorScheme.primaryContainer,
                shape = MaterialTheme.shapes.small
            ) {
                Text(
                    text = "${event.type}",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
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

            InfoRow(
                icon = Icons.Default.LocationOn,
                label = "Location",
                value = event.location
            )

            event.participants?.let { max ->
                Spacer(modifier = Modifier.height(12.dp))
                InfoRow(
                    icon = Icons.Default.Person,
                    label = "Participants",
                    value = "${event.goingUsers.size}/$max people going"
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

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
                            MaterialTheme.colorScheme.surface
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
                            MaterialTheme.colorScheme.secondary
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
            tint = MaterialTheme.colorScheme.primary
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

// Firebase update functions
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