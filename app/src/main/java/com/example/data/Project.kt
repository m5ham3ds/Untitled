package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "projects")
data class Project(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val videoUri: String,
    val title: String,
    val status: String, // e.g. "Processing", "Ready", "Failed"
    val timestamp: Long = System.currentTimeMillis()
)
