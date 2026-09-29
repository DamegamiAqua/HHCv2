package com.example.cofre.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/** Una meta/cofre. Se usa solo la meta id=1; el esquema ya admite varias. */
@Entity(tableName = "goals")
data class GoalEntity(
    @PrimaryKey val id: Long,
    val name: String,
    val targetCents: Long,
    val createdAt: Long,
)

/** TRANSFER_IN = dinero que llegó desde una semana (siempre ligado a un TransferEntity). */
enum class TxType { INITIAL, DEPOSIT, WITHDRAWAL, TRANSFER_IN }

/**
 * Libro mayor del cofre. amountCents va CON SIGNO: aportación/saldo inicial/transferencia > 0, retiro < 0.
 * Saldo del cofre = SUM(amountCents). Si transferId != null, la fila es el reflejo de una transferencia semanal
 * (relación 1:1, índice único) y solo se modifica desde WeekRepository.
 */
@Entity(
    tableName = "transactions",
    foreignKeys = [
        ForeignKey(GoalEntity::class, ["id"], ["goalId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(TransferEntity::class, ["id"], ["transferId"], onDelete = ForeignKey.CASCADE),
    ],
    indices = [Index("goalId"), Index("occurredAt"), Index(value = ["transferId"], unique = true)],
)
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val goalId: Long,
    val amountCents: Long,
    val concept: String,
    val type: TxType,
    val occurredAt: Long,
    val createdAt: Long,
    val updatedAt: Long,
    val transferId: Long? = null,
)
