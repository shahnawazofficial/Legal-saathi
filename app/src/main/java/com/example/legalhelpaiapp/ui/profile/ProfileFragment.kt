package com.example.legalhelpaiapp.ui.profile

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatDelegate
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.example.legalhelpaiapp.R
import com.example.legalhelpaiapp.SplashActivity
import com.example.legalhelpaiapp.databinding.FragmentProfileBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import java.util.UUID

class ProfileFragment : Fragment() {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!
    private lateinit var auth: FirebaseAuth
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var storageReference: StorageReference
    private var selectedImageUri: Uri? = null

    // Image picker launcher
    private lateinit var imagePickerLauncher: ActivityResultLauncher<Intent>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize image picker
        imagePickerLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val data: Intent? = result.data
                selectedImageUri = data?.data
                selectedImageUri?.let { uri ->
                    // Display selected image
                    binding.profileImage.setImageURI(uri)
                    // Upload to Firebase Storage
                    uploadImageToFirebase(uri)
                }
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        auth = FirebaseAuth.getInstance()
        storageReference = FirebaseStorage.getInstance().reference
        sharedPreferences = requireActivity().getSharedPreferences("AppSettings", Context.MODE_PRIVATE)

        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupUserInfo()
        setupClickListeners()
        loadDarkModePreference()
    }

    private fun setupUserInfo() {
        val currentUser = auth.currentUser
        if (currentUser != null) {
            // Set user name
            val displayName = currentUser.displayName
            binding.userNameText.text = if (!displayName.isNullOrEmpty()) {
                displayName
            } else {
                "User Name"
            }

            // Set user email
            binding.userEmailText.text = currentUser.email ?: "No Email"

            // Load profile image if available
            currentUser.photoUrl?.let { photoUrl ->
                Glide.with(this)
                    .load(photoUrl)
                    .placeholder(R.drawable.ic_person)
                    .circleCrop()
                    .into(binding.profileImage)
            }
        }
    }

    private fun setupClickListeners() {
        // Back button - Go back to previous screen
        binding.backButton.setOnClickListener {
            requireActivity().onBackPressed()
        }

        // Menu button - Show options menu
        binding.menuButton.setOnClickListener {
            showOptionsMenu()
        }

        // Edit profile button - Open image picker
        binding.editProfileButton.setOnClickListener {
            openImagePicker()
        }

        // Personal Data - Show user personal information
        binding.personalDataItem.setOnClickListener {
            showPersonalDataDialog()
        }

        // Settings - Navigate to settings or show settings dialog
        binding.settingsItem.setOnClickListener {
            showSettingsDialog()
        }

        // Help Center - Show help information
        binding.helpCenterItem.setOnClickListener {
            showHelpCenterDialog()
        }

        // Dark Mode Switch - FIXED: Remove recreate()
        binding.darkModeSwitch.setOnCheckedChangeListener { _, isChecked ->
            saveDarkModePreference(isChecked)
            applyDarkMode(isChecked)
        }

        // Info App - Show app information
        binding.infoAppItem.setOnClickListener {
            showInfoAppDialog()
        }

        // Terms and Conditions - Show terms
        binding.termsItem.setOnClickListener {
            showTermsAndConditionsDialog()
        }

        // Logout Button
        binding.logoutButton.setOnClickListener {
            showLogoutConfirmationDialog()
        }
    }

    // ========== Image Picker and Upload ==========

    private fun openImagePicker() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        intent.type = "image/*"
        imagePickerLauncher.launch(intent)
    }

    private fun uploadImageToFirebase(imageUri: Uri) {
        val currentUser = auth.currentUser ?: return

        // Show loading
        showToast("Uploading image...")

        // Create a reference to store the image
        val imageRef = storageReference.child("profile_images/${currentUser.uid}/${UUID.randomUUID()}.jpg")

        // Upload image
        imageRef.putFile(imageUri)
            .addOnSuccessListener { taskSnapshot ->
                // Get download URL
                imageRef.downloadUrl.addOnSuccessListener { uri ->
                    updateUserProfileImage(uri)
                }
            }
            .addOnFailureListener { exception ->
                showToast("Failed to upload image: ${exception.message}")
            }
    }

    private fun updateUserProfileImage(imageUri: Uri) {
        val currentUser = auth.currentUser ?: return

        val profileUpdates = UserProfileChangeRequest.Builder()
            .setPhotoUri(imageUri)
            .build()

        currentUser.updateProfile(profileUpdates)
            .addOnSuccessListener {
                showToast("Profile image updated successfully!")
            }
            .addOnFailureListener { exception ->
                showToast("Failed to update profile: ${exception.message}")
            }
    }

    // ========== Personal Data Dialog ==========

    private fun showPersonalDataDialog() {
        val currentUser = auth.currentUser

        val message = """
            📧 Email: ${currentUser?.email ?: "Not available"}
            
            👤 Display Name: ${currentUser?.displayName ?: "Not set"}
            
            📱 Phone: ${currentUser?.phoneNumber ?: "Not set"}
            
            🔐 User ID: ${currentUser?.uid ?: "Not available"}
            
            ✅ Email Verified: ${if (currentUser?.isEmailVerified == true) "Yes" else "No"}
            
            📅 Account Created: ${currentUser?.metadata?.creationTimestamp?.let {
            java.text.SimpleDateFormat("MMM dd, yyyy", java.util.Locale.getDefault())
                .format(java.util.Date(it))
        } ?: "Unknown"}
        """.trimIndent()

        AlertDialog.Builder(requireContext())
            .setTitle("Personal Data")
            .setIcon(R.drawable.ic_person)
            .setMessage(message)
            .setPositiveButton("Edit") { dialog, _ ->
                showEditNameDialog()
                dialog.dismiss()
            }
            .setNegativeButton("Close", null)
            .show()
    }

    private fun showEditNameDialog() {
        val input = android.widget.EditText(requireContext())
        input.hint = "Enter your name"
        input.setText(auth.currentUser?.displayName ?: "")

        AlertDialog.Builder(requireContext())
            .setTitle("Edit Display Name")
            .setView(input)
            .setPositiveButton("Save") { dialog, _ ->
                val newName = input.text.toString().trim()
                if (newName.isNotEmpty()) {
                    updateDisplayName(newName)
                }
                dialog.dismiss()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun updateDisplayName(newName: String) {
        val currentUser = auth.currentUser ?: return

        val profileUpdates = UserProfileChangeRequest.Builder()
            .setDisplayName(newName)
            .build()

        currentUser.updateProfile(profileUpdates)
            .addOnSuccessListener {
                binding.userNameText.text = newName
                showToast("Name updated successfully!")
            }
            .addOnFailureListener { exception ->
                showToast("Failed to update name: ${exception.message}")
            }
    }

    // ========== Settings Dialog ==========

    private fun showSettingsDialog() {
        val settings = arrayOf(
            "🔔 Notification Settings",
            "🔒 Privacy Settings",
            "🌐 Language",
            "📊 Data Usage",
            "🔄 Clear Cache"
        )

        AlertDialog.Builder(requireContext())
            .setTitle("Settings")
            .setIcon(R.drawable.ic_settings)
            .setItems(settings) { dialog, which ->
                when (which) {
                    0 -> showToast("Notification Settings - Coming Soon!")
                    1 -> showToast("Privacy Settings - Coming Soon!")
                    2 -> showLanguageDialog()
                    3 -> showToast("Data Usage - Coming Soon!")
                    4 -> clearCache()
                }
                dialog.dismiss()
            }
            .setNegativeButton("Close", null)
            .show()
    }

    private fun showLanguageDialog() {
        val languages = arrayOf("English", "Spanish", "French", "German", "Hindi")

        AlertDialog.Builder(requireContext())
            .setTitle("Select Language")
            .setItems(languages) { dialog, which ->
                showToast("Language changed to: ${languages[which]}")
                dialog.dismiss()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun clearCache() {
        try {
            val cache = requireActivity().cacheDir
            cache.deleteRecursively()
            showToast("Cache cleared successfully!")
        } catch (e: Exception) {
            showToast("Failed to clear cache: ${e.message}")
        }
    }

    // ========== Help Center Dialog ==========

    private fun showHelpCenterDialog() {
        val helpOptions = arrayOf(
            "📖 FAQ",
            "💬 Contact Support",
            "🎥 Video Tutorials",
            "📝 Report a Problem",
            "⭐ Rate Our App"
        )

        AlertDialog.Builder(requireContext())
            .setTitle("Help Center")
            .setIcon(R.drawable.ic_help)
            .setItems(helpOptions) { dialog, which ->
                when (which) {
                    0 -> showFAQDialog()
                    1 -> contactSupport()
                    2 -> showToast("Video Tutorials - Coming Soon!")
                    3 -> showReportProblemDialog()
                    4 -> rateApp()
                }
                dialog.dismiss()
            }
            .setNegativeButton("Close", null)
            .show()
    }

    private fun showFAQDialog() {
        val faq = """
            ❓ How do I use the AI Chat?
            → Go to the Chat tab and start typing your legal questions.
            
            ❓ Is my data secure?
            → Yes, we use Firebase authentication and encryption.
            
            ❓ How do I reset my password?
            → Go to Login screen and click "Forgot Password".
            
            ❓ Can I delete my account?
            → Yes, contact support for account deletion.
            
            ❓ How do I access legal resources?
            → Navigate to the Resources tab for legal documents and guides.
        """.trimIndent()

        AlertDialog.Builder(requireContext())
            .setTitle("Frequently Asked Questions")
            .setMessage(faq)
            .setPositiveButton("OK", null)
            .show()
    }

    private fun contactSupport() {
        val intent = Intent(Intent.ACTION_SENDTO).apply {
            data = Uri.parse("mailto:support@legalhelpai.com")
            putExtra(Intent.EXTRA_SUBJECT, "Support Request - Legal Help AI")
            putExtra(Intent.EXTRA_TEXT, "Hello Support Team,\n\nI need help with:\n\n")
        }

        try {
            startActivity(Intent.createChooser(intent, "Send Email"))
        } catch (e: Exception) {
            showToast("No email app found")
        }
    }

    private fun showReportProblemDialog() {
        val input = android.widget.EditText(requireContext())
        input.hint = "Describe the problem..."
        input.minLines = 3

        AlertDialog.Builder(requireContext())
            .setTitle("Report a Problem")
            .setView(input)
            .setPositiveButton("Submit") { dialog, _ ->
                val problem = input.text.toString()
                if (problem.isNotEmpty()) {
                    showToast("Problem reported. Thank you!")
                    // Here you can send the problem to your backend or email
                }
                dialog.dismiss()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun rateApp() {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=${requireActivity().packageName}"))
            startActivity(intent)
        } catch (e: Exception) {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=${requireActivity().packageName}"))
            startActivity(intent)
        }
    }

    // ========== Dark Mode - FIXED ==========

    private fun loadDarkModePreference() {
        val isDarkMode = sharedPreferences.getBoolean("dark_mode", false)
        binding.darkModeSwitch.isChecked = isDarkMode
    }

    private fun saveDarkModePreference(isEnabled: Boolean) {
        sharedPreferences.edit().putBoolean("dark_mode", isEnabled).apply()
    }

    private fun applyDarkMode(isEnabled: Boolean) {
        if (isEnabled) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
            showToast("Dark mode will apply on next app restart")
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
            showToast("Light mode will apply on next app restart")
        }
        // ⭐ REMOVED: requireActivity().recreate()
        // Dark mode will now apply when user restarts the app
    }

    // ========== Info App Dialog ==========

    private fun showInfoAppDialog() {
        val appInfo = """
            📱 Legal Help AI App
            
            Version: 1.0.0
            
            📝 Description:
            Your personal AI-powered legal assistant. Get instant answers to your legal questions using advanced Google Gemini AI technology.
            
            ✨ Features:
            • AI-powered chat assistance
            • Legal resource library
            • Secure authentication
            • User-friendly interface
            
            👨‍💻 Developed by: Legal Help AI Team
            
            📧 Contact: support@legalhelpai.com
            
            🌐 Website: www.legalhelpai.com
            
            © 2024 Legal Help AI. All rights reserved.
        """.trimIndent()

        AlertDialog.Builder(requireContext())
            .setTitle("App Information")
            .setIcon(R.drawable.ic_info)
            .setMessage(appInfo)
            .setPositiveButton("OK", null)
            .setNeutralButton("Share App") { _, _ ->
                shareApp()
            }
            .show()
    }

    private fun shareApp() {
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "Legal Help AI App")
            putExtra(Intent.EXTRA_TEXT, "Check out this amazing Legal Help AI app! Download it here: https://play.google.com/store/apps/details?id=${requireActivity().packageName}")
        }
        startActivity(Intent.createChooser(shareIntent, "Share via"))
    }

    // ========== Terms and Conditions Dialog ==========

    private fun showTermsAndConditionsDialog() {
        val terms = """
            📋 Terms and Conditions
            
            Last Updated: November 2024
            
            1. Acceptance of Terms
            By using Legal Help AI, you agree to these terms and conditions.
            
            2. Use of Service
            • This app is for informational purposes only
            • Not a substitute for professional legal advice
            • Use responsibly and ethically
            
            3. User Account
            • You are responsible for your account security
            • Keep your password confidential
            • Notify us of unauthorized access
            
            4. Privacy Policy
            • We respect your privacy
            • Data is encrypted and secure
            • We don't share personal information
            
            5. AI Responses
            • AI responses are generated automatically
            • May not always be 100% accurate
            • Verify important information with a lawyer
            
            6. Limitation of Liability
            • We are not liable for any decisions made based on AI responses
            • Use at your own risk
            
            7. Changes to Terms
            • We may update terms periodically
            • Continued use means acceptance
            
            8. Contact
            For questions: support@legalhelpai.com
            
            By continuing to use this app, you acknowledge that you have read and agree to these terms.
        """.trimIndent()

        AlertDialog.Builder(requireContext())
            .setTitle("Terms and Conditions")
            .setIcon(R.drawable.ic_document)
            .setMessage(terms)
            .setPositiveButton("I Agree", null)
            .setNegativeButton("Decline") { _, _ ->
                showToast("You must agree to continue using the app")
            }
            .show()
    }

    // ========== Options Menu ==========

    private fun showOptionsMenu() {
        val options = arrayOf(
            "🔄 Refresh Profile",
            "📤 Share Profile",
            "⚙️ Advanced Settings"
        )

        AlertDialog.Builder(requireContext())
            .setTitle("Menu Options")
            .setItems(options) { dialog, which ->
                when (which) {
                    0 -> refreshProfile()
                    1 -> shareProfile()
                    2 -> showToast("Advanced Settings - Coming Soon!")
                }
                dialog.dismiss()
            }
            .show()
    }

    private fun refreshProfile() {
        auth.currentUser?.reload()?.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                setupUserInfo()
                showToast("Profile refreshed!")
            } else {
                showToast("Failed to refresh profile")
            }
        }
    }

    private fun shareProfile() {
        val currentUser = auth.currentUser
        val shareText = """
            👤 ${currentUser?.displayName ?: "User"}
            📧 ${currentUser?.email ?: ""}
            
            Connect with me on Legal Help AI!
        """.trimIndent()

        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, shareText)
        }
        startActivity(Intent.createChooser(shareIntent, "Share Profile"))
    }

    // ========== Logout ==========

    private fun showLogoutConfirmationDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle("Logout")
            .setMessage("Are you sure you want to logout?")
            .setIcon(R.drawable.ic_logout)
            .setPositiveButton("Yes, Logout") { dialog, _ ->
                performLogout()
                dialog.dismiss()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun performLogout() {
        // Sign out from Firebase
        auth.signOut()

        // Clear any saved preferences if needed
        sharedPreferences.edit().clear().apply()

        // Navigate to SplashActivity
        val intent = Intent(activity, SplashActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)

        showToast("Logged out successfully!")
    }

    // ========== Helper Methods ==========

    private fun showToast(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}