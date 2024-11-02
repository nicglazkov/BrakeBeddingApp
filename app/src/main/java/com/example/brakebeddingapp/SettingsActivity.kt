package com.example.brakebeddingapp

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity

class SettingsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        val startSpeedEditText = findViewById<EditText>(R.id.startSpeedEditText)
        val targetSpeedEditText = findViewById<EditText>(R.id.targetSpeedEditText)
        val startProcedureButton = findViewById<Button>(R.id.startProcedureButton)

        startProcedureButton.setOnClickListener {
            val startSpeed = startSpeedEditText.text.toString().toDoubleOrNull() ?: 0.0
            val targetSpeed = targetSpeedEditText.text.toString().toDoubleOrNull() ?: 0.0

            val intent = Intent(this, MainActivity::class.java).apply {
                putExtra("startSpeed", startSpeed)
                putExtra("targetSpeed", targetSpeed)
            }
            startActivity(intent)
        }
    }
}
