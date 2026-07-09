package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StyleSettingsScreen(
    onBack: () -> Unit,
    onContinue: (font: String, color: String, position: String, animation: String, showTitle: Boolean) -> Unit
) {
    var selectedFont by remember { mutableStateOf("Default") }
    var selectedColor by remember { mutableStateOf("White") }
    var selectedPosition by remember { mutableStateOf("Center") }
    var selectedAnimation by remember { mutableStateOf("Fade") }
    var showTitle by remember { mutableStateOf(false) }

    val fonts = listOf("Default", "Monospace", "Serif", "SansSerif", "Comic")
    val colors = listOf("White" to Color.White, "Yellow" to Color.Yellow, "Green" to Color.Green, "Cyan" to Color.Cyan, "Red" to Color.Red)
    val positions = listOf("Top", "Center", "Bottom")
    val animations = listOf("Fade", "Pop", "Slide Up", "Typewriter")

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Caption Styling") },
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
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                item {
                    Text("Caption Font", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        fonts.take(3).forEach { font ->
                            FilterChip(
                                selected = selectedFont == font,
                                onClick = { selectedFont = font },
                                label = { Text(font) }
                            )
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        fonts.drop(3).forEach { font ->
                            FilterChip(
                                selected = selectedFont == font,
                                onClick = { selectedFont = font },
                                label = { Text(font) }
                            )
                        }
                    }
                }

                item {
                    Text("Caption Color", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        colors.forEach { (name, color) ->
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(color)
                                    .border(2.dp, if (selectedColor == name) MaterialTheme.colorScheme.primary else Color.Transparent, RoundedCornerShape(20.dp))
                                    .clickable { selectedColor = name }
                                    .padding(4.dp)
                            ) {
                                if (selectedColor == name) {
                                    Icon(
                                        Icons.Default.Check,
                                        contentDescription = null,
                                        tint = if (color == Color.White || color == Color.Yellow) Color.Black else Color.White,
                                        modifier = Modifier.align(Alignment.Center)
                                    )
                                }
                            }
                        }
                    }
                }

                item {
                    Text("Caption Position", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        positions.forEach { pos ->
                            FilterChip(
                                selected = selectedPosition == pos,
                                onClick = { selectedPosition = pos },
                                label = { Text(pos) }
                            )
                        }
                    }
                }

                item {
                    Text("Text Animation", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        animations.take(3).forEach { anim ->
                            FilterChip(
                                selected = selectedAnimation == anim,
                                onClick = { selectedAnimation = anim },
                                label = { Text(anim) }
                            )
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        animations.drop(3).forEach { anim ->
                            FilterChip(
                                selected = selectedAnimation == anim,
                                onClick = { selectedAnimation = anim },
                                label = { Text(anim) }
                            )
                        }
                    }
                }
                
                item {
                    Text("Extra Elements", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = showTitle,
                            onCheckedChange = { showTitle = it }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Add Video Title inside Video")
                    }
                }
            }

            Button(
                onClick = { onContinue(selectedFont, selectedColor, selectedPosition, selectedAnimation, showTitle) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
            ) {
                Text("Continue to Processing", fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}
