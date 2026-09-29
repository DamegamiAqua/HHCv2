package com.example.cofre.data

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/** v1 → v2: semanas, gastos, transferencias y transactions.transferId. Debe coincidir EXACTO con las entidades. */
val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("CREATE TABLE IF NOT EXISTS `weeks` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `startEpochDay` INTEGER NOT NULL, `initialCents` INTEGER NOT NULL, `createdAt` INTEGER NOT NULL, `updatedAt` INTEGER NOT NULL)")
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_weeks_startEpochDay` ON `weeks` (`startEpochDay`)")

        db.execSQL("CREATE TABLE IF NOT EXISTS `expenses` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `weekId` INTEGER NOT NULL, `amountCents` INTEGER NOT NULL, `concept` TEXT NOT NULL, `occurredAt` INTEGER NOT NULL, `photoUri` TEXT, `createdAt` INTEGER NOT NULL, `updatedAt` INTEGER NOT NULL, FOREIGN KEY(`weekId`) REFERENCES `weeks`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_expenses_weekId` ON `expenses` (`weekId`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_expenses_occurredAt` ON `expenses` (`occurredAt`)")

        db.execSQL("CREATE TABLE IF NOT EXISTS `transfers` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `weekId` INTEGER NOT NULL, `amountCents` INTEGER NOT NULL, `concept` TEXT NOT NULL, `occurredAt` INTEGER NOT NULL, `createdAt` INTEGER NOT NULL, `updatedAt` INTEGER NOT NULL, FOREIGN KEY(`weekId`) REFERENCES `weeks`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_transfers_weekId` ON `transfers` (`weekId`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_transfers_occurredAt` ON `transfers` (`occurredAt`)")

        db.execSQL("ALTER TABLE `transactions` ADD COLUMN `transferId` INTEGER REFERENCES `transfers`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE")
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_transactions_transferId` ON `transactions` (`transferId`)")
    }
}
