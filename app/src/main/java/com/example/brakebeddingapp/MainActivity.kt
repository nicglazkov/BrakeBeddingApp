package com.example.brakebeddingapp

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity(), LocationListener {

    private lateinit var locationManager: LocationManager
    private lateinit var speedTextView: TextView
    private lateinit var statusView: View
    private lateinit var instructionTextView: TextView

    private var startSpeed: Double = 0.0
    private var targetSpeed: Double = 0.0
    private val LOCATION_PERMISSION_REQUEST_CODE = 1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        speedTextView = findViewById(R.id.speedTextView)
        statusView = findViewById(R.id.statusView)
        instructionTextView = findViewById(R.id.instructionTextView)

        val openSettingsButton = findViewById<Button>(R.id.openSettingsButton)
        openSettingsButton.setOnClickListener {
            val intent = Intent(this, SettingsActivity::class.java)
            startActivity(intent)
        }

        // Retrieve startSpeed and targetSpeed from Intent
        startSpeed = intent.getDoubleExtra("startSpeed", 0.0)
        targetSpeed = intent.getDoubleExtra("targetSpeed", 0.0)

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
                100L,
                0.1f,
                this
            )
        } catch (e: SecurityException) {
            e.printStackTrace()
        }
    }

    override fun onLocationChanged(location: Location) {
        val speedMph = location.speed * 2.237
        speedTextView.text = "Speed: %.1f mph".format(speedMph)

        // Dynamic instruction and color based on startSpeed and targetSpeed
        val (colorRes, instructionText) = when {
            speedMph >= startSpeed -> android.R.color.holo_green_light to "Maintain Speed"
            speedMph < startSpeed && speedMph > targetSpeed -> android.R.color.holo_orange_light to "Coast"
            speedMph <= targetSpeed -> android.R.color.holo_red_dark to "Slow Down"
            else -> android.R.color.holo_blue_light to "Adjust Speed"
        }

        statusView.setBackgroundColor(ContextCompat.getColor(this, colorRes))
        instructionTextView.text = instructionText
    }

    override fun onProviderDisabled(provider: String) {
        Toast.makeText(this, "GPS disabled. Enable it for location updates.", Toast.LENGTH_SHORT).show()
    }
}
