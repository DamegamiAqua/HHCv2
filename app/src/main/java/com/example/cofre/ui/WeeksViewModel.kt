package com.example.cofre.ui

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.cofre.core.WeekMath
import com.example.cofre.data.LocalMediaStore
import com.example.cofre.data.WeekDetail
import com.example.cofre.data.WeekRepository
import com.example.cofre.data.WeekSummary
import com.example.cofre.ui.errorText
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.launch

 data class WeeksUiState(
    val loading: Boolean = true,
    val summaries: List<WeekSummary> = emptyList(),
    val currentMonday: Long = WeekMath.todayMonday(),
) { val current: WeekSummary? get() = summaries.firstOrNull { it.startEpochDay == currentMonday } }

class WeeksViewModel(private val repo: WeekRepository, private val media: LocalMediaStore) : ViewModel() {
    private val monday = MutableStateFlow(WeekMath.todayMonday())
    val state: StateFlow<WeeksUiState> = combine(repo.summaries, monday) { list, mon -> WeeksUiState(false, list, mon) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), WeeksUiState())

    fun refreshToday() { monday.value = WeekMath.todayMonday() }
    fun observeDetail(weekId: Long): Flow<WeekDetail?> = repo.observeDetail(weekId)
    fun createWeek(startEpochDay: Long, cents: Long, cb: (String?) -> Unit) = run(cb) { repo.createWeek(startEpochDay, cents) }
    fun updateInitial(weekId: Long, cents: Long, cb: (String?) -> Unit) = run(cb) { repo.updateInitial(weekId, cents) }

    fun addExpense(weekId: Long, cents: Long, concept: String, at: Long, photoRef: String?, cb: (String?) -> Unit) = run(cb) { repo.addExpense(weekId, cents, concept, at, photoRef) }
    fun updateExpense(id: Long, cents: Long, concept: String, at: Long, photoRef: String?, cb: (String?) -> Unit) = run(cb) { repo.updateExpense(id, cents, concept, at, photoRef) }

    fun copyExpensePhoto(uri: Uri, cb: (String?, String?) -> Unit) = viewModelScope.launch {
        try {
            val ref = withContext(Dispatchers.IO) { media.copyFromUri(uri, "expenses") }
            cb(ref, null)
        } catch (t: Throwable) {
            cb(null, t.message ?: "No se pudo guardar la foto.")
        }
    }

    fun deleteExpense(id: Long, cb: (String?) -> Unit) = viewModelScope.launch {
        val old = repo.getExpense(id)
        val result = repo.deleteExpense(id)
        if (result.isSuccess) withContext(Dispatchers.IO) { media.delete(old?.photoUri) }
        cb(result.errorText())
    }

    fun deleteExpensePhoto(ref: String?) = viewModelScope.launch { withContext(Dispatchers.IO) { media.delete(ref) } }

    fun addTransfer(weekId: Long, cents: Long, concept: String, at: Long, cb: (String?) -> Unit) = run(cb) { repo.addTransfer(weekId, cents, concept, at) }
    fun updateTransfer(id: Long, cents: Long, concept: String, at: Long, cb: (String?) -> Unit) = run(cb) { repo.updateTransfer(id, cents, concept, at) }
    fun deleteTransfer(id: Long, cb: (String?) -> Unit) = run(cb) { repo.deleteTransfer(id) }

    private fun run(cb: (String?) -> Unit, op: suspend () -> Result<Unit>) { viewModelScope.launch { cb(op().errorText()) } }
    companion object { fun factory(repo: WeekRepository, media: LocalMediaStore) = viewModelFactory { initializer { WeeksViewModel(repo, media) } } }
}
