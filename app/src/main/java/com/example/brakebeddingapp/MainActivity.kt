package com.example.brakebeddingapp

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import androidx.core.content.ContextCompat
import androidx.core.app.ActivityCompat
import android.Manifest
import android.content.pm.PackageManager

class MainActivity : AppCompatActivity(), LocationListener {

    private lateinit var locationManager: LocationManager
    private lateinit var speedTextView: TextView
    private lateinit var statusView: View
    private lateinit var instructionTextView: TextView // Ensure this matches your XML ID

    private val LOCATION_PERMISSION_REQUEST_CODE = 1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Ensure these IDs match what's in your XML
        speedTextView = findViewById(R.id.speedTextView)
        statusView = findViewById(R.id.statusView)
        instructionTextView = findViewById(R.id.instructionTextView) // Matches XML

        val openSettingsButton = findViewById<Button>(R.id.openSettingsButton)
        openSettingsButton.setOnClickListener {
            val intent = Intent(this, SettingsActivity::class.java)
            startActivity(intent)
        }

        checkLocationPermissions()
    }

    private fun checkLocationPermissions() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION),
                LOCATION_PERMISSION_REQUEST_CODE
            )
        } else {
            startLocationUpdates()
        }
    }

    private fun startLocationUpdates() {
        locationManager = getSystemService(LOCATION_SERVICE) as LocationManager
        try {
            locationManager.requestLocationUpdates(
                LocationManager.GPS_PROVIDER,
                300L,
                1f,
                this
            )
        } catch (e: SecurityException) {
            e.printStackTrace()
        }
    }

    override fun onLocationChanged(location: Location) {
        val speedMph = location.speed * 2.237
        speedTextView.text = "Speed: %.1f mph".format(speedMph)

        val colorRes = when {
            speedMph >= 60 -> android.R.color.holo_green_light
            speedMph >= 40 -> android.R.color.holo_orange_light
            speedMph >= 20 -> android.R.color.holo_blue_light
            else -> android.R.color.holo_red_dark
        }
        statusView.setBackgroundColor(ContextCompat.getColor(this, colorRes))

        // Update instruction text based on speed
        instructionTextView.text = when {
            speedMph >= 60 -> "Maintain Speed"
            speedMph >= 40 -> "Speed Up"
            speedMph >= 20 -> "Coast"
            else -> "Slow Down"
        }
    }

    override fun onProviderDisabled(provider: String) {
        Toast.makeText(this, "GPS disabled. Enable it for location updates.", Toast.LENGTH_SHORT).show()
    }
}
