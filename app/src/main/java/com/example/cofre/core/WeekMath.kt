package com.example.cofre.core

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters
import java.util.Locale

/** Semanas lunes→domingo. Las semanas se identifican por el epochDay (Long) de su lunes: sin zonas horarias ni Double. */
object WeekMath {
    private val zone: ZoneId get() = ZoneId.systemDefault()
    private val f = DateTimeFormatter.ofPattern("d MMM", Locale.forLanguageTag("es-MX"))

    fun mondayOf(epochDay: Long): Long =
        LocalDate.ofEpochDay(epochDay).with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).toEpochDay()

    fun todayMonday(): Long = mondayOf(LocalDate.now().toEpochDay())

    /** "28 sep – 4 oct" */
    fun rangeLabel(start: Long): String =
        LocalDate.ofEpochDay(start).format(f) + " – " + LocalDate.ofEpochDay(start + 6).format(f)

    fun startMs(start: Long): Long = LocalDate.ofEpochDay(start).atStartOfDay(zone).toInstant().toEpochMilli()
    fun endMs(start: Long): Long = LocalDate.ofEpochDay(start + 7).atStartOfDay(zone).toInstant().toEpochMilli() - 1
    fun contains(start: Long, ms: Long): Boolean = ms in startMs(start)..endMs(start)

    /** Fecha por defecto para un movimiento nuevo: ahora si cae en la semana; si no, el último día a mediodía. */
    fun defaultAt(start: Long): Long {
        val now = System.currentTimeMillis()
        return if (contains(start, now)) now else DateFmt.combine(LocalDate.ofEpochDay(start + 6), LocalTime.NOON)
    }
}
