package com.example.cofre.ui.screens

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.cofre.data.AppSettings
import com.example.cofre.data.LocalMediaStore
import com.example.cofre.ui.theme.PxBg
import com.example.cofre.ui.theme.PxGold
import com.example.cofre.ui.theme.PxGoldLight
import com.example.cofre.ui.theme.PxBlueLight

@Composable
fun StartupScreen(media: LocalMediaStore, settings: AppSettings) {
    val transition = rememberInfiniteTransition(label = "startup")
    val rotation by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(2600, easing = androidx.compose.animation.core.LinearEasing)),
        label = "startupRotation",
    )
    val pulse by transition.animateFloat(
        initialValue = 0.96f,
        targetValue = 1.04f,
        animationSpec = infiniteRepeatable(tween(850, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "startupPulse",
    )

    Box(
        Modifier.fillMaxSize().background(PxBg),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Box(
                Modifier.size(156.dp).graphicsLayer {
                    scaleX = pulse
                    scaleY = pulse
                },
                contentAlignment = Alignment.Center,
            ) {
                Canvas(Modifier.fillMaxSize()) {
                    drawArc(
                        brush = Brush.sweepGradient(
                            listOf(PxGold, PxBlueLight, Color(0xFFFF4DCE), Color(0xFF54F4FF), PxGold),
                        ),
                        startAngle = rotation,
                        sweepAngle = 300f,
                        useCenter = false,
                        style = Stroke(5.dp.toPx(), cap = StrokeCap.Round),
                    )
                }
                Box(
                    Modifier.size(122.dp).clip(CircleShape).background(Color.Black.copy(alpha = 0.35f)),
                    contentAlignment = Alignment.Center,
                ) {
                    if (settings.profilePhoto != null) {
                        LocalImage(
                            media,
                            settings.profilePhoto,
                            Modifier.fillMaxSize().clip(CircleShape),
                            512,
                            androidx.compose.ui.layout.ContentScale.Crop,
                            "Foto de perfil",
                        )
                    } else {
                        Text(
                            "CH",
                            color = PxGoldLight,
                            fontWeight = FontWeight.Black,
                            fontSize = 34.sp,
                        )
                    }
                }
            }
            Spacer(Modifier.height(24.dp))
            Text(
                "COFRE HERO",
                color = PxGold,
                fontWeight = FontWeight.ExtraBold,
                style = MaterialTheme.typography.headlineSmall,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                "Preparando tu cofre…",
                color = PxBlueLight,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}
