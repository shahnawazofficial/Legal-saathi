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
import com.example.legalhelpaiapp.databinding.ActivityDashboardBinding
import com.example.legalhelpaiapp.emergency.EmergencyFragment // ⭐ IMPORT ADDED
import com.example.legalhelpaiapp.ui.chat.ChatFragment
import com.example.legalhelpaiapp.ui.profile.ProfileFragment
import com.example.legalhelpaiapp.ui.resources.ResourcesFragment

class DashboardActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDashboardBinding
    private val REQUEST_CODE_PERMISSIONS = 100

    // ⭐ New: Define the permission requests required by EmergencyFragment
    private val EMERGENCY_PERMISSIONS = arrayOf(
        Manifest.permission.SEND_SMS,
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION,
        Manifest.permission.CALL_PHONE
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityDashboardBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Request storage permission for profile image upload
        requestStoragePermission()

        // ⭐ NEW: Request Emergency Permissions at startup as well for a smoother UX
        requestEmergencyPermissions()

        // Set the default fragment to the Chat screen
        // NOTE: You might want to change this default to the EmergencyFragment for testing
        // if (savedInstanceState == null) {
        //     replaceFragment(EmergencyFragment())
        // } else {
        //     replaceFragment(ChatFragment())
        // }

        if (savedInstanceState == null) {
            replaceFragment(ChatFragment())
        }

        // Handle bottom navigation item clicks
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
                R.id.navigation_emergency -> { // ⭐ LOGIC ADDED HERE
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

    // Function to replace the fragment in the container
    private fun replaceFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.main_content_frame, fragment)
            .commit()
    }

    // Request storage permission for image picker
    private fun requestStoragePermission() {
        // (Existing logic for READ_MEDIA_IMAGES / READ_EXTERNAL_STORAGE)
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

    // ⭐ NEW: Request all permissions needed for the Emergency Fragment
    private fun requestEmergencyPermissions() {
        val permissionsToRequest = EMERGENCY_PERMISSIONS.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }.toTypedArray()

        if (permissionsToRequest.isNotEmpty()) {
            ActivityCompat.requestPermissions(
                this,
                permissionsToRequest,
                REQUEST_CODE_PERMISSIONS + 1 // Use a different request code
            )
        }
    }

    // Handle permission result
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        when (requestCode) {
            REQUEST_CODE_PERMISSIONS -> {
                // (Existing logic for Storage permission result)
                if (grantResults.isNotEmpty() &&
                    grantResults[0] == PackageManager.PERMISSION_GRANTED
                ) {
                    Toast.makeText(this, "Storage permission granted!", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "Storage permission denied. Profile image upload won't work.", Toast.LENGTH_LONG).show()
                }
            }
            // ⭐ NEW: Add a case to handle the Emergency Permissions result
            REQUEST_CODE_PERMISSIONS + 1 -> {
                // Log or handle the emergency permissions if needed,
                // but EmergencyFragment handles its own logic too.
            }
        }
    }
}