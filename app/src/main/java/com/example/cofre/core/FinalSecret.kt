package com.example.cofre.core

import java.time.LocalDate

/** Condición del final secreto. Se calcula siempre desde el saldo real y la fecha local del dispositivo. */
object FinalSecret {
    const val UNLOCK_TARGET_CENTS = 1_500_000L
    val UNLOCK_DATE: LocalDate = LocalDate.of(2027, 9, 22)

    fun isUnlocked(balanceCents: Long, localDate: LocalDate = LocalDate.now()): Boolean =
        balanceCents >= UNLOCK_TARGET_CENTS && !localDate.isBefore(UNLOCK_DATE)
}
