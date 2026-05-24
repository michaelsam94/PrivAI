package com.michael.privai.domain.scanner

import android.content.Context
import android.net.Uri
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class MLKitOCREngine(private val context: Context) {

    private val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    suspend fun extractText(imageUri: Uri): String = suspendCancellableCoroutine { continuation ->
        try {
            val image = InputImage.fromFilePath(context, imageUri)
            recognizer.process(image)
                .addOnSuccessListener { visionText ->
                    continuation.resume(visionText.text)
                }
                .addOnFailureListener { exception ->
                    continuation.resumeWithException(exception)
                }
        } catch (e: Exception) {
            continuation.resumeWithException(e)
        }
    }
}
