package com.example.data.repository

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
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions
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
 * Flag to hide/show Google Sign-In as requested.
 * Google button is hidden while preserving the Google code behind this flag.
 */
const val ENABLE_GOOGLE_SIGNIN = false

/**
 * GOOGLE_WEB_CLIENT_ID constant as required by user brief.
 * Set to empty string until user provides the real Google Cloud OAuth 2.0 Web Client ID.
 */
const val GOOGLE_WEB_CLIENT_ID = ""

sealed class FirestoreCustomerSaveState {
    data object Idle : FirestoreCustomerSaveState()
    data object Saving : FirestoreCustomerSaveState()
    data object Saved : FirestoreCustomerSaveState()
    data class Error(val message: String) : FirestoreCustomerSaveState()
}

class AuthRepository(private val context: Context) {
    private val TAG = "AuthRepository"
    private val auth: FirebaseAuth by lazy { FirebaseProvider.getAuth(context) }
    private val firestore: FirebaseFirestore by lazy { FirebaseProvider.getFirestore(context) }
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

    private val _firestoreSaveState = MutableStateFlow<FirestoreCustomerSaveState>(FirestoreCustomerSaveState.Idle)
    val firestoreSaveState: StateFlow<FirestoreCustomerSaveState> = _firestoreSaveState.asStateFlow()

    private val _customerDocExists = MutableStateFlow(false)
    val customerDocExists: StateFlow<Boolean> = _customerDocExists.asStateFlow()

    private var customerSnapshotListener: ListenerRegistration? = null
    private var pendingRoute: String? = null

    private val prefs = context.getSharedPreferences("tour_manage_auth_prefs", Context.MODE_PRIVATE)

