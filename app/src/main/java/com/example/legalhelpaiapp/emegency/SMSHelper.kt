package com.example.legalhelpaiapp.emergency

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.telephony.SmsManager
import java.text.SimpleDateFormat
import java.util.*

class SMSHelper(private val context: Context) {

    fun createEmergencyMessage(latitude: Double, longitude: Double): String {
        val timeFormat = SimpleDateFormat("MMM dd, yyyy hh:mm a", Locale.getDefault())
        val currentTime = timeFormat.format(Date())

        val geocoder = android.location.Geocoder(context, Locale.getDefault())
        var address = ""

        try {
            val addresses = geocoder.getFromLocation(latitude, longitude, 1)
            if (addresses?.isNotEmpty() == true) {
                val addr = addresses[0]
                address = buildString {
                    if (addr.locality != null) append("${addr.locality}, ")
                    if (addr.adminArea != null) append("${addr.adminArea}, ")
                    if (addr.countryName != null) append(addr.countryName)
                }
            }
        } catch (e: Exception) {
            address = "Address unavailable"
        }

        val mapsUrl = "https://maps.google.com/?q=$latitude,$longitude"

        return """
🚨 EMERGENCY ALERT 🚨

I NEED HELP!

My Location:
📍 Lat: $latitude, Long: $longitude
📍 $address

🗺️ Google Maps:
$mapsUrl

⏰ Time: $currentTime

- Sent from Legal Help AI App
        """.trimIndent()
    }

    fun sendSMS(phoneNumber: String, message: String): Boolean {
        return try {
            val smsManager = context.getSystemService(SmsManager::class.java)

            // Split message if too long
            val parts = smsManager.divideMessage(message)

            if (parts.size == 1) {
                smsManager.sendTextMessage(phoneNumber, null, message, null, null)
            } else {
                smsManager.sendMultipartTextMessage(phoneNumber, null, parts, null, null)
            }

            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    fun makePhoneCall(phoneNumber: String) {
        try {
            val intent = Intent(Intent.ACTION_DIAL).apply {
                data = Uri.parse("tel:$phoneNumber")
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}