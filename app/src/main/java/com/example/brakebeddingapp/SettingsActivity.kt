package com.example.brakebeddingapp

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.snackbar.Snackbar
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class SettingsActivity : AppCompatActivity() {
    private val stages = mutableListOf<BeddingStage>()
    private lateinit var adapter: StagesAdapter
    private lateinit var recyclerView: RecyclerView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        val numberOfStopsEditText = findViewById<EditText>(R.id.numberOfStopsEditText)
        val startSpeedEditText = findViewById<EditText>(R.id.startSpeedEditText)
        val targetSpeedEditText = findViewById<EditText>(R.id.targetSpeedEditText)
        val gapDistanceEditText = findViewById<EditText>(R.id.gapDistanceEditText)
        val addStageButton = findViewById<Button>(R.id.addStageButton)
        val saveStagesButton = findViewById<Button>(R.id.saveStagesButton)
        recyclerView = findViewById(R.id.stagesRecyclerView)

        loadStages()

        adapter = StagesAdapter(stages)
        setupRecyclerView()

        addStageButton.setOnClickListener {
            try {
                val stage = BeddingStage(
                    numberOfStops = numberOfStopsEditText.text.toString().toInt(),
                    startSpeed = startSpeedEditText.text.toString().toDouble(),
                    targetSpeed = targetSpeedEditText.text.toString().toDouble(),
                    gapDistance = gapDistanceEditText.text.toString().toDouble()
                )
                stages.add(stage)
                adapter.notifyItemInserted(stages.size - 1)

                // Clear input fields after adding
                numberOfStopsEditText.text.clear()
                startSpeedEditText.text.clear()
                targetSpeedEditText.text.clear()
                gapDistanceEditText.text.clear()

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
        val swipeHandler = object : SwipeToDeleteCallback(this) {
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
        }
        val itemTouchHelper = ItemTouchHelper(swipeHandler)
        itemTouchHelper.attachToRecyclerView(recyclerView)
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

// Swipe-to-delete callback class
abstract class SwipeToDeleteCallback(context: Context) :
    ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT) {

    private val deleteIcon = ContextCompat.getDrawable(context, android.R.drawable.ic_menu_delete)
    private val background = ColorDrawable(Color.RED)
    private val iconMargin = 16
    private val paint = Paint().apply {
        color = Color.WHITE
        textSize = 40f
        textAlign = Paint.Align.CENTER
    }

    override fun onMove(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder, target: RecyclerView.ViewHolder): Boolean {
        return false
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
        val itemHeight = itemView.bottom - itemView.top

        // Draw the red background
        background.setBounds(
            itemView.left,
            itemView.top,
            itemView.right,
            itemView.bottom
        )
        background.draw(c)

        // Calculate text position
        val textY = itemView.top + ((itemHeight - paint.textSize) / 2) + paint.textSize
        c.drawText("Swipe to delete", itemView.width / 2f, textY, paint)

        super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)
    }
}