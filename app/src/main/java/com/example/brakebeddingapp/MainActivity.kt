package com.example.brakebeddingapp

import android.Manifest
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
import android.view.WindowManager

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
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        val speedTextView = findViewById<TextView>(R.id.speedTextView)
        val instructionTextView = findViewById<TextView>(R.id.instructionTextView)
        val statusView = findViewById<View>(R.id.statusView)
        val startButton = findViewById<Button>(R.id.startButton)
        val settingsButton = findViewById<Button>(R.id.settingsButton)
        val progressTextView = findViewById<TextView>(R.id.stageProgressTextView)
        findViewById<Button>(R.id.helpButton).setOnClickListener {
            HelpGuideDialog().show(supportFragmentManager, "help_guide")
        }

        stageManager = StageManager(
            context = this,
            speedTextView = speedTextView,
            instructionTextView = instructionTextView,
            statusView = statusView,
            progressTextView = progressTextView,  // Add this line
            handler = handler
        )

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

    override fun onResume() {
        super.onResume()
        // Reload stages when returning to the activity
        stageManager.reloadStages()
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

    // Required LocationListener interface methods
    override fun onProviderEnabled(provider: String) {}
    override fun onProviderDisabled(provider: String) {}
    @Deprecated("Deprecated in Java")
    override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}

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