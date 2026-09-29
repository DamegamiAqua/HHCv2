package com.example.cofre.data

import androidx.room.withTransaction
import com.example.cofre.core.runCatchingSuspend
import com.example.cofre.core.Money
import com.example.cofre.core.WeekMath
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

sealed interface WeekEntry {
    val at: Long
    data class Spend(val expense: ExpenseEntity) : WeekEntry { override val at get() = expense.occurredAt }
    data class Save(val transfer: TransferEntity) : WeekEntry { override val at get() = transfer.occurredAt }
}

data class WeekDetail(val week: WeekEntity, val expenses: List<ExpenseEntity>, val transfers: List<TransferEntity>) {
    val spentCents: Long = expenses.sumOf { it.amountCents }
    val savedCents: Long = transfers.sumOf { it.amountCents }
    val availableCents: Long = week.initialCents - spentCents - savedCents
    val entries: List<WeekEntry> =
        (expenses.map { WeekEntry.Spend(it) } + transfers.map { WeekEntry.Save(it) }).sortedByDescending { it.at }
}

/**
 * Reglas de la semana. Invariantes (todas dentro de db.withTransaction):
 *  - disponible = inicial − Σgastos − Σtransferencias  ≥ 0 siempre.
 *  - toda transferencia tiene EXACTAMENTE una fila espejo en el cofre (TRANSFER_IN) con mismo monto/concepto/fecha.
 *  - editar/borrar una transferencia nunca deja el saldo del cofre < 0.
 *  - fechas de gastos/transferencias dentro de su semana.
 */
class WeekRepository(private val db: AppDatabase) {
    private val weeks = db.weekDao()
    private val expenses = db.expenseDao()
    private val transfers = db.transferDao()
    private val chest = db.transactionDao()

    val summaries: Flow<List<WeekSummary>> = weeks.observeSummaries()

    fun observeDetail(id: Long): Flow<WeekDetail?> =
        combine(weeks.observe(id), expenses.observeByWeek(id), transfers.observeByWeek(id)) { w, e, t ->
            w?.let { WeekDetail(it, e, t) }
        }

    // ---------- semanas ----------
    suspend fun createWeek(startEpochDay: Long, initialCents: Long): Result<Unit> = runCatchingSuspend {
        require(initialCents >= 0) { "La cantidad no puede ser negativa." }
        val start = WeekMath.mondayOf(startEpochDay)
        require(start <= WeekMath.todayMonday()) { "No puedes crear semanas futuras." }
        db.withTransaction {
            if (weeks.getByStart(start) != null) error("Ya existe la semana ${WeekMath.rangeLabel(start)}.")
            val now = System.currentTimeMillis()
            weeks.insert(WeekEntity(startEpochDay = start, initialCents = initialCents, createdAt = now, updatedAt = now))
        }
    }

    suspend fun updateInitial(weekId: Long, initialCents: Long): Result<Unit> = runCatchingSuspend {
        require(initialCents >= 0) { "La cantidad no puede ser negativa." }
        db.withTransaction {
            val w = weeks.get(weekId) ?: error("Semana no encontrada.")
            val used = expenses.sum(weekId) + transfers.sum(weekId)
            if (initialCents < used) error("Ya gastaste y ahorraste ${Money.format(used)} esta semana; el saldo inicial no puede ser menor.")
            weeks.update(w.copy(initialCents = initialCents, updatedAt = System.currentTimeMillis()))
        }
    }

    // ---------- gastos ----------
    suspend fun addExpense(weekId: Long, cents: Long, concept: String, at: Long, photoUri: String? = null): Result<Unit> = runCatchingSuspend {
        require(cents > 0) { "Ingresa una cantidad mayor a cero." }
        db.withTransaction {
            val w = weeks.get(weekId) ?: error("Semana no encontrada.")
            requireInside(w, at)
            if (cents > availableOf(w)) error("No puedes gastar más que el saldo disponible de la semana.")
            val now = System.currentTimeMillis()
            expenses.insert(ExpenseEntity(weekId = weekId, amountCents = cents, concept = concept.clean("Gasto"),
                occurredAt = at, photoUri = photoUri, createdAt = now, updatedAt = now))
        }
    }

