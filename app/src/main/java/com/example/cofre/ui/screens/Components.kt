package com.example.cofre.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.cofre.core.Money
import com.example.cofre.data.SoundEvent
import com.example.cofre.ui.LocalSoundEngine
import com.example.cofre.ui.theme.*
import kotlinx.coroutines.launch

private val CardShape = RoundedCornerShape(20.dp)
private val ButtonShape = RoundedCornerShape(16.dp)

@Composable
fun PixelButton(
    text: String,
    modifier: Modifier = Modifier,
    gold: Boolean = true,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    val sound = LocalSoundEngine.current
    val clickPulse = remember { androidx.compose.animation.core.Animatable(1f) }
    val scope = rememberCoroutineScope()
    val scale = clickPulse.value
    val container = if (gold) PxGold else PxBlue
    val border = if (gold) PxGoldDark else PxBlueLight
    val content = if (gold) PxInk else PxText

    Button(
        onClick = {
            sound.play(SoundEvent.BUTTON)
            scope.launch {
                clickPulse.animateTo(0.965f, tween(70))
                clickPulse.animateTo(1f, spring(dampingRatio = 0.62f, stiffness = 850f))
            }
            onClick()
        },
        modifier = modifier
            .height(54.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            },
        enabled = enabled,
        shape = ButtonShape,
        elevation = ButtonDefaults.buttonElevation(
            defaultElevation = 7.dp,
            pressedElevation = 2.dp,
            disabledElevation = 0.dp,
        ),
        colors = ButtonDefaults.buttonColors(
            containerColor = container,
            contentColor = content,
            disabledContainerColor = PxBlueDark.copy(alpha = 0.4f),
            disabledContentColor = PxDim.copy(alpha = 0.65f),
        ),
        border = androidx.compose.foundation.BorderStroke(1.5.dp, border),
        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 0.dp),
    ) {
        Text(text, fontWeight = FontWeight.Bold, fontSize = 14.sp)
    }
}

@Composable
fun PixelCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(320, easing = FastOutSlowInEasing)) +
            slideInVertically(tween(360, easing = FastOutSlowInEasing)) { it / 16 },
        modifier = modifier,
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        listOf(PxPanel.copy(alpha = 0.97f), PxPanel.copy(alpha = 0.90f)),
                    ),
                    CardShape,
                )
                .border(1.5.dp, PxBlue.copy(alpha = 0.72f), CardShape)
                .padding(14.dp),
            content = content,
        )
    }
}

@Composable
fun ScreenHeader(title: String, onBack: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TextButton(onClick = onBack) {
            Text("‹ Volver", color = PxBlueLight, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.width(8.dp))
        Text(
            title,
            color = PxGold,
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.titleMedium,
        )
    }
}

@Composable
fun PixelField(
    value: String,
    onChange: (String) -> Unit,
    label: String,
    decimal: Boolean = false,
    singleLine: Boolean = true,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label) },
        singleLine = singleLine,
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.fillMaxWidth(),
        keyboardOptions = KeyboardOptions(keyboardType = if (decimal) KeyboardType.Decimal else KeyboardType.Text),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = PxGold,
            unfocusedBorderColor = PxBlue.copy(alpha = 0.8f),
            focusedLabelColor = PxGold,
            unfocusedLabelColor = PxDim,
            cursorColor = PxGold,
            focusedContainerColor = PxInk.copy(alpha = 0.26f),
            unfocusedContainerColor = PxInk.copy(alpha = 0.18f),
        ),
    )
}

/** Barra segmentada RGB: el avance y el brillo se animan de forma independiente. */
@Composable
fun PixelProgress(progress: Float, modifier: Modifier = Modifier) {
    val target = progress.coerceIn(0f, 1f)
    val animatedProgress by animateFloatAsState(
        targetValue = target,
        animationSpec = tween(900, easing = FastOutSlowInEasing),
        label = "progressFill",
    )
    val infinite = rememberInfiniteTransition(label = "rgbBar")
    val phase by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(3600, easing = androidx.compose.animation.core.LinearEasing)),
        label = "rgbHue",
    )
    val glow by infinite.animateFloat(
        initialValue = 0.72f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1100, easing = FastOutSlowInEasing), androidx.compose.animation.core.RepeatMode.Reverse),
        label = "rgbGlow",
    )

    Canvas(modifier.fillMaxWidth().height(30.dp)) {
        val n = 20
        val gap = 3.dp.toPx()
        val seg = (size.width - gap * (n - 1)) / n
        val filled = animatedProgress * n
        for (i in 0 until n) {
            val x = i * (seg + gap)
            drawRoundRect(
                color = PxBlueDark.copy(alpha = 0.32f),
                topLeft = androidx.compose.ui.geometry.Offset(x, 2.dp.toPx()),
                size = androidx.compose.ui.geometry.Size(seg, size.height - 4.dp.toPx()),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(5.dp.toPx(), 5.dp.toPx()),
            )
            val f = (filled - i).coerceIn(0f, 1f)
            if (f > 0f) {
                val hue = (phase + (i / n.toFloat()) * 360f) % 360f
                val rgb = Color.hsv(hue, 0.88f, 1f).copy(alpha = glow)
                drawRoundRect(
                    brush = Brush.horizontalGradient(listOf(rgb.copy(alpha = 0.78f), rgb, Color.hsv((hue + 32f) % 360f, 0.82f, 1f).copy(alpha = glow))),
                    topLeft = androidx.compose.ui.geometry.Offset(x, 2.dp.toPx()),
                    size = androidx.compose.ui.geometry.Size(seg * f, size.height - 4.dp.toPx()),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(5.dp.toPx(), 5.dp.toPx()),
                )
            }
        }
        drawRoundRect(
            color = PxBlueLight.copy(alpha = 0.52f),
            topLeft = androidx.compose.ui.geometry.Offset(0f, 1.dp.toPx()),
            size = androidx.compose.ui.geometry.Size(size.width, size.height - 2.dp.toPx()),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(6.dp.toPx(), 6.dp.toPx()),
            style = Stroke(1.dp.toPx()),
        )
    }
}

/** Resumen semanal: SALDO INICIAL / GASTADO / AHORRADO / DISPONIBLE. */
@Composable
fun WeekSummaryGrid(initial: Long, spent: Long, saved: Long, available: Long) {
    @Composable
    fun Cell(label: String, value: Long, color: Color, m: Modifier) {
        PixelCard(m) {
            Text(label, color = PxDim, style = MaterialTheme.typography.labelSmall)
            Text(
                Money.format(value),
                color = color,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleMedium,
            )
        }
    }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Cell("SALDO INICIAL", initial, PxText, Modifier.weight(1f))
            Cell("GASTADO", spent, PxDanger, Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Cell("AHORRADO", saved, PxBlueLight, Modifier.weight(1f))
            Cell("DISPONIBLE", available, PxGold, Modifier.weight(1f))
        }
    }
}
