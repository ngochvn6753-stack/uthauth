package com.example.uthauth

import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

sealed interface AuthUiState {
    data object Idle : AuthUiState
    data class Error(val message: String) : AuthUiState
    data class Success(val name: String, val email: String) : AuthUiState
}

class AuthViewModel : ViewModel() {
    private val auth = FirebaseAuth.getInstance()
    private val _state = MutableStateFlow<AuthUiState>(AuthUiState.Idle)
    val state: StateFlow<AuthUiState> = _state.asStateFlow()

    fun signInWithIdToken(idToken: String) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        auth.signInWithCredential(credential).addOnCompleteListener { task ->
            _state.value = if (task.isSuccessful) {
                val user = auth.currentUser
                AuthUiState.Success(user?.displayName ?: "", user?.email ?: "")
            } else {
                AuthUiState.Error(task.exception?.message ?: "Đăng nhập thất bại")
            }
        }
    }

    fun onError(message: String) {
        _state.value = AuthUiState.Error(message)
    }

    fun currentUser() = auth.currentUser

    fun signOut() {
        auth.signOut()
        _state.value = AuthUiState.Idle
    }
}