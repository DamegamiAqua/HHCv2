package com.example.cofre.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/** Una semana (lunes→domingo). startEpochDay = LocalDate.toEpochDay() del lunes. initialCents varía cada semana. */
@Entity(tableName = "weeks", indices = [Index(value = ["startEpochDay"], unique = true)])
data class WeekEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val startEpochDay: Long,
    val initialCents: Long,
    val createdAt: Long,
    val updatedAt: Long,
)

/** Gasto semanal. amountCents > 0 (reduce el disponible). photoUri queda listo para Fase 3 (sin UI todavía). */
@Entity(
    tableName = "expenses",
    foreignKeys = [ForeignKey(WeekEntity::class, ["id"], ["weekId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("weekId"), Index("occurredAt")],
)
data class ExpenseEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val weekId: Long,
    val amountCents: Long,
    val concept: String,
    val occurredAt: Long,
    val photoUri: String? = null,
    val createdAt: Long,
    val updatedAt: Long,
)

/**
 * Operación financiera semana → cofre (lado semana, fuente canónica). Su reflejo en el cofre es la fila de
 * `transactions` con transferId = id. Ambas filas se crean/editan/borran SIEMPRE en una sola transacción de Room.
 */
@Entity(
    tableName = "transfers",
    foreignKeys = [ForeignKey(WeekEntity::class, ["id"], ["weekId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("weekId"), Index("occurredAt")],
)
data class TransferEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val weekId: Long,
    val amountCents: Long,
    val concept: String,
    val occurredAt: Long,
    val createdAt: Long,
    val updatedAt: Long,
)
