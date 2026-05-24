package com.michael.privai.domain.scanner

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import com.googlecode.tesseract.android.TessBaseAPI
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import java.io.IOException
import kotlin.coroutines.coroutineContext

class TesseractOCREngine(context: Context) {

    private val appContext = context.applicationContext

    suspend fun extractText(imageUri: Uri, highAccuracy: Boolean): String =
        withContext(kotlinx.coroutines.Dispatchers.Default) {
            if (highAccuracy) {
                extractHighAccuracy(imageUri)
            } else {
                extractFast(imageUri)
            }
        }

    private suspend fun extractFast(imageUri: Uri): String {
        var source = OcrImagePreprocessor.loadOrientedBitmap(appContext, imageUri, highAccuracy = false)
        val session = TesseractSessionHolder.getSession(appContext, highAccuracy = false)

        var bestText = ""
        var bestScore = Int.MIN_VALUE

        try {
            val prepared = OcrImagePreprocessor.prepareFast(source)

            runPass(session, prepared, useAdaptive = true, pageSegMode = PSM_BLOCK)?.let { result ->
                track(result, bestText, bestScore).also { (text, score) ->
                    bestText = text
                    bestScore = score
                }
                if (bestText.isNotBlank()) return bestText.trim()
            }

            runPass(session, prepared, useAdaptive = false, pageSegMode = PSM_BLOCK)?.let { result ->
                track(result, bestText, bestScore).also { (text, score) ->
                    bestText = text
                    bestScore = score
                }
            }

            if (bestText.isNotBlank()) return bestText.trim()

            runPass(session, prepared, useAdaptive = true, pageSegMode = PSM_SPARSE)?.let { result ->
                track(result, bestText, bestScore).also { (text, score) ->
                    bestText = text
                    bestScore = score
                }
            }
        } finally {
            recycleQuietly(source)
        }

        return bestText.trim().ifBlank {
            throw IOException("No readable text found in this image.")
        }
    }

    private suspend fun extractHighAccuracy(imageUri: Uri): String {
        var bestText = ""

        try {
            bestText = withTimeout(HIGH_ACCURACY_TIMEOUT_MS) {
                var localBest = ""
                var localScore = Int.MIN_VALUE
                var source = OcrImagePreprocessor.loadOrientedBitmap(appContext, imageUri, highAccuracy = true)
                try {
                    coroutineContext.ensureActive()
                    source = OcrImagePreprocessor.correctSkew(source)
                    val prepared = OcrImagePreprocessor.prepareEnhanced(source)
                    val session = TesseractSessionHolder.getSession(
                        context = appContext,
                        highAccuracy = true
                    )

                    val passes = listOf(
                        Pass(useAdaptive = true, pageSegMode = PSM_BLOCK),
                        Pass(useAdaptive = false, pageSegMode = PSM_BLOCK),
                        Pass(useAdaptive = true, pageSegMode = PSM_SPARSE),
                        Pass(useOtsu = true, pageSegMode = PSM_BLOCK),
                        Pass(useAdaptive = false, pageSegMode = PSM_SPARSE)
                    )

                    for (pass in passes) {
                        coroutineContext.ensureActive()
                        val result = runHighAccuracyPass(session, prepared, pass) ?: continue
                        val tracked = track(result, localBest, localScore)
                        localBest = tracked.first
                        localScore = tracked.second
                        if (OcrTextQuality.isGoodEnoughHighAccuracy(result.text, result.confidence)) {
                            return@withTimeout localBest.trim()
                        }
                    }

                    localBest.trim()
                } finally {
                    recycleQuietly(source)
                }
            }
        } catch (_: TimeoutCancellationException) {
            if (bestText.isNotBlank()) {
                return bestText.trim()
            }
            throw IOException("High accuracy OCR timed out. Try fast mode or a clearer photo.")
        }

        return bestText.ifBlank {
            throw IOException("No readable text found in this image.")
        }
    }

    private data class Pass(
        val useAdaptive: Boolean = false,
        val useOtsu: Boolean = false,
        val pageSegMode: Int
    )

    private fun runHighAccuracyPass(
        session: TesseractSessionHolder.TesseractSession,
        prepared: OcrImagePreprocessor.PreparedImage,
        pass: Pass
    ): OcrRecognitionResult? {
        val variant = when {
            pass.useOtsu -> OcrImagePreprocessor.buildOtsuVariant(prepared)
            pass.useAdaptive -> OcrImagePreprocessor.buildAdaptiveVariant(prepared)
            else -> OcrImagePreprocessor.buildContrastVariant(prepared)
        }
        return try {
            session.recognize(variant, pass.pageSegMode)
        } finally {
            recycleQuietly(variant)
        }
    }

    private fun runPass(
        session: TesseractSessionHolder.TesseractSession,
        prepared: OcrImagePreprocessor.PreparedImage,
        useAdaptive: Boolean,
        pageSegMode: Int
    ): OcrRecognitionResult? {
        val variant = if (useAdaptive) {
            OcrImagePreprocessor.buildAdaptiveVariant(prepared)
        } else {
            OcrImagePreprocessor.buildContrastVariant(prepared)
        }
        return try {
            session.recognize(variant, pageSegMode)
        } finally {
            recycleQuietly(variant)
        }
    }

    private fun track(
        result: OcrRecognitionResult,
        currentBest: String,
        currentScore: Int
    ): Pair<String, Int> {
        val preferArabic = OcrTextQuality.containsArabic(result.text)
        val score = OcrTextQuality.score(result.text, preferArabic = preferArabic, confidence = result.confidence)
        return if (score > currentScore && result.text.isNotBlank()) {
            result.text to score
        } else {
            currentBest to currentScore
        }
    }

    private fun recycleQuietly(bitmap: Bitmap) {
        if (!bitmap.isRecycled) bitmap.recycle()
    }

    private companion object {
        const val PSM_BLOCK = TessBaseAPI.PageSegMode.PSM_SINGLE_BLOCK
        const val PSM_SPARSE = TessBaseAPI.PageSegMode.PSM_SPARSE_TEXT
        const val HIGH_ACCURACY_TIMEOUT_MS = 45_000L
    }
}
