package com.example.volunteering.ui.screen

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditEventScreen(navController: NavHostController, eventId: String) {
    val context = LocalContext.current
    val auth = FirebaseAuth.getInstance()
    val repository = EventRepository()

    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var date by remember { mutableStateOf("") }
    var time by remember { mutableStateOf("") }
    var participants by remember { mutableStateOf("") }
    var type by remember { mutableStateOf("") }
    var location by remember { mutableStateOf("") }
    var imageUrl by remember { mutableStateOf("") }
    var creatorUid by remember { mutableStateOf<String?>(null) }

    var errorMessage by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(true) }

    val calendar = Calendar.getInstance()
    val dateFormatter = DateTimeFormatter.ofPattern("dd-MM-yyyy")
    val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")

    LaunchedEffect(eventId) {
        repository.getEventById(eventId) { event ->
            if (event != null) {
                title = event.title
                description = event.description
                date = event.date
                time = event.time
                participants = event.participants?.toString() ?: ""
                type = event.type
                location = event.location
                imageUrl = event.imageUrl
                creatorUid = event.creatorUid
            } else {
                errorMessage = "Event not found."
            }
            isLoading = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Edit Event",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = {
                        navController.popBackStack()
                    }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
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
                            val initialDate = try {
                                LocalDate.parse(date, dateFormatter)
                            } catch (e: Exception) {
                                LocalDate.now()
                            }
                            val datePickerDialog = DatePickerDialog(
                                context,
                                { _, year, month, dayOfMonth ->
                                    val selectedDate = LocalDate.of(year, month + 1, dayOfMonth)
                                    date = selectedDate.format(dateFormatter)
                                },
                                initialDate.year,
                                initialDate.monthValue - 1,
                                initialDate.dayOfMonth
                            )
                            datePickerDialog.datePicker.minDate = Calendar.getInstance().timeInMillis
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
                            val initialTime = try {
                                LocalTime.parse(time, timeFormatter)
                            } catch (e: Exception) {
                                LocalTime.now()
                            }
                            TimePickerDialog(
                                context,
                                { _, hour, minute ->
                                    val selectedTime = LocalTime.of(hour, minute)
                                    time = selectedTime.format(timeFormatter)
                                },
                                initialTime.hour,
                                initialTime.minute,
                                true
                            ).show()
                        }
                    }
                }

                OutlinedTextField(
                    value = type,
                    onValueChange = { type = it },
                    label = { Text("Event Type") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    placeholder = { Text("e.g., Community Service, Education, Environmental") }
                )

                OutlinedTextField(
                    value = location,
                    onValueChange = { location = it },
                    label = { Text("Location") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = participants,
                    onValueChange = { participants = it },
                    label = { Text("Max Participants (Optional)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    placeholder = { Text("Leave empty for unlimited") }
                )

                OutlinedTextField(
                    value = imageUrl,
                    onValueChange = { imageUrl = it },
                    label = { Text("Image URL (Optional)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

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
                    onClick = {
                        val uid = auth.currentUser?.uid
                        if (uid == null) {
                            errorMessage = "User not logged in."
                            return@Button
                        }
                        if (uid != creatorUid) {
                            errorMessage = "You do not have permission to edit this event."
                            return@Button
                        }


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

                            val updatedEvent = Event(
                                id = eventId,
                                title = title,
                                description = description,
                                date = date,
                                time = time,
                                participants = participants.toIntOrNull(),
                                type = type,
                                location = location,
                                imageUrl = imageUrl,
                                creatorUid = creatorUid!!
                            )

                            repository.updateEvent(updatedEvent) { success ->
                                if (success) navController.popBackStack()
                                else errorMessage = "Failed to update event. Try again."
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                ) {
                    Text(
                        text = "Save Changes",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}