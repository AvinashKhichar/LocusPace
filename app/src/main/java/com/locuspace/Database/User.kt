package com.locuspace.Database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user")
data class User(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val username: String,
    val photoUri: String? = null   // or photoBase64, etc.
)
