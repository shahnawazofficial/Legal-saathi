package com.example.legalhelpaiapp.utils

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.URL

/**
 * UpdateChecker - Checks for app updates from GitHub
 *
 * How it works:
 * 1. Fetches update_info.json from GitHub
 * 2. Compares with current app version
 * 3. Returns update info if newer version available
 */
class UpdateChecker(private val context: Context) {

    companion object {
        private const val TAG = "UpdateChecker"

        // GitHub raw file URL for update info
        // Format: https://raw.githubusercontent.com/USERNAME/REPO/BRANCH/FILE
        private const val UPDATE_CHECK_URL =
            "https://raw.githubusercontent.com/shahnawazofficial/Legal-saathi/main/update_info.json"

        // Backup URL (in case main fails)
        private const val BACKUP_UPDATE_URL =
            "https://raw.githubusercontent.com/shahnawazofficial/Legal-saathi/master/update_info.json"
    }

    /**
     * Check if update is available
     * Returns UpdateInfo if update available, null otherwise
     */
    suspend fun checkForUpdate(): UpdateInfo? = withContext(Dispatchers.IO) {
        try {
            // Try main URL first
            val updateInfo = fetchUpdateInfo(UPDATE_CHECK_URL)
                ?: fetchUpdateInfo(BACKUP_UPDATE_URL) // Try backup if main fails
                ?: return@withContext null

            // Get current version
            val currentVersionCode = getCurrentVersionCode()

            Log.d(TAG, "Current version: $currentVersionCode")
            Log.d(TAG, "Latest version: ${updateInfo.versionCode}")

            // Check if update available
            if (updateInfo.versionCode > currentVersionCode) {
                // Check if force update required
                if (updateInfo.forceUpdate) {
                    Log.d(TAG, "Force update required!")
                }
                return@withContext updateInfo
            }

            Log.d(TAG, "App is up to date")
            return@withContext null

        } catch (e: Exception) {
            Log.e(TAG, "Error checking for updates", e)
            return@withContext null
        }
    }

    /**
     * Fetch update info from URL
     */
    private fun fetchUpdateInfo(url: String): UpdateInfo? {
        return try {
            val jsonString = URL(url).readText()
            val jsonObject = JSONObject(jsonString)

            UpdateInfo(
                versionName = jsonObject.getString("latestVersion"),
                versionCode = jsonObject.getInt("latestVersionCode"),
                downloadUrl = jsonObject.getString("updateUrl"),
                releaseNotes = jsonObject.getString("releaseNotes"),
                forceUpdate = jsonObject.optBoolean("forceUpdate", false),
                updateTitle = jsonObject.optString("updateTitle", "New Update Available!"),
                updateMessage = jsonObject.optString("updateMessage", "A new version is available."),
                fileSize = jsonObject.optString("fileSize", "Unknown")
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching from $url", e)
            null
        }
    }

    /**
     * Get current app version code
     */
    private fun getCurrentVersionCode(): Int {
        return try {
            val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                packageInfo.longVersionCode.toInt()
            } else {
                @Suppress("DEPRECATION")
                packageInfo.versionCode
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting version code", e)
            1
        }
    }

    /**
     * Get current app version name
     */
    fun getCurrentVersionName(): String {
        return try {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "1.0"
        } catch (e: Exception) {
            "1.0"
        }
    }
}

/**
 * Data class for update information
 */
data class UpdateInfo(
    val versionName: String,
    val versionCode: Int,
    val downloadUrl: String,
    val releaseNotes: String,
    val forceUpdate: Boolean,
    val updateTitle: String,
    val updateMessage: String,
    val fileSize: String
)