package com.example.cofre.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MoneyTest {
    @Test fun parsesWholeAndDecimals() {
        assertEquals(25_000L, Money.parse("250"))
        assertEquals(25_050L, Money.parse("250.5"))
        assertEquals(125_075L, Money.parse("1,250.75"))
    }

    @Test fun rejectsInvalidPrecisionAndSigns() {
        assertNull(Money.parse("250.123"))
        assertNull(Money.parse("-10"))
        assertNull(Money.parse("$"))
    }

    @Test fun formatsSignedCentavos() {
        assertEquals("$1,250.75", Money.format(125_075L))
        assertEquals("-$1,250.75", Money.format(-125_075L))
        assertEquals("+$250.00", Money.formatSigned(25_000L))
    }
}
