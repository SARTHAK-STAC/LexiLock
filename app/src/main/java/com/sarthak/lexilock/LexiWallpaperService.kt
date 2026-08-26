package com.sarthak.lexilock

import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.service.wallpaper.WallpaperService
import android.view.SurfaceHolder

class LexiWallpaperService : WallpaperService() {

    override fun onCreateEngine(): Engine {
        return LexiEngine()

    }

    inner class LexiEngine : Engine() {

        private val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        private val currentWord =
            DailyWordManager.getTodayWord(applicationContext)

        private val drawLock = Any()

        override fun onVisibilityChanged(visible: Boolean) {
            super.onVisibilityChanged(visible)

            if (visible) {
                drawWallpaper()
            }
        }

        override fun onSurfaceCreated(holder: SurfaceHolder) {
            super.onSurfaceCreated(holder)
            drawWallpaper()
        }

        override fun onSurfaceChanged(
            holder: SurfaceHolder,
            format: Int,
            width: Int,
            height: Int
        ) {
            super.onSurfaceChanged(
                holder,
                format,
                width,
                height
            )

            drawWallpaper()
        }
        private fun drawCenteredText(
            canvas: android.graphics.Canvas,
            text: String,
            x: Float,
            y: Float,
            maxWidth: Float,
            startingSize: Float,
            minSize: Float,
            typeface: Typeface
        ) {
            paint.typeface = typeface
            paint.textAlign = Paint.Align.CENTER

            var textSize = startingSize

            // Start with the requested font size
            paint.textSize = textSize

            // Reduce size until the text fits
            while (
                textSize > minSize &&
                paint.measureText(text) > maxWidth
            ) {
                textSize -= 2f
                paint.textSize = textSize
            }

            canvas.drawText(
                text,
                x,
                y,
                paint
            )
        }
        private fun drawWrappedText(
            canvas: android.graphics.Canvas,
            text: String,
            x: Float,
            startY: Float,
            maxWidth: Float,
            textSize: Float,
            lineSpacing: Float
        ) {
            paint.textSize = textSize
            paint.typeface = Typeface.create(
                Typeface.SERIF,
                Typeface.NORMAL
            )

            paint.textAlign = Paint.Align.CENTER

            val words = text.split(" ")

            val lines = mutableListOf<String>()
            var currentLine = ""

            for (word in words) {

                val testLine =
                    if (currentLine.isEmpty()) {
                        word
                    } else {
                        "$currentLine $word"
                    }

                if (paint.measureText(testLine) <= maxWidth) {

                    currentLine = testLine

                } else {

                    if (currentLine.isNotEmpty()) {
                        lines.add(currentLine)
                    }

                    currentLine = word
                }
            }

            if (currentLine.isNotEmpty()) {
                lines.add(currentLine)
            }

            var y = startY

            for (line in lines) {

                canvas.drawText(
                    line,
                    x,
                    y,
                    paint
                )

                y += lineSpacing
            }
        }
        private fun drawWallpaper() {

            synchronized(drawLock) {

                val holder = surfaceHolder ?: return

                if (!holder.surface.isValid) {
                    return
                }

                val canvas = try {
                    holder.lockCanvas()
                } catch (e: Exception) {
                    null
                } ?: return

                try {

                    // -------------------------
                    // BACKGROUND
                    // -------------------------

                    canvas.drawColor(
                        Color.rgb(45, 20, 20)
                    )

                    val centerX = canvas.width / 2f
                    val centerY = canvas.height / 2f

                    paint.color = Color.rgb(245, 225, 200)
                    paint.textAlign = Paint.Align.CENTER

                    // -------------------------
                    // WORD
                    // -------------------------

                    drawCenteredText(
                        canvas = canvas,
                        text = currentWord.word,
                        x = centerX,
                        y = centerY - 150f,
                        maxWidth = canvas.width * 0.90f,
                        startingSize = canvas.width * 0.14f,
                        minSize = canvas.width * 0.07f,
                        typeface = Typeface.create(
                            Typeface.SERIF,
                            Typeface.BOLD
                        )
                    )

                    // -------------------------
                    // PART OF SPEECH
                    // -------------------------

                    drawCenteredText(
                        canvas = canvas,
                        text = currentWord.partOfSpeech,
                        x = centerX,
                        y = centerY - 60f,
                        maxWidth = canvas.width * 0.70f,
                        startingSize = canvas.width * 0.035f,
                        minSize = canvas.width * 0.025f,
                        typeface = Typeface.create(
                            Typeface.SERIF,
                            Typeface.ITALIC
                        )
                    )

                    // -------------------------
                    // PRONUNCIATION
                    // -------------------------

                    drawCenteredText(
                        canvas = canvas,
                        text = currentWord.pronunciation,
                        x = centerX,
                        y = centerY + 20f,
                        maxWidth = canvas.width * 0.85f,
                        startingSize = canvas.width * 0.042f,
                        minSize = canvas.width * 0.028f,
                        typeface = Typeface.create(
                            Typeface.SERIF,
                            Typeface.NORMAL
                        )
                    )

                    // -------------------------
                    // MEANING
                    // -------------------------

                    drawWrappedText(
                        canvas = canvas,
                        text = currentWord.meaning,
                        x = centerX,
                        startY = centerY + 110f,
                        maxWidth = canvas.width * 0.88f,
                        textSize = canvas.width * 0.040f,
                        lineSpacing = canvas.width * 0.055f
                    )

                } finally {

                    try {
                        holder.unlockCanvasAndPost(canvas)
                    } catch (e: Exception) {
                        // Ignore surface errors
                    }
                }
            }
        }

    }
}