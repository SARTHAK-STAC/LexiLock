package com.sarthak.lexilock.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sarthak.lexilock.ui.theme.*

// ==========================================
// LEXILOCK DESIGN SYSTEM: PRODUCTION COMPONENTS
// Human-Designed, Material Restraint, Editorial Simplicity
// Max corner radius: 8dp - 12dp. No glow. No neon. No glassmorphism.
// ==========================================

@Composable
fun LexiCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    backgroundColor: Color = LexiDarkSurfaceSubtle,
    borderColor: Color = LexiDarkDivider,
    cornerRadius: Dp = 10.dp,
    contentPadding: PaddingValues = PaddingValues(16.dp),
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(cornerRadius))
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier),
        color = backgroundColor,
        shape = RoundedCornerShape(cornerRadius),
        border = BorderStroke(1.dp, borderColor)
    ) {
        Column(
            modifier = Modifier.padding(contentPadding),
            content = content
        )
    }
}

// Quiet container compatibility - redirects to plain, clean surface without glow
@Composable
fun LexiGlowCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    cornerRadius: Dp = 10.dp,
    content: @Composable ColumnScope.() -> Unit
) {
    LexiCard(
        modifier = modifier,
        onClick = onClick,
        cornerRadius = cornerRadius,
        backgroundColor = LexiDarkSurfaceSubtle,
        borderColor = LexiDarkDivider,
        content = content
    )
}

@Composable
fun LexiButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: (@Composable () -> Unit)? = null,
    isPrimary: Boolean = true
) {
    if (isPrimary) {
        Button(
            onClick = onClick,
            modifier = modifier.height(44.dp),
            shape = RoundedCornerShape(8.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = LexiDarkAccent,
                contentColor = Color.Black
            ),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
        ) {
            if (icon != null) {
                icon()
                Spacer(modifier = Modifier.width(6.dp))
            }
            Text(
                text = text,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
        }
    } else {
        OutlinedButton(
            onClick = onClick,
            modifier = modifier.height(44.dp),
            shape = RoundedCornerShape(8.dp),
            colors = ButtonDefaults.outlinedButtonColors(
                containerColor = Color.Transparent,
                contentColor = LexiDarkTextPrimary
            ),
            border = BorderStroke(1.dp, LexiDarkDivider),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
        ) {
            if (icon != null) {
                icon()
                Spacer(modifier = Modifier.width(6.dp))
            }
            Text(
                text = text,
                fontSize = 14.sp,
                fontWeight = FontWeight.Normal
            )
        }
    }
}

@Composable
fun LexiIconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    backgroundColor: Color = Color.Transparent,
    borderColor: Color = LexiDarkDivider,
    icon: @Composable () -> Unit
) {
    Surface(
        modifier = modifier
            .size(38.dp)
            .clip(CircleShape)
            .clickable { onClick() },
        color = backgroundColor,
        shape = CircleShape,
        border = BorderStroke(1.dp, borderColor)
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            icon()
        }
    }
}

@Composable
fun LexiChip(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    accentColor: Color = LexiDarkAccent
) {
    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable { onClick() },
        color = if (isSelected) LexiDarkTextPrimary else Color.Transparent,
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(
            1.dp,
            if (isSelected) LexiDarkTextPrimary else LexiDarkDivider
        )
    ) {
        Text(
            text = text,
            color = if (isSelected) LexiDarkBg else LexiDarkTextSecondary,
            fontSize = 13.sp,
            fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Normal,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
        )
    }
}

@Composable
fun LexiSectionHeader(
    title: String,
    subtitle: String? = null,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = title,
            color = LexiDarkTextPrimary,
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold
        )
        if (subtitle != null) {
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                color = LexiDarkTextSecondary,
                fontSize = 13.sp
            )
        }
    }
}

@Composable
fun LexiProgressBar(
    progress: Float,
    modifier: Modifier = Modifier,
    color: Color = LexiDarkAccent,
    trackColor: Color = LexiDarkDivider,
    height: Dp = 4.dp
) {
    val animatedProgress by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 200),
        label = "lexi_progress"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .clip(RoundedCornerShape(height / 2))
            .background(trackColor)
    ) {
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(animatedProgress)
                .clip(RoundedCornerShape(height / 2))
                .background(color)
        )
    }
}

@Composable
fun LexiDivider(
    modifier: Modifier = Modifier,
    color: Color = LexiDarkDivider,
    thickness: Dp = 1.dp
) {
    Divider(modifier = modifier, color = color, thickness = thickness)
}
