package com.example.volunteering.data.repository

import com.example.volunteering.data.model.Event
import com.google.firebase.firestore.FirebaseFirestore

class EventRepository {
    private val firestore = FirebaseFirestore.getInstance()
    private val eventCollection = firestore.collection("events")

    fun createEvent(event: Event, onResult: (Boolean) -> Unit) {
        val documentRef = eventCollection.document()
        val newId = documentRef.id
        val finalEvent = event.copy(id = newId)

        documentRef.set(finalEvent)
            .addOnSuccessListener { onResult(true) }
            .addOnFailureListener { onResult(false) }
    }

    fun getEventById(eventId: String, onResult: (Event?) -> Unit) {
        eventCollection.document(eventId).get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    val event = document.toObject(Event::class.java)

                    onResult(event)
                } else {
                    onResult(null)
                }
            }
            .addOnFailureListener {
                onResult(null)
            }
    }

    fun updateEvent(event: Event, onResult: (Boolean) -> Unit) {
        eventCollection.document(event.id).set(event)
            .addOnSuccessListener { onResult(true) }
            .addOnFailureListener { onResult(false) }
    }

    fun deleteEvent(eventId: String, onResult: (Boolean) -> Unit) {
        eventCollection.document(eventId).delete()
            .addOnSuccessListener { onResult(true) }
            .addOnFailureListener { onResult(false) }
    }
}
