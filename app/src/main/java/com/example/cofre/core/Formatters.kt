package com.example.cofre.core

import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.abs

/** Todo el dinero vive como Long en centavos (MXN). Nunca Float/Double. */
object Money {
    fun format(cents: Long): String {
        val a = abs(cents)
        val whole = String.format(Locale.US, "%,d", a / 100)
        val s = "\$$whole." + String.format(Locale.US, "%02d", a % 100)
        return if (cents < 0) "-$s" else s
    }

    fun formatSigned(cents: Long): String = if (cents > 0) "+" + format(cents) else format(cents)

    /** Texto editable sin símbolos, p. ej. "250.00". */
    fun toInput(cents: Long): String =
        String.format(Locale.US, "%d.%02d", abs(cents) / 100, abs(cents) % 100)

    /** Acepta "250", "250.5", "1,250.75", "$1,250.75". Devuelve centavos (>= 0) o null si es inválido. */
    fun parse(raw: String): Long? {
        val s = raw.trim().removePrefix("\$").replace(",", "").replace(" ", "")
        if (!Regex("""\d{1,10}(\.\d{1,2})?""").matches(s)) return null
        return BigDecimal(s).movePointRight(2).toLong()
    }
}

object DateFmt {
    private val zone: ZoneId get() = ZoneId.systemDefault()
    private val dateF = DateTimeFormatter.ofPattern("d MMM yyyy", Locale.forLanguageTag("es-MX"))
    private val timeF = DateTimeFormatter.ofPattern("HH:mm")

    fun date(ms: Long): String = Instant.ofEpochMilli(ms).atZone(zone).format(dateF)
    fun time(ms: Long): String = Instant.ofEpochMilli(ms).atZone(zone).format(timeF)
    fun localDate(ms: Long): LocalDate = Instant.ofEpochMilli(ms).atZone(zone).toLocalDate()
    fun localTime(ms: Long): LocalTime = Instant.ofEpochMilli(ms).atZone(zone).toLocalTime().withSecond(0).withNano(0)
    fun combine(d: LocalDate, t: LocalTime): Long =
        LocalDateTime.of(d, t).atZone(zone).toInstant().toEpochMilli()
}
