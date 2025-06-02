package com.example.pennykeeper.util

import android.content.Intent
import android.speech.RecognizerIntent
import androidx.activity.result.ActivityResultLauncher

class SpeechRecognitionHelper {
    companion object {
        const val SPEECH_REQUEST_CODE = 0

        fun createSpeechIntent(): Intent {
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
            intent.putExtra(
                RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
            )
            intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak now")
            return intent
        }

        fun startSpeechRecognition(launcher: ActivityResultLauncher<Intent>) {
            launcher.launch(createSpeechIntent())
        }
    }
}