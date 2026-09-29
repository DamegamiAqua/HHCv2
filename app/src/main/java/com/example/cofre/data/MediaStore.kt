package com.example.cofre.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

/** Almacenamiento privado y offline de imágenes. Las referencias son rutas relativas a filesDir. */
class LocalMediaStore(private val context: Context) {
    private val root: File get() = File(context.filesDir, "media")

    fun copyFromUri(uri: Uri, folder: String, extension: String = "jpg"): String {
        val dir = File(root, folder).apply { mkdirs() }
        val name = "${UUID.randomUUID()}.$extension"
        val file = File(dir, name)
        context.contentResolver.openInputStream(uri)?.use { input ->
            FileOutputStream(file).use { output -> input.copyTo(output) }
        } ?: error("No se pudo leer la imagen seleccionada.")
        return relative(file)
    }

    fun copyRefTo(ref: String, destinationRef: String) {
        val source = resolve(ref) ?: error("Archivo multimedia no encontrado.")
        val destination = File(root, destinationRef).apply { parentFile?.mkdirs() }
        source.inputStream().use { input -> FileOutputStream(destination).use { output -> input.copyTo(output) } }
    }

    fun resolve(ref: String?): File? {
        if (ref.isNullOrBlank()) return null
        val file = File(root, ref.removePrefix("media/")).canonicalFile
        if (!file.path.startsWith(root.canonicalPath + File.separator)) return null
        return if (file.isFile) file else null
    }

    fun delete(ref: String?) {
        resolve(ref)?.delete()
    }

    fun decode(ref: String?, maxDimension: Int): Bitmap? {
        val file = resolve(ref) ?: return null
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.absolutePath, bounds)
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
        var sample = 1
        while (bounds.outWidth / sample > maxDimension || bounds.outHeight / sample > maxDimension) sample *= 2
        val opts = BitmapFactory.Options().apply { inSampleSize = sample; inPreferredConfig = Bitmap.Config.ARGB_8888 }
        return BitmapFactory.decodeFile(file.absolutePath, opts)
    }

    private fun relative(file: File): String = "media/" + file.relativeTo(root).path.replace(File.separatorChar, '/')
}
