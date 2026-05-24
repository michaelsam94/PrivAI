package com.michael.privai.domain.scanner

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Matrix
import android.graphics.Paint
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import java.io.File
import java.io.IOException
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sqrt

object OcrImagePreprocessor {

    private const val MAX_OCR_DIMENSION = 2000
    private const val MAX_OCR_DIMENSION_ACCURATE = 2200

    fun loadOrientedBitmap(context: Context, uri: Uri, highAccuracy: Boolean = false): Bitmap {
        val maxDimension = if (highAccuracy) MAX_OCR_DIMENSION_ACCURATE else MAX_OCR_DIMENSION
        val bytes = openImageBytes(context, uri)

        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)

        val decodeOptions = BitmapFactory.Options().apply {
            inSampleSize = calculateInSampleSize(bounds.outWidth, bounds.outHeight, maxDimension)
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }

        var bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, decodeOptions)
            ?: throw IOException("Could not decode image")

        val rotation = readExifRotation(bytes)
        if (rotation != 0) {
            bitmap = rotateBitmap(bitmap, rotation.toFloat(), recycleSource = true)
        }

        return downscaleIfNeeded(bitmap, maxDimension)
    }

    private fun openImageBytes(context: Context, uri: Uri): ByteArray {
        if (uri.scheme == "file") {
            val path = uri.path
            if (!path.isNullOrBlank()) {
                return File(path).readBytes()
            }
        }
        return context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
            ?: throw IOException("Could not read image")
    }

    /** Corrects slight camera tilt using a small probe image for speed. */
    fun correctSkew(source: Bitmap): Bitmap {
        val probeScale = max(2, min(source.width, source.height) / 800)
        val probeWidth = max(1, source.width / probeScale)
        val probeHeight = max(1, source.height / probeScale)
        val probeBitmap = Bitmap.createScaledBitmap(source, probeWidth, probeHeight, true)
        val ownsProbe = probeBitmap !== source

        val probeLuminance = extractLuminance(probeBitmap)
        if (ownsProbe) {
            recycleQuietly(probeBitmap)
        }

        val probeBinary = binarizeOtsu(probeLuminance, probeWidth, probeHeight)
        val angle = estimateSkewAngle(probeBinary, probeWidth, probeHeight)
        recycleQuietly(probeBinary)

        return if (abs(angle) >= 0.6f) {
            rotateBitmap(source, angle, recycleSource = true)
        } else {
            source
        }
    }

    /** Shared luminance pipeline — computed once per source image. */
    data class PreparedImage(
        val sharpened: IntArray,
        val width: Int,
        val height: Int,
        val blockSize: Int
    )

    fun prepareFast(source: Bitmap): PreparedImage {
        val width = source.width
        val height = source.height
        val luminance = extractLuminance(source)
        val blockSize = (min(width, height) / 40).coerceIn(15, 51) or 1
        return PreparedImage(luminance, width, height, blockSize)
    }

    fun buildAdaptiveVariant(prepared: PreparedImage): Bitmap {
        val (luminance, width, height, blockSize) = prepared
        return binarizeAdaptiveGaussian(luminance, width, height, blockSize, constant = 8)
    }

    fun buildContrastVariant(prepared: PreparedImage): Bitmap {
        val (luminance, width, height, _) = prepared
        return luminanceToContrastBitmap(luminance, width, height, contrast = 1.3f)
    }

    fun prepareEnhanced(source: Bitmap): PreparedImage {
        val width = source.width
        val height = source.height
        val sharpened = unsharpMask(extractLuminance(source), width, height)
        val blockSize = (min(width, height) / 40).coerceIn(15, 41) or 1
        return PreparedImage(sharpened, width, height, blockSize)
    }

    suspend fun forEachFastVariant(
        prepared: PreparedImage,
        block: suspend (Bitmap) -> Unit
    ) {
        val (sharpened, width, height, blockSize) = prepared
        block(binarizeAdaptiveGaussian(sharpened, width, height, blockSize, constant = 8))
        block(luminanceToContrastBitmap(sharpened, width, height, contrast = 1.35f))
    }

    suspend fun forEachThoroughVariant(
        prepared: PreparedImage,
        block: suspend (Bitmap) -> Unit
    ) {
        val (sharpened, width, height, blockSize) = prepared
        block(binarizeSauvola(sharpened, width, height, blockSize))
        block(binarizeOtsu(sharpened, width, height))
    }

    /** @deprecated Use prepareThorough + forEachFast/ThoroughVariant from a coroutine. */
    suspend fun forEachPhotoVariant(source: Bitmap, block: suspend (Bitmap) -> Unit) {
        val prepared = prepareEnhanced(source)
        forEachFastVariant(prepared, block)
        forEachThoroughVariant(prepared, block)
    }

    private fun readExifRotation(bytes: ByteArray): Int {
        return runCatching { ExifInterface(bytes.inputStream()).rotationDegrees }.getOrDefault(0)
    }

    private fun rotateBitmap(source: Bitmap, degrees: Float, recycleSource: Boolean = false): Bitmap {
        if (degrees == 0f) return source
        val matrix = Matrix().apply { postRotate(degrees) }
        val rotated = Bitmap.createBitmap(source, 0, 0, source.width, source.height, matrix, true)
        if (recycleSource && rotated != source) {
            recycleQuietly(source)
        }
        return rotated
    }

    fun buildSauvolaVariant(prepared: PreparedImage): Bitmap {
        val (luminance, width, height, blockSize) = prepared
        return binarizeSauvola(luminance, width, height, blockSize)
    }

    fun buildOtsuVariant(prepared: PreparedImage): Bitmap {
        val (luminance, width, height, _) = prepared
        return binarizeOtsu(luminance, width, height)
    }

    private fun downscaleIfNeeded(bitmap: Bitmap, maxDimension: Int = MAX_OCR_DIMENSION): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        val maxDim = max(width, height)
        if (maxDim <= maxDimension) return bitmap

        val scale = maxDimension.toFloat() / maxDim
        val newWidth = max(1, (width * scale).roundToInt())
        val newHeight = max(1, (height * scale).roundToInt())
        val scaled = Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
        if (scaled !== bitmap) {
            recycleQuietly(bitmap)
        }
        return scaled
    }

    private fun calculateInSampleSize(width: Int, height: Int, maxDimension: Int): Int {
        var sampleSize = 1
        while (max(width, height) / sampleSize > maxDimension) {
            sampleSize *= 2
        }
        return sampleSize
    }

    private fun extractLuminance(source: Bitmap): IntArray {
        require(!source.isRecycled) { "Cannot read pixels from a recycled bitmap" }
        val width = source.width
        val height = source.height
        val pixels = IntArray(width * height)
        source.getPixels(pixels, 0, width, 0, 0, width, height)
        return IntArray(pixels.size) { index ->
            val pixel = pixels[index]
            (
                Color.red(pixel) * 0.299 +
                    Color.green(pixel) * 0.587 +
                    Color.blue(pixel) * 0.114
                ).roundToInt().coerceIn(0, 255)
        }
    }

    private fun luminanceToContrastBitmap(
        luminance: IntArray,
        width: Int,
        height: Int,
        contrast: Float
    ): Bitmap {
        val translate = 128f * (1f - contrast)
        val pixels = IntArray(luminance.size)
        for (index in luminance.indices) {
            val value = (luminance[index] * contrast + translate).roundToInt().coerceIn(0, 255)
            pixels[index] = Color.rgb(value, value, value)
        }
        return pixelsToBitmap(pixels, width, height)
    }

    private fun medianFilter(source: IntArray, width: Int, height: Int, radius: Int): IntArray {
        val result = IntArray(source.size)
        val window = IntArray((radius * 2 + 1) * (radius * 2 + 1))
        for (y in 0 until height) {
            for (x in 0 until width) {
                var count = 0
                for (dy in -radius..radius) {
                    val ny = (y + dy).coerceIn(0, height - 1)
                    for (dx in -radius..radius) {
                        val nx = (x + dx).coerceIn(0, width - 1)
                        window[count++] = source[ny * width + nx]
                    }
                }
                window.sort(fromIndex = 0, toIndex = count)
                result[y * width + x] = window[count / 2]
            }
        }
        return result
    }

    private fun unsharpMask(source: IntArray, width: Int, height: Int): IntArray {
        val blurred = boxBlur(source, width, height, radius = 1)
        return IntArray(source.size) { index ->
            (source[index] * 1.6 - blurred[index] * 0.6).roundToInt().coerceIn(0, 255)
        }
    }

    private fun boxBlur(source: IntArray, width: Int, height: Int, radius: Int): IntArray {
        val result = IntArray(source.size)
        for (y in 0 until height) {
            for (x in 0 until width) {
                var sum = 0
                var count = 0
                for (dy in -radius..radius) {
                    val ny = (y + dy).coerceIn(0, height - 1)
                    for (dx in -radius..radius) {
                        val nx = (x + dx).coerceIn(0, width - 1)
                        sum += source[ny * width + nx]
                        count++
                    }
                }
                result[y * width + x] = sum / count
            }
        }
        return result
    }

    private fun binarizeAdaptiveGaussian(
        luminance: IntArray,
        width: Int,
        height: Int,
        blockSize: Int,
        constant: Int
    ): Bitmap {
        val half = blockSize / 2
        val pixels = IntArray(luminance.size)
        for (y in 0 until height) {
            for (x in 0 until width) {
                var sum = 0
                var count = 0
                for (dy in -half..half) {
                    val ny = (y + dy).coerceIn(0, height - 1)
                    for (dx in -half..half) {
                        val nx = (x + dx).coerceIn(0, width - 1)
                        sum += luminance[ny * width + nx]
                        count++
                    }
                }
                val localMean = sum / count
                val value = luminance[y * width + x]
                pixels[y * width + x] =
                    if (value < localMean - constant) Color.BLACK else Color.WHITE
            }
        }
        return pixelsToBitmap(pixels, width, height)
    }

    private fun binarizeSauvola(
        luminance: IntArray,
        width: Int,
        height: Int,
        blockSize: Int
    ): Bitmap {
        val half = blockSize / 2
        val dynamicRange = 128.0
        val k = 0.34
        val pixels = IntArray(luminance.size)

        for (y in 0 until height) {
            for (x in 0 until width) {
                var sum = 0.0
                var sumSquares = 0.0
                var count = 0
                for (dy in -half..half) {
                    val ny = (y + dy).coerceIn(0, height - 1)
                    for (dx in -half..half) {
                        val nx = (x + dx).coerceIn(0, width - 1)
                        val value = luminance[ny * width + nx].toDouble()
                        sum += value
                        sumSquares += value * value
                        count++
                    }
                }
                val mean = sum / count
                val variance = max(0.0, (sumSquares / count) - (mean * mean))
                val stdDev = sqrt(variance)
                val threshold = mean * (1.0 + k * ((stdDev / dynamicRange) - 1.0))
                val value = luminance[y * width + x]
                pixels[y * width + x] = if (value < threshold) Color.BLACK else Color.WHITE
            }
        }
        return pixelsToBitmap(pixels, width, height)
    }

    private fun binarizeOtsu(luminance: IntArray, width: Int, height: Int): Bitmap {
        val histogram = IntArray(256)
        luminance.forEach { tone -> histogram[tone]++ }
        val threshold = computeOtsuThreshold(histogram, luminance.size)
        val pixels = IntArray(luminance.size) { index ->
            if (luminance[index] >= threshold) Color.WHITE else Color.BLACK
        }
        return pixelsToBitmap(pixels, width, height)
    }

    private fun computeOtsuThreshold(histogram: IntArray, totalPixels: Int): Int {
        var sum = 0
        for (tone in histogram.indices) {
            sum += tone * histogram[tone]
        }

        var sumBackground = 0
        var weightBackground = 0
        var maxVariance = 0.0
        var threshold = 128

        for (tone in histogram.indices) {
            weightBackground += histogram[tone]
            if (weightBackground == 0) continue

            val weightForeground = totalPixels - weightBackground
            if (weightForeground == 0) break

            sumBackground += tone * histogram[tone]
            val meanBackground = sumBackground.toDouble() / weightBackground
            val meanForeground = (sum - sumBackground).toDouble() / weightForeground
            val betweenClassVariance = weightBackground.toDouble() * weightForeground *
                (meanBackground - meanForeground) * (meanBackground - meanForeground)

            if (betweenClassVariance > maxVariance) {
                maxVariance = betweenClassVariance
                threshold = tone
            }
        }

        return threshold
    }

    private fun estimateSkewAngle(binary: Bitmap, width: Int, height: Int): Float {
        var bestAngle = 0f
        var bestScore = Double.NEGATIVE_INFINITY

        for (angle in -9..9 step 3) {
            val rotated = if (angle == 0) {
                binary
            } else {
                rotateBitmap(binary, angle.toFloat(), recycleSource = false)
            }
            val score = horizontalProjectionVariance(rotated)
            if (score > bestScore) {
                bestScore = score
                bestAngle = angle.toFloat()
            }
            if (angle != 0) {
                recycleQuietly(rotated)
            }
        }

        return bestAngle
    }

    private fun horizontalProjectionVariance(binary: Bitmap): Double {
        require(!binary.isRecycled) { "Cannot read pixels from a recycled bitmap" }
        val width = binary.width
        val height = binary.height
        val pixels = IntArray(width * height)
        binary.getPixels(pixels, 0, width, 0, 0, width, height)

        val rowCounts = DoubleArray(height)
        for (y in 0 until height) {
            var count = 0
            for (x in 0 until width) {
                if (pixels[y * width + x] == Color.BLACK) count++
            }
            rowCounts[y] = count.toDouble()
        }

        val mean = rowCounts.average()
        return rowCounts.sumOf { delta -> delta * delta - 2 * mean * delta + mean * mean }
    }

    private fun pixelsToBitmap(pixels: IntArray, width: Int, height: Int): Bitmap {
        return Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888).apply {
            setPixels(pixels, 0, width, 0, 0, width, height)
        }
    }

    private fun recycleQuietly(bitmap: Bitmap) {
        if (!bitmap.isRecycled) bitmap.recycle()
    }
}
