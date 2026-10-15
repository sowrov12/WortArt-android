package com.sowrov.germanarticlebangla.data

import java.text.Normalizer
import java.util.Locale

private val MARKS = Regex("\\p{M}+")
private val NOT_ALNUM = Regex("[^a-z0-9 ]")
private val SPACES = Regex("\\s+")

/** Lower case, no accents or punctuation, single spaces: "Müll!" and "mull" compare equal. */
private fun normalizeSpeech(text: String): String {
    val lower = text.lowercase(Locale.GERMAN).replace("ß", "ss")
    val plain = Normalizer.normalize(lower, Normalizer.Form.NFD).replace(MARKS, "")
    return plain.replace(NOT_ALNUM, " ").replace(SPACES, " ").trim()
}

/** true when [heard] contains an accepted article directly followed by the word, e.g. "der Sport". */
fun matchesSpoken(heard: String, word: Word): Boolean {
    val said = " " + normalizeSpeech(heard) + " "
    val target = normalizeSpeech(word.word)
    if (target.isEmpty()) return false
    return word.articles.any { article -> said.contains(" ${article.name} $target ") }
}
