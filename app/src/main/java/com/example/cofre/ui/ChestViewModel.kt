package com.example.cofre.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.cofre.data.ChestRepository
import com.example.cofre.data.TransactionEntity
import com.example.cofre.data.TxType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class ChestUiState(
    val loading: Boolean = true,
    val onboardingDone: Boolean = false,
    val goalName: String = ChestRepository.GOAL_NAME,
    val targetCents: Long = ChestRepository.GOAL_TARGET_CENTS,
    val balanceCents: Long = 0,
    val transactions: List<TransactionEntity> = emptyList(),
) {
    /** 0f..1f. Nunca supera 1 aunque el saldo rebase la meta. */
    val progress: Float
        get() = percent / 100f
    val percent: Int
        get() = if (targetCents <= 0) 0 else (minOf(balanceCents, targetCents) * 100 / targetCents).toInt()
    val surplusCents: Long get() = maxOf(0L, balanceCents - targetCents)
    val goalReached: Boolean get() = targetCents > 0 && balanceCents >= targetCents
}

class ChestViewModel(private val repo: ChestRepository) : ViewModel() {

    val state: StateFlow<ChestUiState> = combine(
        repo.onboardingDone, repo.goal, repo.balance, repo.transactions,
    ) { done, goal, balance, list ->
        ChestUiState(
            loading = false,
            onboardingDone = done,
            goalName = goal?.name ?: ChestRepository.GOAL_NAME,
            targetCents = goal?.targetCents ?: ChestRepository.GOAL_TARGET_CENTS,
            balanceCents = balance,
            transactions = list,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ChestUiState())

    /** true tras una aportación exitosa; la pantalla principal lo consume para animar monedas. */
    private val _pendingBurst = MutableStateFlow(false)
    val pendingBurst: StateFlow<Boolean> = _pendingBurst
    private val _pendingWithdrawal = MutableStateFlow(false)
    val pendingWithdrawal: StateFlow<Boolean> = _pendingWithdrawal

    init { viewModelScope.launch { repo.ensureGoal() } }

    fun consumeBurst() { _pendingBurst.value = false }
    fun consumeWithdrawal() { _pendingWithdrawal.value = false }
    fun triggerBurst() { _pendingBurst.value = true }

    fun completeOnboarding(cents: Long, onDone: () -> Unit) {
        viewModelScope.launch { repo.completeOnboarding(cents); onDone() }
    }

    fun save(type: TxType, cents: Long, concept: String, at: Long, onResult: (String?) -> Unit) {
        viewModelScope.launch {
            val r = repo.addMovement(type, cents, concept, at)
            if (r.isSuccess && type == TxType.DEPOSIT) _pendingBurst.value = true
            if (r.isSuccess && type == TxType.WITHDRAWAL) _pendingWithdrawal.value = true
            onResult(r.errorText())
        }
    }

    fun update(id: Long, cents: Long, concept: String, at: Long, onResult: (String?) -> Unit) {
        viewModelScope.launch { onResult(repo.updateMovement(id, cents, concept, at).errorText()) }
    }

    fun delete(id: Long, onResult: (String?) -> Unit) {
        viewModelScope.launch { onResult(repo.deleteMovement(id).errorText()) }
    }

    companion object {
        fun factory(repo: ChestRepository) = viewModelFactory { initializer { ChestViewModel(repo) } }
    }
}
