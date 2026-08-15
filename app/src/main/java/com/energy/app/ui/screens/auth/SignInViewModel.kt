package com.energy.app.ui.screens.auth

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.energy.app.EnergyApplication
import com.energy.app.data.auth.AuthRepository
import com.energy.app.data.auth.AuthUser
import com.energy.app.data.cloud.CloudRepository
import com.energy.app.data.settings.SettingsRepository
import com.energy.app.data.settings.ThemeMode
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

    private val container = (application as EnergyApplication).container
    private val repository: AuthRepository = container.authRepository
    private val cloud: CloudRepository = container.cloudRepository
    private val settings: SettingsRepository = container.settingsRepository

    private val _uiState = MutableStateFlow(SignInUiState())
    val uiState: StateFlow<SignInUiState> = _uiState.asStateFlow()

    val themeMode: StateFlow<ThemeMode> = settings.themeMode
        .let { flow -> MutableStateFlow(ThemeMode.SYSTEM).also { s ->
            viewModelScope.launch { flow.collect { s.value = it } }
        } }

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch { settings.setThemeMode(mode) }
    }

    fun signInAsGuest() = launchSignIn {
        repository.signInAsGuest()
    }

    /** Google flow: CredentialManager → ID token → Supabase (when configured) → local session. */
    fun signInWithGoogle() {
        viewModelScope.launch {
            _uiState.value = SignInUiState(loading = true)
            when (val result = container.googleSignInHelper.signIn()) {
                is com.energy.app.data.auth.GoogleSignInResult.Success -> {
                    if (cloud.isConfigured) {
                        cloud.signInWithGoogle(result.idToken)
                            .onFailure { e ->
                                _uiState.value = SignInUiState(error = e.message)
                                return@launch
                            }
                    }
                    repository.signInAsGoogle(result.email, result.name)
                        .onSuccess { _uiState.value = SignInUiState(signedIn = true) }
                        .onFailure { e -> _uiState.value = SignInUiState(error = e.message) }
                }
                is com.energy.app.data.auth.GoogleSignInResult.Failure -> {
                    _uiState.value = SignInUiState(error = result.message)
                }
            }
        }
    }

    /** Email + password: create account or sign in (Supabase, when configured). */
    fun signInWithEmail(email: String, password: String, createAccount: Boolean) {
        viewModelScope.launch {
            _uiState.value = SignInUiState(loading = true)
            if (createAccount) {
                cloud.signUpWithEmail(email, password)
                    .onFailure { e ->
                        _uiState.value = SignInUiState(error = e.message)
                        return@launch
                    }
            } else {
                cloud.signInWithEmail(email, password)
                    .onFailure { e ->
                        _uiState.value = SignInUiState(error = e.message)
                        return@launch
                    }
            }
            repository.signInWithEmail(email)
                .onSuccess { _uiState.value = SignInUiState(signedIn = true) }
                .onFailure { e -> _uiState.value = SignInUiState(error = e.message) }
        }
    }

    private fun launchSignIn(block: suspend () -> Result<AuthUser>) {
        viewModelScope.launch {
            _uiState.value = SignInUiState(loading = true)
            _uiState.value = block().fold(
                onSuccess = { SignInUiState(signedIn = true) },
                onFailure = { e -> SignInUiState(error = e.message ?: "Something went wrong") }
            )
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}
