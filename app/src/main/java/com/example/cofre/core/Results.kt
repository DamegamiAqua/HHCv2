package com.example.cofre.core

/** Equivalente suspendible de runCatching: permite capturar excepciones de operaciones Room/DataStore. */
suspend inline fun <T> runCatchingSuspend(crossinline block: suspend () -> T): Result<T> =
    try {
        Result.success(block())
    } catch (t: Throwable) {
        Result.failure(t)
    }
