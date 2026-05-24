package com.michael.privai.domain.scanner

import android.content.Context
import android.content.Intent
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.view.inputmethod.InputMethodManager
import android.view.inputmethod.InputMethodSubtype
import java.util.Locale

data class ResolvedSpeechLocales(
    val primaryLanguageTag: String,
    val allowedLanguageTags: List<String>,
    val multilingualEnabled: Boolean,
    val sourceDescription: String
) {
    val displayLabel: String
        get() = allowedLanguageTags
            .map { tag -> Locale.forLanguageTag(tag).displayName }
            .distinct()
            .joinToString(", ")
}

object SpeechLocaleResolver {

    private const val MAX_LANGUAGES = 3
    private const val LANGUAGE_TAG_NONE = "zz"

    fun createSpeechRecognizer(context: Context): SpeechRecognizer? {
        if (!SpeechRecognizer.isRecognitionAvailable(context)) return null
        return SpeechRecognizer.createSpeechRecognizer(context)
    }

    fun resolve(context: Context): ResolvedSpeechLocales {
        val keyboardTags = getKeyboardLanguageTags(context)
        val systemTags = getSystemLanguageTags(context)

        val allowed = buildFocusedLanguageList(keyboardTags, systemTags)
            .ifEmpty { listOf("en-US") }

        val sourceDescription = when {
            keyboardTags.isNotEmpty() -> "keyboard languages"
            systemTags.isNotEmpty() -> "system languages"
            else -> "default"
        }

        return ResolvedSpeechLocales(
            primaryLanguageTag = allowed.first(),
            allowedLanguageTags = allowed,
            multilingualEnabled = false,
            sourceDescription = sourceDescription
        )
    }

    fun buildRecognizerIntent(locales: ResolvedSpeechLocales): Intent {
        return buildBaseIntent(locales.primaryLanguageTag)
    }

    fun getKeyboardLanguageTags(context: Context): List<String> {
        val imm = context.getSystemService(InputMethodManager::class.java) ?: return emptyList()
        val tags = LinkedHashSet<String>()

        imm.currentInputMethodSubtype?.let { subtype ->
            extractSubtypeLanguageTag(subtype)?.let { tags.add(it) }
        }

        imm.getEnabledInputMethodSubtypeList(null, true).forEach { subtype ->
            if (isKeyboardSubtype(subtype)) {
                extractSubtypeLanguageTag(subtype)?.let { tags.add(it) }
            }
        }

        imm.enabledInputMethodList.forEach { inputMethod ->
            imm.getEnabledInputMethodSubtypeList(inputMethod, true).forEach { subtype ->
                if (isKeyboardSubtype(subtype)) {
                    extractSubtypeLanguageTag(subtype)?.let { tags.add(it) }
                }
            }
        }

        return tags.toList()
    }

    fun getSystemLanguageTags(context: Context): List<String> {
        val tags = LinkedHashSet<String>()
        val locales = context.resources.configuration.locales
        for (index in 0 until locales.size()) {
            tags.add(normalizeLanguageTag(locales[index]))
        }
        if (tags.isEmpty()) {
            tags.add(normalizeLanguageTag(Locale.getDefault()))
        }
        return tags.toList()
    }

    fun isArabicLanguageTag(tag: String): Boolean {
        return tag.substringBefore('-').lowercase(Locale.ROOT) == "ar"
    }

    /**
     * Keeps one tag per language code, keyboard languages first, capped to [MAX_LANGUAGES].
     */
    private fun buildFocusedLanguageList(
        keyboardTags: List<String>,
        systemTags: List<String>
    ): List<String> {
        val result = LinkedHashSet<String>()
        val seenCodes = mutableSetOf<String>()

        fun addTag(rawTag: String) {
            if (result.size >= MAX_LANGUAGES) return
            val tag = normalizeLanguageTagString(rawTag)
            val code = tag.substringBefore('-').lowercase(Locale.ROOT)
            if (code.isBlank() || code in seenCodes) return
            seenCodes.add(code)
            result.add(tag)
        }

        keyboardTags.forEach(::addTag)
        systemTags.forEach(::addTag)

        // Bilingual fallback only when one of en/ar is already present (typical Gboard setup).
        val hasEn = seenCodes.contains("en")
        val hasAr = seenCodes.contains("ar")
        if (hasEn && !hasAr) addTag("ar-EG")
        if (hasAr && !hasEn) addTag("en-US")

        return result.take(MAX_LANGUAGES).toList()
    }

    private fun isKeyboardSubtype(subtype: InputMethodSubtype): Boolean {
        val mode = subtype.mode
        return mode.isNullOrEmpty() || mode == "keyboard"
    }

    private fun extractSubtypeLanguageTag(subtype: InputMethodSubtype): String? {
        val languageTag = subtype.languageTag
        if (languageTag.isNotBlank() && !languageTag.equals(LANGUAGE_TAG_NONE, ignoreCase = true)) {
            return normalizeLanguageTagString(languageTag)
        }

        @Suppress("DEPRECATION")
        val legacyLocale = subtype.locale
        if (legacyLocale.isNotBlank()) {
            return normalizeLanguageTagString(legacyLocale.replace('_', '-'))
        }

        return null
    }

    private fun buildBaseIntent(primaryLanguageTag: String): Intent {
        return Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, primaryLanguageTag)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 15_000L)
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 8_000L)
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_MINIMUM_LENGTH_MILLIS, 1_000L)
        }
    }

    private fun normalizeLanguageTag(locale: Locale): String {
        return normalizeLanguageTagString(locale.toLanguageTag())
    }

    private fun normalizeLanguageTagString(rawTag: String): String {
        val tag = rawTag.trim().replace('_', '-')
        if (tag.isBlank() || tag.equals("und", ignoreCase = true)) {
            return Locale.getDefault().toLanguageTag()
        }
        return tag
    }
}
