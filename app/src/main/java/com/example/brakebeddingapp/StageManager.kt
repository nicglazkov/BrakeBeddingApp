package com.example.brakebeddingapp

import android.content.Context
import android.os.Handler
import android.view.View
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlin.math.abs

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
    private var currentState = State.IDLE
    private var startLocation: android.location.Location? = null
    private var currentSpeed = 0.0
    private var countdownSeconds = 3
    private val SPEED_TOLERANCE = 2.0  // mph
    private val MAX_SPEED_OVERAGE = 5.0  // mph

    private enum class State {
        IDLE,
        DECELERATING,  // New state for when speed is too high
        ACCELERATING,
        HOLDING_SPEED,
        COUNTDOWN,
        BRAKING,
        DRIVING_GAP
    }

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
        currentStageIndex = 0
        startStage()
    }

    private fun startStage() {
        currentCycleCount = 0
        updateUI("Starting Stage ${currentStageIndex + 1}")
        startCycle()
    }

    private fun startCycle() {
        // Check initial speed against target speed
        checkInitialSpeedState()
        startLocation = null
        updateUI("Cycle ${currentCycleCount + 1}")
        updateInstructions()
    }

    private fun checkInitialSpeedState() {
        val currentStage = stages[currentStageIndex]
        val speedDiff = currentSpeed - currentStage.startSpeed

        currentState = when {
            speedDiff > MAX_SPEED_OVERAGE -> State.DECELERATING
            speedDiff < -SPEED_TOLERANCE -> State.ACCELERATING
            abs(speedDiff) <= SPEED_TOLERANCE -> State.HOLDING_SPEED
            else -> State.ACCELERATING
        }
    }

    fun updateSpeed(newSpeed: Double) {
        currentSpeed = newSpeed
        speedTextView.text = "Speed: ${newSpeed.toInt()} mph"

        when (currentState) {
            State.DECELERATING -> handleDecelerating()
            State.ACCELERATING -> handleAccelerating()
            State.HOLDING_SPEED -> handleHoldingSpeed()
            State.BRAKING -> handleBraking()
            State.DRIVING_GAP -> handleDrivingGap()
            else -> {} // No action needed for other states
        }
    }

    fun updateLocation(location: android.location.Location) {
        when (currentState) {
            State.DRIVING_GAP -> {
                if (startLocation == null) {
                    startLocation = location
                } else {
                    val distanceTraveled = startLocation!!.distanceTo(location) * 0.000621371 // Convert meters to miles
                    val currentStage = stages[currentStageIndex]
                    if (distanceTraveled >= currentStage.gapDistance) {
                        completeCycle()
                    }
                }
            }
            else -> {} // No action needed for other states
        }
    }

    private fun handleDecelerating() {
        val currentStage = stages[currentStageIndex]
        val speedDiff = currentSpeed - currentStage.startSpeed

        when {
            speedDiff > MAX_SPEED_OVERAGE -> {
                // Still too fast, stay in DECELERATING state
                updateInstructions()
            }
            abs(speedDiff) <= SPEED_TOLERANCE -> {
                // Within acceptable range
                currentState = State.HOLDING_SPEED
                updateInstructions()
                startSpeedHoldingTimer()
            }
            speedDiff < -SPEED_TOLERANCE -> {
                // Now too slow, switch to accelerating
                currentState = State.ACCELERATING
                updateInstructions()
            }
        }
    }

    private fun handleAccelerating() {
        val currentStage = stages[currentStageIndex]
        val speedDiff = currentSpeed - currentStage.startSpeed

        when {
            speedDiff > MAX_SPEED_OVERAGE -> {
                // Too fast now
                currentState = State.DECELERATING
                updateInstructions()
            }
            abs(speedDiff) <= SPEED_TOLERANCE -> {
                // Within acceptable range
                currentState = State.HOLDING_SPEED
                updateInstructions()
                startSpeedHoldingTimer()
            }
        }
    }

    private fun handleHoldingSpeed() {
        val currentStage = stages[currentStageIndex]
        val speedDiff = currentSpeed - currentStage.startSpeed

        when {
            speedDiff > MAX_SPEED_OVERAGE -> {
                currentState = State.DECELERATING
                updateInstructions()
            }
            speedDiff < -SPEED_TOLERANCE -> {
                currentState = State.ACCELERATING
                updateInstructions()
            }
        }
    }

    private fun handleBraking() {
        val currentStage = stages[currentStageIndex]
        if (currentSpeed <= currentStage.targetSpeed) {
            currentState = State.DRIVING_GAP
            startLocation = null
            updateInstructions()
        }
    }

    private fun handleDrivingGap() {
        // Most logic handled in updateLocation
    }

    private fun startSpeedHoldingTimer() {
        countdownSeconds = 3
        handler.post(object : Runnable {
            override fun run() {
                if (currentState != State.HOLDING_SPEED) return

                if (countdownSeconds > 0) {
                    instructionTextView.text = "Hold speed for $countdownSeconds seconds"
                    countdownSeconds--
                    handler.postDelayed(this, 1000)
                } else {
                    currentState = State.BRAKING
                    updateInstructions()
                }
            }
        })
    }

    private fun completeCycle() {
        currentCycleCount++
        val currentStage = stages[currentStageIndex]

        if (currentCycleCount < currentStage.numberOfStops) {
            startCycle()
        } else {
            completeStage()
        }
    }

    private fun completeStage() {
        currentStageIndex++
        if (currentStageIndex < stages.size) {
            startStage()
        } else {
            currentState = State.IDLE
            updateUI("Procedure Complete!")
        }
    }

    private fun updateInstructions() {
        val currentStage = stages[currentStageIndex]
        val message = when (currentState) {
            State.DECELERATING -> "SLOW DOWN to ${currentStage.startSpeed.toInt()} mph"
            State.ACCELERATING -> "Accelerate to ${currentStage.startSpeed.toInt()} mph"
            State.HOLDING_SPEED -> "Hold speed at ${currentStage.startSpeed.toInt()} mph"
            State.BRAKING -> "BRAKE to ${currentStage.targetSpeed.toInt()} mph"
            State.DRIVING_GAP -> "Drive for ${currentStage.gapDistance} miles"
            else -> ""
        }

        val color = when (currentState) {
            State.DECELERATING -> android.R.color.holo_red_light
            State.BRAKING -> android.R.color.holo_red_dark
            State.ACCELERATING -> android.R.color.holo_orange_light
            State.HOLDING_SPEED -> android.R.color.holo_blue_light
            State.DRIVING_GAP -> android.R.color.holo_green_light
            else -> android.R.color.darker_gray
        }

        instructionTextView.text = message
        statusView.setBackgroundColor(ContextCompat.getColor(context, color))
    }

    private fun updateUI(message: String) {
        instructionTextView.text = message
    }
}