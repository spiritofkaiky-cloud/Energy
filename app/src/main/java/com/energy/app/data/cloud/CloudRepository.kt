package com.energy.app.data.cloud

import com.energy.app.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.UUID

enum class CloudStatus {
    NOT_CONFIGURED,   // supabase.url/key missing from local.properties
    SIGNED_OUT,
    SIGNING_IN,
    SYNCING,
    SYNCED,
    ERROR
}

data class CloudState(
    val status: CloudStatus = if (isConfigured()) CloudStatus.SIGNED_OUT else CloudStatus.NOT_CONFIGURED,
    val userEmail: String? = null,
    val message: String? = null
) {
    companion object {
        fun isConfigured(): Boolean =
            BuildConfig.SUPABASE_URL.isNotBlank() && BuildConfig.SUPABASE_KEY.isNotBlank()
    }
}

/**
 * Supabase gateway (APP_SPEC §10) — pure REST, zero third-party SDKs.
 *
 * Auth:      POST /auth/v1/token?grant_type=id_token  (Google ID token → session)
 * Workouts:  POST /rest/v1/workouts                   (PostgREST insert, RLS-protected)
 *
 * Fully inert until the user's Supabase project is configured in
 * local.properties — the app degrades gracefully to local-only mode.
 */
class CloudRepository {

    private val _state = MutableStateFlow(CloudState())
    val state: StateFlow<CloudState> = _state.asStateFlow()

    val isConfigured: Boolean get() = CloudState.isConfigured()

    private var accessToken: String? = null

    suspend fun signInWithGoogle(idToken: String): Result<Unit> {
        if (!isConfigured) return Result.failure(
            IllegalStateException(
                "Cloud not configured. Add supabase.url and supabase.key to local.properties (see README)."
            )
        )
        _state.value = CloudState(status = CloudStatus.SIGNING_IN)
        return withContext(Dispatchers.IO) {
            runCatching {
                val body = JSONObject()
                    .put("provider", "google")
                    .put("id_token", idToken)
                    .toString()
                val response = http(
                    method = "POST",
                    path = "/auth/v1/token?grant_type=id_token",
                    body = body,
                    auth = false
                )
                val json = JSONObject(response)
                accessToken = json.optString("access_token")
                _state.value = CloudState(
                    status = CloudStatus.SIGNED_OUT,
                    userEmail = json.optJSONObject("user")?.optString("email")
                )
            }.onFailure { e ->
                _state.value = CloudState(status = CloudStatus.ERROR, message = e.message)
            }
        }
    }

    /** Push a workout JSON payload to the `workouts` table (RLS: user owns rows). */
    suspend fun syncWorkout(payload: String): Result<Unit> {
        if (!isConfigured) return Result.failure(IllegalStateException("Cloud not configured"))
        val token = accessToken ?: return Result.failure(
            IllegalStateException("Sign in with Google to sync workouts")
        )
        _state.value = CloudState(status = CloudStatus.SYNCING)
        return withContext(Dispatchers.IO) {
            runCatching {
                val body = JSONObject()
                    .put("id", UUID.randomUUID().toString())
                    .put("payload", payload)
                    .toString()
                http(
                    method = "POST",
                    path = "/rest/v1/workouts",
                    body = body,
                    auth = true
                )
                _state.value = CloudState(status = CloudStatus.SYNCED)
            }.onFailure { e ->
                _state.value = CloudState(status = CloudStatus.ERROR, message = e.message)
            }
        }
    }

    fun signOut() {
        accessToken = null
        _state.value = CloudState(status = CloudStatus.SIGNED_OUT)
    }

    /** Raw Supabase REST call. Returns response body; throws on non-2xx. */
    private fun http(method: String, path: String, body: String?, auth: Boolean): String {
        val conn = URL(BuildConfig.SUPABASE_URL.trimEnd('/') + path).openConnection() as HttpURLConnection
        return try {
            conn.requestMethod = method
            conn.connectTimeout = 15_000
            conn.readTimeout = 20_000
            conn.setRequestProperty("apikey", BuildConfig.SUPABASE_KEY)
            conn.setRequestProperty("Content-Type", "application/json")
            if (auth) {
                conn.setRequestProperty(
                    "Authorization",
                    "Bearer ${accessToken ?: error("Not signed in")}"
                )
            }
            if (body != null) {
                conn.doOutput = true
                conn.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
            }
            val code = conn.responseCode
            val stream = if (code in 200..299) conn.inputStream else conn.errorStream
            val text = stream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()
            if (code !in 200..299) {
                error("HTTP $code: ${text.take(300)}")
            }
            text
        } finally {
            conn.disconnect()
        }
    }
}
