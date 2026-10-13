package com.sarthak.lexilock.data.model

import org.json.JSONObject

enum class WallpaperStyle(val id: String, val title: String) {
    OBSIDIAN("obsidian", "Obsidian"),
    MINIMAL("minimal", "Minimal"),
    AURORA("aurora", "Aurora"),
    PAPER("paper", "Paper"),
    MONOCHROME("monochrome", "Monochrome"),
    PHOTO("photo", "Photo"),
    EDITORIAL("editorial", "Editorial"),
    MUSEUM("museum", "Museum"),
    INK("ink", "Ink"),
    NOIR("noir", "Noir"),
    CUSTOM("custom", "Custom");

    companion object {
        fun fromId(id: String): WallpaperStyle {
            // Glass migration to Obsidian if old ID was stored
            if (id.equals("glass", ignoreCase = true)) return OBSIDIAN
            return values().firstOrNull { it.id.equals(id, ignoreCase = true) } ?: OBSIDIAN
        }
    }
}

enum class VocabPosition(val id: String, val title: String, val defaultY: Float) {
    TOP("top", "Top", 0.18f),
    UPPER_CENTER("upper_center", "Upper Center", 0.28f),
    CENTER("center", "Center", 0.42f),
    LOWER_CENTER("lower_center", "Lower Center", 0.58f),
    BOTTOM("bottom", "Bottom", 0.70f);

    companion object {
        fun fromId(id: String): VocabPosition = values().firstOrNull { it.id.equals(id, ignoreCase = true) } ?: UPPER_CENTER
    }
}

enum class TextAlignment(val id: String, val title: String) {
    LEFT("left", "Left"),
    CENTER("center", "Center"),
    RIGHT("right", "Right");

    companion object {
        fun fromId(id: String): TextAlignment = values().firstOrNull { it.id.equals(id, ignoreCase = true) } ?: CENTER
    }
}

enum class DisplayFont(val id: String, val title: String) {
    EDITORIAL_SERIF("editorial_serif", "Editorial Serif"),
    CLASSIC_SERIF("classic_serif", "Classic Serif"),
    MODERN_SERIF("modern_serif", "Modern Serif"),
    CLEAN_SANS("clean_sans", "Clean Sans"),
    HUMANIST_SANS("humanist_sans", "Humanist Sans"),
    MINIMAL_SANS("minimal_sans", "Minimal Sans"),
    TYPEWRITER("typewriter", "Typewriter");

    companion object {
        fun fromId(id: String): DisplayFont = values().firstOrNull { it.id.equals(id, ignoreCase = true) } ?: EDITORIAL_SERIF
    }
}

enum class BodyFont(val id: String, val title: String) {
    EDITORIAL_SANS("editorial_sans", "Editorial Sans"),
    SYSTEM_SANS("system_sans", "System Sans");

    companion object {
        fun fromId(id: String): BodyFont = values().firstOrNull { it.id.equals(id, ignoreCase = true) } ?: EDITORIAL_SANS
    }
}

enum class PhotoTreatment(val id: String, val title: String) {
    ORIGINAL("original", "Original"),
    DARKEN("darken", "Darken"),
    LIGHTEN("lighten", "Lighten"),
    BLUR("blur", "Blur"),
    STRONG_BLUR("strong_blur", "Strong Blur"),
    MONOCHROME("monochrome", "Monochrome");

    companion object {
        fun fromId(id: String): PhotoTreatment = values().firstOrNull { it.id.equals(id, ignoreCase = true) } ?: DARKEN
    }
}

enum class PhotoOverlay(val id: String, val title: String) {
    NONE("none", "None"),
    DARK_GRADIENT("dark_gradient", "Dark Gradient"),
    LIGHT_GRADIENT("light_gradient", "Light Gradient");

    companion object {
        fun fromId(id: String): PhotoOverlay = values().firstOrNull { it.id.equals(id, ignoreCase = true) } ?: DARK_GRADIENT
    }
}

