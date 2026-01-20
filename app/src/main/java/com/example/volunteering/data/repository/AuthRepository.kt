package com.example.volunteering.data.repository

import com.example.volunteering.data.model.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore

class AuthRepository {
    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()

    fun login(email: String, password: String, onResult: (Boolean, String?) -> Unit) {
        val cleanEmail = email.trim()
        when {
            cleanEmail.isBlank() && password.isBlank() -> {
                onResult(false, "Please enter your email and password.")
                return
            }
            cleanEmail.isBlank() -> {
                onResult(false, "Please enter your email.")
                return
            }
            password.isBlank() -> {
                onResult(false, "Please enter your password.")
                return
            }
        }

        auth.signInWithEmailAndPassword(cleanEmail, password)
            .addOnSuccessListener {
                onResult(true, null)
            }
            .addOnFailureListener { e ->
                val message = when (e) {
                    is com.google.firebase.auth.FirebaseAuthInvalidUserException ->
                        "No account found with this email."

                    is com.google.firebase.auth.FirebaseAuthInvalidCredentialsException ->
                        "Incorrect email or password."

                    is com.google.firebase.FirebaseNetworkException ->
                        "No internet connection. Please try again."

                    else ->
                        "Login failed. Please check your credentials and try again."
                }

                onResult(false, message)
            }
    }

    fun register(name: String, email: String, password: String, onResult: (Boolean, String?) -> Unit) {
        val cleanName = name.trim()
        val cleanEmail = email.trim()
        when {
            cleanName.isBlank() && cleanEmail.isBlank() && password.isBlank() -> {
                onResult(false, "Please enter your name, email, and password.")
                return
            }
            cleanName.isBlank() -> {
                onResult(false, "Please enter your name.")
                return
            }
            cleanEmail.isBlank() -> {
                onResult(false, "Please enter your email.")
                return
            }
            password.isBlank() -> {
                onResult(false, "Please enter your password.")
                return
            }
            password.length < 6 -> {
                onResult(false, "Password must be at least 6 characters long.")
                return
            }
        }

        auth.createUserWithEmailAndPassword(cleanEmail, password)
            .addOnSuccessListener {
                val user = auth.currentUser
                val uid = user?.uid
                if (uid == null) {
                    onResult(false, "Account created, but failed to read user id. Please try again.")
                    return@addOnSuccessListener
                }

                val profileUpdates = UserProfileChangeRequest.Builder()
                    .setDisplayName(cleanName)
                    .build()

                user.updateProfile(profileUpdates)
                    .addOnFailureListener {
                    }

                val userData = User(uid = uid, name = cleanName, email = cleanEmail)
                firestore.collection("users").document(uid).set(userData)
                    .addOnSuccessListener { onResult(true, null) }
                    .addOnFailureListener { e ->
                        val msg = when (e) {
                            is com.google.firebase.FirebaseNetworkException ->
                                "No internet connection. Please try again."
                            else ->
                                "Account created, but failed to save profile data. Please try again."
                        }
                        onResult(false, msg)
                    }
            }
            .addOnFailureListener { e ->
                val message = when (e) {
                    is com.google.firebase.auth.FirebaseAuthUserCollisionException ->
                        "This email is already in use."

                    is com.google.firebase.auth.FirebaseAuthInvalidCredentialsException ->
                        "Please enter a valid email address."

                    is com.google.firebase.FirebaseNetworkException ->
                        "No internet connection. Please try again."

                    is com.google.firebase.auth.FirebaseAuthWeakPasswordException ->
                        "Password is too weak. Please choose a stronger one."

                    else ->
                        "Registration failed. Please try again."
                }

                onResult(false, message)
            }
    }
}
