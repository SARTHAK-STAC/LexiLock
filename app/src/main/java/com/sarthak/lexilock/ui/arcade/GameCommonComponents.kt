package com.sarthak.lexilock.ui.arcade

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import com.sarthak.lexilock.data.model.GameResultData
import com.sarthak.lexilock.ui.components.LexiDivider
import com.sarthak.lexilock.ui.theme.*

@Composable
fun GameHeader(
    title: String,
    score: Int,
    combo: Int = 0,
    remainingSeconds: Int? = null,
    onExitClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .clickable { onExitClick() },
                contentAlignment = Alignment.Center
            ) {
                Text(text = "✕", color = LexiDarkTextSecondary, fontSize = 18.sp)
            }
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(
                    text = title,
                    color = LexiDarkTextPrimary,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.SemiBold
                )
                if (combo > 1) {
                    Text(
                        text = "COMBO ×$combo",
                        color = LexiDarkAccent,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                }
            }
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            if (remainingSeconds != null) {
                Text(
                    text = "${remainingSeconds}s",
                    color = if (remainingSeconds <= 10) Color(0xFFE57373) else LexiDarkTextSecondary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(LexiDarkSurfaceSubtle)
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
            }

            Text(
                text = "$score pts",
                color = LexiDarkAccent,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(LexiDarkSurfaceSubtle)
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            )
        }
    }
}

@Composable
fun GameResultScreen(
    result: GameResultData,
    onPlayAgain: () -> Unit,
    onBackToArcade: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(LexiDarkBg)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "GAME COMPLETE",
            color = LexiDarkTextSecondary,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 2.sp
        )

        Spacer(modifier = Modifier.height(10.dp))

        Text(
            text = result.gameType.title,
            color = LexiDarkTextPrimary,
            fontSize = 32.sp,
            fontWeight = FontWeight.Normal,
            fontFamily = FontFamily.Serif
        )

        Spacer(modifier = Modifier.height(28.dp))

        // STATS CARD
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(LexiDarkSurfaceSubtle)
                .padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(text = "CORRECT", color = LexiDarkTextSecondary, fontSize = 11.sp, letterSpacing = 1.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(text = "${result.correctCount}", color = LexiDarkTextPrimary, fontSize = 24.sp, fontWeight = FontWeight.SemiBold)
                }

                Column {
                    Text(text = "ACCURACY", color = LexiDarkTextSecondary, fontSize = 11.sp, letterSpacing = 1.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(text = "${result.accuracyPercent}%", color = LexiDarkTextPrimary, fontSize = 24.sp, fontWeight = FontWeight.SemiBold)
                }

                Column {
                    Text(text = "XP EARNED", color = LexiDarkTextSecondary, fontSize = 11.sp, letterSpacing = 1.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(text = "+${result.xpEarned}", color = LexiDarkAccent, fontSize = 24.sp, fontWeight = FontWeight.SemiBold)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            LexiDivider(color = LexiDarkDivider)
            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (result.isNewPersonalBest) "New personal best! ✓" else "Personal best: ${result.bestScore}",
                    color = if (result.isNewPersonalBest) LexiDarkAccent else LexiDarkTextSecondary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )

                if (result.maxCombo > 1) {
                    Text(
                        text = "Max combo: ×${result.maxCombo}",
                        color = LexiDarkTextSecondary,
                        fontSize = 13.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(28.dp))

        val localContext = androidx.compose.ui.platform.LocalContext.current

        // ACTION BUTTONS
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Button(
                onClick = onPlayAgain,
                colors = ButtonDefaults.buttonColors(containerColor = LexiDarkAccent),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
            ) {
                Text(text = "Play Again", color = LexiDarkBg, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            }

            Button(
                onClick = {
                    com.sarthak.lexilock.utils.ShareCardGenerator.shareGameResult(
                        context = localContext,
                        gameTitle = result.gameType.title,
                        score = result.correctCount,
                        accuracyPercent = result.accuracyPercent,
                        timeSeconds = null,
                        xpEarned = result.xpEarned,
                        isPersonalBest = result.isNewPersonalBest
                    )
                },
                colors = ButtonDefaults.buttonColors(containerColor = LexiDarkSurfaceSubtle),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
            ) {
                Text(text = "Share Result ↗", color = LexiDarkTextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Medium)
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Button(
            onClick = onBackToArcade,
            colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp)
        ) {
            Text(text = "Back to Arcade", color = LexiDarkTextSecondary, fontSize = 14.sp)
        }
    }
}
