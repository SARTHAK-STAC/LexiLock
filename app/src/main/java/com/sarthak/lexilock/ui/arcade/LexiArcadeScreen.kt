package com.sarthak.lexilock.ui.arcade

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sarthak.lexilock.data.model.MiniGameType
import com.sarthak.lexilock.data.preferences.LexiPreferences
import com.sarthak.lexilock.data.repository.ArcadeProgressionManager
import com.sarthak.lexilock.data.repository.MiniGameManager
import com.sarthak.lexilock.data.repository.PlayerTitleManager
import com.sarthak.lexilock.data.repository.WordRepository
import com.sarthak.lexilock.ui.arcade.games.*
import com.sarthak.lexilock.ui.components.LexiDivider
import com.sarthak.lexilock.ui.components.LexiProgressBar
import com.sarthak.lexilock.ui.practice.PronunciationChallengeScreen
import com.sarthak.lexilock.ui.theme.*

@Composable
fun LexiArcadeScreen(
    context: Context,
    repository: WordRepository,
    prefs: LexiPreferences,
    onBackClick: () -> Unit
) {
    val gameManager = remember { MiniGameManager.getInstance(context) }
    var activeGame by remember { mutableStateOf<MiniGameType?>(null) }
    var isBattleDialogOpen by remember { mutableStateOf(false) }
    var isPronunciationLabOpen by remember { mutableStateOf(false) }

    val featuredGame = remember { gameManager.getTodayFeaturedGame() }
    val allGames = remember { MiniGameType.values() }

    val gamesPlayed = prefs.arcadeGamesPlayed
    val gameStreak = prefs.arcadeGameStreak
    val overallBest = remember(gamesPlayed) { prefs.getOverallBestScore() }
    val arcadeProgression = remember(prefs.arcadeTotalXp) { ArcadeProgressionManager.getProgression(prefs) }
    val activeTitle = remember { PlayerTitleManager.getActiveTitle(prefs) }

    if (isPronunciationLabOpen) {
        PronunciationChallengeScreen(
            context = context,
            repository = repository,
            prefs = prefs,
            onBackClick = { isPronunciationLabOpen = false }
        )
        return
    }

    if (activeGame != null) {
        when (activeGame!!) {
            MiniGameType.QUICK_MATCH -> QuickMatchGame(context, repository, onBackClick = { activeGame = null })
            MiniGameType.WORD_RUSH -> WordRushGame(context, repository, onBackClick = { activeGame = null })
            MiniGameType.FILL_THE_GAP -> FillTheGapGame(context, repository, onBackClick = { activeGame = null })
            MiniGameType.DEFINITION_DUEL -> DefinitionDuelGame(context, repository, onBackClick = { activeGame = null })
            MiniGameType.ODD_WORD -> OddWordGame(context, repository, onBackClick = { activeGame = null })
            MiniGameType.SPELL_IT -> SpellItGame(context, repository, onBackClick = { activeGame = null })
            MiniGameType.MEMORY_FLIP -> MemoryFlipGame(context, repository, onBackClick = { activeGame = null })
            MiniGameType.WORD_LADDER -> WordLadderGame(context, repository, onBackClick = { activeGame = null })
            MiniGameType.GUESS_THE_WORD -> GuessTheWordGame(repository, prefs, onExitGame = { activeGame = null })
        }
        return
    }

    if (isBattleDialogOpen) {
        FriendBattleDialog(
            context = context,
            prefs = prefs,
            repository = repository,
            onDismissRequest = { isBattleDialogOpen = false }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(LexiDarkBg)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 20.dp)
    ) {
        // TOP HEADER
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "LEXI ARCADE",
                    color = LexiDarkAccent,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 2.sp
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Play a minute. Learn a word.",
                    color = LexiDarkTextSecondary,
                    fontSize = 13.sp
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = "🔥", fontSize = 14.sp)
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "$gameStreak",
                    color = LexiDarkTextPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // ARCADE PROGRESSION BAR & TITLE
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(LexiDarkSurfaceSubtle)
                .padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "ARCADE LVL ${arcadeProgression.arcadeLevel}",
                        color = LexiDarkAccent,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "• ${arcadeProgression.title}",
                        color = LexiDarkTextPrimary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Text(
                    text = "${arcadeProgression.currentLevelXp} / ${arcadeProgression.xpForNextLevel} XP",
                    color = LexiDarkTextSecondary,
                    fontSize = 11.sp
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            LexiProgressBar(progress = arcadeProgression.progressFraction, color = LexiDarkAccent)
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Title: $activeTitle",
                    color = LexiDarkTextSecondary,
                    fontSize = 10.sp
                )
                Text(
                    text = "Reward: ${arcadeProgression.nextRewardHint}",
                    color = LexiDarkAccent.copy(alpha = 0.8f),
                    fontSize = 10.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // SPECIAL RETENTION MODES: FRIEND WORD BATTLE & PRONUNCIATION LAB
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Friend Battle Card
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(10.dp))
                    .background(LexiDarkSurfaceSubtle)
                    .clickable { isBattleDialogOpen = true }
                    .padding(14.dp)
            ) {
                Column {
                    Text(text = "⚔️", fontSize = 18.sp)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Word Battle",
                        color = LexiDarkTextPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Challenge a friend",
                        color = LexiDarkTextSecondary,
                        fontSize = 11.sp
                    )
                }
            }

            // Pronunciation Lab Card
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(10.dp))
                    .background(LexiDarkSurfaceSubtle)
                    .clickable { isPronunciationLabOpen = true }
                    .padding(14.dp)
            ) {
                Column {
                    Text(text = "🎙️", fontSize = 18.sp)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Pronunciation",
                        color = LexiDarkTextPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Voice speech lab",
                        color = LexiDarkTextSecondary,
                        fontSize = 11.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(22.dp))

        // TODAY'S FEATURED GAME CARD
        Text(
            text = "TODAY'S GAME",
            color = LexiDarkTextSecondary,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 1.sp
        )

        Spacer(modifier = Modifier.height(8.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(LexiDarkSurfaceSubtle)
                .padding(20.dp)
        ) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(text = featuredGame.iconEmoji, fontSize = 20.sp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = featuredGame.title,
                                color = LexiDarkTextPrimary,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Normal,
                                fontFamily = FontFamily.Serif
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = featuredGame.subtitle,
                            color = LexiDarkTextSecondary,
                            fontSize = 13.sp
                        )
                    }

                    val featuredBest = prefs.getArcadeBestScore(featuredGame.id)
                    if (featuredBest > 0) {
                        Text(
                            text = "Best: $featuredBest",
                            color = LexiDarkAccent,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${featuredGame.estimatedTime} • +${featuredGame.xpBaseReward} XP",
                        color = LexiDarkTextSecondary,
                        fontSize = 12.sp
                    )

                    Button(
                        onClick = { activeGame = featuredGame },
                        colors = ButtonDefaults.buttonColors(containerColor = LexiDarkAccent),
                        shape = RoundedCornerShape(6.dp),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                        modifier = Modifier.height(34.dp)
                    ) {
                        Text(text = "Play Now", color = LexiDarkBg, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(28.dp))

        // ALL GAMES COLLECTION
        Text(
            text = "ALL MINI-GAMES (${allGames.size})",
            color = LexiDarkTextSecondary,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 1.sp
        )

        Spacer(modifier = Modifier.height(10.dp))

        allGames.forEach { game ->
            val bestScore = prefs.getArcadeBestScore(game.id)

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(LexiDarkSurfaceSubtle)
                    .clickable { activeGame = game }
                    .padding(14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Text(text = game.iconEmoji, fontSize = 20.sp)
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = game.title,
                            color = LexiDarkTextPrimary,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "${game.subtitle}  •  ${game.estimatedTime}",
                            color = LexiDarkTextSecondary,
                            fontSize = 12.sp
                        )
                    }
                }

                Column(horizontalAlignment = Alignment.End) {
                    if (bestScore > 0) {
                        Text(
                            text = "Best: $bestScore",
                            color = LexiDarkAccent,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    } else {
                        Text(
                            text = "Play →",
                            color = LexiDarkTextSecondary,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
        LexiDivider(color = LexiDarkDivider)
        Spacer(modifier = Modifier.height(16.dp))

        // ARCADE PERSONAL BEST STATS FOOTER
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(text = "Games Played", color = LexiDarkTextSecondary, fontSize = 12.sp)
                Spacer(modifier = Modifier.height(2.dp))
                Text(text = "$gamesPlayed", color = LexiDarkTextPrimary, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            }

            Column {
                Text(text = "Game Streak", color = LexiDarkTextSecondary, fontSize = 12.sp)
                Spacer(modifier = Modifier.height(2.dp))
                Text(text = "$gameStreak days", color = LexiDarkTextPrimary, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            }

            Column {
                Text(text = "Best Score", color = LexiDarkTextSecondary, fontSize = 12.sp)
                Spacer(modifier = Modifier.height(2.dp))
                Text(text = "$overallBest", color = LexiDarkAccent, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            }
        }

        Spacer(modifier = Modifier.height(20.dp))
    }
}
