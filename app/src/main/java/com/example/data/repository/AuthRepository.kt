package com.example.data.repository

import android.content.Context
import android.util.Log
import com.example.data.API_BASE_URL
import com.example.data.FirebaseProvider
import com.example.data.models.CustomerProfile
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.UserProfileChangeRequest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject

class AuthRepository(private val context: Context) {
    private val TAG = "AuthRepository"
    private val auth: FirebaseAuth by lazy { FirebaseProvider.getAuth(context) }
    private val httpClient = OkHttpClient()

    private val _currentUser = MutableStateFlow<FirebaseUser?>(null)
    val currentUser: StateFlow<FirebaseUser?> = _currentUser.asStateFlow()

    private val _customerProfile = MutableStateFlow<CustomerProfile?>(null)
    val customerProfile: StateFlow<CustomerProfile?> = _customerProfile.asStateFlow()

    private val _authError = MutableStateFlow<String?>(null)
    val authError: StateFlow<String?> = _authError.asStateFlow()

    private val prefs = context.getSharedPreferences("tour_manage_auth_prefs", Context.MODE_PRIVATE)

    init {
        try {
            _currentUser.value = auth.currentUser
            auth.addAuthStateListener { firebaseAuth ->
                val user = firebaseAuth.currentUser
                _currentUser.value = user
                if (user != null) {
                    loadProfile(user)
                } else {
                    _customerProfile.value = null
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error attaching auth state listener", e)
            _authError.value = e.localizedMessage ?: e.message
        }
    }

    private fun loadProfile(user: FirebaseUser) {
        val savedName = prefs.getString("user_name_${user.uid}", user.displayName ?: "") ?: ""
        val savedPhone = prefs.getString("user_phone_${user.uid}", "") ?: ""
        _customerProfile.value = CustomerProfile(
            uid = user.uid,
            name = savedName.ifBlank { user.displayName ?: "Traveler" },
            phone = savedPhone,
            email = user.email ?: "",
            createdAt = Timestamp.now()
        )
    }

    fun clearError() {
        _authError.value = null
    }

    suspend fun getIdToken(): String? = withContext(Dispatchers.IO) {
        try {
            val user = auth.currentUser ?: return@withContext null
            val tokenResult = user.getIdToken(false).await()
            tokenResult.token
        } catch (e: Exception) {
            Log.e(TAG, "Error getting ID token: ${e.message}")
            null
        }
    }

    suspend fun signUp(
        name: String,
        email: String,
        phone: String,
        password: String
    ): Result<FirebaseUser> = withContext(Dispatchers.IO) {
        try {
            _authError.value = null
            val result = auth.createUserWithEmailAndPassword(email.trim(), password).await()
            val user = result.user ?: throw IllegalStateException("User creation returned null")

            // Update displayName
            try {
                val profileUpdates = UserProfileChangeRequest.Builder()
                    .setDisplayName(name.trim())
                    .build()
                user.updateProfile(profileUpdates).await()
            } catch (e: Exception) {
                Log.w(TAG, "Failed to update profile name: ${e.message}")
            }

            // Save phone locally
            prefs.edit()
                .putString("user_name_${user.uid}", name.trim())
                .putString("user_phone_${user.uid}", phone.trim())
                .apply()

            _customerProfile.value = CustomerProfile(
                uid = user.uid,
                name = name.trim(),
                phone = phone.trim(),
                email = user.email ?: email.trim(),
                createdAt = Timestamp.now()
            )

            // Post to backend API if configured
            if (API_BASE_URL.isNotBlank()) {
                val token = user.getIdToken(true).await().token
                val json = JSONObject().apply {
                    put("name", name.trim())
                    put("phone", phone.trim())
                    put("email", email.trim())
                }
                val body = json.toString().toRequestBody("application/json".toMediaType())
                val request = Request.Builder()
                    .url("$API_BASE_URL/api/customers/profile")
                    .addHeader("Authorization", "Bearer $token")
                    .post(body)
                    .build()
                try {
                    httpClient.newCall(request).execute().close()
                } catch (apiEx: Exception) {
                    Log.w(TAG, "API profile sync error: ${apiEx.message}")
                }
            }

            _currentUser.value = user
            Result.success(user)
        } catch (e: Exception) {
            val msg = e.localizedMessage ?: e.message ?: "Registration failed"
            Log.e(TAG, "Sign up failure: $msg", e)
            _authError.value = msg
            Result.failure(e)
        }
    }

    suspend fun logIn(email: String, password: String): Result<FirebaseUser> = withContext(Dispatchers.IO) {
        try {
            _authError.value = null
            val result = auth.signInWithEmailAndPassword(email.trim(), password).await()
            val user = result.user ?: throw IllegalStateException("Login returned null")
            _currentUser.value = user
            loadProfile(user)
            Result.success(user)
        } catch (e: Exception) {
            val msg = e.localizedMessage ?: e.message ?: "Login failed"
            Log.e(TAG, "Login failure: $msg", e)
            _authError.value = msg
            Result.failure(e)
        }
    }

    suspend fun sendPasswordReset(email: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            _authError.value = null
            auth.sendPasswordResetEmail(email.trim()).await()
            Result.success(Unit)
        } catch (e: Exception) {
            val msg = e.localizedMessage ?: e.message ?: "Failed to send reset email"
            Log.e(TAG, "Reset failure: $msg", e)
            _authError.value = msg
            Result.failure(e)
        }
    }

    fun updateProfileInfo(name: String, phone: String) {
        val user = auth.currentUser ?: return
        prefs.edit()
            .putString("user_name_${user.uid}", name.trim())
            .putString("user_phone_${user.uid}", phone.trim())
            .apply()
        _customerProfile.value = CustomerProfile(
            uid = user.uid,
            name = name.trim(),
            phone = phone.trim(),
            email = user.email ?: "",
            createdAt = Timestamp.now()
        )
    }

    fun logOut() {
        try {
            auth.signOut()
            _currentUser.value = null
            _customerProfile.value = null
            _authError.value = null
        } catch (e: Exception) {
            Log.e(TAG, "Error during logout", e)
            _authError.value = e.localizedMessage ?: e.message
        }
    }
}
