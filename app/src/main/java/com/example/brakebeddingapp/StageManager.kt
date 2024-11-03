package com.example.brakebeddingapp

import android.content.Context
import android.os.Handler
import android.view.View
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class StageManager(
    private val context: Context,
    private val speedTextView: TextView,
    private val instructionTextView: TextView,
    private val statusView: View,
    private val handler: Handler
) {
    private var stages: List<BeddingStage> = listOf()
    private var currentStageIndex = 0
    private var currentCycleCount = 0

    init {
        loadStages()
    }

    private fun loadStages() {
        val sharedPreferences = context.getSharedPreferences("BrakeBeddingApp", Context.MODE_PRIVATE)
        val stagesJson = sharedPreferences.getString("stages", null)
        if (stagesJson != null) {
            val type = object : TypeToken<List<BeddingStage>>() {}.type
            stages = Gson().fromJson(stagesJson, type) ?: listOf()
        }
    }

    fun startProcedure() {
        if (stages.isEmpty()) {
            updateUI("No stages available. Please configure stages in Settings.")
            return
        }
        startStage()
    }


    private fun startStage() {
        val stage = stages[currentStageIndex]
        currentCycleCount = 0
        updateUI("Starting Stage ${currentStageIndex + 1}")
        startCycle(stage)
    }

    fun updateSpeed(currentSpeed: Double) {
        speedTextView.text = "Speed: ${currentSpeed.toInt()} mph"
    }

    private fun startCycle(stage: BeddingStage) {
        updateUI("Cycle ${currentCycleCount + 1}")
        handler.postDelayed({
            // Simulate reaching starting speed
            instructionTextView.text = "Hold at start speed"
            statusView.setBackgroundColor(ContextCompat.getColor(context, android.R.color.holo_blue_light))
            handler.postDelayed({
                instructionTextView.text = "Brake now!"
                statusView.setBackgroundColor(ContextCompat.getColor(context, android.R.color.holo_red_dark))
                handler.postDelayed({
                    instructionTextView.text = "Drive for ${stage.gapDistance} miles"
                    currentCycleCount++
                    if (currentCycleCount < stage.numberOfStops) {
                        startCycle(stage)
                    } else {
                        finishStage()
                    }
                }, 3000)
            }, 3000)
        }, 3000)
    }

    private fun finishStage() {
        currentStageIndex++
        if (currentStageIndex < stages.size) {
            startStage()
        } else {
            updateUI("Procedure Complete!")
        }
    }

    private fun updateUI(message: String) {
        instructionTextView.text = message
    }
}
