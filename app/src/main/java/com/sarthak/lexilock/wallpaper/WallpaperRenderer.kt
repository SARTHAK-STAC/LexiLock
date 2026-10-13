package com.sarthak.lexilock.wallpaper

import android.content.Context
import android.graphics.*
import com.sarthak.lexilock.data.model.*
import com.sarthak.lexilock.data.preferences.LexiPreferences
import com.sarthak.lexilock.domain.model.WordItem
import java.io.File
import java.io.FileInputStream

class WallpaperRenderer(private val context: Context) {

    private val prefs = LexiPreferences.getInstance(context)

    // Cached processed photo bitmap to preserve battery life and high performance
    private var cachedPhotoBitmap: Bitmap? = null
    private var cachedPhotoKey: String = ""

    // Typography paints
    private val wordPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val ipaPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val posPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val definitionPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val examplePaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val speakerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
    }
    private val streakPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val dividerPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val bgPaint = Paint()

    var isAudioPlaying: Boolean = false

    // Hitbox for speaker button if tapped
    var speakerHitbox = RectF()
        private set

    fun drawAudioMark(
        canvas: Canvas,
        cx: Float,
        cy: Float,
        size: Float,
        color: Int,
        isPlaying: Boolean = false
    ) {
        val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = color
            style = Paint.Style.FILL
        }
        val arcPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = color
            style = Paint.Style.STROKE
            strokeWidth = (size * 0.16f).coerceAtLeast(1.5f)
            strokeCap = Paint.Cap.ROUND
        }

        val dotOffset = size * 0.22f
        canvas.drawCircle(cx - dotOffset, cy, (size * 0.18f).coerceAtLeast(2.5f), dotPaint)

        val r1 = size * 0.48f
        val rect1 = RectF(cx - dotOffset - r1, cy - r1, cx - dotOffset + r1, cy + r1)
        canvas.drawArc(rect1, -45f, 90f, false, arcPaint)

        val r2 = size * 0.88f
        val rect2 = RectF(cx - dotOffset - r2, cy - r2, cx - dotOffset + r2, cy + r2)
        canvas.drawArc(rect2, -45f, 90f, false, arcPaint)

        if (isPlaying) {
            val r3 = size * 1.28f
            val rect3 = RectF(cx - dotOffset - r3, cy - r3, cx - dotOffset + r3, cy + r3)
            arcPaint.alpha = 180
            canvas.drawArc(rect3, -45f, 90f, false, arcPaint)
        }

        val hitW = (size * 3.8f).coerceAtLeast(80f)
        val hitH = (size * 3.2f).coerceAtLeast(80f)
        speakerHitbox.set(cx - hitW / 2f, cy - hitH / 2f, cx + hitW / 2f, cy + hitH / 2f)
    }

    fun render(
        canvas: Canvas,
        wordItem: WordItem,
        width: Int,
        height: Int,
        overrideConfig: WallpaperConfig? = null
    ) {
        val w = width.toFloat()
        val h = height.toFloat()
        val config = overrideConfig ?: prefs.getWordConfig(wordItem.word)

        // 1. Render Background
        renderBackground(canvas, config, w, h)

        // 2. Setup typography styles, fonts, alignments, colors and scales
        setupPaints(config, w)

        // Check if style has custom editorial composition
        when (config.style) {
            WallpaperStyle.NOIR -> {
                renderNoirComposition(canvas, wordItem, config, w, h)
                return
            }
            WallpaperStyle.EDITORIAL -> {
                renderEditorialComposition(canvas, wordItem, config, w, h)
                return
            }
            WallpaperStyle.MUSEUM -> {
                renderMuseumComposition(canvas, wordItem, config, w, h)
                return
            }
            WallpaperStyle.INK -> {
                renderInkComposition(canvas, wordItem, config, w, h)
                return
            }
            else -> {
                renderStandardOrPerComponent(canvas, wordItem, config, w, h)
            }
        }
    }

    private fun renderStandardOrPerComponent(
        canvas: Canvas,
        wordItem: WordItem,
        config: WallpaperConfig,
        w: Float,
        h: Float
    ) {
        if (config.useComponentPositions) {
            // DRAGGED INDIVIDUAL COMPONENT POSITIONS
            // 1. Word
            if (config.showWord) {
                val formattedWord = wordItem.word.replaceFirstChar { it.uppercase() }
                canvas.drawText(formattedWord, w * config.wordPosX, h * config.wordPosY, wordPaint)
            }
            // 2. IPA
            if (config.showIpa && wordItem.pronunciation.isNotBlank()) {
                canvas.drawText(wordItem.pronunciation, w * config.ipaPosX, h * config.ipaPosY, ipaPaint)
            }
            // 3. POS
            if (config.showPartOfSpeech && wordItem.partOfSpeech.isNotBlank()) {
                canvas.drawText(wordItem.partOfSpeech.lowercase(), w * config.posPosX, h * config.posPosY, posPaint)
            }
            // 4. Definition
            if (config.showDefinition && wordItem.definition.isNotBlank()) {
                val maxDefWidth = w * 0.78f
                val defSpacing = (w * 0.054f * config.lineSpacingScale)
                drawWrappedText(
                    canvas, wordItem.definition, w * config.defPosX, h * config.defPosY,
                    maxDefWidth, definitionPaint, defSpacing, config.alignment
                )
            }
            // 5. Example
            if (config.showExample && wordItem.exampleSentence.isNotBlank()) {
                val formattedExample = "“${wordItem.exampleSentence}”"
                val maxExWidth = w * 0.76f
                val exSpacing = (w * 0.048f * config.lineSpacingScale)
                drawWrappedText(
                    canvas, formattedExample, w * config.exPosX, h * config.exPosY,
                    maxExWidth, examplePaint, exSpacing, config.alignment
                )
            }
            // 6. Streak
            if (config.showStreak) {
                streakPaint.textSize = (w * 0.031f)
                val streakText = if (prefs.currentStreak == 1) "1 day streak" else "${prefs.currentStreak} day streak"
                canvas.drawText(streakText, w * config.streakPosX, h * config.streakPosY, streakPaint)
            }
            // 7. Speaker / Audio Mark
            if (config.showSpeaker) {
                val sX = w * config.wordPosX
                val sY = (h * config.exPosY) + (h * 0.05f)
                drawAudioMark(canvas, sX, sY, w * 0.026f, ipaPaint.color, isAudioPlaying)
            } else {
                speakerHitbox.setEmpty()
            }
            return
        }

        // FLOW LAYOUT (AUTO OR PRESET/DRAGGED GLOBAL ANCHOR)
        val effectiveX = if (config.autoPlacement) {
            // Intelligent auto-placement: if long word, shift slightly for breathing room
            w * 0.5f
        } else {
            w * config.posX
        }

        val startY = if (config.autoPlacement) {
            // Intelligent vertical balance based on word length & components
            val compCount = (if (config.showDefinition) 1 else 0) + (if (config.showExample) 1 else 0)
            if (compCount > 1) h * 0.28f else h * 0.34f
        } else {
            h * config.posY
        }

        var currentY = startY

        // 4. Draw Word
        if (config.showWord) {
            val formattedWord = wordItem.word.replaceFirstChar { it.uppercase() }
            canvas.drawText(formattedWord, effectiveX, currentY, wordPaint)
            currentY += (h * 0.046f * config.lineSpacingScale)
        }

        // 5. Draw IPA + Refined Typographic Sound Mark
        if (config.showIpa && wordItem.pronunciation.isNotBlank()) {
            val ipaText = wordItem.pronunciation
            canvas.drawText(ipaText, effectiveX, currentY, ipaPaint)
            if (config.showSpeaker) {
                val ipaWidth = ipaPaint.measureText(ipaText)
                val audioX = effectiveX + (ipaWidth / 2f) + (w * 0.045f)
                val audioY = currentY - (w * 0.012f)
                drawAudioMark(canvas, audioX, audioY, w * 0.024f, ipaPaint.color, isAudioPlaying)
            }
            currentY += (h * 0.038f * config.lineSpacingScale)
        } else if (config.showSpeaker) {
            drawAudioMark(canvas, effectiveX, currentY, w * 0.024f, ipaPaint.color, isAudioPlaying)
            currentY += (h * 0.038f * config.lineSpacingScale)
        } else {
            speakerHitbox.setEmpty()
        }

        // 6. Draw Part of Speech
        if (config.showPartOfSpeech && wordItem.partOfSpeech.isNotBlank()) {
            canvas.drawText(wordItem.partOfSpeech.lowercase(), effectiveX, currentY, posPaint)
            currentY += (h * 0.036f * config.lineSpacingScale)
        }

        // Subtle thin divider
        if (config.style != WallpaperStyle.MINIMAL && config.showPartOfSpeech) {
            val dividerHalfWidth = w * 0.08f
            canvas.drawLine(effectiveX - dividerHalfWidth, currentY, effectiveX + dividerHalfWidth, currentY, dividerPaint)
            currentY += (h * 0.038f * config.lineSpacingScale)
        } else if (config.showPartOfSpeech) {
            currentY += (h * 0.020f * config.lineSpacingScale)
        }

        // 7. Draw Definition
        if (config.showDefinition && wordItem.definition.isNotBlank()) {
            val maxDefWidth = w * 0.78f
            val defSpacing = (w * 0.054f * config.lineSpacingScale)
            currentY = drawWrappedText(
                canvas, wordItem.definition, effectiveX, currentY,
                maxDefWidth, definitionPaint, defSpacing, config.alignment
            )
            currentY += (h * 0.032f * config.lineSpacingScale)
        }

        // 8. Draw Example
        if (config.showExample && wordItem.exampleSentence.isNotBlank()) {
            val formattedExample = "“${wordItem.exampleSentence}”"
            val maxExWidth = w * 0.76f
            val exSpacing = (w * 0.048f * config.lineSpacingScale)
            currentY = drawWrappedText(
                canvas, formattedExample, effectiveX, currentY,
                maxExWidth, examplePaint, exSpacing, config.alignment
            )
            currentY += (h * 0.038f * config.lineSpacingScale)
        }

        // 10. Streak (Bottom footer)
        if (config.showStreak) {
            streakPaint.textSize = (w * 0.031f)
            val streakText = if (prefs.currentStreak == 1) "1 day streak" else "${prefs.currentStreak} day streak"
            val streakY = (h * 0.88f)
            canvas.drawText(streakText, effectiveX, streakY, streakPaint)
        }
    }

    // ------------------------------------------------------------------------
    // EDITORIAL STYLE: Asymmetric high-end magazine composition
    // ------------------------------------------------------------------------
    private fun renderEditorialComposition(
        canvas: Canvas,
        wordItem: WordItem,
        config: WallpaperConfig,
        w: Float,
        h: Float
    ) {
        val leftX = w * 0.12f
        val alignOriginal = wordPaint.textAlign
        wordPaint.textAlign = Paint.Align.LEFT
        ipaPaint.textAlign = Paint.Align.LEFT
        posPaint.textAlign = Paint.Align.LEFT
        definitionPaint.textAlign = Paint.Align.LEFT
        examplePaint.textAlign = Paint.Align.LEFT

        var curY = h * 0.26f

        // Top subtle publication tag
        val tagPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = posPaint.color
            textSize = w * 0.026f
            letterSpacing = 0.15f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        }
        canvas.drawText("LEXICON · ISSUE NO. ${wordItem.id % 99 + 1}", leftX, curY - (h * 0.03f), tagPaint)

        // Large Asymmetric Vocabulary Word
        if (config.showWord) {
            val formattedWord = wordItem.word.replaceFirstChar { it.uppercase() }
            canvas.drawText(formattedWord, leftX, curY, wordPaint)
            curY += (h * 0.045f * config.lineSpacingScale)
        }

        // Secondary metadata side-by-side or cleanly stacked
        if (config.showPartOfSpeech || config.showIpa) {
            var metaText = ""
            if (config.showPartOfSpeech && wordItem.partOfSpeech.isNotBlank()) {
                metaText += wordItem.partOfSpeech.lowercase()
            }
            if (config.showIpa && wordItem.pronunciation.isNotBlank()) {
                if (metaText.isNotEmpty()) metaText += "   "
                metaText += wordItem.pronunciation
            }
            canvas.drawText(metaText, leftX, curY, ipaPaint)
            curY += (h * 0.025f * config.lineSpacingScale)
        }

        // Fine architectural line divider
        canvas.drawLine(leftX, curY, leftX + (w * 0.22f), curY, dividerPaint)
        curY += (h * 0.040f * config.lineSpacingScale)

        // Definition
        if (config.showDefinition && wordItem.definition.isNotBlank()) {
            val maxDefWidth = w * 0.74f
            val defSpacing = (w * 0.052f * config.lineSpacingScale)
            curY = drawWrappedText(
                canvas, wordItem.definition, leftX, curY,
                maxDefWidth, definitionPaint, defSpacing, TextAlignment.LEFT
            )
            curY += (h * 0.030f * config.lineSpacingScale)
        }

        // Example
        if (config.showExample && wordItem.exampleSentence.isNotBlank()) {
            val formattedExample = "“${wordItem.exampleSentence}”"
            val maxExWidth = w * 0.72f
            val exSpacing = (w * 0.046f * config.lineSpacingScale)
            curY = drawWrappedText(
                canvas, formattedExample, leftX, curY,
                maxExWidth, examplePaint, exSpacing, TextAlignment.LEFT
            )
            curY += (h * 0.035f * config.lineSpacingScale)
        }

        // Speaker / Audio Mark
        if (config.showSpeaker) {
            drawAudioMark(canvas, leftX + (w * 0.04f), curY, w * 0.024f, ipaPaint.color, isAudioPlaying)
            curY += (h * 0.035f * config.lineSpacingScale)
        } else {
            speakerHitbox.setEmpty()
        }

        // Streak at bottom left
        if (config.showStreak) {
            streakPaint.textSize = (w * 0.030f)
            streakPaint.textAlign = Paint.Align.LEFT
            val streakText = if (prefs.currentStreak == 1) "DAY 1 · IN RETROSPECT" else "DAY ${prefs.currentStreak} · VOCABULARY CADENCE"
            canvas.drawText(streakText, leftX, h * 0.88f, streakPaint)
        }

        // Restore paint alignment
        wordPaint.textAlign = alignOriginal
        ipaPaint.textAlign = alignOriginal
        posPaint.textAlign = alignOriginal
        definitionPaint.textAlign = alignOriginal
        examplePaint.textAlign = alignOriginal
    }

    // ------------------------------------------------------------------------
    // MUSEUM STYLE: Exhibition placard aesthetic
    // ------------------------------------------------------------------------
    private fun renderMuseumComposition(
        canvas: Canvas,
        wordItem: WordItem,
        config: WallpaperConfig,
        w: Float,
        h: Float
    ) {
        val centerX = w * 0.5f
        var curY = h * 0.28f

        // Exhibit tag
        val exhibitPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#9C978B")
            textSize = w * 0.024f
            letterSpacing = 0.22f
            textAlign = Paint.Align.CENTER
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
        }
        canvas.drawText("GALLERY COLLECTION · FIG. ${wordItem.id}", centerX, curY - (h * 0.035f), exhibitPaint)

        // Title word (Hero)
        if (config.showWord) {
            val formattedWord = wordItem.word.replaceFirstChar { it.uppercase() }
            canvas.drawText(formattedWord, centerX, curY, wordPaint)
            curY += (h * 0.042f * config.lineSpacingScale)
        }

        // Thin brass/bronze divider line
        val museumDivider = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#443E38")
            strokeWidth = 1f
        }
        canvas.drawLine(centerX - (w * 0.12f), curY, centerX + (w * 0.12f), curY, museumDivider)
        curY += (h * 0.032f * config.lineSpacingScale)

        // POS & IPA
        if (config.showPartOfSpeech || config.showIpa) {
            var subText = ""
            if (config.showPartOfSpeech) subText += wordItem.partOfSpeech.lowercase()
            if (config.showIpa && wordItem.pronunciation.isNotBlank()) {
                if (subText.isNotEmpty()) subText += "   "
                subText += wordItem.pronunciation
            }
            canvas.drawText(subText, centerX, curY, ipaPaint)
            curY += (h * 0.035f * config.lineSpacingScale)
        }

        // Definition
        if (config.showDefinition && wordItem.definition.isNotBlank()) {
            val maxDefWidth = w * 0.74f
            val defSpacing = (w * 0.052f * config.lineSpacingScale)
            curY = drawWrappedText(
                canvas, wordItem.definition, centerX, curY,
                maxDefWidth, definitionPaint, defSpacing, TextAlignment.CENTER
            )
            curY += (h * 0.030f * config.lineSpacingScale)
        }

        // Example
        if (config.showExample && wordItem.exampleSentence.isNotBlank()) {
            val formattedExample = "“${wordItem.exampleSentence}”"
            val maxExWidth = w * 0.72f
            val exSpacing = (w * 0.046f * config.lineSpacingScale)
            curY = drawWrappedText(
                canvas, formattedExample, centerX, curY,
                maxExWidth, examplePaint, exSpacing, TextAlignment.CENTER
            )
            curY += (h * 0.035f * config.lineSpacingScale)
        }

        // Speaker / Audio Mark
        if (config.showSpeaker) {
            drawAudioMark(canvas, centerX, curY, w * 0.024f, ipaPaint.color, isAudioPlaying)
            curY += (h * 0.035f * config.lineSpacingScale)
        } else {
            speakerHitbox.setEmpty()
        }

        // Streak
        if (config.showStreak) {
            streakPaint.textSize = (w * 0.029f)
            streakPaint.textAlign = Paint.Align.CENTER
            val streakText = "ON PERMANENT DISPLAY · ${prefs.currentStreak} CONSECUTIVE DAYS"
            canvas.drawText(streakText, centerX, h * 0.88f, streakPaint)
        }
    }

    // ------------------------------------------------------------------------
    // INK STYLE: Literary monograph aesthetic
    // ------------------------------------------------------------------------
    private fun renderInkComposition(
        canvas: Canvas,
        wordItem: WordItem,
        config: WallpaperConfig,
        w: Float,
        h: Float
    ) {
        val centerX = w * 0.5f
        var curY = h * 0.30f

        // Top monograph mark
        val inkMark = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#4A4642")
            textSize = w * 0.035f
            textAlign = Paint.Align.CENTER
            typeface = Typeface.create(Typeface.SERIF, Typeface.ITALIC)
        }
        canvas.drawText("— § —", centerX, curY - (h * 0.036f), inkMark)

        // Word
        if (config.showWord) {
            val formattedWord = wordItem.word.replaceFirstChar { it.uppercase() }
            canvas.drawText(formattedWord, centerX, curY, wordPaint)
            curY += (h * 0.046f * config.lineSpacingScale)
        }

        // IPA & Part of Speech
        if (config.showIpa && wordItem.pronunciation.isNotBlank()) {
            canvas.drawText(wordItem.pronunciation, centerX, curY, ipaPaint)
            curY += (h * 0.036f * config.lineSpacingScale)
        }

        if (config.showPartOfSpeech && wordItem.partOfSpeech.isNotBlank()) {
            canvas.drawText(wordItem.partOfSpeech.lowercase(), centerX, curY, posPaint)
            curY += (h * 0.032f * config.lineSpacingScale)
        }

        // Thin ink rule
        val inkLine = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#383430")
            strokeWidth = 1.2f
        }
        canvas.drawLine(centerX - (w * 0.15f), curY, centerX + (w * 0.15f), curY, inkLine)
        curY += (h * 0.038f * config.lineSpacingScale)

        // Definition
        if (config.showDefinition && wordItem.definition.isNotBlank()) {
            val maxDefWidth = w * 0.76f
            val defSpacing = (w * 0.052f * config.lineSpacingScale)
            curY = drawWrappedText(
                canvas, wordItem.definition, centerX, curY,
                maxDefWidth, definitionPaint, defSpacing, TextAlignment.CENTER
            )
            curY += (h * 0.032f * config.lineSpacingScale)
        }

        // Example
        if (config.showExample && wordItem.exampleSentence.isNotBlank()) {
            val formattedExample = "“${wordItem.exampleSentence}”"
            val maxExWidth = w * 0.74f
            val exSpacing = (w * 0.046f * config.lineSpacingScale)
            curY = drawWrappedText(
                canvas, formattedExample, centerX, curY,
                maxExWidth, examplePaint, exSpacing, TextAlignment.CENTER
            )
            curY += (h * 0.038f * config.lineSpacingScale)
        }

        // Speaker / Audio Mark
        if (config.showSpeaker) {
            drawAudioMark(canvas, centerX, curY, w * 0.024f, ipaPaint.color, isAudioPlaying)
            curY += (h * 0.035f * config.lineSpacingScale)
        } else {
            speakerHitbox.setEmpty()
        }

        // Streak
        if (config.showStreak) {
            streakPaint.textSize = (w * 0.030f)
            streakPaint.textAlign = Paint.Align.CENTER
            val streakText = if (prefs.currentStreak == 1) "folio no. 1" else "folio no. ${prefs.currentStreak}"
            canvas.drawText(streakText, centerX, h * 0.88f, streakPaint)
        }
    }

    // ------------------------------------------------------------------------
    // NOIR STYLE: Reference screenshot aesthetic
    // Deep warm brown solid background, large cream serif word,
    // small elegant italic metadata, IPA, clean definition, huge negative space
    // ------------------------------------------------------------------------
    private fun renderNoirComposition(
        canvas: Canvas,
        wordItem: WordItem,
        config: WallpaperConfig,
        w: Float,
        h: Float
    ) {
        if (config.useComponentPositions) {
            renderStandardOrPerComponent(canvas, wordItem, config, w, h)
            return
        }

        val centerX = if (config.autoPlacement) w * 0.5f else w * config.posX
        val startY = if (config.autoPlacement) h * 0.36f else h * config.posY
        var curY = startY

        // 1. Large elegant serif vocabulary word (HERO)
        if (config.showWord) {
            val formattedWord = wordItem.word.replaceFirstChar { it.uppercase() }
            canvas.drawText(formattedWord, centerX, curY, wordPaint)
            curY += (h * 0.052f * config.lineSpacingScale)
        }

        // 2. Part of Speech (small elegant italic treatment)
        if (config.showPartOfSpeech && wordItem.partOfSpeech.isNotBlank()) {
            canvas.drawText(wordItem.partOfSpeech.lowercase(), centerX, curY, posPaint)
            curY += (h * 0.038f * config.lineSpacingScale)
        }

        // 3. IPA Pronunciation + Refined Typographic Sound Mark
        if (config.showIpa && wordItem.pronunciation.isNotBlank()) {
            val ipaText = wordItem.pronunciation
            canvas.drawText(ipaText, centerX, curY, ipaPaint)

            if (config.showSpeaker) {
                val ipaWidth = ipaPaint.measureText(ipaText)
                val audioX = centerX + (ipaWidth / 2f) + (w * 0.045f)
                val audioY = curY - (w * 0.012f)
                drawAudioMark(canvas, audioX, audioY, w * 0.024f, ipaPaint.color, isAudioPlaying)
            }
            curY += (h * 0.048f * config.lineSpacingScale)
        } else if (config.showSpeaker) {
            drawAudioMark(canvas, centerX, curY, w * 0.024f, ipaPaint.color, isAudioPlaying)
            curY += (h * 0.040f * config.lineSpacingScale)
        } else {
            speakerHitbox.setEmpty()
        }

        // 4. Definition (clean editorial typography)
        if (config.showDefinition && wordItem.definition.isNotBlank()) {
            val maxDefWidth = w * 0.80f
            val defSpacing = (w * 0.054f * config.lineSpacingScale)
            curY = drawWrappedText(
                canvas, wordItem.definition, centerX, curY,
                maxDefWidth, definitionPaint, defSpacing, config.alignment
            )
            curY += (h * 0.036f * config.lineSpacingScale)
        }

        // 5. Example (if enabled)
        if (config.showExample && wordItem.exampleSentence.isNotBlank()) {
            val formattedExample = "“${wordItem.exampleSentence}”"
            val maxExWidth = w * 0.76f
            val exSpacing = (w * 0.048f * config.lineSpacingScale)
            curY = drawWrappedText(
                canvas, formattedExample, centerX, curY,
                maxExWidth, examplePaint, exSpacing, config.alignment
            )
        }

        // 6. Streak (large negative space at bottom)
        if (config.showStreak) {
            streakPaint.textSize = (w * 0.030f)
            val streakText = if (prefs.currentStreak == 1) "1 day streak" else "${prefs.currentStreak} day streak"
            canvas.drawText(streakText, centerX, h * 0.88f, streakPaint)
        }
    }

    private fun renderBackground(canvas: Canvas, config: WallpaperConfig, w: Float, h: Float) {
        if (config.customBgColorHex.isNotBlank() && config.style != WallpaperStyle.PHOTO) {
            try {
                val customBg = Color.parseColor(config.customBgColorHex)
                canvas.drawColor(customBg)
                if (config.style == WallpaperStyle.NOIR) {
                    val noirVignette = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        shader = RadialGradient(
                            w * 0.5f, h * 0.5f, w * 0.75f,
                            Color.TRANSPARENT,
                            Color.argb(40, 15, 8, 8),
                            Shader.TileMode.CLAMP
                        )
                    }
                    canvas.drawRect(0f, 0f, w, h, noirVignette)
                }
                return
            } catch (e: Exception) {
                // Fall back to style default
            }
        }

        when (config.style) {
            WallpaperStyle.NOIR -> {
                // Rich deep warm brown (#241516) solid background with extremely subtle tonal warmth
                canvas.drawColor(Color.parseColor("#241516"))
                val noirVignette = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    shader = RadialGradient(
                        w * 0.5f, h * 0.5f, w * 0.75f,
                        Color.TRANSPARENT,
                        Color.argb(40, 15, 8, 8),
                        Shader.TileMode.CLAMP
                    )
                }
                canvas.drawRect(0f, 0f, w, h, noirVignette)
            }

            WallpaperStyle.OBSIDIAN -> {
                // Cinematic dark landscape: deep night sky, subtle moonlight, mountain silhouette
                val skyGradient = LinearGradient(
                    0f, 0f, 0f, h,
                    Color.parseColor("#060B12"),
                    Color.parseColor("#0A121E"),
                    Shader.TileMode.CLAMP
                )
                bgPaint.shader = skyGradient
                canvas.drawRect(0f, 0f, w, h, bgPaint)
                bgPaint.shader = null

                // Subtle distant mountain silhouette
                val mountainPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = Color.parseColor("#04070C")
                }
                val path = Path().apply {
                    moveTo(0f, h)
                    lineTo(0f, h * 0.82f)
                    lineTo(w * 0.32f, h * 0.74f)
                    lineTo(w * 0.68f, h * 0.79f)
                    lineTo(w, h * 0.72f)
                    lineTo(w, h)
                    close()
                }
                canvas.drawPath(path, mountainPaint)

                // Atmospheric moon glow in top-right
                val glowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    shader = RadialGradient(
                        w * 0.82f, h * 0.18f, w * 0.35f,
                        Color.parseColor("#142338"),
                        Color.TRANSPARENT,
                        Shader.TileMode.CLAMP
                    )
                }
                canvas.drawCircle(w * 0.82f, h * 0.18f, w * 0.35f, glowPaint)
            }

            WallpaperStyle.MINIMAL -> {
                // Pure typography dictionary aesthetic: solid deep black or warm paper
                val bgColor = if (config.textColorPreset == TextColorPreset.BLACK) {
                    Color.parseColor("#F5F5F0")
                } else {
                    Color.parseColor("#0D0D0D")
                }
                canvas.drawColor(bgColor)
            }

            WallpaperStyle.AURORA -> {
                // Very subtle soft atmospheric aurora (subdued, typography remains hero)
                val auroraGradient = LinearGradient(
                    0f, 0f, w, h,
                    Color.parseColor("#06111C"),
                    Color.parseColor("#0B1C28"),
                    Shader.TileMode.CLAMP
                )
                bgPaint.shader = auroraGradient
                canvas.drawRect(0f, 0f, w, h, bgPaint)
                bgPaint.shader = null

                // Aurora light ribbon (calm, subtle, alpha 75)
                val ribbonPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    shader = LinearGradient(
                        0f, h * 0.3f, w, h * 0.5f,
                        Color.parseColor("#153A3C"),
                        Color.parseColor("#1A2D48"),
                        Shader.TileMode.CLAMP
                    )
                    alpha = 75
                }
                val auroraPath = Path().apply {
                    moveTo(0f, h * 0.35f)
                    cubicTo(w * 0.3f, h * 0.28f, w * 0.7f, h * 0.42f, w, h * 0.34f)
                    lineTo(w, h * 0.52f)
                    cubicTo(w * 0.65f, h * 0.60f, w * 0.35f, h * 0.46f, 0f, h * 0.54f)
                    close()
                }
                canvas.drawPath(auroraPath, ribbonPaint)

                // Night mountain silhouette at base
                val nightGround = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = Color.parseColor("#040A10")
                }
                canvas.drawRect(0f, h * 0.85f, w, h, nightGround)
            }

            WallpaperStyle.PAPER -> {
                // Warm cream modern editorial book page
                canvas.drawColor(Color.parseColor("#F5EFEB"))
                // Subtle deckle edge border line
                val paperLinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = Color.parseColor("#E4DDD2")
                    strokeWidth = 1f
                    style = Paint.Style.STROKE
                }
                canvas.drawRect(w * 0.06f, h * 0.06f, w * 0.94f, h * 0.94f, paperLinePaint)
            }

            WallpaperStyle.MONOCHROME -> {
                // High-contrast clean black & white with subtle depth
                canvas.drawColor(Color.parseColor("#101010"))
                val vignette = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    shader = RadialGradient(
                        w * 0.5f, h * 0.5f, w * 0.7f,
                        Color.TRANSPARENT,
                        Color.parseColor("#050505"),
                        Shader.TileMode.CLAMP
                    )
                }
                canvas.drawRect(0f, 0f, w, h, vignette)
            }

            WallpaperStyle.EDITORIAL -> {
                // Sophisticated dark slate / warm charcoal editorial backdrop
                val editorialGrad = LinearGradient(
                    0f, 0f, 0f, h,
                    Color.parseColor("#16171B"),
                    Color.parseColor("#0F1013"),
                    Shader.TileMode.CLAMP
                )
                bgPaint.shader = editorialGrad
                canvas.drawRect(0f, 0f, w, h, bgPaint)
                bgPaint.shader = null

                // Architectural editorial accent: subtle vertical hairline on left
                val guideLine = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = Color.parseColor("#22242B")
                    strokeWidth = 1f
                }
                canvas.drawLine(w * 0.08f, h * 0.15f, w * 0.08f, h * 0.85f, guideLine)
            }

            WallpaperStyle.MUSEUM -> {
                // Museum exhibition hall neutral dark backdrop with soft spotlight
                canvas.drawColor(Color.parseColor("#151413"))
                val spotLight = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    shader = RadialGradient(
                        w * 0.5f, h * 0.35f, w * 0.55f,
                        Color.parseColor("#252321"),
                        Color.TRANSPARENT,
                        Shader.TileMode.CLAMP
                    )
                }
                canvas.drawCircle(w * 0.5f, h * 0.35f, w * 0.55f, spotLight)
            }

            WallpaperStyle.INK -> {
                // Monograph ink-black and deep carbon environment
                canvas.drawColor(Color.parseColor("#0C0C0D"))
                // Subtle horizontal boundary hairlines
                val inkHairline = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = Color.parseColor("#1E1E22")
                    strokeWidth = 1f
                }
                canvas.drawLine(w * 0.1f, h * 0.20f, w * 0.9f, h * 0.20f, inkHairline)
                canvas.drawLine(w * 0.1f, h * 0.82f, w * 0.9f, h * 0.82f, inkHairline)
            }

            WallpaperStyle.PHOTO, WallpaperStyle.CUSTOM -> {
                renderPhotoBackground(canvas, config, w, h)
            }
        }
    }

    private fun renderPhotoBackground(canvas: Canvas, config: WallpaperConfig, w: Float, h: Float) {
        val photoPath = config.photoPath
        if (photoPath.isNotBlank() && File(photoPath).exists()) {
            val key = "$photoPath:${config.photoTreatment}:${config.photoZoom}:${config.photoPanX}:${config.photoPanY}:${w.toInt()}:${h.toInt()}"
            var bitmap = if (key == cachedPhotoKey) cachedPhotoBitmap else null

            if (bitmap == null) {
                bitmap = loadAndProcessPhoto(photoPath, config, w.toInt(), h.toInt())
                cachedPhotoBitmap = bitmap
                cachedPhotoKey = key
            }

            if (bitmap != null) {
                canvas.drawBitmap(bitmap, 0f, 0f, null)
            } else {
                canvas.drawColor(Color.parseColor("#121418"))
            }
        } else {
            // Placeholder dark aesthetic
            val placeholderGrad = LinearGradient(
                0f, 0f, 0f, h,
                Color.parseColor("#1A1D24"),
                Color.parseColor("#0E1014"),
                Shader.TileMode.CLAMP
            )
            bgPaint.shader = placeholderGrad
            canvas.drawRect(0f, 0f, w, h, bgPaint)
            bgPaint.shader = null
        }

        // Apply Photo Overlay for guaranteed readability
        when (config.photoOverlay) {
            PhotoOverlay.DARK_GRADIENT -> {
                val overlayGrad = LinearGradient(
                    0f, 0f, 0f, h,
                    Color.argb(130, 0, 0, 0),
                    Color.argb(200, 0, 0, 0),
                    Shader.TileMode.CLAMP
                )
                val p = Paint().apply { shader = overlayGrad }
                canvas.drawRect(0f, 0f, w, h, p)
            }
            PhotoOverlay.LIGHT_GRADIENT -> {
                val overlayGrad = LinearGradient(
                    0f, 0f, 0f, h,
                    Color.argb(110, 255, 255, 255),
                    Color.argb(180, 255, 255, 255),
                    Shader.TileMode.CLAMP
                )
                val p = Paint().apply { shader = overlayGrad }
                canvas.drawRect(0f, 0f, w, h, p)
            }
            PhotoOverlay.NONE -> { /* No overlay */ }
        }
    }

    private fun loadAndProcessPhoto(
        path: String,
        config: WallpaperConfig,
        targetWidth: Int,
        targetHeight: Int
    ): Bitmap? {
        return try {
            val file = File(path)
            val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeStream(FileInputStream(file), null, options)

            options.inSampleSize = calculateInSampleSize(options, targetWidth, targetHeight)
            options.inJustDecodeBounds = false
            var src = BitmapFactory.decodeStream(FileInputStream(file), null, options) ?: return null

            val scaled = Bitmap.createScaledBitmap(src, targetWidth, targetHeight, true)
            if (scaled != src) src.recycle()

            val result = Bitmap.createBitmap(targetWidth, targetHeight, Bitmap.Config.ARGB_8888)
            val c = Canvas(result)
            val p = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)

            when (config.photoTreatment) {
                PhotoTreatment.ORIGINAL -> {
                    c.drawBitmap(scaled, 0f, 0f, p)
                }
                PhotoTreatment.DARKEN -> {
                    val cm = ColorMatrix().apply { setScale(0.55f, 0.55f, 0.55f, 1.0f) }
                    p.colorFilter = ColorMatrixColorFilter(cm)
                    c.drawBitmap(scaled, 0f, 0f, p)
                }
                PhotoTreatment.LIGHTEN -> {
                    val cm = ColorMatrix().apply { setScale(1.25f, 1.25f, 1.25f, 1.0f) }
                    p.colorFilter = ColorMatrixColorFilter(cm)
                    c.drawBitmap(scaled, 0f, 0f, p)
                }
                PhotoTreatment.MONOCHROME -> {
                    val cm = ColorMatrix().apply {
                        setSaturation(0f)
                        val darken = ColorMatrix().apply { setScale(0.65f, 0.65f, 0.65f, 1.0f) }
                        postConcat(darken)
                    }
                    p.colorFilter = ColorMatrixColorFilter(cm)
                    c.drawBitmap(scaled, 0f, 0f, p)
                }
                PhotoTreatment.BLUR, PhotoTreatment.STRONG_BLUR -> {
                    val downFactor = if (config.photoTreatment == PhotoTreatment.STRONG_BLUR) 8 else 4
                    val smallW = (targetWidth / downFactor).coerceAtLeast(1)
                    val smallH = (targetHeight / downFactor).coerceAtLeast(1)
                    val small = Bitmap.createScaledBitmap(scaled, smallW, smallH, true)
                    val blurred = Bitmap.createScaledBitmap(small, targetWidth, targetHeight, true)
                    small.recycle()

                    val cm = ColorMatrix().apply { setScale(0.6f, 0.6f, 0.6f, 1.0f) }
                    p.colorFilter = ColorMatrixColorFilter(cm)
                    c.drawBitmap(blurred, 0f, 0f, p)
                    blurred.recycle()
                }
            }
            scaled.recycle()
            result
        } catch (e: Exception) {
            null
        }
    }

    private fun calculateInSampleSize(options: BitmapFactory.Options, reqWidth: Int, reqHeight: Int): Int {
        val (height: Int, width: Int) = options.outHeight to options.outWidth
        var inSampleSize = 1
        if (height > reqHeight || width > reqWidth) {
            val halfHeight: Int = height / 2
            val halfWidth: Int = width / 2
            while (halfHeight / inSampleSize >= reqHeight && halfWidth / inSampleSize >= reqWidth) {
                inSampleSize *= 2
            }
        }
        return inSampleSize
    }

    private fun setupPaints(config: WallpaperConfig, w: Float) {
        val isPaper = config.style == WallpaperStyle.PAPER
        val isNoir = config.style == WallpaperStyle.NOIR

        val primaryColor = when {
            isNoir -> Color.parseColor("#F2E5D2")
            isPaper -> Color.parseColor("#1E1A17")
            config.textColorPreset == TextColorPreset.WARM_WHITE -> Color.parseColor("#FAF7EE")
            config.textColorPreset == TextColorPreset.CREAM -> Color.parseColor("#F4EFE6")
            config.textColorPreset == TextColorPreset.SOFT_GRAY -> Color.parseColor("#B0B0A8")
            config.textColorPreset == TextColorPreset.CHARCOAL -> Color.parseColor("#2C2C2C")
            config.textColorPreset == TextColorPreset.BLACK -> Color.parseColor("#121212")
            config.textColorPreset == TextColorPreset.MUTED_BLUE -> Color.parseColor("#7E96B8")
            config.textColorPreset == TextColorPreset.MUTED_RED -> Color.parseColor("#B87E7E")
            config.textColorPreset == TextColorPreset.MUTED_GREEN -> Color.parseColor("#7EB88E")
            config.textColorPreset == TextColorPreset.CUSTOM -> {
                try { Color.parseColor(config.customTextColorHex) } catch (e: Exception) { Color.parseColor("#FAF7EE") }
            }
            else -> Color.parseColor("#FAF7EE")
        }

        val secondaryColor = when {
            isNoir -> Color.parseColor("#BDAF9F")
            isPaper -> Color.parseColor("#665F56")
            config.textColorPreset == TextColorPreset.BLACK || config.textColorPreset == TextColorPreset.CHARCOAL -> Color.parseColor("#5A5A5A")
            else -> Color.parseColor("#A8A8A2")
        }

        val accentColor = when {
            isNoir -> Color.parseColor("#BDAF9F")
            else -> try {
                Color.parseColor(config.accentColorHex)
            } catch (e: Exception) {
                Color.parseColor("#7E96B8")
            }
        }

        // Per-Component Color Overrides
        val resolvedWordColor = if (config.wordColorHex.isNotBlank()) {
            try { Color.parseColor(config.wordColorHex) } catch (e: Exception) { primaryColor }
        } else primaryColor

        val resolvedIpaColor = if (config.ipaColorHex.isNotBlank()) {
            try { Color.parseColor(config.ipaColorHex) } catch (e: Exception) { secondaryColor }
        } else secondaryColor

        val resolvedPosColor = if (config.posColorHex.isNotBlank()) {
            try { Color.parseColor(config.posColorHex) } catch (e: Exception) { accentColor }
        } else accentColor

        val resolvedDefColor = if (config.defColorHex.isNotBlank()) {
            try { Color.parseColor(config.defColorHex) } catch (e: Exception) { primaryColor }
        } else primaryColor

        val resolvedExColor = if (config.exColorHex.isNotBlank()) {
            try { Color.parseColor(config.exColorHex) } catch (e: Exception) { secondaryColor }
        } else secondaryColor

        val resolvedStreakColor = if (config.streakColorHex.isNotBlank()) {
            try { Color.parseColor(config.streakColorHex) } catch (e: Exception) { secondaryColor }
        } else secondaryColor

        val align = when (config.alignment) {
            TextAlignment.LEFT -> Paint.Align.LEFT
            TextAlignment.CENTER -> Paint.Align.CENTER
            TextAlignment.RIGHT -> Paint.Align.RIGHT
        }

        // Curated Display Fonts
        val serifTypeface = when (config.displayFont) {
            DisplayFont.EDITORIAL_SERIF -> Typeface.create(Typeface.SERIF, Typeface.NORMAL)
            DisplayFont.CLASSIC_SERIF -> Typeface.create(Typeface.SERIF, Typeface.BOLD)
            DisplayFont.MODERN_SERIF -> Typeface.create(Typeface.SERIF, Typeface.ITALIC)
            DisplayFont.CLEAN_SANS, DisplayFont.MINIMAL_SANS -> Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
            DisplayFont.HUMANIST_SANS -> Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            DisplayFont.TYPEWRITER -> Typeface.create(Typeface.MONOSPACE, Typeface.NORMAL)
        }

        val sansTypeface = when (config.bodyFont) {
            BodyFont.EDITORIAL_SANS -> Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
            BodyFont.SYSTEM_SANS -> Typeface.DEFAULT
        }

        // 1. Word Paint
        wordPaint.apply {
            color = resolvedWordColor
            textSize = (w * 0.11f * config.wordSizeScale)
            textAlign = align
            typeface = serifTypeface
            letterSpacing = config.letterSpacing
        }

        // 2. IPA Paint
        ipaPaint.apply {
            color = resolvedIpaColor
            textSize = (w * 0.040f * config.ipaSizeScale)
            textAlign = align
            typeface = sansTypeface
            letterSpacing = config.letterSpacing + 0.04f
        }

        // 3. POS Paint
        posPaint.apply {
            color = resolvedPosColor
            textSize = (w * 0.036f * config.ipaSizeScale)
            textAlign = align
            typeface = Typeface.create(sansTypeface, Typeface.ITALIC)
        }

        // 4. Definition Paint
        definitionPaint.apply {
            color = resolvedDefColor
            textSize = (w * 0.038f * config.definitionSizeScale)
            textAlign = align
            typeface = serifTypeface
        }

        // 5. Example Paint
        examplePaint.apply {
            color = resolvedExColor
            textSize = (w * 0.033f * config.exampleSizeScale)
            textAlign = align
            typeface = Typeface.create(serifTypeface, Typeface.ITALIC)
        }

        // 6. Speaker & Streak
        speakerPaint.apply {
            color = resolvedIpaColor
            textAlign = align
        }

        streakPaint.apply {
            color = resolvedStreakColor
            textAlign = align
            typeface = sansTypeface
            letterSpacing = 0.04f
        }

        dividerPaint.apply {
            color = if (isPaper) Color.parseColor("#DDD2BF") else if (isNoir) Color.parseColor("#3A2726") else Color.parseColor("#282828")
            strokeWidth = 1.5f
        }
    }

    private fun drawWrappedText(
        canvas: Canvas,
        text: String,
        anchorX: Float,
        startY: Float,
        maxWidth: Float,
        paint: Paint,
        lineSpacing: Float,
        alignment: TextAlignment
    ): Float {
        val words = text.split(" ")
        val lines = mutableListOf<String>()
        var currentLine = ""

        for (word in words) {
            val candidate = if (currentLine.isEmpty()) word else "$currentLine $word"
            if (paint.measureText(candidate) <= maxWidth) {
                currentLine = candidate
            } else {
                if (currentLine.isNotEmpty()) lines.add(currentLine)
                currentLine = word
            }
        }
        if (currentLine.isNotEmpty()) lines.add(currentLine)

        var y = startY
        for (line in lines) {
            val lineX = when (alignment) {
                TextAlignment.CENTER -> anchorX
                TextAlignment.LEFT -> anchorX - (maxWidth / 2f)
                TextAlignment.RIGHT -> anchorX + (maxWidth / 2f)
            }
            canvas.drawText(line, lineX, y, paint)
            y += lineSpacing
        }
        return y
    }
}
