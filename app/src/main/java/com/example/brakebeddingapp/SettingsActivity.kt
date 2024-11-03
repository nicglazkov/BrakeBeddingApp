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
import com.google.gson.JsonObject
import com.google.gson.JsonSerializer
import com.google.gson.reflect.TypeToken
import java.lang.reflect.Type
import android.widget.Button
import android.widget.EditText

class SettingsActivity : AppCompatActivity() {
    private val stages = mutableListOf<Stage>()
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
        val cooldownDistanceEditText = findViewById<EditText>(R.id.cooldownDistanceEditText)
        brakingIntensitySpinner = findViewById(R.id.brakingIntensitySpinner)
        val addStageButton = findViewById<Button>(R.id.addStageButton)
        val addCooldownButton = findViewById<Button>(R.id.addCooldownButton)
        val saveStagesButton = findViewById<Button>(R.id.saveStagesButton)
        recyclerView = findViewById(R.id.stagesRecyclerView)

        val toleranceEditText = findViewById<EditText>(R.id.toleranceEditText)
        val maxOverageEditText = findViewById<EditText>(R.id.maxOverageEditText)
        val minUnderageEditText = findViewById<EditText>(R.id.minUnderageEditText)
        val saveButton = findViewById<Button>(R.id.saveButton)

        val sharedPreferences = getSharedPreferences("BrakeBeddingApp", MODE_PRIVATE)
        toleranceEditText.setText(sharedPreferences.getString("speed_tolerance", "2.0"))
        maxOverageEditText.setText(sharedPreferences.getString("max_speed_overage", "5.0"))
        minUnderageEditText.setText(sharedPreferences.getString("min_speed_underage", "1.0"))

        saveButton.setOnClickListener {
            val editor = sharedPreferences.edit()
            editor.putString("speed_tolerance", toleranceEditText.text.toString())
            editor.putString("max_speed_overage", maxOverageEditText.text.toString())
            editor.putString("min_speed_underage", minUnderageEditText.text.toString())
            editor.apply()
            finish()
        }

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

                Snackbar.make(recyclerView, "Stage added", Snackbar.LENGTH_SHORT).show()
            } catch (e: NumberFormatException) {
                Toast.makeText(this, "Please fill all fields with valid numbers", Toast.LENGTH_SHORT).show()
            }
        }

        addCooldownButton.setOnClickListener {
            try {
                val distance = cooldownDistanceEditText.text.toString().toDouble()
                if (distance <= 0) {
                    Toast.makeText(this, "Distance must be greater than 0", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }

                // Only allow one cooldown stage at the end
                if (stages.any { it is CooldownStage }) {
                    Toast.makeText(this, "Cooldown stage already exists. Delete it first to add a new one.", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }

                val cooldownStage = CooldownStage(distance)
                stages.add(cooldownStage)
                adapter.notifyItemInserted(stages.size - 1)

                cooldownDistanceEditText.text.clear()
                Snackbar.make(recyclerView, "Cooldown stage added", Snackbar.LENGTH_SHORT).show()
            } catch (e: NumberFormatException) {
                Toast.makeText(this, "Please enter a valid distance", Toast.LENGTH_SHORT).show()
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
                val gson = Gson().newBuilder()
                    .registerTypeAdapter(Stage::class.java, object : JsonDeserializer<Stage> {
                        override fun deserialize(
                            json: JsonElement,
                            typeOfT: Type,
                            context: JsonDeserializationContext
                        ): Stage {
                            val jsonObject = json.asJsonObject
                            val type = jsonObject.get("type")?.asString

                            return when (type) {
                                "bedding" -> {
                                    val numberOfStops = jsonObject.get("numberOfStops").asInt
                                    val startSpeed = jsonObject.get("startSpeed").asDouble
                                    val targetSpeed = jsonObject.get("targetSpeed").asDouble
                                    val gapDistance = jsonObject.get("gapDistance").asDouble
                                    val brakingIntensity = try {
                                        val intensityStr = jsonObject.get("brakingIntensity").asString
                                        BrakingIntensity.valueOf(intensityStr)
                                    } catch (e: Exception) {
                                        BrakingIntensity.MODERATE
                                    }
                                    BeddingStage(
                                        numberOfStops = numberOfStops,
                                        startSpeed = startSpeed,
                                        targetSpeed = targetSpeed,
                                        gapDistance = gapDistance,
                                        brakingIntensity = brakingIntensity
                                    )
                                }
                                "cooldown" -> {
                                    val distance = jsonObject.get("distance").asDouble
                                    CooldownStage(distance = distance)
                                }
                                else -> {
                                    // For backward compatibility with old data format
                                    val numberOfStops = jsonObject.get("numberOfStops").asInt
                                    val startSpeed = jsonObject.get("startSpeed").asDouble
                                    val targetSpeed = jsonObject.get("targetSpeed").asDouble
                                    val gapDistance = jsonObject.get("gapDistance").asDouble
                                    val brakingIntensity = BrakingIntensity.MODERATE
                                    BeddingStage(
                                        numberOfStops = numberOfStops,
                                        startSpeed = startSpeed,
                                        targetSpeed = targetSpeed,
                                        gapDistance = gapDistance,
                                        brakingIntensity = brakingIntensity
                                    )
                                }
                            }
                        }
                    })
                    .create()

                val type = object : TypeToken<List<Stage>>() {}.type
                val loadedStages = gson.fromJson<List<Stage>>(stagesJson, type) ?: emptyList()
                stages.clear()
                stages.addAll(loadedStages)
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(this, "Error loading saved stages", Toast.LENGTH_SHORT).show()
                stages.clear()
            }
        }
    }

    private fun saveStages() {
        val gson = Gson().newBuilder()
            .registerTypeAdapter(Stage::class.java, JsonSerializer<Stage> { src, typeOfSrc, context ->
                val json = JsonObject()
                when (src) {
                    is BeddingStage -> {
                        json.addProperty("type", "bedding")
                        json.addProperty("numberOfStops", src.numberOfStops)
                        json.addProperty("startSpeed", src.startSpeed)
                        json.addProperty("targetSpeed", src.targetSpeed)
                        json.addProperty("gapDistance", src.gapDistance)
                        json.addProperty("brakingIntensity", src.brakingIntensity.name)
                    }
                    is CooldownStage -> {
                        json.addProperty("type", "cooldown")
                        json.addProperty("distance", src.distance)
                    }
                }
                json
            })
            .create()

        val sharedPreferences = getSharedPreferences("BrakeBeddingApp", Context.MODE_PRIVATE)
        sharedPreferences.edit().putString("stages", gson.toJson(stages)).apply()
    }

}