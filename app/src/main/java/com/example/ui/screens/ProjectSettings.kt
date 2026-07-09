package com.example.ui.screens

import androidx.compose.ui.graphics.Color

object ProjectSettings {
    var captionFont: String = "Default"
    var captionColor: String = "White"
    var captionPosition: String = "Center"
    var captionAnimation: String = "Fade"
    var showVideoTitle: Boolean = false
    
    fun getColorFromName(name: String): Color {
        return when (name) {
            "Yellow" -> Color.Yellow
            "Green" -> Color.Green
            "Cyan" -> Color.Cyan
            "Red" -> Color.Red
            else -> Color.White
        }
    }
}
