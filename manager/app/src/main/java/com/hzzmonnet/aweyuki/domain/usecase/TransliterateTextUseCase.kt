package com.hzzmonnet.aweyuki.domain.usecase

import com.hzzmonnet.aweyuki.domain.text.TextTransliterator

class TransliterateTextUseCase(private val transliterator: TextTransliterator) {
    operator fun invoke(value: String): String = transliterator.transliterate(value)
}
