package com.example.data.update

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import com.example.BuildConfig
import com.google.android.play.core.appupdate.AppUpdateInfo
import com.google.android.play.core.appupdate.AppUpdateManager
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.appupdate.AppUpdateOptions
import com.google.android.play.core.install.InstallStateUpdatedListener
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.InstallStatus
import com.google.android.play.core.install.model.UpdateAvailability
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * UPDATE_MODE constant at the top of the code as requested.
 * Switch between DEMO (for local simulation) and PLAY (for Google Play Store release).
 */
enum class UpdateMode {
    DEMO,
    PLAY
}

val UPDATE_MODE = UpdateMode.DEMO

const val PLAY_STORE_PACKAGE_NAME = "com.tourmanage.app"

sealed class UpdateStatus {
    data object Idle : UpdateStatus()
    data object Checking : UpdateStatus()
    data class UpdateAvailable(
        val newVersionName: String,
        val newVersionCode: Int,
        val releaseNotes: List<String>,
        val isImmediate: Boolean = false,
        val appUpdateInfo: Any? = null
    ) : UpdateStatus()
    data class Downloading(
        val bytesDownloaded: Long,
        val totalBytesToDownload: Long,
        val progressPercent: Int
    ) : UpdateStatus()
    data object Downloaded : UpdateStatus()
    data object UpToDate : UpdateStatus()
    data class Error(val message: String, val canOpenPlayStore: Boolean = true) : UpdateStatus()
}

interface UpdateManager {
    val status: StateFlow<UpdateStatus>
    val lastCheckedTime: StateFlow<Long>
    val isUpdateAvailable: StateFlow<Boolean>
    val mode: UpdateMode

    fun checkForUpdate(isManual: Boolean = true, onUpdateAvailable: (() -> Unit)? = null)
    fun startUpdate(activity: ComponentActivity, launcher: ActivityResultLauncher<IntentSenderRequest>? = null)
    fun completeUpdate()
    fun resetDemo()
    fun onResume(activity: ComponentActivity)
    fun checkDailyUpdate(onUpdateAvailable: (() -> Unit)? = null)
}

/**
 * Open the app page on Google Play Store with market:// URI and web fallback.
 */
fun openPlayStore(context: Context) {
    try {
        val marketIntent = Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$PLAY_STORE_PACKAGE_NAME")).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(marketIntent)
    } catch (e: Exception) {
        val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=$PLAY_STORE_PACKAGE_NAME")).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        try {
            context.startActivity(webIntent)
        } catch (ex: Exception) {
            Log.e("UpdateManager", "Failed to open Play Store", ex)
        }
    }
}

/**
 * DEMO MODE Implementation:
 * - Shows a visible "Demo mode" label.
 * - Simulates checking, update available (v1.1.0 demo), simulated download progress, and restart.
 * - Does not download or install any files.
 * - Includes a reset button.
 */
class DemoUpdateManager(private val context: Context) : UpdateManager {

    private val prefs = context.getSharedPreferences("tour_update_prefs", Context.MODE_PRIVATE)
    private val scope = CoroutineScope(Dispatchers.Main)

    private val _status = MutableStateFlow<UpdateStatus>(UpdateStatus.Idle)
    override val status: StateFlow<UpdateStatus> = _status.asStateFlow()

    private val _lastCheckedTime = MutableStateFlow(prefs.getLong("demo_last_checked", 0L))
    override val lastCheckedTime: StateFlow<Long> = _lastCheckedTime.asStateFlow()

    private val _isUpdateAvailable = MutableStateFlow(prefs.getBoolean("demo_has_update", false))
    override val isUpdateAvailable: StateFlow<Boolean> = _isUpdateAvailable.asStateFlow()

    override val mode: UpdateMode = UpdateMode.DEMO

    private var downloadJob: Job? = null

    init {
        // If an update was previously marked as available and not yet completed
        if (_isUpdateAvailable.value) {
            _status.value = UpdateStatus.UpdateAvailable(
                newVersionName = "1.1.0",
                newVersionCode = 2,
                releaseNotes = listOf(
                    "Smart Comparison Center: Compare travel operators and destinations side-by-side with sticky specs.",
                    "On-device Demo Mode: Full browsing, simulated bookings (DEMO-XXXX), and cached support assistant.",
                    "Instant Day/Night Theming: Seamless switching between System, Light, and Dark modes without restarting."
                )
            )
        }
    }

