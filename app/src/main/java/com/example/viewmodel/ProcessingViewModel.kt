package com.example.viewmodel

import android.app.Application
import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.ui.screens.CaptionSegment
import com.example.ui.screens.ClipManager
import com.example.ui.screens.VideoClip
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import okio.source
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class ProcessingViewModel(application: Application) : AndroidViewModel(application) {

    private val _progress = MutableStateFlow(0f)
    val progress: StateFlow<Float> = _progress

    private val _currentStep = MutableStateFlow("Initializing...")
    val currentStep: StateFlow<String> = _currentStep

    private val _isComplete = MutableStateFlow(false)
    val isComplete: StateFlow<Boolean> = _isComplete

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    private val client = OkHttpClient.Builder()
        .connectTimeout(120, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .writeTimeout(120, TimeUnit.SECONDS)
        .build()

    fun startProcessing(videoUriStr: String, aspectRatio: String, clipCount: Int, clipDuration: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val context = getApplication<Application>()
                val prefs = context.getSharedPreferences("api_keys", Context.MODE_PRIVATE)
                val geminiKey = prefs.getString("gemini_key", "") ?: ""

                if (geminiKey.isEmpty()) {
                    _error.value = "Gemini API Key missing. Please configure it in Settings."
                    return@launch
                }
                
                val uri = Uri.parse(videoUriStr)

                // 1. Upload video to Gemini
                _currentStep.value = "Uploading video to Gemini..."
                _progress.value = 0.1f
                val fileData = uploadVideo(context, uri, geminiKey)
                if (fileData == null) {
                    _error.value = "Failed to upload video to Gemini."
                    return@launch
                }
                val (fileUri, fileName, mimeType) = fileData

                // 2. Poll for ACTIVE state
                _currentStep.value = "Gemini processing video..."
                _progress.value = 0.4f
                var isActive = false
                for (i in 0..30) {
                    val state = getFileState(fileName, geminiKey)
                    if (state == "ACTIVE") {
                        isActive = true
                        break
                    } else if (state == "FAILED") {
                        _error.value = "Gemini failed to process the video."
                        return@launch
                    }
                    delay(3000)
                    _progress.value = 0.4f + (0.1f * (i / 30f))
                }
                if (!isActive) {
                    _error.value = "Video processing timed out."
                    return@launch
                }

                // 3. Extract Highlights using Gemini
                _currentStep.value = "AI finding viral moments & captions..."
                _progress.value = 0.6f
                val clips = getHighlightsFromGemini(fileUri, mimeType, clipCount, clipDuration, geminiKey)
                
                if (clips.isEmpty()) {
                    _error.value = "Could not generate highlights. Check your API Key."
                    return@launch
                }

                // Save to ClipManager so EditorScreen can access them
                ClipManager.clips.clear()
                ClipManager.clips.addAll(clips)

                _currentStep.value = "Done!"
                _progress.value = 1f
                _isComplete.value = true

            } catch (e: Exception) {
                e.printStackTrace()
                _error.value = e.message ?: "An unknown error occurred."
            }
        }
    }

    private suspend fun uploadVideo(context: Context, uri: Uri, geminiKey: String): Triple<String, String, String>? {
        return withContext(Dispatchers.IO) {
            try {
                val contentResolver = context.contentResolver
                val mimeType = contentResolver.getType(uri) ?: "video/mp4"
                
                var fileSize = 0L
                contentResolver.openAssetFileDescriptor(uri, "r")?.use {
                    fileSize = it.length
                }

                val initUrl = "https://generativelanguage.googleapis.com/upload/v1beta/files?uploadType=resumable&key=$geminiKey"
                val initBody = "{\"file\": {\"displayName\": \"Video\"}}".toRequestBody("application/json".toMediaType())
                
                val initRequest = Request.Builder()
                    .url(initUrl)
                    .post(initBody)
                    .header("X-Goog-Upload-Protocol", "resumable")
                    .header("X-Goog-Upload-Command", "start")
                    .header("X-Goog-Upload-Header-Content-Length", fileSize.toString())
                    .header("X-Goog-Upload-Header-Content-Type", mimeType)
                    .build()

                var uploadUrl = ""
                client.newCall(initRequest).execute().use { response ->
                    if (!response.isSuccessful) {
                        Log.e("GeminiAPI", "Init failed: ${response.body?.string()}")
                        return@withContext null
                    }
                    uploadUrl = response.header("X-Goog-Upload-URL") ?: return@withContext null
                }

                val uploadBody = object : RequestBody() {
                    override fun contentType() = mimeType.toMediaType()
                    override fun writeTo(sink: okio.BufferedSink) {
                        contentResolver.openInputStream(uri)?.use { input ->
                            sink.writeAll(input.source())
                        }
                    }
                }

                val uploadRequest = Request.Builder()
                    .url(uploadUrl)
                    .post(uploadBody)
                    .header("X-Goog-Upload-Command", "upload, finalize")
                    .header("X-Goog-Upload-Offset", "0")
                    .build()

                client.newCall(uploadRequest).execute().use { response ->
                    if (!response.isSuccessful) {
                        Log.e("GeminiAPI", "Upload failed: ${response.body?.string()}")
                        return@withContext null
                    }
                    val body = response.body?.string() ?: return@withContext null
                    val json = JSONObject(body)
                    val fileObj = json.getJSONObject("file")
                    return@withContext Triple(fileObj.getString("uri"), fileObj.getString("name"), mimeType)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }
    }

    private suspend fun getFileState(fileName: String, geminiKey: String): String? {
        return withContext(Dispatchers.IO) {
            try {
                val url = "https://generativelanguage.googleapis.com/v1beta/$fileName?key=$geminiKey"
                val request = Request.Builder().url(url).get().build()
                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) return@withContext null
                    val body = response.body?.string() ?: return@withContext null
                    val json = JSONObject(body)
                    return@withContext json.optString("state")
                }
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }
    }

    private suspend fun getHighlightsFromGemini(fileUri: String, mimeType: String, count: Int, duration: Int, geminiKey: String): List<VideoClip> = withContext(Dispatchers.IO) {
        try {
            val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent?key=$geminiKey"
            
            val prompt = """
                Analyze the uploaded video. Find the $count most interesting, viral, or engaging parts based on engagement markers (e.g., strong emotions, controversial statements, high energy).
                Each part should be roughly $duration seconds long (based on context).
                For each part, provide the start time, end time, a title, and the exact transcript of what is spoken in that segment split into short caption segments (1-3 words each).
                Return ONLY a JSON array with this exact structure, do not wrap it in markdown:
                [
                  {
                    "name": "Viral Highlight Name",
                    "startTimeSeconds": 10.5,
                    "endTimeSeconds": 25.0,
                    "segments": [
                      {
                         "text": "Exact short text",
                         "startProgress": 0.0,
                         "endProgress": 0.5
                      }
                    ]
                  }
                ]
                Note: startTimeSeconds and endTimeSeconds are the actual times in the video.
                startProgress and endProgress should be the relative progress of the segment within the highlight (0.0 to 1.0).
            """.trimIndent()

            val jsonBody = JSONObject().apply {
                put("contents", JSONArray().apply {
                    put(JSONObject().apply {
                        put("parts", JSONArray().apply {
                            put(JSONObject().apply {
                                put("fileData", JSONObject().apply {
                                    put("mimeType", mimeType)
                                    put("fileUri", fileUri)
                                })
                            })
                            put(JSONObject().apply {
                                put("text", prompt)
                            })
                        })
                    })
                })
                put("generationConfig", JSONObject().apply {
                    put("responseMimeType", "application/json")
                })
            }

            val requestBody = jsonBody.toString().toRequestBody("application/json".toMediaType())
            val request = Request.Builder().url(url).post(requestBody).build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    Log.e("GeminiAPI", "generateContent failed: ${response.body?.string()}")
                    return@use emptyList<VideoClip>()
                }
                val responseBody = response.body?.string() ?: return@use emptyList<VideoClip>()
                Log.d("GeminiAPI", "Response: $responseBody")
                
                val responseJson = JSONObject(responseBody)
                val candidates = responseJson.optJSONArray("candidates") ?: return@use emptyList<VideoClip>()
                if (candidates.length() == 0) return@use emptyList<VideoClip>()
                val content = candidates.getJSONObject(0).optJSONObject("content") ?: return@use emptyList<VideoClip>()
                val parts = content.optJSONArray("parts") ?: return@use emptyList<VideoClip>()
                if (parts.length() == 0) return@use emptyList<VideoClip>()
                val textResponse = parts.getJSONObject(0).optString("text", "")
                
                val jsonArray = JSONArray(textResponse)
                val clips = mutableListOf<VideoClip>()
                for (i in 0 until jsonArray.length()) {
                    val obj = jsonArray.getJSONObject(i)
                    
                    val segmentsArray = obj.optJSONArray("segments")
                    val segments = mutableListOf<CaptionSegment>()
                    if (segmentsArray != null) {
                        for (j in 0 until segmentsArray.length()) {
                            val segObj = segmentsArray.getJSONObject(j)
                            segments.add(CaptionSegment(
                                id = j,
                                text = segObj.optString("text", ""),
                                startProgress = segObj.optDouble("startProgress", 0.0).toFloat(),
                                endProgress = segObj.optDouble("endProgress", 1.0).toFloat()
                            ))
                        }
                    }
                    
                    // We map startTimeSeconds/endTimeSeconds to trimStart/trimEnd (for UI simplicity, 0.0 to 1.0, wait...
                    // In EditorScreen, ExoPlayer needs actual times. 
                    // Let's modify VideoClip to use actual seconds, or keep as 0f..1f and we need video duration.
                    // Actually, if we use actual seconds for trimStart/trimEnd, we need to adapt EditorScreen.
                    // For now, let's just pass them directly, we will adapt EditorScreen to use actual times!
                    
                    clips.add(VideoClip(
                        id = i,
                        name = obj.optString("name", "Highlight ${i+1}"),
                        trimStart = obj.optDouble("startTimeSeconds", 0.0).toFloat(),
                        trimEnd = obj.optDouble("endTimeSeconds", 1.0).toFloat(),
                        segments = segments
                    ))
                }
                return@use clips
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return@withContext emptyList()
    }
}
