package com.example.legalhelpaiapp.utils

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.util.Log
import android.widget.Toast
import androidx.core.content.FileProvider
import java.io.File

/**
 * UpdateManager - Handles downloading and installing APK updates
 *
 * Features:
 * - Downloads APK using Android DownloadManager
 * - Shows progress notification
 * - Auto-installs after download (with user permission)
 * - Handles Android 7+ FileProvider requirements
 */
class UpdateManager(private val context: Context) {

    companion object {
        private const val TAG = "UpdateManager"
        private const val DOWNLOAD_ID_KEY = "update_download_id"
    }

    private var downloadId: Long = -1
    private var onDownloadComplete: ((Boolean) -> Unit)? = null

    /**
     * Download update APK
     */
    fun downloadUpdate(updateInfo: UpdateInfo, onComplete: (Boolean) -> Unit) {
        this.onDownloadComplete = onComplete

        try {
            // Create download request
            val request = DownloadManager.Request(Uri.parse(updateInfo.downloadUrl))
                .setTitle("Legal Saathi Update")
                .setDescription("Downloading v${updateInfo.versionName} (${updateInfo.fileSize})")
                .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE)
                .setDestinationInExternalPublicDir(
                    Environment.DIRECTORY_DOWNLOADS,
                    "LegalSaathi_v${updateInfo.versionName}.apk"
                )
                .setAllowedOverMetered(true)
                .setAllowedOverRoaming(true)

            // Get download manager
            val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager

            // Start download
            downloadId = downloadManager.enqueue(request)

            // Save download ID
            context.getSharedPreferences("update_prefs", Context.MODE_PRIVATE)
                .edit()
                .putLong(DOWNLOAD_ID_KEY, downloadId)
                .apply()

            // Register download complete receiver
            registerDownloadReceiver()

            Toast.makeText(context, "Downloading update...", Toast.LENGTH_SHORT).show()

            Log.d(TAG, "Download started: $downloadId")

        } catch (e: Exception) {
            Log.e(TAG, "Error downloading update", e)
            onComplete(false)
            Toast.makeText(context, "Download failed: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    /**
     * Register broadcast receiver for download completion
     */
    private fun registerDownloadReceiver() {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                val id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1)

                if (id == downloadId) {
                    Log.d(TAG, "Download complete: $id")
                    handleDownloadComplete()
                    context.unregisterReceiver(this)
                }
            }
        }

        context.registerReceiver(
            receiver,
            IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE)
        )
    }

    /**
     * Handle download completion
     */
    private fun handleDownloadComplete() {
        val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager

        val query = DownloadManager.Query().setFilterById(downloadId)
        val cursor: Cursor = downloadManager.query(query)

        if (cursor.moveToFirst()) {
            val statusIndex = cursor.getColumnIndex(DownloadManager.COLUMN_STATUS)
            val status = cursor.getInt(statusIndex)

            when (status) {
                DownloadManager.STATUS_SUCCESSFUL -> {
                    Log.d(TAG, "Download successful")
                    installUpdate(cursor, downloadManager)
                    onDownloadComplete?.invoke(true)
                }
                DownloadManager.STATUS_FAILED -> {
                    Log.e(TAG, "Download failed")
                    onDownloadComplete?.invoke(false)
                    Toast.makeText(context, "Download failed. Please try again.", Toast.LENGTH_LONG).show()
                }
            }
        }
        cursor.close()
    }

    /**
     * Install downloaded APK
     */
    private fun installUpdate(cursor: Cursor, downloadManager: DownloadManager) {
        try {
            val uriIndex = cursor.getColumnIndex(DownloadManager.COLUMN_LOCAL_URI)
            val downloadUri = cursor.getString(uriIndex)

            if (downloadUri != null) {
                val file = File(Uri.parse(downloadUri).path!!)

                if (file.exists()) {
                    val intent = Intent(Intent.ACTION_VIEW)

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                        // Android 7.0+ requires FileProvider
                        val apkUri = FileProvider.getUriForFile(
                            context,
                            "${context.packageName}.provider",
                            file
                        )
                        intent.setDataAndType(apkUri, "application/vnd.android.package-archive")
                        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    } else {
                        // For older Android versions
                        intent.setDataAndType(Uri.fromFile(file), "application/vnd.android.package-archive")
                    }

                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(intent)

                    Log.d(TAG, "Installation started")
                } else {
                    Log.e(TAG, "Downloaded file not found: ${file.absolutePath}")
                    Toast.makeText(context, "Installation file not found", Toast.LENGTH_LONG).show()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error installing update", e)
            Toast.makeText(context, "Installation failed: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    /**
     * Get download progress
     */
    fun getDownloadProgress(): Int {
        if (downloadId == -1L) return 0

        val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        val query = DownloadManager.Query().setFilterById(downloadId)
        val cursor = downloadManager.query(query)

        if (cursor.moveToFirst()) {
            val bytesDownloadedIndex = cursor.getColumnIndex(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR)
            val bytesTotalIndex = cursor.getColumnIndex(DownloadManager.COLUMN_TOTAL_SIZE_BYTES)

            val bytesDownloaded = cursor.getInt(bytesDownloadedIndex)
            val bytesTotal = cursor.getInt(bytesTotalIndex)

            cursor.close()

            return if (bytesTotal > 0) {
                ((bytesDownloaded * 100L) / bytesTotal).toInt()
            } else {
                0
            }
        }
        cursor.close()
        return 0
    }

    /**
     * Cancel download
     */
    fun cancelDownload() {
        if (downloadId != -1L) {
            val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            downloadManager.remove(downloadId)
            downloadId = -1
            Log.d(TAG, "Download cancelled")
        }
    }
}