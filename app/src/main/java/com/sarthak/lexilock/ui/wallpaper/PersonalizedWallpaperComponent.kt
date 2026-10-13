package com.sarthak.lexilock.ui.wallpaper

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
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
import com.sarthak.lexilock.ui.theme.*

@Composable
fun TabMakeMyWallpaperContent(
    config: WallpaperConfig,
    currentWord: WordItem,
    onConfigChange: (WallpaperConfig) -> Unit,
    onOpenWordSelector: () -> Unit
) {
    val bgColors = listOf("#0E1116", "#141518", "#1E1A17", "#000000", "#181A22", "#F5F2EB", "#1B221B")
    val wordColors = listOf("#FAF7EE", "#FFFFFF", "#F5EBE1", "#E6EDF3", "#FCEBD9", "#1E1A17")
    val accentColors = listOf("#7E96B8", "#A39788", "#58A6FF", "#E08342", "#87A987", "#BDAF9F")

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp)
    ) {
        Text(
            text = "MAKE MY WALLPAPER",
            color = LexiDarkTextSecondary,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp
        )
        Text(
            text = "Fine-tune every visual dimension into your bespoke daily wallpaper.",
            color = LexiDarkTextSecondary.copy(alpha = 0.7f),
            fontSize = 13.sp,
            modifier = Modifier.padding(top = 4.dp, bottom = 16.dp)
        )

        // 1. Featured Word Selector
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(LexiDarkSurfaceSubtle)
                .clickable { onOpenWordSelector() }
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "FEATURED WORD",
                    color = LexiDarkTextSecondary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = currentWord.word,
                    color = LexiDarkTextPrimary,
                    fontSize = 17.sp,
                    fontFamily = FontFamily.Serif,
                    fontWeight = FontWeight.Medium
                )
            }
            Text(
                text = "Change →",
                color = LexiDarkAccent,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium
            )
        }

        Spacer(modifier = Modifier.height(18.dp))

        // 2. Background Color
        Text(
            text = "CANVAS BACKGROUND",
            color = LexiDarkTextSecondary,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp
        )
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            bgColors.forEach { hex ->
                val isSelected = config.customBgColorHex == hex
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Color(android.graphics.Color.parseColor(hex)))
                        .border(
                            if (isSelected) 2.5.dp else 1.dp,
                            if (isSelected) LexiDarkAccent else Color.White.copy(alpha = 0.2f),
                            CircleShape
                        )
                        .clickable {
                            onConfigChange(config.copy(customBgColorHex = hex))
                        }
                )
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // 3. Word Typography Color
        Text(
            text = "HERO WORD COLOR",
            color = LexiDarkTextSecondary,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp
        )
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            wordColors.forEach { hex ->
                val isSelected = config.wordColorHex == hex
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Color(android.graphics.Color.parseColor(hex)))
                        .border(
                            if (isSelected) 2.5.dp else 1.dp,
                            if (isSelected) LexiDarkAccent else Color.White.copy(alpha = 0.2f),
                            CircleShape
                        )
                        .clickable {
                            onConfigChange(config.copy(wordColorHex = hex))
                        }
                )
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // 4. Accent & Secondary Color
        Text(
            text = "SECONDARY ACCENT COLOR",
            color = LexiDarkTextSecondary,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp
        )
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            accentColors.forEach { hex ->
                val isSelected = config.accentColorHex == hex
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Color(android.graphics.Color.parseColor(hex)))
                        .border(
                            if (isSelected) 2.5.dp else 1.dp,
                            if (isSelected) LexiDarkAccent else Color.White.copy(alpha = 0.2f),
                            CircleShape
                        )
                        .clickable {
                            onConfigChange(config.copy(accentColorHex = hex, posColorHex = hex))
                        }
                )
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // 5. Typography Font Choice
        Text(
            text = "TYPOGRAPHY",
            color = LexiDarkTextSecondary,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp
        )
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf(
                DisplayFont.EDITORIAL_SERIF to "Editorial Serif",
                DisplayFont.MINIMAL_SANS to "Minimal Sans",
                DisplayFont.TYPEWRITER to "Monospace"
            ).forEach { (font, label) ->
                val isSelected = config.displayFont == font
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isSelected) LexiDarkSurfaceSubtle else Color.Transparent)
                        .border(
                            1.dp,
                            if (isSelected) LexiDarkAccent else LexiDarkDivider,
                            RoundedCornerShape(8.dp)
                        )
                        .clickable {
                            onConfigChange(config.copy(displayFont = font))
                        }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = label,
                        color = if (isSelected) LexiDarkTextPrimary else LexiDarkTextSecondary,
                        fontSize = 12.sp,
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // 6. Layout Alignment
        Text(
            text = "LAYOUT ALIGNMENT",
            color = LexiDarkTextSecondary,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp
        )
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf(
                TextAlignment.CENTER to "Center Aligned",
                TextAlignment.LEFT to "Left Editorial"
            ).forEach { (align, label) ->
                val isSelected = config.alignment == align
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isSelected) LexiDarkSurfaceSubtle else Color.Transparent)
                        .border(
                            1.dp,
                            if (isSelected) LexiDarkAccent else LexiDarkDivider,
                            RoundedCornerShape(8.dp)
                        )
                        .clickable {
                            onConfigChange(config.copy(alignment = align))
                        }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = label,
                        color = if (isSelected) LexiDarkTextPrimary else LexiDarkTextSecondary,
                        fontSize = 12.sp,
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                    )
                }
            }
        }
    }
}