    override fun checkForUpdate(isManual: Boolean, onUpdateAvailable: (() -> Unit)?) {
        scope.launch {
            _status.value = UpdateStatus.Checking
            delay(850L) // Short realistic check delay

            val now = System.currentTimeMillis()
            _lastCheckedTime.value = now
            prefs.edit().putLong("demo_last_checked", now).apply()

            val demoNotes = listOf(
                "Smart Comparison Center: Compare travel operators and destinations side-by-side with sticky specs.",
                "On-device Demo Mode: Full browsing, simulated bookings (DEMO-XXXX), and cached support assistant.",
                "Instant Day/Night Theming: Seamless switching between System, Light, and Dark modes without restarting."
            )

            _status.value = UpdateStatus.UpdateAvailable(
                newVersionName = "1.1.0",
                newVersionCode = 2,
                releaseNotes = demoNotes
            )
            _isUpdateAvailable.value = true
            prefs.edit().putBoolean("demo_has_update", true).apply()

            onUpdateAvailable?.invoke()
        }
    }

    override fun startUpdate(activity: ComponentActivity, launcher: ActivityResultLauncher<IntentSenderRequest>?) {
        downloadJob?.cancel()
        downloadJob = scope.launch {
            val totalBytes = 18_500_000L // 18.5 MB simulated size
            val steps = 20
            for (i in 1..steps) {
                delay(120L)
                val percent = (i * 100) / steps
                val downloaded = (totalBytes * percent) / 100
                _status.value = UpdateStatus.Downloading(
                    bytesDownloaded = downloaded,
                    totalBytesToDownload = totalBytes,
                    progressPercent = percent
                )
            }
            delay(200L)
            _status.value = UpdateStatus.Downloaded
        }
    }

    override fun completeUpdate() {
        scope.launch {
            _status.value = UpdateStatus.UpToDate
            _isUpdateAvailable.value = false
            prefs.edit().putBoolean("demo_has_update", false).apply()
        }
    }

    override fun resetDemo() {
        downloadJob?.cancel()
        _status.value = UpdateStatus.Idle
        _isUpdateAvailable.value = false
        prefs.edit().putBoolean("demo_has_update", false).apply()
    }

    override fun onResume(activity: ComponentActivity) {
        // In demo mode, maintain current state
    }

    override fun checkDailyUpdate(onUpdateAvailable: (() -> Unit)?) {
        val last = _lastCheckedTime.value
        val now = System.currentTimeMillis()
        val oneDayMs = 24 * 60 * 60 * 1000L
        if (last == 0L || (now - last) >= oneDayMs) {
            checkForUpdate(isManual = false, onUpdateAvailable = onUpdateAvailable)
        }
    }
}

/**
 * PLAY MODE Implementation:
 * - Uses Google Play In-App Updates library (com.google.android.play:app-update).
 * - Checks AppUpdateInfo for update availability and version code.
 * - Flexible update by default with InstallStateUpdatedListener and completeUpdate.
 * - Immediate update when update priority >= 4.
 * - Handles onResume to resume in-progress or downloaded updates.
 * - Falls back to Play Store link if check fails or app not installed from Play.
 */
class PlayUpdateManager(private val context: Context) : UpdateManager {

    private val TAG = "PlayUpdateManager"
    private val prefs = context.getSharedPreferences("tour_update_prefs", Context.MODE_PRIVATE)
    private val appUpdateManager: AppUpdateManager by lazy { AppUpdateManagerFactory.create(context) }
    private val scope = CoroutineScope(Dispatchers.Main)

    private val _status = MutableStateFlow<UpdateStatus>(UpdateStatus.Idle)
    override val status: StateFlow<UpdateStatus> = _status.asStateFlow()

    private val _lastCheckedTime = MutableStateFlow(prefs.getLong("play_last_checked", 0L))
    override val lastCheckedTime: StateFlow<Long> = _lastCheckedTime.asStateFlow()

    private val _isUpdateAvailable = MutableStateFlow(false)
    override val isUpdateAvailable: StateFlow<Boolean> = _isUpdateAvailable.asStateFlow()

    override val mode: UpdateMode = UpdateMode.PLAY

    private val installListener = InstallStateUpdatedListener { state ->
        when (state.installStatus()) {
            InstallStatus.DOWNLOADING -> {
                val bytes = state.bytesDownloaded()
                val total = state.totalBytesToDownload()
                val percent = if (total > 0) ((bytes * 100) / total).toInt() else 0
                _status.value = UpdateStatus.Downloading(
                    bytesDownloaded = bytes,
                    totalBytesToDownload = total,
                    progressPercent = percent
                )
            }
            InstallStatus.DOWNLOADED -> {
                _status.value = UpdateStatus.Downloaded
            }
            InstallStatus.INSTALLED -> {
                _status.value = UpdateStatus.UpToDate
                _isUpdateAvailable.value = false
            }
            InstallStatus.FAILED -> {
                _status.value = UpdateStatus.Error("Google Play update download failed. Please update from Play Store.")
            }
            InstallStatus.CANCELED -> {
                _status.value = UpdateStatus.Idle
            }
            else -> {
                Log.d(TAG, "Install status: ${state.installStatus()}")
            }
        }
    }

    init {
        try {
            appUpdateManager.registerListener(installListener)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to register Play install listener", e)
        }
    }

