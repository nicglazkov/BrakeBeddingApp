package com.example.brakebeddingapp

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

enum class BrakingIntensity(val displayName: String) {
    LIGHT("Light braking"),
    MODERATE("Moderate braking"),
    FIRM("Firm braking"),
    THRESHOLD("Full brakes (threshold braking)"),
    ABS("Full brakes (activate ABS)");

    companion object {
        fun fromDisplayName(name: String): BrakingIntensity {
            return values().firstOrNull { it.displayName == name } ?: MODERATE
        }
    }
}

sealed class Stage : Parcelable {
    abstract val type: String
}

@Parcelize
data class BeddingStage(
    val numberOfStops: Int,
    val startSpeed: Double,
    val targetSpeed: Double,
    val gapDistance: Double,
    val brakingIntensity: BrakingIntensity
) : Stage() {
    override val type = "bedding"
}

@Parcelize
data class CooldownStage(
    val distance: Double
) : Stage() {
    override val type = "cooldown"
}