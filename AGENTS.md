Project requires real API integrations. Mocking data in the final views is strictly prohibited unless specifically instructed. The AI processing pipeline has been implemented to call Gemini.
The editor screen has been implemented using ExoPlayer for video playback and a composable Box overlay for text editing.

The visual progress tracking on the processing screen is improved to clearly display AI generation steps.
The export feature in the editor screen has a simulated processing and success state.

The editor screen includes a 'Save Project' function using SharedPreferences to store current caption modifications (text, color, font, position, and aspect ratio). 
The 'Customize Style' bottom sheet lets users toggle visual parameters and aspect ratio, immediately updating the simulated preview.
