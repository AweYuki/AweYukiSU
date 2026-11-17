package com.hzzmonnet.aweyuki.domain.usecase

import com.hzzmonnet.aweyuki.data.logging.BugreportRepository
import java.io.File

class GenerateBugreportUseCase(
    private val repository: BugreportRepository,
) {
    operator fun invoke(): File = repository.create()
}
