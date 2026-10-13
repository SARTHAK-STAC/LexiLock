package com.sarthak.lexilock.ui.wallpaper

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sarthak.lexilock.data.model.*
import com.sarthak.lexilock.data.preferences.LexiPreferences
import com.sarthak.lexilock.data.repository.WordRepository
import com.sarthak.lexilock.domain.model.WordItem
import com.sarthak.lexilock.ui.components.LexiDivider
import com.sarthak.lexilock.ui.theme.*
import com.sarthak.lexilock.wallpaper.LexiLockWallpaperService
import com.sarthak.lexilock.wallpaper.WallpaperRenderer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.util.Locale
import java.util.UUID

enum class DragTarget(val title: String) {
    ENTIRE_CARD("All / Layout"),
    WORD("Word"),
    IPA("IPA"),
    PART_OF_SPEECH("POS"),
    DEFINITION("Definition"),
    EXAMPLE("Example"),
    STREAK("Streak")
}

enum class ColorComponentTarget(val title: String) {
    WORD("Word"),
    IPA("IPA"),
    PART_OF_SPEECH("POS"),
    DEFINITION("Definition"),
    EXAMPLE("Example"),
    STREAK("Streak")
}

private fun getLuminance(hex: String): Float {
    return try {
        val colorInt = android.graphics.Color.parseColor(hex)
        val r = android.graphics.Color.red(colorInt) / 255.0f
        val g = android.graphics.Color.green(colorInt) / 255.0f
        val b = android.graphics.Color.blue(colorInt) / 255.0f
        0.2126f * r + 0.7152f * g + 0.0722f * b
    } catch (e: Exception) {
        0.5f
    }
}

private fun calculateContrastRatio(fgHex: String, bgHex: String): Float {
    val l1 = getLuminance(fgHex)
    val l2 = getLuminance(bgHex)
    val lighter = maxOf(l1, l2)
    val darker = minOf(l1, l2)
    return (lighter + 0.05f) / (darker + 0.05f)
}

private fun getComponentHex(target: ColorComponentTarget, cfg: WallpaperConfig): String {
    return when (target) {
        ColorComponentTarget.WORD -> if (cfg.wordColorHex.isNotBlank()) cfg.wordColorHex else if (cfg.style == WallpaperStyle.NOIR) "#F2E5D2" else if (cfg.style == WallpaperStyle.PAPER) "#1E1A17" else "#FAF7EE"
        ColorComponentTarget.IPA -> if (cfg.ipaColorHex.isNotBlank()) cfg.ipaColorHex else if (cfg.style == WallpaperStyle.NOIR) "#BDAF9F" else if (cfg.style == WallpaperStyle.PAPER) "#665F56" else "#A8A8A2"
        ColorComponentTarget.PART_OF_SPEECH -> if (cfg.posColorHex.isNotBlank()) cfg.posColorHex else if (cfg.style == WallpaperStyle.NOIR) "#BDAF9F" else if (cfg.style == WallpaperStyle.PAPER) "#665F56" else "#7E96B8"
        ColorComponentTarget.DEFINITION -> if (cfg.defColorHex.isNotBlank()) cfg.defColorHex else if (cfg.style == WallpaperStyle.NOIR) "#F2E5D2" else if (cfg.style == WallpaperStyle.PAPER) "#1E1A17" else "#FAF7EE"
        ColorComponentTarget.EXAMPLE -> if (cfg.exColorHex.isNotBlank()) cfg.exColorHex else if (cfg.style == WallpaperStyle.NOIR) "#BDAF9F" else if (cfg.style == WallpaperStyle.PAPER) "#665F56" else "#A8A8A2"
        ColorComponentTarget.STREAK -> if (cfg.streakColorHex.isNotBlank()) cfg.streakColorHex else if (cfg.style == WallpaperStyle.NOIR) "#BDAF9F" else if (cfg.style == WallpaperStyle.PAPER) "#665F56" else "#A8A8A2"
    }
}

private fun updateComponentColor(config: WallpaperConfig, target: ColorComponentTarget, hex: String): WallpaperConfig {
    return when (target) {
        ColorComponentTarget.WORD -> config.copy(wordColorHex = hex)
        ColorComponentTarget.IPA -> config.copy(ipaColorHex = hex)
        ColorComponentTarget.PART_OF_SPEECH -> config.copy(posColorHex = hex)
        ColorComponentTarget.DEFINITION -> config.copy(defColorHex = hex)
        ColorComponentTarget.EXAMPLE -> config.copy(exColorHex = hex)
        ColorComponentTarget.STREAK -> config.copy(streakColorHex = hex)
    }
}

