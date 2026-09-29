package com.example.cofre.data

import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.room.withTransaction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlin.jvm.JvmName
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedOutputStream
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipException
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream
import com.example.cofre.core.WeekMath

/** Backup offline, versionado y autocontenido. No contiene credenciales ni datos de red. */
class BackupManager(
    private val context: Context,
    private val db: AppDatabase,
    private val settings: SettingsStore,
    private val media: LocalMediaStore,
) {
    companion object {
        const val FORMAT_VERSION = 1
        private const val MAX_BACKUP_BYTES = 256L * 1024L * 1024L
        private const val MAX_MEDIA_ENTRY_BYTES = 25L * 1024L * 1024L
        private const val MAX_TOTAL_MEDIA_BYTES = 256L * 1024L * 1024L
    }

    suspend fun exportTo(uri: Uri) = withContext(Dispatchers.IO) {
        val snapshot = db.withTransaction {
            BackupSnapshot(
                goals = db.goalDao().all(),
                transactions = db.transactionDao().all(),
                weeks = db.weekDao().all(),
                expenses = db.expenseDao().all(),
                transfers = db.transferDao().all(),
            )
        }
        val appSettings = settings.settings.first()
        val refs: Set<String> = buildSet {
            snapshot.expenses.mapNotNullTo(this) { it.photoUri }
            appSettings.profilePhoto?.let { add(it) }
            appSettings.chestBackground?.let { add(it) }
            appSettings.weekBackground?.let { add(it) }
            appSettings.otherBackground?.let { add(it) }
        }
        context.contentResolver.openOutputStream(uri)?.use { raw ->
            ZipOutputStream(BufferedOutputStream(raw)).use { zip ->
                val root = JSONObject()
                    .put("format", "cofre-hero-backup")
                    .put("version", FORMAT_VERSION)
                    .put("createdAt", System.currentTimeMillis())
                    .put("settings", settingsJson(appSettings))
                    .put("goals", snapshot.goals.toJson())
                    .put("transactions", snapshot.transactions.toJson())
                    .put("weeks", snapshot.weeks.toJson())
                    .put("expenses", snapshot.expenses.toJson())
                    .put("transfers", snapshot.transfers.toJson())
                putText(zip, "backup.json", root.toString())
                refs.forEach { ref ->
                    val file = media.resolve(ref) ?: error("Falta el archivo multimedia $ref")
                    zip.putNextEntry(ZipEntry(ref))
                    file.inputStream().use { it.copyTo(zip) }
                    zip.closeEntry()
                }
            }
        } ?: error("No se pudo abrir el destino del backup.")
    }

    suspend fun importFrom(uri: Uri) = withContext(Dispatchers.IO) {
        val staging = File(context.cacheDir, "backup_restore_${System.currentTimeMillis()}").apply { mkdirs() }
        try {
            lateinit var parsed: ParsedBackup
            ZipFileFromUri(context, uri).use { zip ->
                val root = JSONObject(zip.readText("backup.json"))
                validateFormat(root)
                parsed = parseSnapshot(root)
                validateSnapshot(parsed)
                val refs: Set<String> = buildSet {
                    parsed.expenses.mapNotNullTo(this) { it.photoUri }
                    addAll(listOfNotNull(parsed.settings.profilePhoto, parsed.settings.chestBackground, parsed.settings.weekBackground, parsed.settings.otherBackground))
                }
                var totalMediaBytes = 0L
                refs.forEach { ref ->
                    val safe = safeRelative(ref)
                    val entry = zip.entry(safe) ?: error("Falta el archivo multimedia $safe")
                    require(entry.size < 0 || entry.size <= MAX_MEDIA_ENTRY_BYTES) { "El archivo multimedia $safe supera el tamaño permitido para un backup." }
                    val out = File(staging, safe.removePrefix("media/")).apply { parentFile?.mkdirs() }
                    zip.open(entry).use { input -> out.outputStream().use { output ->
                        var entryBytes = 0L
                        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                        while (true) {
                            val read = input.read(buffer)
                            if (read < 0) break
                            entryBytes += read
                            totalMediaBytes += read
                            require(entryBytes <= MAX_MEDIA_ENTRY_BYTES) { "El archivo multimedia $safe supera el tamaño permitido para un backup." }
                            require(totalMediaBytes <= MAX_TOTAL_MEDIA_BYTES) { "Las fotografías y fondos superan el tamaño total permitido para un backup." }
                            output.write(buffer, 0, read)
                        }
                    } }
                    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                    BitmapFactory.decodeFile(out.absolutePath, bounds)
                    require(bounds.outWidth > 0 && bounds.outHeight > 0) { "El archivo multimedia $safe no es una imagen válida o está corrupto." }
                }
            }
            db.withTransaction {
                db.transactionDao().clear()
                db.transferDao().clear()
                db.expenseDao().clear()
                db.weekDao().clear()
                db.goalDao().clear()
                parsed.goals.forEach { db.goalDao().insert(it) }
                parsed.weeks.forEach { db.weekDao().insert(it) }
                parsed.expenses.forEach { e -> db.expenseDao().insert(ExpenseEntity(e.id, e.weekId, e.amountCents, e.concept, e.occurredAt, e.photoUri, e.createdAt, e.updatedAt)) }
                parsed.transfers.forEach { t -> db.transferDao().insert(TransferEntity(t.id, t.weekId, t.amountCents, t.concept, t.occurredAt, t.createdAt, t.updatedAt)) }
                parsed.transactions.forEach { db.transactionDao().insert(it) }
            }
            settings.restore(parsed.settings)
            replaceMedia(staging)
        } finally {
            staging.deleteRecursively()
        }
    }

    private suspend fun replaceMedia(staging: File) {
        val root = File(context.filesDir, "media")
        val old = File(context.filesDir, "media_old_restore")
        if (old.exists()) old.deleteRecursively()
        val hadOld = root.exists()
        if (hadOld && !root.renameTo(old)) error("No se pudo preparar el reemplazo de las imágenes actuales.")
        try {
            if (!staging.renameTo(root)) {
                root.mkdirs()
                staging.copyRecursively(root, overwrite = true)
                staging.deleteRecursively()
            }
            if (hadOld) old.deleteRecursively()
        } catch (t: Throwable) {
            root.deleteRecursively()
            if (hadOld) old.renameTo(root)
            throw t
        }
    }

    private fun validateFormat(root: JSONObject) {
        require(root.optString("format") == "cofre-hero-backup") { "El archivo no es un backup de Cofre Hero." }
        require(root.optInt("version", -1) == FORMAT_VERSION) { "Versión de backup no soportada." }
    }

    private fun validateSnapshot(p: ParsedBackup) {
        require(p.goals.size == 1 && p.goals.single().id == ChestRepository.GOAL_ID) { "Backup inválido: falta la meta principal del cofre." }
        val goal = p.goals.single()
        require(goal.targetCents == ChestRepository.GOAL_TARGET_CENTS) { "Backup inválido: meta incompatible." }
        require(goal.name == ChestRepository.GOAL_NAME) { "Backup inválido: nombre de meta incompatible." }
        require(p.weeks.all { it.id > 0 && it.initialCents >= 0 && it.startEpochDay == WeekMath.mondayOf(it.startEpochDay) && it.startEpochDay <= WeekMath.todayMonday() && it.createdAt >= 0 && it.updatedAt >= 0 }) { "Backup inválido: semana incorrecta." }
        val weekIds = p.weeks.map { it.id }.toSet()
        require(weekIds.size == p.weeks.size) { "Backup inválido: semanas duplicadas." }
        val transactionIds = p.transactions.map { it.id }.toSet()
        require(transactionIds.size == p.transactions.size && p.transactions.all { it.id > 0 }) { "Backup inválido: movimientos duplicados o con ID incorrecto." }
        val transferIds = p.transfers.map { it.id }.toSet()
        require(transferIds.size == p.transfers.size && p.transfers.all { it.id > 0 }) { "Backup inválido: transferencias duplicadas o con ID incorrecto." }
        val expenseIds = p.expenses.map { it.id }.toSet()
        require(expenseIds.size == p.expenses.size && p.expenses.all { it.id > 0 }) { "Backup inválido: gastos duplicados o con ID incorrecto." }
        require(p.expenses.all { it.id > 0 && it.amountCents > 0 && it.weekId in weekIds && WeekMath.contains(it.weekStart, it.occurredAt) }) { "Backup inválido: gasto fuera de su semana o cantidad incorrecta." }
        require(p.transfers.all { it.id > 0 && it.amountCents > 0 && it.weekId in weekIds && WeekMath.contains(it.weekStart, it.occurredAt) }) { "Backup inválido: transferencia inválida." }
        val byWeek = p.weeks.associateBy { it.id }
        byWeek.values.forEach { w ->
            val used = p.expenses.filter { it.weekId == w.id }.sumOf { it.amountCents } + p.transfers.filter { it.weekId == w.id }.sumOf { it.amountCents }
            require(w.initialCents >= used) { "Backup inválido: saldo semanal negativo." }
        }
        val goalId = ChestRepository.GOAL_ID
        require(p.transactions.all { it.goalId == goalId }) { "Backup inválido: movimiento ligado a una meta inexistente." }
        require(p.transactions.count { it.type == TxType.INITIAL } <= 1) { "Backup inválido: hay más de un saldo inicial." }
        require(p.transactions.all { it.occurredAt >= 0 && it.createdAt >= 0 && it.updatedAt >= 0 }) { "Backup inválido: fechas internas de movimientos incorrectas." }
        require(p.expenses.all { it.createdAt >= 0 && it.updatedAt >= 0 }) { "Backup inválido: fechas internas de gastos incorrectas." }
        require(p.transfers.all { it.createdAt >= 0 && it.updatedAt >= 0 }) { "Backup inválido: fechas internas de transferencias incorrectas." }
        require(p.transactions.all { when (it.type) { TxType.WITHDRAWAL -> it.amountCents < 0; TxType.INITIAL, TxType.DEPOSIT, TxType.TRANSFER_IN -> it.amountCents > 0 } }) { "Backup inválido: signo monetario incorrecto." }
        require(p.transactions.all { (it.type == TxType.TRANSFER_IN) == (it.transferId != null) }) { "Backup inválido: vínculo de transferencia incorrecto." }
        require(p.transactions.sumOf { it.amountCents } >= 0) { "Backup inválido: el saldo del cofre sería negativo." }
        val mirrors = p.transactions.filter { it.transferId != null }
        require(mirrors.size == transferIds.size) { "Backup inválido: falta el espejo de una transferencia." }
        require(mirrors.map { it.transferId!! }.toSet() == transferIds) { "Backup inválido: relación transferencia↔cofre incompleta." }
        p.transfers.forEach { t ->
            val mirror = mirrors.singleOrNull { it.transferId == t.id } ?: error("Backup inválido: transferencia ${t.id} sin espejo.")
            require(mirror.amountCents == t.amountCents && mirror.concept == t.concept && mirror.occurredAt == t.occurredAt && mirror.type == TxType.TRANSFER_IN) { "Backup inválido: espejo de transferencia ${t.id} inconsistente." }
        }
    }

    private fun safeRelative(ref: String): String {
        require(ref.startsWith("media/")) { "Ruta multimedia inválida." }
        require(!ref.contains("..") && !ref.contains("\\")) { "Ruta multimedia insegura." }
        return ref
    }

    private fun settingsJson(s: AppSettings) = JSONObject()
        .put("onboardingDone", s.onboardingDone).put("soundsEnabled", s.soundsEnabled).put("effectsVolumeMilli", (s.effectsVolume * 1000f).toInt())
        .putOpt("profilePhoto", s.profilePhoto)
        .put("chestBackground", bgJson(s.chestBackground, s.chestScale, s.chestOffsetX, s.chestOffsetY, s.chestOpacity))
        .put("weekBackground", bgJson(s.weekBackground, s.weekScale, s.weekOffsetX, s.weekOffsetY, s.weekOpacity))
        .put("otherBackground", bgJson(s.otherBackground, s.otherScale, s.otherOffsetX, s.otherOffsetY, s.otherOpacity))

    private fun bgJson(ref: String?, scale: Float, x: Float, y: Float, opacity: Float) = JSONObject()
        .putOpt("ref", ref).put("scaleMilli", (scale * 1000f).toInt()).put("xMilli", (x * 1000f).toInt()).put("yMilli", (y * 1000f).toInt()).put("opacityMilli", (opacity.coerceIn(0f, 1f) * 1000f).toInt())

    private fun putText(zip: ZipOutputStream, name: String, text: String) { zip.putNextEntry(ZipEntry(name)); zip.write(text.toByteArray(Charsets.UTF_8)); zip.closeEntry() }

@JvmName("goalListToJson")
    private fun List<GoalEntity>.toJson() = JSONArray().also { a -> forEach { a.put(JSONObject().put("id", it.id).put("name", it.name).put("targetCents", it.targetCents).put("createdAt", it.createdAt)) } }
@JvmName("transactionListToJson")
    private fun List<TransactionEntity>.toJson() = JSONArray().also { a -> forEach { a.put(JSONObject().put("id", it.id).put("goalId", it.goalId).put("amountCents", it.amountCents).put("concept", it.concept).put("type", it.type.name).put("occurredAt", it.occurredAt).put("createdAt", it.createdAt).put("updatedAt", it.updatedAt).putOpt("transferId", it.transferId)) } }
  @JvmName("weekListToJson")
    private fun List<WeekEntity>.toJson() = JSONArray().also { a -> forEach { a.put(JSONObject().put("id", it.id).put("startEpochDay", it.startEpochDay).put("initialCents", it.initialCents).put("createdAt", it.createdAt).put("updatedAt", it.updatedAt)) } }
   @JvmName("expenseListToJson")
    private fun List<ExpenseEntity>.toJson() = JSONArray().also { a -> forEach { a.put(JSONObject().put("id", it.id).put("weekId", it.weekId).put("amountCents", it.amountCents).put("concept", it.concept).put("occurredAt", it.occurredAt).putOpt("photoUri", it.photoUri).put("createdAt", it.createdAt).put("updatedAt", it.updatedAt)) } }
   @JvmName("transferListToJson")
    private fun List<TransferEntity>.toJson() = JSONArray().also { a -> forEach { a.put(JSONObject().put("id", it.id).put("weekId", it.weekId).put("amountCents", it.amountCents).put("concept", it.concept).put("occurredAt", it.occurredAt).put("createdAt", it.createdAt).put("updatedAt", it.updatedAt)) } }

    private data class Quad(val ref: String?, val scale: Float, val offset: FloatArray, val opacity: Float)
    private data class BackupSnapshot(val goals: List<GoalEntity>, val transactions: List<TransactionEntity>, val weeks: List<WeekEntity>, val expenses: List<ExpenseEntity>, val transfers: List<TransferEntity>)
    private data class ParsedBackup(val settings: AppSettings, val goals: List<GoalEntity>, val transactions: List<TransactionEntity>, val weeks: List<WeekEntity>, val expenses: List<ExpenseBackup>, val transfers: List<TransferBackup>)
    private data class ExpenseBackup(val id: Long, val weekId: Long, val amountCents: Long, val concept: String, val occurredAt: Long, val photoUri: String?, val createdAt: Long, val updatedAt: Long, val weekStart: Long)
    private data class TransferBackup(val id: Long, val weekId: Long, val amountCents: Long, val concept: String, val occurredAt: Long, val createdAt: Long, val updatedAt: Long, val weekStart: Long)

    private fun parseSnapshot(root: JSONObject): ParsedBackup {
        val weeks = root.getJSONArray("weeks").toWeekList()
        val byWeek = weeks.associateBy { it.id }
        val goals = root.getJSONArray("goals").toGoalList()
        val txs = root.getJSONArray("transactions").toTxList()
        val ex = root.getJSONArray("expenses").toExpenseList().map { it.copy(weekStart = byWeek[it.weekId]?.startEpochDay ?: Long.MIN_VALUE) }
        val tr = root.getJSONArray("transfers").toTransferList().map { it.copy(weekStart = byWeek[it.weekId]?.startEpochDay ?: Long.MIN_VALUE) }
        val s = root.getJSONObject("settings").toSettings()
        return ParsedBackup(s, goals, txs, weeks, ex, tr)
    }

    private fun JSONArray.toGoalList() = (0 until length()).map { o -> getJSONObject(o).let { GoalEntity(it.getLong("id"), it.getString("name"), it.getLong("targetCents"), it.getLong("createdAt")) } }
    private fun JSONArray.toTxList() = (0 until length()).map { o -> getJSONObject(o).let { TransactionEntity(it.getLong("id"), it.getLong("goalId"), it.getLong("amountCents"), it.getString("concept"), TxType.valueOf(it.getString("type")), it.getLong("occurredAt"), it.getLong("createdAt"), it.getLong("updatedAt"), if (it.isNull("transferId")) null else it.getLong("transferId")) } }
    private fun JSONArray.toWeekList() = (0 until length()).map { o -> getJSONObject(o).let { WeekEntity(it.getLong("id"), it.getLong("startEpochDay"), it.getLong("initialCents"), it.getLong("createdAt"), it.getLong("updatedAt")) } }
    private fun JSONArray.toExpenseList() = (0 until length()).map { o -> getJSONObject(o).let { ExpenseBackup(it.getLong("id"), it.getLong("weekId"), it.getLong("amountCents"), it.getString("concept"), it.getLong("occurredAt"), if (it.isNull("photoUri")) null else it.getString("photoUri"), it.getLong("createdAt"), it.getLong("updatedAt"), 0) } }
    private fun JSONArray.toTransferList() = (0 until length()).map { o -> getJSONObject(o).let { TransferBackup(it.getLong("id"), it.getLong("weekId"), it.getLong("amountCents"), it.getString("concept"), it.getLong("occurredAt"), it.getLong("createdAt"), it.getLong("updatedAt"), 0) } }

    private fun JSONObject.toSettings(): AppSettings {
        fun bg(key: String): Quad {
            val o = optJSONObject(key) ?: JSONObject()
            return Quad(o.optString("ref", null), o.optInt("scaleMilli", 1000) / 1000f, floatArrayOf(o.optInt("xMilli", 0) / 1000f, o.optInt("yMilli", 0) / 1000f), o.optInt("opacityMilli", 1000) / 1000f)
        }
        val c = bg("chestBackground"); val w = bg("weekBackground"); val o = bg("otherBackground")
        return AppSettings(optBoolean("onboardingDone", false), optBoolean("soundsEnabled", true), optInt("effectsVolumeMilli", 750) / 1000f, optString("profilePhoto", null), c.ref, c.scale, c.offset[0], c.offset[1], c.opacity, w.ref, w.scale, w.offset[0], w.offset[1], w.opacity, o.ref, o.scale, o.offset[0], o.offset[1], o.opacity)
    }

    private class ZipFileFromUri(private val context: Context, uri: Uri) : AutoCloseable {
        private val temp = File.createTempFile("cofre_backup_", ".zip", context.cacheDir)
        private val zip: ZipFile
        init {
            context.contentResolver.openInputStream(uri)?.use { input ->
                temp.outputStream().use { output ->
                    var total = 0L
                    val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                    while (true) {
                        val read = input.read(buffer)
                        if (read < 0) break
                        total += read
                        require(total <= MAX_BACKUP_BYTES) { "El backup supera el tamaño máximo permitido de 256 MB." }
                        output.write(buffer, 0, read)
                    }
                }
            } ?: error("No se pudo leer el backup.")
            try { zip = ZipFile(temp) } catch (e: ZipException) { throw IllegalArgumentException("El archivo de backup está corrupto o no es un ZIP válido.", e) }
        }
        fun entry(name: String): ZipEntry? = zip.getEntry(name)
        fun readText(name: String): String = zip.getEntry(name)?.let { zip.getInputStream(it).bufferedReader(Charsets.UTF_8).use { r -> r.readText() } } ?: error("Falta backup.json")
        fun open(entry: ZipEntry) = zip.getInputStream(entry).buffered()
        override fun close() { zip.close(); temp.delete() }
    }
}
