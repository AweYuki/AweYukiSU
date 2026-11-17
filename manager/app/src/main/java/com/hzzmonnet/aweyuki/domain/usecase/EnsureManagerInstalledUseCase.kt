package com.hzzmonnet.aweyuki.domain.usecase

import com.hzzmonnet.aweyuki.data.application.ApplicationControlRepository

class EnsureManagerInstalledUseCase(
    private val repository: ApplicationControlRepository,
) {
    suspend operator fun invoke(): Result<Unit> = repository.ensureManagerInstalled()
}
