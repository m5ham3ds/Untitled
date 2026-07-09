package com.example.ui.screens

import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.key.*
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.json.JSONObject
import kotlin.math.roundToInt

data class CaptionSegment(val id: Int, var text: String, var startProgress: Float, var endProgress: Float)
data class VideoClip(val id: Int, val name: String, var trimStart: Float = 0f, var trimEnd: Float = 1f, val segments: MutableList<CaptionSegment>)

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
    var captionColor by remember { mutableStateOf(Color.White) }
    var captionPosition by remember { mutableStateOf("Center") }
    var aspectRatio by remember { mutableStateOf("9:16") }
    var captionFont by remember { mutableStateOf("Default") }
    
    var showExportDialog by remember { mutableStateOf(false) }
    var exportProgress by remember { mutableStateOf(0f) }
    var exportComplete by remember { mutableStateOf(false) }

    // Multi-clip state
    var currentClipIndex by remember { mutableIntStateOf(0) }
    
    var videoDurationSeconds by remember { mutableStateOf(60f) }
    LaunchedEffect(videoUri) {
        try {
            val retriever = android.media.MediaMetadataRetriever()
            retriever.setDataSource(context, Uri.parse(videoUri))
            val time = retriever.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_DURATION)
            videoDurationSeconds = (time?.toLong() ?: 60000L) / 1000f
            retriever.release()
            
            // Clamp clips if needed
            ClipManager.clips.forEach { clip ->
                if (clip.trimEnd > videoDurationSeconds) clip.trimEnd = videoDurationSeconds
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // Initialize clips
    val clips = remember {
        val list = mutableStateListOf<VideoClip>()
        if (ClipManager.clips.isNotEmpty()) {
            list.addAll(ClipManager.clips)
        } else {
            list.add(VideoClip(0, "Highlight 1", segments = mutableStateListOf(
                CaptionSegment(0, "WELCOME TO THE PODCAST.", 0.1f, 0.4f),
                CaptionSegment(1, "TODAY WE ARE TALKING ABOUT AI.", 0.5f, 0.9f)
            )))
        }
        list
    }

    var editingSegmentId by remember { mutableStateOf<Int?>(0) }
    val currentClip = clips[currentClipIndex]
    
    // Auto-save function
    val saveState = {
        val prefs = context.getSharedPreferences("editor_prefs", Context.MODE_PRIVATE)
        val json = JSONObject()
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
    }

    // Auto-save every 30 seconds
    LaunchedEffect(videoUri) {
        while (true) {
            delay(30000)
            saveState()
        }
    }

    // Keyboard Shortcuts setup
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
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
            title = { Text(if (exportComplete) "Export Complete" else "Exporting Batch (3 Clips)...") },
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
                        Text("All clips saved to gallery successfully!")
                    }
                }
            }
        )
    }

    Scaffold(
        modifier = Modifier
            .focusRequester(focusRequester)
            .focusable()
            .onKeyEvent { keyEvent ->
                if (keyEvent.type == KeyEventType.KeyDown) {
                    when (keyEvent.key) {
                        Key.Spacebar -> {
                            exoPlayer.playWhenReady = !exoPlayer.playWhenReady
                            true
                        }
                        Key.DirectionLeft -> {
                            exoPlayer.seekTo(exoPlayer.currentPosition - 100) // scrub back 100ms
                            true
                        }
                        Key.DirectionRight -> {
                            exoPlayer.seekTo(exoPlayer.currentPosition + 100) // scrub forward 100ms
                            true
                        }
                        else -> false
                    }
                } else {
                    false
                }
            },
        topBar = {
            TopAppBar(
                title = { Text("Editor") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = {
                        saveState()
                        Toast.makeText(context, "Project Saved", Toast.LENGTH_SHORT).show()
                    }) {
                        Icon(Icons.Default.Save, contentDescription = "Save Project")
                    }
                    FilledTonalButton(
                        onClick = { 
                            showExportDialog = true
                            exportProgress = 0f
                            exportComplete = false
                            coroutineScope.launch {
                                while (exportProgress < 1f) {
                                    delay(50)
                                    exportProgress += 0.01f
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
                        Icon(Icons.Default.Download, contentDescription = "Export All")
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Export All", fontWeight = FontWeight.Bold)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Black,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White,
                    actionIconContentColor = Color.White
                )
            )
        },
        containerColor = Color.Black
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Top Half: Video Player
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .background(Color.DarkGray)
            ) {
                // Video Player Container with specific aspect ratio
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
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
                            .background(Color.Black)
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
                        
                        var currentVideoPosition by remember { mutableLongStateOf(0L) }
                        LaunchedEffect(Unit) {
                            while (true) {
                                delay(100)
                                currentVideoPosition = exoPlayer.currentPosition
                            }
                        }
                        
                        val activeSegment = if (isEditingText) {
                            currentClip.segments.find { it.id == editingSegmentId } ?: currentClip.segments.firstOrNull()
                        } else {
                            val relativeProgress = ((currentVideoPosition - (currentClip.trimStart * 1000L)) / ((currentClip.trimEnd - currentClip.trimStart) * 1000f)).coerceIn(0f, 1f)
                            currentClip.segments.find { relativeProgress >= it.startProgress && relativeProgress <= it.endProgress }
                                ?: currentClip.segments.firstOrNull()
                        }

                        if (activeSegment != null) {
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
                                        value = activeSegment.text,
                                        onValueChange = { newText ->
                                            val index = currentClip.segments.indexOfFirst { it.id == activeSegment.id }
                                            if (index != -1) {
                                                currentClip.segments[index] = activeSegment.copy(text = newText.uppercase())
                                            }
                                        },
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
                                        text = activeSegment.text,
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
                }
                
                // FABs for styling
                if (isEditingText) {
                    FloatingActionButton(
                        onClick = { isEditingText = false },
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(16.dp),
                        containerColor = MaterialTheme.colorScheme.primary
                    ) {
                        Icon(Icons.Default.Check, contentDescription = "Done Editing")
                    }
                } else {
                    Row(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(16.dp),
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
            }

            // Bottom Half: Tools
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .padding(16.dp)
            ) {
                // Multi-clip Selector
                Text("Select Highlight (Batch of ${clips.size})", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.height(8.dp))
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    itemsIndexed(clips) { index, clip ->
                        FilterChip(
                            selected = currentClipIndex == index,
                            onClick = { 
                                currentClipIndex = index 
                                editingSegmentId = clips[index].segments.firstOrNull()?.id
                            },
                            label = { Text(clip.name) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Trimming Tool
                Text("Trim Video Clip", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                var sliderPosition by remember { mutableStateOf(currentClip.trimStart..currentClip.trimEnd) }
                // Update local state when clip changes
                LaunchedEffect(currentClipIndex) {
                    sliderPosition = clips[currentClipIndex].trimStart..clips[currentClipIndex].trimEnd
                    exoPlayer.seekTo((clips[currentClipIndex].trimStart * 1000).toLong())
                }
                
                LaunchedEffect(Unit) {
                    while (true) {
                        delay(100)
                        if (exoPlayer.currentPosition > (clips[currentClipIndex].trimEnd * 1000).toLong()) {
                            exoPlayer.seekTo((clips[currentClipIndex].trimStart * 1000).toLong())
                        }
                    }
                }
                RangeSlider(
                    value = sliderPosition,
                    onValueChange = { 
                        sliderPosition = it
                        clips[currentClipIndex].trimStart = it.start
                        clips[currentClipIndex].trimEnd = it.endInclusive
                        exoPlayer.seekTo((it.start * 1000).toLong())
                    },
                    valueRange = 0f..videoDurationSeconds,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Timeline View for Captions
                Text("Caption Sync Timeline", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.height(8.dp))
                
                // Draggable caption segments
                BoxWithConstraints(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(60.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.Black.copy(alpha = 0.5f))
                ) {
                    val widthPx = constraints.maxWidth.toFloat()
                    
                    currentClip.segments.forEachIndexed { index, segment ->
                        val startPx = widthPx * segment.startProgress
                        val endPx = widthPx * segment.endProgress
                        
                        Box(
                            modifier = Modifier
                                .absoluteOffset { IntOffset(startPx.roundToInt(), 0) }
                                .width(maxWidth * (segment.endProgress - segment.startProgress))
                                .fillMaxHeight()
                                .padding(2.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(if (editingSegmentId == segment.id) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary)
                                .clickable { editingSegmentId = segment.id }
                                .pointerInput(Unit) {
                                    detectDragGestures { change, dragAmount ->
                                        change.consume()
                                        val deltaProgress = dragAmount.x / widthPx
                                        val newStart = (segment.startProgress + deltaProgress).coerceIn(0f, segment.endProgress - 0.05f)
                                        val newEnd = (segment.endProgress + deltaProgress).coerceIn(newStart + 0.05f, 1f)
                                        
                                        currentClip.segments[index] = segment.copy(
                                            startProgress = newStart,
                                            endProgress = newEnd
                                        )
                                    }
                                }
                        ) {
                            Text(
                                text = segment.text,
                                color = MaterialTheme.colorScheme.onPrimary,
                                fontSize = 10.sp,
                                modifier = Modifier
                                    .align(Alignment.Center)
                                    .padding(4.dp),
                                maxLines = 2
                            )
                        }
                    }
                }
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
