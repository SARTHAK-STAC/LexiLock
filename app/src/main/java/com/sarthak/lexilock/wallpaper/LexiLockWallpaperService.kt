package com.sarthak.lexilock.wallpaper

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.service.wallpaper.WallpaperService
import android.speech.tts.TextToSpeech
import android.util.Log
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
        private const val TAG = "LexiWallpaperService"

        @Volatile
        private var activeEngine: LexiLockEngine? = null

        fun requestRedraw(context: Context? = null) {
            Log.d(TAG, "requestRedraw() triggered, activeEngine=${activeEngine != null}")
            activeEngine?.refreshAndDraw()
            context?.let { ctx ->
                try {
                    val intent = Intent("com.sarthak.lexilock.ACTION_REQUEST_WALLPAPER_REDRAW").apply {
                        setPackage(ctx.packageName)
                    }
                    ctx.sendBroadcast(intent)
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to broadcast ACTION_REQUEST_WALLPAPER_REDRAW", e)
                }
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        Log.i(TAG, "WallpaperService.onCreate")
    }

    override fun onDestroy() {
        Log.i(TAG, "WallpaperService.onDestroy")
        super.onDestroy()
    }

    override fun onCreateEngine(): Engine {
        Log.i(TAG, "WallpaperService.onCreateEngine")
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
            Log.i(TAG, "LexiLockEngine.onCreate (isPreview=$isPreview)")
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
                Log.d(TAG, "updateReceiver successfully registered")
            } catch (e: Exception) {
                Log.w(TAG, "updateReceiver registration failed", e)
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
            Log.d(TAG, "LexiLockEngine.refreshAndDraw (isPreview=$isPreview)")
            currentWord = repository.getActiveWallpaperWord()
            drawWallpaper()
            scheduleNextSlotCheck()
        }

        override fun onVisibilityChanged(visible: Boolean) {
            super.onVisibilityChanged(visible)
            Log.d(TAG, "LexiLockEngine.onVisibilityChanged: visible=$visible (isPreview=$isPreview)")
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
            Log.i(TAG, "LexiLockEngine.onSurfaceCreated (isPreview=$isPreview)")
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
            Log.d(TAG, "LexiLockEngine.onSurfaceChanged: ${width}x${height} (isPreview=$isPreview)")
            currentWord = repository.getActiveWallpaperWord()
            drawWallpaper()
            scheduleNextSlotCheck()
        }

        override fun onTouchEvent(event: MotionEvent) {
            super.onTouchEvent(event)
            gestureDetector.onTouchEvent(event)
        }

        override fun onDestroy() {
            Log.i(TAG, "LexiLockEngine.onDestroy (isPreview=$isPreview)")
            super.onDestroy()
            handler.removeCallbacks(slotRotationRunnable)
            if (isReceiverRegistered) {
                try {
                    applicationContext.unregisterReceiver(updateReceiver)
                    Log.d(TAG, "updateReceiver successfully unregistered")
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to unregister updateReceiver", e)
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
