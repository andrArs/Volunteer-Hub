package com.example.volunteering.ui.screen

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowDropUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.example.volunteering.data.model.Event
import com.example.volunteering.data.repository.EventRepository
import com.google.firebase.auth.FirebaseAuth
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Calendar
import com.example.volunteering.data.model.EventTypes
import android.net.Uri
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import com.google.firebase.storage.FirebaseStorage
import java.util.UUID
import coil.compose.AsyncImage
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.ui.window.PopupProperties
import com.example.volunteering.utils.GeocodingService
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateEventScreen(navController: NavHostController) {
    val context = LocalContext.current
    val auth = FirebaseAuth.getInstance()
    val repository = EventRepository()

    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var date by remember { mutableStateOf("") }
    var time by remember { mutableStateOf("") }
    var participants by remember { mutableStateOf("") }
    var type by remember { mutableStateOf("") }
    var showTypeMenu by remember { mutableStateOf(false) }
    var location by remember { mutableStateOf("") }
    var imageUrl by remember { mutableStateOf("") }
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var isUploadingImage by remember { mutableStateOf(false) }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            selectedImageUri = it
            isUploadingImage = true
            uploadImageToFirebase(it) { downloadUrl ->
                imageUrl = downloadUrl ?: ""
                isUploadingImage = false
                if (downloadUrl == null) {
                    var errorMessage = "Failed to upload image"
                }
            }
        }
    }
    var errorMessage by remember { mutableStateOf("") }

    val calendar = Calendar.getInstance()
    val dateFormatter = DateTimeFormatter.ofPattern("dd-MM-yyyy")
    val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")

    var latitude by remember { mutableStateOf<Double?>(null) }
    var longitude by remember { mutableStateOf<Double?>(null) }
    var isGeocodingLocation by remember { mutableStateOf(false) }

    val geocodingService = remember { GeocodingService(context) }
    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        TopAppBar(
            title = {
                Text(
                    text = "Create Event",
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

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Event Name") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Description") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3,
                maxLines = 5
            )

            // Date Picker
            val dateInteractionSource = remember { MutableInteractionSource() }
            OutlinedTextField(
                value = date,
                onValueChange = {},
                label = { Text("Date") },
                readOnly = true,
                modifier = Modifier.fillMaxWidth(),
                interactionSource = dateInteractionSource,
                singleLine = true
            )

            LaunchedEffect(dateInteractionSource) {
                dateInteractionSource.interactions.collect { interaction ->
                    if (interaction is PressInteraction.Release) {
                        val datePickerDialog = DatePickerDialog(
                            context,
                            { _, year, month, dayOfMonth ->
                                val selectedDate = LocalDate.of(year, month + 1, dayOfMonth)
                                date = selectedDate.format(dateFormatter)
                            },
                            calendar.get(Calendar.YEAR),
                            calendar.get(Calendar.MONTH),
                            calendar.get(Calendar.DAY_OF_MONTH)
                        )
                        datePickerDialog.datePicker.minDate = calendar.timeInMillis
                        datePickerDialog.show()
                    }
                }
            }

            val timeInteractionSource = remember { MutableInteractionSource() }
            OutlinedTextField(
                value = time,
                onValueChange = {},
                label = { Text("Time") },
                readOnly = true,
                modifier = Modifier.fillMaxWidth(),
                interactionSource = timeInteractionSource,
                singleLine = true
            )

            LaunchedEffect(timeInteractionSource) {
                timeInteractionSource.interactions.collect { interaction ->
                    if (interaction is PressInteraction.Release) {
                        TimePickerDialog(
                            context,
                            { _, hour, minute ->
                                val selectedTime = LocalTime.of(hour, minute)
                                time = selectedTime.format(timeFormatter)
                            },
                            calendar.get(Calendar.HOUR_OF_DAY),
                            calendar.get(Calendar.MINUTE),
                            true
                        ).show()
                    }
                }
            }

            Box(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = type,
                    onValueChange = {},
                    label = { Text("Event Type") },
                    modifier = Modifier.fillMaxWidth(),
                    readOnly = true,
                    singleLine = true,
                    trailingIcon = {
                        IconButton(onClick = { showTypeMenu = !showTypeMenu }) {
                            Icon(
                                imageVector = if (showTypeMenu)
                                    Icons.Default.ArrowDropUp
                                else
                                    Icons.Default.ArrowDropDown,
                                contentDescription = "Select type"
                            )
                        }
                    }
                )

                DropdownMenu(
                    expanded = showTypeMenu,
                    onDismissRequest = { showTypeMenu = false },
                    modifier = Modifier
                        .fillMaxWidth(0.9f)
                        .heightIn(max = 300.dp)
                ) {
                    EventTypes.ALL_TYPES.forEach { eventType ->
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = eventType,
                                    style = MaterialTheme.typography.bodyLarge
                                )
                            },
                            onClick = {
                                type = eventType
                                showTypeMenu = false
                            }
                        )
                    }
                }
            }

            var locationSuggestions by remember { mutableStateOf<List<android.location.Address>>(emptyList()) }
            var showLocationMenu by remember { mutableStateOf(false) }

            Box(
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = location,
                    onValueChange = { newLocation ->
                        location = newLocation
                        if (latitude != null) {
                            latitude = null
                            longitude = null
                        }

                        if (newLocation.length > 2) {
                            scope.launch {
                                isGeocodingLocation = true
                                try {
                                    val results = geocodingService.getAddressSuggestions(newLocation)
                                    locationSuggestions = results
                                    showLocationMenu = results.isNotEmpty()
                                } catch (e: Exception) {
                                    Log.e("CreateEvent", "Error fetching suggestions", e)
                                }
                                isGeocodingLocation = false
                            }
                        } else {
                            showLocationMenu = false
                        }
                    },
                    label = { Text("Location") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    trailingIcon = {
                        if (isGeocodingLocation) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp
                            )
                        } else if (latitude != null && longitude != null) {
                            Icon(
                                imageVector = Icons.Default.LocationOn,
                                contentDescription = "Location confirmed",
                                tint = Color(0xFF4CAF50)
                            )
                        }
                    }
                )

                DropdownMenu(
                    expanded = showLocationMenu,
                    onDismissRequest = { showLocationMenu = false },
                    properties = PopupProperties(focusable = false),
                    modifier = Modifier.fillMaxWidth(0.9f).heightIn(max = 200.dp)
                ) {
                    locationSuggestions.forEach { address ->
                        val addressText = (0..address.maxAddressLineIndex)
                            .joinToString(", ") { address.getAddressLine(it) }

                        DropdownMenuItem(
                            text = {
                                Column {
                                    Text(
                                        text = address.featureName ?: addressText.take(20),
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = addressText,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color.Gray,
                                        maxLines = 1
                                    )
                                }
                            },
                            onClick = {

                                val specificName = address.featureName
                                val fullAddress = addressText

                                val finalLocationText = if (specificName != null && !specificName[0].isDigit()) {
                                    "$specificName ($fullAddress)"
                                } else {
                                    fullAddress
                                }

                                location = finalLocationText

                                latitude = address.latitude
                                longitude = address.longitude

                                showLocationMenu = false
                                isGeocodingLocation = false

                                Log.d("CreateEvent", "Selected: $finalLocationText -> $latitude, $longitude")
                            }
                        )
                    }
                }
            }

            OutlinedTextField(
                value = participants,
                onValueChange = { participants = it },
                label = { Text("Max Participants (Optional)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                placeholder = { Text("Leave empty for unlimited") }
            )

            Column(modifier = Modifier.fillMaxWidth()) {


                if (selectedImageUri != null) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Box(modifier = Modifier.fillMaxSize()) {
                            AsyncImage(
                                model = selectedImageUri,
                                contentDescription = "Selected image",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                            if (isUploadingImage) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(Color.Black.copy(alpha = 0.5f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CircularProgressIndicator(color = Color.White)
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }

                OutlinedButton(
                    onClick = { imagePickerLauncher.launch("image/*") },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isUploadingImage,
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(if (selectedImageUri != null) "Change Image" else "Upload Image")
                }
            }

            if (errorMessage.isNotEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Text(
                        text = "$errorMessage",
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                enabled = !isUploadingImage,

                onClick = {
                    val formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy")
                    val today = LocalDate.now()
                    val eventDate = try {
                        LocalDate.parse(date, formatter)
                    } catch (e: Exception) {
                        null
                    }

                    val isValid = when {
                        title.isBlank() || description.isBlank() || date.isBlank() || time.isBlank() ||
                                type.isBlank() || location.isBlank() -> {
                            errorMessage = "All fields except image and participants are required."
                            false
                        }
                        eventDate == null || eventDate.isBefore(today) -> {
                            errorMessage = "Date must be today or in the future."
                            false
                        }
                        participants.isNotBlank() && participants.toIntOrNull() == null -> {
                            errorMessage = "Participants must be a valid number."
                            false
                        }
                        participants.toIntOrNull()?.let { it <= 0 } == true -> {
                            errorMessage = "Participants must be a positive number."
                            false
                        }
                        else -> {
                            val eventTime = try {
                                LocalTime.parse(time, timeFormatter)
                            } catch (e: Exception) {
                                null
                            }

                            if (eventDate == today && eventTime != null && eventTime.isBefore(LocalTime.now())) {
                                errorMessage = "Time must be in the future."
                                false
                            } else true
                        }
                    }

                    if (isValid) {
                        val uid = auth.currentUser?.uid
                        if (uid == null) {
                            errorMessage = "User not logged in."
                            return@Button
                        }

                        val event = Event(
                            title = title,
                            description = description,
                            date = date,
                            time = time,
                            participants = participants.toIntOrNull(),
                            type = type,
                            location = location,
                            latitude = latitude,
                            longitude = longitude,
                            imageUrl = imageUrl,
                            creatorUid = uid
                        )
                        repository.createEvent(event) { success ->
                            if (success) navController.navigate("home")
                            else errorMessage = "Failed to create event. Try again."
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
            ) {
                if (isUploadingImage) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Wait, uploading image...")
                } else {
                    Text(
                        text = "Create Event",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

fun uploadImageToFirebase(uri: Uri, onComplete: (String?) -> Unit) {
    val storage = FirebaseStorage.getInstance()
    val storageRef = storage.reference
    val imageRef = storageRef.child("event_images/${UUID.randomUUID()}.jpg")

    imageRef.putFile(uri)
        .addOnSuccessListener {
            imageRef.downloadUrl.addOnSuccessListener { downloadUri ->
                onComplete(downloadUri.toString())
            }.addOnFailureListener {
                onComplete(null)
            }
        }
        .addOnFailureListener {
            onComplete(null)
        }
}