package com.example.cofre.ui

import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.cofre.core.WeekMath
import com.example.cofre.core.FinalSecret
import com.example.cofre.data.AppSettings
import com.example.cofre.data.LocalMediaStore
import com.example.cofre.data.TxType
import com.example.cofre.data.WeekDetail
import com.example.cofre.data.WeekEntry
import com.example.cofre.ui.screens.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import java.time.LocalDate

@Composable
fun AppNav(vm: ChestViewModel, weeksVm: WeeksViewModel, settingsVm: SettingsViewModel, media: LocalMediaStore) {
    val state by vm.state.collectAsStateWithLifecycle()
    val weeks by weeksVm.state.collectAsStateWithLifecycle()
    val settings by settingsVm.settings.collectAsStateWithLifecycle()
    val sound = LocalSoundEngine.current
    var showStartup by rememberSaveable { mutableStateOf(true) }
    LaunchedEffect(Unit) {
        delay(900)
        showStartup = false
    }
    LaunchedEffect(settings.soundsEnabled, settings.effectsVolume) { sound.enabled = settings.soundsEnabled; sound.volume = settings.effectsVolume }
    if (showStartup || state.loading || weeks.loading) {
        StartupScreen(media, settings)
        return
    }
    AppNavHost(vm, weeksVm, settingsVm, media, state, weeks, settings)
}

private fun longArgs(vararg names: String) = names.map { navArgument(it) { type = NavType.LongType } }

@Composable
private fun WeekHost(weeksVm: WeeksViewModel, weekId: Long, content: @Composable (WeekDetail) -> Unit) {
    val detail by remember(weekId) { weeksVm.observeDetail(weekId) }.collectAsStateWithLifecycle(initialValue = null)
    detail?.let { content(it) }
}

