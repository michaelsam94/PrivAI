package com.michael.privai.domain.scanner

import android.content.Context
import android.net.Uri

/**
 * Routes OCR to the best on-device engine for the user's languages.
 * ML Kit handles Latin script well but does not support Arabic; Tesseract fills that gap.
 */
class DocumentOCREngine(context: Context) {

    private val appContext = context.applicationContext
    private val mlKitEngine = MLKitOCREngine(appContext)
    private val tesseractEngine = TesseractOCREngine(appContext)

    suspend fun extractText(imageUri: Uri, highAccuracy: Boolean): String {
        val tesseractText = runCatching {
            tesseractEngine.extractText(imageUri, highAccuracy)
        }.getOrDefault("")

        if (highAccuracy) {
            return tesseractText.ifBlank {
                throw java.io.IOException("No readable text found in this image.")
            }
        }

        val mlKitText = runCatching { mlKitEngine.extractText(imageUri) }.getOrDefault("")
        return pickBestResult(mlKitText, tesseractText)
    }

    private fun pickBestResult(mlKitText: String, tesseractText: String): String {
        when {
            mlKitText.isBlank() && tesseractText.isBlank() ->
                throw java.io.IOException("No readable text found in this image.")
            mlKitText.isBlank() -> return tesseractText
            tesseractText.isBlank() -> return mlKitText
        }

        val mlHasArabic = OcrTextQuality.containsArabic(mlKitText)
        val tessHasArabic = OcrTextQuality.containsArabic(tesseractText)

        // English / Latin-only documents: ML Kit is usually stronger.
        if (!mlHasArabic && !tessHasArabic) {
            val mlScore = OcrTextQuality.score(mlKitText, preferArabic = false)
            val tessScore = OcrTextQuality.score(tesseractText, preferArabic = false)
            return if (tessScore > mlScore) tesseractText else mlKitText
        }

        // Arabic or mixed script: prefer bilingual Tesseract.
        if (tessHasArabic) {
            val tessScore = OcrTextQuality.score(tesseractText, preferArabic = true)
            val mlScore = OcrTextQuality.score(mlKitText, preferArabic = true)
            return if (tessScore >= mlScore) tesseractText else mlKitText
        }

        return mlKitText
    }
}
