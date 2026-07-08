package com.example.ui.screens

import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImportScreen(
    onBack: () -> Unit,
    onContinue: (String) -> Unit
) {
    val context = LocalContext.current
    var selectedVideoUri by remember { mutableStateOf<Uri?>(null) }
    var videoName by remember { mutableStateOf("") }
    var videoUrl by remember { mutableStateOf("") }
    var tabIndex by remember { mutableStateOf(0) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val errorSizeMsg = stringResource(R.string.import_error_size)
    val errorFormatMsg = stringResource(R.string.import_error_format)

    val videoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            val contentResolver = context.contentResolver
            val mimeType = contentResolver.getType(uri)
            
            var size = 0L
            var name = ""
            contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (cursor.moveToFirst()) {
                    if (sizeIndex != -1) size = cursor.getLong(sizeIndex)
                    if (nameIndex != -1) name = cursor.getString(nameIndex)
                }
            }

            // Validate format (e.g. mp4)
            if (mimeType != null && !mimeType.startsWith("video/")) {
                errorMessage = errorFormatMsg
                selectedVideoUri = null
            } else if (size > 100 * 1024 * 1024) { // Max 100MB for example
                errorMessage = errorSizeMsg
                selectedVideoUri = null
            } else {
                errorMessage = null
                selectedVideoUri = uri
                videoName = name.ifEmpty { "Selected Video" }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.import_title)) },
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
            TabRow(selectedTabIndex = tabIndex) {
                Tab(
                    selected = tabIndex == 0,
                    onClick = { tabIndex = 0 },
                    text = { Text(stringResource(R.string.import_from_device)) }
                )
                Tab(
                    selected = tabIndex == 1,
                    onClick = { tabIndex = 1 },
                    text = { Text(stringResource(R.string.import_from_url)) }
                )
            }
            
            Spacer(modifier = Modifier.height(32.dp))

            if (tabIndex == 0) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Button(onClick = { videoPickerLauncher.launch("video/*") }) {
                            Icon(Icons.Default.UploadFile, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Select Video")
                        }
                        if (selectedVideoUri != null) {
                            Spacer(modifier = Modifier.height(16.dp))
                            Text("Video selected: $videoName")
                        }
                        if (errorMessage != null) {
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(errorMessage!!, color = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            } else {
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    OutlinedTextField(
                        value = videoUrl,
                        onValueChange = { videoUrl = it },
                        label = { Text("YouTube URL") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
            
            Button(
                onClick = {
                    if (tabIndex == 0 && selectedVideoUri != null) {
                        onContinue(selectedVideoUri.toString())
                    } else if (tabIndex == 1 && videoUrl.isNotEmpty()) {
                        onContinue(videoUrl)
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                enabled = (tabIndex == 0 && selectedVideoUri != null) || (tabIndex == 1 && videoUrl.isNotEmpty())
            ) {
                Text(stringResource(R.string.import_continue), fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}
