package com.example.ui.screens

import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.json.JSONObject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorScreen(
    videoUri: String,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    
    val exoPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            val mediaItem = MediaItem.fromUri(Uri.parse(videoUri))
            setMediaItem(mediaItem)
            prepare()
            playWhenReady = true
            repeatMode = ExoPlayer.REPEAT_MODE_ALL
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            exoPlayer.release()
        }
    }

    var isEditingText by remember { mutableStateOf(false) }
    var showStylePanel by remember { mutableStateOf(false) }
    var captionText by remember { mutableStateOf("WELCOME TO THE PODCAST. TODAY WE ARE TALKING ABOUT AI.") }
    var captionColor by remember { mutableStateOf(Color.White) }
    var captionPosition by remember { mutableStateOf("Center") }
    var aspectRatio by remember { mutableStateOf("9:16") }
    var captionFont by remember { mutableStateOf("Default") }
    
    var showExportDialog by remember { mutableStateOf(false) }
    var exportProgress by remember { mutableStateOf(0f) }
    var exportComplete by remember { mutableStateOf(false) }

    // Load state from SharedPreferences
    LaunchedEffect(videoUri) {
        val prefs = context.getSharedPreferences("editor_prefs", Context.MODE_PRIVATE)
        val stateJson = prefs.getString(videoUri, null)
        if (stateJson != null) {
            try {
                val json = JSONObject(stateJson)
                captionText = json.optString("text", captionText)
                val colorInt = json.optInt("color", android.graphics.Color.WHITE)
                captionColor = Color(colorInt)
                captionPosition = json.optString("position", captionPosition)
                aspectRatio = json.optString("aspectRatio", aspectRatio)
                captionFont = json.optString("font", captionFont)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    val saveState = {
        val prefs = context.getSharedPreferences("editor_prefs", Context.MODE_PRIVATE)
        val json = JSONObject()
        json.put("text", captionText)
        json.put("color", android.graphics.Color.argb(
            (captionColor.alpha * 255).toInt(),
            (captionColor.red * 255).toInt(),
            (captionColor.green * 255).toInt(),
            (captionColor.blue * 255).toInt()
        ))
        json.put("position", captionPosition)
        json.put("aspectRatio", aspectRatio)
        json.put("font", captionFont)
        
        prefs.edit().putString(videoUri, json.toString()).apply()
        Toast.makeText(context, "Project Saved", Toast.LENGTH_SHORT).show()
    }

    if (showExportDialog) {
        AlertDialog(
            onDismissRequest = { if (exportComplete) showExportDialog = false },
            confirmButton = {
                if (exportComplete) {
                    TextButton(onClick = { showExportDialog = false; onBack() }) {
                        Text("Done")
                    }
                }
            },
            title = { Text(if (exportComplete) "Export Complete" else "Exporting Video...") },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    if (!exportComplete) {
                        LinearProgressIndicator(
                            progress = { exportProgress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp))
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("${(exportProgress * 100).toInt()}%")
                    } else {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Success",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Video saved to gallery successfully!")
                    }
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Edit Video") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = saveState) {
                        Icon(Icons.Default.Save, contentDescription = "Save Project")
                    }
                    FilledTonalButton(
                        onClick = { 
                            showExportDialog = true
                            exportProgress = 0f
                            exportComplete = false
                            coroutineScope.launch {
                                while (exportProgress < 1f) {
                                    delay(100)
                                    exportProgress += 0.05f
                                }
                                exportComplete = true
                            }
                        },
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Icon(Icons.Default.Download, contentDescription = "Export")
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Export", fontWeight = FontWeight.Bold)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Black.copy(alpha = 0.5f),
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White,
                    actionIconContentColor = Color.White
                )
            )
        },
        containerColor = Color.Black
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Simulated Aspect Ratio Container
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                // Video Player Container with specific aspect ratio
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(
                            when (aspectRatio) {
                                "9:16" -> 9f / 16f
                                "1:1" -> 1f
                                "16:9" -> 16f / 9f
                                else -> 9f / 16f
                            }
                        )
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.DarkGray)
                ) {
                    AndroidView(
                        factory = { ctx ->
                            PlayerView(ctx).apply {
                                player = exoPlayer
                                useController = false
                                layoutParams = android.view.ViewGroup.LayoutParams(
                                    android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                                    android.view.ViewGroup.LayoutParams.MATCH_PARENT
                                )
                                resizeMode = androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                            }
                        },
                        modifier = Modifier.fillMaxSize()
                    )

                    // Caption Overlay
                    val align = when (captionPosition) {
                        "Top" -> Alignment.TopCenter
                        "Center" -> Alignment.Center
                        "Bottom" -> Alignment.BottomCenter
                        else -> Alignment.Center
                    }

                    val font = when (captionFont) {
                        "Monospace" -> FontFamily.Monospace
                        "Serif" -> FontFamily.Serif
                        "SansSerif" -> FontFamily.SansSerif
                        else -> FontFamily.Default
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(align)
                            .padding(horizontal = 24.dp, vertical = 32.dp)
                            .clickable { isEditingText = true },
                        contentAlignment = Alignment.Center
                    ) {
                        if (isEditingText) {
                            BasicTextField(
                                value = captionText,
                                onValueChange = { captionText = it.uppercase() },
                                textStyle = TextStyle(
                                    color = captionColor,
                                    fontSize = 28.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontFamily = font,
                                    textAlign = TextAlign.Center,
                                    shadow = androidx.compose.ui.graphics.Shadow(
                                        color = Color.Black,
                                        offset = androidx.compose.ui.geometry.Offset(4f, 4f),
                                        blurRadius = 8f
                                    )
                                ),
                                cursorBrush = SolidColor(captionColor),
                                modifier = Modifier
                                    .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(8.dp))
                                    .padding(16.dp)
                                    .fillMaxWidth()
                            )
                        } else {
                            Text(
                                text = captionText,
                                color = captionColor,
                                fontSize = 28.sp,
                                fontWeight = FontWeight.ExtraBold,
                                fontFamily = font,
                                textAlign = TextAlign.Center,
                                style = TextStyle(
                                    shadow = androidx.compose.ui.graphics.Shadow(
                                        color = Color.Black,
                                        offset = androidx.compose.ui.geometry.Offset(4f, 4f),
                                        blurRadius = 8f
                                    )
                                ),
                                modifier = Modifier
                                    .background(Color.Black.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                                    .padding(16.dp)
                                    .fillMaxWidth()
                            )
                        }
                    }
                }
            }

            // Edit Controls
            if (isEditingText) {
                FloatingActionButton(
                    onClick = { isEditingText = false },
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(32.dp),
                    containerColor = MaterialTheme.colorScheme.primary
                ) {
                    Icon(Icons.Default.Check, contentDescription = "Done Editing")
                }
            } else {
                Row(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(32.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    FloatingActionButton(
                        onClick = { showStylePanel = true },
                        containerColor = MaterialTheme.colorScheme.secondary
                    ) {
                        Icon(Icons.Default.Palette, contentDescription = "Style Options")
                    }
                    FloatingActionButton(
                        onClick = { isEditingText = true },
                        containerColor = MaterialTheme.colorScheme.primary
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit Text")
                    }
                }
            }
            
            // Instruction
            if (!isEditingText && !showStylePanel) {
                Text(
                    text = "Tap the caption to edit text",
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 14.sp,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 48.dp)
                )
            }
        }
    }

    if (showStylePanel) {
        ModalBottomSheet(
            onDismissRequest = { showStylePanel = false },
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
                    .padding(bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                Text("Customize Style", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                
                // Aspect Ratio Selection
                Column {
                    Text("Aspect Ratio (Output)", style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        listOf("9:16" to "Portrait", "1:1" to "Square", "16:9" to "Landscape").forEach { (ratio, name) ->
                            FilterChip(
                                selected = aspectRatio == ratio,
                                onClick = { aspectRatio = ratio },
                                label = { Text(name) }
                            )
                        }
                    }
                }

                // Position Selection
                Column {
                    Text("Caption Position", style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        listOf("Top", "Center", "Bottom").forEach { pos ->
                            FilterChip(
                                selected = captionPosition == pos,
                                onClick = { captionPosition = pos },
                                label = { Text(pos) }
                            )
                        }
                    }
                }

                // Color Selection
                Column {
                    Text("Caption Color", style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        val colors = listOf(Color.White, Color.Yellow, Color.Green, Color.Cyan, Color.Red)
                        colors.forEach { col ->
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(col)
                                    .clickable { captionColor = col }
                                    .padding(4.dp)
                            ) {
                                if (captionColor == col) {
                                    Icon(
                                        Icons.Default.Check, 
                                        contentDescription = null, 
                                        tint = if (col == Color.White || col == Color.Yellow) Color.Black else Color.White,
                                        modifier = Modifier.align(Alignment.Center)
                                    )
                                }
                            }
                        }
                    }
                }

                // Font Selection
                Column {
                    Text("Caption Font", style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        listOf("Default", "Monospace", "Serif", "SansSerif").forEach { font ->
                            FilterChip(
                                selected = captionFont == font,
                                onClick = { captionFont = font },
                                label = { Text(font) }
                            )
                        }
                    }
                }
            }
        }
    }
}
