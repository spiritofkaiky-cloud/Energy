package com.energy.app.data.auth

data class AuthUser(
    val id: String,
    val name: String,
    val email: String?,
    val isGuest: Boolean
)

interface AuthRepository {
    suspend fun signInAsGuest(): Result<AuthUser>
    suspend fun signInAsGoogle(email: String?, name: String?): Result<AuthUser>
    suspend fun signInWithEmail(email: String): Result<AuthUser>

    /**
     * Legacy seam — real Google flow lives in SignInViewModel
     * (CredentialManager → Supabase). Kept for interface stability.
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
                "Google Sign-In is driven by SignInViewModel via CredentialManager + Supabase."
            )
        )

    override suspend fun signInAsGoogle(email: String?, name: String?): Result<AuthUser> {
        val user = AuthUser(
            id = "google",
            name = name ?: "Google Runner",
            email = email,
            isGuest = false
        )
        this.user = user
        return Result.success(user)
    }

    override suspend fun signInWithEmail(email: String): Result<AuthUser> {
        val user = AuthUser(
            id = "email",
            name = email.substringBefore("@").replaceFirstChar { it.uppercase() },
            email = email,
            isGuest = false
        )
        this.user = user
        return Result.success(user)
    }

    override fun currentUser(): AuthUser? = user

    override fun signOut() {
        user = null
    }
}
