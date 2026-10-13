package com.sarthak.lexilock.ui.word

import android.content.Context
import android.speech.tts.TextToSpeech
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sarthak.lexilock.data.repository.WordRepository
import com.sarthak.lexilock.domain.model.WordItem
import com.sarthak.lexilock.ui.components.*
import com.sarthak.lexilock.ui.theme.*
import java.util.Locale

@Composable
fun WordDetailsScreen(
    context: Context,
    wordItem: WordItem,
    repository: WordRepository,
    onBackClick: () -> Unit
) {
    var word by remember { mutableStateOf(wordItem) }
    var isSaved by remember { mutableStateOf(word.isSaved) }
    var isMastered by remember { mutableStateOf(word.isMastered) }
    var ttsInstance by remember { mutableStateOf<TextToSpeech?>(null) }

    DisposableEffect(Unit) {
        val tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                // Initialized
            }
        }
        tts.language = Locale.US
        ttsInstance = tts

        onDispose {
            tts.stop()
            tts.shutdown()
        }
    }

    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(LexiDarkBg)
            .verticalScroll(scrollState)
            .padding(horizontal = 24.dp, vertical = 20.dp)
    ) {
        // TOP NAVIGATION: Clean Back arrow
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .clickable { onBackClick() },
                contentAlignment = Alignment.Center
            ) {
                LexiIcons.Back(modifier = Modifier.size(20.dp), tint = LexiDarkTextPrimary)
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // WORD HERO TITLE (Editorial Serif)
        Text(
            text = word.word.replaceFirstChar { it.uppercase() },
            color = LexiDarkTextPrimary,
            fontSize = 38.sp,
            fontWeight = FontWeight.Normal,
            fontFamily = FontFamily.Serif,
            lineHeight = 44.sp,
            letterSpacing = (-0.5).sp
        )

        Spacer(modifier = Modifier.height(4.dp))

        // Part of Speech & Phonetic
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = word.partOfSpeech.lowercase(),
                color = LexiDarkTextSecondary,
                fontSize = 15.sp,
                fontStyle = FontStyle.Italic
            )
            if (word.pronunciation.isNotBlank()) {
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = word.pronunciation,
                    color = LexiDarkTextSecondary.copy(alpha = 0.7f),
                    fontSize = 14.sp
                )
            }
            if (word.difficulty.isNotBlank()) {
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "·  ${word.difficulty.replaceFirstChar { it.uppercase() }}",
                    color = LexiDarkTextSecondary.copy(alpha = 0.7f),
                    fontSize = 13.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(28.dp))
        LexiDivider(color = LexiDarkDivider)
        Spacer(modifier = Modifier.height(24.dp))

        // SECTION: DEFINITION
        Text(
            text = "Definition",
            color = LexiDarkTextSecondary,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = word.definition,
            color = LexiDarkTextPrimary,
            fontSize = 16.sp,
            lineHeight = 24.sp
        )

        Spacer(modifier = Modifier.height(28.dp))

        // SECTION: EXAMPLE
        Text(
            text = "Example",
            color = LexiDarkTextSecondary,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "“${word.exampleSentence}”",
            color = LexiDarkTextPrimary,
            fontSize = 15.sp,
            fontStyle = FontStyle.Italic,
            lineHeight = 23.sp
        )

        // SECTION: SYNONYMS
        if (word.synonyms.isNotEmpty()) {
            Spacer(modifier = Modifier.height(28.dp))
            Text(
                text = "Synonyms",
                color = LexiDarkTextSecondary,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = word.synonyms.joinToString("  ·  "),
                color = LexiDarkTextPrimary,
                fontSize = 15.sp,
                lineHeight = 22.sp
            )
        }

        // SECTION: ANTONYMS
        if (word.antonyms.isNotEmpty()) {
            Spacer(modifier = Modifier.height(28.dp))
            Text(
                text = "Antonyms",
                color = LexiDarkTextSecondary,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = word.antonyms.joinToString("  ·  "),
                color = LexiDarkTextPrimary,
                fontSize = 15.sp,
                lineHeight = 22.sp
            )
        }

        Spacer(modifier = Modifier.height(36.dp))
        LexiDivider(color = LexiDarkDivider)
        Spacer(modifier = Modifier.height(28.dp))

        // ACTIONS: Pronounce, Save, Master (Text and simple outlines)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Pronounce
            OutlinedButton(
                onClick = {
                    ttsInstance?.speak(word.word, TextToSpeech.QUEUE_FLUSH, null, "tts_details")
                },
                shape = RoundedCornerShape(8.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, LexiDarkDivider),
                colors = ButtonDefaults.outlinedButtonColors(containerColor = Color.Transparent),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
            ) {
                LexiIcons.Sound(modifier = Modifier.size(15.dp), tint = LexiDarkTextPrimary)
                Spacer(modifier = Modifier.width(6.dp))
                Text("Pronounce", color = LexiDarkTextPrimary, fontSize = 13.sp)
            }

            // Save
            OutlinedButton(
                onClick = {
                    val newSave = !isSaved
                    isSaved = newSave
                    repository.toggleSave(word.id, newSave)
                    word = word.copy(isSaved = newSave)
                },
                shape = RoundedCornerShape(8.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, LexiDarkDivider),
                colors = ButtonDefaults.outlinedButtonColors(containerColor = Color.Transparent),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
            ) {
                LexiIcons.Heart(
                    modifier = Modifier.size(15.dp),
                    tint = if (isSaved) LexiDarkTextPrimary else LexiDarkTextSecondary,
                    filled = isSaved
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (isSaved) "Saved" else "Save",
                    color = LexiDarkTextPrimary,
                    fontSize = 13.sp
                )
            }

            // Master
            Button(
                onClick = {
                    val newMaster = !isMastered
                    isMastered = newMaster
                    repository.markAsMastered(word.id, newMaster)
                    word = word.copy(isMastered = newMaster)
                },
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isMastered) LexiDarkTextPrimary else LexiDarkSurfaceSubtle,
                    contentColor = if (isMastered) LexiDarkBg else LexiDarkTextPrimary
                ),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
            ) {
                LexiIcons.Check(
                    modifier = Modifier.size(15.dp),
                    tint = if (isMastered) LexiDarkBg else LexiDarkTextPrimary
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (isMastered) "Mastered" else "Master",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        Spacer(modifier = Modifier.height(28.dp))
    }
}