    suspend fun updateExpense(id: Long, cents: Long, concept: String, at: Long, photoUri: String? = null): Result<Unit> = runCatchingSuspend {
        require(cents > 0) { "Ingresa una cantidad mayor a cero." }
        db.withTransaction {
            val old = expenses.get(id) ?: error("Gasto no encontrado.")
            val w = weeks.get(old.weekId) ?: error("Semana no encontrada.")
            requireInside(w, at)
            if (cents > availableOf(w) + old.amountCents) error("Este cambio dejaría el saldo de la semana en negativo.")
            expenses.update(old.copy(amountCents = cents, concept = concept.clean("Gasto"), occurredAt = at, photoUri = photoUri,
                updatedAt = System.currentTimeMillis()))
        }
    }

    suspend fun getExpense(id: Long): ExpenseEntity? = expenses.get(id)

    suspend fun deleteExpense(id: Long): Result<Unit> = runCatchingSuspend {
        db.withTransaction {
            expenses.get(id) ?: error("Gasto no encontrado.")
            expenses.delete(id) // borrar un gasto solo aumenta el disponible: siempre válido
        }
    }

    // ---------- transferencias semana → cofre (una sola operación atómica) ----------
    suspend fun addTransfer(weekId: Long, cents: Long, concept: String, at: Long): Result<Unit> = runCatchingSuspend {
        require(cents > 0) { "Ingresa una cantidad mayor a cero." }
        db.withTransaction {
            val w = weeks.get(weekId) ?: error("Semana no encontrada.")
            requireInside(w, at)
            if (cents > availableOf(w)) error("No puedes transferir más que el saldo disponible de la semana.")
            val now = System.currentTimeMillis()
            val c = concept.clean("Ahorro semanal")
            val tid = transfers.insert(TransferEntity(weekId = weekId, amountCents = cents, concept = c,
                occurredAt = at, createdAt = now, updatedAt = now))
            chest.insert(TransactionEntity(goalId = ChestRepository.GOAL_ID, amountCents = cents, concept = c,
                type = TxType.TRANSFER_IN, occurredAt = at, createdAt = now, updatedAt = now, transferId = tid))
        }
    }

    suspend fun updateTransfer(id: Long, cents: Long, concept: String, at: Long): Result<Unit> = runCatchingSuspend {
        require(cents > 0) { "Ingresa una cantidad mayor a cero." }
        db.withTransaction {
            val old = transfers.get(id) ?: error("Transferencia no encontrada.")
            val w = weeks.get(old.weekId) ?: error("Semana no encontrada.")
            requireInside(w, at)
            if (cents > availableOf(w) + old.amountCents) error("Este cambio dejaría el saldo de la semana en negativo.")
            if (chest.balance(ChestRepository.GOAL_ID) - old.amountCents + cents < 0) {
                error("El cofre no tiene saldo suficiente para reducir esta transferencia (ya retiraste ese dinero).")
            }
            val mirror = chest.getByTransfer(id) ?: error("Inconsistencia: la transferencia no tiene movimiento en el cofre.")
            val now = System.currentTimeMillis()
            val c = concept.clean("Ahorro semanal")
            transfers.update(old.copy(amountCents = cents, concept = c, occurredAt = at, updatedAt = now))
            chest.update(mirror.copy(amountCents = cents, concept = c, occurredAt = at, updatedAt = now))
        }
    }

    suspend fun deleteTransfer(id: Long): Result<Unit> = runCatchingSuspend {
        db.withTransaction {
            val old = transfers.get(id) ?: error("Transferencia no encontrada.")
            if (chest.balance(ChestRepository.GOAL_ID) - old.amountCents < 0) {
                error("No se puede revertir: el cofre ya no tiene ese dinero (hiciste retiros).")
            }
            val mirror = chest.getByTransfer(id) ?: error("Inconsistencia: la transferencia no tiene movimiento en el cofre.")
            chest.delete(mirror.id)
            transfers.delete(id) // el dinero vuelve al disponible de la semana
        }
    }

    // ---------- helpers ----------
    private suspend fun availableOf(w: WeekEntity): Long = w.initialCents - expenses.sum(w.id) - transfers.sum(w.id)

    private fun requireInside(w: WeekEntity, at: Long) {
        if (!WeekMath.contains(w.startEpochDay, at)) error("La fecha debe estar dentro de la semana ${WeekMath.rangeLabel(w.startEpochDay)}.")
    }

    private fun String.clean(default: String) = trim().ifEmpty { default }
}
