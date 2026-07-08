package com.example.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProcessingSettingsScreen(
    videoUri: String,
    onBack: () -> Unit,
    onStartProcessing: (aspectRatio: String, autoCaptions: Boolean) -> Unit
) {
    var selectedAspectRatio by remember { mutableStateOf("9:16") }
    var autoCaptions by remember { mutableStateOf(true) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.processing_settings_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(16.dp)
        ) {
            LazyColumn(modifier = Modifier.weight(1f)) {
                item {
                    Text(
                        stringResource(R.string.settings_aspect_ratio_title),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    val aspectRatios = listOf(
                        "9:16" to stringResource(R.string.settings_aspect_tiktok),
                        "9:16_reels" to stringResource(R.string.settings_aspect_reels),
                        "9:16_shorts" to stringResource(R.string.settings_aspect_shorts),
                        "1:1" to stringResource(R.string.settings_aspect_square),
                        "16:9" to stringResource(R.string.settings_aspect_landscape)
                    )

                    aspectRatios.forEach { (value, label) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = selectedAspectRatio == value,
                                onClick = { selectedAspectRatio = value }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(label)
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))
                    
                    Text(
                        "Processing Features",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = autoCaptions,
                            onCheckedChange = { autoCaptions = it }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Auto Captions & Highlights")
                    }
                }
            }

            Button(
                onClick = { onStartProcessing(selectedAspectRatio, autoCaptions) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
            ) {
                Text(stringResource(R.string.processing_start), fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}
