package com.hzzmonet.aweyuki.domain.usecase

import com.hzzmonet.aweyuki.domain.text.TextTransliterator

class TransliterateTextUseCase(private val transliterator: TextTransliterator) {
    operator fun invoke(value: String): String = transliterator.transliterate(value)
}
