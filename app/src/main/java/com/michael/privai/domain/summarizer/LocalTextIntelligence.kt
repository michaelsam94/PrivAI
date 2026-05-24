package com.michael.privai.domain.summarizer

import java.util.Locale

data class SummaryResult(
    val summaryText: String,
    val bulletPoints: List<String>,
    val keysExtract: List<String>,
    val sentiment: String
)

object LocalTextIntelligence {

    private val STOP_WORDS = setOf(
        "the", "a", "an", "and", "or", "but", "if", "because", "as", "until", 
        "while", "of", "at", "by", "for", "with", "about", "against", "between", 
        "into", "through", "during", "before", "after", "above", "below", "to", 
        "from", "up", "down", "in", "out", "on", "off", "over", "under", "again", 
        "further", "then", "once", "here", "there", "when", "where", "why", "how", 
        "all", "any", "both", "each", "few", "more", "most", "other", "some", "such", 
        "no", "nor", "not", "only", "own", "same", "so", "than", "too", "very", 
        "s", "t", "can", "will", "just", "don", "should", "now", "i", "me", "my", 
        "myself", "we", "our", "ours", "ourselves", "you", "your", "yours", 
        "yourself", "yourselves", "he", "him", "his", "himself", "she", "her", 
        "hers", "herself", "it", "its", "itself", "they", "them", "their", 
        "theirs", "themselves", "what", "which", "who", "whom", "this", "that", 
        "these", "those", "am", "is", "are", "was", "were", "be", "been", "being", 
        "have", "has", "had", "having", "do", "does", "did", "doing", "would", "could"
    )

    private val POSITIVE_WORDS = setOf(
        "good", "great", "excellent", "beautiful", "wonderful", "amazing", "love", 
        "perfect", "superb", "outstanding", "happy", "positive", "successful", "glad", 
        "fantastic", "brilliant", "awesome", "intelligent", "easy", "secure", "safe", 
        "private", "best", "pleased", "satisfied"
    )

    private val NEGATIVE_WORDS = setOf(
        "bad", "terrible", "awful", "worst", "hate", "unhappy", "negative", "fail", 
        "failure", "poor", "difficult", "hard", "error", "leak", "danger", "unsafe", 
        "risk", "failed", "broken", "useless", "annoying", "boring", "sad"
    )

    /**
     * Highly robust extractive summary generator using sentence grading (inspired by TF-IDF / PageRank relevance scoring).
     * 100% Offline, ultra-fast, highly accurate for transcriptions and document scans.
     */
    fun analyzeText(text: String, bulletCount: Int = 4): SummaryResult {
        if (text.isBlank()) {
            return SummaryResult(
                summaryText = "No content to summarize.",
                bulletPoints = emptyList(),
                keysExtract = emptyList(),
                sentiment = "Neutral"
            )
        }

        // 1. Sentence splitting
        val sentences = text.split(Regex("(?<=[.!?])\\s+"))
            .map { it.trim() }
            .filter { it.length > 5 }

        if (sentences.isEmpty()) {
            return SummaryResult(
                summaryText = text,
                bulletPoints = listOf(text),
                keysExtract = emptyList(),
                sentiment = "Neutral"
            )
        }

        // 2. Tokenize and calculate word frequencies (ignoring stop words)
        val wordFrequencies = mutableMapOf<String, Int>()
        val totalWordsList = mutableListOf<String>()

        val splitRegex = Regex("[^a-zA-Z0-9]+")
        for (sentence in sentences) {
            val tokens = sentence.lowercase(Locale.ROOT).split(splitRegex)
            for (token in tokens) {
                if (token.length > 2 && token !in STOP_WORDS) {
                    wordFrequencies[token] = wordFrequencies.getOrDefault(token, 0) + 1
                    totalWordsList.add(token)
                }
            }
        }

        // 3. Score sentences based on term occurrences
        val sentenceScores = mutableMapOf<Int, Double>()
        for (i in sentences.indices) {
            val sentence = sentences[i]
            val tokens = sentence.lowercase(Locale.ROOT).split(splitRegex)
                .filter { it.length > 2 && it !in STOP_WORDS }
            
            if (tokens.isEmpty()) continue

            var score = 0.0
            for (token in tokens) {
                score += wordFrequencies.getOrDefault(token, 0).toDouble()
            }
            // Normalize score by sentence length to avoid favoring long sentences unfairly
            // and apply standard length damping
            sentenceScores[i] = score / (1.0 + Math.log(tokens.size.toDouble() + 1.0))
        }

        // 4. Extract top sentences in their original order
        val sortedIndices = sentenceScores.entries
            .sortedByDescending { it.value }
            .take(bulletCount)
            .map { it.key }
            .sorted() // Restore chronological flow

        val bullets = sortedIndices.map { index ->
            val s = sentences[index]
            if (s.startsWith("•") || s.startsWith("-")) s else "• $s"
        }

        val mainSummary = sortedIndices.joinToString(" ") { sentences[it] }

        // 5. Extract top keywords
        val keywords = wordFrequencies.entries
            .sortedByDescending { it.value }
            .take(6)
            .map { it.key.replaceFirstChar { char -> if (char.isLowerCase()) char.titlecase(Locale.ROOT) else char.toString() } }

        // 6. Basic Sentiment rating
        var sentimentScore = 0
        for (word in totalWordsList) {
            if (word in POSITIVE_WORDS) sentimentScore++
            if (word in NEGATIVE_WORDS) sentimentScore--
        }

        val sentimentString = when {
            sentimentScore > 1 -> "Positive"
            sentimentScore < -1 -> "Negative"
            else -> "Neutral"
        }

        return SummaryResult(
            summaryText = mainSummary,
            bulletPoints = bullets,
            keysExtract = keywords,
            sentiment = sentimentString
        )
    }
}