@Composable
fun WallpaperStudioScreen(
    context: Context,
    prefs: LexiPreferences,
    repository: WordRepository? = null,
    initialWordItem: WordItem,
    onApplyToSystemClick: () -> Unit,
    onBackClick: () -> Unit
) {
    val allWords = remember { repository?.getAllWords() ?: listOf(initialWordItem) }
    var currentWord by remember { mutableStateOf(initialWordItem) }

    var config by remember(currentWord.word) {
        mutableStateOf(prefs.getWordConfig(currentWord.word))
    }
    var presets by remember { mutableStateOf(prefs.customWallpaperPresets) }

    var activeTab by remember { mutableStateOf(1) }
    val tabs = listOf("Styles", "Customize Word", "Drops", "Make My Wallpaper", "Schedule", "Photo", "Presets")

    var selectedDragTarget by remember { mutableStateOf(DragTarget.ENTIRE_CARD) }
    var selectedColorTarget by remember { mutableStateOf(ColorComponentTarget.WORD) }

    var tts by remember { mutableStateOf<TextToSpeech?>(null) }
    var isTtsPlaying by remember { mutableStateOf(false) }

    DisposableEffect(context) {
        var ttsInstance: TextToSpeech? = null
        ttsInstance = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                ttsInstance?.language = Locale.US
            }
        }
        ttsInstance.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) { isTtsPlaying = true }
            override fun onDone(utteranceId: String?) { isTtsPlaying = false }
            override fun onError(utteranceId: String?) { isTtsPlaying = false }
        })
        tts = ttsInstance
        onDispose {
            ttsInstance.stop()
            ttsInstance.shutdown()
        }
    }

    var showWordSelectorDialog by remember { mutableStateOf(false) }
    var showSaveDialog by remember { mutableStateOf(false) }
    var showResetWordConfirm by remember { mutableStateOf(false) }
    var showResetAllConfirm by remember { mutableStateOf(false) }
    var pendingApplyPreset by remember { mutableStateOf<CustomWallpaperPreset?>(null) }
    var showApplyPresetDialog by remember { mutableStateOf(false) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            val localPath = copyUriToInternalStorage(context, uri)
            if (localPath != null) {
                config = config.copy(
                    style = WallpaperStyle.PHOTO,
                    photoUri = uri.toString(),
                    photoPath = localPath
                )
                prefs.setWordConfig(currentWord.word, config)
                LexiLockWallpaperService.requestRedraw()
            }
        }
    }

    val renderer = remember { WallpaperRenderer(context) }
    val previewBitmap = remember(config, currentWord, isTtsPlaying) {
        val previewW = 420
        val previewH = 680
        val bitmap = Bitmap.createBitmap(previewW, previewH, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        renderer.isAudioPlaying = isTtsPlaying
        renderer.render(canvas, currentWord, previewW, previewH, config)
        bitmap
    }

    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(LexiDarkBg)
            .padding(horizontal = 20.dp, vertical = 14.dp)
    ) {
        // TOP HEADER
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .clickable { onBackClick() },
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = "←", color = LexiDarkTextPrimary, fontSize = 20.sp)
                }
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = "Wallpaper Studio",
                        color = LexiDarkTextPrimary,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "Customizing “${currentWord.word}”",
                        color = LexiDarkAccent,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Row {
                Text(
                    text = "Apply",
                    color = LexiDarkAccent,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(LexiDarkSurfaceSubtle)
                        .clickable {
                            prefs.manualWallpaperWord = currentWord.word
                            prefs.wallpaperWordMode = "manual"
                            prefs.setWordConfig(currentWord.word, config)
                            prefs.wallpaperConfig = config
                            LexiLockWallpaperService.requestRedraw(context)
                            Toast.makeText(context, "“${currentWord.word}” set as active wallpaper", Toast.LENGTH_SHORT).show()
                            onApplyToSystemClick()
                        }
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                )

                Spacer(modifier = Modifier.width(8.dp))

                Text(
                    text = "Switch Word",
                    color = LexiDarkTextSecondary,
                    fontSize = 13.sp,
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(LexiDarkSurfaceSubtle)
                        .clickable { showWordSelectorDialog = true }
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // LIVE PREVIEW CARD
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(260.dp)
                .clip(RoundedCornerShape(14.dp))
                .border(1.dp, LexiDarkDivider, RoundedCornerShape(14.dp))
                .background(Color.Black),
            contentAlignment = Alignment.Center
        ) {
            Image(
                bitmap = previewBitmap.asImageBitmap(),
                contentDescription = "Live Wallpaper Preview",
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(selectedDragTarget, config) {
                        detectDragGestures { _, dragAmount ->
                            val deltaX = dragAmount.x / 420f
                            val deltaY = dragAmount.y / 680f
                            config = when (selectedDragTarget) {
                                DragTarget.ENTIRE_CARD -> {
                                    val newX = (config.posX + deltaX).coerceIn(0.10f, 0.90f)
                                    val newY = (config.posY + deltaY).coerceIn(0.10f, 0.90f)
                                    config.copy(posX = newX, posY = newY, autoPlacement = false)
                                }
                                DragTarget.WORD -> {
                                    val newX = (config.wordPosX + deltaX).coerceIn(0.10f, 0.90f)
                                    val newY = (config.wordPosY + deltaY).coerceIn(0.10f, 0.85f)
                                    config.copy(wordPosX = newX, wordPosY = newY, useComponentPositions = true, autoPlacement = false)
                                }
                                DragTarget.IPA -> {
                                    val newX = (config.ipaPosX + deltaX).coerceIn(0.10f, 0.90f)
                                    val newY = (config.ipaPosY + deltaY).coerceIn(0.10f, 0.85f)
                                    config.copy(ipaPosX = newX, ipaPosY = newY, useComponentPositions = true, autoPlacement = false)
                                }
                                DragTarget.PART_OF_SPEECH -> {
                                    val newX = (config.posPosX + deltaX).coerceIn(0.10f, 0.90f)
                                    val newY = (config.posPosY + deltaY).coerceIn(0.10f, 0.85f)
                                    config.copy(posPosX = newX, posPosY = newY, useComponentPositions = true, autoPlacement = false)
                                }
                                DragTarget.DEFINITION -> {
                                    val newX = (config.defPosX + deltaX).coerceIn(0.10f, 0.90f)
                                    val newY = (config.defPosY + deltaY).coerceIn(0.10f, 0.85f)
                                    config.copy(defPosX = newX, defPosY = newY, useComponentPositions = true, autoPlacement = false)
                                }
                                DragTarget.EXAMPLE -> {
                                    val newX = (config.exPosX + deltaX).coerceIn(0.10f, 0.90f)
                                    val newY = (config.exPosY + deltaY).coerceIn(0.10f, 0.85f)
                                    config.copy(exPosX = newX, exPosY = newY, useComponentPositions = true, autoPlacement = false)
                                }
                                DragTarget.STREAK -> {
                                    val newX = (config.streakPosX + deltaX).coerceIn(0.10f, 0.90f)
                                    val newY = (config.streakPosY + deltaY).coerceIn(0.10f, 0.95f)
                                    config.copy(streakPosX = newX, streakPosY = newY, useComponentPositions = true, autoPlacement = false)
                                }
                            }
                            prefs.setWordConfig(currentWord.word, config)
                            LexiLockWallpaperService.requestRedraw()
                        }
                    }
            )

            // Drag mode & pronunciation tester in top bar of preview card
            Row(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Drag: ${selectedDragTarget.title}",
                    color = Color.White.copy(alpha = 0.6f),
                    fontSize = 11.sp
                )
                // Typographic Audio Test Mark
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(if (isTtsPlaying) LexiDarkAccent.copy(alpha = 0.3f) else Color.White.copy(alpha = 0.12f))
                        .clickable {
                            tts?.speak(currentWord.word, TextToSpeech.QUEUE_FLUSH, null, "studio_tap")
                        }
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = if (isTtsPlaying) "◉))) Playing" else "◉))) Listen",
                        color = if (isTtsPlaying) LexiDarkAccent else Color.White.copy(alpha = 0.85f),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
                Text(
                    text = if (config.autoPlacement) "Auto Layout" else "Manual",
                    color = Color.White.copy(alpha = 0.6f),
                    fontSize = 11.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // DRAG TARGET SELECTOR CHIPS
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            DragTarget.values().forEach { target ->
                val isSelected = selectedDragTarget == target
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (isSelected) LexiDarkAccent.copy(alpha = 0.2f) else LexiDarkSurfaceSubtle)
                        .border(1.dp, if (isSelected) LexiDarkAccent else LexiDarkDivider, RoundedCornerShape(6.dp))
                        .clickable { selectedDragTarget = target }
                        .padding(horizontal = 10.dp, vertical = 5.dp)
                ) {
                    Text(
                        text = target.title,
                        color = if (isSelected) LexiDarkAccent else LexiDarkTextSecondary,
                        fontSize = 11.sp,
                        fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Normal
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // STUDIO TABS: Styles | Customize Word | Photo | Presets
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            tabs.forEachIndexed { index, tab ->
                val isSelected = activeTab == index
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isSelected) LexiDarkSurfaceSubtle else Color.Transparent)
                        .border(
                            1.dp,
                            if (isSelected) LexiDarkAccent.copy(alpha = 0.6f) else LexiDarkDivider,
                            RoundedCornerShape(8.dp)
                        )
                        .clickable { activeTab = index }
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = tab,
                        color = if (isSelected) LexiDarkAccent else LexiDarkTextSecondary,
                        fontSize = 13.sp,
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))
        LexiDivider(color = LexiDarkDivider)
        Spacer(modifier = Modifier.height(10.dp))

        // TAB CONTENT
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(scrollState)
        ) {
            when (activeTab) {
                0 -> TabStylesContent(
                    config = config,
                    onStyleSelect = { style ->
                        config = config.copy(style = style)
                        prefs.setWordConfig(currentWord.word, config)
                        LexiLockWallpaperService.requestRedraw()
                    }
                )
                1 -> TabCustomizeWordContent(
                    config = config,
                    currentWord = currentWord,
                    selectedColorTarget = selectedColorTarget,
                    onColorTargetSelect = { selectedColorTarget = it },
                    onConfigChange = { newCfg ->
                        config = newCfg
                        prefs.setWordConfig(currentWord.word, newCfg)
                        LexiLockWallpaperService.requestRedraw()
                    },
                    onResetColors = {
                        config = config.copy(
                            customBgColorHex = "",
                            wordColorHex = "",
                            ipaColorHex = "",
                            posColorHex = "",
                            defColorHex = "",
                            exColorHex = "",
                            streakColorHex = ""
                        )
                        prefs.setWordConfig(currentWord.word, config)
                        LexiLockWallpaperService.requestRedraw()
                        Toast.makeText(context, "Colors reset to style defaults", Toast.LENGTH_SHORT).show()
                    },
                    onResetWord = { showResetWordConfirm = true },
                    onResetAllWords = { showResetAllConfirm = true },
                    onSavePreset = { showSaveDialog = true }
                )
                2 -> TabDropsContent(
                    currentConfig = config,
                    onApplyDrop = { drop ->
                        config = config.copy(
                            style = drop.style,
                            customBgColorHex = drop.bgHex,
                            wordColorHex = drop.wordHex,
                            accentColorHex = drop.accentHex,
                            posColorHex = drop.accentHex,
                            displayFont = drop.font,
                            alignment = drop.alignment
                        )
                        prefs.setWordConfig(currentWord.word, config)
                        prefs.wallpaperConfig = config
                        LexiLockWallpaperService.requestRedraw(context)
                        Toast.makeText(context, "${drop.name} drop applied to wallpaper", Toast.LENGTH_SHORT).show()
                    }
                )
                3 -> TabMakeMyWallpaperContent(
                    config = config,
                    currentWord = currentWord,
                    onConfigChange = { newCfg ->
                        config = newCfg
                        prefs.setWordConfig(currentWord.word, newCfg)
                        prefs.wallpaperConfig = newCfg
                        LexiLockWallpaperService.requestRedraw(context)
                    },
                    onOpenWordSelector = { showWordSelectorDialog = true }
                )
                4 -> TabScheduleContent(
                    repository = repository,
                    prefs = prefs,
                    currentWord = currentWord,
                    onSelectScheduledWord = { selected ->
                        currentWord = selected
                        config = prefs.getWordConfig(selected.word)
                    },
                    onSwitchToDailyMode = {
                        prefs.wallpaperWordMode = "daily"
                        LexiLockWallpaperService.requestRedraw(context)
                        Toast.makeText(context, "Resumed scheduled daily rotation", Toast.LENGTH_SHORT).show()
                    }
                )
                5 -> TabPhotoContent(
                    config = config,
                    onPickPhotoClick = { photoPickerLauncher.launch("image/*") },
                    onTreatmentChange = { treatment ->
                        config = config.copy(photoTreatment = treatment)
                        prefs.setWordConfig(currentWord.word, config)
                        LexiLockWallpaperService.requestRedraw()
                    }
                )
                6 -> TabPresetsContent(
                    presets = presets,
                    onApplyPreset = { preset ->
                        pendingApplyPreset = preset
                        showApplyPresetDialog = true
                    },
                    onDeletePreset = { presetId ->
                        prefs.deletePreset(presetId)
                        presets = prefs.customWallpaperPresets
                    }
                )
            }
        }
    }

    StudioDialogs(
        context = context,
        prefs = prefs,
        repository = repository,
        allWords = allWords,
        currentWord = currentWord,
        config = config,
        presets = presets,
        showWordSelectorDialog = showWordSelectorDialog,
        onDismissWordSelector = { showWordSelectorDialog = false },
        onSelectWord = { item ->
            currentWord = item
            config = prefs.getWordConfig(item.word)
            showWordSelectorDialog = false
        },
        showApplyPresetDialog = showApplyPresetDialog,
        pendingApplyPreset = pendingApplyPreset,
        onDismissApplyPreset = {
            showApplyPresetDialog = false
            pendingApplyPreset = null
        },
        onPresetApplied = { updatedCfg ->
            config = updatedCfg
            showApplyPresetDialog = false
            pendingApplyPreset = null
        },
        showSaveDialog = showSaveDialog,
        onDismissSaveDialog = { showSaveDialog = false },
        onPresetSaved = { updatedPresets ->
            presets = updatedPresets
            showSaveDialog = false
        },
        showResetWordConfirm = showResetWordConfirm,
        onDismissResetWord = { showResetWordConfirm = false },
        onConfirmResetWord = {
            prefs.resetWordConfig(currentWord.word)
            config = prefs.getWordConfig(currentWord.word)
            LexiLockWallpaperService.requestRedraw()
            showResetWordConfirm = false
            Toast.makeText(context, "Reset custom layout for “${currentWord.word}”", Toast.LENGTH_SHORT).show()
        },
        showResetAllConfirm = showResetAllConfirm,
        onDismissResetAll = { showResetAllConfirm = false },
        onConfirmResetAll = {
            prefs.resetAllWordCustomizations()
            config = prefs.getWordConfig(currentWord.word)
            LexiLockWallpaperService.requestRedraw()
            showResetAllConfirm = false
            Toast.makeText(context, "All word configurations reset to defaults", Toast.LENGTH_SHORT).show()
        }
    )
}

