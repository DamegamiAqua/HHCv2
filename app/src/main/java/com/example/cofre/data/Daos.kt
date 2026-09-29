package com.example.cofre.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface GoalDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIgnore(goal: GoalEntity)

    @Query("SELECT * FROM goals WHERE id = :id")
    fun observe(id: Long): Flow<GoalEntity?>

    @Query("SELECT * FROM goals ORDER BY id")
    suspend fun all(): List<GoalEntity>

    @Insert
    suspend fun insert(goal: GoalEntity)

    @Query("DELETE FROM goals")
    suspend fun clear()
}

@Dao
interface TransactionDao {
    @Query("SELECT COALESCE(SUM(amountCents), 0) FROM transactions WHERE goalId = :goalId")
    fun observeBalance(goalId: Long): Flow<Long>

    @Query("SELECT COALESCE(SUM(amountCents), 0) FROM transactions WHERE goalId = :goalId")
    suspend fun balance(goalId: Long): Long

    @Query("SELECT * FROM transactions WHERE goalId = :goalId ORDER BY occurredAt DESC, id DESC")
    fun observeAll(goalId: Long): Flow<List<TransactionEntity>>

    @Query("SELECT COUNT(*) FROM transactions WHERE goalId = :goalId")
    suspend fun count(goalId: Long): Int

    @Query("SELECT * FROM transactions WHERE id = :id")
    suspend fun get(id: Long): TransactionEntity?

    @Insert
    suspend fun insert(tx: TransactionEntity): Long

    @Update
    suspend fun update(tx: TransactionEntity)

    @Query("SELECT * FROM transactions WHERE transferId = :transferId")
    suspend fun getByTransfer(transferId: Long): TransactionEntity?

    @Query("DELETE FROM transactions WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("SELECT * FROM transactions ORDER BY id")
    suspend fun all(): List<TransactionEntity>

    @Query("DELETE FROM transactions")
    suspend fun clear()
}
