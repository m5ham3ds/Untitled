package com.example.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModelsManagerScreen(onBack: () -> Unit) {
    var geminiKey by remember { mutableStateOf("") }
    var hfToken by remember { mutableStateOf("") }
    var showGeminiKey by remember { mutableStateOf(false) }
    var showHfToken by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.models_manager_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) { 
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") 
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).padding(16.dp)) {
            Text(stringResource(R.string.models_manager_api_keys), fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = geminiKey,
                onValueChange = { geminiKey = it },
                label = { Text(stringResource(R.string.models_manager_gemini_label)) },
                visualTransformation = if (showGeminiKey) VisualTransformation.None else PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth()
            )
            Text("aistudio.google.com/apikey", fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
            
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedTextField(
                value = hfToken,
                onValueChange = { hfToken = it },
                label = { Text(stringResource(R.string.models_manager_hf_label)) },
                visualTransformation = if (showHfToken) VisualTransformation.None else PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth()
            )
            Text("huggingface.co/settings/tokens", fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
            
            Spacer(modifier = Modifier.height(32.dp))
            Button(
                onClick = { /* Save keys securely */ },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.models_manager_save))
            }
        }
    }
}
