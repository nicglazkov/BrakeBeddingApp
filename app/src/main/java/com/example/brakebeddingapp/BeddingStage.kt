package com.example.brakebeddingapp

import android.os.Parcel
import android.os.Parcelable

data class BeddingStage(
    val numberOfStops: Int,
    val startSpeed: Double,
    val targetSpeed: Double,
    val gapDistance: Double // in miles
) : Parcelable {
    constructor(parcel: Parcel) : this(
        parcel.readInt(),
        parcel.readDouble(),
        parcel.readDouble(),
        parcel.readDouble()
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeInt(numberOfStops)
        parcel.writeDouble(startSpeed)
        parcel.writeDouble(targetSpeed)
        parcel.writeDouble(gapDistance)
    }

    override fun describeContents(): Int = 0

    companion object CREATOR : Parcelable.Creator<BeddingStage> {
        override fun createFromParcel(parcel: Parcel): BeddingStage = BeddingStage(parcel)
        override fun newArray(size: Int): Array<BeddingStage?> = arrayOfNulls(size)
    }
}
