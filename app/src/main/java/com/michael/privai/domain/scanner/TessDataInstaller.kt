package com.michael.privai.domain.scanner

import android.content.Context
import java.io.File
import java.io.IOException

object TessDataInstaller {

    private const val INSTALL_VERSION = 5

    /**
     * Returns the parent directory that contains a readable `tessdata/` subfolder.
     * Copies the fast or best Arabic model depending on [highAccuracy].
     */
    @Synchronized
    fun ensureInstalled(context: Context, highAccuracy: Boolean): String {
        val dataRoot = File(context.filesDir, "tesseract")
        val tessDataDir = File(dataRoot, "tessdata")
        val versionFile = File(dataRoot, "version.txt")
        val expectedVersion = versionLabel(highAccuracy)
        val installedVersion = versionFile.takeIf { it.exists() }?.readText()

        if (installedVersion != expectedVersion) {
            if (tessDataDir.exists()) {
                tessDataDir.listFiles()?.forEach { it.delete() }
            }
            if (!tessDataDir.exists() && !tessDataDir.mkdirs()) {
                throw IOException("Could not create tessdata directory")
            }
            versionFile.writeText(expectedVersion)
        } else if (!tessDataDir.exists() && !tessDataDir.mkdirs()) {
            throw IOException("Could not create tessdata directory")
        }

        copyModelIfMissing(
            context = context,
            assetPath = if (highAccuracy) "tessdata/ara_best.traineddata" else "tessdata/ara.traineddata",
            destination = File(tessDataDir, "ara.traineddata")
        )

        copyModelIfMissing(
            context = context,
            assetPath = "tessdata/eng.traineddata",
            destination = File(tessDataDir, "eng.traineddata")
        )

        return dataRoot.absolutePath
    }

    private fun versionLabel(highAccuracy: Boolean): String {
        return "$INSTALL_VERSION-${if (highAccuracy) "best" else "fast"}"
    }

    private fun copyModelIfMissing(context: Context, assetPath: String, destination: File) {
        if (destination.exists() && destination.length() > 0L) return

        context.assets.open(assetPath).use { input ->
            destination.outputStream().use { output -> input.copyTo(output) }
        }
    }
}
