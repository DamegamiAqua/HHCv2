package com.example.cofre.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.cofre.core.Money
import com.example.cofre.data.ChestRepository
import com.example.cofre.ui.theme.*

@Composable
fun OnboardingScreen(onConfirm: (Long) -> Unit) {
    var amount by rememberSaveable { mutableStateOf("0") }
    var error by remember { mutableStateOf<String?>(null) }

    Scaffold(containerColor = PxBg) { pad ->
        Column(Modifier.padding(pad).padding(20.dp).fillMaxSize().imePadding(), verticalArrangement = Arrangement.Center) {
            Chest(0.6f, Modifier.fillMaxWidth().height(220.dp))
            Spacer(Modifier.height(16.dp))
            Text("TU COFRE", color = PxGold, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.headlineMedium)
            Text("Meta: ${ChestRepository.GOAL_NAME} · ${Money.format(ChestRepository.GOAL_TARGET_CENTS)} MXN", color = PxDim)
            Spacer(Modifier.height(16.dp))
            Text("¿Cuánto dinero ya tienes ahorrado?", color = PxText, style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            PixelField(amount, { amount = it; error = null }, "Saldo inicial (MXN)", decimal = true)
            error?.let { Text(it, color = PxDanger, modifier = Modifier.padding(top = 4.dp)) }
            Spacer(Modifier.height(16.dp))
            PixelButton("COMENZAR", Modifier.fillMaxWidth()) {
                val cents = Money.parse(amount)
                if (cents == null) error = "Ingresa una cantidad válida (por ejemplo 0 o 250.50)." else onConfirm(cents)
            }
        }
    }
}