@Composable
private fun AppNavHost(vm: ChestViewModel, weeksVm: WeeksViewModel, settingsVm: SettingsViewModel, media: LocalMediaStore, state: ChestUiState, weeks: WeeksUiState, settings: AppSettings) {
    val nav = rememberNavController()
    val start = rememberSaveable { if (state.onboardingDone) "home" else "onboarding" }
    val pendingBurst by vm.pendingBurst.collectAsStateWithLifecycle()
    val route = nav.currentBackStackEntryAsState().value?.destination?.route
    val sound = LocalSoundEngine.current
    var localDate by remember { mutableStateOf(LocalDate.now()) }
    LaunchedEffect(Unit) {
        while (isActive) {
            localDate = LocalDate.now()
            delay(30_000)
        }
    }
    val finalUnlocked = FinalSecret.isUnlocked(state.balanceCents, localDate)
    var promptedFor by rememberSaveable { mutableStateOf(-1L) }
    LaunchedEffect(route, weeks.currentMonday, weeks.current == null, state.onboardingDone) {
        if (route == "home" && state.onboardingDone && weeks.current == null && promptedFor != weeks.currentMonday) {
            promptedFor = weeks.currentMonday
            nav.navigate("weekSetup/current")
        }
    }
    LaunchedEffect(route) { if (route != null) sound.play(com.example.cofre.data.SoundEvent.NAVIGATION) }

    NavHost(
        nav,
        startDestination = start,
        enterTransition = {
            fadeIn(tween(240, easing = FastOutSlowInEasing)) +
                slideInHorizontally(tween(300, easing = FastOutSlowInEasing)) { it / 14 } +
                scaleIn(tween(260, easing = FastOutSlowInEasing), initialScale = 0.985f)
        },
        exitTransition = {
            fadeOut(tween(180, easing = FastOutSlowInEasing)) +
                slideOutHorizontally(tween(220, easing = FastOutSlowInEasing)) { -it / 18 } +
                scaleOut(tween(200, easing = FastOutSlowInEasing), targetScale = 0.99f)
        },
        popEnterTransition = {
            fadeIn(tween(220, easing = FastOutSlowInEasing)) +
                slideInHorizontally(tween(280, easing = FastOutSlowInEasing)) { -it / 14 }
        },
        popExitTransition = {
            fadeOut(tween(170, easing = FastOutSlowInEasing)) +
                slideOutHorizontally(tween(210, easing = FastOutSlowInEasing)) { it / 18 }
        },
    ) {
        composable("onboarding") {
            OnboardingScreen { cents -> vm.completeOnboarding(cents) { nav.navigate("home") { popUpTo("onboarding") { inclusive = true } } } }
        }
        composable("home") {
            val pendingWithdrawal by vm.pendingWithdrawal.collectAsStateWithLifecycle()
            HomeScreen(state, pendingBurst, vm::consumeBurst, pendingWithdrawal, vm::consumeWithdrawal,
                onDeposit = { nav.navigate("form/DEPOSIT") }, onWithdraw = { nav.navigate("form/WITHDRAWAL") }, onHistory = { nav.navigate("history") },
                week = weeks.current, onWeek = { weeks.current?.let { nav.navigate("week/${it.id}") } }, onWeeks = { nav.navigate("weeks") }, onNewWeek = { nav.navigate("weekSetup/current") },
                onSettings = { nav.navigate("settings") }, onSecret = { if (finalUnlocked) nav.navigate("finalSecret") }, finalUnlocked = finalUnlocked, settings = settings, media = media)
        }
        composable("settings") { SettingsScreen(settingsVm, media, finalUnlocked, { if (finalUnlocked) nav.navigate("finalSecret") }) { nav.popBackStack() } }
        composable("finalSecret") {
            if (finalUnlocked) FinalSecretScreen { nav.popBackStack() }
            else LaunchedEffect(Unit) { nav.popBackStack() }
        }
        composable("form/{kind}", arguments = listOf(navArgument("kind") { type = NavType.StringType })) { entry ->
            val type = TxType.valueOf(entry.arguments!!.getString("kind")!!)
            MovementFormScreen(if (type == TxType.WITHDRAWAL) "Retirar del cofre" else "Agregar al cofre", if (type == TxType.WITHDRAWAL) "RETIRAR" else "AGREGAR", type == TxType.WITHDRAWAL, null, state.balanceCents, { nav.popBackStack() }) { cents, concept, at, onError ->
                vm.save(type, cents, concept, at) { err ->
                    if (err == null) { sound.play(if (type == TxType.WITHDRAWAL) com.example.cofre.data.SoundEvent.WITHDRAWAL else com.example.cofre.data.SoundEvent.CONTRIBUTION); nav.popBackStack() } else onError(err)
                }
            }
        }
        composable("edit/{id}", arguments = longArgs("id")) { entry ->
            val id = entry.arguments!!.getLong("id")
            val tx = state.transactions.firstOrNull { it.id == id }
            if (tx == null || tx.transferId != null) LaunchedEffect(Unit) { nav.popBackStack() }
            else MovementFormScreen("Editar movimiento", "GUARDAR", tx.type == TxType.WITHDRAWAL, tx, state.balanceCents, { nav.popBackStack() }) { cents, concept, at, onError -> vm.update(id, cents, concept, at) { err -> if (err == null) nav.popBackStack() else onError(err) } }
        }
        composable("history") {
            HistoryScreen(state.transactions, { nav.popBackStack() }, { nav.navigate("edit/$it") }, { id, cb -> vm.delete(id, cb) })
        }
        composable("weekSetup/current") {
            WeekSetupScreen("Nueva semana", "¿Cuánto dinero tienes disponible esta semana?", weeks.currentMonday, false, null, { nav.popBackStack() }) { startDay, cents, onError -> weeksVm.createWeek(startDay, cents) { err -> if (err == null) nav.popBackStack() else onError(err) } }
        }
        composable("weekSetup/past") {
            WeekSetupScreen("Semana anterior", "¿Cuánto dinero tenías disponible esa semana?", weeks.currentMonday - 7, true, null, { nav.popBackStack() }) { startDay, cents, onError -> weeksVm.createWeek(startDay, cents) { err -> if (err == null) nav.popBackStack() else onError(err) } }
        }
        composable("weekEdit/{weekId}", arguments = longArgs("weekId")) { entry ->
            WeekHost(weeksVm, entry.arguments!!.getLong("weekId")) { d ->
                WeekSetupScreen("Saldo inicial", "Saldo disponible al inicio de la semana", d.week.startEpochDay, false, d.week.initialCents, { nav.popBackStack() }) { _, cents, onError -> weeksVm.updateInitial(d.week.id, cents) { err -> if (err == null) nav.popBackStack() else onError(err) } }
            }
        }
        composable("weeks") {
            WeeksHistoryScreen(weeks.summaries, weeks.currentMonday, { nav.popBackStack() }, { nav.navigate("week/$it") }, { nav.navigate("weekSetup/past") }, settings, media)
        }
        composable("week/{weekId}", arguments = longArgs("weekId")) { entry ->
            WeekHost(weeksVm, entry.arguments!!.getLong("weekId")) { d ->
                val id = d.week.id
                WeekDetailScreen(d, { nav.popBackStack() }, { nav.navigate("expense/$id/0") }, { nav.navigate("transfer/$id/0") }, { nav.navigate("weekAnalysis/$id") }, { nav.navigate("weekEdit/$id") },
                    { e -> when (e) { is WeekEntry.Spend -> nav.navigate("expense/$id/${e.expense.id}"); is WeekEntry.Save -> nav.navigate("transfer/$id/${e.transfer.id}") } },
                    { e, cb -> when (e) { is WeekEntry.Spend -> weeksVm.deleteExpense(e.expense.id, cb); is WeekEntry.Save -> weeksVm.deleteTransfer(e.transfer.id, cb) } }, settings, media)
            }
        }
        composable("weekAnalysis/{weekId}", arguments = longArgs("weekId")) { entry -> WeekHost(weeksVm, entry.arguments!!.getLong("weekId")) { d -> WeekAnalysisScreen(d, { nav.popBackStack() }, settings, media) } }
        composable("expense/{weekId}/{id}", arguments = longArgs("weekId", "id")) { entry ->
            val id = entry.arguments!!.getLong("id")
            WeekHost(weeksVm, entry.arguments!!.getLong("weekId")) { d ->
                val editing = d.expenses.firstOrNull { it.id == id }
                if (id != 0L && editing == null) LaunchedEffect(Unit) { nav.popBackStack() }
                else WeekEntryFormScreen(WeekEntryKind.EXPENSE, if (editing == null) "Nuevo gasto" else "Editar gasto", if (editing == null) "REGISTRAR GASTO" else "GUARDAR", editing == null, d.week.startEpochDay, d.availableCents + (editing?.amountCents ?: 0L), editing?.amountCents, editing?.concept ?: "", editing?.occurredAt ?: WeekMath.defaultAt(d.week.startEpochDay), editing?.photoUri, media, { nav.popBackStack() }, weeksVm::copyExpensePhoto) { cents, concept, at, photoRef, onError ->
                    if (editing == null) weeksVm.addExpense(d.week.id, cents, concept, at, photoRef, onError)
                    else weeksVm.updateExpense(editing.id, cents, concept, at, photoRef, onError)
                }
            }
        }
        composable("transfer/{weekId}/{id}", arguments = longArgs("weekId", "id")) { entry ->
            val id = entry.arguments!!.getLong("id")
            WeekHost(weeksVm, entry.arguments!!.getLong("weekId")) { d ->
                val editing = d.transfers.firstOrNull { it.id == id }
                if (id != 0L && editing == null) LaunchedEffect(Unit) { nav.popBackStack() }
                else WeekEntryFormScreen(WeekEntryKind.TRANSFER, if (editing == null) "Pasar al cofre" else "Editar transferencia", if (editing == null) "PASAR AL COFRE" else "GUARDAR", editing == null, d.week.startEpochDay, d.availableCents + (editing?.amountCents ?: 0L), editing?.amountCents, editing?.concept ?: "", editing?.occurredAt ?: WeekMath.defaultAt(d.week.startEpochDay), null, media, { if (editing == null) nav.popBackStack("home", false) else nav.popBackStack() }, { _, cb -> cb(null, "No se puede añadir foto a una transferencia.") }) { cents, concept, at, _, onError ->
                    if (editing == null) weeksVm.addTransfer(d.week.id, cents, concept, at) { err -> if (err == null) { sound.play(com.example.cofre.data.SoundEvent.TRANSFER); vm.triggerBurst() } else onError(err) }
                    else weeksVm.updateTransfer(editing.id, cents, concept, at) { err -> if (err == null) Unit else onError(err) }
                }
            }
        }
    }
}
