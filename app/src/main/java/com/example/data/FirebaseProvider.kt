package com.example.data

import android.content.Context
import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.firebase.firestore.PersistentCacheSettings

/**
 * Global API Base URL constant as required:
 * Customers save data (profile, bookings, chat, callbacks) through this backend API.
 * Defaults to empty string until backend is deployed.
 */
const val API_BASE_URL = ""

object FirebaseProvider {
    private const val TAG = "FirebaseProvider"
    private const val FIRESTORE_DATABASE_ID = "ai-studio-dae5c51a-b0ae-407b-aafd-5180425e30e1"

    private var firebaseApp: FirebaseApp? = null
    private var firestoreInstance: FirebaseFirestore? = null
    private var authInstance: FirebaseAuth? = null

    var lastInitError: String? = null
        private set

    @Synchronized
    fun initialize(context: Context) {
        if (firebaseApp != null) return

        try {
            val existingApps = FirebaseApp.getApps(context)
            firebaseApp = if (existingApps.isNotEmpty()) {
                existingApps[0]
            } else {
                val options = FirebaseOptions.Builder()
                    .setApiKey("AIzaSyCr0F5bj8xzZp_o4BtlAPoy6g1vnXfhAxg")
                    .setApplicationId("1:365774462468:web:2dac12cee1588adc0adf51")
                    .setProjectId("polynomial-path-2vxch")
                    .setStorageBucket("polynomial-path-2vxch.firebasestorage.app")
                    .setGcmSenderId("365774462468")
                    .build()
                FirebaseApp.initializeApp(context.applicationContext, options)
            }

            val app = firebaseApp ?: error("FirebaseApp failed to initialize")

            // Initialize named Firestore database
            val db = FirebaseFirestore.getInstance(app, FIRESTORE_DATABASE_ID)
            try {
                val cacheSettings = PersistentCacheSettings.newBuilder().build()
                val settings = FirebaseFirestoreSettings.Builder()
                    .setLocalCacheSettings(cacheSettings)
                    .build()
                db.firestoreSettings = settings
            } catch (e: Exception) {
                Log.w(TAG, "Cache settings warning: ${e.message}")
            }
            firestoreInstance = db

            // Initialize Auth
            authInstance = FirebaseAuth.getInstance(app)
            lastInitError = null
            Log.i(TAG, "Firebase initialized successfully with named DB $FIRESTORE_DATABASE_ID")
        } catch (e: Exception) {
            Log.e(TAG, "Firebase initialization error", e)
            lastInitError = e.localizedMessage ?: e.message ?: "Failed to initialize Firebase"
        }
    }

    fun getFirestore(context: Context): FirebaseFirestore {
        if (firestoreInstance == null) {
            initialize(context)
        }
        return firestoreInstance ?: error("Firestore not available: $lastInitError")
    }

    fun getAuth(context: Context): FirebaseAuth {
        if (authInstance == null) {
            initialize(context)
        }
        return authInstance ?: error("FirebaseAuth not available: $lastInitError")
    }
}
