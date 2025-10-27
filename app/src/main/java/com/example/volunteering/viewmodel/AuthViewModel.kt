package com.example.volunteering.viewmodel

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import com.example.volunteering.data.repository.AuthRepository

class AuthViewModel : ViewModel() {
    private val repo = AuthRepository()

    var errorMessage = mutableStateOf<String?>(null)
        private set

    fun login(email: String, password: String, onSuccess: () -> Unit) {
        repo.login(email, password) { success, error ->
            if (success) {
                errorMessage.value = null
                onSuccess()
            } else {
                errorMessage.value = error
            }
        }
    }

    fun register(name: String, email: String, password: String, onSuccess: () -> Unit) {
        repo.register(name, email, password) { success, error ->
            if (success) {
                errorMessage.value = null
                onSuccess()
            } else {
                errorMessage.value = error
            }
        }
    }

}
