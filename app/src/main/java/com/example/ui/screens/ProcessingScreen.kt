package com.example.ui.screens

import android.app.Application
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
            modifier = Modifier.padding(32.dp)
        ) {
            Text(
                text = stringResource(R.string.processing_title),
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            
            Spacer(modifier = Modifier.height(32.dp))
            
            if (error == null) {
                AiLoadingSpinner()
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            Text(
                text = error ?: currentStep,
                color = if (error != null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onBackground,
                fontSize = 16.sp
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            if (error == null) {
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
            }
            
            Spacer(modifier = Modifier.height(48.dp))
            
            OutlinedButton(onClick = onCancel) {
                Text(
                    if (error != null) "Back" else stringResource(R.string.processing_cancel), 
                    color = MaterialTheme.colorScheme.error
                )
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
