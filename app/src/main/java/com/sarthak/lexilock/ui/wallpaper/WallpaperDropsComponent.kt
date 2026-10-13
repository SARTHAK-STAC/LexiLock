package com.sarthak.lexilock.ui.wallpaper

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sarthak.lexilock.data.model.*
import com.sarthak.lexilock.domain.model.WordItem
import com.sarthak.lexilock.ui.components.LexiDivider
import com.sarthak.lexilock.ui.theme.*

data class WallpaperDropItem(
    val id: String,
    val name: String,
    val tag: String,
    val description: String,
    val style: WallpaperStyle,
    val bgHex: String,
    val wordHex: String,
    val accentHex: String,
    val font: DisplayFont,
    val alignment: TextAlignment
)

val WALLPAPER_DROPS = listOf(
    WallpaperDropItem(
        id = "drop_noir",
        name = "NOIR",
        tag = "Studio Curator Edition",
        description = "Pure matte black with warm cream typography and subtle cinema margins.",
        style = WallpaperStyle.NOIR,
        bgHex = "#0C0D0E",
        wordHex = "#F5EBE1",
        accentHex = "#A39788",
        font = DisplayFont.EDITORIAL_SERIF,
        alignment = TextAlignment.CENTER
    ),
    WallpaperDropItem(
        id = "drop_museum",
        name = "MUSEUM",
        tag = "Gallery Collection",
        description = "Deep architectural charcoal with champagne accents and classical serif hierarchy.",
        style = WallpaperStyle.OBSIDIAN,
        bgHex = "#16171A",
        wordHex = "#EFECE6",
        accentHex = "#C4B59D",
        font = DisplayFont.EDITORIAL_SERIF,
        alignment = TextAlignment.CENTER
    ),
    WallpaperDropItem(
        id = "drop_editorial",
        name = "EDITORIAL",
        tag = "Daily Press Edition",
        description = "Crisp newsprint contrast with refined headline balance and structured leading.",
        style = WallpaperStyle.MINIMAL,
        bgHex = "#121316",
        wordHex = "#F8F8F6",
        accentHex = "#7A8B9E",
        font = DisplayFont.MINIMAL_SANS,
        alignment = TextAlignment.LEFT
    ),
    WallpaperDropItem(
        id = "drop_ink",
        name = "INK",
        tag = "High Contrast",
        description = "Stark monochrome precision with pitch black canvas and pure white typography.",
        style = WallpaperStyle.MONOCHROME,
        bgHex = "#000000",
        wordHex = "#FFFFFF",
        accentHex = "#888888",
        font = DisplayFont.EDITORIAL_SERIF,
        alignment = TextAlignment.CENTER
    ),
    WallpaperDropItem(
        id = "drop_paper",
        name = "PAPER",
        tag = "Warm Tactile",
        description = "Subtle tactile parchment warmth paired with deep organic espresso lettering.",
        style = WallpaperStyle.PAPER,
        bgHex = "#F5F1E8",
        wordHex = "#1F1A17",
        accentHex = "#70665B",
        font = DisplayFont.EDITORIAL_SERIF,
        alignment = TextAlignment.CENTER
    ),
    WallpaperDropItem(
        id = "drop_obsidian",
        name = "OBSIDIAN",
        tag = "Cinematic Night",
        description = "Deep slate blue darkness with cool steel blue rhetoric accents.",
        style = WallpaperStyle.OBSIDIAN,
        bgHex = "#0E1217",
        wordHex = "#E6EDF3",
        accentHex = "#58A6FF",
        font = DisplayFont.EDITORIAL_SERIF,
        alignment = TextAlignment.CENTER
    ),
    WallpaperDropItem(
        id = "drop_seasonal",
        name = "SEASONAL",
        tag = "Autumn Amber",
        description = "Muted espresso tones elevated by amber ember highlights and warm glow.",
        style = WallpaperStyle.AURORA,
        bgHex = "#1A1512",
        wordHex = "#FCEBD9",
        accentHex = "#E08342",
        font = DisplayFont.EDITORIAL_SERIF,
        alignment = TextAlignment.CENTER
    )
)

@Composable
fun TabDropsContent(
    currentConfig: WallpaperConfig,
    onApplyDrop: (WallpaperDropItem) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp)
    ) {
        Text(
            text = "CURATED WALLPAPER DROPS",
            color = LexiDarkTextSecondary,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp
        )
        Text(
            text = "Handcrafted editorial themes designed for distraction-free lock screens.",
            color = LexiDarkTextSecondary.copy(alpha = 0.7f),
            fontSize = 13.sp,
            modifier = Modifier.padding(top = 4.dp, bottom = 16.dp)
        )

        WALLPAPER_DROPS.forEach { drop ->
            val isActive = currentConfig.style == drop.style &&
                    (currentConfig.customBgColorHex == drop.bgHex || currentConfig.customBgColorHex.isBlank())

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(LexiDarkSurfaceSubtle)
                    .border(
                        1.dp,
                        if (isActive) LexiDarkAccent else LexiDarkDivider,
                        RoundedCornerShape(12.dp)
                    )
                    .clickable { onApplyDrop(drop) }
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = drop.name,
                                color = LexiDarkTextPrimary,
                                fontSize = 18.sp,
                                fontFamily = FontFamily.Serif,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(LexiDarkDivider)
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = drop.tag,
                                    color = LexiDarkAccent,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = drop.description,
                            color = LexiDarkTextSecondary,
                            fontSize = 13.sp
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    // Color Swatch preview circles
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(18.dp)
                                .clip(CircleShape)
                                .background(Color(android.graphics.Color.parseColor(drop.bgHex)))
                                .border(0.5.dp, Color.White.copy(alpha = 0.2f), CircleShape)
                        )
                        Box(
                            modifier = Modifier
                                .size(18.dp)
                                .clip(CircleShape)
                                .background(Color(android.graphics.Color.parseColor(drop.wordHex)))
                        )
                        Box(
                            modifier = Modifier
                                .size(18.dp)
                                .clip(CircleShape)
                                .background(Color(android.graphics.Color.parseColor(drop.accentHex)))
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    Text(
                        text = if (isActive) "Active Drop ✓" else "Apply Drop →",
                        color = if (isActive) LexiDarkAccent else LexiDarkTextPrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}
