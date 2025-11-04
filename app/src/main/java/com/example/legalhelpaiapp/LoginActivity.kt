package com.example.legalhelpaiapp.ui.login

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.legalhelpaiapp.DashboardActivity
import com.example.legalhelpaiapp.SignupActivity
import com.example.legalhelpaiapp.databinding.ActivityLoginBinding
import com.google.firebase.auth.FirebaseAuth

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize Firebase Auth
        auth = FirebaseAuth.getInstance()

        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Check if user is already signed in
        val currentUser = auth.currentUser
        if (currentUser != null) {
            startActivity(Intent(this, DashboardActivity::class.java))
            finish()
        }

        binding.login.setOnClickListener {
            val email = binding.username.text.toString().trim()
            val password = binding.password.text.toString().trim()

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Please enter email and password.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            showLoading(true)

            auth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this) { task ->
                    showLoading(false)
                    if (task.isSuccessful) {
                        // Sign in success, navigate to DashboardActivity
                        val user = auth.currentUser
                        Toast.makeText(baseContext, "Authentication successful.", Toast.LENGTH_SHORT).show()
                        startActivity(Intent(this, DashboardActivity::class.java))
                        finish()
                    } else {
                        // If sign in fails, display a message to the user.
                        Toast.makeText(baseContext, "Authentication failed. Check your email or password.", Toast.LENGTH_LONG).show()
                    }
                }
        }

        binding.createAccount.setOnClickListener {
            startActivity(Intent(this, SignupActivity::class.java))
        }

        binding.forgotPassword.setOnClickListener {
            showForgotPasswordDialog()
        }

        binding.username.addTextChangedListener(loginTextWatcher)
        binding.password.addTextChangedListener(loginTextWatcher)
    }

    private val loginTextWatcher = object : TextWatcher {
        override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
        override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
            val email = binding.username.text.toString().trim()
            val password = binding.password.text.toString().trim()
            binding.login.isEnabled = email.isNotEmpty() && password.length > 5
        }
        override fun afterTextChanged(s: Editable) {}
    }

    private fun showLoading(isLoading: Boolean) {
        binding.loading.visibility = if (isLoading) View.VISIBLE else View.GONE
    }

    private fun showForgotPasswordDialog() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Reset Password")

        // Set up the input
        val input = EditText(this)
        input.hint = "Enter your email address"
        input.inputType = android.text.InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS
        input.setPadding(50, 30, 50, 30)

        // Pre-fill with username if it's an email
        val currentEmail = binding.username.text.toString().trim()
        if (currentEmail.isNotEmpty() && android.util.Patterns.EMAIL_ADDRESS.matcher(currentEmail).matches()) {
            input.setText(currentEmail)
        }

        builder.setView(input)

        // Set up the buttons
        builder.setPositiveButton("Send Reset Link") { dialog, _ ->
            val email = input.text.toString().trim()

            if (email.isEmpty()) {
                Toast.makeText(this, "Please enter your email address", Toast.LENGTH_SHORT).show()
                return@setPositiveButton
            }

            if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                Toast.makeText(this, "Please enter a valid email address", Toast.LENGTH_SHORT).show()
                return@setPositiveButton
            }

            sendPasswordResetEmail(email)
            dialog.dismiss()
        }

        builder.setNegativeButton("Cancel") { dialog, _ ->
            dialog.cancel()
        }

        builder.show()
    }

    private fun sendPasswordResetEmail(email: String) {
        showLoading(true)

        auth.sendPasswordResetEmail(email)
            .addOnSuccessListener {
                showLoading(false)
                AlertDialog.Builder(this)
                    .setTitle("Email Sent")
                    .setMessage("Password reset link has been sent to $email\n\nPlease check your inbox and spam folder.")
                    .setPositiveButton("OK") { dialog, _ ->
                        dialog.dismiss()
                    }
                    .show()
            }
            .addOnFailureListener { exception ->
                showLoading(false)
                val errorMessage = when {
                    exception.message?.contains("no user record", ignoreCase = true) == true ->
                        "No account found with this email address. Please check and try again."
                    exception.message?.contains("network", ignoreCase = true) == true ->
                        "Network error. Please check your internet connection."
                    else -> exception.message ?: "Failed to send reset email"
                }

                AlertDialog.Builder(this)
                    .setTitle("Error")
                    .setMessage(errorMessage)
                    .setPositiveButton("OK") { dialog, _ ->
                        dialog.dismiss()
                    }
                    .show()
            }
    }
}