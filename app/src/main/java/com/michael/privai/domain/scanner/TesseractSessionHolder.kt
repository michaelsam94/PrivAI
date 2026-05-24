package com.michael.privai.domain.scanner

import android.content.Context
import android.graphics.Bitmap
import com.googlecode.tesseract.android.TessBaseAPI
import java.io.IOException

/**
 * Keeps one warm Tesseract session to avoid reloading models on every scan.
 */
object TesseractSessionHolder {

    /** Recognizes both Arabic and Latin script in a single pass. */
    const val BILINGUAL_LANGUAGES = "ara+eng"

    @Volatile
    private var session: TesseractSession? = null

    @Synchronized
    fun warmUp(context: Context, highAccuracy: Boolean = false) {
        getSession(context, highAccuracy = highAccuracy)
    }

    @Synchronized
    fun getSession(
        context: Context,
        highAccuracy: Boolean = false,
        languages: String = BILINGUAL_LANGUAGES
    ): TesseractSession {
        val appContext = context.applicationContext
        val dataPath = TessDataInstaller.ensureInstalled(appContext, highAccuracy)
        val existing = session
        if (existing != null && existing.matches(dataPath, languages, highAccuracy)) {
            return existing
        }

        existing?.close()
        val created = TesseractSession(dataPath, languages, highAccuracy)
        if (!created.isReady) {
            throw IOException("Tesseract failed to initialize for languages: $languages")
        }
        session = created
        return created
    }

    @Synchronized
    fun release() {
        session?.close()
        session = null
    }

    class TesseractSession internal constructor(
        private val dataPath: String,
        val languages: String,
        val highAccuracy: Boolean
    ) {
        private val api = TessBaseAPI()
        val isReady: Boolean = api.init(dataPath, languages, TessBaseAPI.OEM_LSTM_ONLY)

        init {
            if (isReady) {
                api.setVariable("user_defined_dpi", "300")
                api.setVariable("preserve_interword_spaces", "1")
                if (highAccuracy) {
                    api.setVariable("textord_heavy_nr", "1")
                }
            }
        }

        fun matches(path: String, langs: String, accuracy: Boolean): Boolean {
            return isReady &&
                dataPath == path &&
                languages == langs &&
                highAccuracy == accuracy
        }

        fun recognize(bitmap: Bitmap, pageSegMode: Int): OcrRecognitionResult {
            api.setPageSegMode(pageSegMode)
            api.setImage(bitmap)
            return OcrRecognitionResult(
                text = api.utF8Text?.trim().orEmpty(),
                confidence = api.meanConfidence().coerceAtLeast(0)
            )
        }

        fun close() {
            api.recycle()
        }
    }
}

data class OcrRecognitionResult(val text: String, val confidence: Int)
