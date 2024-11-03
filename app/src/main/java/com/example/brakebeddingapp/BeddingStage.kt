package com.example.brakebeddingapp

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class BeddingStage(
    val numberOfStops: Int,
    val startSpeed: Double,
    val targetSpeed: Double,
    val gapDistance: Double
) : Parcelable
