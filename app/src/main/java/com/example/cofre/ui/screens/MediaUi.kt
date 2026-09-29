package com.example.cofre.ui.screens

import androidx.compose.ui.draw.clipToBounds
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.example.cofre.data.AppSettings
import com.example.cofre.data.LocalMediaStore
import com.example.cofre.ui.theme.PxBg
import com.example.cofre.ui.theme.PxGold
import com.example.cofre.ui.theme.PxPanel

@Composable
fun LocalImage(store: LocalMediaStore, ref: String?, modifier: Modifier = Modifier, sizePx: Int = 512, contentScale: ContentScale = ContentScale.Fit, contentDescription: String? = null) {
    val bitmap by produceState<android.graphics.Bitmap?>(initialValue = null, ref, sizePx) {
        value = withContext(Dispatchers.IO) { store.decode(ref, sizePx) }
    }
    bitmap?.let { Image(it.asImageBitmap(), contentDescription = contentDescription, modifier = modifier, contentScale = contentScale) }
}

@Composable
fun PhotoPreviewDialog(store: LocalMediaStore, ref: String, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = PxPanel,
        title = { Text("Fotografía", color = PxGold) },
        text = { Box(Modifier.size(300.dp).background(Color.Black), contentAlignment = Alignment.Center) { LocalImage(store, ref, Modifier.fillMaxSize(), 1600, ContentScale.Fit, "Fotografía ampliada") } },
        confirmButton = { TextButton(onDismiss) { Text("Cerrar", color = PxGold) } },
    )
}

@Composable
fun BackgroundLayer(store: LocalMediaStore, settings: AppSettings, section: String, modifier: Modifier = Modifier) {
    data class Spec(val ref: String?, val scale: Float, val x: Float, val y: Float, val opacity: Float)
    val spec = when (section) {
        "chest" -> Spec(settings.chestBackground, settings.chestScale, settings.chestOffsetX, settings.chestOffsetY, settings.chestOpacity)
        "week" -> Spec(settings.weekBackground, settings.weekScale, settings.weekOffsetX, settings.weekOffsetY, settings.weekOpacity)
        else -> Spec(settings.otherBackground, settings.otherScale, settings.otherOffsetX, settings.otherOffsetY, settings.otherOpacity)
    }
    if (!spec.ref.isNullOrBlank()) {
        val alpha by animateFloatAsState(
            targetValue = spec.opacity.coerceIn(0f, 1f),
            animationSpec = tween(280),
            label = "backgroundOpacity",
        )
        Box(modifier.clipToBounds()) {
            LocalImage(
                store,
                spec.ref,
                Modifier.fillMaxSize().graphicsLayer(
                    scaleX = spec.scale,
                    scaleY = spec.scale,
                    translationX = spec.x,
                    translationY = spec.y,
                    alpha = alpha,
                ),
                1400,
                ContentScale.Crop,
            )
            // Velo oscuro sutil para que la imagen no compita con el contenido.
            Box(Modifier.fillMaxSize().background(PxBg.copy(alpha = 0.66f)))
        }
    }
}
