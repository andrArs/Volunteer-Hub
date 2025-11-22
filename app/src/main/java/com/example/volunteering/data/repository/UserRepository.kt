package com.example.volunteering.data.repository

import android.util.Log
import com.example.volunteering.data.model.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class UserRepository {
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    suspend fun getCurrentUser(): User? {
        return try {
            val uid = auth.currentUser?.uid ?: return null
            val doc = firestore.collection("users").document(uid).get().await()
            doc.toObject(User::class.java)?.copy(uid = doc.id)
        } catch (e: Exception) {
            Log.e("UserRepository", "Error getting user", e)
            null
        }
    }
}