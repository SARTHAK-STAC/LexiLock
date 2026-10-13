package com.sarthak.lexilock.utils

import android.content.Context
import android.os.Build
import android.os.CombinedVibration
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.HapticFeedbackConstants
import android.view.View
import com.sarthak.lexilock.data.preferences.LexiPreferences

object LexiSoundHapticsManager {

    fun performHaptic(context: Context, type: FeedbackType) {
        val prefs = LexiPreferences.getInstance(context)
        if (!prefs.isHapticsEnabled) return

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                val vibrator = vibratorManager?.defaultVibrator
                val effect = when (type) {
                    FeedbackType.LIGHT_TICK -> VibrationEffect.createPredefined(VibrationEffect.EFFECT_TICK)
                    FeedbackType.CORRECT -> VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK)
                    FeedbackType.ERROR -> VibrationEffect.createWaveform(longArrayOf(0, 35, 60, 35), intArrayOf(0, 180, 0, 180), -1)
                    FeedbackType.LEVEL_UP -> VibrationEffect.createWaveform(longArrayOf(0, 40, 50, 60), intArrayOf(0, 150, 0, 220), -1)
                    FeedbackType.STREAK -> VibrationEffect.createWaveform(longArrayOf(0, 30, 40, 30, 40, 50), intArrayOf(0, 120, 0, 160, 0, 200), -1)
                }
                vibrator?.vibrate(effect)
            } else {
                @Suppress("DEPRECATION")
                val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                val pattern = when (type) {
                    FeedbackType.LIGHT_TICK -> longArrayOf(0, 15)
                    FeedbackType.CORRECT -> longArrayOf(0, 30)
                    FeedbackType.ERROR -> longArrayOf(0, 40, 60, 40)
                    FeedbackType.LEVEL_UP -> longArrayOf(0, 50, 60, 80)
                    FeedbackType.STREAK -> longArrayOf(0, 40, 50, 40, 50, 60)
                }
                vibrator?.vibrate(pattern, -1)
            }
        } catch (e: Exception) {
            // Ignored on devices without vibrator or permission
        }
    }

    fun performSound(context: Context, type: FeedbackType) {
        val prefs = LexiPreferences.getInstance(context)
        if (!prefs.isSoundEffectsEnabled) return
        // Minimal system tones to avoid intrusive casino effects and never collide with TTS
        try {
            val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? android.media.AudioManager
            when (type) {
                FeedbackType.CORRECT -> audioManager?.playSoundEffect(android.media.AudioManager.FX_KEY_CLICK, 0.4f)
                FeedbackType.ERROR -> audioManager?.playSoundEffect(android.media.AudioManager.FX_KEYPRESS_INVALID, 0.5f)
                FeedbackType.LIGHT_TICK -> audioManager?.playSoundEffect(android.media.AudioManager.FX_KEY_CLICK, 0.2f)
                FeedbackType.LEVEL_UP -> audioManager?.playSoundEffect(android.media.AudioManager.FX_KEY_CLICK, 0.6f)
                FeedbackType.STREAK -> audioManager?.playSoundEffect(android.media.AudioManager.FX_KEY_CLICK, 0.6f)
            }
        } catch (e: Exception) {
            // Handled safely
        }
    }
}

enum class FeedbackType {
    LIGHT_TICK,
    CORRECT,
    ERROR,
    LEVEL_UP,
    STREAK
}
