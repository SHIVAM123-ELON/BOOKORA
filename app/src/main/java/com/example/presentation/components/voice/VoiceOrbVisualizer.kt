package com.example.presentation.components.voice

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.model.voice.LiveConnectionState
import com.example.ui.theme.PolishPrimaryIndigo
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun VoiceOrbVisualizer(
    state: LiveConnectionState,
    audioLevel: Float, // 0.0f to 1.0f
    personaName: String,
    modifier: Modifier = Modifier
) {
    // Pulse animation for idle/active state
    val infiniteTransition = rememberInfiniteTransition(label = "orb_pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.96f,
        targetValue = 1.04f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )

    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(8000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )

    val dynamicScale = when (state) {
        LiveConnectionState.LISTENING -> (1.0f + audioLevel * 0.45f).coerceIn(1.0f, 1.45f)
        LiveConnectionState.SPEAKING -> (1.0f + audioLevel * 0.35f).coerceIn(1.0f, 1.35f)
        LiveConnectionState.THINKING -> pulseScale * 1.08f
        else -> pulseScale
    }

    val primaryColor = when (state) {
        LiveConnectionState.LISTENING -> Color(0xFF10B981) // Emerald Active Mic
        LiveConnectionState.SPEAKING -> PolishPrimaryIndigo // Indigo Live Gemini Speech
        LiveConnectionState.THINKING -> Color(0xFFF59E0B) // Amber Processing
        LiveConnectionState.ERROR -> Color(0xFFEF4444)
        else -> PolishPrimaryIndigo
    }

    val secondaryColor = when (state) {
        LiveConnectionState.LISTENING -> Color(0xFF34D399)
        LiveConnectionState.SPEAKING -> Color(0xFF818CF8)
        LiveConnectionState.THINKING -> Color(0xFFFCD34D)
        else -> Color(0xFFA5B4FC)
    }

    Box(
        modifier = modifier
            .size(240.dp)
            .testTag("voice_orb_visualizer"),
        contentAlignment = Alignment.Center
    ) {
        // Outer concentric wave ripples
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .scale(dynamicScale)
        ) {
            val center = this.center
            val baseRadius = size.minDimension / 2f * 0.72f

            // Outer ring 1
            drawCircle(
                color = primaryColor.copy(alpha = 0.12f + audioLevel * 0.2f),
                radius = baseRadius * 1.35f,
                style = Stroke(width = 2.dp.toPx())
            )

            // Outer ring 2
            drawCircle(
                color = secondaryColor.copy(alpha = 0.18f + audioLevel * 0.25f),
                radius = baseRadius * 1.15f,
                style = Stroke(width = 3.dp.toPx())
            )

            // Frequency nodes around ring
            val nodeCount = 12
            for (i in 0 until nodeCount) {
                val angle = (i * (360f / nodeCount) + rotation) * (PI / 180f)
                val nodeOffset = if (state == LiveConnectionState.SPEAKING || state == LiveConnectionState.LISTENING) {
                    (sin(angle * 3 + rotation * 0.05) * (audioLevel * 18.dp.toPx())).toFloat()
                } else 0f

                val nodeRadius = baseRadius * 1.15f + nodeOffset
                val nodeX = center.x + cos(angle).toFloat() * nodeRadius
                val nodeY = center.y + sin(angle).toFloat() * nodeRadius

                drawCircle(
                    color = primaryColor.copy(alpha = 0.6f + audioLevel * 0.4f),
                    radius = (3.dp.toPx() + audioLevel * 4.dp.toPx()),
                    center = androidx.compose.ui.geometry.Offset(nodeX, nodeY)
                )
            }
        }

        // Inner glowing gradient Orb
        Surface(
            modifier = Modifier
                .size(130.dp)
                .scale(dynamicScale)
                .clip(CircleShape),
            color = Color.Transparent,
            shadowElevation = 12.dp
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                secondaryColor,
                                primaryColor,
                                primaryColor.copy(alpha = 0.85f)
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = when (state) {
                            LiveConnectionState.LISTENING -> Icons.Default.Mic
                            LiveConnectionState.SPEAKING -> Icons.Default.VolumeUp
                            LiveConnectionState.THINKING -> Icons.Default.GraphicEq
                            else -> Icons.Default.GraphicEq
                        },
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(38.dp)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = personaName,
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            fontSize = 12.sp
                        )
                    )
                }
            }
        }
    }
}
