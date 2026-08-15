package com.energy.app.data.coach

import android.content.Context
import android.speech.tts.TextToSpeech
import java.util.Locale

/**
 * Audio & voice coaching (§2) — real TTS announcements for distance/time
 * milestones. Off by default; never speaks over music uninvited. All calls
 * are runCatching-guarded so a missing TTS engine can't break a workout.
 */
object WorkoutCoach {

    @Volatile
    private var tts: TextToSpeech? = null

    fun init(context: Context) {
        if (tts != null) return
        runCatching {
            tts = TextToSpeech(context.applicationContext) { status ->
                if (status == TextToSpeech.SUCCESS) {
                    tts?.language = Locale.getDefault()
                }
            }
        }
    }

    fun speak(text: String) {
        runCatching {
            tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "energy-coach")
        }
    }

    fun shutdown() {
        runCatching {
            tts?.stop()
            tts?.shutdown()
        }
        tts = null
    }
}
