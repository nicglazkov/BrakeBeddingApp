package com.example.brakebeddingapp

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class SettingsActivity : AppCompatActivity() {
    private lateinit var stagesAdapter: StagesAdapter
    private val sharedPreferences by lazy {
        getSharedPreferences("BrakeBeddingAppPrefs", MODE_PRIVATE)
    }
    private val gson by lazy { Gson() }
    private var stages = mutableListOf<BeddingStage>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        // Load stages from SharedPreferences
        stages = loadStages().toMutableList()

        // Setup RecyclerView
        val recyclerView = findViewById<RecyclerView>(R.id.stagesRecyclerView)
        stagesAdapter = StagesAdapter(stages, onStageLongClick = { position ->
            // Remove the item and update SharedPreferences
            stages.removeAt(position)
            stagesAdapter.notifyItemRemoved(position)
            saveStages(stages)
        })
        recyclerView.adapter = stagesAdapter
        recyclerView.layoutManager = LinearLayoutManager(this)

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
            stagesAdapter.notifyItemInserted(stages.size - 1)
            saveStages(stages)

            // Clear input fields
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

    private fun saveStages(stages: List<BeddingStage>) {
        val editor = sharedPreferences.edit()
        val json = gson.toJson(stages)
        editor.putString("stages_key", json)
        editor.apply()
    }

    private fun loadStages(): List<BeddingStage> {
        val json = sharedPreferences.getString("stages_key", null)
        return if (json != null) {
            val type = object : TypeToken<List<BeddingStage>>() {}.type
            gson.fromJson(json, type)
        } else {
            emptyList()
        }
    }
}
