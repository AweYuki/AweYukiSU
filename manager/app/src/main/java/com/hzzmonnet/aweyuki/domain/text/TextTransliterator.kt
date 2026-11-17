package com.hzzmonnet.aweyuki.domain.text

fun interface TextTransliterator {
    fun transliterate(value: String): String
}
