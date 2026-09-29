package com.example.cofre.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [GoalEntity::class, TransactionEntity::class, WeekEntity::class, ExpenseEntity::class, TransferEntity::class],
    version = 2,
    exportSchema = true,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun goalDao(): GoalDao
    abstract fun transactionDao(): TransactionDao
    abstract fun weekDao(): WeekDao
    abstract fun expenseDao(): ExpenseDao
    abstract fun transferDao(): TransferDao

    companion object {
        fun build(context: Context): AppDatabase =
            Room.databaseBuilder(context, AppDatabase::class.java, "cofre.db")
                .addMigrations(MIGRATION_1_2)
                .build()
    }
}
