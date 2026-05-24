package com.michael.privai.domain.scanner

import android.content.Context
import android.net.Uri
import java.io.File
import java.io.IOException

object OcrImageCache {

    fun ensureLocalCopy(context: Context, uri: Uri): Uri {
        if (uri.scheme == "file") {
            val path = uri.path
            if (!path.isNullOrBlank() && File(path).exists()) return uri
        }

        val dir = File(context.cacheDir, "ocr_inputs").apply { mkdirs() }
        dir.listFiles()?.forEach { it.delete() }

        val dest = File(dir, "scan.jpg")
        val inputStream = context.contentResolver.openInputStream(uri)
            ?: throw IOException("Could not read the selected image. Try picking the image again.")

        inputStream.use { input ->
            dest.outputStream().use { output -> input.copyTo(output) }
        }

        if (!dest.exists() || dest.length() == 0L) {
            throw IOException("Could not save the selected image for OCR.")
        }

        return Uri.fromFile(dest)
    }
}
