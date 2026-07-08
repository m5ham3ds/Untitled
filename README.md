# ClipViral

Convert long videos into short viral clips using AI, directly on your Android device.

## Features
- **Auto Reframe**: Uses ML to track faces and crop videos to 9:16.
- **Smart Highlights**: Uses Gemini AI to find the most viral moments.
- **Auto Captions**: Uses Hugging Face Whisper for accurate subtitles.
- **Local Processing**: Video processing is done locally via FFmpeg.

## Setup & API Keys
This app uses free AI models. You will need:
1. **Google Gemini API Key**: Get it from [aistudio.google.com/apikey](https://aistudio.google.com/apikey)
2. **Hugging Face Access Token**: Get it from [huggingface.co/settings/tokens](https://huggingface.co/settings/tokens)

Enter these keys in the app's "AI Models Manager" screen.

## Building the App
To build locally, ensure you have Android Studio and JDK 17 installed.
`./gradlew assembleDebug`

This project also uses GitHub Actions for continuous integration.