@Composable
private fun TabStylesContent(
    config: WallpaperConfig,
    onStyleSelect: (WallpaperStyle) -> Unit
) {
    Text(text = "EDITORIAL STYLES", color = LexiDarkTextSecondary, fontSize = 12.sp, fontWeight = FontWeight.Medium)
    Spacer(modifier = Modifier.height(8.dp))

    val styles = listOf(
        WallpaperStyle.OBSIDIAN to "Cinematic dark landscape with mountain & moon",
        WallpaperStyle.MINIMAL to "Typography-only dictionary aesthetic with pure whitespace",
        WallpaperStyle.AURORA to "Subtle soft atmospheric ribbon; typography remains hero",
        WallpaperStyle.PAPER to "Warm cream editorial modern book page",
        WallpaperStyle.MONOCHROME to "High-contrast editorial black & white depth",
        WallpaperStyle.PHOTO to "Personal photograph with adaptive readability",
        WallpaperStyle.EDITORIAL to "Asymmetric magazine composition with architectural divider",
        WallpaperStyle.MUSEUM to "Museum exhibit placard title aesthetic",
        WallpaperStyle.INK to "Literary carbon monograph with fine ink rule",
        WallpaperStyle.NOIR to "Deep warm near-black brown with literary cream serif typography",
        WallpaperStyle.CUSTOM to "Fully user-configurable typography layer"
    )

    styles.forEach { (style, subtitle) ->
        val isSelected = config.style == style
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(if (isSelected) LexiDarkSurfaceSubtle else Color.Transparent)
                .clickable { onStyleSelect(style) }
                .padding(vertical = 10.dp, horizontal = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = style.title,
                    color = if (isSelected) LexiDarkAccent else LexiDarkTextPrimary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    color = LexiDarkTextSecondary,
                    fontSize = 12.sp
                )
            }
            if (isSelected) {
                Text(text = "✓", color = LexiDarkAccent, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
    }
}

@Composable
private fun TabCustomizeWordContent(
    config: WallpaperConfig,
    currentWord: WordItem,
    selectedColorTarget: ColorComponentTarget,
    onColorTargetSelect: (ColorComponentTarget) -> Unit,
    onConfigChange: (WallpaperConfig) -> Unit,
    onResetColors: () -> Unit,
    onResetWord: () -> Unit,
    onResetAllWords: () -> Unit,
    onSavePreset: () -> Unit
) {
    // Layout Mode (AUTO vs MANUAL)
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = "Smart Auto Layout", color = LexiDarkTextPrimary, fontSize = 14.sp)
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf("AUTO", "MANUAL").forEach { mode ->
                val isAuto = mode == "AUTO"
                val isSelected = if (isAuto) config.autoPlacement else !config.autoPlacement
                Text(
                    text = mode,
                    color = if (isSelected) LexiDarkAccent else LexiDarkTextSecondary,
                    fontSize = 12.sp,
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(if (isSelected) LexiDarkSurfaceSubtle else Color.Transparent)
                        .clickable {
                            onConfigChange(
                                config.copy(
                                    autoPlacement = isAuto,
                                    useComponentPositions = if (isAuto) false else config.useComponentPositions
                                )
                            )
                        }
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                )
            }
        }
    }

    Spacer(modifier = Modifier.height(16.dp))

    // CURATED TYPOGRAPHY (5 curated fonts)
    Text(text = "TYPOGRAPHY", color = LexiDarkTextSecondary, fontSize = 12.sp, fontWeight = FontWeight.Medium)
    Spacer(modifier = Modifier.height(8.dp))
    Row(
        modifier = Modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        DisplayFont.values().forEach { font ->
            val isSelected = config.displayFont == font
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(if (isSelected) LexiDarkSurfaceSubtle else Color.Transparent)
                    .border(1.dp, if (isSelected) LexiDarkAccent else LexiDarkDivider, RoundedCornerShape(6.dp))
                    .clickable { onConfigChange(config.copy(displayFont = font)) }
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Text(text = font.title, color = if (isSelected) LexiDarkAccent else LexiDarkTextSecondary, fontSize = 12.sp)
            }
        }
    }

    Spacer(modifier = Modifier.height(16.dp))

    // COLOR CUSTOMIZATION SECTION
    EditorialColorSection(
        config = config,
        currentWord = currentWord,
        selectedColorTarget = selectedColorTarget,
        onColorTargetSelect = onColorTargetSelect,
        onConfigChange = onConfigChange,
        onResetColors = onResetColors
    )

    Spacer(modifier = Modifier.height(16.dp))

    // POSITION PRESETS & ALIGNMENT
    Text(text = "POSITION & ALIGNMENT", color = LexiDarkTextSecondary, fontSize = 12.sp, fontWeight = FontWeight.Medium)
    Spacer(modifier = Modifier.height(8.dp))
    Row(
        modifier = Modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        VocabPosition.values().forEach { pos ->
            val isSelected = config.positionPreset == pos && !config.autoPlacement
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(if (isSelected) LexiDarkSurfaceSubtle else Color.Transparent)
                    .border(1.dp, if (isSelected) LexiDarkAccent else LexiDarkDivider, RoundedCornerShape(6.dp))
                    .clickable {
                        onConfigChange(
                            config.copy(
                                positionPreset = pos,
                                posY = pos.defaultY,
                                autoPlacement = false
                            )
                        )
                    }
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Text(text = pos.title, color = if (isSelected) LexiDarkAccent else LexiDarkTextSecondary, fontSize = 12.sp)
            }
        }
    }

    Spacer(modifier = Modifier.height(8.dp))

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = "Text Alignment", color = LexiDarkTextPrimary, fontSize = 14.sp)
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            TextAlignment.values().forEach { align ->
                val isSelected = config.alignment == align
                Text(
                    text = align.title,
                    color = if (isSelected) LexiDarkAccent else LexiDarkTextSecondary,
                    fontSize = 12.sp,
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(if (isSelected) LexiDarkSurfaceSubtle else Color.Transparent)
                        .clickable { onConfigChange(config.copy(alignment = align)) }
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                )
            }
        }
    }

    Spacer(modifier = Modifier.height(16.dp))

    // SCALES & TYPOGRAPHY METRICS
    Text(text = "TYPOGRAPHY SCALES", color = LexiDarkTextSecondary, fontSize = 12.sp, fontWeight = FontWeight.Medium)
    Spacer(modifier = Modifier.height(8.dp))

    TypographyScaleStepper(
        label = "Word scale",
        scale = config.wordSizeScale,
        steps = listOf("70%" to 0.70f, "85%" to 0.85f, "100%" to 1.0f, "115%" to 1.15f, "130%" to 1.30f),
        onScaleChange = { onConfigChange(config.copy(wordSizeScale = it)) }
    )

    Spacer(modifier = Modifier.height(8.dp))

    TypographyScaleStepper(
        label = "Definition scale",
        scale = config.definitionSizeScale,
        steps = listOf("75%" to 0.75f, "85%" to 0.85f, "100%" to 1.0f, "115%" to 1.15f),
        onScaleChange = { onConfigChange(config.copy(definitionSizeScale = it)) }
    )

    Spacer(modifier = Modifier.height(8.dp))

    SliderControl(
        label = "Line spacing",
        value = config.lineSpacingScale,
        valueRange = 0.8f..1.5f,
        onValueChange = { onConfigChange(config.copy(lineSpacingScale = it)) }
    )

    SliderControl(
        label = "Letter spacing",
        value = config.letterSpacing,
        valueRange = -0.05f..0.15f,
        onValueChange = { onConfigChange(config.copy(letterSpacing = it)) }
    )

    Spacer(modifier = Modifier.height(16.dp))

    // SHOW / HIDE COMPONENTS
    Text(text = "SHOW / HIDE COMPONENTS", color = LexiDarkTextSecondary, fontSize = 12.sp, fontWeight = FontWeight.Medium)
    Spacer(modifier = Modifier.height(6.dp))

    SwitchRow(label = "Word", checked = config.showWord) { onConfigChange(config.copy(showWord = it)) }
    SwitchRow(label = "IPA Pronunciation", checked = config.showIpa) { onConfigChange(config.copy(showIpa = it)) }
    SwitchRow(label = "Part of Speech", checked = config.showPartOfSpeech) { onConfigChange(config.copy(showPartOfSpeech = it)) }
    SwitchRow(label = "Definition", checked = config.showDefinition) { onConfigChange(config.copy(showDefinition = it)) }
    SwitchRow(label = "Example", checked = config.showExample) { onConfigChange(config.copy(showExample = it)) }
    SwitchRow(label = "Streak Count", checked = config.showStreak) { onConfigChange(config.copy(showStreak = it)) }

    Spacer(modifier = Modifier.height(16.dp))

    // RESET & SAVE PRESET BUTTONS
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Button(
            onClick = onResetWord,
            colors = ButtonDefaults.buttonColors(containerColor = LexiDarkSurfaceSubtle),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.weight(1f)
        ) {
            Text(text = "Reset this word", color = LexiDarkTextPrimary, fontSize = 12.sp)
        }
        Button(
            onClick = onResetAllWords,
            colors = ButtonDefaults.buttonColors(containerColor = LexiDarkSurfaceSubtle),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.weight(1f)
        ) {
            Text(text = "Reset all words", color = Color.Red.copy(alpha = 0.8f), fontSize = 12.sp)
        }
    }

    Spacer(modifier = Modifier.height(10.dp))
    Button(
        onClick = onSavePreset,
        colors = ButtonDefaults.buttonColors(containerColor = LexiDarkSurfaceSubtle),
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(text = "Save as Reusable Preset", color = LexiDarkAccent, fontSize = 13.sp)
    }
}