enum class TextColorPreset(val id: String, val title: String, val hex: String) {
    WARM_WHITE("warm_white", "Warm White", "#FAF7EE"),
    CREAM("cream", "Cream", "#F4EFE6"),
    SOFT_GRAY("soft_gray", "Soft Gray", "#B0B0A8"),
    CHARCOAL("charcoal", "Charcoal", "#2C2C2C"),
    BLACK("black", "Black", "#121212"),
    MUTED_BLUE("muted_blue", "Muted Blue", "#7E96B8"),
    MUTED_RED("muted_red", "Muted Red", "#B87E7E"),
    MUTED_GREEN("muted_green", "Muted Green", "#7EB88E"),
    CUSTOM("custom", "Custom", "#FAF7EE");

    companion object {
        fun fromId(id: String): TextColorPreset {
            if (id.equals("white", ignoreCase = true)) return WARM_WHITE
            return values().firstOrNull { it.id.equals(id, ignoreCase = true) } ?: WARM_WHITE
        }
    }
}

enum class EditorialColorPalette(
    val id: String,
    val title: String,
    val bgHex: String,
    val wordHex: String,
    val secondaryHex: String
) {
    NOIR("noir", "Noir", "#241516", "#F2E5D2", "#BDAF9F"),
    WARM_PAPER("warm_paper", "Warm Paper", "#F3EBDD", "#201A17", "#6E625A"),
    OBSIDIAN("obsidian", "Obsidian", "#101010", "#F5F5F2", "#A5A5A5"),
    FOREST("forest", "Forest", "#17221C", "#E8E3D5", "#B4B8A8"),
    MIDNIGHT("midnight", "Midnight", "#111827", "#E7E9EE", "#AEB5C3"),
    CLAY("clay", "Clay", "#38251F", "#F1DFCB", "#BFA998");

    companion object {
        fun fromId(id: String): EditorialColorPalette? =
            values().firstOrNull { it.id.equals(id, ignoreCase = true) }
    }
}