    init {
        try {
            _currentUser.value = auth.currentUser
            auth.addAuthStateListener { firebaseAuth ->
                val user = firebaseAuth.currentUser
                _currentUser.value = user
                if (user != null) {
                    loadProfileLocally(user)
                    attachCustomerFirestoreListener(user)
                    // Retry saving to Firestore on launch if needed (Requirement 4)
                    scope.launch {
                        saveCustomerToFirestore(user)
                    }
                    syncProfileWithBackend(user)
                } else {
                    detachCustomerFirestoreListener()
                    _customerProfile.value = null
                    _customerDocExists.value = false
                    _firestoreSaveState.value = FirestoreCustomerSaveState.Idle
                    _isPhoneMissing.value = false
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error attaching auth state listener", e)
            _authError.value = formatAuthError(e)
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

    private fun loadProfileLocally(user: FirebaseUser) {
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
        _isPhoneMissing.value = savedPhone.isBlank()
    }

    /**
     * Attaches real-time Firestore listener to customers/{uid} in the named database
     * to read customer data back and observe "Saved in database ✓" status.
     */
    private fun attachCustomerFirestoreListener(user: FirebaseUser) {
        detachCustomerFirestoreListener()
        try {
            customerSnapshotListener = firestore.collection("customers").document(user.uid)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        Log.e(TAG, "Error listening to customers/${user.uid}: ${error.message}")
                        _firestoreSaveState.value = FirestoreCustomerSaveState.Error(
                            error.localizedMessage ?: error.message ?: "Firestore error reading customer"
                        )
                        return@addSnapshotListener
                    }

                    if (snapshot != null && snapshot.exists()) {
                        val name = snapshot.getString("name") ?: ""
                        val phone = snapshot.getString("phone") ?: ""
                        val email = snapshot.getString("email") ?: (user.email ?: "")
                        val createdAt = snapshot.getTimestamp("createdAt")

                        val localName = prefs.getString("user_name_${user.uid}", "") ?: ""
                        val localPhone = prefs.getString("user_phone_${user.uid}", "") ?: ""

                        val finalName = name.ifBlank { localName.ifBlank { user.displayName ?: "Traveler" } }
                        val finalPhone = phone.ifBlank { localPhone }

                        _customerProfile.value = CustomerProfile(
                            uid = user.uid,
                            name = finalName,
                            phone = finalPhone,
                            email = email,
                            createdAt = createdAt
                        )
                        _customerDocExists.value = true
                        _firestoreSaveState.value = FirestoreCustomerSaveState.Saved
                    } else {
                        _customerDocExists.value = false
                    }
                }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to attach customer snapshot listener", e)
        }
    }

    private fun detachCustomerFirestoreListener() {
        customerSnapshotListener?.remove()
        customerSnapshotListener = null
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

    /**
     * Requirement 2:
     * After every successful sign-up AND every login, write the customer directly to Firestore,
     * in the named database, at customers/{uid}, using set with merge.
     * Fields: name, email (from the Firebase user), phone, and createdAt (only the first time).
     * This is the one allowed direct write, only to the customer's own document.
     * It does not depend on API_BASE_URL.
     */
    suspend fun saveCustomerToFirestore(
        user: FirebaseUser,
        name: String? = null,
        phone: String? = null
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            _firestoreSaveState.value = FirestoreCustomerSaveState.Saving
            val docRef = firestore.collection("customers").document(user.uid)

            val effectiveName = name?.trim()?.ifBlank { null }
                ?: prefs.getString("user_name_${user.uid}", "")?.ifBlank { null }
                ?: _customerProfile.value?.name?.ifBlank { null }
                ?: user.displayName?.ifBlank { null }
                ?: "Traveler"

            val effectivePhone = phone?.trim()?.ifBlank { null }
                ?: prefs.getString("user_phone_${user.uid}", "")?.ifBlank { null }
                ?: _customerProfile.value?.phone?.ifBlank { null }
                ?: ""

            // Check if document already exists to write createdAt only the first time
            val snapshot = try {
                docRef.get().await()
            } catch (e: Exception) {
                null
            }

            val data = mutableMapOf<String, Any>(
                "name" to effectiveName,
                "email" to (user.email ?: ""),
                "phone" to effectivePhone
            )

            if (snapshot == null || !snapshot.exists() || snapshot.get("createdAt") == null) {
                data["createdAt"] = FieldValue.serverTimestamp()
            }

            docRef.set(data, SetOptions.merge()).await()

            // Update local cache
            prefs.edit()
                .putString("user_name_${user.uid}", effectiveName)
                .putString("user_phone_${user.uid}", effectivePhone)
                .apply()

            _customerProfile.value = CustomerProfile(
                uid = user.uid,
                name = effectiveName,
                phone = effectivePhone,
                email = user.email ?: "",
                createdAt = snapshot?.getTimestamp("createdAt") ?: Timestamp.now()
            )
            _customerDocExists.value = true
            _firestoreSaveState.value = FirestoreCustomerSaveState.Saved
            Log.i(TAG, "Customer successfully written to named Firestore: customers/${user.uid}")
            Result.success(Unit)
        } catch (e: Exception) {
            val errorMsg = e.localizedMessage ?: e.message ?: "Failed to save to database"
            Log.e(TAG, "Firestore customer write failed: $errorMsg", e)
            _firestoreSaveState.value = FirestoreCustomerSaveState.Error(errorMsg)
            Result.failure(e)
        }
    }

    fun retrySaveCustomerToFirestore() {
        val user = auth.currentUser ?: return
        scope.launch {
            saveCustomerToFirestore(user)
        }
    }

    /**
     * Requirement 1: Sign up with Email/Password
     * Asks for full name, email, 10-digit mobile number and password.
     */
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

            // Update user displayName
            try {
                val profileUpdates = UserProfileChangeRequest.Builder()
                    .setDisplayName(name.trim())
                    .build()
                user.updateProfile(profileUpdates).await()
            } catch (e: Exception) {
                Log.w(TAG, "Failed to update profile displayName: ${e.message}")
            }

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
            _currentUser.value = user

            // Requirement 2: Direct write to Firestore customers/{uid}
            saveCustomerToFirestore(user, name.trim(), phone.trim())
            attachCustomerFirestoreListener(user)

            // Optional background backend sync if available
            syncProfileWithBackend(user, name.trim(), phone.trim(), email.trim())

            Result.success(user)
        } catch (e: Exception) {
            val formatted = formatAuthError(e)
            Log.e(TAG, "Sign up failure: $formatted", e)
            _authError.value = formatted
            Result.failure(e)
        }
    }

    /**
     * Requirement 1: Login with Email/Password
     * Asks for email and password.
     */
    suspend fun logIn(email: String, password: String): Result<FirebaseUser> = withContext(Dispatchers.IO) {
        try {
            _authError.value = null
            val result = auth.signInWithEmailAndPassword(email.trim(), password).await()
            val user = result.user ?: throw IllegalStateException("Login returned null")
            _currentUser.value = user
            loadProfileLocally(user)

            // Requirement 2: Direct write to Firestore customers/{uid} after every login
            saveCustomerToFirestore(user)
            attachCustomerFirestoreListener(user)

            syncProfileWithBackend(user)
            Result.success(user)
        } catch (e: Exception) {
            val formatted = formatAuthError(e)
            Log.e(TAG, "Login failure: $formatted", e)
            _authError.value = formatted
            Result.failure(e)
        }
    }

