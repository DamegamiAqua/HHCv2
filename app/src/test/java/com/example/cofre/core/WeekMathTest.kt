package com.example.cofre.core

import java.time.DayOfWeek
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WeekMathTest {
    @Test fun mondayCalculationIsStable() {
        val monday = LocalDate.of(2026, 9, 28)
        val sunday = monday.plusDays(6)
        assertEquals(monday.toEpochDay(), WeekMath.mondayOf(sunday.toEpochDay()))
        assertEquals(DayOfWeek.MONDAY, monday.dayOfWeek)
    }

    @Test fun containsUsesLocalWeekBounds() {
        val monday = LocalDate.of(2026, 9, 28).toEpochDay()
        assertTrue(WeekMath.contains(monday, WeekMath.startMs(monday)))
        assertTrue(WeekMath.contains(monday, WeekMath.endMs(monday)))
    }
}
