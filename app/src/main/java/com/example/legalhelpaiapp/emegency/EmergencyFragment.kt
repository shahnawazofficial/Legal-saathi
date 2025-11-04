package com.example.legalhelpaiapp.emergency

import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.legalhelpaiapp.R
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class EmergencyFragment : Fragment() {

    private lateinit var contactsRecyclerView: RecyclerView
    private lateinit var sosButton: View
    private lateinit var progressRing: ProgressBar
    private lateinit var emptyStateLayout: LinearLayout
    private lateinit var fabAddContact: FloatingActionButton
    private lateinit var callPoliceButton: Button
    private lateinit var callAmbulanceButton: Button

    private lateinit var contactAdapter: EmergencyContactAdapter
    private lateinit var locationHelper: LocationHelper
    private lateinit var smsHelper: SMSHelper
    private lateinit var permissionsHelper: PermissionsHelper

    private val contacts = mutableListOf<EmergencyContact>()
    private var isHoldingButton = false
    private var holdProgress = 0
    private val holdHandler = Handler(Looper.getMainLooper())

    private val HOLD_DURATION = 3000L
    private val PROGRESS_UPDATE_INTERVAL = 30L

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_emergency, container, false)

        initializeHelpers()
        initializeViews(view)
        setupRecyclerView()
        loadContacts()
        setupSOSButton()
        setupQuickActions()
        checkPermissions()

        return view
    }

    private fun initializeHelpers() {
        locationHelper = LocationHelper(requireContext())
        smsHelper = SMSHelper(requireContext())
        permissionsHelper = PermissionsHelper(this)
    }

    private fun initializeViews(view: View) {
        contactsRecyclerView = view.findViewById(R.id.contactsRecyclerView)
        sosButton = view.findViewById(R.id.sosButton)
        progressRing = view.findViewById(R.id.progressRing)
        emptyStateLayout = view.findViewById(R.id.emptyStateLayout)
        fabAddContact = view.findViewById(R.id.fabAddContact)
        callPoliceButton = view.findViewById(R.id.callPoliceButton)
        callAmbulanceButton = view.findViewById(R.id.callAmbulanceButton)

        fabAddContact.setOnClickListener {
            if (contacts.size >= 5) {
                Toast.makeText(context, "Maximum 5 contacts allowed", Toast.LENGTH_SHORT).show()
            } else {
                showAddContactDialog()
            }
        }
    }

    private fun setupRecyclerView() {
        contactAdapter = EmergencyContactAdapter(
            contacts,
            onEditClick = { contact -> showEditContactDialog(contact) },
            onDeleteClick = { contact -> deleteContact(contact) }
        )
        contactsRecyclerView.layoutManager = LinearLayoutManager(context)
        contactsRecyclerView.adapter = contactAdapter
    }

    private fun setupSOSButton() {
        sosButton.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    startHolding()
                    sosButton.performClick()
                    true
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    stopHolding()
                    true
                }
                else -> false
            }
        }
    }

    private fun setupQuickActions() {
        callPoliceButton.setOnClickListener {
            smsHelper.makePhoneCall("100")
        }

        callAmbulanceButton.setOnClickListener {
            smsHelper.makePhoneCall("108")
        }
    }

    private fun startHolding() {
        isHoldingButton = true
        holdProgress = 0
        progressRing.progress = 0
        progressRing.visibility = View.VISIBLE

        holdHandler.post(object : Runnable {
            override fun run() {
                if (isHoldingButton) {
                    holdProgress += PROGRESS_UPDATE_INTERVAL.toInt()
                    val progress = (holdProgress * 100 / HOLD_DURATION).toInt()
                    progressRing.progress = progress

                    if (holdProgress >= HOLD_DURATION) {
                        onHoldComplete()
                    } else {
                        holdHandler.postDelayed(this, PROGRESS_UPDATE_INTERVAL)
                    }
                }
            }
        })

        vibrateDevice(50)
    }

    private fun stopHolding() {
        isHoldingButton = false
        holdHandler.removeCallbacksAndMessages(null)
        progressRing.visibility = View.INVISIBLE
        progressRing.progress = 0
    }

    private fun onHoldComplete() {
        stopHolding()
        vibrateDevice(200)
        showConfirmationDialog()
    }

    private fun showConfirmationDialog() {
        val selectedContacts = contacts.filter { it.isSelected }

        if (selectedContacts.isEmpty()) {
            Toast.makeText(context, "Please select at least one contact", Toast.LENGTH_LONG).show()
            return
        }

        val message = "🚨 EMERGENCY ALERT 🚨\n\n" +
                "Send alert to ${selectedContacts.size} contact(s)?\n\n" +
                "They will receive:\n" +
                "• Your current location\n" +
                "• Google Maps link\n" +
                "• Emergency help message\n\n" +
                "⚠️ Use only for genuine emergencies"

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Send Emergency Alert?")
            .setMessage(message)
            .setPositiveButton("Send Alert") { _, _ ->
                sendEmergencyAlert(selectedContacts)
            }
            .setNegativeButton("Cancel", null)
            .setCancelable(true)
            .show()
    }

    private fun sendEmergencyAlert(selectedContacts: List<EmergencyContact>) {
        if (!permissionsHelper.hasAllPermissions()) {
            Toast.makeText(context, "Missing required permissions", Toast.LENGTH_SHORT).show()
            checkPermissions()
            return
        }

        val progressDialog = AlertDialog.Builder(requireContext())
            .setTitle("Sending Alert...")
            .setMessage("Getting your location and sending SMS...")
            .setCancelable(false)
            .create()
        progressDialog.show()

        locationHelper.getCurrentLocation(
            onSuccess = { location ->
                val message = smsHelper.createEmergencyMessage(
                    latitude = location.latitude,
                    longitude = location.longitude
                )

                var successCount = 0
                var failCount = 0

                selectedContacts.forEach { contact ->
                    if (smsHelper.sendSMS(contact.phoneNumber, message)) {
                        successCount++
                    } else {
                        failCount++
                    }
                }

                progressDialog.dismiss()
                showSuccessMessage(successCount, failCount)
            },
            onFailure = { error ->
                progressDialog.dismiss()
                Toast.makeText(context, "Error: $error", Toast.LENGTH_LONG).show()
            }
        )
    }

    private fun showSuccessMessage(successCount: Int, failCount: Int) {
        vibrateDevice(500)

        val message = if (failCount == 0) {
            "✅ Alert sent successfully to $successCount contact(s)!"
        } else {
            "⚠️ Alert sent to $successCount contact(s), $failCount failed"
        }

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Alert Sent")
            .setMessage(message)
            .setPositiveButton("OK", null)
            .show()
    }

    private fun showAddContactDialog() {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_add_contact, null)
        val nameInput: EditText = dialogView.findViewById(R.id.nameInput)
        val phoneInput: EditText = dialogView.findViewById(R.id.phoneInput)

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Add Emergency Contact")
            .setView(dialogView)
            .setPositiveButton("Add") { _, _ ->
                val name = nameInput.text.toString().trim()
                val phone = phoneInput.text.toString().trim()

                if (name.isNotEmpty() && phone.isNotEmpty()) {
                    val contact = EmergencyContact(
                        id = System.currentTimeMillis().toString(),
                        name = name,
                        phoneNumber = phone,
                        isSelected = true
                    )
                    addContact(contact)
                } else {
                    Toast.makeText(context, "Please fill all fields", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showEditContactDialog(contact: EmergencyContact) {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_add_contact, null)
        val nameInput: EditText = dialogView.findViewById(R.id.nameInput)
        val phoneInput: EditText = dialogView.findViewById(R.id.phoneInput)

        nameInput.setText(contact.name)
        phoneInput.setText(contact.phoneNumber)

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Edit Contact")
            .setView(dialogView)
            .setPositiveButton("Save") { _, _ ->
                val name = nameInput.text.toString().trim()
                val phone = phoneInput.text.toString().trim()

                if (name.isNotEmpty() && phone.isNotEmpty()) {
                    contact.name = name
                    contact.phoneNumber = phone
                    saveContacts()
                    contactAdapter.notifyDataSetChanged()
                } else {
                    Toast.makeText(context, "Please fill all fields", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun addContact(contact: EmergencyContact) {
        contacts.add(contact)
        saveContacts()
        updateUI()
    }

    private fun deleteContact(contact: EmergencyContact) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Delete Contact")
            .setMessage("Delete ${contact.name}?")
            .setPositiveButton("Delete") { _, _ ->
                contacts.remove(contact)
                saveContacts()
                updateUI()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun loadContacts() {
        val prefs = requireContext().getSharedPreferences("emergency_contacts", 0)
        val json = prefs.getString("contacts", "[]")

        contacts.clear()
        contacts.addAll(EmergencyContact.fromJsonArray(json ?: "[]"))

        updateUI()
    }

    private fun saveContacts() {
        val prefs = requireContext().getSharedPreferences("emergency_contacts", 0)
        val json = EmergencyContact.toJsonArray(contacts)
        prefs.edit().putString("contacts", json).apply()
    }

    private fun updateUI() {
        if (contacts.isEmpty()) {
            emptyStateLayout.visibility = View.VISIBLE
            contactsRecyclerView.visibility = View.GONE
        } else {
            emptyStateLayout.visibility = View.GONE
            contactsRecyclerView.visibility = View.VISIBLE
        }
        contactAdapter.notifyDataSetChanged()
    }

    private fun checkPermissions() {
        if (!permissionsHelper.hasAllPermissions()) {
            permissionsHelper.requestPermissions()
        }
    }

    private fun vibrateDevice(duration: Long) {
        val vibrator = ContextCompat.getSystemService(requireContext(), Vibrator::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator?.vibrate(VibrationEffect.createOneShot(duration, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            @Suppress("DEPRECATION")
            vibrator?.vibrate(duration)
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        permissionsHelper.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    override fun onDestroy() {
        super.onDestroy()
        holdHandler.removeCallbacksAndMessages(null)
    }
}