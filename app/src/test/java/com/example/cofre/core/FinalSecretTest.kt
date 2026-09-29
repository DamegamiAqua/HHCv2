package com.example.cofre.core

import java.time.LocalDate
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FinalSecretTest {
    @Test fun `15000 before unlock date is locked`() {
        assertFalse(FinalSecret.isUnlocked(1_500_000L, LocalDate.of(2027, 9, 21)))
    }

    @Test fun `14999 on unlock date is locked`() {
        assertFalse(FinalSecret.isUnlocked(1_499_900L, LocalDate.of(2027, 9, 22)))
    }

    @Test fun `15000 on unlock date is unlocked`() {
        assertTrue(FinalSecret.isUnlocked(1_500_000L, LocalDate.of(2027, 9, 22)))
    }

    @Test fun `more than target after unlock date stays unlocked`() {
        assertTrue(FinalSecret.isUnlocked(1_525_000L, LocalDate.of(2027, 9, 23)))
    }
}
