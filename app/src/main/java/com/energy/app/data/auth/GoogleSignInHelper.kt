package com.energy.app.data.auth

import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetCredentialResponse
import com.energy.app.BuildConfig
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

sealed interface GoogleSignInResult {
    data class Success(val idToken: String, val email: String?, val name: String?) : GoogleSignInResult
    data class Failure(val message: String) : GoogleSignInResult
}

/**
 * Google sign-in via CredentialManager (APP_SPEC §5.2). Requires the
 * Google Cloud OAuth client ID in local.properties (`google.clientId`).
 * With no client ID configured, the flow reports a clear setup message.
 */
class GoogleSignInHelper(private val context: Context) {

    private val _state = MutableStateFlow<GoogleSignInResult?>(null)
    val state: StateFlow<GoogleSignInResult?> = _state.asStateFlow()

    suspend fun signIn(): GoogleSignInResult {
        val clientId = BuildConfig.GOOGLE_CLIENT_ID
        if (clientId.isBlank()) {
            return GoogleSignInResult.Failure(
                "Google sign-in is not configured yet.\n\nSetup (2 min): create an OAuth 2.0 client ID " +
                    "(type: Web application) in Google Cloud Console, then put it in local.properties as " +
                    "google.clientId=… and rebuild."
            )
        }
        return runCatching {
            val option = GetGoogleIdOption.Builder()
                .setServerClientId(clientId)
                .setFilterByAuthorizedAccounts(false)
                .build()
            val request = GetCredentialRequest.Builder().addCredentialOption(option).build()
            val credentialManager = CredentialManager.create(context)
            val response: GetCredentialResponse = credentialManager.getCredential(context, request)
            val credential = response.credential
            if (credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                val googleCredential = GoogleIdTokenCredential.createFrom(credential.data)
                GoogleSignInResult.Success(
                    idToken = googleCredential.idToken,
                    email = googleCredential.id,
                    name = googleCredential.displayName
                )
            } else {
                GoogleSignInResult.Failure("Unsupported credential type: ${credential.type}")
            }
        }.getOrElse { e ->
            GoogleSignInResult.Failure(
                when {
                    e.message?.contains("CANCELLED", ignoreCase = true) == true -> "Sign-in cancelled."
                    else -> "Couldn't reach Google — check your connection and that the device has a Google account."
                }
            )
        }
    }
}
