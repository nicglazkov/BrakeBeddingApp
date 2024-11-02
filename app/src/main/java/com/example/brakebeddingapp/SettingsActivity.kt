package com.example.brakebeddingapp

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity

class SettingsActivity : AppCompatActivity() {

    private val stages = mutableListOf<BeddingStage>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        val numberOfStopsEditText = findViewById<EditText>(R.id.numberOfStopsEditText)
        val startSpeedEditText = findViewById<EditText>(R.id.startSpeedEditText)
        val targetSpeedEditText = findViewById<EditText>(R.id.targetSpeedEditText)
        val gapDistanceEditText = findViewById<EditText>(R.id.gapDistanceEditText)
        val addStageButton = findViewById<Button>(R.id.addStageButton)
        val startProcedureButton = findViewById<Button>(R.id.startProcedureButton)

        addStageButton.setOnClickListener {
            val numberOfStops = numberOfStopsEditText.text.toString().toIntOrNull() ?: return@setOnClickListener
            val startSpeed = startSpeedEditText.text.toString().toDoubleOrNull() ?: return@setOnClickListener
            val targetSpeed = targetSpeedEditText.text.toString().toDoubleOrNull() ?: return@setOnClickListener
            val gapDistance = gapDistanceEditText.text.toString().toDoubleOrNull() ?: return@setOnClickListener

            val stage = BeddingStage(numberOfStops, startSpeed, targetSpeed, gapDistance)
            stages.add(stage)

            numberOfStopsEditText.text.clear()
            startSpeedEditText.text.clear()
            targetSpeedEditText.text.clear()
            gapDistanceEditText.text.clear()
        }

        startProcedureButton.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            intent.putParcelableArrayListExtra("stages", ArrayList(stages))
            startActivity(intent)
        }
    }
}
