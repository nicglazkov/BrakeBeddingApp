package com.example.brakebeddingapp

import android.content.Context
import android.os.Handler
import android.view.View
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlin.math.abs
import kotlin.math.max

class StageManager(
    private val context: Context,
    private val speedTextView: TextView,
    private val instructionTextView: TextView,
    private val statusView: View,
    private val progressTextView: TextView,
    private val handler: Handler
) {
    private var stages: List<BeddingStage> = listOf()
    private var currentStageIndex = 0
    private var currentCycleCount = 0
    private var currentState = State.IDLE
    private var currentSpeed = 0.0
    private var countdownSeconds = 3
    private var remainingDistance = 0.0
    private var lastUpdateTime = System.currentTimeMillis()
    private var SPEED_TOLERANCE = 2.0  // mph
    private var MAX_SPEED_OVERAGE = 5.0  // mph
    private var MIN_SPEED_UNDERAGE = 1.0  // mph

    private enum class State {
        IDLE,
        DECELERATING,
        ACCELERATING,
        HOLDING_SPEED,
        COUNTDOWN,
        BRAKING,
        DRIVING_GAP
    }

    init {
        loadStages()
        loadThresholds()
        updateUI("Ready to start")
    }

    private fun loadThresholds() {
        val sharedPreferences = context.getSharedPreferences("BrakeBeddingApp", Context.MODE_PRIVATE)
        SPEED_TOLERANCE = sharedPreferences.getString("speed_tolerance", "2.0")?.toDouble() ?: 2.0
        MAX_SPEED_OVERAGE = sharedPreferences.getString("max_speed_overage", "5.0")?.toDouble() ?: 5.0
        MIN_SPEED_UNDERAGE = sharedPreferences.getString("min_speed_underage", "1.0")?.toDouble() ?: 1.0
    }

    private fun handleDrivingGap() {
        val timeEstimate = if (currentSpeed > 0) {
            (remainingDistance / currentSpeed) * 60.0 // minutes
        } else {
            0.0
        }

        val minutesRemaining = timeEstimate.toInt()
        val secondsRemaining = ((timeEstimate - minutesRemaining) * 60).toInt()

        val timeString = if (timeEstimate > 0) {
            if (minutesRemaining > 0) {
                "$minutesRemaining:${String.format("%02d", secondsRemaining)} min"
            } else {
                "$secondsRemaining sec"
            }
        } else {
            ""
        }

        val distanceStr = String.format("%.2f", remainingDistance)
        if (remainingDistance <= 0) {
            val nextStage = if (currentCycleCount + 1 < getCurrentStage().numberOfStops) {
                completeCycle()
            } else {
                completeStage()
            }
            return
        }

        val message = "$distanceStr miles left${if (timeString.isNotEmpty()) ", $timeString" else ""}"
        instructionTextView.text = message
    }

    private fun updateProgress() {
        if (stages.isEmpty()) {
            progressTextView.text = "No stages configured"
            return
        }

        val currentStage = getCurrentStage()
        val stageInfo = StringBuilder()

        // Show stage progress
        stageInfo.append("Stage ${currentStageIndex + 1} of ${stages.size}\n")
        stageInfo.append("Cycle ${currentCycleCount + 1} of ${currentStage.numberOfStops}\n")

        // Show braking intensity if available
        currentStage.brakingIntensity?.let { intensity ->
            stageInfo.append("Using ${intensity.displayName}")
        }

        progressTextView.text = stageInfo.toString()
    }

    private fun loadStages() {
        val sharedPreferences = context.getSharedPreferences("BrakeBeddingApp", Context.MODE_PRIVATE)
        val stagesJson = sharedPreferences.getString("stages", null)
        if (stagesJson != null) {
            try {
                val type = object : TypeToken<List<BeddingStage>>() {}.type
                stages = Gson().fromJson(stagesJson, type) ?: listOf()
            } catch (e: Exception) {
                stages = listOf()
            }
        }
    }

    private fun checkInitialSpeedState() {
        val currentStage = getCurrentStage()
        val speedDiff = currentSpeed - currentStage.startSpeed

        currentState = when {
            speedDiff > MAX_SPEED_OVERAGE -> State.DECELERATING
            speedDiff < -MIN_SPEED_UNDERAGE -> State.ACCELERATING
            abs(speedDiff) <= SPEED_TOLERANCE -> State.HOLDING_SPEED
            else -> State.ACCELERATING
        }
    }

    fun startProcedure() {
        if (stages.isEmpty()) {
            updateUI("No stages available. Please configure stages in Settings.")
            return
        }
        currentStageIndex = 0
        currentCycleCount = 0
        updateProgress()
        startStage()
    }

    private fun startStage() {
        currentCycleCount = 0
        updateUI("Starting Stage ${currentStageIndex + 1}")
        updateProgress()
        startCycle()
    }

    private fun startCycle() {
        val currentStage = getCurrentStage()
        // Initialize states based on current speed
        val speedDiff = currentSpeed - currentStage.startSpeed

        currentState = when {
            speedDiff > MAX_SPEED_OVERAGE -> State.DECELERATING
            speedDiff < -SPEED_TOLERANCE -> State.ACCELERATING
            abs(speedDiff) <= SPEED_TOLERANCE -> State.HOLDING_SPEED
            else -> State.ACCELERATING
        }

        remainingDistance = currentStage.gapDistance
        lastUpdateTime = System.currentTimeMillis()
        updateUI("Cycle ${currentCycleCount + 1} of ${currentStage.numberOfStops}")
        updateProgress()
        updateInstructions()
    }

    fun updateSpeed(newSpeed: Double) {
        val currentTime = System.currentTimeMillis()
        val elapsedTime = (currentTime - lastUpdateTime) / 1000.0 // Convert to seconds
        lastUpdateTime = currentTime

        currentSpeed = newSpeed
        speedTextView.text = "Speed: ${String.format("%.1f", newSpeed)} mph"

        // Update distance if we're in the driving gap phase
        if (currentState == State.DRIVING_GAP && currentSpeed > 0) {
            // Calculate distance traveled since last update
            val distanceTraveled = (currentSpeed * elapsedTime) / 3600.0 // Convert to miles (mph * hours)
            remainingDistance = max(0.0, remainingDistance - distanceTraveled)
            updateGapDistance()

            if (remainingDistance <= 0) {
                completeCycle()
            }
        }

        when (currentState) {
            State.DECELERATING -> handleDecelerating()
            State.ACCELERATING -> handleAccelerating()
            State.HOLDING_SPEED -> handleHoldingSpeed()
            State.BRAKING -> handleBraking()
            State.DRIVING_GAP -> updateGapDistance()
            else -> {} // No action needed for other states
        }
    }

    private fun getCurrentStage(): BeddingStage {
        if (stages.isEmpty()) {
            throw IllegalStateException("No stages available")
        }
        return stages[currentStageIndex]
    }

    private fun handleDecelerating() {
        val currentStage = getCurrentStage()
        val speedDiff = currentSpeed - currentStage.startSpeed

        when {
            speedDiff > MAX_SPEED_OVERAGE -> {
                updateInstructions()
            }
            abs(speedDiff) <= SPEED_TOLERANCE -> {
                currentState = State.HOLDING_SPEED
                updateInstructions()
                startSpeedHoldingTimer()
            }
            speedDiff < -SPEED_TOLERANCE -> {
                currentState = State.ACCELERATING
                updateInstructions()
            }
        }
    }

    private fun handleAccelerating() {
        val currentStage = getCurrentStage()
        val speedDiff = currentSpeed - currentStage.startSpeed

        when {
            speedDiff > MAX_SPEED_OVERAGE -> {
                currentState = State.DECELERATING
                updateInstructions()
            }
            abs(speedDiff) <= SPEED_TOLERANCE -> {
                currentState = State.HOLDING_SPEED
                updateInstructions()
                startSpeedHoldingTimer()
            }
        }
    }

    private fun handleHoldingSpeed() {
        val currentStage = getCurrentStage()
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
        val currentStage = getCurrentStage()
        if (currentSpeed <= currentStage.targetSpeed) {
            currentState = State.DRIVING_GAP
            remainingDistance = currentStage.gapDistance
            updateInstructions()
        }
    }

    private fun updateGapDistance() {
        val timeEstimate = if (currentSpeed > 0) {
            (remainingDistance / currentSpeed) * 60.0 // minutes
        } else {
            0.0
        }

        val minutesRemaining = timeEstimate.toInt()
        val secondsRemaining = ((timeEstimate - minutesRemaining) * 60).toInt()

        val timeString = if (timeEstimate > 0) {
            if (minutesRemaining > 0) {
                "$minutesRemaining:${String.format("%02d", secondsRemaining)} min"
            } else {
                "$secondsRemaining sec"
            }
        } else {
            ""
        }

        val distanceStr = String.format("%.2f", remainingDistance)
        val message = if (remainingDistance > 0) {
            "$distanceStr miles left${if (timeString.isNotEmpty()) ", $timeString" else ""}"
        } else {
            "Distance complete"
        }

        instructionTextView.text = message
    }

    private fun startSpeedHoldingTimer() {
        countdownSeconds = 3
        handler.post(object : Runnable {
            override fun run() {
                if (currentState != State.HOLDING_SPEED) return

                // Check if we're still within tolerance
                val speedDiff = currentSpeed - getCurrentStage().startSpeed
                if (abs(speedDiff) > SPEED_TOLERANCE) {
                    currentState = if (speedDiff > 0) State.DECELERATING else State.ACCELERATING
                    updateInstructions()
                    return
                }

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

    fun reloadStages() {
        loadStages()
        // Reset state if we're not currently running a procedure
        if (currentState == State.IDLE) {
            currentStageIndex = 0
            currentCycleCount = 0
            if (stages.isEmpty()) {
                updateUI("No stages configured. Please add stages in Settings.")
            } else {
                updateUI("Ready to start")
            }
            updateProgress()
        }
    }


    private fun completeCycle() {
        currentCycleCount++
        val currentStage = getCurrentStage()

        if (currentCycleCount < currentStage.numberOfStops) {
            // When starting the next cycle, immediately check if we're at the right speed
            val speedDiff = currentSpeed - currentStage.startSpeed
            currentState = when {
                speedDiff > MAX_SPEED_OVERAGE -> State.DECELERATING
                speedDiff < -SPEED_TOLERANCE -> State.ACCELERATING
                else -> State.HOLDING_SPEED
            }

            remainingDistance = currentStage.gapDistance
            updateProgress()
            updateInstructions()

            // If we're already at holding speed, start the countdown
            if (currentState == State.HOLDING_SPEED) {
                startSpeedHoldingTimer()
            }
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
            progressTextView.text = "All stages complete!"
        }
    }

    private fun updateInstructions() {
        val currentStage = getCurrentStage()
        val message = StringBuilder()

        when (currentState) {
            State.DECELERATING -> {
                message.append("SLOW DOWN to ${currentStage.startSpeed.toInt()} mph")
            }
            State.ACCELERATING -> {
                message.append("Accelerate to ${currentStage.startSpeed.toInt()} mph")
            }
            State.HOLDING_SPEED -> {
                message.append("Hold speed at ${currentStage.startSpeed.toInt()} mph")
            }
            State.BRAKING -> {
                currentStage.brakingIntensity?.let { intensity ->
                    message.append("${intensity.displayName}\n")
                }
                message.append("BRAKE to ${currentStage.targetSpeed.toInt()} mph")
            }
            State.DRIVING_GAP -> {
                message.append("${String.format("%.2f", remainingDistance)} miles left")
            }
            else -> {}
        }

        val color = when (currentState) {
            State.DECELERATING -> android.R.color.holo_red_light
            State.BRAKING -> android.R.color.holo_red_dark
            State.ACCELERATING -> android.R.color.holo_orange_light
            State.HOLDING_SPEED -> android.R.color.holo_blue_light
            State.DRIVING_GAP -> android.R.color.holo_green_light
            else -> android.R.color.darker_gray
        }

        instructionTextView.text = message.toString()
        statusView.setBackgroundColor(ContextCompat.getColor(context, color))
    }

    private fun updateUI(message: String) {
        instructionTextView.text = message
    }
}