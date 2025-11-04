package com.example.legalhelpaiapp.emergency

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import androidx.core.app.ActivityCompat
import java.util.*

class LocationHelper(private val context: Context) {

    private val locationManager: LocationManager =
        context.getSystemService(Context.LOCATION_SERVICE) as LocationManager

    fun getCurrentLocation(
        onSuccess: (Location) -> Unit,
        onFailure: (String) -> Unit
    ) {
        try {
            // Check permissions
            if (ActivityCompat.checkSelfPermission(
                    context,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(
                    context,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                onFailure("Location permission not granted")
                return
            }

            // Check if GPS is enabled
            val isGpsEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
            val isNetworkEnabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)

            if (!isGpsEnabled && !isNetworkEnabled) {
                onFailure("GPS is disabled. Please enable location services.")
                return
            }

            // Try to get last known location first
            val lastKnownLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                ?: locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)

            if (lastKnownLocation != null) {
                onSuccess(lastKnownLocation)
                return
            }

            // Request fresh location
            val locationListener = object : LocationListener {
                override fun onLocationChanged(location: Location) {
                    onSuccess(location)
                    locationManager.removeUpdates(this)
                }

                override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
                override fun onProviderEnabled(provider: String) {}
                override fun onProviderDisabled(provider: String) {
                    onFailure("Location provider disabled")
                }
            }

            // Request location updates
            if (isGpsEnabled) {
                locationManager.requestLocationUpdates(
                    LocationManager.GPS_PROVIDER,
                    0L,
                    0f,
                    locationListener
                )
            } else if (isNetworkEnabled) {
                locationManager.requestLocationUpdates(
                    LocationManager.NETWORK_PROVIDER,
                    0L,
                    0f,
                    locationListener
                )
            }

            // Timeout after 10 seconds
            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                locationManager.removeUpdates(locationListener)
                onFailure("Location timeout. Please try again.")
            }, 10000)

        } catch (e: SecurityException) {
            onFailure("Location permission denied")
        } catch (e: Exception) {
            onFailure("Error getting location: ${e.message}")
        }
    }

    fun getAddressFromLocation(latitude: Double, longitude: Double): String {
        return try {
            val geocoder = Geocoder(context, Locale.getDefault())
            val addresses = geocoder.getFromLocation(latitude, longitude, 1)

            if (addresses != null && addresses.isNotEmpty()) {
                val address = addresses[0]
                buildString {
                    address.getAddressLine(0)?.let { append(it) }
                }
            } else {
                "Location: $latitude, $longitude"
            }
        } catch (e: Exception) {
            "Location: $latitude, $longitude"
        }
    }
}