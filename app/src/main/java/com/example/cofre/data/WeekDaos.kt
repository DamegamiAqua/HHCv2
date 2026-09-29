package com.example.cofre.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

/** Fila de resumen para listas: los totales los calcula SQL (SUM de Long). */
data class WeekSummary(
    val id: Long,
    val startEpochDay: Long,
    val initialCents: Long,
    val spentCents: Long,
    val savedCents: Long,
) {
    val availableCents: Long get() = initialCents - spentCents - savedCents
}

@Dao
interface WeekDao {
    @Query("SELECT * FROM weeks WHERE id = :id")
    fun observe(id: Long): Flow<WeekEntity?>

    @Query("SELECT * FROM weeks WHERE id = :id")
    suspend fun get(id: Long): WeekEntity?

    @Query("SELECT * FROM weeks WHERE startEpochDay = :start")
    suspend fun getByStart(start: Long): WeekEntity?

    @Insert
    suspend fun insert(week: WeekEntity): Long

    @Update
    suspend fun update(week: WeekEntity)

    @Query(
        """SELECT w.id AS id, w.startEpochDay AS startEpochDay, w.initialCents AS initialCents,
        COALESCE((SELECT SUM(e.amountCents) FROM expenses e WHERE e.weekId = w.id), 0) AS spentCents,
        COALESCE((SELECT SUM(t.amountCents) FROM transfers t WHERE t.weekId = w.id), 0) AS savedCents
        FROM weeks w ORDER BY w.startEpochDay DESC"""
    )
    fun observeSummaries(): Flow<List<WeekSummary>>

    @Query("SELECT * FROM weeks ORDER BY id")
    suspend fun all(): List<WeekEntity>

    @Query("DELETE FROM weeks")
    suspend fun clear()
}

@Dao
interface ExpenseDao {
    @Query("SELECT * FROM expenses WHERE weekId = :weekId ORDER BY occurredAt DESC, id DESC")
    fun observeByWeek(weekId: Long): Flow<List<ExpenseEntity>>

    @Query("SELECT COALESCE(SUM(amountCents), 0) FROM expenses WHERE weekId = :weekId")
    suspend fun sum(weekId: Long): Long

    @Query("SELECT * FROM expenses WHERE id = :id")
    suspend fun get(id: Long): ExpenseEntity?

    @Insert
    suspend fun insert(e: ExpenseEntity): Long

    @Update
    suspend fun update(e: ExpenseEntity)

    @Query("DELETE FROM expenses WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("SELECT * FROM expenses ORDER BY id")
    suspend fun all(): List<ExpenseEntity>

    @Query("DELETE FROM expenses")
    suspend fun clear()
}

@Dao
interface TransferDao {
    @Query("SELECT * FROM transfers WHERE weekId = :weekId ORDER BY occurredAt DESC, id DESC")
    fun observeByWeek(weekId: Long): Flow<List<TransferEntity>>

    @Query("SELECT COALESCE(SUM(amountCents), 0) FROM transfers WHERE weekId = :weekId")
    suspend fun sum(weekId: Long): Long

    @Query("SELECT * FROM transfers WHERE id = :id")
    suspend fun get(id: Long): TransferEntity?

    @Insert
    suspend fun insert(t: TransferEntity): Long

    @Update
    suspend fun update(t: TransferEntity)

    @Query("DELETE FROM transfers WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("SELECT * FROM transfers ORDER BY id")
    suspend fun all(): List<TransferEntity>

    @Query("DELETE FROM transfers")
    suspend fun clear()
}