@Composable
private fun EditorialColorSection(
    config: WallpaperConfig,
    currentWord: WordItem,
    selectedColorTarget: ColorComponentTarget,
    onColorTargetSelect: (ColorComponentTarget) -> Unit,
    onConfigChange: (WallpaperConfig) -> Unit,
    onResetColors: () -> Unit
) {
    // 1. CURATED COLOR PALETTES
    Text(text = "COLOR PALETTES", color = LexiDarkTextSecondary, fontSize = 12.sp, fontWeight = FontWeight.Medium)
    Spacer(modifier = Modifier.height(8.dp))
    Row(
        modifier = Modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        EditorialColorPalette.values().forEach { palette ->
            val isSelected = config.customBgColorHex.equals(palette.bgHex, ignoreCase = true) &&
                             config.wordColorHex.equals(palette.wordHex, ignoreCase = true)
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (isSelected) LexiDarkSurfaceSubtle else Color.Transparent)
                    .border(1.dp, if (isSelected) LexiDarkAccent else LexiDarkDivider, RoundedCornerShape(8.dp))
                    .clickable {
                        onConfigChange(
                            config.copy(
                                customBgColorHex = palette.bgHex,
                                wordColorHex = palette.wordHex,
                                ipaColorHex = palette.secondaryHex,
                                posColorHex = palette.secondaryHex,
                                defColorHex = palette.wordHex,
                                exColorHex = palette.secondaryHex,
                                streakColorHex = palette.secondaryHex
                            )
                        )
                    }
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .clip(CircleShape)
                            .background(Color(android.graphics.Color.parseColor(palette.bgHex)))
                            .border(0.5.dp, Color.White.copy(alpha = 0.3f), CircleShape)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(Color(android.graphics.Color.parseColor(palette.wordHex)))
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = palette.title,
                        color = if (isSelected) LexiDarkAccent else LexiDarkTextSecondary,
                        fontSize = 12.sp,
                        fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Normal
                    )
                }
            }
        }
    }

    Spacer(modifier = Modifier.height(16.dp))

    // 2. BACKGROUND COLOR CONTROL
    Text(text = "BACKGROUND COLOR", color = LexiDarkTextSecondary, fontSize = 12.sp, fontWeight = FontWeight.Medium)
    Spacer(modifier = Modifier.height(8.dp))

    val bgPresets = listOf(
        "#241516" to "Noir",
        "#101010" to "Obsidian",
        "#000000" to "Pure Black",
        "#17221C" to "Forest",
        "#111827" to "Midnight",
        "#38251F" to "Clay",
        "#F3EBDD" to "Warm Paper",
        "#F5EFEB" to "Paper",
        "#FFFFFF" to "White",
        "#243447" to "Navy"
    )
    Row(
        modifier = Modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        bgPresets.forEach { (hex, title) ->
            val isSelected = config.customBgColorHex.equals(hex, ignoreCase = true) ||
                             (config.customBgColorHex.isEmpty() && config.style == WallpaperStyle.NOIR && hex == "#241516")
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(if (isSelected) LexiDarkSurfaceSubtle else Color.Transparent)
                    .border(1.dp, if (isSelected) LexiDarkAccent else LexiDarkDivider, RoundedCornerShape(6.dp))
                    .clickable { onConfigChange(config.copy(customBgColorHex = hex)) }
                    .padding(horizontal = 8.dp, vertical = 5.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .clip(CircleShape)
                            .background(try { Color(android.graphics.Color.parseColor(hex)) } catch (e: Exception) { Color.Black })
                            .border(0.5.dp, Color.White.copy(alpha = 0.4f), CircleShape)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = title, color = if (isSelected) LexiDarkAccent else LexiDarkTextSecondary, fontSize = 11.sp)
                }
            }
        }
    }

    Spacer(modifier = Modifier.height(8.dp))

    var bgHexText by remember(config.customBgColorHex) {
        mutableStateOf(if (config.customBgColorHex.isNotBlank()) config.customBgColorHex else if (config.style == WallpaperStyle.NOIR) "#241516" else "#101010")
    }
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(try { Color(android.graphics.Color.parseColor(bgHexText)) } catch (e: Exception) { Color.DarkGray })
                    .border(1.dp, LexiDarkDivider, RoundedCornerShape(4.dp))
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = "Custom Background Hex:", color = LexiDarkTextSecondary, fontSize = 12.sp)
        }
        OutlinedTextField(
            value = bgHexText,
            onValueChange = { input ->
                bgHexText = input
                if (input.matches(Regex("^#([A-Fa-f0-9]{6})$"))) {
                    onConfigChange(config.copy(customBgColorHex = input))
                }
            },
            singleLine = true,
            modifier = Modifier.width(130.dp).height(48.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = LexiDarkAccent,
                unfocusedBorderColor = LexiDarkDivider,
                focusedTextColor = LexiDarkTextPrimary,
                unfocusedTextColor = LexiDarkTextPrimary
            ),
            textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp)
        )
    }

    Spacer(modifier = Modifier.height(16.dp))

    // 3. COMPONENT COLOR CONTROL
    Text(text = "TEXT COMPONENT COLOR", color = LexiDarkTextSecondary, fontSize = 12.sp, fontWeight = FontWeight.Medium)
    Spacer(modifier = Modifier.height(8.dp))

    Row(
        modifier = Modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        ColorComponentTarget.values().forEach { target ->
            val isSelected = selectedColorTarget == target
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(if (isSelected) LexiDarkAccent.copy(alpha = 0.2f) else LexiDarkSurfaceSubtle)
                    .border(1.dp, if (isSelected) LexiDarkAccent else LexiDarkDivider, RoundedCornerShape(6.dp))
                    .clickable { onColorTargetSelect(target) }
                    .padding(horizontal = 10.dp, vertical = 5.dp)
            ) {
                Text(
                    text = target.title,
                    color = if (isSelected) LexiDarkAccent else LexiDarkTextSecondary,
                    fontSize = 11.sp,
                    fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Normal
                )
            }
        }
    }

    Spacer(modifier = Modifier.height(8.dp))

    val textPresets = listOf(
        "#F2E5D2" to "Cream",
        "#FAF7EE" to "Warm White",
        "#F4EFE6" to "Alabaster",
        "#BDAF9F" to "Beige",
        "#D7CBBE" to "Muted Tan",
        "#A5A5A5" to "Soft Gray",
        "#7E96B8" to "Muted Blue",
        "#B87E7E" to "Muted Red",
        "#7EB88E" to "Muted Green",
        "#201A17" to "Charcoal",
        "#121212" to "Black"
    )
    Row(
        modifier = Modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        textPresets.forEach { (hex, title) ->
            val curHex = getComponentHex(selectedColorTarget, config)
            val isSelected = curHex.equals(hex, ignoreCase = true)
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(if (isSelected) LexiDarkSurfaceSubtle else Color.Transparent)
                    .border(1.dp, if (isSelected) LexiDarkAccent else LexiDarkDivider, RoundedCornerShape(6.dp))
                    .clickable { onConfigChange(updateComponentColor(config, selectedColorTarget, hex)) }
                    .padding(horizontal = 8.dp, vertical = 5.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(try { Color(android.graphics.Color.parseColor(hex)) } catch (e: Exception) { Color.White })
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(text = title, color = if (isSelected) LexiDarkAccent else LexiDarkTextSecondary, fontSize = 11.sp)
                }
            }
        }
    }

    Spacer(modifier = Modifier.height(8.dp))

    val currentTargetHex = getComponentHex(selectedColorTarget, config)
    var compHexText by remember(currentTargetHex, selectedColorTarget) {
        mutableStateOf(currentTargetHex)
    }
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(try { Color(android.graphics.Color.parseColor(compHexText)) } catch (e: Exception) { Color.LightGray })
                    .border(1.dp, LexiDarkDivider, RoundedCornerShape(4.dp))
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = "${selectedColorTarget.title} Hex:", color = LexiDarkTextSecondary, fontSize = 12.sp)
        }
        OutlinedTextField(
            value = compHexText,
            onValueChange = { input ->
                compHexText = input
                if (input.matches(Regex("^#([A-Fa-f0-9]{6})$"))) {
                    onConfigChange(updateComponentColor(config, selectedColorTarget, input))
                }
            },
            singleLine = true,
            modifier = Modifier.width(130.dp).height(48.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = LexiDarkAccent,
                unfocusedBorderColor = LexiDarkDivider,
                focusedTextColor = LexiDarkTextPrimary,
                unfocusedTextColor = LexiDarkTextPrimary
            ),
            textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp)
        )
    }

    // 4. CONTRAST / READABILITY SAFEGUARD
    val effectiveBgHex = if (config.customBgColorHex.isNotBlank()) config.customBgColorHex else if (config.style == WallpaperStyle.NOIR) "#241516" else if (config.style == WallpaperStyle.PAPER) "#F5EFEB" else "#101010"
    val effectiveWordHex = if (config.wordColorHex.isNotBlank()) config.wordColorHex else if (config.style == WallpaperStyle.NOIR) "#F2E5D2" else if (config.style == WallpaperStyle.PAPER) "#1E1A17" else "#FAF7EE"
    val contrastRatio = calculateContrastRatio(effectiveWordHex, effectiveBgHex)

    if (contrastRatio < 3.0f) {
        Spacer(modifier = Modifier.height(10.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0xFF332A15))
                .border(1.dp, Color(0xFF8A6D2B), RoundedCornerShape(8.dp))
                .padding(10.dp)
        ) {
            Column {
                Text(
                    text = "Low contrast — readability may be affected",
                    color = Color(0xFFFFD580),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(6.dp))
                Button(
                    onClick = {
                        val bgLum = getLuminance(effectiveBgHex)
                        val improvedWordColor = if (bgLum < 0.5f) "#F2E5D2" else "#1A1A1A"
                        val improvedSecColor = if (bgLum < 0.5f) "#BDAF9F" else "#555555"
                        onConfigChange(
                            config.copy(
                                wordColorHex = improvedWordColor,
                                defColorHex = improvedWordColor,
                                ipaColorHex = improvedSecColor,
                                posColorHex = improvedSecColor,
                                exColorHex = improvedSecColor,
                                streakColorHex = improvedSecColor
                            )
                        )
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF8A6D2B)),
                    shape = RoundedCornerShape(6.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                    modifier = Modifier.height(32.dp)
                ) {
                    Text(text = "Improve automatically", color = Color.White, fontSize = 11.sp)
                }
            }
        }
    }

    Spacer(modifier = Modifier.height(10.dp))

    // 5. RESET COLORS BUTTON
    Button(
        onClick = onResetColors,
        colors = ButtonDefaults.buttonColors(containerColor = LexiDarkSurfaceSubtle),
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(text = "Reset colors to style defaults", color = LexiDarkAccent, fontSize = 12.sp)
    }
}

