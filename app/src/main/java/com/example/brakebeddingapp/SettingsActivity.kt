package com.example.brakebeddingapp

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class SettingsActivity : AppCompatActivity() {

    private val stages = mutableListOf<BeddingStage>()
    private lateinit var adapter: StagesAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        val numberOfStopsEditText = findViewById<EditText>(R.id.numberOfStopsEditText)
        val startSpeedEditText = findViewById<EditText>(R.id.startSpeedEditText)
        val targetSpeedEditText = findViewById<EditText>(R.id.targetSpeedEditText)
        val gapDistanceEditText = findViewById<EditText>(R.id.gapDistanceEditText)
        val addStageButton = findViewById<Button>(R.id.addStageButton)
        val saveStagesButton = findViewById<Button>(R.id.saveStagesButton)

        loadStages()

        adapter = StagesAdapter(stages) { stage ->
            Toast.makeText(this, "Long-clicked: $stage", Toast.LENGTH_SHORT).show()
            // Optional: Add functionality to edit or delete the stage here if desired
        }

        findViewById<RecyclerView>(R.id.stagesRecyclerView).apply {
            layoutManager = LinearLayoutManager(this@SettingsActivity)
            adapter = this@SettingsActivity.adapter
        }

        addStageButton.setOnClickListener {
            val stage = BeddingStage(
                numberOfStops = numberOfStopsEditText.text.toString().toInt(),
                startSpeed = startSpeedEditText.text.toString().toDouble(),
                targetSpeed = targetSpeedEditText.text.toString().toDouble(),
                gapDistance = gapDistanceEditText.text.toString().toDouble()
            )
            stages.add(stage)
            adapter.notifyItemInserted(stages.size - 1)
        }

        saveStagesButton.setOnClickListener {
            saveStages()
            Toast.makeText(this, "Stages saved", Toast.LENGTH_SHORT).show()
            startActivity(Intent(this, MainActivity::class.java))
        }
    }

    private fun saveStages() {
        val sharedPreferences = getSharedPreferences("BrakeBeddingApp", Context.MODE_PRIVATE)
        sharedPreferences.edit().putString("stages", Gson().toJson(stages)).apply()
    }

    private fun loadStages() {
        val sharedPreferences = getSharedPreferences("BrakeBeddingApp", Context.MODE_PRIVATE)
        val stagesJson = sharedPreferences.getString("stages", null)
        if (stagesJson != null) {
            val type = object : TypeToken<List<BeddingStage>>() {}.type
            stages.addAll(Gson().fromJson(stagesJson, type))
        }
    }
}