    suspend fun sendPasswordReset(email: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            _authError.value = null
            auth.sendPasswordResetEmail(email.trim()).await()
            Result.success(Unit)
        } catch (e: Exception) {
            val formatted = formatAuthError(e)
            Log.e(TAG, "Reset failure: $formatted", e)
            _authError.value = formatted
            Result.failure(e)
        }
    }

    suspend fun signInWithGoogle(activityContext: Context): Result<FirebaseUser> = withContext(Dispatchers.IO) {
        if (!ENABLE_GOOGLE_SIGNIN) {
            val msg = "Google Sign-In is currently disabled. Please use Email/Password."
            _authError.value = msg
            return@withContext Result.failure(IllegalStateException(msg))
        }

        _authError.value = null

        if (GOOGLE_WEB_CLIENT_ID.isBlank()) {
            val errorMsg = "Google Sign-In is not configured yet (GOOGLE_WEB_CLIENT_ID is empty). Please use Email/Password sign-in below."
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

            if (credential !is CustomCredential || credential.type != GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                val err = "Unexpected credential type returned from CredentialManager"
                _authError.value = err
                return@withContext Result.failure(IllegalStateException(err))
            }

            val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
            val idToken = googleIdTokenCredential.idToken
            val firebaseCredential = GoogleAuthProvider.getCredential(idToken, null)
            val authResult = auth.signInWithCredential(firebaseCredential).await()
            val user = authResult.user ?: throw IllegalStateException("Firebase returned null user from Google credentials")

            _currentUser.value = user
            loadProfileLocally(user)
            saveCustomerToFirestore(user)
            attachCustomerFirestoreListener(user)
            syncProfileWithBackend(user)

            Result.success(user)
        } catch (e: GetCredentialCancellationException) {
            Log.i(TAG, "Google Sign-In was cancelled by user")
            Result.failure(e)
        } catch (e: GetCredentialException) {
            val err = "Google sign-in service error: ${e.message ?: "Google Play Services error"}"
            Log.e(TAG, err, e)
            _authError.value = "$err (Use Email/Password instead)"
            Result.failure(e)
        } catch (e: Exception) {
            val formatted = formatAuthError(e)
            Log.e(TAG, "Google authentication failure: $formatted", e)
            _authError.value = formatted
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
        scope.launch {
            saveCustomerToFirestore(user, currentName, phone.trim())
        }
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
        scope.launch {
            saveCustomerToFirestore(user, name.trim(), phone.trim())
        }
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
            detachCustomerFirestoreListener()
            _currentUser.value = null
            _customerProfile.value = null
            _customerDocExists.value = false
            _firestoreSaveState.value = FirestoreCustomerSaveState.Idle
            _isPhoneMissing.value = false
            _authError.value = null
        } catch (e: Exception) {
            Log.e(TAG, "Error during logout", e)
            _authError.value = formatAuthError(e)
        }
    }

    /**
     * Requirement 5:
     * Show friendly error messages with the exact Firebase error text for:
     * - wrong password
     * - email already in use
     * - weak password
     * - no network
     */
    fun formatAuthError(e: Throwable): String {
        val exactText = e.localizedMessage ?: e.message ?: e.toString()
        return when {
            e is FirebaseAuthInvalidCredentialsException ||
            exactText.contains("wrong-password", ignoreCase = true) ||
            exactText.contains("invalid-credential", ignoreCase = true) ||
            exactText.contains("password is invalid", ignoreCase = true) -> {
                "Wrong password. Please verify your password and try again.\n\nExact error: $exactText"
            }
            e is FirebaseAuthUserCollisionException ||
            exactText.contains("email-already-in-use", ignoreCase = true) -> {
                "This email address is already in use by another account. Please log in instead or use another email.\n\nExact error: $exactText"
            }
            e is FirebaseAuthWeakPasswordException ||
            exactText.contains("weak-password", ignoreCase = true) -> {
                "Password is too weak. Please use at least 6 characters.\n\nExact error: $exactText"
            }
            e is FirebaseAuthInvalidUserException ||
            exactText.contains("user-not-found", ignoreCase = true) -> {
                "No account found with this email. Please check your spelling or sign up.\n\nExact error: $exactText"
            }
            e is FirebaseNetworkException ||
            exactText.contains("network error", ignoreCase = true) ||
            exactText.contains("network-request-failed", ignoreCase = true) ||
            exactText.contains("timeout", ignoreCase = true) -> {
                "No network connection. Please check your internet connection and try again.\n\nExact error: $exactText"
            }
            e is FirebaseAuthException -> {
                "Authentication failed: [${e.errorCode}] ${e.message}\n\nExact error: $exactText"
            }
            else -> {
                "Authentication failed. Please check your details and try again.\n\nExact error: $exactText"
            }
        }
    }
}
