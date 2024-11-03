package com.example.brakebeddingapp

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.snackbar.Snackbar
import com.google.gson.Gson
import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.reflect.TypeToken
import java.lang.reflect.Type

class SettingsActivity : AppCompatActivity() {
    private val stages = mutableListOf<BeddingStage>()
    private lateinit var adapter: StagesAdapter
    private lateinit var recyclerView: RecyclerView
    private lateinit var brakingIntensitySpinner: Spinner

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        val numberOfStopsEditText = findViewById<EditText>(R.id.numberOfStopsEditText)
        val startSpeedEditText = findViewById<EditText>(R.id.startSpeedEditText)
        val targetSpeedEditText = findViewById<EditText>(R.id.targetSpeedEditText)
        val gapDistanceEditText = findViewById<EditText>(R.id.gapDistanceEditText)
        brakingIntensitySpinner = findViewById(R.id.brakingIntensitySpinner)
        val addStageButton = findViewById<Button>(R.id.addStageButton)
        val saveStagesButton = findViewById<Button>(R.id.saveStagesButton)
        recyclerView = findViewById(R.id.stagesRecyclerView)

        // Setup spinner
        val intensityAdapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            BrakingIntensity.values().map { it.displayName }
        )
        intensityAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        brakingIntensitySpinner.adapter = intensityAdapter

        loadSavedStages()

        adapter = StagesAdapter(stages)
        setupRecyclerView()

        addStageButton.setOnClickListener {
            try {
                val stage = BeddingStage(
                    numberOfStops = numberOfStopsEditText.text.toString().toInt(),
                    startSpeed = startSpeedEditText.text.toString().toDouble(),
                    targetSpeed = targetSpeedEditText.text.toString().toDouble(),
                    gapDistance = gapDistanceEditText.text.toString().toDouble(),
                    brakingIntensity = BrakingIntensity.fromDisplayName(
                        brakingIntensitySpinner.selectedItem.toString()
                    )
                )
                stages.add(stage)
                adapter.notifyItemInserted(stages.size - 1)

                // Clear input fields after adding
                numberOfStopsEditText.text.clear()
                startSpeedEditText.text.clear()
                targetSpeedEditText.text.clear()
                gapDistanceEditText.text.clear()
                brakingIntensitySpinner.setSelection(0)

                // Show confirmation
                Snackbar.make(recyclerView, "Stage added", Snackbar.LENGTH_SHORT).show()
            } catch (e: NumberFormatException) {
                Toast.makeText(this, "Please fill all fields with valid numbers", Toast.LENGTH_SHORT).show()
            }
        }

        saveStagesButton.setOnClickListener {
            saveStages()
            Toast.makeText(this, "Stages saved", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    private fun setupRecyclerView() {
        recyclerView.apply {
            layoutManager = LinearLayoutManager(this@SettingsActivity)
            adapter = this@SettingsActivity.adapter
        }

        // Add swipe-to-delete functionality
        val swipeHandler = object : ItemTouchHelper.SimpleCallback(
            0, ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT
        ) {
            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean = false

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val position = viewHolder.adapterPosition
                val removedStage = stages[position]

                // Remove the item
                stages.removeAt(position)
                adapter.notifyItemRemoved(position)

                // Show undo snackbar
                Snackbar.make(recyclerView, "Stage removed", Snackbar.LENGTH_LONG)
                    .setAction("UNDO") {
                        stages.add(position, removedStage)
                        adapter.notifyItemInserted(position)
                    }.show()
            }

            override fun onChildDraw(
                c: Canvas,
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                dX: Float,
                dY: Float,
                actionState: Int,
                isCurrentlyActive: Boolean
            ) {
                val itemView = viewHolder.itemView
                val background = ColorDrawable(Color.RED)
                val paint = Paint().apply {
                    color = Color.WHITE
                    textSize = 40f
                    textAlign = Paint.Align.CENTER
                }

                // Draw the red background
                background.setBounds(
                    itemView.left,
                    itemView.top,
                    itemView.right,
                    itemView.bottom
                )
                background.draw(c)

                // Calculate text position and draw
                val textY = itemView.top + ((itemView.bottom - itemView.top) / 2f) + paint.textSize / 3
                c.drawText("Swipe to delete", itemView.width / 2f, textY, paint)

                super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)
            }
        }
        ItemTouchHelper(swipeHandler).attachToRecyclerView(recyclerView)
    }

    private fun loadSavedStages() {
        val sharedPreferences = getSharedPreferences("BrakeBeddingApp", Context.MODE_PRIVATE)
        val stagesJson = sharedPreferences.getString("stages", null)

        if (stagesJson != null) {
            try {
                // Create Gson instance with custom deserializer
                val gson = Gson().newBuilder()
                    .registerTypeAdapter(BeddingStage::class.java, object : JsonDeserializer<BeddingStage> {
                        override fun deserialize(
                            json: JsonElement,
                            typeOfT: Type,
                            context: JsonDeserializationContext
                        ): BeddingStage {
                            val jsonObject = json.asJsonObject

                            val numberOfStops = jsonObject.get("numberOfStops").asInt
                            val startSpeed = jsonObject.get("startSpeed").asDouble
                            val targetSpeed = jsonObject.get("targetSpeed").asDouble
                            val gapDistance = jsonObject.get("gapDistance").asDouble

                            // Try to get braking intensity, default to MODERATE if not present
                            val brakingIntensity = try {
                                val intensityStr = jsonObject.get("brakingIntensity").asString
                                BrakingIntensity.valueOf(intensityStr)
                            } catch (e: Exception) {
                                BrakingIntensity.MODERATE
                            }

                            return BeddingStage(
                                numberOfStops = numberOfStops,
                                startSpeed = startSpeed,
                                targetSpeed = targetSpeed,
                                gapDistance = gapDistance,
                                brakingIntensity = brakingIntensity
                            )
                        }
                    })
                    .create()

                val type = object : TypeToken<List<BeddingStage>>() {}.type
                val loadedStages = gson.fromJson<List<BeddingStage>>(stagesJson, type)
                stages.addAll(loadedStages ?: emptyList())
            } catch (e: Exception) {
                // If there's any error, clear the stages and show an error
                stages.clear()
                Toast.makeText(this, "Error loading saved stages", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun saveStages() {
        val sharedPreferences = getSharedPreferences("BrakeBeddingApp", Context.MODE_PRIVATE)
        sharedPreferences.edit().putString("stages", Gson().toJson(stages)).apply()
    }
}