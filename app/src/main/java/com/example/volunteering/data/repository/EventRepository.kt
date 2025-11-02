package com.example.volunteering.data.repository

import com.example.volunteering.data.model.Event
import com.google.firebase.firestore.FirebaseFirestore

class EventRepository {
    private val firestore = FirebaseFirestore.getInstance()

    fun createEvent(event: Event, onResult: (Boolean) -> Unit) {
        firestore.collection("events")
            .add(event)
            .addOnSuccessListener { onResult(true) }
            .addOnFailureListener { onResult(false) }
    }
}
