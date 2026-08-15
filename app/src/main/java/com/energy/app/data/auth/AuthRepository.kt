package com.energy.app.data.auth

data class AuthUser(
    val id: String,
    val name: String,
    val email: String?,
    val isGuest: Boolean
)

interface AuthRepository {
    suspend fun signInAsGuest(): Result<AuthUser>

    /**
     * Google Sign-In lands in M5 once the Supabase project + OAuth clients exist
     * (APP_SPEC §10). The ID token arrives from the Android CredentialManager flow.
     */
    suspend fun signInWithGoogle(idToken: String): Result<AuthUser>

    fun currentUser(): AuthUser?
    fun signOut()
}

/** In-memory guest implementation — no accounts, no cloud (M1–M4). */
class GuestAuthRepository : AuthRepository {

    private var user: AuthUser? = null

    override suspend fun signInAsGuest(): Result<AuthUser> {
        val guest = AuthUser(
            id = "guest",
            name = "Guest Runner",
            email = null,
            isGuest = true
        )
        user = guest
        return Result.success(guest)
    }

    override suspend fun signInWithGoogle(idToken: String): Result<AuthUser> =
        Result.failure(
            UnsupportedOperationException(
                "Google Sign-In arrives in M5 — it needs the Supabase project. Try guest mode!"
            )
        )

    override fun currentUser(): AuthUser? = user

    override fun signOut() {
        user = null
    }
}
