package com.example.ui.screens

import android.app.Application
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.outlined.Circle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.R
import com.example.ui.theme.Primary
import com.example.ui.theme.PrimaryVariant
import com.example.viewmodel.ProcessingViewModel

@Composable
fun ProcessingScreen(
    videoUri: String,
    aspectRatio: String,
    clipCount: Int,
    clipDuration: Int,
    onCancel: () -> Unit,
    onProcessingComplete: () -> Unit
) {
    val context = LocalContext.current.applicationContext as Application
    val viewModel: ProcessingViewModel = viewModel(
        factory = object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return ProcessingViewModel(context) as T
            }
        }
    )

    val progress by viewModel.progress.collectAsState()
    val currentStep by viewModel.currentStep.collectAsState()
    val isComplete by viewModel.isComplete.collectAsState()
    val error by viewModel.error.collectAsState()

    val steps = listOf(
        "Preparing video...",
        "Extracting Audio & Transcribing...",
        "Gemini AI: Finding Viral Moments...",
        "Cropping & Exporting Clips..."
    )

    LaunchedEffect(Unit) {
        viewModel.startProcessing(videoUri, aspectRatio, clipCount, clipDuration)
    }

    LaunchedEffect(isComplete) {
        if (isComplete) {
            onProcessingComplete()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp).fillMaxWidth()
        ) {
            Text(
                text = stringResource(R.string.processing_title),
                fontSize = 28.sp,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.primary
            )
            
            Spacer(modifier = Modifier.height(48.dp))
            
            if (error == null) {
                AiLoadingSpinner()
            } else {
                Text(
                    text = error ?: "Unknown error",
                    color = MaterialTheme.colorScheme.error,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(16.dp)
                )
            }
            
            Spacer(modifier = Modifier.height(48.dp))
            
            if (error == null) {
                // Steps List
                Column(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
                ) {
                    steps.forEachIndexed { index, stepName ->
                        val isCurrentOrDone = progress >= getStepProgressThreshold(index)
                        val isDone = progress > getStepProgressThreshold(index) || isComplete
                        
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(vertical = 8.dp)
                        ) {
                            Icon(
                                imageVector = if (isDone) Icons.Filled.CheckCircle else Icons.Outlined.Circle,
                                contentDescription = null,
                                tint = if (isDone) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Text(
                                text = stepName,
                                color = if (isCurrentOrDone) MaterialTheme.colorScheme.onBackground else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f),
                                fontWeight = if (isCurrentOrDone && !isDone) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 16.sp
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(32.dp))
                
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(12.dp)
                        .clip(CircleShape),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = "${(progress * 100).toInt()}%",
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            }
            
            Spacer(modifier = Modifier.height(48.dp))
            
            OutlinedButton(
                onClick = onCancel,
                modifier = Modifier.fillMaxWidth(0.6f)
            ) {
                Text(
                    if (error != null) "Back" else stringResource(R.string.processing_cancel), 
                    color = MaterialTheme.colorScheme.error,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

fun getStepProgressThreshold(index: Int): Float {
    return when(index) {
        0 -> 0.0f  // Preparing
        1 -> 0.1f  // Extracting audio
        2 -> 0.3f  // Gemini AI
        3 -> 0.5f  // Cropping
        else -> 1.0f
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
            .size(120.dp)
            .rotate(rotation)
            .background(
                brush = Brush.sweepGradient(
                    colors = listOf(
                        Primary.copy(alpha = 0.2f),
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
                .padding(6.dp)
                .background(MaterialTheme.colorScheme.background, CircleShape)
        )
    }
}
