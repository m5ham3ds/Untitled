package com.example.ui.screens

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
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
    val context = LocalContext.current
    val sharedPref = remember { context.getSharedPreferences("api_keys", Context.MODE_PRIVATE) }

    var geminiKey by remember { mutableStateOf(sharedPref.getString("gemini_key", "") ?: "") }
    var hfToken by remember { mutableStateOf(sharedPref.getString("hf_token", "") ?: "") }
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
                onClick = {
                    sharedPref.edit()
                        .putString("gemini_key", geminiKey)
                        .putString("hf_token", hfToken)
                        .apply()
                    Toast.makeText(context, R.string.models_manager_save, Toast.LENGTH_SHORT).show()
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.models_manager_save))
            }
        }
    }
}
