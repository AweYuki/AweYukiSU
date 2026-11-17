package com.hzzmonnet.aweyuki.domain.usecase

import java.io.File
import com.hzzmonnet.aweyuki.data.logging.BugreportRepository

class GenerateBugreportUseCase(
    private val repository: BugreportRepository,
) {
    operator fun invoke(): File = repository.create()
}
