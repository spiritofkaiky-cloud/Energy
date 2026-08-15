package com.energy.app.ui.screens.auth

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.energy.app.EnergyApplication
import com.energy.app.data.auth.AuthRepository
import com.energy.app.data.auth.AuthUser
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class SignInUiState(
    val loading: Boolean = false,
    val error: String? = null,
    val signedIn: Boolean = false
)

class SignInViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: AuthRepository =
        (application as EnergyApplication).container.authRepository

    private val _uiState = MutableStateFlow(SignInUiState())
    val uiState: StateFlow<SignInUiState> = _uiState.asStateFlow()

    fun signInAsGuest() = launchSignIn { repository.signInAsGuest() }

    fun signInWithGoogle(idToken: String) = launchSignIn { repository.signInWithGoogle(idToken) }

    private fun launchSignIn(block: suspend () -> Result<AuthUser>) {
        viewModelScope.launch {
            _uiState.value = SignInUiState(loading = true)
            val result = block()
            _uiState.value = result.fold(
                onSuccess = { SignInUiState(signedIn = true) },
                onFailure = { e -> SignInUiState(error = e.message ?: "Something went wrong") }
            )
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}
