package com.example.legalhelpaiapp

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.legalhelpaiapp.databinding.ActivityDashboardBinding
import com.example.legalhelpaiapp.emergency.EmergencyFragment // ⭐ Your existing import
import com.example.legalhelpaiapp.ui.chat.ChatFragment
import com.example.legalhelpaiapp.ui.profile.ProfileFragment
import com.example.legalhelpaiapp.ui.resources.ResourcesFragment
// ⭐ NEW: Import update system classes
import com.example.legalhelpaiapp.utils.UpdateChecker
import com.example.legalhelpaiapp.utils.UpdateDialog
import com.example.legalhelpaiapp.utils.UpdateManager
import kotlinx.coroutines.launch

class DashboardActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDashboardBinding
    private val REQUEST_CODE_PERMISSIONS = 100
    private val REQUEST_CODE_INSTALL_PERMISSION = 102 // ⭐ NEW: For update installation

    // Your existing emergency permissions
    private val EMERGENCY_PERMISSIONS = arrayOf(
        Manifest.permission.SEND_SMS,
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION,
        Manifest.permission.CALL_PHONE
    )

    // ⭐ NEW: Update system variables
    private lateinit var updateChecker: UpdateChecker
    private lateinit var updateManager: UpdateManager
    private var updateDialog: UpdateDialog? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityDashboardBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // ⭐ NEW: Initialize update system
        updateChecker = UpdateChecker(this)
        updateManager = UpdateManager(this)

        // Your existing permission requests
        requestStoragePermission()
        requestEmergencyPermissions()

        // ⭐ NEW: Check for app updates (runs in background, doesn't block UI)
        checkForUpdates()

        // Your existing fragment setup
        if (savedInstanceState == null) {
            replaceFragment(ChatFragment())
        }

        // Your existing bottom navigation (unchanged)
        binding.bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.navigation_chat -> {
                    replaceFragment(ChatFragment())
                    true
                }
                R.id.navigation_resources -> {
                    replaceFragment(ResourcesFragment())
                    true
                }
                R.id.navigation_emergency -> {
                    replaceFragment(EmergencyFragment())
                    true
                }
                R.id.navigation_profile -> {
                    replaceFragment(ProfileFragment())
                    true
                }
                else -> false
            }
        }
    }

    // Your existing fragment replacement (unchanged)
    private fun replaceFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.main_content_frame, fragment)
            .commit()
    }

    // ⭐ NEW: Check for app updates
    private fun checkForUpdates() {
        lifecycleScope.launch {
            try {
                val updateInfo = updateChecker.checkForUpdate()

                if (updateInfo != null) {
                    // Update available - show dialog
                    showUpdateDialog(updateInfo)
                }
                // If no update, do nothing (silent)
            } catch (e: Exception) {
                // Failed to check - fail silently, don't bother user
                e.printStackTrace()
            }
        }
    }

    // ⭐ NEW: Show beautiful update dialog
    private fun showUpdateDialog(updateInfo: com.example.legalhelpaiapp.utils.UpdateInfo) {
        // Don't show if already showing
        if (updateDialog?.isShowing() == true) {
            return
        }

        updateDialog = UpdateDialog(
            context = this,
            updateInfo = updateInfo,
            onUpdateClick = {
                // User wants to update
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    // Android 8+ needs install permission
                    if (!packageManager.canRequestPackageInstalls()) {
                        requestInstallPermission()
                    } else {
                        startUpdateDownload(updateInfo)
                    }
                } else {
                    // Android 7 and below - direct download
                    startUpdateDownload(updateInfo)
                }
            },
            onLaterClick = {
                // User clicked "Later" - do nothing
                Toast.makeText(this, "You can update later", Toast.LENGTH_SHORT).show()
            }
        )

        updateDialog?.show()
    }

    // ⭐ NEW: Start downloading update
    private fun startUpdateDownload(updateInfo: com.example.legalhelpaiapp.utils.UpdateInfo) {
        updateManager.downloadUpdate(updateInfo) { success ->
            runOnUiThread {
                if (success) {
                    Toast.makeText(this, "Update downloaded! Installing...", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "Update download failed. Try again later.", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    // ⭐ NEW: Request install permission (Android 8+)
    private fun requestInstallPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val intent = android.content.Intent(android.provider.Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES)
            intent.data = android.net.Uri.parse("package:$packageName")
            startActivityForResult(intent, REQUEST_CODE_INSTALL_PERMISSION)
        }
    }

    // Your existing storage permission request (unchanged)
    private fun requestStoragePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.READ_MEDIA_IMAGES
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.READ_MEDIA_IMAGES),
                    REQUEST_CODE_PERMISSIONS
                )
            }
        } else {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.READ_EXTERNAL_STORAGE
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),
                    REQUEST_CODE_PERMISSIONS
                )
            }
        }
    }

    // Your existing emergency permissions request (unchanged)
    private fun requestEmergencyPermissions() {
        val permissionsToRequest = EMERGENCY_PERMISSIONS.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }.toTypedArray()

        if (permissionsToRequest.isNotEmpty()) {
            ActivityCompat.requestPermissions(
                this,
                permissionsToRequest,
                REQUEST_CODE_PERMISSIONS + 1
            )
        }
    }

    // Your existing permission result handler (unchanged)
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        when (requestCode) {
            REQUEST_CODE_PERMISSIONS -> {
                if (grantResults.isNotEmpty() &&
                    grantResults[0] == PackageManager.PERMISSION_GRANTED
                ) {
                    Toast.makeText(this, "Storage permission granted!", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "Storage permission denied. Profile image upload won't work.", Toast.LENGTH_LONG).show()
                }
            }
            REQUEST_CODE_PERMISSIONS + 1 -> {
                // Emergency permissions handled by EmergencyFragment
            }
        }
    }

    // ⭐ NEW: Handle activity results (for install permission)
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: android.content.Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == REQUEST_CODE_INSTALL_PERMISSION) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                if (packageManager.canRequestPackageInstalls()) {
                    // Permission granted - check for updates again to start download
                    Toast.makeText(this, "Permission granted! Checking for updates...", Toast.LENGTH_SHORT).show()
                    checkForUpdates()
                } else {
                    Toast.makeText(this, "Install permission denied. Cannot install updates.", Toast.LENGTH_LONG).show()
                }
            }
        }
    }
}