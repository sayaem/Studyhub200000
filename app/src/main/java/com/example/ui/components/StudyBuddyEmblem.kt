package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

enum class EmblemSize(val dp: Dp) {
    SMALL(24.dp),
    TOOLBAR(30.dp),
    MEDIUM(42.dp),
    LARGE(60.dp),
    HERO(84.dp)
}

/**
 * Native Vector/Canvas Robot Mascot for Study Buddy matching reference image.
 */
@Composable
fun StudyBuddyRobotMascot(
    modifier: Modifier = Modifier,
    size: Dp = 48.dp,
    isAnimated: Boolean = false
) {
    val scalePulse = if (isAnimated) {
        val infiniteTransition = rememberInfiniteTransition(label = "robot_pulse")
        val animateScale by infiniteTransition.animateFloat(
            initialValue = 0.96f,
            targetValue = 1.05f,
            animationSpec = infiniteRepeatable(
                animation = tween(1200, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "pulse_scale"
        )
        animateScale
    } else {
        1.0f
    }

    Canvas(
        modifier = modifier
            .size(size)
            .scale(scalePulse)
    ) {
        val w = size.toPx()
        val h = size.toPx()

        // 1. Ears (Left and right side flaps)
        val earWidth = w * 0.12f
        val earHeight = h * 0.28f
        val earRadius = CornerRadius(earWidth / 2f, earWidth / 2f)

        val earGradient = Brush.linearGradient(
            colors = listOf(Color(0xFF00C9A7), Color(0xFF008B99))
        )

        drawRoundRect(
            brush = earGradient,
            topLeft = Offset(0f, (h - earHeight) / 2f),
            size = Size(earWidth, earHeight),
            cornerRadius = earRadius
        )

        drawRoundRect(
            brush = earGradient,
            topLeft = Offset(w - earWidth, (h - earHeight) / 2f),
            size = Size(earWidth, earHeight),
            cornerRadius = earRadius
        )

        // 2. Robot Outer Head (Rounded Hexagon/Capsule Head)
        val headLeft = w * 0.08f
        val headRight = w * 0.92f
        val headTop = h * 0.08f
        val headBottom = h * 0.92f
        val rx = w * 0.36f
        val ry = h * 0.36f

        val headPath = Path().apply {
            addRoundRect(
                RoundRect(
                    rect = Rect(headLeft, headTop, headRight, headBottom),
                    cornerRadius = CornerRadius(rx, ry)
                )
            )
        }

        val headGradient = Brush.verticalGradient(
            colors = listOf(
                Color(0xFF00D2C8), // Top cyan highlight
                Color(0xFF0796A3), // Middle vibrant teal
                Color(0xFF026B75)  // Bottom deep teal
            )
        )

        drawPath(path = headPath, brush = headGradient)

        // Soft top highlight
        drawCircle(
            color = Color.White.copy(alpha = 0.22f),
            radius = w * 0.26f,
            center = Offset(w * 0.5f, h * 0.22f)
        )

        // 3. Dark Navy Visor Screen
        val visorWidth = w * 0.62f
        val visorHeight = h * 0.44f
        val visorLeft = (w - visorWidth) / 2f
        val visorTop = h * 0.30f

        val visorPath = Path().apply {
            addRoundRect(
                RoundRect(
                    rect = Rect(visorLeft, visorTop, visorLeft + visorWidth, visorTop + visorHeight),
                    cornerRadius = CornerRadius(visorWidth * 0.38f, visorHeight * 0.48f)
                )
            )
        }

        drawPath(
            path = visorPath,
            color = Color(0xFF0F172A) // Deep navy dark face screen
        )

        // 4. Glowing Cyan Curved Eyes
        val eyeWidth = visorWidth * 0.26f
        val eyeHeight = visorHeight * 0.42f
        val eyeTop = visorTop + (visorHeight - eyeHeight) / 2f
        val leftEyeX = visorLeft + visorWidth * 0.20f
        val rightEyeX = visorLeft + visorWidth * 0.80f - eyeWidth

        val eyeColor = Color(0xFF38BDF8) // Bright cyan

        // Left Eye
        drawRoundRect(
            color = eyeColor,
            topLeft = Offset(leftEyeX, eyeTop),
            size = Size(eyeWidth, eyeHeight),
            cornerRadius = CornerRadius(eyeWidth / 2f, eyeHeight / 2f)
        )

        // Right Eye
        drawRoundRect(
            color = eyeColor,
            topLeft = Offset(rightEyeX, eyeTop),
            size = Size(eyeWidth, eyeHeight),
            cornerRadius = CornerRadius(eyeWidth / 2f, eyeHeight / 2f)
        )

        // Eye glint highlights
        drawCircle(
            color = Color.White,
            radius = eyeWidth * 0.22f,
            center = Offset(leftEyeX + eyeWidth * 0.65f, eyeTop + eyeHeight * 0.35f)
        )
        drawCircle(
            color = Color.White,
            radius = eyeWidth * 0.22f,
            center = Offset(rightEyeX + eyeWidth * 0.65f, eyeTop + eyeHeight * 0.35f)
        )
    }
}

/**
 * Compatibility wrapper function for StudyBuddyEmblem.
 */
@Composable
fun StudyBuddyEmblem(
    size: EmblemSize = EmblemSize.MEDIUM,
    isAnimated: Boolean = false,
    modifier: Modifier = Modifier
) {
    StudyBuddyRobotMascot(
        modifier = modifier,
        size = size.dp,
        isAnimated = isAnimated
    )
}