@Composable
private fun TabScheduleContent(
    repository: WordRepository?,
    prefs: LexiPreferences,
    currentWord: WordItem,
    onSelectScheduledWord: (WordItem) -> Unit,
    onSwitchToDailyMode: () -> Unit
) {
    val schedule = remember { repository?.getDailyWordSchedule() ?: emptyList() }
    val isManualMode = prefs.wallpaperWordMode == "manual"

    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        // MODE BANNER
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(LexiDarkSurfaceSubtle)
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (isManualMode) "Manual Pin Mode Active" else "Daily Rotation Schedule",
                    color = if (isManualMode) LexiDarkAccent else LexiDarkTextPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = if (isManualMode)
                        "Wallpaper is pinned to “${prefs.manualWallpaperWord}”. It will not change automatically."
                    else
                        "${schedule.size} words scheduled today (${24 / schedule.size.coerceAtLeast(1)}h per slot).",
                    color = LexiDarkTextSecondary,
                    fontSize = 12.sp
                )
            }

            if (isManualMode) {
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = onSwitchToDailyMode,
                    colors = ButtonDefaults.buttonColors(containerColor = LexiDarkAccent),
                    shape = RoundedCornerShape(6.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    modifier = Modifier.height(32.dp)
                ) {
                    Text("Resume Schedule", color = LexiDarkBg, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "TODAY'S ROTATION SLOTS",
            color = LexiDarkTextSecondary,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 1.sp
        )
        Spacer(modifier = Modifier.height(8.dp))

        if (schedule.isEmpty()) {
            Text(
                text = "No scheduled slots available.",
                color = LexiDarkTextSecondary,
                fontSize = 13.sp
            )
        } else {
            schedule.forEach { slot ->
                val isSlotCurrent = slot.isCurrent
                val isSelectedInStudio = slot.word.word.equals(currentWord.word, ignoreCase = true)

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(
                            if (isSelectedInStudio) LexiDarkAccent.copy(alpha = 0.12f)
                            else LexiDarkSurfaceSubtle
                        )
                        .clickable { onSelectScheduledWord(slot.word) }
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = slot.word.word.replaceFirstChar { it.uppercase() },
                                color = if (isSelectedInStudio) LexiDarkAccent else LexiDarkTextPrimary,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            if (isSlotCurrent) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "LIVE NOW",
                                    color = LexiDarkAccent,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(LexiDarkAccent.copy(alpha = 0.2f))
                                        .padding(horizontal = 5.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(3.dp))
                        Text(
                            text = "${slot.timeRangeText}  •  ${slot.countdownText}",
                            color = LexiDarkTextSecondary,
                            fontSize = 12.sp
                        )
                    }

                    Text(
                        text = if (isSelectedInStudio) "Viewing" else "Select",
                        color = if (isSelectedInStudio) LexiDarkAccent else LexiDarkTextSecondary,
                        fontSize = 12.sp,
                        fontWeight = if (isSelectedInStudio) FontWeight.SemiBold else FontWeight.Normal
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun TabPhotoContent(
    config: WallpaperConfig,
    onPickPhotoClick: () -> Unit,
    onTreatmentChange: (PhotoTreatment) -> Unit
) {
    Text(text = "PERSONAL PHOTOGRAPH", color = LexiDarkTextSecondary, fontSize = 12.sp, fontWeight = FontWeight.Medium)
    Spacer(modifier = Modifier.height(10.dp))

    Button(
        onClick = onPickPhotoClick,
        colors = ButtonDefaults.buttonColors(containerColor = LexiDarkSurfaceSubtle),
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = if (config.photoPath.isNotBlank()) "Change Selected Photo" else "Select Photo with Android Picker",
            color = LexiDarkTextPrimary,
            fontSize = 14.sp
        )
    }

    if (config.photoPath.isNotBlank()) {
        Spacer(modifier = Modifier.height(20.dp))
        Text(text = "AUTOMATIC READABILITY TREATMENT", color = LexiDarkTextSecondary, fontSize = 12.sp, fontWeight = FontWeight.Medium)
        Spacer(modifier = Modifier.height(8.dp))

        Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            PhotoTreatment.values().forEach { treatment ->
                val isSelected = config.photoTreatment == treatment
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (isSelected) LexiDarkSurfaceSubtle else Color.Transparent)
                        .border(1.dp, if (isSelected) LexiDarkAccent else LexiDarkDivider, RoundedCornerShape(6.dp))
                        .clickable { onTreatmentChange(treatment) }
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text(text = treatment.title, color = if (isSelected) LexiDarkAccent else LexiDarkTextSecondary, fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
private fun TabPresetsContent(
    presets: List<CustomWallpaperPreset>,
    onApplyPreset: (CustomWallpaperPreset) -> Unit,
    onDeletePreset: (String) -> Unit
) {
    Text(text = "SAVED PRESETS", color = LexiDarkTextSecondary, fontSize = 12.sp, fontWeight = FontWeight.Medium)
    Spacer(modifier = Modifier.height(8.dp))

    if (presets.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 24.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "No saved presets yet.\nCustomize a word layout and tap “Save as Reusable Preset” to store it here.",
                color = LexiDarkTextSecondary,
                fontSize = 13.sp,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                lineHeight = 18.sp
            )
        }
    } else {
        presets.forEach { preset ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(LexiDarkSurfaceSubtle)
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = preset.name,
                        color = LexiDarkTextPrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "${preset.config.style.title} • ${preset.config.displayFont.title}",
                        color = LexiDarkTextSecondary,
                        fontSize = 12.sp
                    )
                }
                Row {
                    Text(
                        text = "Apply",
                        color = LexiDarkAccent,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier
                            .clickable { onApplyPreset(preset) }
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Delete",
                        color = Color.Red.copy(alpha = 0.7f),
                        fontSize = 13.sp,
                        modifier = Modifier
                            .clickable { onDeletePreset(preset.id) }
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
private fun StudioDialogs(
    context: Context,
    prefs: LexiPreferences,
    repository: WordRepository?,
    allWords: List<WordItem>,
    currentWord: WordItem,
    config: WallpaperConfig,
    presets: List<CustomWallpaperPreset>,
    showWordSelectorDialog: Boolean,
    onDismissWordSelector: () -> Unit,
    onSelectWord: (WordItem) -> Unit,
    showApplyPresetDialog: Boolean,
    pendingApplyPreset: CustomWallpaperPreset?,
    onDismissApplyPreset: () -> Unit,
    onPresetApplied: (WallpaperConfig) -> Unit,
    showSaveDialog: Boolean,
    onDismissSaveDialog: () -> Unit,
    onPresetSaved: (List<CustomWallpaperPreset>) -> Unit,
    showResetWordConfirm: Boolean,
    onDismissResetWord: () -> Unit,
    onConfirmResetWord: () -> Unit,
    showResetAllConfirm: Boolean,
    onDismissResetAll: () -> Unit,
    onConfirmResetAll: () -> Unit
) {
    var wordSearchQuery by remember { mutableStateOf("") }
    var presetNameInput by remember { mutableStateOf("") }

    // 1. SELECT VOCABULARY WORD DIALOG
    if (showWordSelectorDialog) {
        var wordSelectorTab by remember { mutableIntStateOf(0) } // 0 = Today's Words, 1 = All Words
        val scheduledSlots = remember { repository?.getDailyWordSchedule() ?: emptyList() }
        val filteredWords = remember(wordSearchQuery, allWords) {
            if (wordSearchQuery.isBlank()) allWords.take(50)
            else allWords.filter { it.word.contains(wordSearchQuery.trim(), ignoreCase = true) || it.definition.contains(wordSearchQuery.trim(), ignoreCase = true) }.take(50)
        }

        AlertDialog(
            onDismissRequest = onDismissWordSelector,
            title = {
                Text(
                    text = "Select Word for Wallpaper",
                    color = LexiDarkTextPrimary,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.SemiBold
                )
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth().heightIn(max = 420.dp)) {
                    // TAB SELECTOR: TODAY'S SCHEDULE vs ALL WORDS
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(LexiDarkBg)
                            .padding(4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (wordSelectorTab == 0) LexiDarkSurfaceSubtle else Color.Transparent)
                                .clickable { wordSelectorTab = 0 }
                                .padding(vertical = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Today's Words (${scheduledSlots.size})",
                                color = if (wordSelectorTab == 0) LexiDarkAccent else LexiDarkTextSecondary,
                                fontSize = 12.sp,
                                fontWeight = if (wordSelectorTab == 0) FontWeight.SemiBold else FontWeight.Normal
                            )
                        }

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (wordSelectorTab == 1) LexiDarkSurfaceSubtle else Color.Transparent)
                                .clickable { wordSelectorTab = 1 }
                                .padding(vertical = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "All Words (${allWords.size})",
                                color = if (wordSelectorTab == 1) LexiDarkAccent else LexiDarkTextSecondary,
                                fontSize = 12.sp,
                                fontWeight = if (wordSelectorTab == 1) FontWeight.SemiBold else FontWeight.Normal
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    if (wordSelectorTab == 0) {
                        // TODAY'S WORDS LIST WITH TIME SLOTS
                        Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                            if (scheduledSlots.isEmpty()) {
                                Text(
                                    text = "No scheduled words found for today.",
                                    color = LexiDarkTextSecondary,
                                    fontSize = 13.sp,
                                    modifier = Modifier.padding(16.dp)
                                )
                            } else {
                                scheduledSlots.forEach { slot ->
                                    val item = slot.word
                                    val isCurrent = item.word.equals(currentWord.word, ignoreCase = true)
                                    val isCustomized = prefs.hasWordConfig(item.word)

                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(if (isCurrent) LexiDarkAccent.copy(alpha = 0.15f) else Color.Transparent)
                                            .clickable {
                                                onSelectWord(item)
                                            }
                                            .padding(vertical = 8.dp, horizontal = 10.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(
                                                    text = item.word.replaceFirstChar { it.uppercase() },
                                                    color = if (isCurrent) LexiDarkAccent else LexiDarkTextPrimary,
                                                    fontSize = 15.sp,
                                                    fontWeight = FontWeight.SemiBold
                                                )
                                                if (slot.isCurrent) {
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    Text(
                                                        text = "Active Now",
                                                        color = LexiDarkAccent,
                                                        fontSize = 10.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        modifier = Modifier
                                                            .clip(RoundedCornerShape(4.dp))
                                                            .background(LexiDarkAccent.copy(alpha = 0.2f))
                                                            .padding(horizontal = 5.dp, vertical = 2.dp)
                                                    )
                                                }
                                            }
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(
                                                text = "${slot.timeRangeText} • ${item.partOfSpeech.lowercase()}",
                                                color = LexiDarkTextSecondary,
                                                fontSize = 12.sp
                                            )
                                        }

                                        if (isCustomized) {
                                            Text(
                                                text = "Customized",
                                                color = LexiDarkAccent,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Medium
                                            )
                                        }
                                    }
                                    LexiDivider(color = LexiDarkDivider)
                                }
                            }
                        }
                    } else {
                        // ALL WORDS LIST WITH SEARCH
                        OutlinedTextField(
                            value = wordSearchQuery,
                            onValueChange = { wordSearchQuery = it },
                            placeholder = { Text("Search vocabulary...", color = LexiDarkTextSecondary) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                            if (filteredWords.isEmpty()) {
                                Text(
                                    text = "No words matching “$wordSearchQuery”",
                                    color = LexiDarkTextSecondary,
                                    fontSize = 13.sp,
                                    modifier = Modifier.padding(16.dp)
                                )
                            } else {
                                filteredWords.forEach { item ->
                                    val isCurrent = item.word.equals(currentWord.word, ignoreCase = true)
                                    val isCustomized = prefs.hasWordConfig(item.word)

                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(if (isCurrent) LexiDarkAccent.copy(alpha = 0.15f) else Color.Transparent)
                                            .clickable {
                                                onSelectWord(item)
                                                wordSearchQuery = ""
                                            }
                                            .padding(vertical = 10.dp, horizontal = 8.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = item.word.replaceFirstChar { it.uppercase() },
                                                color = if (isCurrent) LexiDarkAccent else LexiDarkTextPrimary,
                                                fontSize = 15.sp,
                                                fontWeight = FontWeight.Medium
                                            )
                                            Text(
                                                text = item.definition,
                                                color = LexiDarkTextSecondary,
                                                fontSize = 11.sp,
                                                maxLines = 1
                                            )
                                        }
                                        if (isCustomized) {
                                            Text(text = "Custom", color = LexiDarkAccent, fontSize = 11.sp)
                                        }
                                    }
                                    LexiDivider(color = LexiDarkDivider)
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = onDismissWordSelector) {
                    Text("Close", color = LexiDarkTextSecondary)
                }
            },
            containerColor = LexiDarkSurfaceSubtle
        )
    }

    // 2. APPLY PRESET SCOPE DIALOG
    if (showApplyPresetDialog && pendingApplyPreset != null) {
        val preset = pendingApplyPreset
        AlertDialog(
            onDismissRequest = onDismissApplyPreset,
            title = { Text(text = "Apply Preset: ${preset.name}", color = LexiDarkTextPrimary, fontSize = 17.sp) },
            text = {
                Text(
                    text = "Do you want to apply this preset to “${currentWord.word}” only, or to all words?",
                    color = LexiDarkTextSecondary,
                    fontSize = 14.sp
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val all = repository?.getAllWords() ?: listOf(currentWord)
                        all.forEach { w ->
                            prefs.setWordConfig(w.word, preset.config)
                        }
                        prefs.wallpaperConfig = preset.config
                        LexiLockWallpaperService.requestRedraw()
                        onPresetApplied(preset.config)
                        Toast.makeText(context, "Preset applied to all words", Toast.LENGTH_SHORT).show()
                    }
                ) {
                    Text("All Words", color = LexiDarkAccent, fontWeight = FontWeight.SemiBold)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        prefs.setWordConfig(currentWord.word, preset.config)
                        LexiLockWallpaperService.requestRedraw()
                        onPresetApplied(preset.config)
                        Toast.makeText(context, "Preset applied to “${currentWord.word}”", Toast.LENGTH_SHORT).show()
                    }
                ) {
                    Text("This Word Only", color = LexiDarkTextPrimary)
                }
            },
            containerColor = LexiDarkSurfaceSubtle
        )
    }

    // 3. SAVE PRESET DIALOG
    if (showSaveDialog) {
        AlertDialog(
            onDismissRequest = onDismissSaveDialog,
            title = { Text(text = "Save Preset", color = LexiDarkTextPrimary, fontSize = 17.sp) },
            text = {
                Column {
                    Text(
                        text = "Enter a name to save this layout and style for future words:",
                        color = LexiDarkTextSecondary,
                        fontSize = 13.sp
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = presetNameInput,
                        onValueChange = { presetNameInput = it },
                        placeholder = { Text("e.g. My Warm Editorial", color = LexiDarkTextSecondary) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val name = presetNameInput.trim().ifEmpty { "Preset ${presets.size + 1}" }
                        val newPreset = CustomWallpaperPreset(
                            id = UUID.randomUUID().toString(),
                            name = name,
                            config = config
                        )
                        prefs.savePreset(newPreset)
                        presetNameInput = ""
                        onPresetSaved(prefs.customWallpaperPresets)
                        Toast.makeText(context, "Preset “$name” saved", Toast.LENGTH_SHORT).show()
                    }
                ) {
                    Text("Save", color = LexiDarkAccent, fontWeight = FontWeight.SemiBold)
                }
            },
            dismissButton = {
                TextButton(onClick = onDismissSaveDialog) {
                    Text("Cancel", color = LexiDarkTextSecondary)
                }
            },
            containerColor = LexiDarkSurfaceSubtle
        )
    }

    // 4. RESET THIS WORD CONFIRMATION
    if (showResetWordConfirm) {
        AlertDialog(
            onDismissRequest = onDismissResetWord,
            title = { Text(text = "Reset layout for “${currentWord.word}”?", color = LexiDarkTextPrimary, fontSize = 17.sp) },
            text = {
                Text(
                    text = "This will clear custom fonts, positions, scales, and colors for this word and restore defaults.",
                    color = LexiDarkTextSecondary,
                    fontSize = 14.sp
                )
            },
            confirmButton = {
                TextButton(onClick = onConfirmResetWord) {
                    Text("Reset", color = Color.Red, fontWeight = FontWeight.SemiBold)
                }
            },
            dismissButton = {
                TextButton(onClick = onDismissResetWord) {
                    Text("Cancel", color = LexiDarkTextSecondary)
                }
            },
            containerColor = LexiDarkSurfaceSubtle
        )
    }

    // 5. RESET ALL WORDS CONFIRMATION
    if (showResetAllConfirm) {
        AlertDialog(
            onDismissRequest = onDismissResetAll,
            title = { Text(text = "Reset ALL custom layouts?", color = Color.Red, fontSize = 17.sp) },
            text = {
                Text(
                    text = "This will delete custom layout configurations for ALL vocabulary words across the app. This cannot be undone.",
                    color = LexiDarkTextSecondary,
                    fontSize = 14.sp
                )
            },
            confirmButton = {
                TextButton(onClick = onConfirmResetAll) {
                    Text("Reset All", color = Color.Red, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = onDismissResetAll) {
                    Text("Cancel", color = LexiDarkTextSecondary)
                }
            },
            containerColor = LexiDarkSurfaceSubtle
        )
    }
}

@Composable
private fun TypographyScaleStepper(
    label: String,
    scale: Float,
    steps: List<Pair<String, Float>>,
    onScaleChange: (Float) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = label, color = LexiDarkTextPrimary, fontSize = 13.sp)
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                steps.forEach { (stepName, stepVal) ->
                    val isSelected = Math.abs(scale - stepVal) < 0.06f
                    Text(
                        text = stepName,
                        color = if (isSelected) LexiDarkAccent else LexiDarkTextSecondary,
                        fontSize = 11.sp,
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(if (isSelected) LexiDarkSurfaceSubtle else Color.Transparent)
                            .clickable { onScaleChange(stepVal) }
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun SliderControl(
    label: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float> = 0.7f..1.4f,
    onValueChange: (Float) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(text = label, color = LexiDarkTextPrimary, fontSize = 13.sp)
            Text(text = "${(value * 100).toInt()}%", color = LexiDarkTextSecondary, fontSize = 12.sp)
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            colors = SliderDefaults.colors(
                thumbColor = LexiDarkAccent,
                activeTrackColor = LexiDarkAccent,
                inactiveTrackColor = LexiDarkDivider
            )
        )
    }
}

@Composable
private fun SwitchRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, color = LexiDarkTextPrimary, fontSize = 14.sp)
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = LexiDarkAccent,
                checkedTrackColor = LexiDarkSurfaceSubtle,
                uncheckedThumbColor = LexiDarkTextSecondary,
                uncheckedTrackColor = LexiDarkDivider
            )
        )
    }
}

private fun copyUriToInternalStorage(context: Context, uri: Uri): String? {
    return try {
        val inputStream: InputStream? = context.contentResolver.openInputStream(uri)
        val file = File(context.filesDir, "user_wallpaper_${System.currentTimeMillis()}.jpg")
        val outputStream = FileOutputStream(file)
        inputStream?.copyTo(outputStream)
        inputStream?.close()
        outputStream.close()
        file.absolutePath
    } catch (e: Exception) {
        null
    }
}