data class WallpaperConfig(
    val style: WallpaperStyle = WallpaperStyle.OBSIDIAN,
    // Custom/Manual Position (Normalized 0.0 .. 1.0)
    val positionPreset: VocabPosition = VocabPosition.UPPER_CENTER,
    val posX: Float = 0.5f,
    val posY: Float = 0.28f,
    val autoPlacement: Boolean = false,
    
    // Component individual positions (offsets from base position or normalized)
    // If null/zero, inherits from base posX / relative layout
    val wordPosX: Float = 0.5f,
    val wordPosY: Float = 0.28f,
    val ipaPosX: Float = 0.5f,
    val ipaPosY: Float = 0.33f,
    val posPosX: Float = 0.5f,
    val posPosY: Float = 0.37f,
    val defPosX: Float = 0.5f,
    val defPosY: Float = 0.43f,
    val exPosX: Float = 0.5f,
    val exPosY: Float = 0.51f,
    val streakPosX: Float = 0.5f,
    val streakPosY: Float = 0.88f,
    val useComponentPositions: Boolean = false,

    // Sizing (Scaling factor 0.7f .. 1.5f, default 1.0f)
    val wordSizeScale: Float = 1.0f,
    val ipaSizeScale: Float = 1.0f,
    val definitionSizeScale: Float = 1.0f,
    val exampleSizeScale: Float = 1.0f,
    
    // Spacing
    val letterSpacing: Float = 0.0f, // -0.05f .. 0.15f
    val lineSpacingScale: Float = 1.0f, // 0.8f .. 1.6f
    
    // Alignment & Fonts
    val alignment: TextAlignment = TextAlignment.CENTER,
    val displayFont: DisplayFont = DisplayFont.EDITORIAL_SERIF,
    val bodyFont: BodyFont = BodyFont.EDITORIAL_SANS,
    
    // Visibility
    val showWord: Boolean = true,
    val showIpa: Boolean = true,
    val showPartOfSpeech: Boolean = true,
    val showDefinition: Boolean = true,
    val showExample: Boolean = true,
    val showSpeaker: Boolean = true,
    val showStreak: Boolean = true,
    
    // Colors: Global & Per-Component Color Overrides (Hex strings with leading '#')
    val textColorPreset: TextColorPreset = TextColorPreset.WARM_WHITE,
    val customTextColorHex: String = "#FAF7EE",
    val accentColorHex: String = "#7E96B8",
    val customBgColorHex: String = "", // Empty means use style's default background
    val wordColorHex: String = "",     // Empty means use text color
    val ipaColorHex: String = "",      // Empty means use secondary color
    val posColorHex: String = "",      // Empty means use accent/secondary color
    val defColorHex: String = "",      // Empty means use text color
    val exColorHex: String = "",       // Empty means use secondary color
    val streakColorHex: String = "",   // Empty means use secondary color
    
    // Photo Settings
    val photoUri: String = "",
    val photoPath: String = "",
    val photoTreatment: PhotoTreatment = PhotoTreatment.DARKEN,
    val photoOverlay: PhotoOverlay = PhotoOverlay.DARK_GRADIENT,
    val photoZoom: Float = 1.0f,
    val photoPanX: Float = 0.0f,
    val photoPanY: Float = 0.0f
) {
    fun toJson(): String {
        val json = JSONObject()
        json.put("style", style.id)
        json.put("positionPreset", positionPreset.id)
        json.put("posX", posX.toDouble())
        json.put("posY", posY.toDouble())
        json.put("autoPlacement", autoPlacement)

        json.put("wordPosX", wordPosX.toDouble())
        json.put("wordPosY", wordPosY.toDouble())
        json.put("ipaPosX", ipaPosX.toDouble())
        json.put("ipaPosY", ipaPosY.toDouble())
        json.put("posPosX", posPosX.toDouble())
        json.put("posPosY", posPosY.toDouble())
        json.put("defPosX", defPosX.toDouble())
        json.put("defPosY", defPosY.toDouble())
        json.put("exPosX", exPosX.toDouble())
        json.put("exPosY", exPosY.toDouble())
        json.put("streakPosX", streakPosX.toDouble())
        json.put("streakPosY", streakPosY.toDouble())
        json.put("useComponentPositions", useComponentPositions)

        json.put("wordSizeScale", wordSizeScale.toDouble())
        json.put("ipaSizeScale", ipaSizeScale.toDouble())
        json.put("definitionSizeScale", definitionSizeScale.toDouble())
        json.put("exampleSizeScale", exampleSizeScale.toDouble())
        json.put("letterSpacing", letterSpacing.toDouble())
        json.put("lineSpacingScale", lineSpacingScale.toDouble())
        json.put("alignment", alignment.id)
        json.put("displayFont", displayFont.id)
        json.put("bodyFont", bodyFont.id)
        json.put("showWord", showWord)
        json.put("showIpa", showIpa)
        json.put("showPartOfSpeech", showPartOfSpeech)
        json.put("showDefinition", showDefinition)
        json.put("showExample", showExample)
        json.put("showSpeaker", showSpeaker)
        json.put("showStreak", showStreak)
        json.put("textColorPreset", textColorPreset.id)
        json.put("customTextColorHex", customTextColorHex)
        json.put("accentColorHex", accentColorHex)
        json.put("customBgColorHex", customBgColorHex)
        json.put("wordColorHex", wordColorHex)
        json.put("ipaColorHex", ipaColorHex)
        json.put("posColorHex", posColorHex)
        json.put("defColorHex", defColorHex)
        json.put("exColorHex", exColorHex)
        json.put("streakColorHex", streakColorHex)
        json.put("photoUri", photoUri)
        json.put("photoPath", photoPath)
        json.put("photoTreatment", photoTreatment.id)
        json.put("photoOverlay", photoOverlay.id)
        json.put("photoZoom", photoZoom.toDouble())
        json.put("photoPanX", photoPanX.toDouble())
        json.put("photoPanY", photoPanY.toDouble())
        return json.toString()
    }

    companion object {
        fun fromJson(jsonStr: String): WallpaperConfig {
            if (jsonStr.isBlank()) return WallpaperConfig()
            return try {
                val json = JSONObject(jsonStr)
                val basePosX = json.optDouble("posX", 0.5).toFloat()
                val basePosY = json.optDouble("posY", 0.28).toFloat()
                val useComp = json.optBoolean("useComponentPositions", false)

                WallpaperConfig(
                    style = WallpaperStyle.fromId(json.optString("style", "obsidian")),
                    positionPreset = VocabPosition.fromId(json.optString("positionPreset", "upper_center")),
                    posX = basePosX,
                    posY = basePosY,
                    autoPlacement = json.optBoolean("autoPlacement", false),

                    wordPosX = json.optDouble("wordPosX", basePosX.toDouble()).toFloat(),
                    wordPosY = json.optDouble("wordPosY", basePosY.toDouble()).toFloat(),
                    ipaPosX = json.optDouble("ipaPosX", basePosX.toDouble()).toFloat(),
                    ipaPosY = json.optDouble("ipaPosY", (basePosY + 0.05).toDouble()).toFloat(),
                    posPosX = json.optDouble("posPosX", basePosX.toDouble()).toFloat(),
                    posPosY = json.optDouble("posPosY", (basePosY + 0.09).toDouble()).toFloat(),
                    defPosX = json.optDouble("defPosX", basePosX.toDouble()).toFloat(),
                    defPosY = json.optDouble("defPosY", (basePosY + 0.15).toDouble()).toFloat(),
                    exPosX = json.optDouble("exPosX", basePosX.toDouble()).toFloat(),
                    exPosY = json.optDouble("exPosY", (basePosY + 0.23).toDouble()).toFloat(),
                    streakPosX = json.optDouble("streakPosX", basePosX.toDouble()).toFloat(),
                    streakPosY = json.optDouble("streakPosY", 0.88).toFloat(),
                    useComponentPositions = useComp,

                    wordSizeScale = json.optDouble("wordSizeScale", 1.0).toFloat(),
                    ipaSizeScale = json.optDouble("ipaSizeScale", 1.0).toFloat(),
                    definitionSizeScale = json.optDouble("definitionSizeScale", 1.0).toFloat(),
                    exampleSizeScale = json.optDouble("exampleSizeScale", 1.0).toFloat(),
                    letterSpacing = json.optDouble("letterSpacing", 0.0).toFloat(),
                    lineSpacingScale = json.optDouble("lineSpacingScale", 1.0).toFloat(),
                    alignment = TextAlignment.fromId(json.optString("alignment", "center")),
                    displayFont = DisplayFont.fromId(json.optString("displayFont", "editorial_serif")),
                    bodyFont = BodyFont.fromId(json.optString("bodyFont", "editorial_sans")),
                    showWord = json.optBoolean("showWord", true),
                    showIpa = json.optBoolean("showIpa", true),
                    showPartOfSpeech = json.optBoolean("showPartOfSpeech", true),
                    showDefinition = json.optBoolean("showDefinition", true),
                    showExample = json.optBoolean("showExample", true),
                    showSpeaker = json.optBoolean("showSpeaker", true),
                    showStreak = json.optBoolean("showStreak", true),
                    textColorPreset = TextColorPreset.fromId(json.optString("textColorPreset", "warm_white")),
                    customTextColorHex = json.optString("customTextColorHex", "#FAF7EE"),
                    accentColorHex = json.optString("accentColorHex", "#7E96B8"),
                    customBgColorHex = json.optString("customBgColorHex", ""),
                    wordColorHex = json.optString("wordColorHex", ""),
                    ipaColorHex = json.optString("ipaColorHex", ""),
                    posColorHex = json.optString("posColorHex", ""),
                    defColorHex = json.optString("defColorHex", ""),
                    exColorHex = json.optString("exColorHex", ""),
                    streakColorHex = json.optString("streakColorHex", ""),
                    photoUri = json.optString("photoUri", ""),
                    photoPath = json.optString("photoPath", ""),
                    photoTreatment = PhotoTreatment.fromId(json.optString("photoTreatment", "darken")),
                    photoOverlay = PhotoOverlay.fromId(json.optString("photoOverlay", "dark_gradient")),
                    photoZoom = json.optDouble("photoZoom", 1.0).toFloat(),
                    photoPanX = json.optDouble("photoPanX", 0.0).toFloat(),
                    photoPanY = json.optDouble("photoPanY", 0.0).toFloat()
                )
            } catch (e: Exception) {
                WallpaperConfig()
            }
        }
    }
}

data class CustomWallpaperPreset(
    val id: String,
    val name: String,
    val config: WallpaperConfig
) {
    fun toJson(): String {
        val json = JSONObject()
        json.put("id", id)
        json.put("name", name)
        json.put("config", JSONObject(config.toJson()))
        return json.toString()
    }

    companion object {
        fun fromJson(jsonStr: String): CustomWallpaperPreset? {
            return try {
                val json = JSONObject(jsonStr)
                CustomWallpaperPreset(
                    id = json.getString("id"),
                    name = json.getString("name"),
                    config = WallpaperConfig.fromJson(json.getJSONObject("config").toString())
                )
            } catch (e: Exception) {
                null
            }
        }
    }
}