    override fun checkForUpdate(isManual: Boolean, onUpdateAvailable: (() -> Unit)?) {
        _status.value = UpdateStatus.Checking

        try {
            val appUpdateInfoTask = appUpdateManager.appUpdateInfo
            appUpdateInfoTask.addOnSuccessListener { appUpdateInfo ->
                val now = System.currentTimeMillis()
                _lastCheckedTime.value = now
                prefs.edit().putLong("play_last_checked", now).apply()

                if (appUpdateInfo.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE) {
                    val isImmediate = appUpdateInfo.updatePriority() >= 4
                    val newVersionCode = appUpdateInfo.availableVersionCode()
                    _isUpdateAvailable.value = true
                    _status.value = UpdateStatus.UpdateAvailable(
                        newVersionName = "v$newVersionCode",
                        newVersionCode = newVersionCode,
                        releaseNotes = listOf(
                            "Latest performance enhancements and speed optimizations",
                            "Tour package security and checkout stability updates",
                            "Resolved recent connectivity and layout issues"
                        ),
                        isImmediate = isImmediate,
                        appUpdateInfo = appUpdateInfo
                    )
                    onUpdateAvailable?.invoke()
                } else if (appUpdateInfo.updateAvailability() == UpdateAvailability.UPDATE_NOT_AVAILABLE) {
                    _isUpdateAvailable.value = false
                    _status.value = UpdateStatus.UpToDate
                } else {
                    _isUpdateAvailable.value = false
                    _status.value = UpdateStatus.UpToDate
                }
            }.addOnFailureListener { e ->
                Log.w(TAG, "Google Play update check failed", e)
                val msg = if (isManual) {
                    "Google Play In-App Update is available when installed via Google Play Store. You can check directly on Google Play."
                } else {
                    "Unable to check Google Play updates."
                }
                _status.value = UpdateStatus.Error(msg, canOpenPlayStore = true)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception checking update", e)
            _status.value = UpdateStatus.Error("Play Store check encountered an error: ${e.message}", canOpenPlayStore = true)
        }
    }

    override fun startUpdate(activity: ComponentActivity, launcher: ActivityResultLauncher<IntentSenderRequest>?) {
        val curr = _status.value
        if (curr !is UpdateStatus.UpdateAvailable || curr.appUpdateInfo !is AppUpdateInfo) {
            openPlayStore(activity)
            return
        }

        val appUpdateInfo = curr.appUpdateInfo
        val updateType = if (curr.isImmediate) AppUpdateType.IMMEDIATE else AppUpdateType.FLEXIBLE

        try {
            if (launcher != null) {
                appUpdateManager.startUpdateFlowForResult(
                    appUpdateInfo,
                    launcher,
                    AppUpdateOptions.defaultOptions(updateType)
                )
            } else {
                @Suppress("DEPRECATION")
                appUpdateManager.startUpdateFlowForResult(
                    appUpdateInfo,
                    updateType,
                    activity,
                    9001
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start update flow", e)
            openPlayStore(activity)
        }
    }

    override fun completeUpdate() {
        try {
            appUpdateManager.completeUpdate()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to complete update", e)
        }
    }

    override fun resetDemo() {
        // Play mode has no demo reset, but clear error if any
        if (_status.value is UpdateStatus.Error) {
            _status.value = UpdateStatus.Idle
        }
    }

    override fun onResume(activity: ComponentActivity) {
        try {
            appUpdateManager.appUpdateInfo.addOnSuccessListener { appUpdateInfo ->
                if (appUpdateInfo.installStatus() == InstallStatus.DOWNLOADED) {
                    _status.value = UpdateStatus.Downloaded
                } else if (appUpdateInfo.updateAvailability() == UpdateAvailability.DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS) {
                    // Resume immediate update
                    try {
                        @Suppress("DEPRECATION")
                        appUpdateManager.startUpdateFlowForResult(
                            appUpdateInfo,
                            AppUpdateType.IMMEDIATE,
                            activity,
                            9001
                        )
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to resume immediate update", e)
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "onResume error in PlayUpdateManager", e)
        }
    }

    override fun checkDailyUpdate(onUpdateAvailable: (() -> Unit)?) {
        val last = _lastCheckedTime.value
        val now = System.currentTimeMillis()
        val oneDayMs = 24 * 60 * 60 * 1000L
        if (last == 0L || (now - last) >= oneDayMs) {
            checkForUpdate(isManual = false, onUpdateAvailable = onUpdateAvailable)
        }
    }
}

/**
 * Factory creating the active UpdateManager based on UPDATE_MODE.
 */
fun createUpdateManager(context: Context): UpdateManager {
    return when (UPDATE_MODE) {
        UpdateMode.DEMO -> DemoUpdateManager(context)
        UpdateMode.PLAY -> PlayUpdateManager(context)
    }
}
