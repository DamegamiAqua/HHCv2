package com.example.cofre.ui

/** Mensaje de error legible (en español) de una operación de repositorio, o null si fue exitosa. */
internal fun Result<Unit>.errorText(): String? =
    exceptionOrNull()?.let { it.message ?: "Ocurrió un error inesperado." }
