package com.example.brakebeddingapp

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity(), LocationListener {

    private lateinit var stageManager: StageManager
    private lateinit var locationManager: LocationManager
    private val handler = Handler(Looper.getMainLooper())
    private var lastSpeed = 0.0
    private val updateInterval = 333L // ~3Hz (1000ms / 3)

    private val speedUpdateRunnable = object : Runnable {
        override fun run() {
            stageManager.updateSpeed(lastSpeed)
            handler.postDelayed(this, updateInterval)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val speedTextView = findViewById<TextView>(R.id.speedTextView)
        val instructionTextView = findViewById<TextView>(R.id.instructionTextView)
        val statusView = findViewById<View>(R.id.statusView)
        val startButton = findViewById<Button>(R.id.startButton)
        val settingsButton = findViewById<Button>(R.id.settingsButton)

        stageManager = StageManager(this, speedTextView, instructionTextView, statusView, handler)

        settingsButton.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }

        startButton.setOnClickListener {
            stageManager.startProcedure()
        }

        // Initialize location manager for speed tracking
        locationManager = getSystemService(LOCATION_SERVICE) as LocationManager
        requestLocationUpdates()

        // Start the speed update timer
        handler.post(speedUpdateRunnable)
    }

    private fun requestLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), 1)
            return
        }
        // Request frequent updates from GPS, but we'll throttle the UI updates ourselves
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0f, this)
    }

    override fun onLocationChanged(location: Location) {
        val speedInMps = location.speed
        lastSpeed = speedInMps * 2.23694 // Convert meters/second to miles/hour
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 1 && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            requestLocationUpdates()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        locationManager.removeUpdates(this)
        handler.removeCallbacks(speedUpdateRunnable)
    }
}