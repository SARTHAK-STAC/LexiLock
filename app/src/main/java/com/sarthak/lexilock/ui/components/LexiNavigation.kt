package com.sarthak.lexilock.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sarthak.lexilock.ui.theme.*

// ==========================================
// LEXILOCK STANDARD MATERIAL NAVIGATION & ICONS
// Clean, Crisp Vector Rendering, No Holographic Styles
// ==========================================

object LexiIcons {

    @Composable
    fun Home(modifier: Modifier = Modifier, tint: Color = LexiDarkTextSecondary) {
        Canvas(modifier = modifier.size(20.dp)) {
            val w = size.width
            val h = size.height
            val path = Path().apply {
                moveTo(w * 0.5f, h * 0.16f)
                lineTo(w * 0.85f, h * 0.46f)
                lineTo(w * 0.74f, h * 0.46f)
                lineTo(w * 0.74f, h * 0.84f)
                lineTo(w * 0.26f, h * 0.84f)
                lineTo(w * 0.26f, h * 0.46f)
                lineTo(w * 0.15f, h * 0.46f)
                close()
            }
            drawPath(path = path, color = tint, style = Stroke(width = 2.4f))
        }
    }

    @Composable
    fun Compass(modifier: Modifier = Modifier, tint: Color = LexiDarkTextSecondary) {
        Canvas(modifier = modifier.size(20.dp)) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val radius = size.width * 0.38f
            drawCircle(color = tint, radius = radius, center = center, style = Stroke(width = 2.2f))

            val needle = Path().apply {
                moveTo(center.x + radius * 0.44f, center.y - radius * 0.44f)
                lineTo(center.x + radius * 0.1f, center.y + radius * 0.1f)
                lineTo(center.x - radius * 0.44f, center.y + radius * 0.44f)
                lineTo(center.x - radius * 0.1f, center.y - radius * 0.1f)
                close()
            }
            drawPath(needle, color = tint)
        }
    }

    @Composable
    fun Bookmark(modifier: Modifier = Modifier, tint: Color = LexiDarkTextSecondary, filled: Boolean = false) {
        Canvas(modifier = modifier.size(20.dp)) {
            val w = size.width
            val h = size.height
            val path = Path().apply {
                moveTo(w * 0.28f, h * 0.16f)
                lineTo(w * 0.72f, h * 0.16f)
                lineTo(w * 0.72f, h * 0.84f)
                lineTo(w * 0.50f, h * 0.68f)
                lineTo(w * 0.28f, h * 0.84f)
                close()
            }
            if (filled) {
                drawPath(path, color = tint)
            } else {
                drawPath(path, color = tint, style = Stroke(width = 2.2f))
            }
        }
    }

    @Composable
    fun Gamepad(modifier: Modifier = Modifier, tint: Color = LexiDarkTextSecondary) {
        Canvas(modifier = modifier.size(20.dp)) {
            val w = size.width
            val h = size.height
            // Clean gamepad D-pad and action circles
            drawRoundRect(
                color = tint,
                topLeft = Offset(w * 0.16f, h * 0.28f),
                size = Size(w * 0.68f, h * 0.44f),
                cornerRadius = CornerRadius(6f, 6f),
                style = Stroke(width = 2.2f)
            )
            // Left dpad plus
            drawLine(tint, Offset(w * 0.30f, h * 0.42f), Offset(w * 0.40f, h * 0.42f), strokeWidth = 2f)
            drawLine(tint, Offset(w * 0.35f, h * 0.37f), Offset(w * 0.35f, h * 0.47f), strokeWidth = 2f)
            // Right action dots
            drawCircle(tint, radius = 2.2f, center = Offset(w * 0.64f, h * 0.42f))
            drawCircle(tint, radius = 2.2f, center = Offset(w * 0.72f, h * 0.50f))
        }
    }

    @Composable
    fun Chart(modifier: Modifier = Modifier, tint: Color = LexiDarkTextSecondary) {
        Canvas(modifier = modifier.size(20.dp)) {
            val w = size.width
            val h = size.height
            drawRoundRect(
                color = tint,
                topLeft = Offset(w * 0.20f, h * 0.52f),
                size = Size(w * 0.14f, h * 0.34f),
                cornerRadius = CornerRadius(2f, 2f)
            )
            drawRoundRect(
                color = tint,
                topLeft = Offset(w * 0.43f, h * 0.26f),
                size = Size(w * 0.14f, h * 0.60f),
                cornerRadius = CornerRadius(2f, 2f)
            )
            drawRoundRect(
                color = tint,
                topLeft = Offset(w * 0.66f, h * 0.40f),
                size = Size(w * 0.14f, h * 0.46f),
                cornerRadius = CornerRadius(2f, 2f)
            )
        }
    }

    @Composable
    fun Gear(modifier: Modifier = Modifier, tint: Color = LexiDarkTextSecondary) {
        Canvas(modifier = modifier.size(20.dp)) {
            val center = Offset(size.width / 2f, size.height / 2f)
            drawCircle(color = tint, radius = size.width * 0.36f, center = center, style = Stroke(width = 2.2f))
            drawCircle(color = tint, radius = size.width * 0.14f, center = center)
        }
    }

    @Composable
    fun Sound(modifier: Modifier = Modifier, tint: Color = LexiDarkTextPrimary) {
        Canvas(modifier = modifier.size(18.dp)) {
            val w = size.width
            val h = size.height
            val speaker = Path().apply {
                moveTo(w * 0.18f, h * 0.38f)
                lineTo(w * 0.34f, h * 0.38f)
                lineTo(w * 0.54f, h * 0.22f)
                lineTo(w * 0.54f, h * 0.78f)
                lineTo(w * 0.34f, h * 0.62f)
                lineTo(w * 0.18f, h * 0.62f)
                close()
            }
            drawPath(speaker, color = tint)
            drawArc(
                color = tint,
                startAngle = -40f,
                sweepAngle = 80f,
                useCenter = false,
                topLeft = Offset(w * 0.50f, h * 0.30f),
                size = Size(w * 0.30f, h * 0.40f),
                style = Stroke(width = 2.2f)
            )
        }
    }

    @Composable
    fun Heart(modifier: Modifier = Modifier, tint: Color = LexiDarkTextSecondary, filled: Boolean = false) {
        Canvas(modifier = modifier.size(20.dp)) {
            val w = size.width
            val h = size.height
            val path = Path().apply {
                moveTo(w * 0.5f, h * 0.80f)
                cubicTo(w * 0.16f, h * 0.56f, w * 0.08f, h * 0.32f, w * 0.26f, h * 0.20f)
                cubicTo(w * 0.40f, h * 0.14f, w * 0.48f, h * 0.26f, w * 0.50f, h * 0.32f)
                cubicTo(w * 0.52f, h * 0.26f, w * 0.60f, h * 0.14f, w * 0.74f, h * 0.20f)
                cubicTo(w * 0.92f, h * 0.32f, w * 0.84f, h * 0.56f, w * 0.5f, h * 0.80f)
                close()
            }
            if (filled) {
                drawPath(path, color = tint)
            } else {
                drawPath(path, color = tint, style = Stroke(width = 2.2f))
            }
        }
    }

    @Composable
    fun Check(modifier: Modifier = Modifier, tint: Color = LexiDarkTextPrimary) {
        Canvas(modifier = modifier.size(18.dp)) {
            val w = size.width
            val h = size.height
            val check = Path().apply {
                moveTo(w * 0.20f, h * 0.52f)
                lineTo(w * 0.42f, h * 0.72f)
                lineTo(w * 0.80f, h * 0.26f)
            }
            drawPath(check, color = tint, style = Stroke(width = 2.4f))
        }
    }

    @Composable
    fun Lock(modifier: Modifier = Modifier, tint: Color = LexiDarkTextSecondary) {
        Canvas(modifier = modifier.size(18.dp)) {
            val w = size.width
            val h = size.height
            drawArc(
                color = tint,
                startAngle = 180f,
                sweepAngle = 180f,
                useCenter = false,
                topLeft = Offset(w * 0.28f, h * 0.15f),
                size = Size(w * 0.44f, h * 0.40f),
                style = Stroke(width = 2.2f)
            )
            drawRoundRect(
                color = tint,
                topLeft = Offset(w * 0.20f, h * 0.44f),
                size = Size(w * 0.60f, h * 0.44f),
                cornerRadius = CornerRadius(4f, 4f)
            )
            drawCircle(color = LexiDarkBg, radius = w * 0.06f, center = Offset(w * 0.5f, h * 0.62f))
        }
    }

    @Composable
    fun Back(modifier: Modifier = Modifier, tint: Color = LexiDarkTextPrimary) {
        Canvas(modifier = modifier.size(20.dp)) {
            val w = size.width
            val h = size.height
            val path = Path().apply {
                moveTo(w * 0.60f, h * 0.22f)
                lineTo(w * 0.32f, h * 0.50f)
                lineTo(w * 0.60f, h * 0.78f)
            }
            drawPath(path, color = tint, style = Stroke(width = 2.4f))
        }
    }
}

