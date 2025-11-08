package com.example.volunteering.ui.screen

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.example.volunteering.data.model.Event
import com.example.volunteering.data.repository.EventRepository
import com.google.firebase.auth.FirebaseAuth
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Calendar

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
    var location by remember { mutableStateOf("") }
    var imageUrl by remember { mutableStateOf("") }

    var errorMessage by remember { mutableStateOf("") }

    val calendar = Calendar.getInstance()
    val dateFormatter = DateTimeFormatter.ofPattern("dd-MM-yyyy")
    val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")
    val fieldModifier = Modifier
        .fillMaxWidth()
        .height(56.dp)
    val fieldTextStyle = MaterialTheme.typography.bodyLarge

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Create New Event", style = MaterialTheme.typography.headlineSmall)

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Event Name") })
        OutlinedTextField(value = description, onValueChange = { description = it }, label = { Text("Description") })

        // DATE PICKER
        val dateInteractionSource = remember { MutableInteractionSource() }
        OutlinedTextField(
            value = date,
            onValueChange = {},
            label = { Text("Date") },
            readOnly = true,
            modifier = fieldModifier,
            textStyle = fieldTextStyle,
            interactionSource = dateInteractionSource
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

// TIME PICKER
        val timeInteractionSource = remember { MutableInteractionSource() }
        OutlinedTextField(
            value = time,
            onValueChange = {},
            label = { Text("Time") },
            readOnly = true,
            modifier = Modifier
                .fillMaxWidth(),
            interactionSource = timeInteractionSource
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

        OutlinedTextField(value = participants, onValueChange = { participants = it }, label = { Text("Max Participants") })
        OutlinedTextField(value = type, onValueChange = { type = it }, label = { Text("Event Type") })
        OutlinedTextField(value = location, onValueChange = { location = it }, label = { Text("Location") })
        OutlinedTextField(value = imageUrl, onValueChange = { imageUrl = it }, label = { Text("Image URL") })

        if (errorMessage.isNotEmpty()) {
            Text(errorMessage, color = Color.Red, style = MaterialTheme.typography.bodySmall)
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(onClick = {
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
                    imageUrl = imageUrl,
                    creatorUid = uid
                )
                repository.createEvent(event) { success ->
                    if (success) navController.navigate("home")
                    else errorMessage = "Failed to create event. Try again."
                }
            }
        }) {
            Text("Create Event")
        }
    }
}
