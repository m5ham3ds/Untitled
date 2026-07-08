package com.example.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.theme.Primary
import com.example.ui.theme.PrimaryVariant
import kotlinx.coroutines.delay

@Composable
fun ProcessingScreen(
    onCancel: () -> Unit,
    onProcessingComplete: () -> Unit
) {
    // Simulate processing
    var progress by remember { mutableStateOf(0f) }
    var currentStep by remember { mutableStateOf("Initializing AI Models...") }

    LaunchedEffect(Unit) {
        val steps = listOf(
            "Analyzing Audio...",
            "Transcribing Text...",
            "Extracting Highlights...",
            "Auto-Reframing to 9:16...",
            "Generating Captions...",
            "Exporting Clips..."
        )
        
        for (i in steps.indices) {
            currentStep = steps[i]
            val stepProgress = 1f / steps.size
            for (j in 1..10) {
                delay(150)
                progress += stepProgress / 10f
            }
        }
        delay(500)
        onProcessingComplete()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp)
        ) {
            Text(
                text = stringResource(R.string.processing_title),
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // Custom AI Loading Spinner
            AiLoadingSpinner()
            
            Spacer(modifier = Modifier.height(32.dp))
            
            Text(
                text = currentStep,
                color = MaterialTheme.colorScheme.onBackground,
                fontSize = 16.sp
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(CircleShape),
                color = MaterialTheme.colorScheme.secondary,
                trackColor = MaterialTheme.colorScheme.surface
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = "${(progress * 100).toInt()}%",
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
            )

            Spacer(modifier = Modifier.height(48.dp))
            
            OutlinedButton(onClick = onCancel) {
                Text(stringResource(R.string.processing_cancel), color = MaterialTheme.colorScheme.error)
            }
        }
    }
}

@Composable
fun AiLoadingSpinner() {
    val infiniteTransition = rememberInfiniteTransition(label = "spinner")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ), label = "rotation"
    )

    Box(
        modifier = Modifier
            .size(100.dp)
            .rotate(rotation)
            .background(
                brush = Brush.sweepGradient(
                    colors = listOf(
                        Primary,
                        PrimaryVariant,
                        Primary
                    )
                ),
                shape = CircleShape
            )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(4.dp)
                .background(MaterialTheme.colorScheme.background, CircleShape)
        )
    }
}
