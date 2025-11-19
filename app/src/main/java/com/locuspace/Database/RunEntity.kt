package com.locuspace.Database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "runs")
data class RunEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Long,          // System.currentTimeMillis()
    val durationMillis: Long,     // total time of run
    val distanceMeters: Double,   // total distance
    val avgSpeedKmh: Double       // average speed
)