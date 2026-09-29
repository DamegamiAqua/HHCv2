package com.example.cofre.data

import androidx.room.withTransaction
import com.example.cofre.core.runCatchingSuspend
import kotlinx.coroutines.flow.Flow

/** Única fuente de verdad. Toda regla financiera (saldo no negativo, signos) vive aquí. */
class ChestRepository(private val db: AppDatabase, private val settings: SettingsStore) {
    companion object {
        const val GOAL_ID = 1L
        const val GOAL_NAME = "Hero Hunk 160R 4V"
        const val GOAL_TARGET_CENTS = 1_500_000L // $15,000.00 MXN
    }

    private val goals = db.goalDao()
    private val txs = db.transactionDao()

    val onboardingDone: Flow<Boolean> = settings.onboardingDone
    val goal: Flow<GoalEntity?> = goals.observe(GOAL_ID)
    val balance: Flow<Long> = txs.observeBalance(GOAL_ID)
    val transactions: Flow<List<TransactionEntity>> = txs.observeAll(GOAL_ID)

    suspend fun ensureGoal() {
        goals.insertIgnore(GoalEntity(GOAL_ID, GOAL_NAME, GOAL_TARGET_CENTS, System.currentTimeMillis()))
    }

    suspend fun completeOnboarding(initialCents: Long) {
        db.withTransaction {
            ensureGoal()
            // Idempotencia defensiva: si DataStore falla después de confirmar Room,
            // reintentar onboarding no debe duplicar el saldo inicial.
            if (txs.count(GOAL_ID) == 0 && initialCents > 0) {
                val now = System.currentTimeMillis()
                txs.insert(TransactionEntity(goalId = GOAL_ID, amountCents = initialCents, concept = "Saldo inicial",
                    type = TxType.INITIAL, occurredAt = now, createdAt = now, updatedAt = now))
            }
        }
        settings.setOnboardingDone()
    }

    suspend fun addMovement(type: TxType, cents: Long, concept: String, at: Long): Result<Unit> = runCatchingSuspend {
        require(type == TxType.DEPOSIT || type == TxType.WITHDRAWAL) { "Tipo inválido." }
        require(cents > 0) { "Ingresa una cantidad mayor a cero." }
        db.withTransaction {
            if (type == TxType.WITHDRAWAL && cents > txs.balance(GOAL_ID)) {
                error("No puedes retirar más que tu saldo disponible.")
            }
            val now = System.currentTimeMillis()
            txs.insert(TransactionEntity(goalId = GOAL_ID,
                amountCents = if (type == TxType.WITHDRAWAL) -cents else cents,
                concept = concept.cleanConcept(), type = type, occurredAt = at, createdAt = now, updatedAt = now))
        }
    }

    suspend fun updateMovement(id: Long, cents: Long, concept: String, at: Long): Result<Unit> = runCatchingSuspend {
        require(cents > 0) { "Ingresa una cantidad mayor a cero." }
        db.withTransaction {
            val old = txs.get(id) ?: error("Movimiento no encontrado.")
            if (old.transferId != null) error("Este movimiento viene de una transferencia semanal; edítalo o bórralo desde la semana.")
            val signed = if (old.type == TxType.WITHDRAWAL) -cents else cents
            if (txs.balance(GOAL_ID) - old.amountCents + signed < 0) error("Este cambio dejaría tu saldo en negativo.")
            txs.update(old.copy(amountCents = signed, concept = concept.cleanConcept(),
                occurredAt = at, updatedAt = System.currentTimeMillis()))
        }
    }

    suspend fun deleteMovement(id: Long): Result<Unit> = runCatchingSuspend {
        db.withTransaction {
            val old = txs.get(id) ?: error("Movimiento no encontrado.")
            if (old.transferId != null) error("Este movimiento viene de una transferencia semanal; edítalo o bórralo desde la semana.")
            if (txs.balance(GOAL_ID) - old.amountCents < 0) error("Borrarlo dejaría tu saldo en negativo.")
            txs.delete(id)
        }
    }

    private fun String.cleanConcept() = trim().ifEmpty { "Sin concepto" }
}
