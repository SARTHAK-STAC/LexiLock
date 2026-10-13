package com.sarthak.lexilock.wallpaper

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.service.wallpaper.WallpaperService
import android.speech.tts.TextToSpeech
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.SurfaceHolder
import com.sarthak.lexilock.data.preferences.LexiPreferences
import com.sarthak.lexilock.data.repository.WordRepository
import com.sarthak.lexilock.data.util.LexiTimeProvider
import com.sarthak.lexilock.domain.model.WordItem
import com.sarthak.lexilock.notifications.DailyWordNotificationScheduler
import java.util.Locale

open class LexiLockWallpaperService : WallpaperService() {

    companion object {
        @Volatile
        private var activeEngine: LexiLockEngine? = null

        fun requestRedraw(context: Context? = null) {
            activeEngine?.refreshAndDraw()
            context?.let { ctx ->
                try {
                    val intent = Intent("com.sarthak.lexilock.ACTION_REQUEST_WALLPAPER_REDRAW").apply {
                        setPackage(ctx.packageName)
                    }
                    ctx.sendBroadcast(intent)
                } catch (e: Exception) {
                    // Ignore broadcast errors
                }
            }
        }
    }

    override fun onCreateEngine(): Engine {
        return LexiLockEngine().also {
            activeEngine = it
        }
    }

    inner class LexiLockEngine : Engine() {

        private val repository = WordRepository(applicationContext)
        private val prefs = LexiPreferences.getInstance(applicationContext)
        private val renderer = WallpaperRenderer(applicationContext)
        private var currentWord: WordItem? = null
        private val drawLock = Any()
        private val handler = android.os.Handler(android.os.Looper.getMainLooper())
        private var isVisibleState = false
        private var isReceiverRegistered = false

        private val updateReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                when (intent.action) {
                    "com.sarthak.lexilock.ACTION_REQUEST_WALLPAPER_REDRAW" -> {
                        refreshAndDraw()
                    }
                    "com.sarthak.lexilock.ACTION_ADVANCE_TIME" -> {
                        val hours = intent.getDoubleExtra("hours", 0.0)
                        LexiTimeProvider.advanceHours(hours)
                        refreshAndDraw()
                    }
                    "com.sarthak.lexilock.ACTION_RESET_TIME" -> {
                        LexiTimeProvider.resetTime()
                        refreshAndDraw()
                    }
                    "com.sarthak.lexilock.ACTION_SET_TIME_HOUR" -> {
                        val hour = intent.getIntExtra("hour", 0)
                        val min = intent.getIntExtra("minute", 0)
                        LexiTimeProvider.setHourOfDay(hour, min)
                        refreshAndDraw()
                    }
                }
            }
        }

        private val slotRotationRunnable = object : Runnable {
            override fun run() {
                if (isVisibleState && prefs.wallpaperWordMode == "daily") {
                    val nextWord = repository.getActiveWallpaperWord()
                    val wordChanged = (currentWord?.id != nextWord.id)
                    currentWord = nextWord
                    drawWallpaper()
                    if (wordChanged && prefs.isDailyNotificationEnabled) {
                        DailyWordNotificationScheduler.showNewWordNotification(applicationContext, nextWord)
                    }
                    scheduleNextSlotCheck()
                }
            }
        }

        private fun scheduleNextSlotCheck() {
            handler.removeCallbacks(slotRotationRunnable)
            if (!isVisibleState || prefs.wallpaperWordMode != "daily") return
            val delayMs = repository.getRemainingMillisToNextSlot() + 300L
            handler.postDelayed(slotRotationRunnable, delayMs)
        }

        private var tts: TextToSpeech? = null

        override fun onCreate(surfaceHolder: SurfaceHolder?) {
            super.onCreate(surfaceHolder)
            setTouchEventsEnabled(true)
            tts = TextToSpeech(applicationContext) { status ->
                if (status == TextToSpeech.SUCCESS) {
                    tts?.language = Locale.US
                }
            }

            try {
                val filter = IntentFilter().apply {
                    addAction("com.sarthak.lexilock.ACTION_REQUEST_WALLPAPER_REDRAW")
                    addAction("com.sarthak.lexilock.ACTION_ADVANCE_TIME")
                    addAction("com.sarthak.lexilock.ACTION_RESET_TIME")
                    addAction("com.sarthak.lexilock.ACTION_SET_TIME_HOUR")
                }
                androidx.core.content.ContextCompat.registerReceiver(
                    applicationContext,
                    updateReceiver,
                    filter,
                    androidx.core.content.ContextCompat.RECEIVER_NOT_EXPORTED
                )
                isReceiverRegistered = true
            } catch (e: Exception) {
                // Ignore receiver registration failure
            }
        }

        private val gestureDetector = GestureDetector(
            applicationContext,
            object : GestureDetector.SimpleOnGestureListener() {
                override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
                    val wordToSpeak = currentWord?.word ?: return false
                    if (!renderer.speakerHitbox.isEmpty && renderer.speakerHitbox.contains(e.x, e.y)) {
                        tts?.speak(wordToSpeak, TextToSpeech.QUEUE_FLUSH, null, "tts_wallpaper")
                        return true
                    }
                    return false
                }
            }
        )

        fun refreshAndDraw() {
            currentWord = repository.getActiveWallpaperWord()
            drawWallpaper()
            scheduleNextSlotCheck()
        }

        override fun onVisibilityChanged(visible: Boolean) {
            super.onVisibilityChanged(visible)
            isVisibleState = visible
            if (visible) {
                currentWord = repository.getActiveWallpaperWord()
                drawWallpaper()
                scheduleNextSlotCheck()
            } else {
                handler.removeCallbacks(slotRotationRunnable)
            }
        }

        override fun onSurfaceCreated(holder: SurfaceHolder) {
            super.onSurfaceCreated(holder)
            currentWord = repository.getActiveWallpaperWord()
            drawWallpaper()
            scheduleNextSlotCheck()
        }

        override fun onSurfaceChanged(
            holder: SurfaceHolder,
            format: Int,
            width: Int,
            height: Int
        ) {
            super.onSurfaceChanged(holder, format, width, height)
            currentWord = repository.getActiveWallpaperWord()
            drawWallpaper()
            scheduleNextSlotCheck()
        }

        override fun onTouchEvent(event: MotionEvent) {
            super.onTouchEvent(event)
            gestureDetector.onTouchEvent(event)
        }

        override fun onDestroy() {
            super.onDestroy()
            handler.removeCallbacks(slotRotationRunnable)
            if (isReceiverRegistered) {
                try {
                    unregisterReceiver(updateReceiver)
                } catch (e: Exception) {
                    // Ignore unregister errors
                }
                isReceiverRegistered = false
            }
            tts?.stop()
            tts?.shutdown()
            tts = null
            if (activeEngine == this) {
                activeEngine = null
            }
        }

        private fun drawWallpaper() {
            synchronized(drawLock) {
                val holder = surfaceHolder ?: return
                if (!holder.surface.isValid) return

                val canvas = try {
                    holder.lockCanvas()
                } catch (e: Exception) {
                    null
                } ?: return

                try {
                    val word = currentWord ?: repository.getActiveWallpaperWord().also { currentWord = it }
                    val wordConfig = prefs.getWordConfig(word.word)
                    renderer.render(canvas, word, canvas.width, canvas.height, wordConfig)
                } finally {
                    try {
                        holder.unlockCanvasAndPost(canvas)
                    } catch (e: Exception) {
                        // Surface might have been destroyed
                    }
                }
            }
        }
    }
}
