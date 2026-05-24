package com.michael.privai.domain.scanner

import java.lang.Character.UnicodeBlock

object OcrTextQuality {

    fun score(text: String, preferArabic: Boolean, confidence: Int = 0): Int {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return 0

        val arabicChars = trimmed.count { isArabicChar(it) }
        val latinLetters = trimmed.count { Character.isLetter(it) && !isArabicChar(it) }
        val digits = trimmed.count { Character.isDigit(it) }
        val whitespace = trimmed.count { it.isWhitespace() }
        val meaningful = arabicChars + latinLetters + digits

        if (meaningful == 0) return confidence

        val noiseRatio = (trimmed.length - meaningful - whitespace).toFloat() / trimmed.length
        var score = confidence * 2 + meaningful * 4 + trimmed.length

        if (preferArabic) {
            score += arabicChars * 25
            val arabicRatio = arabicChars.toFloat() / meaningful
            score += (arabicRatio * 100).toInt()
            if (arabicChars == 0) score /= 5
            // Penalize broken Arabic: isolated presentation forms or excessive Latin noise.
            if (latinLetters > arabicChars && arabicChars > 0) score /= 2
        } else if (arabicChars > 0) {
            score += arabicChars * 10
        }

        if (noiseRatio > 0.35f) {
            score = (score * (1f - noiseRatio)).toInt()
        }

        return score
    }

    fun containsArabic(text: String): Boolean = text.any { isArabicChar(it) }

    fun isGoodEnoughHighAccuracy(text: String, confidence: Int): Boolean {
        if (isStrongArabicResult(text, confidence)) return true
        if (text.length >= 10 && confidence >= 40 && containsArabic(text)) return true
        if (isStrongLatinResult(text, confidence)) return true
        return text.length >= 12 && confidence >= 45
    }

    fun isStrongLatinResult(text: String, confidence: Int): Boolean {
        if (text.length < 8 || confidence < 55) return false
        val latinLetters = text.count { Character.isLetter(it) && !isArabicChar(it) }
        return latinLetters >= 6 && latinLetters.toFloat() / text.length >= 0.35f
    }

    fun isStrongArabicResult(text: String, confidence: Int): Boolean {
        if (text.length < 8 || confidence < 60) return false
        val arabicChars = text.count { isArabicChar(it) }
        return arabicChars >= 6 && arabicChars.toFloat() / text.length >= 0.35f
    }

    private fun isArabicChar(char: Char): Boolean {
        val block = UnicodeBlock.of(char)
        return block == UnicodeBlock.ARABIC ||
            block == UnicodeBlock.ARABIC_SUPPLEMENT ||
            block == UnicodeBlock.ARABIC_EXTENDED_A ||
            block == UnicodeBlock.ARABIC_PRESENTATION_FORMS_A ||
            block == UnicodeBlock.ARABIC_PRESENTATION_FORMS_B
    }
}
