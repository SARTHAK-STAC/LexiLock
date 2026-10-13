package com.sarthak.lexilock.utils

import android.content.Context
import android.content.Intent
import android.graphics.*
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import androidx.core.content.FileProvider
import com.sarthak.lexilock.domain.model.WordItem
import java.io.File
import java.io.FileOutputStream

object ShareCardGenerator {

    private const val CARD_WIDTH = 1080
    private const val CARD_HEIGHT = 1350

    fun shareWordCard(
        context: Context,
        wordItem: WordItem,
        badgeTitle: String = "WORD OF THE DAY"
    ) {
        val bitmap = Bitmap.createBitmap(CARD_WIDTH, CARD_HEIGHT, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        drawBaseCard(canvas, badgeTitle)

        // Hero Word
        val wordPaint = TextPaint().apply {
            color = Color.parseColor("#F5F5F0")
            textSize = 96f
            typeface = Typeface.create(Typeface.SERIF, Typeface.BOLD)
            isAntiAlias = true
        }
        canvas.drawText(wordItem.word, 100f, 420f, wordPaint)

        // Pronunciation & Part of speech
        val subPaint = TextPaint().apply {
            color = Color.parseColor("#8A8F99")
            textSize = 38f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
            isAntiAlias = true
        }
        val subText = buildString {
            if (wordItem.partOfSpeech.isNotBlank()) append(wordItem.partOfSpeech)
            if (wordItem.pronunciation.isNotBlank()) {
                if (isNotEmpty()) append("  •  ")
                append(wordItem.pronunciation)
            }
        }
        canvas.drawText(subText, 100f, 490f, subPaint)

        // Divider
        val divPaint = Paint().apply {
            color = Color.parseColor("#26292E")
            strokeWidth = 2f
        }
        canvas.drawLine(100f, 540f, CARD_WIDTH - 100f, 540f, divPaint)

        // Definition
        val defPaint = TextPaint().apply {
            color = Color.parseColor("#E6E8EC")
            textSize = 46f
            typeface = Typeface.create(Typeface.SERIF, Typeface.NORMAL)
            isAntiAlias = true
        }
        drawMultilineText(
            text = wordItem.definition,
            x = 100f,
            y = 600f,
            width = CARD_WIDTH - 200,
            paint = defPaint,
            canvas = canvas
        )

        // Example sentence
        if (wordItem.exampleSentence.isNotBlank()) {
            val exPaint = TextPaint().apply {
                color = Color.parseColor("#9BA1AD")
                textSize = 38f
                typeface = Typeface.create(Typeface.SERIF, Typeface.ITALIC)
                isAntiAlias = true
            }
            val formattedExample = "“${wordItem.exampleSentence.trim()}”"
            drawMultilineText(
                text = formattedExample,
                x = 100f,
                y = 820f,
                width = CARD_WIDTH - 200,
                paint = exPaint,
                canvas = canvas
            )
        }

        launchShareIntent(context, bitmap, "LexiLock_${wordItem.word}.png", "Learn '${wordItem.word}' on LexiLock")
    }

    fun shareChallengeResult(
        context: Context,
        gameTitle: String,
        score: Int,
        accuracy: Int,
        xpEarned: Int,
        streakDays: Int
    ) {
        val bitmap = Bitmap.createBitmap(CARD_WIDTH, CARD_HEIGHT, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        drawBaseCard(canvas, "DAILY CHALLENGE COMPLETED")

        // Game Title
        val titlePaint = TextPaint().apply {
            color = Color.parseColor("#F5F5F0")
            textSize = 80f
            typeface = Typeface.create(Typeface.SERIF, Typeface.BOLD)
            isAntiAlias = true
        }
        canvas.drawText(gameTitle, 100f, 420f, titlePaint)

        // Score Box
        val scoreBoxPaint = Paint().apply {
            color = Color.parseColor("#1C1E22")
            isAntiAlias = true
        }
        val boxRect = RectF(100f, 480f, CARD_WIDTH - 100f, 850f)
        canvas.drawRoundRect(boxRect, 24f, 24f, scoreBoxPaint)

        val statLabelPaint = TextPaint().apply {
            color = Color.parseColor("#8A8F99")
            textSize = 32f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
            isAntiAlias = true
        }
        val statValuePaint = TextPaint().apply {
            color = Color.parseColor("#FFFFFF")
            textSize = 68f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            isAntiAlias = true
        }

        // Col 1: Score
        canvas.drawText("SCORE", 160f, 560f, statLabelPaint)
        canvas.drawText("$score pts", 160f, 650f, statValuePaint)

        // Col 2: Accuracy
        canvas.drawText("ACCURACY", 600f, 560f, statLabelPaint)
        canvas.drawText("$accuracy%", 600f, 650f, statValuePaint)

        // Bottom of box: XP & Streak
        val subBoxPaint = TextPaint().apply {
            color = Color.parseColor("#7A9BEE")
            textSize = 40f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            isAntiAlias = true
        }
        canvas.drawText("+$xpEarned XP Earned  •  🔥 $streakDays Day Streak", 160f, 770f, subBoxPaint)

        launchShareIntent(context, bitmap, "LexiLock_Challenge_Result.png", "Completed today's LexiLock challenge with $accuracy% accuracy!")
    }

    fun shareMysteryWord(
        context: Context,
        wordItem: WordItem,
        cluesUsed: Int,
        xpEarned: Int
    ) {
        val bitmap = Bitmap.createBitmap(CARD_WIDTH, CARD_HEIGHT, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        drawBaseCard(canvas, "MYSTERY WORD SOLVED")

        val titlePaint = TextPaint().apply {
            color = Color.parseColor("#F5F5F0")
            textSize = 96f
            typeface = Typeface.create(Typeface.SERIF, Typeface.BOLD)
            isAntiAlias = true
        }
        canvas.drawText(wordItem.word, 100f, 420f, titlePaint)

        val clueInfoPaint = TextPaint().apply {
            color = Color.parseColor("#7A9BEE")
            textSize = 38f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            isAntiAlias = true
        }
        canvas.drawText("Solved with $cluesUsed clue${if (cluesUsed > 1) "s" else ""}  •  +$xpEarned XP", 100f, 490f, clueInfoPaint)

        // Divider
        val divPaint = Paint().apply {
            color = Color.parseColor("#26292E")
            strokeWidth = 2f
        }
        canvas.drawLine(100f, 540f, CARD_WIDTH - 100f, 540f, divPaint)

        // Definition
        val defPaint = TextPaint().apply {
            color = Color.parseColor("#E6E8EC")
            textSize = 46f
            typeface = Typeface.create(Typeface.SERIF, Typeface.NORMAL)
            isAntiAlias = true
        }
        drawMultilineText(
            text = wordItem.definition,
            x = 100f,
            y = 610f,
            width = CARD_WIDTH - 200,
            paint = defPaint,
            canvas = canvas
        )

        launchShareIntent(context, bitmap, "LexiLock_Mystery_${wordItem.word}.png", "I solved today's mystery word '${wordItem.word}' on LexiLock!")
    }

    fun shareStreakMilestone(context: Context, streakDays: Int) {
        val bitmap = Bitmap.createBitmap(CARD_WIDTH, CARD_HEIGHT, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        drawBaseCard(canvas, "STREAK MILESTONE")

        val flamePaint = TextPaint().apply {
            color = Color.parseColor("#F5F5F0")
            textSize = 140f
            isAntiAlias = true
        }
        canvas.drawText("🔥", 100f, 440f, flamePaint)

        val daysPaint = TextPaint().apply {
            color = Color.parseColor("#F5F5F0")
            textSize = 100f
            typeface = Typeface.create(Typeface.SERIF, Typeface.BOLD)
            isAntiAlias = true
        }
        canvas.drawText("$streakDays Days", 260f, 430f, daysPaint)

        val subPaint = TextPaint().apply {
            color = Color.parseColor("#8A8F99")
            textSize = 40f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
            isAntiAlias = true
        }
        canvas.drawText("Unbroken vocabulary learning streak", 100f, 530f, subPaint)

        launchShareIntent(context, bitmap, "LexiLock_Streak_${streakDays}.png", "I've maintained a $streakDays-day vocabulary streak on LexiLock!")
    }

    fun shareGameResult(
        context: Context,
        gameTitle: String,
        score: Int,
        accuracyPercent: Int,
        timeSeconds: Int?,
        xpEarned: Int,
        isPersonalBest: Boolean
    ) {
        val bitmap = Bitmap.createBitmap(CARD_WIDTH, CARD_HEIGHT, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        drawBaseCard(canvas, if (isPersonalBest) "NEW PERSONAL BEST" else "ARCADE RESULT")

        // Game Title
        val titlePaint = TextPaint().apply {
            color = Color.parseColor("#F5F5F0")
            textSize = 84f
            typeface = Typeface.create(Typeface.SERIF, Typeface.BOLD)
            isAntiAlias = true
        }
        canvas.drawText(gameTitle, 100f, 400f, titlePaint)

        // Stat Box
        val boxPaint = Paint().apply {
            color = Color.parseColor("#1B1D22")
            isAntiAlias = true
        }
        canvas.drawRoundRect(RectF(100f, 460f, CARD_WIDTH - 100f, 850f), 24f, 24f, boxPaint)

        val statLabelPaint = TextPaint().apply {
            color = Color.parseColor("#8A8F99")
            textSize = 28f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
            isAntiAlias = true
            letterSpacing = 0.08f
        }
        val statValuePaint = TextPaint().apply {
            color = Color.parseColor("#FFFFFF")
            textSize = 64f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            isAntiAlias = true
        }

        // Col 1: Score
        canvas.drawText("SCORE", 160f, 550f, statLabelPaint)
        canvas.drawText("$score pts", 160f, 630f, statValuePaint)

        // Col 2: Accuracy
        canvas.drawText("ACCURACY", 580f, 550f, statLabelPaint)
        canvas.drawText("$accuracyPercent%", 580f, 630f, statValuePaint)

        // Time / XP row
        val timeLabel = if (timeSeconds != null && timeSeconds > 0) "${timeSeconds}s" else "--"
        canvas.drawText("TIME", 160f, 720f, statLabelPaint)
        canvas.drawText(timeLabel, 160f, 780f, TextPaint(statValuePaint).apply { textSize = 44f })

        canvas.drawText("XP EARNED", 580f, 720f, statLabelPaint)
        canvas.drawText("+$xpEarned XP", 580f, 780f, TextPaint(statValuePaint).apply {
            textSize = 44f
            color = Color.parseColor("#7A9BEE")
        })

        // Call to action prompt
        val ctaPaint = TextPaint().apply {
            color = Color.parseColor("#E6E8EC")
            textSize = 42f
            typeface = Typeface.create(Typeface.SERIF, Typeface.ITALIC)
            isAntiAlias = true
        }
        canvas.drawText("“Can you beat my score?”", 100f, 960f, ctaPaint)

        launchShareIntent(
            context,
            bitmap,
            "LexiLock_${gameTitle.replace(" ", "_")}.png",
            "I scored $score pts ($accuracyPercent% accuracy) in $gameTitle on LexiLock! Can you beat me?"
        )
    }

    fun shareLevelUp(
        context: Context,
        levelNumber: Int,
        levelTitle: String,
        totalXp: Int
    ) {
        val bitmap = Bitmap.createBitmap(CARD_WIDTH, CARD_HEIGHT, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        drawBaseCard(canvas, "LEVEL MILESTONE")

        val subPaint = TextPaint().apply {
            color = Color.parseColor("#7A9BEE")
            textSize = 36f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            isAntiAlias = true
            letterSpacing = 0.1f
        }
        canvas.drawText("ADVANCED TO LEVEL $levelNumber", 100f, 400f, subPaint)

        val titlePaint = TextPaint().apply {
            color = Color.parseColor("#F5F5F0")
            textSize = 88f
            typeface = Typeface.create(Typeface.SERIF, Typeface.BOLD)
            isAntiAlias = true
        }
        canvas.drawText(levelTitle, 100f, 500f, titlePaint)

        val divPaint = Paint().apply {
            color = Color.parseColor("#26292E")
            strokeWidth = 2f
        }
        canvas.drawLine(100f, 560f, CARD_WIDTH - 100f, 560f, divPaint)

        val descPaint = TextPaint().apply {
            color = Color.parseColor("#E6E8EC")
            textSize = 42f
            typeface = Typeface.create(Typeface.SERIF, Typeface.NORMAL)
            isAntiAlias = true
        }
        canvas.drawText("Total Career Progress: $totalXp XP", 100f, 640f, descPaint)

        val footerQuotePaint = TextPaint().apply {
            color = Color.parseColor("#8A8F99")
            textSize = 36f
            typeface = Typeface.create(Typeface.SERIF, Typeface.ITALIC)
            isAntiAlias = true
        }
        canvas.drawText("“Consistency is the mother of mastery.”", 100f, 760f, footerQuotePaint)

        launchShareIntent(
            context,
            bitmap,
            "LexiLock_Level_$levelNumber.png",
            "I just reached Level $levelNumber ($levelTitle) on LexiLock with $totalXp XP!"
        )
    }

    private fun drawBaseCard(canvas: Canvas, badgeTitle: String) {
        // Deep matte background
        val bgPaint = Paint().apply {
            color = Color.parseColor("#121417")
        }
        canvas.drawRect(0f, 0f, CARD_WIDTH.toFloat(), CARD_HEIGHT.toFloat(), bgPaint)

        // Subtle outer border
        val borderPaint = Paint().apply {
            color = Color.parseColor("#26292E")
            style = Paint.Style.STROKE
            strokeWidth = 3f
        }
        canvas.drawRoundRect(RectF(40f, 40f, CARD_WIDTH - 40f, CARD_HEIGHT - 40f), 32f, 32f, borderPaint)

        // Brand at top left
        val brandPaint = TextPaint().apply {
            color = Color.parseColor("#FFFFFF")
            textSize = 34f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            isAntiAlias = true
            letterSpacing = 0.12f
        }
        canvas.drawText("LEXILOCK", 100f, 150f, brandPaint)

        // Badge at top right
        val badgeBgPaint = Paint().apply {
            color = Color.parseColor("#1F2228")
            isAntiAlias = true
        }
        val badgeTextPaint = TextPaint().apply {
            color = Color.parseColor("#8A8F99")
            textSize = 24f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            isAntiAlias = true
            letterSpacing = 0.08f
        }
        val badgeWidth = badgeTextPaint.measureText(badgeTitle) + 48f
        val badgeRect = RectF(CARD_WIDTH - 100f - badgeWidth, 115f, CARD_WIDTH - 100f, 165f)
        canvas.drawRoundRect(badgeRect, 12f, 12f, badgeBgPaint)
        canvas.drawText(badgeTitle, badgeRect.left + 24f, 150f, badgeTextPaint)

        // Bottom Footer
        val footerDivPaint = Paint().apply {
            color = Color.parseColor("#1F2228")
            strokeWidth = 2f
        }
        canvas.drawLine(100f, CARD_HEIGHT - 160f, CARD_WIDTH - 100f, CARD_HEIGHT - 160f, footerDivPaint)

        val footerPaint = TextPaint().apply {
            color = Color.parseColor("#7E8594")
            textSize = 28f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
            isAntiAlias = true
            letterSpacing = 0.04f
        }
        canvas.drawText("Learn a word every day with LexiLock", 100f, CARD_HEIGHT - 95f, footerPaint)
    }

    private fun drawMultilineText(
        text: String,
        x: Float,
        y: Float,
        width: Int,
        paint: TextPaint,
        canvas: Canvas
    ) {
        val staticLayout = StaticLayout.Builder.obtain(text, 0, text.length, paint, width)
            .setAlignment(Layout.Alignment.ALIGN_NORMAL)
            .setLineSpacing(12f, 1f)
            .setIncludePad(false)
            .build()

        canvas.save()
        canvas.translate(x, y)
        staticLayout.draw(canvas)
        canvas.restore()
    }

    private fun launchShareIntent(
        context: Context,
        bitmap: Bitmap,
        filename: String,
        shareText: String
    ) {
        try {
            val shareDir = File(context.cacheDir, "shared_images").apply { mkdirs() }
            val file = File(shareDir, filename)
            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            }

            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )

            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "image/png"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_TEXT, shareText)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(intent, "Share via"))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
