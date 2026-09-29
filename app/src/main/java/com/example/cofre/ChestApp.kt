package com.example.cofre

import android.app.Application
import com.example.cofre.data.AppDatabase
import com.example.cofre.data.ChestRepository
import com.example.cofre.data.SettingsStore
import com.example.cofre.data.WeekRepository

class ChestApp : Application() {
    private val database: AppDatabase by lazy { AppDatabase.build(this) }
    val settingsStore: SettingsStore by lazy { SettingsStore(this) }
    val repository: ChestRepository by lazy { ChestRepository(database, settingsStore) }
    val weekRepository: WeekRepository by lazy { WeekRepository(database) }
    val mediaStore: com.example.cofre.data.LocalMediaStore by lazy { com.example.cofre.data.LocalMediaStore(this) }
    val backupManager: com.example.cofre.data.BackupManager by lazy { com.example.cofre.data.BackupManager(this, database, settingsStore, mediaStore) }
}
