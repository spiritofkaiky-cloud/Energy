package com.energy.app.data.auth

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

data class AuthUser(
    val id: String,
    val name: String,
    val email: String?,
    val isGuest: Boolean
)

private val Context.authStore by preferencesDataStore(name = "energy_auth")

/**
 * Auth with a persisted session (APP_SPEC §37): the user stays signed in
 * across process death and app restarts. Guests are remembered too, so
 * relaunching never forces a re-tap of "Continue as guest".
 */
interface AuthRepository {
    suspend fun signInAsGuest(): Result<AuthUser>
    suspend fun signInAsGoogle(email: String?, name: String?): Result<AuthUser>
    suspend fun signInWithEmail(email: String): Result<AuthUser>

    /**
     * Legacy seam — real Google flow lives in SignInViewModel
     * (CredentialManager → Supabase). Kept for interface stability.
     */
    suspend fun signInWithGoogle(idToken: String): Result<AuthUser>

    /** Reactive current user (null = signed out). */
    val currentUser: StateFlow<AuthUser?>

    fun currentUserIsGuest(): Boolean = currentUser.value?.isGuest != false

    fun signOut()
}

class PersistedAuthRepository(private val context: Context) : AuthRepository {

    private object Keys {
        val ID = stringPreferencesKey("user_id")
        val NAME = stringPreferencesKey("user_name")
        val EMAIL = stringPreferencesKey("user_email")
        val GUEST = booleanPreferencesKey("user_guest")
        val SIGNED_IN = booleanPreferencesKey("signed_in")
    }

    private val _currentUser = MutableStateFlow<AuthUser?>(null)
    override val currentUser: StateFlow<AuthUser?> = _currentUser.asStateFlow()

    /** One-shot read of the persisted session at startup. */
    suspend fun restoreSession() {
        val prefs = runCatching { context.authStore.data.first() }.getOrNull() ?: return
        if (prefs[Keys.SIGNED_IN] != true) return
        _currentUser.value = AuthUser(
            id = prefs[Keys.ID] ?: "guest",
            name = prefs[Keys.NAME] ?: "Runner",
            email = prefs[Keys.EMAIL],
            isGuest = prefs[Keys.GUEST] ?: true
        )
    }

    private suspend fun persist(user: AuthUser) {
        context.authStore.edit { prefs ->
            prefs[Keys.SIGNED_IN] = true
            prefs[Keys.ID] = user.id
            prefs[Keys.NAME] = user.name
            prefs[Keys.EMAIL] = user.email.orEmpty()
            prefs[Keys.GUEST] = user.isGuest
        }
        _currentUser.value = user
    }

    override suspend fun signInAsGuest(): Result<AuthUser> {
        val guest = AuthUser(id = "guest", name = "Guest Runner", email = null, isGuest = true)
        persist(guest)
        return Result.success(guest)
    }

    override suspend fun signInWithGoogle(idToken: String): Result<AuthUser> =
        Result.failure(
            UnsupportedOperationException(
                "Google Sign-In is driven by SignInViewModel via CredentialManager + Supabase."
            )
        )

    override suspend fun signInAsGoogle(email: String?, name: String?): Result<AuthUser> {
        val user = AuthUser(
            id = "google",
            name = name ?: "Runner",
            email = email,
            isGuest = false
        )
        persist(user)
        return Result.success(user)
    }

    override suspend fun signInWithEmail(email: String): Result<AuthUser> {
        val user = AuthUser(
            id = "email",
            name = email.substringBefore("@").replaceFirstChar { it.uppercase() },
            email = email,
            isGuest = false
        )
        persist(user)
        return Result.success(user)
    }

    override fun signOut() {
        _currentUser.value = null
        // Clear the persisted session asynchronously — the in-memory
        // sign-out is what gates the UI.
        CoroutineScope(Dispatchers.IO).launch {
            context.authStore.edit { prefs ->
                prefs[Keys.SIGNED_IN] = false
                prefs[Keys.EMAIL] = ""
            }
        }
    }
}