enum class NavigationTab(val title: String) {
    HOME("Home"),
    EXPLORE("Explore"),
    ARCADE("Arcade"),
    PROGRESS("Progress"),
    SETTINGS("Settings")
}

@Composable
fun LexiBottomNavigationBar(
    currentTab: NavigationTab,
    onTabSelected: (NavigationTab) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = LexiDarkBg,
        tonalElevation = 0.dp
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            LexiDivider(color = LexiDarkDivider)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp, horizontal = 8.dp),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                NavigationTab.values().forEach { tab ->
                    val isSelected = tab == currentTab
                    val itemColor by animateColorAsState(
                        targetValue = if (isSelected) LexiDarkTextPrimary else LexiDarkTextSecondary,
                        animationSpec = tween(150),
                        label = "tab_color"
                    )

                    Column(
                        modifier = Modifier
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) { onTabSelected(tab) }
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        when (tab) {
                            NavigationTab.HOME -> LexiIcons.Home(tint = itemColor)
                            NavigationTab.EXPLORE -> LexiIcons.Compass(tint = itemColor)
                            NavigationTab.ARCADE -> LexiIcons.Gamepad(tint = itemColor)
                            NavigationTab.PROGRESS -> LexiIcons.Chart(tint = itemColor)
                            NavigationTab.SETTINGS -> LexiIcons.Gear(tint = itemColor)
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = tab.title,
                            color = itemColor,
                            fontSize = 10.sp,
                            fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Normal
                        )
                    }
                }
            }
        }
    }
}
