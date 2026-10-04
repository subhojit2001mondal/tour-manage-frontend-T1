package com.example.data.repository

import android.app.Activity
import android.content.Context
import android.util.Log
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import com.example.data.API_BASE_URL
import com.example.data.FirebaseProvider
import com.example.data.models.CustomerProfile
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
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

/**
 * GOOGLE_WEB_CLIENT_ID constant as required by user brief.
 * Set to empty string until user provides the real Google Cloud OAuth 2.0 Web Client ID.
 */
const val GOOGLE_WEB_CLIENT_ID = ""

class AuthRepository(private val context: Context) {
    private val TAG = "AuthRepository"
    private val auth: FirebaseAuth by lazy { FirebaseProvider.getAuth(context) }
    private val httpClient = OkHttpClient()
    private val scope = CoroutineScope(Dispatchers.IO)

    private val _currentUser = MutableStateFlow<FirebaseUser?>(null)
    val currentUser: StateFlow<FirebaseUser?> = _currentUser.asStateFlow()

    private val _customerProfile = MutableStateFlow<CustomerProfile?>(null)
    val customerProfile: StateFlow<CustomerProfile?> = _customerProfile.asStateFlow()

    private val _isPhoneMissing = MutableStateFlow(false)
    val isPhoneMissing: StateFlow<Boolean> = _isPhoneMissing.asStateFlow()

    private val _authError = MutableStateFlow<String?>(null)
    val authError: StateFlow<String?> = _authError.asStateFlow()

    private var pendingRoute: String? = null

    private val prefs = context.getSharedPreferences("tour_manage_auth_prefs", Context.MODE_PRIVATE)

    init {
        try {
            _currentUser.value = auth.currentUser
            auth.addAuthStateListener { firebaseAuth ->
                val user = firebaseAuth.currentUser
                _currentUser.value = user
                if (user != null) {
                    loadProfile(user)
                    // Sync customer profile with backend API if pending or logged in
                    syncProfileWithBackend(user)
                } else {
                    _customerProfile.value = null
                    _isPhoneMissing.value = false
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error attaching auth state listener", e)
            _authError.value = e.localizedMessage ?: e.message
        }
    }

    fun setPendingRoute(route: String?) {
        pendingRoute = route
    }

    fun consumePendingRoute(): String? {
        val r = pendingRoute
        pendingRoute = null
        return r
    }

    private fun loadProfile(user: FirebaseUser) {
        val savedName = prefs.getString("user_name_${user.uid}", user.displayName ?: "") ?: ""
        val savedPhone = prefs.getString("user_phone_${user.uid}", "") ?: ""
        val effectiveName = savedName.ifBlank { user.displayName ?: "Traveler" }

        _customerProfile.value = CustomerProfile(
            uid = user.uid,
            name = effectiveName,
            phone = savedPhone,
            email = user.email ?: "",
            createdAt = Timestamp.now()
        )

        // Flag if phone number is missing (e.g. from Google login)
        _isPhoneMissing.value = savedPhone.isBlank()
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

    suspend fun signInWithGoogle(activityContext: Context): Result<FirebaseUser> = withContext(Dispatchers.IO) {
        _authError.value = null

        if (GOOGLE_WEB_CLIENT_ID.isBlank()) {
            val errorMsg = "Google Sign-In is not configured yet (GOOGLE_WEB_CLIENT_ID is empty). Please configure GOOGLE_WEB_CLIENT_ID or use Email/Password sign-in below."
            _authError.value = errorMsg
            return@withContext Result.failure(IllegalStateException(errorMsg))
        }

        try {
            val credentialManager = CredentialManager.create(activityContext)
            val googleIdOption = GetGoogleIdOption.Builder()
                .setFilterByAuthorizedAccounts(false)
                .setServerClientId(GOOGLE_WEB_CLIENT_ID)
                .setAutoSelectEnabled(false)
                .build()

            val request = GetCredentialRequest.Builder()
                .addCredentialOption(googleIdOption)
                .build()

            val result = credentialManager.getCredential(activityContext, request)
            val credential = result.credential

            if (credential is CustomCredential && credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                val idToken = googleIdTokenCredential.idToken
                val authCredential = GoogleAuthProvider.getCredential(idToken, null)
                val authResult = auth.signInWithCredential(authCredential).await()
                val user = authResult.user ?: throw IllegalStateException("Google sign-in returned null user")

                _currentUser.value = user
                loadProfile(user)
                Result.success(user)
            } else {
                throw IllegalStateException("Unexpected credential response type from Credential Manager")
            }
        } catch (e: GetCredentialCancellationException) {
            // User cancelled Google picker
            Result.failure(e)
        } catch (e: Exception) {
            val msg = e.localizedMessage ?: e.message ?: "Google Sign-In failed"
            Log.e(TAG, "Google sign-in exception: $msg", e)
            _authError.value = "Google Sign-In Error: $msg"
            Result.failure(e)
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

            // Save phone and name locally
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
            _isPhoneMissing.value = false

            // Post to backend API
            syncProfileWithBackend(user, name.trim(), phone.trim(), email.trim())

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

    fun completeProfilePhone(phone: String) {
        val user = auth.currentUser ?: return
        val currentName = _customerProfile.value?.name ?: user.displayName ?: "Traveler"
        prefs.edit()
            .putString("user_phone_${user.uid}", phone.trim())
            .apply()

        _customerProfile.value = CustomerProfile(
            uid = user.uid,
            name = currentName,
            phone = phone.trim(),
            email = user.email ?: "",
            createdAt = Timestamp.now()
        )
        _isPhoneMissing.value = false
        syncProfileWithBackend(user, currentName, phone.trim(), user.email ?: "")
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
        _isPhoneMissing.value = phone.trim().isBlank()
        syncProfileWithBackend(user, name.trim(), phone.trim(), user.email ?: "")
    }

    private fun syncProfileWithBackend(
        user: FirebaseUser,
        name: String? = null,
        phone: String? = null,
        email: String? = null
    ) {
        if (API_BASE_URL.isBlank()) return

        scope.launch {
            try {
                val token = user.getIdToken(true).await().token
                val finalName = name ?: prefs.getString("user_name_${user.uid}", user.displayName ?: "") ?: ""
                val finalPhone = phone ?: prefs.getString("user_phone_${user.uid}", "") ?: ""
                val finalEmail = email ?: user.email ?: ""

                val json = JSONObject().apply {
                    put("name", finalName)
                    put("phone", finalPhone)
                    put("email", finalEmail)
                }
                val body = json.toString().toRequestBody("application/json".toMediaType())
                val request = Request.Builder()
                    .url("$API_BASE_URL/api/customers/profile")
                    .addHeader("Authorization", "Bearer $token")
                    .post(body)
                    .build()
                val response = httpClient.newCall(request).execute()
                response.close()
            } catch (e: Exception) {
                Log.w(TAG, "Async API profile sync retry scheduled: ${e.message}")
            }
        }
    }

    fun logOut() {
        try {
            auth.signOut()
            _currentUser.value = null
            _customerProfile.value = null
            _isPhoneMissing.value = false
            _authError.value = null
        } catch (e: Exception) {
            Log.e(TAG, "Error during logout", e)
            _authError.value = e.localizedMessage ?: e.message
        }
    }
}
